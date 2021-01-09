/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.controllers;

import ch.qos.logback.classic.Logger;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import dev.jaxcksn.nanoleafJava.NLDevice;
import dev.jaxcksn.nanoleafMusic.DataManager;
import dev.jaxcksn.nanoleafMusic.EffectManager;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.musicEffect.EffectType;
import dev.jaxcksn.nanoleafMusic.utility.Settings;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class PlaybackView {
    public Text trackName;
    public Text trackArtists;
    public BorderPane mainPane;
    public CheckMenuItem albumColorsCheckbox;
    public MenuItem colorPaletteSelector;
    public MenuItem reloadEffectItem;
    public Rectangle trackArtFrame;
    public MenuButton menuButton;
    public MenuItem aboutMenuItem;
    public ToggleGroup effectRadio;
    public AnchorPane loadingPane;
    public RadioMenuItem FireworksToggle;
    public RadioMenuItem PulseBeatToggle;
    public Label EffectLabel;
    public RadioMenuItem VibeToggle;
    private EffectManager effectManager;
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.PlaybackView");
    private Scene palettePickerScene;

    public void initData(SpotifyApi spotifyApi, int expiresIn, NLDevice device) {
        logger.info("Initializing the playback view");
        PulseBeatToggle.setUserData(EffectType.PULSEBEAT);
        FireworksToggle.setUserData(EffectType.FIREWORKS);
        VibeToggle.setUserData(EffectType.VIBE);

        effectManager = new EffectManager(spotifyApi, expiresIn, device, this);
        Settings loadedSettings = effectManager.settings;
        logger.info("Album mode set to {}", loadedSettings.albumColors);
        if (loadedSettings.albumColors) {
            colorPaletteSelector.setDisable(true);
            albumColorsCheckbox.setSelected(true);

        } else {
            colorPaletteSelector.setDisable(false);
            albumColorsCheckbox.setSelected(false);
        }

        switch (effectManager.settings.activeEffectType) {
            case PULSEBEAT:
                PulseBeatToggle.setSelected(true);
                FireworksToggle.setSelected(false);
                VibeToggle.setSelected(false);
                EffectLabel.setText("PULSEBEAT");
                break;
            case FIREWORKS:
                PulseBeatToggle.setSelected(false);
                FireworksToggle.setSelected(true);
                VibeToggle.setSelected(false);
                EffectLabel.setText("FIREWORKS");
                break;
            case VIBE:
                PulseBeatToggle.setSelected(false);
                FireworksToggle.setSelected(false);
                VibeToggle.setSelected(true);
                EffectLabel.setText("VIBE");
                break;
        }

        effectRadio.selectedToggleProperty().addListener((ObservableValue<? extends Toggle> ov,
                                                          Toggle old_toggle, Toggle new_toggle) -> {
            old_toggle.setSelected(false);
            new_toggle.setSelected(true);
            changeEffect((EffectType) new_toggle.getUserData());
        });

        albumColorsCheckbox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            colorPaletteSelector.setDisable(new_val);
            effectManager.settings.albumColors = new_val;
            if (new_val) {
                effectManager.displayTrackInformation(true, false);
            }

            Thread setAlbumColorData = new Thread(() -> DataManager.changeAlbumMode(new_val));
            setAlbumColorData.setName("data");
            setAlbumColorData.start();
        });

        Thread startEffectThread = new Thread(() -> {
            effectManager.startEffect();
        });
        startEffectThread.setName("effect-start");
        startEffectThread.start();

        FXMLLoader palettePickerLoader = new FXMLLoader(dev.jaxcksn.nanoleafMusic.Main.class.getResource("/palettePicker.fxml"));
        try {
            Parent palettePickerRoot = palettePickerLoader.load();
            PalettePicker palettePicker = palettePickerLoader.getController();
            palettePicker.initColors(effectManager);
            palettePicker.updatePalette();
            palettePickerScene = new Scene(palettePickerRoot, 400, 300);
            palettePickerScene.getStylesheets().add("/gui.css");
            logger.info("Palette picker is ready");
        } catch (IOException e) {
            Main.showException(e);
        }
    }

    public void setPlayback(String songName, ArtistSimplified[] artists, String albumArtwork) {
        trackArtFrame.setFill(new ImagePattern(new Image(albumArtwork)));
        StringBuilder artistString = new StringBuilder("by ");
        for (int i = 1; i <= artists.length; i++) {
            int artistIndex = i - 1;
            if (i > 1 && i != artists.length) {
                artistString.append(", ");
            } else if (i > 1) {
                artistString.append(" & ");
            }
            artistString.append(artists[artistIndex].getName());
            //Parent root = mainPane.getScene().getRoot();
            //root.setStyle("-fx-playback-accent:  #0FD95F;");
        }

        trackName.setText(songName);
        trackArtists.setText(artistString.toString());
    }

    public void setPlayback(boolean paused) {
        trackArtFrame.setFill(Color.web("#b5b5b5"));
        if (!paused) {
            trackName.setText("Not Playing");
            trackArtists.setText("Play music to start the effect");
        } else {
            trackName.setText("Effect Paused");
            trackArtists.setText("Resume playback to start the effect");
        }
    }

    public void showColorView(ActionEvent event) {
        Stage stage = new Stage();
        stage.setScene(palettePickerScene);
        stage.show();
    }

    public void reloadEffectManager(ActionEvent event) {
        setLoading(true);
        effectManager.reloadEffect();
        setLoading(false);
    }

    private void setLoading(boolean status) {
        mainPane.setDisable(status);
        loadingPane.setVisible(status);
    }

    public void changeEffect(EffectType effectType) {
        setLoading(true);
        new Thread(() -> {
            effectManager.switchEffect(effectType);
            if (effectManager.isPlaying) {
                effectManager.displayTrackInformation(true, false);
            }
            setLoading(false);
        }).start();

        Thread changeEffectThread = new Thread(() -> {
            DataManager.changeEffectType(effectType);
        });
        changeEffectThread.setName("data");
        changeEffectThread.start();
        EffectLabel.setText(effectType.toString());
    }

    public void showAbout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("About this Program");
        alert.setContentText("NanoleafMusic v1.2-beta\nCopyright (c) 2021, Jaxcksn.\nAll rights reserved.");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        dialogPane.getStylesheets().add("/gui.css");
        alert.showAndWait();
    }


}
