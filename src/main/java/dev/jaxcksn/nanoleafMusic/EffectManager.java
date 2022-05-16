/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic;

import ch.qos.logback.classic.Logger;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.miscellaneous.AudioAnalysis;
import com.wrapper.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.Image;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERefreshRequest;
import com.wrapper.spotify.requests.data.player.GetUsersCurrentlyPlayingTrackRequest;
import com.wrapper.spotify.requests.data.tracks.GetAudioAnalysisForTrackRequest;
import de.androidpit.colorthief.ColorThief;
import dev.jaxcksn.nanoleafMusic.controllers.PlaybackView;
import dev.jaxcksn.nanoleafMusic.effects.EffectType;
import dev.jaxcksn.nanoleafMusic.effects.FireworkEffect;
import dev.jaxcksn.nanoleafMusic.effects.MusicEffect;
import dev.jaxcksn.nanoleafMusic.effects.PulseBeatEffect;
import dev.jaxcksn.nanoleafMusic.effects.VibeEffect;
import dev.jaxcksn.nanoleafMusic.utility.PaletteColor;
import dev.jaxcksn.nanoleafMusic.utility.Settings;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;
import io.github.rowak.nanoleafapi.Color;
import io.github.rowak.nanoleafapi.NanoleafDevice;
import io.github.rowak.nanoleafapi.NanoleafException;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EffectManager {
    //--- Control Variables
    public SpotifyApi spotifyApi;
    private int expiresIn;
    public NanoleafDevice device;
    private final PlaybackView viewController;
    public MusicEffect activeEffect;

    //--- Effect Variables
    private boolean isRunning;
    public boolean isPlaying = false;
    private Track currentTrack;
    private AudioAnalysis currentTrackAnalysis;
    private int progress;
    private String artworkURL = "@images/gray-square.png";
    public Settings settings;
    public Color[] palette = new Color[]{Color.RED, Color.BLUE, Color.GREEN};
    private ScheduledExecutorService sES;
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.EffectManager");

    public EffectManager(SpotifyApi spotifyApi, int expiresIn, NanoleafDevice device, PlaybackView viewController) {
        this.spotifyApi = spotifyApi;
        this.expiresIn = expiresIn;
        this.device = device;
        this.viewController = viewController;
        settings = DataManager.loadSettings();
        switch (settings.activeEffectType) {
            case FIREWORKS:
                this.activeEffect = new FireworkEffect(palette, device);
                break;
            case PULSEBEAT:
                this.activeEffect = new PulseBeatEffect(palette, device);
                break;
            case VIBE:
                this.activeEffect = new VibeEffect(palette, device);
                break;
        }

        logger.info("Effect manager object initialized, starting refresh timer");
        startRefreshTimer();
    }

    public void switchEffect(EffectType effectType) {
        settings.activeEffectType = effectType;
        Color[] currentPalette = activeEffect.getPalette();
        switch (effectType) {
            case FIREWORKS:
                this.activeEffect = new FireworkEffect(currentPalette, device);
                break;
            case PULSEBEAT:
                this.activeEffect = new PulseBeatEffect(currentPalette, device);
                break;
            case VIBE:
                this.activeEffect = new VibeEffect(currentPalette, device);
                break;
        }
    }


    public void reloadEffect() {
        logger.info("Reloading Effect...");
        logger.warn("Attempting to shut down the Scheduled Executor Service. If this fails, the program can't recover.");
        sES.shutdownNow();
        try {
            if (!sES.awaitTermination(60, TimeUnit.SECONDS)) {
                throw new Exception("Scheduled Executor Service failed to shutdown");
            } else {
                isPlaying = false;
                isRunning = false;
                if (settings.albumColors) {
                    displayTrackInformation(false, false);
                } else {
                    activeEffect.setPalette(palette);
                }
                this.startEffect();
                logger.info("Finished reloading the effect");
            }
        } catch (Exception e) {
            Main.showException(e);
            System.exit(1);
        }


    }


    private void startRefreshTimer() {
        ScheduledExecutorService rsES = Executors.newSingleThreadScheduledExecutor();
        Runnable refreshTask = () -> {
            try {
                AuthorizationCodePKCERefreshRequest authorizationCodePKCERefreshRequest = spotifyApi.authorizationCodePKCERefresh().build();
                AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodePKCERefreshRequest.execute();
                spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
                spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
                expiresIn = authorizationCodeCredentials.getExpiresIn();
            } catch (ParseException | IOException e) {
                Main.showException(e);
            } catch (SpotifyWebApiException spotifyWebApiException) {
                showSWAE(spotifyWebApiException);
            }
        };

        rsES.scheduleAtFixedRate(refreshTask, expiresIn / 2, expiresIn, TimeUnit.SECONDS);

    }

    public void startEffect() {
        if(!isRunning) {
            isRunning = true;
            if (!settings.albumColors) {
                palette = PaletteColor.toEffectColorArray(settings.colorPalette);
            }
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            sES = Executors.newScheduledThreadPool(4 * availableProcessors, new Main.NamedThreadFactory("effect"));
            logger.info("There are {} cores available, using a thread pool of {} threads", availableProcessors, 4 * availableProcessors);
            Runnable effectPulseTask = () -> {
                if (isPlaying) {
                    try {
                        pulseTask();
                    } catch (IOException e) {
                        Main.showException(e);
                    }
                }
            };

            Runnable spotifyUpdateTask = () -> {
                try {
                    spotifyTask();
                } catch (ParseException | IOException | InterruptedException e) {
                    Main.showException(e);
                } catch (SpotifyWebApiException spotifyWebApiException) {
                    showSWAE(spotifyWebApiException);
                }
            };

            //Prevents UI From Freezing Up when Nothing is Playing
            Thread initThread = new Thread(this::initEffect);
            initThread.setName("effect-init");
            initThread.start();
            try {
                initThread.join();
            } catch (InterruptedException e) {
                Main.showException(e);
            }

            sES.scheduleAtFixedRate(effectPulseTask, 0, 100, TimeUnit.MILLISECONDS);
            logger.info("Effect timer was scheduled");
            sES.scheduleAtFixedRate(spotifyUpdateTask, 0, 2000, TimeUnit.MILLISECONDS);
            logger.info("Spotify Poll timer was scheduled");
            displayTrackInformation(true, false);
        }
    }

    private void initEffect() {
        try {
            CurrentlyPlaying currentlyPlaying = getCurrentlyPlaying();
            if (currentlyPlaying == null) {
                logger.warn("Nothing is playing on Spotify, polling again in 5 seconds.");
            }
            while (currentlyPlaying == null && !isPlaying) {
                TimeUnit.SECONDS.sleep(5);
                currentlyPlaying = getCurrentlyPlaying();
            }
            assert currentlyPlaying != null;
            currentTrack = ((Track) currentlyPlaying.getItem());
            currentTrackAnalysis = getTrackAnalysis(currentTrack.getId());
            progress = currentlyPlaying.getProgress_ms();
            isPlaying = true;
            logger.info("Finished effect initialization");
        } catch (ParseException | IOException | InterruptedException e) {
            Main.showException(e);
        } catch (SpotifyWebApiException spotifyWebApiException) {
            showSWAE(spotifyWebApiException);
        }
    }

    private void showSWAE(SpotifyWebApiException spotifyWebApiException) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Exception");
        alert.setHeaderText("Spotify Web Api Exception");
        alert.setContentText("An exception was thrown.");


// Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        spotifyWebApiException.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

// Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);
        alert.showAndWait();
    }
    // ---

    private void pulseTask() throws IOException {
        if (isPlaying) {
            SpecificAudioAnalysis analysis = SpecificAudioAnalysis.getAnalysis(currentTrackAnalysis, progress, 100);
            activeEffect.run(analysis);
            progress += 100;
        }
    }

    private void spotifyTask() throws ParseException, SpotifyWebApiException, IOException, InterruptedException {
        CurrentlyPlaying currentPlayback = getCurrentlyPlaying();
        if (currentPlayback == null) {
            if (isPlaying) {
                logger.warn("Nothing is playing on Spotify. ");
            }
            isPlaying = false;
            viewController.setPlayback(false);
        } else {
            Track newTrack = ((Track) currentPlayback.getItem());
            if (!currentTrack.getId().equals(newTrack.getId())) {
                currentTrack = newTrack;
                currentTrackAnalysis = getTrackAnalysis(newTrack.getId());
                progress = currentPlayback.getProgress_ms();
                displayTrackInformation(true, false);
            }

            float progressDifference = Math.abs(currentPlayback.getProgress_ms() - progress);
            if (currentPlayback.getIs_playing() && !isPlaying) {
                isPlaying = true;

                progress = currentPlayback.getProgress_ms() + 1000;
                displayTrackInformation(true, false);
            } else if (!currentPlayback.getIs_playing() && isPlaying) {
                isPlaying = false;
                progress = currentPlayback.getProgress_ms();
                displayTrackInformation(true, true);
            } else if (currentPlayback.getIs_playing() && progressDifference >= 10) {
                progress = currentPlayback.getProgress_ms();
            }
        }
    }

    public void displayTrackInformation(boolean updateArt, boolean isPaused) {
        if (!settings.albumColors) {
            if (updateArt) {
                activeEffect.setSongChanged();
                Image[] artwork = currentTrack.getAlbum().getImages();
                artworkURL = artwork[1].getUrl();
            }

            ArtistSimplified[] songArtists = currentTrack.getArtists();

            new Thread(() -> {
                activeEffect.setPalette(palette);
                viewController.setPlayback(currentTrack.getName(), songArtists, artworkURL);
            }).start();
        } else {
            if (isPlaying) {
                if (updateArt) {
                    activeEffect.setSongChanged();
                    Image[] artwork = currentTrack.getAlbum().getImages();
                    artworkURL = artwork[1].getUrl();
                }
                ArtistSimplified[] songArtists = currentTrack.getArtists();

                new Thread(() -> {
                    try {
                        BufferedImage image = ImageIO.read(new URL(artworkURL));
                        int[][] colorArray = ColorThief.getPalette(image, 6);
                        activeEffect.setPalette(colorArray);
                    } catch (IOException e) {
                        Main.showException(e);
                    }
                    viewController.setPlayback(currentTrack.getName(), songArtists, artworkURL);
                }).start();
            } else {
                viewController.setPlayback(isPaused);
            }
        }
    }

    // ---

    private CurrentlyPlaying getCurrentlyPlaying() throws ParseException, SpotifyWebApiException, IOException {
        final GetUsersCurrentlyPlayingTrackRequest trackRequest = spotifyApi.getUsersCurrentlyPlayingTrack().additionalTypes("track").build();
        return trackRequest.execute();
    }

    private AudioAnalysis getTrackAnalysis(String trackID) throws ParseException, SpotifyWebApiException, IOException {
        final GetAudioAnalysisForTrackRequest trackAnalysisRequest = spotifyApi.getAudioAnalysisForTrack(trackID).build();
        return trackAnalysisRequest.execute();
    }
}
