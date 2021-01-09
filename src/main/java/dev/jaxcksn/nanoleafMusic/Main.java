/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic;

import ca.weblite.objc.Proxy;
import ch.qos.logback.classic.Logger;
import dev.jaxcksn.nanoleafMusic.utility.NSProcessInfoUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends Application {
    private static Proxy appNapPrevented;
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.dev.jaxcksn.nanoleafMusic.Main");

    public static void main(String[] args) {
        logger.info("Application Started");
        if (isMac()) {
            logger.info("macOS detected, attempting to prevent App Nap");
            appNapPrevented = NSProcessInfoUtils.beginActivityWithOptions("Needs to be alive to constantly update effect.");
        }

        Thread.UncaughtExceptionHandler handler = ((th, ex) -> {
            showException(new Exception(ex));
        });
        Thread.setDefaultUncaughtExceptionHandler(handler);

        launch(args);

    }

    @Override
    public void start(Stage stage) throws Exception {
        Thread.currentThread().setName("app");
        Parent root = FXMLLoader.load(getClass().getResource("/connectToDevice.fxml"));
        Scene scene = new Scene(root, 400, 300);

        Font.loadFont(getClass().getResourceAsStream("/fonts/OpenSans-Regular.ttf"), 13);
        Font.loadFont(getClass().getResourceAsStream("/fonts/OpenSans-Bold.ttf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/fonts/OpenSans-SemiBold.ttf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/fonts/OpenSans-ExtraBold.ttf"), 12);
        Font.loadFont(getClass().getResourceAsStream("/fonts/HankRnd-Black.ttf"), 12);
        root.getStylesheets().add("/gui.css");
        logger.info("JavaFX Assets Loaded");
        stage.setTitle("nanoleafMusic");
        stage.setResizable(false);
        stage.setScene(scene);
        logger.info("Setting JavaFX scene to 'ConnectToDevice' view");
        stage.show();

    }

    public static class NamedThreadFactory implements ThreadFactory {
        String name;
        AtomicInteger threadNo = new AtomicInteger(0);

        public NamedThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            String threadName = name + "-" + threadNo.incrementAndGet();
            return new Thread(r, threadName);
        }
    }

    @Override
    public void stop() throws Exception {
        if (isMac()) {
            NSProcessInfoUtils.endActivity(appNapPrevented);
        }
        System.exit(0);
    }

    public static boolean isMac() {
        String OS = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
        return OS.contains("mac");
    }

    public static void showException(Exception e) {
        logger.error("An Exception was Thrown", e);
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Exception");
            alert.setHeaderText("Program Exception Thrown");
            alert.setContentText("An exception or error was thrown while running the program. The program might not run after this happens.");

            // Create expandable Exception.
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String exceptionText = sw.toString();

            Label label = new Label("The exception stacktrace was:");

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);

            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(label, 0, 0);
            expContent.add(textArea, 0, 1);

// Set expandable Exception into the dialog pane.
            alert.getDialogPane().setExpandableContent(expContent);
            alert.getDialogPane().getStylesheets().add("/gui.css");
            alert.showAndWait();
        });
    }
}

