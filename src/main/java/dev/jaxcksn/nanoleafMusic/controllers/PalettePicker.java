/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.controllers;

import dev.jaxcksn.nanoleafMusic.DataManager;
import dev.jaxcksn.nanoleafMusic.EffectManager;
import dev.jaxcksn.nanoleafMusic.utility.PaletteColor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.*;
import java.util.Optional;


public class PalettePicker {
    public ListView<PaletteColor> PaletteColorListView;
    public ObservableList<PaletteColor> ColorList;
    public Button resetBtn;
    public Button removeSelectedBtn;
    public ColorPicker colorPicker;
    public Button addBtn;
    public Button saveBtn;
    private EffectManager effectManager;

    public void initColors(EffectManager effectManager) {
        this.effectManager = effectManager;
        updatePalette();
        PaletteColorListView.setCellFactory(paletteColorListView -> new PaletteCell());
    }

    public void updatePalette() {
        ColorList = PaletteColor.toPaletteList(effectManager.settings.colorPalette);
        PaletteColorListView.setItems(ColorList);
    }

    public void addColorToPalette(ActionEvent aE) {
        Color selectedColor = colorPicker.getValue();
        if (ColorList.size() < 12) {
            ColorList.add(new PaletteColor(selectedColor));
            if (saveBtn.isDisabled() && ColorList.size() > 2) {
                saveBtn.setDisable(false);
            } else if (!saveBtn.isDisabled() && ColorList.size() < 3) {
                saveBtn.setDisable(true);
            }
            PaletteColorListView.setItems(ColorList);
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setHeaderText(null);
            alert.setContentText("You have reached the max number of colors for this palette, you must remove an existing color before you can add another one.");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            dialogPane.getStylesheets().add("/gui.css");
            alert.showAndWait();
        }
    }

    public void removeColorFromPalette(ActionEvent aE) {
        int selectedIndex = PaletteColorListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
            Toolkit.getDefaultToolkit().beep();
        } else {
            ColorList.remove(selectedIndex);
            if (saveBtn.isDisabled() && ColorList.size() > 2) {
                saveBtn.setDisable(false);
            } else if (!saveBtn.isDisabled() && ColorList.size() < 3) {
                saveBtn.setDisable(true);
            }
            PaletteColorListView.setItems(ColorList);
        }
    }

    public void resetPalette(ActionEvent aE) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Are you sure?");
        alert.setContentText("There is no way to undo this, and you will have to add all your colors again!");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        dialogPane.getStylesheets().add("/gui.css");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            saveBtn.setDisable(true);
            ColorList = FXCollections.observableArrayList();
            PaletteColorListView.setItems(ColorList);
        }
    }

    public void savePalette(ActionEvent aE) {
        //Fail silently first, and just make the change instead of bugging the user.
        if (ColorList.size() < 3) {
            for (int i = 0; i < 3 - ColorList.size(); i++) {
                ColorList.add(new PaletteColor("#FFFFFF"));
            }
        } else if (ColorList.size() > 12) {
            for (int i = 1; i <= ColorList.size() - 12; i++) {
                ColorList.remove(12 + i);
            }
        }
        //If for some reason it's still wrong:
        assert ColorList.size() < 13;
        assert ColorList.size() > 2;
        StringBuilder colorString = new StringBuilder();
        for (int i = 0; i < ColorList.size(); i++) {
            colorString.append(ColorList.get(i).hexCode);
            if (i != ColorList.size() - 1) {
                colorString.append(',');
            }
        }
        effectManager.settings.colorPalette = colorString.toString();
        new Thread(() -> DataManager.updateSettings(effectManager.settings)).start();
        effectManager.palette = PaletteColor.toEffectColorArray(colorString.toString());
        effectManager.pulseBeat.setPalette(effectManager.palette);
        ((Stage) saveBtn.getScene().getWindow()).close();
    }

}
