/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.musicEffect;


import ch.qos.logback.classic.Logger;
import dev.jaxcksn.nanoleafJava.NLColor;
import dev.jaxcksn.nanoleafJava.NLDevice;
import dev.jaxcksn.nanoleafJava.NLPanel;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * An abstract class for all music effects, including custom ones.
 *
 * @author Jaxcksn
 */
public abstract class MusicEffect {
    /**
     * The color palette that the effect is using
     */
    protected NLColor[] palette;
    /**
     * The device the effect is controlling
     */
    protected final NLDevice device;
    /**
     * The panels of the device
     */
    protected NLPanel[] panels;
    /**
     * The index of the effect within the palette
     */
    protected int paletteIndex = 0;
    /**
     * If the effect is using album colors or not
     */
    protected boolean albumMode;
    /**
     * The index of the last panel an effect was applied too
     */
    protected int lastPanel = -1;
    /**
     * Has the song changed?
     */
    protected boolean songChanged;
    protected static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.MusicEffect");


    public MusicEffect(NLColor[] palette, NLDevice device) {
        this.palette = palette;
        this.device = device;
        this.panels = device.panelLayout.getPanels();
    }

    public NLColor[] getPalette() {
        return palette;
    }

    /**
     * Adjusts the Luma of a given color until the brightness is at a satisfactory level.
     *
     * @param color      An RGB array of integers representing a color.
     * @param brightness The brightness of the current color.
     * @return An RGB array of the adjusted color
     */
    protected int[] adjustLuma(int[] color, double brightness) {
        float[] hsbColor = new NLColor(color).getHSB();
        float newBrightness = (float) (brightness * 100 + ThreadLocalRandom.current().nextInt(10));
        hsbColor[2] = newBrightness;

        int[] rgb = hsvToRgb(hsbColor[0], hsbColor[1], hsbColor[2]);
        double luma = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];


        if (luma < 65 && brightness < 1) {
            return adjustLuma(new int[]{rgb[0], rgb[1], rgb[2]}, brightness + 0.1);
        } else {
            return new int[]{rgb[0], rgb[1], rgb[2]};
        }
    }

    protected static int[] hsvToRgb(float H, float S, float V) {
        float R, G, B;

        H /= 360f;
        S /= 100f;
        V /= 100f;

        if (S == 0) {
            R = V * 255;
            G = V * 255;
            B = V * 255;
        } else {
            float var_h = H * 6;
            if (var_h == 6)
                var_h = 0; // H must be < 1
            int var_i = (int) Math.floor(var_h); // Or ... var_i =
            // floor( var_h )
            float var_1 = V * (1 - S);
            float var_2 = V * (1 - S * (var_h - var_i));
            float var_3 = V * (1 - S * (1 - (var_h - var_i)));

            float var_r;
            float var_g;
            float var_b;
            if (var_i == 0) {
                var_r = V;
                var_g = var_3;
                var_b = var_1;
            } else if (var_i == 1) {
                var_r = var_2;
                var_g = V;
                var_b = var_1;
            } else if (var_i == 2) {
                var_r = var_1;
                var_g = V;
                var_b = var_3;
            } else if (var_i == 3) {
                var_r = var_1;
                var_g = var_2;
                var_b = V;
            } else if (var_i == 4) {
                var_r = var_3;
                var_g = var_1;
                var_b = V;
            } else {
                var_r = V;
                var_g = var_1;
                var_b = var_2;
            }

            R = var_r * 255; // RGB results from 0 to 255
            G = var_g * 255;
            B = var_b * 255;
        }

        return new int[]{(int) R, (int) G, (int) B};
    }


    /**
     * Sets the color palette to the given colors and adjusts them for appropriate luma values.
     *
     * @param colors A multidimensional array of RGB integer arrays from Color Thief
     */
    public void setPalette(int[][] colors) {
        if (!albumMode) {
            albumMode = true;
        }
        NLColor[] newPalette = new NLColor[colors.length];


        for (int i = 0; i < colors.length; i++) {
            int r = colors[i][0];
            int g = colors[i][1];
            int b = colors[i][2];

            // This mainly to stop colors that are just black, as they kind of ruin the effect.
            double luma = 0.2126 * r + 0.7152 * g + 0.0722 * b;
            if (luma < 65) {
                //Let's lighten it.
                int[] bright = adjustLuma(colors[i], 0.5);
                newPalette[i] = new NLColor(bright[0], bright[1], bright[2]);
                //logger.info("Brightening: "+ Arrays.toString(colors[i])+" to "+Arrays.toString(newPalette[i].getRGB()));
            } else {
                newPalette[i] = new NLColor(r, g, b);
            }

        }
        palette = newPalette;
    }

    /**
     * Manually sets the color palette to the supplied array, without adjusting luma.
     *
     * @param colors An array of Aurora Colors to directly set as the color palette
     */
    public void setPalette(NLColor[] colors) {
        if (albumMode) {
            albumMode = false;
        }
        palette = colors;
    }

    public abstract void run(SpecificAudioAnalysis analysis);

    //Custom Effect Methods

    /**
     * Advance forward the current position within the color palette.
     *
     * @param excludeFirst Should the first color be excluded when advancing?
     * @param excludeLast  Should the last color be excluded when advancing?
     * @action 0
     */
    @Action
    protected void nextColor(boolean excludeFirst, boolean excludeLast) {
        if (excludeLast) {
            if (paletteIndex == palette.length - 2) {
                if (excludeFirst) {
                    paletteIndex = 1;
                } else {
                    paletteIndex = 0;
                }

            } else {
                paletteIndex++;
            }
        } else {
            if (paletteIndex == palette.length - 1) {
                if (excludeFirst) {
                    paletteIndex = 1;
                } else {
                    paletteIndex = 0;
                }

            } else {
                paletteIndex++;
            }
        }
    }

    /**
     * Advance forward the current position within the color palette.
     *
     * @action 0
     */
    @Action
    protected void nextColor() {
        nextColor(false, false);
    }


    public void setSongChanged() {
        songChanged = true;
    }
}