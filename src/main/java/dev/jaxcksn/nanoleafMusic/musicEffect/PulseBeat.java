/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.musicEffect;

import com.wrapper.spotify.model_objects.miscellaneous.AudioAnalysisSegment;
import dev.jaxcksn.nanoleafJava.*;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PulseBeat extends MusicEffect {
    private float loudness = 0.5f;

    public PulseBeat(NLColor[] palette, NLDevice device) {
        super(palette, device);
    }

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

    @Override
    public void run(SpecificAudioAnalysis analysis) {
        loudness = SpotifyEffectUtils.getLoudness(loudness, analysis);
        if (analysis.getBeat() != null && palette.length > 0) {
            int panelIndex = ThreadLocalRandom.current().nextInt(panels.length);
            int panelId = panels[panelIndex].getPanelID();
            int r = palette[paletteIndex].getR();
            int g = palette[paletteIndex].getG();
            int b = palette[paletteIndex].getB();

            Color newOriginal = new java.awt.Color(r, g, b);
            newOriginal = applyLoudnessToColor(newOriginal);
            NLColor original = new NLColor(newOriginal.getRed(), newOriginal.getGreen(), newOriginal.getBlue());
            Color newDarker = newOriginal.darker().darker();
            NLColor darker = new NLColor(newDarker.getRed(), newDarker.getGreen(), newDarker.getBlue());
            NLEffectBuilder ceb = new NLEffectBuilder(panels);
            ceb.addFrame(panelId, new NLFrame(original, 1));
            ceb.addFrame(panelId, new NLFrame(new NLColor(0, 0, 0), 5));
            List<Integer> marked = new ArrayList<>();
            marked.add(panelId);
            final int INITIAL_TIME = 1;
            setNeighbors(panels[panelIndex], marked,
                    panels, ceb, darker, INITIAL_TIME);
            new Thread(() ->
            {
                try {
                    device.effects.displayEffect(ceb.build());
                } catch (IOException sce) {
                    logger.warn("Unrecoverable exception was thrown. Shutting down program.");
                    Main.showException(sce);
                    System.exit(1);
                }
            }).start();
            nextColor();
        }
    }


    public void setNeighbors(NLPanel panel, final List<Integer> marked,
                             NLPanel[] panels, NLEffectBuilder ceb, NLColor color,
                             int time) {
        time += 1;
        for (NLPanel p : panel.getNeighbors(panels)) {
            if (!marked.contains(p.getPanelID())) {
                ceb.addFrame(p, new NLFrame(color, time));
                ceb.addFrame(p, new NLFrame(new NLColor(0, 0, 0), 5));
                marked.add(p.getPanelID());
                setNeighbors(p, marked, panels, ceb, color, time);
            }
        }
    }

    private NLColor applyLoudnessToColor(NLColor color) {
        float[] hsb;
        hsb = color.getHSB();
        hsb[2] = ((hsb[2] * 100f) * loudness) / 100f;
        color = new NLColor(hsvToRgb(hsb[0], hsb[1], hsb[2]));
        return color;
    }

    private java.awt.Color applyLoudnessToColor(java.awt.Color color) {
        float[] hsb = new float[3];
        hsb = java.awt.Color.RGBtoHSB(color.getRed(),
                color.getGreen(), color.getBlue(), hsb);
        hsb[2] = ((hsb[2] * 100f) * loudness) / 100f;
        color = java.awt.Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
        return color;
    }
}