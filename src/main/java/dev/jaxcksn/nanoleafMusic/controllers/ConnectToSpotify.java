/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.controllers;

import ch.qos.logback.classic.Logger;
import dev.jaxcksn.nanoleafJava.NLDevice;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.SpotifyManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;

public class ConnectToSpotify {
    private NLDevice device;
    private SpotifyManager spotifyManager;
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.ConnectToSpotify");
    @FXML
    private BorderPane borderPane;
    @FXML
    private AnchorPane loadingPane;

    public void initialize() {
        spotifyManager = new SpotifyManager();
    }

    public void initData(NLDevice device) {
        this.device = device;
    }

    public void startConnection(javafx.event.ActionEvent actionEvent) {
        setLoading(true);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(spotifyManager.connectURI);
                logger.info("Opened authentication URL in browser");
                String accessCode = spotifyManager.cbServer.getAuthCode();
                spotifyManager.getCredentials(accessCode);
                logger.info("Successfully created Spotify API credentials from accessCode");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Spotify Connected");
                alert.setContentText("Spotify was successfully connected.");
                DialogPane dialogPane = alert.getDialogPane();
                dialogPane.setMinHeight(Region.USE_PREF_SIZE);
                dialogPane.getStylesheets().add("/gui.css");
                alert.showAndWait();
                spotifyManager.cbServer.destroy();
                transitionToPlayer();
            } catch (IOException e) {
                dev.jaxcksn.nanoleafMusic.Main.showException(e);
            }
        }
    }

    private void setLoading(boolean b) {
        borderPane.setDisable(b);
        loadingPane.setVisible(b);
    }

    private void transitionToPlayer() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/playbackView.fxml"));
            Parent root = loader.load();
            PlaybackView playbackView = loader.getController();
            playbackView.initData(spotifyManager.spotifyApi,spotifyManager.expiresIn,device);
            Stage stage = (Stage) borderPane.getScene().getWindow();
            Scene scene = new Scene(root, 400, 300);
            scene.getStylesheets().add("/gui.css");
            logger.info("Setting JavaFX scene to 'PlaybackView' view");
            stage.setScene(scene);

        } catch (IOException e) {
            Main.showException(e);
        }
    }

}
