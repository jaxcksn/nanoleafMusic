/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.effects;

import com.wrapper.spotify.model_objects.miscellaneous.AudioAnalysisSegment;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;
import io.github.rowak.nanoleafapi.*;
import io.github.rowak.nanoleafapi.effectbuilder.CustomEffectBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PulseBeatEffect implements MusicEffect {
    private static class SpotifyEffectUtils {
        public static float getLoudness(float previousLoudness, SpecificAudioAnalysis analysis) {
            AudioAnalysisSegment segment = analysis.getSegment();
            if (segment != null) {
                float avg = (segment.getLoudnessMax() +
                        segment.getLoudnessStart() + 0.1f) / 2f;
                return loudnessToPercent(avg, segment.getLoudnessMax());
            }
            return previousLoudness;
        }

        public static float loudnessToPercent(float loudness, float max) {
            final float MIN = -40.0f;
            if (loudness < MIN) {
                return 0f;
            } else if (loudness > max) {
                return 1f;
            }
            return (1 - loudness / MIN);
        }
    }


    private float loudness = 0.5f;
    private final Random random;
    private Color[] palette;
    private final Aurora aurora;
    private Panel[] panels;
    private int paletteIndex = 0;
    public boolean albumMode = false;
    public final EffectType effectType = EffectType.PULSEBEAT;

    public PulseBeatEffect(Color[] palette, Aurora aurora) {
        this.palette = palette;
        this.aurora = aurora;
        try {
            this.panels = aurora.panelLayout().getPanels();
        } catch (StatusCodeException e) {
            Main.showException(e);
        }
        random = new Random();
        System.out.println("\u001b[92;1m✔\u001b[0m Pulse Beat Loaded");
    }

    @Override
    public EffectType getEffectType() {
        return effectType;
    }

    @Override
    public void setSongChanged() {
        //DO NOTHING.
    }

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

    //If the palette is manually set
    public void setPalette(Color[] colors) {
        if (albumMode) {
            System.out.println("\u001b[96;1mℹ\u001b[0m Changed to Palette Mode");
            albumMode = false;
        }
        palette = colors;
    }

    public void run(SpecificAudioAnalysis analysis) throws StatusCodeException {
        loudness = SpotifyEffectUtils.getLoudness(loudness, analysis);
        if (analysis.getBeat() != null && palette.length > 0) {
            int panelIndex = random.nextInt(panels.length);
            int panelId = panels[panelIndex].getId();
            int r = palette[paletteIndex].getRed();
            int g = palette[paletteIndex].getGreen();
            int b = palette[paletteIndex].getBlue();
            java.awt.Color original = new java.awt.Color(r, g, b);
            original = applyLoudnessToColor(original);
            java.awt.Color darker = original.darker().darker().darker();
            CustomEffectBuilder ceb = new CustomEffectBuilder(aurora);
            ceb.addFrame(panelId, new Frame(original.getRed(), original.getGreen(), original.getBlue(), 0, 1));
            ceb.addFrame(panelId, new Frame(0, 0, 0, 0, 5));
            List<Integer> marked = new ArrayList<>();
            marked.add(panelId);
            final int INITIAL_TIME = 1;
            setNeighbors(panels[panelIndex], marked,
                    panels, ceb, darker, INITIAL_TIME);
            new Thread(() ->
            {
                try {
                    aurora.effects().displayEffect(ceb.build("", false));
                } catch (StatusCodeException sce) {
                    Main.showException(sce);
                }
            }).start();
            setNextPaletteColor();
        }
    }

    public void setNeighbors(Panel panel, final List<Integer> marked,
                             Panel[] panels, CustomEffectBuilder ceb, java.awt.Color color,
                             int time) {
        time += 1;
        for (Panel p : panel.getNeighbors(panels)) {
            if (!marked.contains(p.getId())) {
                ceb.addFrame(p, new Frame(color.getRed(),
                        color.getGreen(), color.getBlue(), 0, time));
                ceb.addFrame(p, new Frame(0, 0, 0, 0, 5));
                marked.add(p.getId());
                setNeighbors(p, marked, panels, ceb, color, time);
            }
        }
    }

    private java.awt.Color applyLoudnessToColor(java.awt.Color color) {
        float[] hsb = new float[3];
        hsb = java.awt.Color.RGBtoHSB(color.getRed(),
                color.getGreen(), color.getBlue(), hsb);
        hsb[2] = ((hsb[2] * 100f) * loudness) / 100f;
        color = java.awt.Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
        return color;
    }

    protected void setNextPaletteColor() {
        if (paletteIndex == palette.length - 1) {
            paletteIndex = 0;
        } else {
            paletteIndex++;
        }
    }
}