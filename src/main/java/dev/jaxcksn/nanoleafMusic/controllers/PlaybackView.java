/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.controllers;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import dev.jaxcksn.nanoleafMusic.DataManager;
import dev.jaxcksn.nanoleafMusic.EffectManager;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.Settings;
import io.github.rowak.nanoleafapi.Aurora;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

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
    private EffectManager effectManager;

    private Scene palettePickerScene;

    public void initData(SpotifyApi spotifyApi, int expiresIn, Aurora device) {
        effectManager = new EffectManager(spotifyApi, expiresIn, device, this);
        Settings loadedSettings = effectManager.settings;
        if (loadedSettings.albumColors) {
            colorPaletteSelector.setDisable(true);
            albumColorsCheckbox.setSelected(true);
        } else {
            colorPaletteSelector.setDisable(false);
            albumColorsCheckbox.setSelected(false);
        }

        albumColorsCheckbox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            colorPaletteSelector.setDisable(new_val);
            effectManager.settings.albumColors = new_val;
            new Thread(() -> DataManager.changeAlbumMode(new_val)).start();
        });

        new Thread(() -> {
            effectManager.startEffect();
        }).start();

        FXMLLoader palettePickerLoader = new FXMLLoader(Main.class.getResource("/palettePicker.fxml"));
        try {
            Parent palettePickerRoot = palettePickerLoader.load();
            PalettePicker palettePicker = palettePickerLoader.getController();
            palettePicker.initColors(effectManager);
            palettePicker.updatePalette();
            palettePickerScene = new Scene(palettePickerRoot, 400, 300);
            palettePickerScene.getStylesheets().add("/gui.css");
        } catch (IOException e) {
            e.printStackTrace();
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
        effectManager.reloadEffect();
    }

    public void showAbout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("About this Program");
        alert.setContentText("NanoleafMusic v1.0.0 \nCopyright (c) 2020, Jaxcksn. All rights reserved.");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        dialogPane.getStylesheets().add("/gui.css");
        alert.showAndWait();
    }
}
