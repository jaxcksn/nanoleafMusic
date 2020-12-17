/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic;

import ch.qos.logback.classic.Logger;
import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERequest;
import dev.jaxcksn.nanoleafMusic.utility.CallbackServer;
import dev.jaxcksn.nanoleafMusic.utility.PKCE;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;

public class SpotifyManager {
    final static private String CLIENT_ID = "e0323b6a37ad487d80275aca99407602"; //2
    final static private URI REDIRECT_URI = SpotifyHttpManager.makeUri("http://localhost:8001/connect");
    protected static String pkceVerification;
    public final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .build();
    public CallbackServer cbServer;
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.SpotifyManager");
    public URI connectURI;
    public int expiresIn;

    public SpotifyManager() {
        //Setup the PKCE verification
        try {
            pkceVerification = PKCE.generateCodeVerifier();
        } catch (UnsupportedEncodingException e) {
            Main.showException(e);
        }

        AuthorizationCodeUriRequest authorizationCodeUriRequest = null;
        try {
            authorizationCodeUriRequest = spotifyApi.authorizationCodePKCEUri(PKCE.generateCodeChallenge(pkceVerification))
                    .scope("user-read-currently-playing, user-read-playback-state")
                    .build();
        } catch (NoSuchAlgorithmException e) {
            Main.showException(e);
        }
        assert authorizationCodeUriRequest != null;
        connectURI = authorizationCodeUriRequest.execute();
        cbServer = new CallbackServer();
        logger.info("Callback server started and listening at localhost:8001/connect");
    }

    public void getCredentials(String accessCode) {
        AuthorizationCodePKCERequest authorizationCodePKCERequest = spotifyApi.authorizationCodePKCE(accessCode, pkceVerification)
                .build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodePKCERequest.execute();
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            expiresIn = authorizationCodeCredentials.getExpiresIn();
        } catch (ParseException | IOException e) {
            Main.showException(e);
        } catch (SpotifyWebApiException spotifyWebApiException) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Exception");
            alert.setHeaderText("Spotify Web Api Exception");
            alert.setContentText("An exception was thrown.");


// Create expandable Exception.
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            spotifyWebApiException.printStackTrace(pw);
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
            alert.showAndWait();
        }
    }
}
