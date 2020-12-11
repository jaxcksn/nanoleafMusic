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
import dev.jaxcksn.nanoleafMusic.utility.PaletteColor;
import dev.jaxcksn.nanoleafMusic.utility.PulseBeat;
import dev.jaxcksn.nanoleafMusic.utility.Settings;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;
import io.github.rowak.nanoleafapi.Aurora;
import io.github.rowak.nanoleafapi.Color;
import io.github.rowak.nanoleafapi.StatusCodeException;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
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
    public PulseBeat pulseBeat;

    //--- Effect Variables
    private boolean isRunning, isPlaying = false;
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
        this.pulseBeat = new PulseBeat(palette, device);
        settings = DataManager.loadSettings();
        System.out.println("\u001b[92;1m✔\u001b[0m Effect Manager Loaded");
        startRefreshTimer();
    }

    public void reloadEffect() {
        System.out.println("\n\u001b[96;1m\u001b[0m Attempting to Restart Effect");
        sES.shutdownNow();
        try {
            if (!sES.awaitTermination(30, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate");
            } else {
                isPlaying = false;
                isRunning = false;
                if (settings.albumColors) {
                    displayTrackInformation(false, false);
                } else {
                    pulseBeat.setPalette(palette);
                }
                this.startEffect();
                System.out.println("\u001b[92;1m✔\u001b[0m Finished Restarting Effect\n");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
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
                e.printStackTrace();
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
                    } catch (StatusCodeException e) {
                        e.printStackTrace();
                    }
                }
            };

            Runnable spotifyUpdateTask = () -> {
                try {
                    spotifyTask();
                } catch (ParseException | IOException | InterruptedException e) {
                    e.printStackTrace();
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
                e.printStackTrace();
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
            e.printStackTrace();
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

    private void pulseTask() throws StatusCodeException {
        SpecificAudioAnalysis analysis = SpecificAudioAnalysis.getAnalysis(currentTrackAnalysis,progress,100);
        pulseBeat.run(analysis);
        progress += 100;
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
                    e.printStackTrace();
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

    private void displayTrackInformation(boolean updateArt, boolean isPaused) {
        if (!settings.albumColors) {
            if (updateArt) {
                Image[] artwork = currentTrack.getAlbum().getImages();
                artworkURL = artwork[1].getUrl();
            }

            ArtistSimplified[] songArtists = currentTrack.getArtists();

            new Thread(() -> {
                pulseBeat.setPalette(palette);
                viewController.setPlayback(currentTrack.getName(), songArtists, artworkURL);
            }).start();
        } else {
            if (isPlaying) {
                if (updateArt) {
                    Image[] artwork = currentTrack.getAlbum().getImages();
                    artworkURL = artwork[1].getUrl();
                }
                ArtistSimplified[] songArtists = currentTrack.getArtists();

                new Thread(() -> {
                    try {
                        BufferedImage image = ImageIO.read(new URL(artworkURL));
                        int[][] colorArray = ColorThief.getPalette(image, 6);
                        pulseBeat.setPalette(colorArray);
                    } catch (IOException e) {
                        e.printStackTrace();
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
