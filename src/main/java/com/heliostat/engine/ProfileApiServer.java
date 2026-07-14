package com.heliostat.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliostat.engine.model.UserProfile;
import com.heliostat.engine.repository.UserProfileRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class ProfileApiServer {

    private static final UserProfileRepository repository = new UserProfileRepository("storage/profiles");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", new RootHandler());

        // Map the endpoint path
        server.createContext("/api/profiles", new ProfileHandler());

        server.setExecutor(null); // default executor
        System.out.println("[INFO] Heliostat Profile REST Web Service running on http://localhost:8080/api/profiles");
        server.start();
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Prevent matching sub-paths unintentionally if they aren't explicitly registered
            if (!exchange.getRequestURI().getPath().equals("/")) {
                String error = "{\"error\": \"Not Found\"}";
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(404, error.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String welcomeMessage = "{\"message\": \"Welcome to the Heliostat Engine API Console (Project 0.5)\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, welcomeMessage.length());

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(welcomeMessage.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    static class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // Set global response headers for JSON output
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            try {
                switch (method) {
                    case "POST":
                        handleCreateOrUpdate(exchange);
                        break;
                    case "GET":
                        handleRead(exchange);
                        break;
                    case "DELETE":
                        handleDelete(exchange);
                        break;
                    default:
                        sendResponse(exchange, 405, "{\"error\": \"Method Not Allowed\"}");
                }
            } catch (Exception e) {
                String errorMsg = "{\"error\": \"" + e.getMessage() + "\"}";
                sendResponse(exchange, 500, errorMsg);
            }
        }

        // POST /api/profiles -> Accept JSON payload to write/overwrite profile
        private void handleCreateOrUpdate(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody();
            UserProfile profile = objectMapper.readValue(is, UserProfile.class);

            repository.save(profile);

            String jsonResponse = objectMapper.writeValueAsString(profile);
            sendResponse(exchange, 201, jsonResponse);
        }

        // GET /api/profiles OR GET /api/profiles?id=xyz
        private void handleRead(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();

            if (query != null && query.startsWith("id=")) {
                String id = query.split("=")[1];
                Optional<UserProfile> profile = repository.findById(id);
                if (profile.isPresent()) {
                    sendResponse(exchange, 200, objectMapper.writeValueAsString(profile.get()));
                } else {
                    sendResponse(exchange, 404, "{\"error\": \"Profile not found\"}");
                }
            } else {
                // Get all records in directory
                List<UserProfile> profiles = repository.findAll();
                sendResponse(exchange, 200, objectMapper.writeValueAsString(profiles));
            }
        }

        // DELETE /api/profiles?id=xyz
        private void handleDelete(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("id=")) {
                String id = query.split("=")[1];
                boolean deleted = repository.deleteById(id);
                if (deleted) {
                    sendResponse(exchange, 200, "{\"message\": \"Profile successfully deleted\"}");
                } else {
                    sendResponse(exchange, 404, "{\"error\": \"Profile not found for deletion\"}");
                }
            } else {
                sendResponse(exchange, 400, "{\"error\": \"Missing required missing 'id' parameter\"}");
            }
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
            byte[] bytes = responseText.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}