/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.effects;

import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;
import io.github.rowak.nanoleafapi.*;
import io.github.rowak.nanoleafapi.effectbuilder.CustomEffectBuilder;

import java.util.Random;

public class FireworkEffect implements MusicEffect {
    public Color[] palette;
    public Aurora device;
    private Panel[] panels;
    private final Random random;
    public boolean albumMode = false;
    private int paletteIndex = 0;
    public final EffectType effectType = EffectType.FIREWORKS;
    public boolean songChanged = false;

    public FireworkEffect(Color[] palette, Aurora device) {
        this.palette = palette;
        this.device = device;
        try {
            this.panels = device.panelLayout().getPanels();
        } catch (StatusCodeException e) {
            e.printStackTrace();
        }
        this.random = new Random();
        System.out.println("\u001b[92;1m✔\u001b[0m Fireworks Loaded");
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
            System.out.println("\u001b[96;1mℹ\u001b[0m Changed to Album Mode");
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
            System.out.println("\u001b[96;1mℹ\u001b[0m Changed to Palette Mode");
            albumMode = false;
        }
        palette = colors;
    }


    public void run(SpecificAudioAnalysis analysis) throws StatusCodeException {
        if (analysis.getBeat() != null && palette.length > 0) {
            Color color = palette[paletteIndex];
            int[] colorRGB = {color.getRed(), color.getGreen(), color.getBlue()};
            int originPanelIndex = random.nextInt(panels.length);
            int panelID = panels[originPanelIndex].getId();
            Panel[] neighbors = panels[originPanelIndex].getNeighbors(panels);
            int fireworkLength = random.nextInt(neighbors.length + 1);
            CustomEffectBuilder ceb = new CustomEffectBuilder(device);
            Frame toColor = new Frame(colorRGB[0], colorRGB[1], colorRGB[2], 0, 1);
            Frame toBlack = new Frame(0, 0, 0, 0, 5);
            if (songChanged) {
                songChanged = false;
                ceb.addFrameToAllPanels(new Frame(0, 0, 0, 0, 1));
            }
            ceb.addFrame(panelID, toColor);
            for (int i = 0; i < fireworkLength; i++) {
                ceb.addFrame(neighbors[i].getId(), toColor);
            }
            ceb.addFrame(panelID, toBlack);
            for (int i = 0; i < fireworkLength; i++) {
                ceb.addFrame(neighbors[i].getId(), toBlack);
            }
            new Thread(() -> {
                try {
                    device.effects().displayEffect(ceb.build("", false));
                } catch (StatusCodeException e) {
                    e.printStackTrace();
                }
            }).start();

            setNextPaletteColor();
        }
    }

    protected void setNextPaletteColor() {
        if (paletteIndex == palette.length - 1) {
            paletteIndex = 0;
        } else {
            paletteIndex++;
        }
    }

    @Override
    public void setSongChanged() {
        songChanged = true;
    }
}
