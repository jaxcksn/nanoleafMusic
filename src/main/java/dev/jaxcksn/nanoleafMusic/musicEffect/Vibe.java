/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.musicEffect;

import com.wrapper.spotify.model_objects.miscellaneous.AudioAnalysisMeasure;
import dev.jaxcksn.nanoleafJava.NLColor;
import dev.jaxcksn.nanoleafJava.NLDevice;
import dev.jaxcksn.nanoleafJava.NLEffectBuilder;
import dev.jaxcksn.nanoleafJava.NLFrame;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

public class Vibe extends MusicEffect {
    private AudioAnalysisMeasure currentSection;

    public Vibe(NLColor[] palette, NLDevice device) {
        super(palette, device);
    }

    @Override
    public void run(SpecificAudioAnalysis analysis) {
        if (analysis.getBeat() != null) {
            if (currentSection == null) {
                currentSection = analysis.getBar();
            } else if (currentSection != analysis.getBar()) {
                currentSection = analysis.getBar();
                nextColor();
            }
            NLColor color = palette[paletteIndex];
            int panelId = panels[ThreadLocalRandom.current().nextInt(panels.length)].getPanelID();
            int[] colorRGB = {color.getR(), color.getG(), color.getB()};
            java.awt.Color darkerColor = new java.awt.Color(colorRGB[0], colorRGB[1], colorRGB[2]).darker().darker().darker();
            NLEffectBuilder ceb = new NLEffectBuilder(device.panelLayout.getPanels());
            ceb.addFrameToAll(new NLFrame(new NLColor(darkerColor.getRed(), darkerColor.getGreen(), darkerColor.getBlue()), 2));
            ceb.addFrame(panelId, new NLFrame(new NLColor(colorRGB[0], colorRGB[1], colorRGB[2]), 1));


            new Thread(() -> {
                try {
                    device.effects.displayEffect(ceb.build());
                } catch (IOException e) {
                    logger.error("Unrecoverable exception was thrown. Shutting down program.");
                    Main.showException(e);
                    System.exit(1);
                }
            }).start();


        }
    }


}