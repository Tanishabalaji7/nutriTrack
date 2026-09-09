package com.nutritracker.service;

import com.nutritracker.dao.ChatDao;
import com.nutritracker.dao.FoodDao;
import com.nutritracker.dao.UserDao;
import com.nutritracker.dao.WaterLogDao;
import com.nutritracker.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatbotService {
    private final ChatDao chatDao;
    private final UserDao userDao;
    private final FoodDao foodDao;
    private final MealLogService mealLogService;
    private final WaterLogDao waterLogDao;
    private final CalorieCalculatorService calorieService;
    private final AgeProfileAnalysisService ageAnalysisService;
    private final GeminiService geminiService;

    public ChatbotService() {
        this.chatDao = new ChatDao();
        this.userDao = new UserDao();
        this.foodDao = new FoodDao();
        this.mealLogService = new MealLogService();
        this.waterLogDao = new WaterLogDao();
        this.calorieService = new CalorieCalculatorService();
        this.ageAnalysisService = new AgeProfileAnalysisService();
        this.geminiService = new GeminiService();
    }

    public JSONObject processUserMessage(int userId, String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            JSONObject err = new JSONObject();
            err.put("reply", "I'm listening! Ask me any question about your nutrition, calories, meal suggestions, or tell me what you ate today.");
            return err;
        }

        String msg = rawMessage.trim();
        String lower = msg.toLowerCase();
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        UserProfile user = userDao.getUserProfile(userId);
        List<ChatMessage> recentHistory = chatDao.getRecentMessages(userId, 10);

        // Save User Message to SQLite
        chatDao.saveMessage(new ChatMessage(0, userId, "user", msg, "user_query", "{}", null));

        JSONObject botResponse = new JSONObject();
        String replyText = null;
        String actionType = "advice";
        JSONObject metadata = new JSONObject();
        JSONArray actionChips = new JSONArray();

        // -------------------------------------------------------------
        // 1. Check Hydration Logging Intent (e.g., "drank 500ml water", "log 250ml water", "water 300ml")
        // -------------------------------------------------------------
        Pattern waterPattern = Pattern.compile("(?:drank|log|add|had)?\\s*(\\d+)\\s*(?:ml|milliliters?|glass(?:es)?|cups?)\\s*(?:of)?\\s*water");
        Matcher waterMatcher = waterPattern.matcher(lower);
        if (waterMatcher.find() || (lower.contains("water") && lower.matches(".*\\b(\\d{2,4})\\s*ml.*"))) {
            int amount = 250;
            try {
                if (waterMatcher.find(0)) {
                    amount = Integer.parseInt(waterMatcher.group(1));
                    if (lower.contains("glass") || lower.contains("cup")) {
                        amount = amount * 250;
                    }
                } else {
                    Matcher m = Pattern.compile("(\\d{2,4})").matcher(lower);
                    if (m.find()) amount = Integer.parseInt(m.group(1));
                }
            } catch (Exception ignored) {}

            waterLogDao.addWaterLog(new WaterLog(0, userId, today, amount, null));
            int totalWater = waterLogDao.getTotalWaterForDate(userId, today);
            int goal = (int)(user.getWaterGoalLiters() * 1000);

            replyText = "💧 Great job! Logged **" + amount + "ml** of water for today. Total hydration is now **" + totalWater + "ml / " + goal + "ml** (" + Math.round(((double)totalWater / Math.max(1, goal)) * 100) + "% of your goal).";
            actionType = "logged_water";
            metadata.put("amountMl", amount);
            metadata.put("totalWaterMl", totalWater);
            actionChips.put("Log 250ml Water").put("How are my calories?").put("Suggest a healthy meal");
        }
        // -------------------------------------------------------------
        // 2. Check Natural Language Food Logging Intent (e.g. "I ate 2 rotis and palak paneer for lunch", "log 150g grilled chicken for lunch")
        // -------------------------------------------------------------
        else if (lower.startsWith("i ate ") || lower.startsWith("i had ") || lower.startsWith("log ") || lower.startsWith("ate ") || lower.contains(" for breakfast") || lower.contains(" for lunch") || lower.contains(" for dinner") || lower.contains(" for snack")) {
            JSONObject logResult = tryParseAndLogFood(userId, msg, today);
            if (logResult != null && logResult.getJSONArray("loggedItems").length() > 0) {
                JSONArray logged = logResult.getJSONArray("loggedItems");
                double totalCals = logResult.getDouble("totalCalories");
                double totalProt = logResult.getDouble("totalProtein");
                String meal = logResult.getString("mealType");

                StringBuilder sb = new StringBuilder();
                sb.append("✅ Successfully logged to **").append(capitalize(meal)).append("**:\n");
                for (int i = 0; i < logged.length(); i++) {
                    JSONObject item = logged.getJSONObject(i);
                    sb.append("• **").append(item.getString("name")).append("** (").append(item.getDouble("quantity")).append("x) — ")
                            .append(item.getDouble("calories")).append(" kcal, ")
                            .append(item.getDouble("protein")).append("g protein\n");
                }
                sb.append("\n📊 **Meal Totals:** **").append(Math.round(totalCals)).append(" kcal** | **").append(Math.round(totalProt)).append("g Protein**.");

                // If Gemini is available, add a quick smart AI commentary
                if (geminiService.isConfigured(user)) {
                    JSONObject dailySum = mealLogService.getDailySummary(userId, today);
                    String aiTip = geminiService.askGemini(user, "I just logged " + msg + ". In 2 sentences, give quick clinical feedback on this meal's nutritional value and how it fits my goals.", dailySum, null);
                    if (aiTip != null && !aiTip.trim().isEmpty()) {
                        sb.append("\n\n💡 **Gemini Nutritionist Insight:**\n").append(aiTip.trim());
                    }
                }

                replyText = sb.toString();
                actionType = "logged_food";
                metadata.put("loggedItems", logged);
                metadata.put("mealType", meal);
                metadata.put("totalCalories", totalCals);
                actionChips.put("Show daily summary").put("How many calories left?").put("Suggest high protein snack");
            } else {
                replyText = "I heard you want to log food, but I couldn't match the exact items in our verified database. Try specifying like: *\"Log 2 Rotis and Palak Paneer for lunch\"* or use the Quick Log button!";
                actionType = "advice";
                actionChips.put("Search Food Database").put("Show My Logs").put("Suggest a healthy meal");
            }
        }
        // -------------------------------------------------------------
        // 3. Open-Ended Questions: Check Gemini AI First for Unlimited Intelligence
        // -------------------------------------------------------------
        else {
            JSONObject dailySum = mealLogService.getDailySummary(userId, today);

            // If Gemini is configured, invoke Gemini with full multi-turn context
            if (geminiService.isConfigured(user)) {
                String geminiAnswer = geminiService.askGemini(user, msg, dailySum, recentHistory);
                if (geminiAnswer != null && !geminiAnswer.trim().isEmpty()) {
                    replyText = geminiAnswer.trim();
                    actionType = "gemini_ai";
                    actionChips.put("How are my calories today?").put("Suggest a healthy recipe").put("Log 250ml Water").put("Tell me more");
                }
            }

            // Fallback to built-in clinical nutrition & NLP engine if Gemini not configured or request fails
            if (replyText == null) {
                replyText = handleBuiltInNutritionQueries(lower, msg, user, today, dailySum, metadata, actionChips);
            }
        }

        botResponse.put("reply", replyText);
        botResponse.put("actionType", actionType);
        botResponse.put("metadata", metadata);
        botResponse.put("actionChips", actionChips);

        // Persist Bot Message in SQLite
        chatDao.saveMessage(new ChatMessage(0, userId, "bot", replyText, actionType, metadata.toString(), null));

        return botResponse;
    }

    private String handleBuiltInNutritionQueries(String lower, String msg, UserProfile user, String today, JSONObject dailySum, JSONObject metadata, JSONArray actionChips) {
        String replyText;

        // Daily Calorie / Deficit / Progress Status
        if (lower.contains("calories left") || lower.contains("deficit") || lower.contains("how am i doing") || lower.contains("status") || lower.contains("progress") || lower.contains("summary")) {
            JSONObject analysis = dailySum.getJSONObject("analysis");
            double targetCal = analysis.getDouble("targetCalories");
            double consumedCal = analysis.getDouble("consumedCalories");
            double remCal = analysis.getDouble("remainingCalories");
            int score = analysis.getInt("nutritionScore");
            String grade = analysis.getString("scoreGrade");

            JSONObject macros = analysis.getJSONObject("macros");
            double protEaten = macros.getJSONObject("protein").getDouble("consumed");
            double protTarget = macros.getJSONObject("protein").getDouble("target");

            replyText = "📊 **Your Daily Nutrition Status (" + today + "):**\n\n" +
                    "🔥 **Calories:** " + Math.round(consumedCal) + " / " + Math.round(targetCal) + " kcal (" + (remCal >= 0 ? Math.round(remCal) + " kcal remaining" : Math.abs(Math.round(remCal)) + " kcal over target") + ")\n" +
                    "🥩 **Protein:** " + Math.round(protEaten) + "g / " + Math.round(protTarget) + "g (" + Math.round((protEaten / Math.max(1, protTarget)) * 100) + "%)\n" +
                    "⭐ **Nutrition Quality Score:** **" + score + "/100 (Grade " + grade + ")**\n\n";

            JSONArray alerts = analysis.getJSONArray("healthAlerts");
            if (alerts.length() > 0) {
                replyText += "🔔 **Observation:** " + alerts.getString(0) + "\n\n";
            } else {
                replyText += "✨ You are well-aligned with your daily targets!\n\n";
            }
            replyText += "💡 *Tip: Connect your free Google Gemini API key in Settings to unlock deep conversational AI answering any question.*";
            actionChips.put("Suggest healthy dinner").put("Log 250ml Water").put("High protein Indian foods");
        }
        // Recipe & Meal Suggestions (including Indian & global dishes)
        else if (lower.contains("suggest") || lower.contains("recipe") || lower.contains("what should i eat") || lower.contains("meal idea") || lower.contains("dinner idea") || lower.contains("breakfast idea") || lower.contains("lunch idea") || lower.contains("snack idea")) {
            JSONObject mealSuggestion = generateMealSuggestion(lower, user);
            replyText = mealSuggestion.getString("text");
            metadata.put("recipe", mealSuggestion.getJSONObject("details"));
            actionChips.put("Suggest another recipe").put("Log this meal").put("How many calories left?");
        }
        // Specific Food Nutrition Information Queries
        else if (lower.contains("calories in") || lower.contains("nutrition of") || lower.contains("tell me about") || (lower.contains("is ") && (lower.contains("healthy") || lower.contains("good")))) {
            String query = lower.replaceAll("how many calories in|nutrition of|tell me about|is|healthy|good for you|\\?", "").trim();
            FoodItem food = foodDao.findBestMatchByName(query);
            if (food != null) {
                replyText = "🥑 **Nutritional Profile: " + food.getName() + "** (per " + food.getServingSize() + " " + food.getServingUnit() + "):\n\n" +
                        "• **Calories:** " + food.getCalories() + " kcal\n" +
                        "• **Protein:** " + food.getProteinG() + "g\n" +
                        "• **Carbs:** " + food.getCarbsG() + "g (Fiber: " + food.getFiberG() + "g, Sugar: " + food.getSugarG() + "g)\n" +
                        "• **Fats:** " + food.getFatG() + "g\n" +
                        "• **Micros:** Calcium: " + food.getCalciumMg() + "mg | Iron: " + food.getIronMg() + "mg | Potassium: " + food.getPotassiumMg() + "mg | Vit D: " + food.getVitaminDMcg() + "mcg\n\n" +
                        "💡 *Category:* " + food.getCategory();
                metadata.put("food", food.toJSON());
                actionChips.put("Log " + food.getName()).put("Suggest a recipe with this").put("Check my daily deficit");
            } else {
                replyText = "I couldn't find exact nutrition for \"" + query + "\". You can search our database of 200+ verified whole foods (including complete Indian and global cuisines) or add a custom food item!";
                actionChips.put("Search Foods").put("Show High Protein Foods").put("Suggest Indian Dinner");
            }
        }
        // Age Profile Advice
        else if (lower.contains("age") || lower.contains("years old") || lower.contains("bracket") || lower.contains("senior") || lower.contains("teen") || lower.contains("child") || lower.contains("elderly")) {
            AgeNutritionRule rule = AgeNutritionRule.getRuleForAge(user.getAge());
            replyText = "🧬 **Age-Specific Nutrition Profile (" + rule.getBracketName() + " - " + user.getAge() + " yrs)**\n\n" +
                    "🎯 **Physiological Focus:** " + rule.getPhysiologicalFocus() + "\n\n" +
                    "📌 **Key Priority Nutrients:**\n";
            for (String nut : rule.getPriorityNutrients()) {
                replyText += "• " + nut + "\n";
            }
            replyText += "\n🥗 **Recommended Foods:** " + String.join(", ", rule.getRecommendedFoods()) + "\n\n" +
                    "⚠️ **Age Risks to Monitor:** " + String.join(" ", rule.getAgeSpecificRisks());
            metadata.put("ageRule", rule.toJSON());
            actionChips.put("Check my calcium & iron").put("How are my macros?").put("Suggest meal for my age");
        }
        // General Coaching & Greeting
        else {
            replyText = "Hello! I am **NutriBot AI**, your smart personal nutritionist and diet coach. 🍏\n\n" +
                    "Here are things I can do for you:\n" +
                    "1. 🍽️ **Log food naturally** — *\"I had 2 rotis and palak paneer for lunch\"*\n" +
                    "2. 🤖 **Gemini AI Intelligence** — *Connect your Gemini API key in Settings for open-ended answers to any nutrition, cooking, workout, or medical dietary question!*\n" +
                    "3. 🥗 **Indian & Global Meal Ideas** — *\"Suggest a high-protein vegetarian Indian dinner under 500 kcal\"*\n" +
                    "4. 📊 **Track Deficit & BMR** — *\"How are my calories and protein today?\"*\n" +
                    "5. 💧 **Track Hydration** — *\"Log 500ml water\"*\n\n" +
                    "How can I help you today?";
            actionChips.put("How are my calories?").put("Suggest high-protein dinner").put("Log 250ml Water").put("Configure Gemini API");
        }

        return replyText;
    }

    private JSONObject tryParseAndLogFood(int userId, String msg, String date) {
        String lower = msg.toLowerCase();
        String mealType = "snack";
        if (lower.contains("breakfast")) mealType = "breakfast";
        else if (lower.contains("lunch")) mealType = "lunch";
        else if (lower.contains("dinner")) mealType = "dinner";

        // Clean message
        String cleanMsg = lower
                .replaceAll("^i (?:ate|had|consumed|took|drank)\\s+", "")
                .replaceAll("^log\\s+", "")
                .replaceAll("^ate\\s+", "")
                .replaceAll("\\s+for (?:breakfast|lunch|dinner|snack|supper).*", "")
                .replaceAll("\\s+today.*", "")
                .trim();

        // Split into food clauses (e.g. "2 rotis and palak paneer" -> ["2 rotis", "palak paneer"])
        String[] segments = cleanMsg.split(",|\\band\\b|\\bwith\\b|\\+|&");
        List<FoodItem> allFoods = foodDao.getAllFoods();
        List<FoodItem> matchedFoods = new ArrayList<>();
        List<Double> quantities = new ArrayList<>();

        for (String seg : segments) {
            String cleanSeg = seg.trim();
            if (cleanSeg.length() < 2) continue;

            FoodItem bestFood = null;
            int bestScore = 0;

            for (FoodItem f : allFoods) {
                int score = computeMatchScore(cleanSeg, f);
                if (score > bestScore) {
                    bestScore = score;
                    bestFood = f;
                }
            }

            if (bestFood != null && bestScore >= 5) {
                if (!matchedFoods.contains(bestFood)) {
                    double qty = 1.0;
                    // Extract quantity from segment (e.g. "2 rotis", "150g chicken", "2 eggs")
                    Matcher qm = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:g|grams|pieces?|eggs?|slices?|servings?|cups?|bowls?|rotis?|chapatis?|dosas?|idlis?|puris?)?").matcher(cleanSeg);
                    if (qm.find()) {
                        try {
                            double val = Double.parseDouble(qm.group(1));
                            if (val > 10 && bestFood.getServingSize() > 0) {
                                qty = val / bestFood.getServingSize();
                            } else if (val > 0) {
                                qty = val;
                            }
                        } catch (Exception ignored) {}
                    }

                    matchedFoods.add(bestFood);
                    quantities.add(Math.round(qty * 10.0) / 10.0);
                }
            }
        }

        if (matchedFoods.isEmpty()) {
            return null;
        }

        JSONArray loggedArray = new JSONArray();
        double totalCal = 0;
        double totalProt = 0;

        for (int i = 0; i < matchedFoods.size(); i++) {
            FoodItem food = matchedFoods.get(i);
            double qty = quantities.get(i);

            mealLogService.logFoodItem(userId, food.getId(), mealType, qty, date, "", "Logged via NutriBot AI");

            double itemCal = Math.round(food.getCalories() * qty * 10.0) / 10.0;
            double itemProt = Math.round(food.getProteinG() * qty * 10.0) / 10.0;
            totalCal += itemCal;
            totalProt += itemProt;

            JSONObject itemJson = new JSONObject();
            itemJson.put("id", food.getId());
            itemJson.put("name", food.getName());
            itemJson.put("quantity", qty);
            itemJson.put("calories", itemCal);
            itemJson.put("protein", itemProt);
            loggedArray.put(itemJson);
        }

        JSONObject result = new JSONObject();
        result.put("mealType", mealType);
        result.put("loggedItems", loggedArray);
        result.put("totalCalories", Math.round(totalCal * 10.0) / 10.0);
        result.put("totalProtein", Math.round(totalProt * 10.0) / 10.0);
        return result;
    }

    private int computeMatchScore(String segment, FoodItem food) {
        String seg = segment.toLowerCase().replaceAll("[0-9]", "").trim();
        String foodName = food.getName().toLowerCase();
        int score = 0;

        // Exact match
        if (foodName.equals(seg)) return 100;

        // Check aliases in food name (e.g. "Whole Wheat Roti / Chapati" -> ["whole wheat roti", "chapati"])
        String[] aliases = foodName.split("[/()]");
        for (String alias : aliases) {
            String cleanAlias = alias.trim();
            if (cleanAlias.isEmpty()) continue;
            if (seg.equals(cleanAlias)) return 80;
            if (seg.contains(cleanAlias)) score = Math.max(score, 60);
            if (cleanAlias.contains(seg) && seg.length() >= 3) score = Math.max(score, 50);
        }

        // Specific Indian staples default rules
        if (seg.contains("roti") || seg.contains("chapati")) {
            if (foodName.contains("whole wheat roti")) score = Math.max(score, 45);
            else if (seg.contains("bajra") && foodName.contains("bajra roti")) score = Math.max(score, 70);
            else if (seg.contains("jowar") && foodName.contains("jowar roti")) score = Math.max(score, 70);
            else if (seg.contains("tandoori") && foodName.contains("tandoori roti")) score = Math.max(score, 70);
        }
        if (seg.contains("palak paneer") && foodName.contains("palak paneer")) score = Math.max(score, 75);
        if (seg.contains("paneer butter") && foodName.contains("paneer butter")) score = Math.max(score, 75);
        if (seg.contains("chicken tikka") && foodName.contains("chicken tikka")) score = Math.max(score, 75);
        if (seg.contains("butter chicken") && foodName.contains("butter chicken")) score = Math.max(score, 75);
        if (seg.contains("biryani") && foodName.contains("biryani")) {
            if (seg.contains("chicken") && foodName.contains("chicken dum biryani")) score = Math.max(score, 80);
            else if (seg.contains("mutton") && foodName.contains("mutton dum biryani")) score = Math.max(score, 80);
            else if (foodName.contains("vegetable dum biryani")) score = Math.max(score, 45);
        }
        if (seg.contains("dosa") && foodName.contains("dosa")) {
            if (seg.contains("masala") && foodName.contains("masala dosa")) score = Math.max(score, 75);
            else if (foodName.contains("crispy plain dosa")) score = Math.max(score, 45);
        }
        if (seg.contains("idli") && foodName.contains("idli")) score = Math.max(score, 50);
        if (seg.contains("chai") || seg.contains("tea")) {
            if (foodName.contains("masala chai")) score = Math.max(score, 50);
        }
        if (seg.contains("lassi") && foodName.contains("lassi")) score = Math.max(score, 50);
        if (seg.contains("samosa") && foodName.contains("samosa")) score = Math.max(score, 60);

        return score;
    }

    private JSONObject generateMealSuggestion(String query, UserProfile user) {
        JSONObject result = new JSONObject();
        JSONObject details = new JSONObject();

        boolean isHighProtein = query.contains("protein") || "muscle_gain".equals(user.getGoal());
        boolean isIndian = query.contains("indian") || query.contains("roti") || query.contains("paneer") || query.contains("dal") || query.contains("dosa") || query.contains("biryani") || query.contains("curry");
        boolean isVeg = "vegetarian".equals(user.getDietaryPref()) || "vegan".equals(user.getDietaryPref()) || query.contains("veg");

        String title;
        String desc;
        int calories;
        int protein;
        int carbs;
        int fat;
        List<String> ingredients = new ArrayList<>();

        if (isIndian && isVeg) {
            title = "High-Protein Palak Paneer with Jowar Millet Roti & Sprouted Salad";
            desc = "Authentic North-Indian superfood meal loaded with fresh spinach iron, 480mg paneer calcium, low-glycemic millet fiber, and fresh lemon-dressed sprouts.";
            calories = 430;
            protein = 25;
            carbs = 38;
            fat = 21;
            ingredients.add("180g Fresh Palak Paneer (Spinach & Cottage Cheese)");
            ingredients.add("1 Jowar Roti (Sorghum Millet Flatbread)");
            ingredients.add("1/2 cup Sprouted Moong Chaat with Cucumber & Lemon");
            ingredients.add("1 glass Salted Masala Chaas / Buttermilk");
        } else if (isIndian && !isVeg) {
            title = "Tandoori Chicken Tikka with Steamed Basmati Rice & Cucumber Raita";
            desc = "High-leucine muscle-building Indian dinner featuring lean yogurt-marinated grilled chicken breast, fragrant cumin basmati rice, and cooling probiotic curd.";
            calories = 490;
            protein = 44;
            carbs = 48;
            fat = 13;
            ingredients.add("180g Tandoori Chicken Tikka (6 skewers)");
            ingredients.add("100g Steamed Basmati Rice with Cumin");
            ingredients.add("1 bowl Cucumber Mint Dahi Raita");
            ingredients.add("Fresh Onion Slices with Squeezed Lemon");
        } else if (isVeg) {
            title = "Mediterranean Quinoa Power Bowl with Crispy Tofu";
            desc = "A complete plant-based protein bowl loaded with organic quinoa, crispy garlic tofu, baby spinach, cherry tomatoes, and tahini drizzle.";
            calories = 420;
            protein = 24;
            carbs = 48;
            fat = 15;
            ingredients.add("150g Organic Firm Tofu (Cubed & pan-seared)");
            ingredients.add("100g Cooked Quinoa");
            ingredients.add("1 cup Fresh Baby Spinach");
            ingredients.add("1 tbsp Extra Virgin Olive Oil / Tahini");
        } else if (isHighProtein) {
            title = "Herb-Crusted Salmon & Roasted Asparagus Bowl";
            desc = "Rich in EPA/DHA Omega-3 fatty acids, high-leucine protein for muscle preservation, and potassium for vascular health.";
            calories = 480;
            protein = 38;
            carbs = 26;
            fat = 22;
            ingredients.add("150g Atlantic Salmon (Baked with dill & lemon)");
            ingredients.add("100g Baked Sweet Potato");
            ingredients.add("120g Roasted Asparagus");
            ingredients.add("1 tsp Olive Oil");
        } else {
            title = "Avocado Toast with Poached Eggs & Chia Sprinkles";
            desc = "Balanced morning fuel with healthy monounsaturated fats, sustained energy complex carbs, and high-bioavailability egg protein.";
            calories = 360;
            protein = 18;
            carbs = 30;
            fat = 19;
            ingredients.add("2 slices 100% Whole Wheat Bread");
            ingredients.add("1/2 Fresh Hass Avocado (Mashed)");
            ingredients.add("2 Poached Free-Range Eggs");
            ingredients.add("1 tsp Chia Seeds & Red Pepper Flakes");
        }

        details.put("title", title);
        details.put("description", desc);
        details.put("calories", calories);
        details.put("protein", protein);
        details.put("carbs", carbs);
        details.put("fat", fat);
        details.put("ingredients", new JSONArray(ingredients));

        String text = "👨‍🍳 **Recommended Recipe: " + title + "**\n\n" +
                desc + "\n\n" +
                "🔥 **Macros:** " + calories + " kcal | **Protein:** " + protein + "g | **Carbs:** " + carbs + "g | **Fats:** " + fat + "g\n\n" +
                "📝 **Ingredients:**\n";
        for (String ing : ingredients) {
            text += "• " + ing + "\n";
        }

        result.put("text", text);
        result.put("details", details);
        return result;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
