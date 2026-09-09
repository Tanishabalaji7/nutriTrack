package com.nutritracker.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class NutritionAnalysis {
    private double bmr;
    private double tdee;
    private double targetCalories;
    private double consumedCalories;
    private double remainingCalories;

    // Macros Target & Consumed
    private double targetProteinG;
    private double consumedProteinG;
    private double targetCarbsG;
    private double consumedCarbsG;
    private double targetFatG;
    private double consumedFatG;

    // Micros Consumed & Target
    private double targetFiberG;
    private double consumedFiberG;
    private double targetCalciumMg;
    private double consumedCalciumMg;
    private double targetIronMg;
    private double consumedIronMg;
    private double targetSodiumMgMax;
    private double consumedSodiumMg;
    private double targetPotassiumMg;
    private double consumedPotassiumMg;
    private double targetVitaminCMg;
    private double consumedVitaminCMg;
    private double targetVitaminDMcg;
    private double consumedVitaminDMcg;
    private double targetVitaminB12Mcg;
    private double consumedVitaminB12Mcg;

    private int nutritionScore; // 0 - 100
    private String scoreGrade; // A+, A, B, C, D
    private AgeNutritionRule ageRule;
    private List<String> healthAlerts;
    private List<String> recommendations;

    public NutritionAnalysis() {
        this.healthAlerts = new ArrayList<>();
        this.recommendations = new ArrayList<>();
    }

    // Getters and setters
    public double getBmr() { return bmr; }
    public void setBmr(double bmr) { this.bmr = bmr; }

    public double getTdee() { return tdee; }
    public void setTdee(double tdee) { this.tdee = tdee; }

    public double getTargetCalories() { return targetCalories; }
    public void setTargetCalories(double targetCalories) { this.targetCalories = targetCalories; }

    public double getConsumedCalories() { return consumedCalories; }
    public void setConsumedCalories(double consumedCalories) { this.consumedCalories = consumedCalories; }

    public double getRemainingCalories() { return remainingCalories; }
    public void setRemainingCalories(double remainingCalories) { this.remainingCalories = remainingCalories; }

    public double getTargetProteinG() { return targetProteinG; }
    public void setTargetProteinG(double targetProteinG) { this.targetProteinG = targetProteinG; }

    public double getConsumedProteinG() { return consumedProteinG; }
    public void setConsumedProteinG(double consumedProteinG) { this.consumedProteinG = consumedProteinG; }

    public double getTargetCarbsG() { return targetCarbsG; }
    public void setTargetCarbsG(double targetCarbsG) { this.targetCarbsG = targetCarbsG; }

    public double getConsumedCarbsG() { return consumedCarbsG; }
    public void setConsumedCarbsG(double consumedCarbsG) { this.consumedCarbsG = consumedCarbsG; }

    public double getTargetFatG() { return targetFatG; }
    public void setTargetFatG(double targetFatG) { this.targetFatG = targetFatG; }

    public double getConsumedFatG() { return consumedFatG; }
    public void setConsumedFatG(double consumedFatG) { this.consumedFatG = consumedFatG; }

    public double getTargetFiberG() { return targetFiberG; }
    public void setTargetFiberG(double targetFiberG) { this.targetFiberG = targetFiberG; }

    public double getConsumedFiberG() { return consumedFiberG; }
    public void setConsumedFiberG(double consumedFiberG) { this.consumedFiberG = consumedFiberG; }

    public double getTargetCalciumMg() { return targetCalciumMg; }
    public void setTargetCalciumMg(double targetCalciumMg) { this.targetCalciumMg = targetCalciumMg; }

    public double getConsumedCalciumMg() { return consumedCalciumMg; }
    public void setConsumedCalciumMg(double consumedCalciumMg) { this.consumedCalciumMg = consumedCalciumMg; }

    public double getTargetIronMg() { return targetIronMg; }
    public void setTargetIronMg(double targetIronMg) { this.targetIronMg = targetIronMg; }

    public double getConsumedIronMg() { return consumedIronMg; }
    public void setConsumedIronMg(double consumedIronMg) { this.consumedIronMg = consumedIronMg; }

    public double getTargetSodiumMgMax() { return targetSodiumMgMax; }
    public void setTargetSodiumMgMax(double targetSodiumMgMax) { this.targetSodiumMgMax = targetSodiumMgMax; }

    public double getConsumedSodiumMg() { return consumedSodiumMg; }
    public void setConsumedSodiumMg(double consumedSodiumMg) { this.consumedSodiumMg = consumedSodiumMg; }

    public double getTargetPotassiumMg() { return targetPotassiumMg; }
    public void setTargetPotassiumMg(double targetPotassiumMg) { this.targetPotassiumMg = targetPotassiumMg; }

    public double getConsumedPotassiumMg() { return consumedPotassiumMg; }
    public void setConsumedPotassiumMg(double consumedPotassiumMg) { this.consumedPotassiumMg = consumedPotassiumMg; }

    public double getTargetVitaminCMg() { return targetVitaminCMg; }
    public void setTargetVitaminCMg(double targetVitaminCMg) { this.targetVitaminCMg = targetVitaminCMg; }

    public double getConsumedVitaminCMg() { return consumedVitaminCMg; }
    public void setConsumedVitaminCMg(double consumedVitaminCMg) { this.consumedVitaminCMg = consumedVitaminCMg; }

    public double getTargetVitaminDMcg() { return targetVitaminDMcg; }
    public void setTargetVitaminDMcg(double targetVitaminDMcg) { this.targetVitaminDMcg = targetVitaminDMcg; }

    public double getConsumedVitaminDMcg() { return consumedVitaminDMcg; }
    public void setConsumedVitaminDMcg(double consumedVitaminDMcg) { this.consumedVitaminDMcg = consumedVitaminDMcg; }

    public double getTargetVitaminB12Mcg() { return targetVitaminB12Mcg; }
    public void setTargetVitaminB12Mcg(double targetVitaminB12Mcg) { this.targetVitaminB12Mcg = targetVitaminB12Mcg; }

    public double getConsumedVitaminB12Mcg() { return consumedVitaminB12Mcg; }
    public void setConsumedVitaminB12Mcg(double consumedVitaminB12Mcg) { this.consumedVitaminB12Mcg = consumedVitaminB12Mcg; }

    public int getNutritionScore() { return nutritionScore; }
    public void setNutritionScore(int nutritionScore) { this.nutritionScore = nutritionScore; }

    public String getScoreGrade() { return scoreGrade; }
    public void setScoreGrade(String scoreGrade) { this.scoreGrade = scoreGrade; }

    public AgeNutritionRule getAgeRule() { return ageRule; }
    public void setAgeRule(AgeNutritionRule ageRule) { this.ageRule = ageRule; }

    public List<String> getHealthAlerts() { return healthAlerts; }
    public void setHealthAlerts(List<String> healthAlerts) { this.healthAlerts = healthAlerts; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("bmr", Math.round(bmr));
        json.put("tdee", Math.round(tdee));
        json.put("targetCalories", Math.round(targetCalories));
        json.put("consumedCalories", Math.round(consumedCalories * 10.0) / 10.0);
        json.put("remainingCalories", Math.round(remainingCalories * 10.0) / 10.0);

        JSONObject macros = new JSONObject();
        macros.put("protein", new JSONObject().put("target", Math.round(targetProteinG)).put("consumed", Math.round(consumedProteinG * 10.0) / 10.0));
        macros.put("carbs", new JSONObject().put("target", Math.round(targetCarbsG)).put("consumed", Math.round(consumedCarbsG * 10.0) / 10.0));
        macros.put("fat", new JSONObject().put("target", Math.round(targetFatG)).put("consumed", Math.round(consumedFatG * 10.0) / 10.0));
        json.put("macros", macros);

        JSONObject micros = new JSONObject();
        micros.put("fiber", new JSONObject().put("target", targetFiberG).put("consumed", Math.round(consumedFiberG * 10.0) / 10.0).put("unit", "g"));
        micros.put("calcium", new JSONObject().put("target", targetCalciumMg).put("consumed", Math.round(consumedCalciumMg * 10.0) / 10.0).put("unit", "mg"));
        micros.put("iron", new JSONObject().put("target", targetIronMg).put("consumed", Math.round(consumedIronMg * 10.0) / 10.0).put("unit", "mg"));
        micros.put("sodium", new JSONObject().put("targetMax", targetSodiumMgMax).put("consumed", Math.round(consumedSodiumMg * 10.0) / 10.0).put("unit", "mg"));
        micros.put("potassium", new JSONObject().put("target", targetPotassiumMg).put("consumed", Math.round(consumedPotassiumMg * 10.0) / 10.0).put("unit", "mg"));
        micros.put("vitaminC", new JSONObject().put("target", targetVitaminCMg).put("consumed", Math.round(consumedVitaminCMg * 10.0) / 10.0).put("unit", "mg"));
        micros.put("vitaminD", new JSONObject().put("target", targetVitaminDMcg).put("consumed", Math.round(consumedVitaminDMcg * 10.0) / 10.0).put("unit", "mcg"));
        micros.put("vitaminB12", new JSONObject().put("target", targetVitaminB12Mcg).put("consumed", Math.round(consumedVitaminB12Mcg * 10.0) / 10.0).put("unit", "mcg"));
        json.put("micros", micros);

        json.put("nutritionScore", nutritionScore);
        json.put("scoreGrade", scoreGrade != null ? scoreGrade : "B+");
        if (ageRule != null) {
            json.put("ageRule", ageRule.toJSON());
        }
        json.put("healthAlerts", new JSONArray(healthAlerts));
        json.put("recommendations", new JSONArray(recommendations));

        return json;
    }
}
