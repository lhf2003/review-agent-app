package com.review.agent.client;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class StaticServer {
    private HttpServer server;
    private final Path distRoot;
    private final int port;
    private final HttpClient client = HttpClient.newBuilder().build();

    public StaticServer(Path distRoot, int port) {
        this.distRoot = distRoot;
        this.port = port;
    }

    public void start() throws IOException {
        if (server != null) return;
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/api", this::proxyHandler);
        server.createContext("/", this::staticHandler);
        server.setExecutor(null);
        server.start();
        System.out.println("StaticServer started at http://localhost:" + port + "/ serving " + distRoot);
    }

    private void staticHandler(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Path file = resolveStatic(path);
        if (file == null) file = distRoot.resolve("index.html");
        byte[] bytes = Files.readAllBytes(file);
        String contentType = guessContentType(file);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void proxyHandler(HttpExchange exchange) throws IOException {
        try {
            String backend = "http://localhost:8081";
            String tail = Optional.ofNullable(exchange.getRequestURI().getPath()).orElse("").replaceFirst("^/api", "");
            URI uri = URI.create(backend + tail + Optional.ofNullable(exchange.getRequestURI().getQuery()).map(q -> "?" + q).orElse(""));
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri).method(exchange.getRequestMethod(), getBodyPublisher(exchange));
            Headers reqHeaders = exchange.getRequestHeaders();
            for (String k : reqHeaders.keySet()) {
                if (k.equalsIgnoreCase("host")) continue;
                for (String v : reqHeaders.get(k)) builder.header(k, v);
            }
            HttpResponse<byte[]> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            Headers resHeaders = exchange.getResponseHeaders();
            resp.headers().map().forEach((k, vs) -> vs.forEach(v -> resHeaders.add(k, v)));
            exchange.sendResponseHeaders(resp.statusCode(), resp.body() == null ? -1 : resp.body().length);
            try (OutputStream os = exchange.getResponseBody()) {
                if (resp.body() != null) os.write(resp.body());
            }
        } catch (Exception e) {
            byte[] bytes = ("Proxy error: " + e.getMessage()).getBytes();
            exchange.sendResponseHeaders(502, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    private HttpRequest.BodyPublisher getBodyPublisher(HttpExchange exchange) throws IOException {
        if (exchange.getRequestBody() == null) return HttpRequest.BodyPublishers.noBody();
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        return bytes.length == 0 ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(bytes);
    }

    private Path resolveStatic(String requestPath) {
        if (requestPath == null || requestPath.equals("/")) return distRoot.resolve("index.html");
        Path p = distRoot.resolve(requestPath.substring(1));
        if (Files.exists(p)) return p;
        return null;
    }

    private String guessContentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
