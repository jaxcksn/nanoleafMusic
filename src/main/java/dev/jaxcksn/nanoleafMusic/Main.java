package dev.jaxcksn.nanoleafMusic;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import jfxtras.styles.jmetro8.JMetro;

public class Main extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        JMetro jMetro = new JMetro(JMetro.Style.DARK);
        Parent root = FXMLLoader.load(getClass().getResource("/connectToDevice.fxml"));
        Scene scene = new Scene(root, 400, 300);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Raleway-Regular.ttf"),12);
        Font.loadFont(getClass().getResourceAsStream("/fonts/Raleway-Bold.ttf"),12);
        root.getStylesheets().add("/gui.css");
        stage.setTitle("nanoleafMusic");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        System.exit(0);
    }
}
