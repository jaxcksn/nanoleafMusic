/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic;

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
import dev.jaxcksn.nanoleafMusic.effects.*;
import dev.jaxcksn.nanoleafMusic.utility.PaletteColor;
import dev.jaxcksn.nanoleafMusic.utility.Settings;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;
import io.github.rowak.nanoleafapi.Aurora;
import io.github.rowak.nanoleafapi.Color;
import io.github.rowak.nanoleafapi.StatusCodeException;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.apache.hc.core5.http.ParseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EffectManager {
    //--- Control Variables
    public SpotifyApi spotifyApi;
    private int expiresIn;
    public Aurora device;
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

    public EffectManager(SpotifyApi spotifyApi, int expiresIn, Aurora device, PlaybackView viewController) {
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

        System.out.println("\u001b[92;1m✔\u001b[0m Effect Manager Loaded");
        startRefreshTimer();
    }

    public void switchEffect(EffectType effectType) {
        System.out.println("\u001b[96;1mℹ\u001b[0m Changing Effect");
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
        System.out.println("\n" + "\u001b[96;1mℹ\u001b[0m Attempting to Restart Effect");
        sES.shutdownNow();
        try {
            if (!sES.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate");
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Error Reloading Effect");
                alert.setContentText("There was issue stopping the scheduled executor tasks, and the program needs to be restarted.");
                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.setMinHeight(Region.USE_PREF_SIZE);
                dialogPane.getStylesheets().add("/gui.css");
                alert.showAndWait();
            } else {
                isPlaying = false;
                isRunning = false;
                if (settings.albumColors) {
                    displayTrackInformation(false, false);
                } else {
                    activeEffect.setPalette(palette);
                }
                this.startEffect();
                System.out.println("\u001b[92;1m✔\u001b[0m Finished Restarting Effect\n");
            }
        } catch (InterruptedException e) {
            Main.showException(e);
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

            sES = Executors.newScheduledThreadPool(4 * Runtime.getRuntime().availableProcessors());
            System.out.println("\u001b[96;1mℹ\u001b[0m Using " + 4 * Runtime.getRuntime().availableProcessors() + " threads.");
            Runnable effectPulseTask = () -> {
                if (isPlaying) {
                    try {
                        pulseTask();
                    } catch (StatusCodeException | IOException e) {
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
            initThread.start();
            System.out.println("\u001b[92;1m✔\u001b[0m Starting Initialization");
            try {
                initThread.join();
            } catch (InterruptedException e) {
                Main.showException(e);
            }

            sES.scheduleAtFixedRate(effectPulseTask, 0, 100, TimeUnit.MILLISECONDS);
            System.out.println("\u001b[92;1m✔\u001b[0m Pulse Timers Started");
            sES.scheduleAtFixedRate(spotifyUpdateTask, 0, 2000, TimeUnit.MILLISECONDS);
            System.out.println("\u001b[92;1m✔\u001b[0m Spotify Update Timers Started");
            displayTrackInformation(true, false);
        }
    }

    private void initEffect() {
        try {
            CurrentlyPlaying currentlyPlaying = getCurrentlyPlaying();
            if (currentlyPlaying == null) {
                System.out.println("\u001b[96;1mℹ\u001b[0m Current Playback returned null, starting wait loop.");
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
            System.out.println("\u001b[92;1m✔\u001b[0m Finished initialization");
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

    private void pulseTask() throws StatusCodeException, IOException {
        if (isPlaying) {
            SpecificAudioAnalysis analysis = SpecificAudioAnalysis.getAnalysis(currentTrackAnalysis, progress, 100);
            activeEffect.run(analysis);
            progress += 100;
        }
    }

    private void spotifyTask() throws ParseException, SpotifyWebApiException, IOException, InterruptedException {
        CurrentlyPlaying currentPlayback = getCurrentlyPlaying();
        if (currentPlayback == null) {
            isPlaying = false;
            CountDownLatch playLatch = new CountDownLatch(1);
            displayTrackInformation(false, false);
            new Thread(() -> {
                try {
                    CurrentlyPlaying current = getCurrentlyPlaying();
                    while (current == null) {
                        TimeUnit.SECONDS.sleep(4);
                        current = getCurrentlyPlaying();
                    }
                    playLatch.countDown();
                } catch (ParseException | IOException | InterruptedException e) {
                    Main.showException(e);
                } catch (SpotifyWebApiException spotifyWebApiException) {
                    showSWAE(spotifyWebApiException);
                }
            }).start();
            playLatch.await();
            currentPlayback = getCurrentlyPlaying();
        }

        Track newTrack = ((Track) currentPlayback.getItem());
        if(!currentTrack.getId().equals(newTrack.getId())) {
            currentTrack = newTrack;
            currentTrackAnalysis = getTrackAnalysis(newTrack.getId());
            progress = currentPlayback.getProgress_ms();
            displayTrackInformation(true, false);
        }

        float progressDifference = Math.abs(currentPlayback.getProgress_ms() - progress);
        if(currentPlayback.getIs_playing() && !isPlaying) {
            isPlaying = true;
            progress = currentPlayback.getProgress_ms() + 500;
            displayTrackInformation(true, false);
        } else if(!currentPlayback.getIs_playing() && isPlaying) {
            isPlaying = false;
            progress = currentPlayback.getProgress_ms();
            displayTrackInformation(true, true);
        } else if (currentPlayback.getIs_playing() && progressDifference >= 10) {
            progress = currentPlayback.getProgress_ms();
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
