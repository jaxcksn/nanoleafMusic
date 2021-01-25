/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic;

import ch.qos.logback.classic.Logger;
import dev.jaxcksn.nanoleafJava.NLDevice;
import dev.jaxcksn.nanoleafJava.NLDeviceData;
import dev.jaxcksn.nanoleafMusic.musicEffect.EffectType;
import dev.jaxcksn.nanoleafMusic.utility.DataManagerException;
import dev.jaxcksn.nanoleafMusic.utility.Settings;
import dev.jaxcksn.nanoleafMusic.utility.dMEC;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * The entire purpose of this class is to manage the save data, and storing of Nanoleaf
 * information. It uses the Preferences API. I'd prefer a better approach, but this works
 * for now.
 */

public class DataManager {
    private static final Preferences preferences = Preferences.userNodeForPackage(Main.class);
    public boolean hasSaved;
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.DataManager");

    public DataManager() {
        String testForSaved = preferences.get("savedDevice", null);
        hasSaved = testForSaved != null && !testForSaved.isEmpty();
    }

    public void saveDevice(NLDevice device) {
        preferences.remove("savedDevice");
        String str = device.deviceData.hostName +
                ";" +
                device.deviceData.port +
                ";" +
                device.getAuthCode();
        preferences.put("savedDevice", str);
        logger.info("Saved {} to preferences", device.getDeviceName());
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public NLDevice loadDevice() {
        String saved = preferences.get("savedDevice", null);
        if (saved == null || saved.isEmpty() || !hasSaved) {
            logger.error("Could not load from preferences, key is null or empty.");
            throw new DataManagerException("Could not load from preferences, key is null or empty.", dMEC.NDS);
        } else {
            try {
                String[] deviceData = saved.split(";", 3);
                String hostName = deviceData[0];
                int port = Integer.parseInt(deviceData[1]);
                String accessToken = deviceData[2];
                logger.info("Loading device at {} from preferences", hostName);
                return new NLDevice(new NLDeviceData(hostName, port, ""), accessToken);

            } catch (Exception e) {
                logger.error("Could not process saved device data, string may be malformed.", e);
                throw new DataManagerException("Could not process saved device data, string may be malformed.", dMEC.MDS);
            }
        }
    }

    public JSONObject debugReconnectData() {
        JSONObject data = new JSONObject();
        String saved = preferences.get("savedDevice", null);
        if (saved == null || saved.isEmpty() || !hasSaved) {
            logger.error("Could not load from preferences, key is null or empty.");
            throw new DataManagerException("Could not load from preferences, key is null or empty.", dMEC.NDS);
        } else {
            try {
                String[] deviceData = saved.split(";", 3);
                String hostName = deviceData[0];
                int port = Integer.parseInt(deviceData[1]);
                String accessToken = deviceData[2];
                logger.info("Loading device at {} from preferences", hostName);
                data.put("hostName", hostName);
                data.put("port", port);
                data.put("accessToken", accessToken);
            } catch (Exception e) {
                logger.error("Could not process saved device data, string may be malformed.", e);
                throw new DataManagerException("Could not process saved device data, string may be malformed.", dMEC.MDS);
            }
        }


        return data;
    }

    public void removeDevice() {
        preferences.remove("savedDevice");
        logger.info("Removed saved device from preferences");
        hasSaved = false;
    }

    public static Settings loadSettings() {
        boolean albumColors = preferences.getBoolean("useAlbumColors", true);
        String colorPalette = preferences.get("colorPalette", "#FF0000,#00FF00,#0000FF");
        if (colorPalette.length() > 95) {
            colorPalette = colorPalette.substring(0, 95);
        } else if (colorPalette.length() < 23) {
            colorPalette = "#FF0000,#00FF00,#0000FF";
        }
        String effectString = preferences.get("selectedEffect", "PULSEBEAT");
        EffectType activeEffectType = EffectType.valueOf(effectString);
        logger.info("Loaded settings from preferences");
        return new Settings(albumColors, colorPalette, activeEffectType);
    }

    public static void updateSettings(Settings settings) {
        preferences.putBoolean("useAlbumColors", settings.albumColors);
        preferences.put("colorPalette", settings.colorPalette);
        preferences.put("savedEffect", settings.activeEffectType.toString());
        logger.info("Updated settings in preferences");
    }

    public static void changeAlbumMode(boolean b) {
        preferences.putBoolean("useAlbumColors", b);
        logger.info("Changed album mode to {} in preferences", b);
    }

    public static void changeEffectType(EffectType effectType) {
        preferences.put("selectedEffect", effectType.toString());
        logger.info("Changed saved effect type to {} in preferences", effectType);
    }

    public static void clearSavedData() {
        try {
            preferences.clear();
            logger.info("Cleared all data from preferences");
        } catch (BackingStoreException e) {
            Main.showException(e);
        }
    }


}
