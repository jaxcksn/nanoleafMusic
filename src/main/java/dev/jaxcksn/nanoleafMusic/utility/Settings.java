/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.utility;

import dev.jaxcksn.nanoleafMusic.effects.EffectType;

public class Settings {
    public boolean albumColors;
    public String colorPalette;
    public EffectType activeEffectType;

    public Settings(boolean albumColors, String colorPalette, EffectType activeEffectType) {
        this.activeEffectType = activeEffectType;
        this.albumColors = albumColors;
        this.colorPalette = colorPalette;
    }
}
