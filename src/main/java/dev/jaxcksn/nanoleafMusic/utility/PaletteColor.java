/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.utility;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

public class PaletteColor {
    public String hexCode;
    public Color jfxColor;
    public io.github.rowak.nanoleafapi.Color effectColor;

    //If initialized from javafx paint color
    public PaletteColor(Color javafxColor) {
        hexCode = "#" + (hexFormat(javafxColor.getRed()) + hexFormat(javafxColor.getGreen()) + hexFormat(javafxColor.getBlue()))
                .toUpperCase();
        jfxColor = javafxColor;
        effectColor = io.github.rowak.nanoleafapi.Color.fromRGB(Integer.valueOf(hexCode.substring(1, 3), 16),
                Integer.valueOf(hexCode.substring(3, 5), 16),
                Integer.valueOf(hexCode.substring(5, 7), 16));
    }

    public PaletteColor(String hexCodeColor) {
        hexCode = hexCodeColor;
        jfxColor = Color.web(hexCodeColor);
        effectColor = io.github.rowak.nanoleafapi.Color.fromRGB(Integer.valueOf(hexCode.substring(1, 3), 16),
                Integer.valueOf(hexCode.substring(3, 5), 16),
                Integer.valueOf(hexCode.substring(5, 7), 16));
    }

    public static ObservableList<PaletteColor> toPaletteList(String colorValues) {
        String[] colors = colorValues.split(",", (int) Math.ceil((double) colorValues.length() / 8));
        ObservableList<PaletteColor> colorList = FXCollections.observableArrayList();
        for (String color : colors) {
            colorList.add(new PaletteColor(color));
        }
        return colorList;
    }

    public static io.github.rowak.nanoleafapi.Color[] toEffectColorArray(String colorValues) {
        String[] colors = colorValues.split(",", (int) Math.ceil((double) colorValues.length() / 8));
        io.github.rowak.nanoleafapi.Color[] pulseColors = new io.github.rowak.nanoleafapi.Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            String colorStr = colors[i];
            pulseColors[i] = io.github.rowak.nanoleafapi.Color.fromRGB(Integer.valueOf(colorStr.substring(1, 3), 16),
                    Integer.valueOf(colorStr.substring(3, 5), 16),
                    Integer.valueOf(colorStr.substring(5, 7), 16));
        }
        return pulseColors;
    }

    public static io.github.rowak.nanoleafapi.Color[] toEffectColorArray(String[] colors) {
        io.github.rowak.nanoleafapi.Color[] pulseColors = new io.github.rowak.nanoleafapi.Color[colors.length];
        for (int i = 0; i < colors.length; i++) {
            String colorStr = colors[i];
            pulseColors[i] = io.github.rowak.nanoleafapi.Color.fromRGB(Integer.valueOf(colorStr.substring(1, 3), 16),
                    Integer.valueOf(colorStr.substring(3, 5), 16),
                    Integer.valueOf(colorStr.substring(5, 7), 16));
        }
        return pulseColors;
    }

    public static io.github.rowak.nanoleafapi.Color[] toEffectColorArray(ObservableList<PaletteColor> colors) {
        return colors.stream().map(color -> color.effectColor).toArray(io.github.rowak.nanoleafapi.Color[]::new);
    }


    private String hexFormat(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }
}
