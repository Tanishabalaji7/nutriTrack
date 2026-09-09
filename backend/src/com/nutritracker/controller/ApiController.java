package com.nutritracker.controller;

import com.nutritracker.dao.ChatDao;
import com.nutritracker.dao.FoodDao;
import com.nutritracker.dao.UserDao;
import com.nutritracker.dao.WaterLogDao;
import com.nutritracker.model.*;
import com.nutritracker.service.*;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiController {
    private final UserDao userDao;
    private final FoodDao foodDao;
    private final MealLogService mealLogService;
    private final WaterLogDao waterLogDao;
    private final ChatDao chatDao;
    private final ChatbotService chatbotService;
    private final AgeProfileAnalysisService ageAnalysisService;
    private final CalorieCalculatorService calorieService;
    private final GeminiService geminiService;

    public ApiController() {
        this.userDao = new UserDao();
        this.foodDao = new FoodDao();
        this.mealLogService = new MealLogService();
        this.waterLogDao = new WaterLogDao();
        this.chatDao = new ChatDao();
        this.chatbotService = new ChatbotService();
        this.ageAnalysisService = new AgeProfileAnalysisService();
        this.calorieService = new CalorieCalculatorService();
        this.geminiService = new GeminiService();
    }

    public void handleApi(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        Map<String, String> queryParams = parseQueryParams(query);

        // Handle CORS Pre-flight
        if ("OPTIONS".equals(method)) {
            sendResponse(exchange, 204, "");
            return;
        }

        try {
            // Determine user from header or query param
            int currentUserId = 1;
            String headerUserId = exchange.getRequestHeaders().getFirst("X-User-Id");
            if (headerUserId != null && !headerUserId.isEmpty()) {
                try { currentUserId = Integer.parseInt(headerUserId); } catch (Exception ignored) {}
            } else if (queryParams.containsKey("userId")) {
                try { currentUserId = Integer.parseInt(queryParams.get("userId")); } catch (Exception ignored) {}
            }

            // Authentication endpoints
            if (path.equals("/api/auth/login") && "POST".equals(method)) {
                String body = readRequestBody(exchange);
                JSONObject json = new JSONObject(body);
                String email = json.getString("email");
                String password = json.optString("password", "");

                UserProfile user = userDao.authenticate(email, password);
                if (user != null) {
                    JSONObject res = new JSONObject();
                    res.put("success", true);
                    res.put("user", user.toJSON());
                    sendJsonResponse(exchange, 200, res);
                } else {
                    sendJsonResponse(exchange, 401, new JSONObject().put("success", false).put("error", "Invalid email or password"));
                }
                return;
            } else if (path.equals("/api/auth/signup") && "POST".equals(method)) {
                String body = readRequestBody(exchange);
                JSONObject json = new JSONObject(body);
                String email = json.getString("email");

                UserProfile existing = userDao.findByEmail(email);
                if (existing != null) {
                    sendJsonResponse(exchange, 400, new JSONObject().put("success", false).put("error", "An account with this email already exists"));
                    return;
                }

                UserProfile newUser = UserProfile.fromJSON(json);
                UserProfile created = userDao.createUser(newUser);
                if (created != null) {
                    JSONObject res = new JSONObject();
                    res.put("success", true);
                    res.put("user", created.toJSON());
                    sendJsonResponse(exchange, 201, res);
                } else {
                    sendJsonResponse(exchange, 500, new JSONObject().put("success", false).put("error", "Failed to create account"));
                }
                return;
            } else if (path.equals("/api/auth/google") && "POST".equals(method)) {
                String body = readRequestBody(exchange);
                JSONObject json = new JSONObject(body);
                String email = json.optString("email", "google.user@example.com");
                String name = json.optString("name", "Google User");

                UserProfile user = userDao.findByEmail(email);
                if (user == null) {
                    UserProfile newUser = new UserProfile();
                    newUser.setName(name);
                    newUser.setEmail(email);
                    newUser.setAge(json.optInt("age", 26));
                    newUser.setGender(json.optString("gender", "female"));
                    newUser.setHeightCm(json.optDouble("heightCm", 168.0));
                    newUser.setWeightKg(json.optDouble("weightKg", 62.0));
                    newUser.setGoal(json.optString("goal", "maintain"));
                    newUser.setActivityLevel(json.optString("activityLevel", "moderate"));
                    user = userDao.createUser(newUser);
                }

                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("user", user.toJSON());
                sendJsonResponse(exchange, 200, res);
                return;
            }

            // Profile Endpoints
            if (path.equals("/api/profile") && "GET".equals(method)) {
                UserProfile user = userDao.getUserProfile(currentUserId);
                sendJsonResponse(exchange, 200, user.toJSON());
            } else if (path.equals("/api/profile") && "POST".equals(method)) {
                String body = readRequestBody(exchange);
                JSONObject json = new JSONObject(body);
                UserProfile user = UserProfile.fromJSON(json);
                user.setId(currentUserId);
                userDao.updateUserProfile(user);
                sendJsonResponse(exchange, 200, userDao.getUserProfile(currentUserId).toJSON());
            } else if (path.equals("/api/profile/age-analysis") && "GET".equals(method)) {
                UserProfile user = userDao.getUserProfile(currentUserId);
                int age = user.getAge();
                if (queryParams.containsKey("age")) {
                    try { age = Integer.parseInt(queryParams.get("age")); } catch (Exception ignored) {}
                }
                AgeNutritionRule rule = AgeNutritionRule.getRuleForAge(age);
                sendJsonResponse(exchange, 200, rule.toJSON());
            } 
            // Gemini AI API Status & Key management
            else if (path.equals("/api/gemini/status") && "GET".equals(method)) {
                UserProfile user = userDao.getUserProfile(currentUserId);
                boolean isConfigured = geminiService.isConfigured(user);
                JSONObject res = new JSONObject();
                res.put("configured", isConfigured);
                res.put("model", "gemini-3.7-flash");
                res.put("source", user.getGeminiApiKey() != null && !user.getGeminiApiKey().isEmpty() ? "user_profile" : (System.getenv("GEMINI_API_KEY") != null ? "environment" : "none"));
                sendJsonResponse(exchange, 200, res);
            } else if (path.equals("/api/gemini/key") && "POST".equals(method)) {
                String body = readRequestBody(exchange);
                JSONObject json = new JSONObject(body);
                String apiKey = json.optString("apiKey", "").trim();
                userDao.updateGeminiApiKey(currentUserId, apiKey);
                UserProfile updated = userDao.getUserProfile(currentUserId);
                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("configured", geminiService.isConfigured(updated));
                sendJsonResponse(exchange, 200, res);
            }
            // Foods Library Endpoints
            else if (path.equals("/api/foods") && "GET".equals(method)) {
                String q = queryParams.getOrDefault("q", "");
                String cat = queryParams.getOrDefault("category", "");
                List<FoodItem> foods = foodDao.searchFoods(q, cat);
                JSONArray arr = new JSONArray();
                for (FoodItem f : foods) arr.put(f.toJSON());
                JSONObject res = new JSONObject();
                res.put("foods", arr);
                sendJsonResponse(exchange, 200, res);
            } else if (path.equals("/api/foods") && "POST".equals(method)) {
                String body = readRequestBody(exchange);
                JSONObject json = new JSONObject(body);
                FoodItem food = FoodItem.fromJSON(json);
                food.setCustom(true);
                FoodItem created = foodDao.createFood(food);
                sendJsonResponse(exchange, 201, created != null ? created.toJSON() : new JSONObject().put("error", "Failed to create food"));
            } else if (path.equals("/api/foods/categories") && "GET".equals(method)) {
                List<String> categories = foodDao.getCategories();
                JSONObject res = new JSONObject();
                res.put("categories", new JSONArray(categories));
                sendJsonResponse(exchange, 200, res);
            } 
            // Logs & Tracker Endpoints
            else if (path.equals("/api/logs/daily") && "GET".equals(method)) {
                String date = queryParams.getOrDefault("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                JSONObject summary = mealLogService.getDailySummary(currentUserId, date);
                sendJsonResponse(exchange, 200, summary);
            } else if (path.equals("/api/logs") && "POST".equals(method)) {
                String body = readRequestBody(exchange);
                JSONObject json = new JSONObject(body);
                int foodId = json.getInt("foodId");
                String mealType = json.optString("mealType", "snack");
                double quantity = json.optDouble("quantity", 1.0);
                String date = json.optString("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                String notes = json.optString("notes", "");

                MealLog log = mealLogService.logFoodItem(currentUserId, foodId, mealType, quantity, date, "", notes);
                sendJsonResponse(exchange, 201, log != null ? log.toJSON() : new JSONObject().put("error", "Failed to log meal"));
            } else if (path.startsWith("/api/logs/") && "DELETE".equals(method)) {
                String idStr = path.substring("/api/logs/".length());
                int logId = Integer.parseInt(idStr);
                boolean deleted = mealLogService.deleteLog(logId);
                JSONObject res = new JSONObject();
                res.put("success", deleted);
                sendJsonResponse(exchange, 200, res);
            } else if (path.equals("/api/water") && "POST".equals(method)) {
                String body = readRequestBody(exchange);
                JSONObject json = new JSONObject(body);
                int amountMl = json.optInt("amountMl", 250);
                String date = json.optString("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                waterLogDao.addWaterLog(new WaterLog(0, currentUserId, date, amountMl, null));
                int totalWater = waterLogDao.getTotalWaterForDate(currentUserId, date);
                JSONObject res = new JSONObject();
                res.put("success", true);
                res.put("totalWaterMl", totalWater);
                sendJsonResponse(exchange, 200, res);
            } else if (path.equals("/api/water/reset") && "POST".equals(method)) {
                String date = queryParams.getOrDefault("date", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                waterLogDao.resetWaterForDate(currentUserId, date);
                JSONObject res = new JSONObject().put("success", true).put("totalWaterMl", 0);
                sendJsonResponse(exchange, 200, res);
            } else if (path.equals("/api/analytics/weekly") && "GET".equals(method)) {
                JSONObject weekly = mealLogService.getWeeklyTrends(currentUserId);
                sendJsonResponse(exchange, 200, weekly);
            } 
            // Chatbot Endpoints
            else if (path.equals("/api/chat") && "POST".equals(method)) {
                String body = readRequestBody(exchange);
                JSONObject json = new JSONObject(body);
                String message = json.getString("message");
                JSONObject botReply = chatbotService.processUserMessage(currentUserId, message);
                sendJsonResponse(exchange, 200, botReply);
            } else if (path.equals("/api/chat/history") && "GET".equals(method)) {
                List<ChatMessage> history = chatDao.getRecentMessages(currentUserId, 50);
                JSONArray arr = new JSONArray();
                for (ChatMessage m : history) arr.put(m.toJSON());
                JSONObject res = new JSONObject();
                res.put("history", arr);
                sendJsonResponse(exchange, 200, res);
            } else if (path.equals("/api/chat/history") && "DELETE".equals(method)) {
                chatDao.clearChatHistory(currentUserId);
                sendJsonResponse(exchange, 200, new JSONObject().put("success", true));
            } else if (path.equals("/api/health") && "GET".equals(method)) {
                sendJsonResponse(exchange, 200, new JSONObject().put("status", "UP").put("service", "NutriTracker Java REST Backend with Gemini AI"));
            } else {
                sendJsonResponse(exchange, 404, new JSONObject().put("error", "Endpoint not found: " + path));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JSONObject err = new JSONObject();
            err.put("error", e.getMessage());
            sendJsonResponse(exchange, 500, err);
        }
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, JSONObject json) throws IOException {
        String response = json.toString();
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-User-Id");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-User-Id");
        exchange.sendResponseHeaders(statusCode, bytes.length > 0 ? bytes.length : -1);
        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name());
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name());
                    params.put(key, value);
                } else if (idx < 0) {
                    params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8.name()), "");
                }
            } catch (UnsupportedEncodingException ignored) {}
        }
        return params;
    }
}
