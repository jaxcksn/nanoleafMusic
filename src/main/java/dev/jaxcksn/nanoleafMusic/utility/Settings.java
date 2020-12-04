package dev.jaxcksn.nanoleafMusic.utility;

public class Settings {
    public boolean albumColors;
    public int albumPaletteLength;
    public String colorPalette;

    public Settings(boolean albumColors, int albumPaletteLength, String colorPalette) {

        this.albumColors = albumColors;
        this.albumPaletteLength = albumPaletteLength;
        this.colorPalette = colorPalette;
    }
}
