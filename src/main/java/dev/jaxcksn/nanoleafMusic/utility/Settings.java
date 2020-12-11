/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.utility;

public class Settings {
    public boolean albumColors;
    /**
     * This is a debug setting, and should only be changed manually in preferences.
     * Adjusting can cause performance issues, so do it at your own risk.
     */
    public int albumPaletteLength;
    public String colorPalette;

    public Settings(boolean albumColors, int albumPaletteLength, String colorPalette) {

        this.albumColors = albumColors;
        this.albumPaletteLength = albumPaletteLength;
        this.colorPalette = colorPalette;
    }
}
