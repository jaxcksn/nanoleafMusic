package dev.jaxcksn.nanoleafMusic;

import com.github.kevinsawicki.http.HttpRequest;
import dev.jaxcksn.nanoleafMusic.utility.Settings;
import dev.jaxcksn.nanoleafMusic.utility.dMEC;
import io.github.rowak.nanoleafapi.Aurora;
import io.github.rowak.nanoleafapi.StatusCodeException;
import dev.jaxcksn.nanoleafMusic.utility.DataManagerException;

import java.util.prefs.*;

/**
 * The entire purpose of this class is to manage the save data, and storing of Nanoleaf
 * information. It uses the Preferences API. I'd prefer a better approach, but this works
 * for now.
 */

public class DataManager {
    private static final Preferences preferences = Preferences.userNodeForPackage(Main.class);
    public boolean hasSaved;

    public DataManager() {
        String testForSaved = preferences.get("savedDevice",null);
        hasSaved = testForSaved != null && !testForSaved.isEmpty();
    }

    public void saveDevice(Aurora device) {
        preferences.remove("savedDevice");
        String str = device.getHostName() +
                ";" +
                device.getPort() +
                ";" +
                device.getAccessToken();
        preferences.put("savedDevice", str);
        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public Aurora loadDevice() {
        String saved = preferences.get("savedDevice",null);
        if (saved == null || saved.isEmpty() || !hasSaved) {
            throw new DataManagerException("Could not load from preferences, key is null or empty.", dMEC.NDS);
        } else {
            try {
                String[] deviceData = saved.split(";", 3);
                String hostName = deviceData[0];
                int port = Integer.parseInt(deviceData[1]);
                String accessToken = deviceData[2];
                try {
                    return new Aurora(hostName,port,"v1",accessToken);
                } catch (StatusCodeException | HttpRequest.HttpRequestException e) {
                    throw new DataManagerException("Error creating device object from saved data.",dMEC.ISD);
                }

            } catch (Exception e) {
                throw new DataManagerException("Could not process saved device data, string may be malformed.", dMEC.MDS);
            }
        }
    }

    public void removeDevice() {
        preferences.remove("savedDevice");
        hasSaved = false;
    }

    public static Settings loadSettings() {
        boolean albumColors = preferences.getBoolean("useAlbumColors",true);
        int albumPaletteLength = preferences.getInt("numberOfAlbumColors",6);
        if(albumPaletteLength > 12) {
            albumPaletteLength = 12;
        } else if (albumPaletteLength < 3){
            albumPaletteLength = 3;
        }
        String colorPalette = preferences.get("colorPalette","#FF0000,#00FF00,#0000FF");
        if(colorPalette.length() > 95) {
            colorPalette = colorPalette.substring(0,95);
        } else if ( colorPalette.length() < 23) {
            colorPalette="#FF0000,#00FF00,#0000FF";
        }
        return new Settings(albumColors,albumPaletteLength,colorPalette);
    }

    public static void updateSettings(Settings settings) {
        preferences.putBoolean("useAlbumColors",settings.albumColors);
        preferences.put("colorPalette",settings.colorPalette);
    }

    public static void changeAlbumMode(boolean b) {
        preferences.putBoolean("useAlbumColors",b);
    }


}
