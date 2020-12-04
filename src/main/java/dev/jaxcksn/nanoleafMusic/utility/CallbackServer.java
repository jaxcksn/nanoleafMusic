package dev.jaxcksn.nanoleafMusic.utility;
import spark.ModelAndView;
import spark.template.jade.JadeTemplateEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static spark.Spark.*;

public class CallbackServer {
    private final CountDownLatch codeLatch = new CountDownLatch(1);
    protected String accessCode;

    public CallbackServer() {
        port(8001);
        staticFiles.location("/public");

        after("/connect",(req,res)->{
           codeLatch.countDown();
        });

        get("/connect", (req,res)->{
            Map<String, Boolean> map = new HashMap<>();
            String code = req.queryParams("code");
            if( code == null || code.isEmpty()) {
                map.put("isError",true);
                accessCode = "";
                return new ModelAndView(map, "error");
            } else {
                map.put("isError", false);
                accessCode = code;
                return new ModelAndView(map, "connect");
            }

        }, new JadeTemplateEngine());


    }

    public String awaitAccessCode() {
        try {
            codeLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return accessCode;
    }

    public void stopServer() {
        stop();
    }
}
