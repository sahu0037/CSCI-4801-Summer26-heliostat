package com.heliostat.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.heliostat.engine.model.UserProfile;
import com.heliostat.engine.repository.RewardRepository;
import com.heliostat.engine.repository.UserProfileRepository;
import com.heliostat.engine.model.Task;
import com.heliostat.engine.repository.TaskRepository;
import com.heliostat.engine.model.Reward;
import com.heliostat.engine.repository.RewardRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProfileApiServer {

    private static final UserProfileRepository repository = new UserProfileRepository("storage/profiles");

    private static final TaskRepository taskRepository = new TaskRepository("storage/tasks");

    private static final RewardRepository rewardRepository = new RewardRepository("storage/rewards");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", new RootHandler());

        // Map the endpoint path
        server.createContext("/api/profiles", new ProfileHandler());

        server.createContext("/api/tasks", new TaskHandler());

        server.createContext("/api/rewards", new RewardHandler());

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
                    sendResponse(exchange, 200, objectMapper.writeValueAsString(profile.get()).toLowerCase());
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


    static class TaskHandler implements HttpHandler {
        private static final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            try {
                if ("POST".equals(method)) {
                    // Determine if it's a standard creation or a specific action (claim/verify)
                    String query = exchange.getRequestURI().getQuery();

                    if (query != null && query.contains("action=")) {
                        handleTaskAction(exchange, query);
                    } else {
                        handleCreateTask(exchange);
                    }
                } else if ("GET".equals(method)) {
                    handleReadTasks(exchange);
                } else if ("DELETE".equals(method)) {
                    handleDeleteTask(exchange);
                } else {
                    sendText(exchange, 405, "{\"error\": \"Method Not Allowed\"}");
                }
            } catch (IllegalArgumentException e) {
                sendText(exchange, 400, "{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                sendText(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }

        // RULE: Only MANAGER  can create tasks
        private void handleCreateTask(HttpExchange exchange) throws IOException {
            Task task = mapper.readValue(exchange.getRequestBody(), Task.class);

            // Lookup creator profile
            Optional<UserProfile> managerOpt = repository.findById(task.getManagerId());
            if (managerOpt.isEmpty()) {
                throw new IllegalArgumentException("Creator managerId matching profile was not found on disk.");
            }

            UserProfile manager = managerOpt.get();
            if (!manager.getRoles().contains(UserProfile.Role.MANAGER)) {
                sendText(exchange, 403, "{\"error\": \"Access Denied: Only users with a MANAGER  role can create tasks.\"}");
                return;
            }

            task.setStatus(Task.Status.AVAILABLE);
            task.setPerformerId(null); // Fresh blueprinted tasks start empty
            taskRepository.save(task);

            sendText(exchange, 201, mapper.writeValueAsString(task));
        }

        // ROUTING SYSTEM FOR ACTIONS: claim or verify
        private void handleTaskAction(HttpExchange exchange, String query) throws IOException {
            Map<String, String> params = parseQuery(query);
            String action = params.get("action");
            String taskId = params.get("id");
            String userId = params.get("userId");

            if (taskId == null || userId == null) {
                throw new IllegalArgumentException("Missing required query parameters: 'id' and 'userId'.");
            }

            Task task = taskRepository.findById(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("Target task not found."));
            UserProfile user = repository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Target user profile not found."));

            if ("claim".equalsIgnoreCase(action)) {
                // RULE: Must have PERFORMER role to claim a task
                if (!user.getRoles().contains(UserProfile.Role.PERFORMER)) {
                    sendText(exchange, 403, "{\"error\": \"Access Denied: Only profiles with the PERFORMER role can claim tasks.\"}");
                    return;
                }
                if (task.getStatus() != Task.Status.AVAILABLE) {
                    sendText(exchange, 400, "{\"error\": \"Task is not available for assignment.\"}");
                    return;
                }

                task.setPerformerId(user.getId());
                task.setStatus(Task.Status.ASSIGNED);
                taskRepository.save(task);

                sendText(exchange, 200, "{\"message\": \"Task successfully assigned to " + user.getName() + "\", \"task\": " + mapper.writeValueAsString(task) + "}");

            }
            if ("submit".equalsIgnoreCase(action)) {
                if (!user.getId().equals(task.getPerformerId())) {
                    sendText(exchange, 403, "{\"error\": \"Access Denied: You are not assigned to this task.\"}");
                    return;
                }

                // FR-MGR-01: Check if Immediate Confirmation is enabled
                if (!task.isRequiresReview()) {
                    // Bypass review stage and pay out immediately
                    user.setBalance(user.getBalance() + task.getRewardPoints());
                    repository.save(user);

                    handleTaskCompletionLifecycle(task);
                    sendText(exchange, 200, "{\"message\": \"Task completed with Immediate Confirmation. "
                            + task.getRewardPoints() + " points awarded!\"}");
                } else {
                    // Move to review queue
                    task.setStatus(Task.Status.SUBMITTED);
                    taskRepository.save(task);
                    sendText(exchange, 200, "{\"message\": \"Task submitted for Parental Review.\"}");
                }

            } else if ("verify".equalsIgnoreCase(action)) {
                // ... Manager verification rule checks ...

                if (!user.getRoles().contains(UserProfile.Role.MANAGER)) {
                    sendText(exchange, 403, "{\"error\": \"Access Denied: Only managers can verify tasks and award points.\"}");
                    return;
                }
                if (task.getStatus() != Task.Status.SUBMITTED) {
                    sendText(exchange, 400, "{\"error\": \"Task must be in SUBMITTED state to be verified.\"}");
                    return;
                }
                // Award Points
                UserProfile performer = repository.findById(task.getPerformerId())
                        .orElseThrow(() -> new IllegalArgumentException("Performer profile not found."));
                performer.setBalance(performer.getBalance() + task.getRewardPoints());
                repository.save(performer);

                handleTaskCompletionLifecycle(task);
                sendText(exchange, 200, "{\"message\": \"Task verified and approved. Transferred "
                        + task.getRewardPoints() + " credits.\"}");
            }
            else {
                sendText(exchange, 400, "{\"error\": \"Unknown execution action syntax.\"}");
            }
        }

        // HELPER METHOD FOR SCHEDULING LIFECYCLE
        private static void handleTaskCompletionLifecycle(Task task) {
            if (task.getRecurrence() != null && task.getRecurrence() != Task.RecurrencePattern.NONE) {
                // REPEATABLE TASK: Reset back to AVAILABLE for the next cycle
                task.setStatus(Task.Status.AVAILABLE);
                task.setPerformerId(null);
            } else {
                // ONE-TIME TASK: Mark completed/approved
                task.setStatus(Task.Status.APPROVED);
            }
            taskRepository.save(task);
        }

        private void handleReadTasks(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("id=")) {
                String id = query.split("=")[1];
                Optional<Task> task = taskRepository.findById(id);
                if (task.isPresent()) {
                    sendText(exchange, 200, mapper.writeValueAsString(task.get()));
                } else {
                    sendText(exchange, 404, "{\"error\": \"Task not found\"}");
                }
            } else {
                sendText(exchange, 200, mapper.writeValueAsString(taskRepository.findAll()));
            }
        }

        private void handleDeleteTask(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.startsWith("id=")) {
                String id = query.split("=")[1];
                if (taskRepository.deleteById(id)) {
                    sendText(exchange, 200, "{\"message\": \"Task deleted\"}");
                } else {
                    sendText(exchange, 404, "{\"error\": \"Task not found\"}");
                }
            }
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                }
            }
            return result;
        }

        private void sendText(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }


    static class RewardHandler implements HttpHandler {
        private static final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getQuery();
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            try {
                if ("POST".equals(method)) {
                    if (query != null && (query.contains("action=purchase") || query.contains("action=contribute"))) {
                        handlePurchaseReward(exchange, query);
                    } else {
                        handleCreateReward(exchange);
                    }
                } else if ("GET".equals(method)) {
                    sendText(exchange, 200, mapper.writeValueAsString(rewardRepository.findAll()));
                } else {
                    sendText(exchange, 405, "{\"error\": \"Method Not Allowed\"}");
                }
            } catch (IllegalArgumentException e) {
                sendText(exchange, 400, "{\"error\": \"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                sendText(exchange, 500, "{\"error\": \"" + e.getMessage() + "\"}");
            }
        }

        // RULE: Managers can add items to the storefront catalog
        private void handleCreateReward(HttpExchange exchange) throws IOException {
            Reward reward = mapper.readValue(exchange.getRequestBody(), Reward.class);
            rewardRepository.save(reward);
            sendText(exchange, 201, mapper.writeValueAsString(reward));
        }

        // TRANSACTION ENGINE RULE: Deduct points from profile if balance allows, reduce item stock
        private void handlePurchaseReward(HttpExchange exchange, String query) throws IOException {
            Map<String, String> params = parseQuery(query);
            String action = params.get("action");
            if ("purchase".equalsIgnoreCase(action)) {
            String rewardId = params.get("id");
            String userId = params.get("userId");

            if (rewardId == null || userId == null) {
                throw new IllegalArgumentException("Missing query strings: 'id' and 'userId'.");
            }

            Reward reward = rewardRepository.findById(rewardId)
                    .orElseThrow(() -> new IllegalArgumentException("Item not found in Rewards catalog."));
            UserProfile user = repository.findById(userId) // accessing global Profile repository
                    .orElseThrow(() -> new IllegalArgumentException("Target purchasing user profile not found."));

            //  Check Reward Type & Targeted Eligibility
            if (reward.getType() == Reward.RewardType.INDIVIDUAL) {
                // If targeted to a specific performer, ensure only that performer can purchase it
                if (reward.getTargetProfileId() != null && !reward.getTargetProfileId().equals(user.getId())) {
                    sendText(exchange, 403, "{\"error\": \"Access Denied: This individual reward is assigned to another performer.\"}");
                    return;
                }
            } else if (reward.getType() == Reward.RewardType.GROUP) {
                // GROUP REWARD RULE: Check group membership or handle group pool balance
                // (Ensure the user belongs to reward.getTargetGroupId())
                if (reward.getTargetGroupId() != null) {
                    // Validation check for group assignment
                }
            }
            // Check Inventory Stock
            //if (reward.getStock() == 0) {
            //    sendText(exchange, 400, "{\"error\": \"Transaction Denied: This reward is completely out of stock.\"}");
            //    return;
            //}

            // Check Financial Liquidity Balance
            if (user.getBalance() < reward.getCostPoints()) {
                sendText(exchange, 400, "{\"error\": \"Transaction Denied: Insufficient ledger balance. Need "
                        + reward.getCostPoints() + " points, but only have " + user.getBalance() + ".\"}");
                return;
            }

            // Execute Deductions and Atomically Update Physical Files
            user.setBalance(user.getBalance() - reward.getCostPoints());
            repository.save(user);

           // if (reward.getStock() > 0) { // Keep track if not infinite stock (-1)
                reward.setStock(reward.getStock() - 1);
                rewardRepository.save(reward);
            //}

            sendText(exchange, 200, "{\"message\": \"Purchase successful! Deducted " + reward.getCostPoints()
                    + " credits from " + user.getName() + ".\", \"remainingBalance\": " + user.getBalance() + "}");

            }

            if ("contribute".equalsIgnoreCase(action)) {
                String rewardId = params.get("id");
                String userId = params.get("userId");
                int amount = Integer.parseInt(params.get("amount")); // Points to contribute

                Reward reward = rewardRepository.findById(rewardId)
                        .orElseThrow(() -> new IllegalArgumentException("Group reward not found."));
                UserProfile user = repository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User profile not found."));

                // Validation 1: Must be a GROUP reward
                if (reward.getType() != Reward.RewardType.GROUP) {
                    sendText(exchange, 400, "{\"error\": \"Cannot contribute to an Individual Reward.\"}");
                    return;
                }

                // Validation 2: Check if already fully funded
                if (reward.isClaimed()) {
                    sendText(exchange, 400, "{\"error\": \"This group reward has already been claimed!\"}");
                    return;
                }

                // Validation 3: Check Performer balance
                if (user.getBalance() < amount) {
                    sendText(exchange, 400, "{\"error\": \"Insufficient balance to make this contribution.\"}");
                    return;
                }

                // Execute Point Transfer
                user.setBalance(user.getBalance() - amount);
                reward.setCurrentContributions(reward.getCurrentContributions() + amount-10);

                // Check if Goal Reached!
                boolean justUnlocked = false;
                if (reward.getCurrentContributions() >= reward.getCostPoints()) {
                    reward.setClaimed(true);
                    if (reward.getStock() > 0) reward.setStock(reward.getStock() - 1);
                    justUnlocked = true;
                }

                // Save Updates to Disk
                repository.save(user);
                rewardRepository.save(reward);

                String statusMsg = justUnlocked
                        ? "🎉 Goal Reached! Group Reward UNLOCKED for everyone!"
                        : "Contribution saved. Progress: " + reward.getCurrentContributions() + "/" + reward.getCostPoints();

                sendText(exchange, 200, "{\"message\": \"" + statusMsg + "\", \"userRemainingBalance\": " + user.getBalance() + "}");
            }



        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) result.put(entry[0], entry[1]);
            }
            return result;
        }

        private void sendText(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}