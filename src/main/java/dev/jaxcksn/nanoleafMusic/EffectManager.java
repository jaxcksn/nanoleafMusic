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
import dev.jaxcksn.nanoleafMusic.utility.PulseBeat;
import dev.jaxcksn.nanoleafMusic.utility.Settings;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;
import io.github.rowak.nanoleafapi.Aurora;
import io.github.rowak.nanoleafapi.Color;
import io.github.rowak.nanoleafapi.StatusCodeException;
import org.apache.hc.core5.http.ParseException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
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
    private final CountDownLatch initLatch = new CountDownLatch(1);

    //--- Effect Variables
    private boolean isRunning, isPlaying = false;
    private Track currentTrack;
    private AudioAnalysis currentTrackAnalysis;
    private int progress;
    private String artworkURL = "@images/gray-square.png";
    public Settings settings;
    public Color[] palette = new Color[]{Color.RED,Color.BLUE,Color.GREEN};

    public EffectManager(SpotifyApi spotifyApi, int expiresIn, Aurora device, PlaybackView viewController) {
        this.spotifyApi = spotifyApi;
        this.expiresIn = expiresIn;
        this.device = device;
        this.viewController = viewController;
        this.pulseBeat = new PulseBeat(palette,device);
        settings = DataManager.loadSettings();
        startRefreshTimer();
    }

    public boolean getIsPlaying() {
        try {
            return getCurrentlyPlaying() != null;
        } catch (ParseException | SpotifyWebApiException | IOException e) {
            return false;
        }
    }

    private void startRefreshTimer() {
        ScheduledExecutorService sES = Executors.newSingleThreadScheduledExecutor();
        Runnable refreshTask = () -> {
                try {
                    AuthorizationCodePKCERefreshRequest authorizationCodePKCERefreshRequest = spotifyApi.authorizationCodePKCERefresh().build();
                    AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodePKCERefreshRequest.execute();
                    spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
                    spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
                    expiresIn = authorizationCodeCredentials.getExpiresIn();
                } catch (ParseException | SpotifyWebApiException | IOException e) {
                    e.printStackTrace();
                }
        };

        sES.scheduleAtFixedRate(refreshTask,expiresIn/2,expiresIn, TimeUnit.SECONDS);

    }

    public void startEffect() {
        if(!isRunning) {
            isRunning = true;

            //TODO: Add not playing catch.
            initEffect();
            try {
                initLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!settings.albumColors) {
                palette = PlaybackView.setColors(settings.colorPalette);
            }

            ScheduledExecutorService sES = Executors.newScheduledThreadPool(5);
            Runnable effectPulseTask = () -> {
                if(isPlaying) {
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
                } catch (ParseException | SpotifyWebApiException | IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            };

            sES.scheduleAtFixedRate(effectPulseTask,0,100,TimeUnit.MILLISECONDS);
            sES.scheduleAtFixedRate(spotifyUpdateTask,0,2000,TimeUnit.MILLISECONDS);
        }
    }

    private void initEffect() {
        try {
            CurrentlyPlaying currentlyPlaying = getCurrentlyPlaying();
            while (currentlyPlaying == null) {
                TimeUnit.SECONDS.sleep(5);
                currentlyPlaying = getCurrentlyPlaying();
            }
            initLatch.countDown();
            currentTrack = ((Track) currentlyPlaying.getItem());
            currentTrackAnalysis = getTrackAnalysis(currentTrack.getId());
            progress = currentlyPlaying.getProgress_ms();
            isPlaying = true;
            displayTrackInformation(true);

        } catch (ParseException | SpotifyWebApiException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
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
            displayTrackInformation(false);
            CountDownLatch playLatch = new CountDownLatch(1);
            new Thread(() -> {
                try {
                    CurrentlyPlaying current = getCurrentlyPlaying();
                    while (current == null) {
                        TimeUnit.SECONDS.sleep(4);
                        current = getCurrentlyPlaying();
                    }
                    playLatch.countDown();
                } catch (ParseException | IOException | SpotifyWebApiException | InterruptedException e) {
                    e.printStackTrace();
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
            displayTrackInformation(true);
        }

        float progressDifference = Math.abs(currentPlayback.getProgress_ms() - progress);
        if(currentPlayback.getIs_playing() && !isPlaying) {
            isPlaying = true;
            progress = currentPlayback.getProgress_ms()+500;
            displayTrackInformation(true);
        } else if(!currentPlayback.getIs_playing() && isPlaying) {
            isPlaying = false;
            progress = currentPlayback.getProgress_ms();
            displayTrackInformation(false);
        } else if (currentPlayback.getIs_playing() && progressDifference >= 10) {
            progress = currentPlayback.getProgress_ms();
        }


    }

    private void displayTrackInformation(boolean updateArt) {
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
                viewController.setPlayback();
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
