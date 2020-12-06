package dev.jaxcksn.nanoleafMusic.controllers;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import dev.jaxcksn.nanoleafMusic.DataManager;
import dev.jaxcksn.nanoleafMusic.EffectManager;
import dev.jaxcksn.nanoleafMusic.utility.Settings;
import io.github.rowak.nanoleafapi.Aurora;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import java.awt.*;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


public class PlaybackView {
    public Image albumArt;
    public Text trackName;
    public Text trackArtists;
    public ImageView albumArtView;
    public BorderPane mainPane;
    public BorderPane colorPane;
    public ListView<String> colorList;
    public CheckMenuItem albumColorsCheckbox;
    public MenuItem colorPaletteSelector;
    public ColorPicker colorPicker;
    public Button startButton;
    private EffectManager effectManager;
    private ObservableList<String> colorValues;


    public void initData(SpotifyApi spotifyApi, int expiresIn, Aurora device) {
        effectManager = new EffectManager(spotifyApi, expiresIn, device, this);
        startButton.setDefaultButton(true);
        effectManagerReady();
    }

    public void setPlayback(String songName, ArtistSimplified[] artists, String albumArtwork) {
        StringBuilder artistString = new StringBuilder("by ");
        for (int i = 1; i <= artists.length; i++) {
            int artistIndex = i - 1;
            if(i>1 && i != artists.length) {
                artistString.append(", ");
            } else if(i>1) {
                artistString.append(" & ");
            }

            artistString.append(artists[artistIndex].getName());
        }

        trackName.setText(songName);
        trackArtists.setText(artistString.toString());
        albumArtView.setImage(new Image(albumArtwork));
    }

    public void effectManagerReady() {

        Settings loadedSettings = effectManager.settings;
        colorValues = FXCollections.observableArrayList();
        if(loadedSettings.albumColors) {
            colorPaletteSelector.setDisable(true);
            albumColorsCheckbox.setSelected(true);
        } else {
            colorPaletteSelector.setDisable(false);
            albumColorsCheckbox.setSelected(false);
        }

        String[] savedColorPalette = loadedSettings.colorPalette.split(",", (int) Math.ceil((double) loadedSettings.colorPalette.length() /8));
        Collections.addAll(colorValues, savedColorPalette);

        albumColorsCheckbox.selectedProperty().addListener((ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
            colorPaletteSelector.setDisable(new_val);
            effectManager.settings.albumColors = new_val;
            new Thread(()->{
                DataManager.changeAlbumMode(new_val);
            }).start();
        });

        colorList.setItems(colorValues);
    }

    public void setPlayback() {
        trackName.setText("Not Playing");
        trackArtists.setText("Play music to start");
        albumArtView.setImage(new Image(String.valueOf(getClass().getResource("/images/gray-square.png"))));
    }
    
    public void showColorView(ActionEvent event) {
        mainPane.setVisible(false);
        colorPane.setVisible(true);
    };

    public void addColor(ActionEvent event) {
        Color selectedColor = colorPicker.getValue();

        String hex = toHexString(selectedColor);
        colorValues.add(hex);
        colorList.setItems(colorValues);
    }

    public void removeColor(ActionEvent event) {
        int selectedIndex = colorList.getSelectionModel().getSelectedIndex();
        if(selectedIndex == -1) {
            Toolkit.getDefaultToolkit().beep();
        } else {
            colorValues.remove(selectedIndex);
            colorList.setItems(colorValues);

        }

    }

    public void saveColors(ActionEvent event) {
        StringBuilder colorString = new StringBuilder("");
        for (int i = 0; i < colorValues.size(); i++) {
            colorString.append(colorValues.get(i));
            if (i != colorValues.size()-1) {
                colorString.append(',');
            }
        }
        effectManager.settings.colorPalette = colorString.toString();
        new Thread(()-> {
            DataManager.updateSettings(effectManager.settings);
        }).start();
        effectManager.palette = setColors(colorValues.toArray(new String[0]));
        effectManager.pulseBeat.setPalette(effectManager.palette);
        colorPane.setVisible(false);
        mainPane.setVisible(true);
    }

    private String format(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    public String toHexString(Color value) {
        return "#" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue()))
                .toUpperCase();
    }

    public static io.github.rowak.nanoleafapi.Color[] setColors(String[] colors) {
        io.github.rowak.nanoleafapi.Color[] pulseColors = new io.github.rowak.nanoleafapi.Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            String colorStr = colors[i];
            pulseColors[i] = io.github.rowak.nanoleafapi.Color.fromRGB(Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                    Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                    Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
        }

        return pulseColors;
    }

    public static io.github.rowak.nanoleafapi.Color[] setColors(String colorData) {
        String[] colors = colorData.split(",", (int) Math.ceil((double) colorData.length() /8));
        io.github.rowak.nanoleafapi.Color[] pulseColors = new io.github.rowak.nanoleafapi.Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            String colorStr = colors[i];
            pulseColors[i] = io.github.rowak.nanoleafapi.Color.fromRGB(Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                    Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                    Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
        }

        return pulseColors;
    }


    public void start(ActionEvent event) {
        if(effectManager.getIsPlaying()) {

            startButton.setVisible(false);
            effectManager.startEffect();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("There is nothing playing on Spotify, you must be playing music before starting the effect.");
            Toolkit.getDefaultToolkit().beep();
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add("/gui.css");
            alert.showAndWait();
        }
    }
}
