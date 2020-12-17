/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.controllers;

import ch.qos.logback.classic.Logger;
import dev.jaxcksn.nanoleafMusic.DataManager;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.DataManagerException;
import io.github.rowak.nanoleafapi.Aurora;
import io.github.rowak.nanoleafapi.AuroraMetadata;
import io.github.rowak.nanoleafapi.StatusCodeException;
import io.github.rowak.nanoleafapi.tools.Setup;
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
import net.straylightlabs.hola.dns.Domain;
import net.straylightlabs.hola.sd.Instance;
import net.straylightlabs.hola.sd.Query;
import net.straylightlabs.hola.sd.Service;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.BufferUnderflowException;
import java.util.List;
import java.util.*;

public class ConnectToDevice {
    @FXML
    private Button reconnectBtn;
    @FXML
    private ListView<String> nanoleafList;
    @FXML
    private BorderPane selectPane;
    @FXML
    private AnchorPane loadingPane;

    private ObservableList<String> deviceList;
    private List<AuroraMetadata> auroraList;
    private DataManager dataManager;
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.ConnectToDevice");

    public void initialize() {
        dataManager = new DataManager();
        if (!dataManager.hasSaved) {
            reconnectBtn.setDisable(true);
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

    static class ipv6Exception extends Exception {
        String message;

        ipv6Exception(String message) {
            this.message=message;
        }

        @Override
        public String toString() {
            return "ipv6Exception{" +
                    "message='" + message + '\'' +
                    '}';
        }
    }

    private List<AuroraMetadata> findNanoleaf() throws IOException {
        List<AuroraMetadata> auroras = new ArrayList<>();

        try {
            Service service = Service.fromName("_nanoleafapi._tcp");
            Query query = Query.createFor(service, Domain.LOCAL);
            Set<Instance> instances = query.runOnce();
            instances.forEach((instance -> {
                try {
                    auroras.add(fromMDNSInstance(instance));
                } catch (ipv6Exception ignored) {
                    System.out.println("IPV6Exception");
                }
            }));
        } catch (Exception e) {
            Main.showException(e);
        }

        return auroras;
    }

    /**
     * This is patch of the fromMDNSInstance function included in the Nanoleaf API Wrapper. It makes sure
     * that the IPv4 address is used instead of the Ipv6 address.
     *
     * @param instance the MDNS instance to build from.
     * @return an AuroraMetadata object
     * @throws ipv6Exception
     */
    public static AuroraMetadata fromMDNSInstance(Instance instance) throws ipv6Exception {
        AuroraMetadata metadata = new AuroraMetadata(null, 0, null, null);
        Iterator<InetAddress> addresses = instance.getAddresses().iterator();
        String hostName,deviceId;

        InetAddress initialAddress = addresses.next();
        if(initialAddress instanceof Inet4Address) {
            hostName = initialAddress.getCanonicalHostName();
            deviceId = initialAddress.getHostAddress();
        } else {
            InetAddress nextAddress = addresses.next();
            if(nextAddress instanceof Inet4Address) {
                hostName = nextAddress.getCanonicalHostName();
                deviceId = nextAddress.getHostAddress();
            } else {
                throw new ipv6Exception("There are no available IPv4 addresses for the program to connect to.");
            }
        }

        metadata.setHostName(hostName);
        metadata.setPort(instance.getPort());
        metadata.setDeviceId(deviceId);
        metadata.setDeviceName(instance.getName());
        return metadata;
    }


    private void findDevices() {
        setLoading(true);
        List<AuroraMetadata> auroras = null;
        try {
            auroras = findNanoleaf();
            logger.info("Found {} devices from mDNS query", auroras.size());
            auroraList = auroras;
            for (AuroraMetadata aurora : auroras) {
                deviceList.add(aurora.getDeviceName());
            }
        } catch (IOException e) {
            Main.showException(e);
        }
        setLoading(false);
    }

    public void reconnectToDevice(ActionEvent actionEvent) {
        //The button shouldn't be enabled if this is false.
        assert dataManager.hasSaved;
        try {
            Aurora savedDevice = dataManager.loadDevice();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Connected to " + savedDevice.getName());
            alert.setContentText("Successfully reconnected to saved device.");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add("/gui.css");
            alert.showAndWait();
            logger.info("Successfully connected to {}", savedDevice.getName());
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
        }
    }


    public void connectToSelected(ActionEvent actionEvent) {
        if (nanoleafList.getSelectionModel().getSelectedIndex() != -1) {
            setLoading(true);

            int auroraIndex = nanoleafList.getSelectionModel().getSelectedIndex();
            AuroraMetadata selectedDevice = auroraList.get(auroraIndex);

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

    private void getAccessToken(AuroraMetadata metadata) {
        try {
            logger.info("Asking {} for an access token", metadata.getDeviceName());
            String accessToken = Setup.createAccessToken(metadata.getHostName(),metadata.getPort(),"v1");
            Aurora connectedDevice = new Aurora(metadata.getHostName(),metadata.getPort(),"v1",accessToken);
            logger.info("Successfully connected to {}", metadata.getDeviceName());
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Save device for quick reconnect?");
            alert.setContentText("You can opt to save this device and access token to quickly reconnect next time. Saving this device will overwrite any previous saved devices.");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            dialogPane.getStylesheets().add("/gui.css");
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                dataManager.saveDevice(connectedDevice);
            }
            transitionToSpotify(connectedDevice);

        } catch (StatusCodeException.ForbiddenException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Error connecting to " + metadata.getDeviceName());
            alert.setContentText("The request to get a access token for the device was refused, please make sure that the LED indicator on the controller is flashing white before attempting to connect.");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setMinHeight(Region.USE_PREF_SIZE);
            dialogPane.getStylesheets().add("/gui.css");
            alert.showAndWait();
            setLoading(false);
        } catch (StatusCodeException e) {
            Main.showException(e);
        }
    }

    private void transitionToSpotify(Aurora device) {
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
            Main.showException(e);
        }

    }
}
