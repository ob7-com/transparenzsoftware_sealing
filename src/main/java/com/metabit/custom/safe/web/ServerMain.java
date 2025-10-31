package com.metabit.custom.safe.web;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.util.Map;

public class ServerMain
{
    public static void main(String[] args)
    {
        create().start(getPort());
    }

    public static Javalin create()
    {
        Javalin app = Javalin.create(config ->
        {
            config.staticFiles.add(staticFiles ->
            {
                staticFiles.hostedPath = "/"; // serve files under /
                staticFiles.directory = "/public"; // from classpath
                staticFiles.location = Location.CLASSPATH;
                staticFiles.precompress = false;
            });
        });

        registerRoutes(app);
        return app;
    }

    static void registerRoutes(Javalin app)
    {
        app.get("/api/health", ctx -> ctx.json(Map.of("status", "ok")));
        SealController.register(app);
        RevealController.register(app);
        KeysController.register(app);
        VerifyController.register(app);
    }

    private static int getPort()
    {
        String port = System.getenv("PORT");
        if (port == null || port.isEmpty())
        {
            return 8080;
        }
        return Integer.parseInt(port);
    }
}


