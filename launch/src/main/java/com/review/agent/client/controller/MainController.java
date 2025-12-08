package com.review.agent.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.concurrent.Worker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import netscape.javascript.JSObject;
import com.review.agent.client.StaticServer;

public class MainController {
    @FXML
    private Label dataLabel;

    @FXML
    private WebView webView;

    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private volatile boolean fallbackApplied = false;

    @FXML
    public void initialize() {
        if (webView == null) return;
        WebEngine webEngine = webView.getEngine();
        String url = resolveFrontendUrl();
        if (url.startsWith("file:")) {
            Path index = resolveDistIndex();
            if (index != null) {
                try {
                    new StaticServer(index.getParent(), 3000).start();
                    url = "http://localhost:3000/";
                } catch (Exception e) {
                    System.out.println("StaticServer start failed: " + e.getMessage());
                }
            }
        }
        webEngine.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaBridge", this);
                System.out.println("WebView load succeeded: " + webEngine.getLocation());
            } else if (n == Worker.State.FAILED) {
                System.out.println("WebView load failed: " + webEngine.getLocation());
                if (!fallbackApplied && isDevUrl(webEngine.getLocation())) {
                    Path index = resolveDistIndex();
                    if (index != null) {
                        try {
                            new StaticServer(index.getParent(), 3000).start();
                            String serverUrl = "http://localhost:3000/";
                            System.out.println("Fallback to local server: " + serverUrl);
                            fallbackApplied = true;
                            webEngine.load(serverUrl);
                        } catch (Exception e) {
                            String fallback = index.toUri().toString();
                            System.out.println("Fallback to dist: " + fallback);
                            fallbackApplied = true;
                            webEngine.load(fallback);
                        }
                    }
                }
            }
        });
        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldEx, newEx) -> {
            if (newEx != null) System.out.println("WebView exception: " + newEx.getMessage());
        });
        webEngine.load(url);
    }

    private String resolveFrontendUrl() {
        // Prefer dev server; fallback handled in state listener
        String dev = "http://localhost:5173/";
        if (dev != null) return dev;
        Path index = resolveDistIndex();
        if (index != null) return index.toUri().toString();
        return "about:blank";
    }

    private Path resolveDistIndex() {
        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path[] candidates = new Path[]{
                cwd.getParent() != null ? cwd.getParent().resolve("frontend").resolve("dist").resolve("index.html") : null,
                cwd.resolve("frontend").resolve("dist").resolve("index.html")
        };
        for (Path p : candidates) {
            if (p != null && Files.exists(p)) return p.normalize();
        }
        return null;
    }

    private boolean isDevUrl(String location) {
        return location != null && location.startsWith("http://localhost:5173");
    }

    private boolean isDevServerAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://localhost:5173/")).GET().build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void showMessage(String message) {
        System.out.println(message);
    }
}
