package com.nutritracker.server;

import com.nutritracker.controller.ApiController;
import com.nutritracker.database.DatabaseManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

public class NutritionTrackerServer {
    // Render sets PORT env var automatically; fallback to 8080 for local dev
    private static final int PORT = Integer.parseInt(
        System.getenv().getOrDefault("PORT", "8080")
    );
    private static final String FRONTEND_DIR = "frontend";


    public static void main(String[] args) {
        try {
            System.out.println("====================================================");
            System.out.println("🥗 NutriTracker AI — Java REST Backend & Server");
            System.out.println("====================================================");

            // 1. Initialize SQLite Database
            DatabaseManager.getInstance();

            // 2. Start HTTP Server
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            ApiController apiController = new ApiController();

            // API Routing
            server.createContext("/api", apiController::handleApi);

            // Static Frontend Assets Routing
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(Executors.newFixedThreadPool(16));
            server.start();

            System.out.println("🚀 Server running successfully at: http://localhost:" + PORT);
            System.out.println("📱 Open http://localhost:" + PORT + " in your browser to access the dashboard.");
            System.out.println("📡 API Base URL: http://localhost:" + PORT + "/api");
            System.out.println("💾 Database: SQLite (backend/data/nutrition_tracker.db)");
            System.out.println("====================================================");

        } catch (Exception e) {
            System.err.println("Fatal error starting NutriTracker server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();

            // If root, serve index.html
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Clean path
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            Path filePath = Paths.get(FRONTEND_DIR, path);
            File file = filePath.toFile();

            if (!file.exists() || file.isDirectory()) {
                // Fallback to index.html for SPA routing if HTML request
                filePath = Paths.get(FRONTEND_DIR, "index.html");
                file = filePath.toFile();
            }

            if (!file.exists()) {
                String notFound = "404 Not Found: " + path;
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            String contentType = getMimeType(file.getName());
            byte[] fileBytes = Files.readAllBytes(filePath);

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
            exchange.getResponseHeaders().set("Pragma", "no-cache");
            exchange.getResponseHeaders().set("Expires", "0");
            exchange.sendResponseHeaders(200, fileBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        }

        private String getMimeType(String filename) {
            String lower = filename.toLowerCase();
            if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".ico")) return "image/x-icon";
            if (lower.endsWith(".woff2")) return "font/woff2";
            if (lower.endsWith(".woff")) return "font/woff";
            return "application/octet-stream";
        }
    }
}
