/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.effects;

import ch.qos.logback.classic.Logger;
import com.wrapper.spotify.model_objects.miscellaneous.AudioAnalysisMeasure;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;
import io.github.rowak.nanoleafapi.*;
import io.github.rowak.nanoleafapi.effectbuilder.CustomEffectBuilder;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * More minimal beat effect, where the colors fade from one to another, and changes in the
 * parts of the song changes the color.
 */
public class VibeEffect implements MusicEffect {
    public Color[] palette;
    public Aurora device;
    private Panel[] panels;
    private final Random random;
    public boolean albumMode = false;
    private int paletteIndex = 0;
    public final EffectType effectType = EffectType.VIBE;
    public boolean songChanged = false;
    private AudioAnalysisMeasure currentSection;
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.MusicEffect");

    public VibeEffect(Color[] palette, Aurora device) {
        this.palette = palette;
        this.device = device;
        try {
            this.panels = device.panelLayout().getPanels();
        } catch (StatusCodeException e) {
            Main.showException(e);
        }
        this.random = new Random();
        paletteIndex = random.nextInt(palette.length);
        logger.info("Vibe effect was loaded");
    }


    @Override
    public void setSongChanged() {
        songChanged = true;
    }

    @Override
    public void run(SpecificAudioAnalysis analysis) throws StatusCodeException {
        if (analysis.getBeat() != null) {
            if (currentSection == null) {
                currentSection = analysis.getBar();
            } else if (currentSection != analysis.getBar()) {
                currentSection = analysis.getBar();
                setNextPaletteColor();
            }
            Color color = palette[paletteIndex];
            int panelId = panels[random.nextInt(panels.length)].getId();
            int[] colorRGB = {color.getRed(), color.getGreen(), color.getBlue()};
            java.awt.Color darkerColor = new java.awt.Color(colorRGB[0], colorRGB[1], colorRGB[2]).darker().darker().darker();
            CustomEffectBuilder ceb = new CustomEffectBuilder(device);
            ceb.addFrameToAllPanels(new Frame(darkerColor.getRed(),
                    darkerColor.getGreen(), darkerColor.getBlue(), 0, 2));
            ceb.addFrame(panelId, new Frame(colorRGB[0], colorRGB[1], colorRGB[2], 0, 1));


            new Thread(() -> {
                try {
                    device.effects().displayEffect(ceb.build("", false));
                } catch (StatusCodeException e) {
                    logger.error("Unrecoverable exception was thrown. Shutting down program.");
                    Main.showException(e);
                    System.exit(1);
                }
            }).start();


        }
    }

    @Override
    public Color[] getPalette() {
        return palette;
    }

    private int[] adjustLuma(int[] color, double brightness) {
        float[] hsbColor = java.awt.Color.RGBtoHSB(color[0], color[1], color[2], null);
        float newBrightness = (float) (brightness * 100 + random.nextInt(10)) / 100;
        hsbColor[2] = newBrightness;

        int rgb = java.awt.Color.HSBtoRGB(hsbColor[0], hsbColor[1], hsbColor[2]);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        double luma = 0.2126 * r + 0.7152 * g + 0.0722 * b;

        if (luma < 65 && brightness < 1) {
            //if it's still too dark.
            return adjustLuma(new int[]{r, g, b}, brightness + 0.1);
        } else {
            return new int[]{r, g, b};
        }
    }


    public void setPalette(int[][] colors) {
        if (!albumMode) {
            albumMode = true;
        }
        Color[] newPalette = new Color[colors.length];


        for (int i = 0; i < colors.length; i++) {
            int r = colors[i][0];
            int g = colors[i][1];
            int b = colors[i][2];

            // This mainly to stop colors that are just black, as they kind of ruin the effect.
            double luma = 0.2126 * r + 0.7152 * g + 0.0722 * b;
            if (luma < 65) {
                //Let's lighten it.
                int[] bright = adjustLuma(colors[i], 0.5);
                newPalette[i] = Color.fromRGB(bright[0], bright[1], bright[2]);
            } else {
                newPalette[i] = Color.fromRGB(r, g, b);
            }

        }

        palette = newPalette;
    }

    public EffectType getEffectType() {
        return effectType;
    }

    //If the palette is manually set
    public void setPalette(Color[] colors) {
        if (albumMode) {
            albumMode = false;
        }
        palette = colors;
    }

    protected void setNextPaletteColor() {
        if (paletteIndex == palette.length - 1) {
            paletteIndex = 0;
        } else {
            paletteIndex++;
        }
    }

}
