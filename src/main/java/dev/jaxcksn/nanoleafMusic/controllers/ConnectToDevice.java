package dev.jaxcksn.nanoleafMusic.controllers;

import dev.jaxcksn.nanoleafMusic.DataManager;
import dev.jaxcksn.nanoleafMusic.Main;
import dev.jaxcksn.nanoleafMusic.utility.DataManagerException;
import io.github.rowak.nanoleafapi.Aurora;
import io.github.rowak.nanoleafapi.StatusCodeException;
import javafx.collections.FXCollections;
import io.github.rowak.nanoleafapi.AuroraMetadata;
import io.github.rowak.nanoleafapi.tools.Setup;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.awt.Toolkit;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.List;
import java.util.Optional;

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

    public void initialize() {
        dataManager = new DataManager();
        if(!dataManager.hasSaved) {
            reconnectBtn.setDisable(true);
        }

        deviceList = FXCollections.observableArrayList();
        try {
            findDevices();
        } catch (BufferUnderflowException e) {
            System.out.println("No Devices Found.");
        }
        nanoleafList.setItems(deviceList);
    }


    private void findDevices() {
        setLoading(true);
        List<AuroraMetadata> auroras = null;
        try {
            auroras = Setup.findAuroras();
            auroraList = auroras;
            for (AuroraMetadata aurora : auroras) {
                System.out.println(aurora.getDeviceName());
                deviceList.add(aurora.getDeviceName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        setLoading(false);
    }

    public void reconnectToDevice(ActionEvent actionEvent) {
        //The button shouldn't be enabled if this is false.
        assert dataManager.hasSaved;
        try {
          Aurora savedDevice = dataManager.loadDevice();
          Alert alert = new Alert(Alert.AlertType.INFORMATION);
          alert.setHeaderText("Connected to "+savedDevice.getName());
          alert.setContentText("Successfully reconnected to saved device.");
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add("/gui.css");
          alert.showAndWait();
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
            findDevices();
        } catch (BufferUnderflowException e) {
            System.out.println("No Devices Found.");
        }
        nanoleafList.setItems(deviceList);
    }

    private void getAccessToken(AuroraMetadata metadata) {
        try {
            String accessToken = Setup.createAccessToken(metadata.getHostName(),metadata.getPort(),"v1");
            Aurora connectedDevice = new Aurora(metadata.getHostName(),metadata.getPort(),"v1",accessToken);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setHeaderText("Save device for quick reconnect?");
            alert.setContentText("You can save the device and access token to quickly reconnect last time. Saving this device will overwrite any previous saved devices.");
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
            dialogPane.getStylesheets().add("/gui.css");
            alert.showAndWait();
            setLoading(false);
        } catch (StatusCodeException e) {
            e.printStackTrace();
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
            stage.setScene(scene);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
