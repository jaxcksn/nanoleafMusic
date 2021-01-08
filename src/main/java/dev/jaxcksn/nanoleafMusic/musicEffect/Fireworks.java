/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.musicEffect;

import dev.jaxcksn.nanoleafJava.*;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class Fireworks extends MusicEffect {
    public Fireworks(NLColor[] palette, NLDevice device) {
        super(palette, device);
    }

    @Override
    public void run(SpecificAudioAnalysis analysis) {
        if (analysis.getBeat() != null && palette.length > 0) {
            NLColor color = palette[paletteIndex];
            int[] colorRGB = {color.getR(), color.getG(), color.getB()};
            int originPanelIndex = ThreadLocalRandom.current().nextInt(panels.length);
            int panelID = panels[originPanelIndex].getPanelID();
            NLPanel[] neighbors = panels[originPanelIndex].getNeighbors(panels);
            int fireworkLength = ThreadLocalRandom.current().nextInt(neighbors.length + 1);
            NLEffectBuilder ceb = new NLEffectBuilder(panels);
            NLFrame toColor = new NLFrame(new NLColor(colorRGB[0], colorRGB[1], colorRGB[2]), 1);
            NLFrame toBlack = new NLFrame(new NLColor(0, 0, 0), 5);
            if (songChanged) {
                songChanged = false;
                ceb.addFrameToAll(new NLFrame(new NLColor(0, 0, 0), 1));
            }
            ceb.addFrame(panelID, toColor);
            for (int i = 0; i < fireworkLength; i++) {
                ceb.addFrame(neighbors[i].getPanelID(), toColor);
            }
            ceb.addFrame(panelID, toBlack);
            for (int i = 0; i < fireworkLength; i++) {
                ceb.addFrame(neighbors[i].getPanelID(), toBlack);
            }
            new Thread(() -> {
                try {
                    device.effects.displayEffect(ceb.build());
                } catch (IOException e) {
                    logger.warn("Unrecoverable exception was thrown. Shutting down program.");
                    Main.showException(e);
                    System.exit(1);
                }
            }).start();

            nextColor();
        }
    }
}