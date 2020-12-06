package dev.jaxcksn.nanoleafMusic;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERefreshRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERequest;
import dev.jaxcksn.nanoleafMusic.utility.CallbackServer;
import dev.jaxcksn.nanoleafMusic.utility.PKCE;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import java.util.TimerTask;

public class SpotifyManager {
    final static private String CLIENT_ID = "e0323b6a37ad487d80275aca99407602";
    final static private URI REDIRECT_URI = SpotifyHttpManager.makeUri("http://localhost:8001/connect");
    protected static String pkceVerification;
    public final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .build();
    public CallbackServer cbServer;

    public URI connectURI;
    public int expiresIn;

    public SpotifyManager() {
        //Setup the PKCE verification
        try {
            pkceVerification = PKCE.generateCodeVerifier();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        AuthorizationCodeUriRequest authorizationCodeUriRequest = null;
        try {
            authorizationCodeUriRequest = spotifyApi.authorizationCodePKCEUri(PKCE.generateCodeChallenge(pkceVerification))
                    .scope("user-read-currently-playing, user-read-playback-state")
                    .build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        assert authorizationCodeUriRequest != null;
        connectURI = authorizationCodeUriRequest.execute();
        cbServer = new CallbackServer();
    }

    public void getCredentials(String accessCode) {
        AuthorizationCodePKCERequest authorizationCodePKCERequest = spotifyApi.authorizationCodePKCE(accessCode, pkceVerification)
                .build();
        try {
            AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodePKCERequest.execute();
            spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
            spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());
            expiresIn = authorizationCodeCredentials.getExpiresIn();
        } catch (ParseException | SpotifyWebApiException | IOException e) {
            e.printStackTrace();
        }
    }
}
