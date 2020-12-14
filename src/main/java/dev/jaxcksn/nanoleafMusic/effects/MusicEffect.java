/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.effects;

import dev.jaxcksn.nanoleafMusic.utility.SpecificAudioAnalysis;
import io.github.rowak.nanoleafapi.Color;
import io.github.rowak.nanoleafapi.StatusCodeException;

public interface MusicEffect {
    void run(SpecificAudioAnalysis analysis) throws StatusCodeException;

    void setPalette(Color[] palette);

    void setPalette(int[][] palette);
}
