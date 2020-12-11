package dev.jaxcksn.nanoleafMusic.controllers;

import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.PaletteColor;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.IOException;

public class PaletteCell extends ListCell<PaletteColor> {
    private final PaletteCellData pcD;
    public PaletteColor cellColor;

    /**
     * A JavaFX node, meant to go inside of a ListCell. It previews a color.
     */
    private class PaletteCellData extends HBox {
        @FXML
        private Rectangle colorGraphic;
        @FXML
        private Text hexCode;

        /**
         * Creates the JavaFX Node that will go inside of the List Cell and contains methods to update the
         * data inside. Has the added bonus of keeping FXML loading to minimum.
         */
        private PaletteCellData() {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/paletteCell.fxml"));
            loader.setController(this);
            loader.setRoot(this);
            try {
                loader.load();
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        /**
         * Sets the cell to the given palette color.
         *
         * @param paletteColor The color this cell should show
         */
        public void setCellColor(PaletteColor paletteColor) {
            colorGraphic.setFill(paletteColor.jfxColor);
            hexCode.setText(paletteColor.hexCode);
        }

    }

    public PaletteCell() {
        this.pcD = new PaletteCellData();
    }

    @Override
    protected void updateItem(PaletteColor paletteColor, boolean empty) {
        super.updateItem(paletteColor, empty);
        if (empty || paletteColor == null) {
            setPrefHeight(50);
            setText(null);
            setGraphic(null);
        } else {
            pcD.setCellColor(paletteColor);
            this.cellColor = paletteColor;
            setText(null);
            setGraphic(pcD);
        }
    }


}
