/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.utility;

import dev.jaxcksn.nanoleafJava.NLColor;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

public class PaletteColor {
    public String hexCode;
    public Color jfxColor;
    public NLColor effectColor;

    //If initialized from javafx paint color
    public PaletteColor(Color javafxColor) {
        hexCode = "#" + (hexFormat(javafxColor.getRed()) + hexFormat(javafxColor.getGreen()) + hexFormat(javafxColor.getBlue()))
                .toUpperCase();
        jfxColor = javafxColor;
        effectColor = new NLColor(Integer.valueOf(hexCode.substring(1, 3), 16),
                Integer.valueOf(hexCode.substring(3, 5), 16),
                Integer.valueOf(hexCode.substring(5, 7), 16));
    }

    public PaletteColor(String hexCodeColor) {
        hexCode = hexCodeColor;
        jfxColor = Color.web(hexCodeColor);
        effectColor = new NLColor(Integer.valueOf(hexCode.substring(1, 3), 16),
                Integer.valueOf(hexCode.substring(3, 5), 16),
                Integer.valueOf(hexCode.substring(5, 7), 16));
    }

    public PaletteColor(NLColor color) {
        hexCode = "#" + (hexFormat(color.getR()) + hexFormat(color.getG()) + hexFormat(color.getB()))
                .toUpperCase();
        jfxColor = Color.web(hexCode);
        effectColor = color;
    }

    public int[] toRGB() {
        return new int[]{effectColor.getR(), effectColor.getG(), effectColor.getB()};
    }

    public static ObservableList<PaletteColor> toPaletteList(String colorValues) {
        String[] colors = colorValues.split(",", (int) Math.ceil((double) colorValues.length() / 8));
        ObservableList<PaletteColor> colorList = FXCollections.observableArrayList();
        for (String color : colors) {
            colorList.add(new PaletteColor(color));
        }
        return colorList;
    }

    public static NLColor[] toEffectColorArray(String colorValues) {
        String[] colors = colorValues.split(",", (int) Math.ceil((double) colorValues.length() / 8));
        NLColor[] pulseColors = new NLColor[colors.length];
        for (int i = 0; i < colors.length; i++) {
            String colorStr = colors[i];
            pulseColors[i] = new NLColor(Integer.valueOf(colorStr.substring(1, 3), 16),
                    Integer.valueOf(colorStr.substring(3, 5), 16),
                    Integer.valueOf(colorStr.substring(5, 7), 16));
        }
        return pulseColors;
    }

    public static NLColor[] toEffectColorArray(String[] colors) {
        NLColor[] pulseColors = new NLColor[colors.length];
        for (int i = 0; i < colors.length; i++) {
            String colorStr = colors[i];
            pulseColors[i] = new NLColor(Integer.valueOf(colorStr.substring(1, 3), 16),
                    Integer.valueOf(colorStr.substring(3, 5), 16),
                    Integer.valueOf(colorStr.substring(5, 7), 16));
        }
        return pulseColors;
    }

    public static NLColor[] toEffectColorArray(ObservableList<PaletteColor> colors) {
        return colors.stream().map(color -> color.effectColor).toArray(NLColor[]::new);
    }


    private String hexFormat(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }
}
