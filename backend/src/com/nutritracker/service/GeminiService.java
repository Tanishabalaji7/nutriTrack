package com.nutritracker.service;

import com.nutritracker.model.ChatMessage;
import com.nutritracker.model.UserProfile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class GeminiService {
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String DEFAULT_MODEL = "gemini-3.7-flash";
    private static final String FALLBACK_MODEL = "gemini-2.5-flash";

    private final HttpClient httpClient;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    /**
     * Resolves the active Gemini API Key for a user, environment, or .env file
     */
    public String resolveApiKey(UserProfile user) {
        if (user != null && user.getGeminiApiKey() != null && !user.getGeminiApiKey().trim().isEmpty()) {
            return user.getGeminiApiKey().trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }
        String googleKey = System.getenv("GOOGLE_API_KEY");
        if (googleKey != null && !googleKey.trim().isEmpty()) {
            return googleKey.trim();
        }
        String propKey = System.getProperty("gemini.api.key");
        if (propKey != null && !propKey.trim().isEmpty()) {
            return propKey.trim();
        }

        // Check .env files in root or backend/
        try {
            java.io.File envFile = new java.io.File(".env");
            if (!envFile.exists()) envFile = new java.io.File("backend/.env");
            if (!envFile.exists()) envFile = new java.io.File("../.env");
            if (envFile.exists()) {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(envFile.toPath());
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("#") || line.isEmpty()) continue;
                    if (line.startsWith("GEMINI_API_KEY=") || line.startsWith("GOOGLE_API_KEY=")) {
                        String val = line.substring(line.indexOf('=') + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1).trim();
                        }
                        if (!val.isEmpty()) return val;
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Checks if Gemini API is configured
     */
    public boolean isConfigured(UserProfile user) {
        String key = resolveApiKey(user);
        return key != null && !key.isEmpty();
    }

    /**
     * Ask Gemini a nutritional / coaching question with full user context and chat history
     */
    public String askGemini(UserProfile user, String userMessage, JSONObject dailySummary, List<ChatMessage> recentHistory) {
        String apiKey = resolveApiKey(user);
        if (apiKey == null || apiKey.isEmpty()) {
            return null; // Signals caller to use internal rule engine or prompt user
        }

        try {
            // Build rich system instruction with user biometric data and daily nutrition status
            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("You are NutriBot AI, an empathetic, highly knowledgeable, and scientifically-grounded clinical and sports nutritionist. ");
            systemPrompt.append("You give personalized diet, meal, macro, micronutrient, and fitness guidance tailored to the user's biometric profile and goals. ");
            systemPrompt.append("Format your responses beautifully with Markdown (bold text, bullet points, headers, emojis) for easy reading.\n\n");

            if (user != null) {
                systemPrompt.append("USER BIOMETRIC PROFILE:\n");
                systemPrompt.append("- Name: ").append(user.getName()).append("\n");
                systemPrompt.append("- Age: ").append(user.getAge()).append(" years\n");
                systemPrompt.append("- Gender: ").append(user.getGender()).append("\n");
                systemPrompt.append("- Height: ").append(user.getHeightCm()).append(" cm | Weight: ").append(user.getWeightKg()).append(" kg\n");
                systemPrompt.append("- Activity Level: ").append(user.getActivityLevel()).append("\n");
                systemPrompt.append("- Fitness Goal: ").append(user.getGoal()).append(" (e.g. weight_loss, maintain, muscle_gain)\n");
                systemPrompt.append("- Dietary Preference: ").append(user.getDietaryPref()).append(" (e.g. omnivore, vegetarian, vegan, pescatarian)\n");
            }

            if (dailySummary != null && dailySummary.has("analysis")) {
                JSONObject analysis = dailySummary.getJSONObject("analysis");
                systemPrompt.append("\nTODAY'S LIVE NUTRITION INTAKE:\n");
                systemPrompt.append("- Consumed: ").append(Math.round(analysis.optDouble("consumedCalories", 0))).append(" kcal / Target: ").append(Math.round(analysis.optDouble("targetCalories", 2000))).append(" kcal\n");
                systemPrompt.append("- Remaining Calorie Budget: ").append(Math.round(analysis.optDouble("remainingCalories", 0))).append(" kcal\n");

                if (analysis.has("macros")) {
                    JSONObject macros = analysis.getJSONObject("macros");
                    if (macros.has("protein")) {
                        systemPrompt.append("- Protein Consumed: ").append(Math.round(macros.getJSONObject("protein").optDouble("consumed", 0))).append("g / ").append(Math.round(macros.getJSONObject("protein").optDouble("target", 100))).append("g\n");
                    }
                }
                if (dailySummary.has("waterSummary")) {
                    JSONObject water = dailySummary.getJSONObject("waterSummary");
                    systemPrompt.append("- Water Drank: ").append(water.optInt("totalConsumedMl", 0)).append("ml / Goal: ").append(water.optInt("goalMl", 2500)).append("ml\n");
                }
            }

            systemPrompt.append("\nKNOWLEDGE BASE NOTE:\n");
            systemPrompt.append("You have comprehensive mastery of global and regional cuisines including authentic multi-regional Indian food (Rotis, Parathas, Theplas, Millets like Ragi/Bajra/Jowar, Dals, Paneer dishes, Tandoori chicken/fish, Idli/Dosa/Sambar, Biryanis, Chaats, Chai, Lassi, Makhana, superfoods) as well as Western, Mediterranean, and Asian cuisines. When the user asks for meal ideas, recommendations, or cultural dietary advice, provide specific dishes, practical portion sizes, macros, and actionable tips.");

            // Build Request JSON
            JSONObject requestBody = new JSONObject();

            // 1. System instruction
            JSONObject systemInstruction = new JSONObject();
            JSONArray sysParts = new JSONArray();
            sysParts.put(new JSONObject().put("text", systemPrompt.toString()));
            systemInstruction.put("parts", sysParts);
            requestBody.put("systemInstruction", systemInstruction);

            // 2. Contents / Multi-turn history
            JSONArray contents = new JSONArray();

            if (recentHistory != null) {
                // Add up to last 6 messages for context
                int start = Math.max(0, recentHistory.size() - 6);
                for (int i = start; i < recentHistory.size(); i++) {
                    ChatMessage msg = recentHistory.get(i);
                    String role = "user".equalsIgnoreCase(msg.getSender()) ? "user" : "model";
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("role", role);
                    JSONArray parts = new JSONArray();
                    parts.put(new JSONObject().put("text", msg.getMessage()));
                    msgObj.put("parts", parts);
                    contents.put(msgObj);
                }
            }

            // Append current user message
            JSONObject currentMsg = new JSONObject();
            currentMsg.put("role", "user");
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", userMessage));
            currentMsg.put("parts", parts);
            contents.put(currentMsg);

            requestBody.put("contents", contents);

            // 3. Generation config (thinkingBudget 0 eliminates reasoning delay for sub-second responses)
            JSONObject genConfig = new JSONObject();
            genConfig.put("temperature", 0.7);
            genConfig.put("maxOutputTokens", 800);
            JSONObject thinkingConfig = new JSONObject();
            thinkingConfig.put("thinkingBudget", 0);
            genConfig.put("thinkingConfig", thinkingConfig);
            requestBody.put("generationConfig", genConfig);

            // Try fast verified models in priority order
            String[] modelsToTry = new String[]{
                "gemini-3.7-flash", 
                "gemini-3.6-flash", 
                "gemini-3.5-flash", 
                "gemini-flash-latest",
                "gemini-2.5-flash"
            };
            for (String model : modelsToTry) {
                String response = sendGeminiRequest(model, apiKey, requestBody.toString());
                if (response != null) {
                    String parsed = parseGeminiText(response);
                    if (parsed != null && !parsed.trim().isEmpty()) {
                        return parsed;
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Gemini API invocation error: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private String sendGeminiRequest(String model, String apiKey, String jsonPayload) {
        try {
            String url = GEMINI_API_BASE + model + ":generateContent?key=" + apiKey;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            } else {
                System.err.println("Gemini API returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            System.err.println("Error sending request to Gemini API: " + e.getMessage());
        }
        return null;
    }

    private String parseGeminiText(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            if (json.has("candidates")) {
                JSONArray candidates = json.getJSONArray("candidates");
                if (candidates.length() > 0) {
                    JSONObject firstCand = candidates.getJSONObject(0);
                    if (firstCand.has("content")) {
                        JSONObject content = firstCand.getJSONObject("content");
                        if (content.has("parts")) {
                            JSONArray parts = content.getJSONArray("parts");
                            if (parts.length() > 0) {
                                return parts.getJSONObject(0).getString("text");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Gemini response JSON: " + e.getMessage());
        }
        return null;
    }
}
