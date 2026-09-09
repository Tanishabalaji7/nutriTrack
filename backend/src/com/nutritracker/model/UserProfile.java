package com.nutritracker.model;

import org.json.JSONObject;

public class UserProfile {
    private int id;
    private String name;
    private String email;
    private String password;
    private int age;
    private String gender; // "male", "female", "other"
    private double heightCm;
    private double weightKg;
    private String activityLevel; // "sedentary", "light", "moderate", "active", "very_active"
    private String goal; // "weight_loss", "maintain", "muscle_gain"
    private String dietaryPref; // "omnivore", "vegetarian", "vegan", "keto", "pescatarian"
    private int customCalorieTarget;
    private double waterGoalLiters;
    private double startingWeightKg;
    private double targetWeightKg;
    private String geminiApiKey;
    private String createdAt;

    public UserProfile() {
        this.id = 1;
        this.name = "Alex Morgan";
        this.email = "alex@example.com";
        this.password = "";
        this.age = 28;
        this.gender = "female";
        this.heightCm = 168.0;
        this.weightKg = 64.0;
        this.startingWeightKg = 67.0;
        this.targetWeightKg = 60.0;
        this.activityLevel = "moderate";
        this.goal = "maintain";
        this.dietaryPref = "omnivore";
        this.customCalorieTarget = 0; // 0 means auto-calculate via BMR/TDEE
        this.waterGoalLiters = 2.5;
        this.geminiApiKey = "";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public String getActivityLevel() { return activityLevel; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getDietaryPref() { return dietaryPref; }
    public void setDietaryPref(String dietaryPref) { this.dietaryPref = dietaryPref; }

    public double getStartingWeightKg() {
        if (startingWeightKg <= 0) {
            if ("weight_loss".equalsIgnoreCase(goal)) return weightKg + 3.0;
            if ("muscle_gain".equalsIgnoreCase(goal)) return Math.max(30.0, weightKg - 2.0);
            return weightKg;
        }
        return startingWeightKg;
    }
    public void setStartingWeightKg(double startingWeightKg) { this.startingWeightKg = startingWeightKg; }

    public double getTargetWeightKg() {
        if (targetWeightKg <= 0) {
            if ("weight_loss".equalsIgnoreCase(goal)) return Math.max(35.0, weightKg - 5.0);
            if ("muscle_gain".equalsIgnoreCase(goal)) return weightKg + 4.0;
            return weightKg;
        }
        return targetWeightKg;
    }
    public void setTargetWeightKg(double targetWeightKg) { this.targetWeightKg = targetWeightKg; }

    public int getCustomCalorieTarget() { return customCalorieTarget; }
    public void setCustomCalorieTarget(int customCalorieTarget) { this.customCalorieTarget = customCalorieTarget; }

    public double getWaterGoalLiters() { return waterGoalLiters; }
    public void setWaterGoalLiters(double waterGoalLiters) { this.waterGoalLiters = waterGoalLiters; }

    public String getGeminiApiKey() { return geminiApiKey; }
    public void setGeminiApiKey(String geminiApiKey) { this.geminiApiKey = geminiApiKey; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name != null ? name : "User");
        json.put("email", email != null ? email : "");
        json.put("age", age);
        json.put("gender", gender != null ? gender : "female");
        json.put("heightCm", heightCm);
        json.put("weightKg", weightKg);
        json.put("startingWeightKg", getStartingWeightKg());
        json.put("targetWeightKg", getTargetWeightKg());
        json.put("activityLevel", activityLevel != null ? activityLevel : "moderate");
        json.put("goal", goal != null ? goal : "maintain");
        json.put("dietaryPref", dietaryPref != null ? dietaryPref : "omnivore");
        json.put("customCalorieTarget", customCalorieTarget);
        json.put("waterGoalLiters", waterGoalLiters);
        json.put("geminiApiKey", geminiApiKey != null ? geminiApiKey : "");
        json.put("hasGeminiKey", geminiApiKey != null && !geminiApiKey.trim().isEmpty());
        json.put("createdAt", createdAt != null ? createdAt : "");
        return json;
    }

    public static UserProfile fromJSON(JSONObject json) {
        UserProfile p = new UserProfile();
        if (json.has("id")) p.setId(json.getInt("id"));
        if (json.has("name")) p.setName(json.getString("name"));
        if (json.has("email")) p.setEmail(json.getString("email"));
        if (json.has("password")) p.setPassword(json.getString("password"));
        if (json.has("age")) p.setAge(json.getInt("age"));
        if (json.has("gender")) p.setGender(json.getString("gender"));
        if (json.has("heightCm")) p.setHeightCm(json.getDouble("heightCm"));
        if (json.has("weightKg")) p.setWeightKg(json.getDouble("weightKg"));
        if (json.has("startingWeightKg")) p.setStartingWeightKg(json.getDouble("startingWeightKg"));
        if (json.has("targetWeightKg")) p.setTargetWeightKg(json.getDouble("targetWeightKg"));
        if (json.has("activityLevel")) p.setActivityLevel(json.getString("activityLevel"));
        if (json.has("goal")) p.setGoal(json.getString("goal"));
        if (json.has("dietaryPref")) p.setDietaryPref(json.getString("dietaryPref"));
        if (json.has("customCalorieTarget")) p.setCustomCalorieTarget(json.getInt("customCalorieTarget"));
        if (json.has("waterGoalLiters")) p.setWaterGoalLiters(json.getDouble("waterGoalLiters"));
        if (json.has("geminiApiKey")) p.setGeminiApiKey(json.getString("geminiApiKey"));
        return p;
    }
}
