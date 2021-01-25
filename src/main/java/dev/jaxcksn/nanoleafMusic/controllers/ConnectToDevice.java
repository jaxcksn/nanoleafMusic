/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.controllers;

import ch.qos.logback.classic.Logger;
import dev.jaxcksn.NLJava;
import dev.jaxcksn.nanoleafJava.NLDevice;
import dev.jaxcksn.nanoleafJava.NLDeviceData;
import dev.jaxcksn.nanoleafJava.NLDeviceModel;
import dev.jaxcksn.nanoleafJava.NLException;
import dev.jaxcksn.nanoleafMusic.DataManager;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.DataManagerException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.BufferUnderflowException;
import java.util.List;
import java.util.Optional;

public class ConnectToDevice {
    public Button debugButton;
    @FXML
    private Button reconnectBtn;
    @FXML
    private ListView<String> nanoleafList;
    @FXML
    private BorderPane selectPane;
    @FXML
    private AnchorPane loadingPane;

    private ObservableList<String> deviceList;
    private List<NLDeviceData> devicesList;
    private DataManager dataManager;
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.ConnectToDevice");

    public void initialize() {
        dataManager = new DataManager();
        if (!dataManager.hasSaved) {
            reconnectBtn.setDisable(true);
            debugButton.setDisable(true);
        }

        deviceList = FXCollections.observableArrayList();
        try {
            Thread refreshThread = new Thread(() -> {
                findDevices();
                nanoleafList.setItems(deviceList);
            });
            refreshThread.setName("initial-mDNS");
            refreshThread.start();
        } catch (BufferUnderflowException ignored) {

        }
    }



    private void findDevices() {
        setLoading(true);
        List<NLDeviceData> devices = null;
        devices = NLJava.discoverDevices();
        logger.info("Found {} devices from mDNS query", devices.size());
        devicesList = devices;
        for (NLDeviceData device : devices) {
            deviceList.add(device.deviceName);
        }
        setLoading(false);
    }

    public void reconnectToDevice(ActionEvent actionEvent) {
        //The button shouldn't be enabled if this is false.
        assert dataManager.hasSaved;
        try {
            NLDevice savedDevice = dataManager.loadDevice();
            Alert alert;


            if (!Main.isMac() && savedDevice.deviceModel.equals(NLDeviceModel.CANVAS)) {
                alert = new Alert(Alert.AlertType.WARNING);
                alert.setHeaderText("Reconnected to Canvas device.");
                alert.setContentText("No errors were thrown while reconnecting. But there may be an issue with reconnecting to Canvas devices on Windows.\n" +
                        "\nIf you have issues reconnecting, try pressing the \"Clear Saved\" button and then trying to reconnect manually may fix this.");
                alert.getButtonTypes().add(new ButtonType("HELP", ButtonBar.ButtonData.NO));
            } else {
                alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Connected to " + savedDevice.getDeviceName());
                alert.setContentText("Successfully reconnected to saved device.");
            }

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add("/gui.css");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get().getText().equals("HELP")) {
                Desktop.getDesktop().browse(new URI("https://github.com/jaxcksn/nanoleafMusic/issues/14"));
            }
            logger.info("Successfully connected to {}", savedDevice.getDeviceName());
            transitionToSpotify(savedDevice);
        } catch (DataManagerException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            String alertContent = "This is awkward, this shouldn't happen.";
            String alertHeader = "Unknown Error";
            switch (e.code) {
                case NDS:
                    //this shouldn't ever be thrown here, but just in case.
                    alertHeader = "No Data Saved";
                    alertContent = "There is no device saved to reconnect to, please connect to a device on the list.";
                    reconnectBtn.setDisable(true);
                    break;
                case MDS:
                    alertHeader = "Malformed Data String";
                    alertContent = "The data of the saved device is unreadable or malformed, please connect to a device on the list.";
                    dataManager.removeDevice();
                    reconnectBtn.setDisable(true);
                    break;
                case ISD:
                    alertHeader = "Invalid Saved Data";
                    alertContent = "We couldn't connect to the saved device, please make sure the device is online. If you get this error again, try connecting to it from the list.";
                    break;
            }
            alert.setHeaderText(alertHeader);
            alert.setContentText(alertContent);
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add("/gui.css");
            alert.showAndWait();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void connectToSelected(ActionEvent actionEvent) {
        if (nanoleafList.getSelectionModel().getSelectedIndex() != -1) {
            setLoading(true);

            int auroraIndex = nanoleafList.getSelectionModel().getSelectedIndex();
            NLDeviceData selectedDevice = devicesList.get(auroraIndex);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Connect to Device");
            alert.setContentText("To connect, press and hold the power button on the device controller for 5 to 10 seconds until the LED indicator starts flashing white. Then press OK to attempt to connect.");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add("/gui.css");
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() &&result.get() == ButtonType.OK) {
                getAccessToken(selectedDevice);
            } else {
                // ... user chose CANCEL or closed the dialog
                System.exit(0);
            }
        } else {
            /* do nothing. */
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void setLoading(boolean b) {
        selectPane.setDisable(b);
        loadingPane.setVisible(b);
    }

    public void refreshList(ActionEvent actionEvent) {

        deviceList = FXCollections.observableArrayList();
        try {
            Thread refreshThread = new Thread(() -> {

                findDevices();
                nanoleafList.setItems(deviceList);
            });
            refreshThread.setName("refresh-mDNS");
            refreshThread.start();
        } catch (BufferUnderflowException e) {
            Main.showException(e);
        }

    }


    private void getAccessToken(NLDeviceData deviceData) {
        try {
            logger.info("Asking {} for an access token", deviceData.deviceName);
            NLDevice connectedDevice = new NLDevice(deviceData);

            logger.info("Successfully connected to {}", deviceData.deviceName);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Save device for quick reconnect?");
            alert.setContentText("You can opt to save this device and access token to quickly reconnect next time.\nSaving this device will overwrite any previous saved devices.");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            dialogPane.getStylesheets().add("/gui.css");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                dataManager.saveDevice(connectedDevice);
            }
            transitionToSpotify(connectedDevice);

        } catch (NLException.AuthCodeException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error connecting to " + deviceData.deviceName);
            alert.setContentText("The request to get a access token for the device was refused, please make sure that the LED indicator on the controller is flashing white before attempting to connect.");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            dialogPane.getStylesheets().add("/gui.css");
            alert.showAndWait();
            setLoading(false);
        } catch (NLException | IOException e) {
            e.printStackTrace();
        }
    }

    private void transitionToSpotify(NLDevice device) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/connectToSpotify.fxml"));
            Parent root = loader.load();
            ConnectToSpotify connectToSpotifyCtrl = loader.getController();
            connectToSpotifyCtrl.initData(device);
            Stage stage = (Stage) nanoleafList.getScene().getWindow();
            Scene scene = new Scene(root, 400, 300);
            scene.getStylesheets().add("/gui.css");
            logger.info("Setting JavaFX scene to 'ConnectToSpotify' view");
            stage.setScene(scene);
        } catch (IOException e) {
            dev.jaxcksn.nanoleafMusic.Main.showException(e);
        }

    }

    public void DebugReconnect(ActionEvent actionEvent) {
        dataManager.removeDevice();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText("Delete Saved Device?");
        alert.setContentText("This will delete all reconnection data.");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinHeight(Region.USE_PREF_SIZE);
        dialogPane.getStylesheets().add("/gui.css");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            reconnectBtn.setDisable(true);
            debugButton.setDisable(true);
            dataManager.removeDevice();
        }
    }
}
