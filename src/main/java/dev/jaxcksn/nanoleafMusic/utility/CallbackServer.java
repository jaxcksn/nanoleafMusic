/*
 * Copyright (c) 2020, Jaxcksn
 * All rights reserved.
 */

package dev.jaxcksn.nanoleafMusic.utility;

import ch.qos.logback.classic.Logger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.jaxcksn.nanoleafMusic.Main;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class CallbackServer {
    protected static HttpServer server;
    private final authServerHandler requestHandler = new authServerHandler();
    private static final Logger logger
            = (Logger) LoggerFactory.getLogger("nanoleafMusic.CallbackServer");

    static public class authServerHandler implements HttpHandler {
        private final CountDownLatch tokenLatch = new CountDownLatch(1);
        private String authCode;
        private static final Logger logger
                = (Logger) LoggerFactory.getLogger("nanoleafMusic.CallbackServer");

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParamValue = null;


            if ("GET".equals(httpExchange.getRequestMethod())) {
                requestParamValue = handleGetRequest(httpExchange);
            }

            if (requestParamValue == null || requestParamValue.equals("SPOTIFYERROR")) {
                String textResponse = "<html lang=\"en\">\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                        "    <title>Error</title>\n" +
                        "    <link href=\"https://fonts.googleapis.com/css2?family=Open+Sans:wght@400;800&display=swap\" rel=\"stylesheet\">\n" +
                        "    <style>\n" +
                        "        body {\n" +
                        "            font-family: 'Open Sans', sans-serif;\n" +
                        "            background-color:#DC143C;\n" +
                        "        }\n" +
                        "\n" +
                        "        h1 {\n" +
                        "            font-weight: 900;\n" +
                        "        }\n" +
                        "\n" +
                        "        .vertical-center {\n" +
                        "            height: 100vh;\n" +
                        "            display: flex;\n" +
                        "            flex-direction: column;\n" +
                        "            justify-content: center;\n" +
                        "            align-items: center;\n" +
                        "            color: white;\n" +
                        "        }\n" +
                        "        </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <div class=\"vertical-center\">\n" +
                        "        <h1>Authentication Unsuccessful.</h1>\n" +
                        "        <p>Please restart the program and try again.</p>\n" +
                        "    </div>\n" +
                        "</body>\n" +
                        "</html>";
                logger.error("Received fatal response with from callback");
                httpExchange.sendResponseHeaders(200, textResponse.length());
                httpExchange.getResponseBody().write(textResponse.getBytes());
                httpExchange.getResponseBody().flush();
                httpExchange.getResponseBody().close();
                System.exit(1);
            } else {
                authCode = requestParamValue;
                String textResponse = "<!DOCTYPE html>\n" +
                        "<html lang=\"en\">\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                        "    <title>Success!</title>\n" +
                        "    <link href=\"https://fonts.googleapis.com/css2?family==Open+Sans:wght@400;800&display=swap\" rel=\"stylesheet\">\n" +
                        "    <style>\n" +
                        "        body {\n" +
                        "            font-family: 'Open Sans', sans-serif;\n" +
                        "            background-color: white;\n" +
                        "        }\n" +
                        "\n" +
                        "        h1 {\n" +
                        "            font-weight: 800;\n" +
                        "        }\n" +
                        "\n" +
                        "        .vertical-center {\n" +
                        "            height: 100vh;\n" +
                        "            display: flex;\n" +
                        "            flex-direction: column;\n" +
                        "            justify-content: center;\n" +
                        "            color: black;\n" +
                        "            align-items: center;\n" +
                        "        }\n" +
                        "        </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <div class=\"vertical-center\">\n" +
                        "        <h1>Spotify Connection was Successful</h1>\n" +
                        "        <p>You can now close this page.</p>\n" +
                        "    </div>\n" +
                        "</body>\n" +
                        "</html>";
                logger.info("Received valid response from callback");
                httpExchange.sendResponseHeaders(200, textResponse.length());
                httpExchange.getResponseBody().write(textResponse.getBytes());
                httpExchange.getResponseBody().flush();
                httpExchange.getResponseBody().close();
                tokenLatch.countDown();
            }
        }

        private String handleGetRequest(HttpExchange httpExchange) {
            String query = httpExchange.getRequestURI().toString().split("\\?")[1].split("=")[0];

            if (!query.equals("code")) {
                return "SPOTIFYERROR";
            } else {
                return httpExchange.
                        getRequestURI()
                        .toString()
                        .split("\\?")[1]
                        .split("=")[1];
            }


        }

        private String fetchAuthCode() {
            try {
                logger.info("Waiting for request to callback server");
                tokenLatch.await();
            } catch (InterruptedException e) {
                Main.showException(e);
            }
            logger.info("Passing access code from callback");
            return authCode;
        }

    }


    public CallbackServer() {

        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8001), 0);
            server.createContext("/connect", requestHandler);
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(5, new Main.NamedThreadFactory("callback"));
            server.setExecutor(threadPoolExecutor);
            server.start();
        } catch (IOException e) {
            Main.showException(e);
        }

    }

    public String getAuthCode() {
        return requestHandler.fetchAuthCode();
    }

    public void destroy() {
        logger.info("Destroying the callback server");
        server.stop(0);
    }

}
