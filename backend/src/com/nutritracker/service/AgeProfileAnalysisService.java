package com.nutritracker.service;

import com.nutritracker.model.AgeNutritionRule;
import com.nutritracker.model.MealLog;
import com.nutritracker.model.NutritionAnalysis;
import com.nutritracker.model.UserProfile;

import java.util.ArrayList;
import java.util.List;

public class AgeProfileAnalysisService {
    private final CalorieCalculatorService calorieCalculatorService;

    public AgeProfileAnalysisService() {
        this.calorieCalculatorService = new CalorieCalculatorService();
    }

    public NutritionAnalysis performFullAnalysis(UserProfile profile, List<MealLog> dailyLogs) {
        NutritionAnalysis analysis = new NutritionAnalysis();

        // 1. Calculate BMR and TDEE
        double bmr = calorieCalculatorService.calculateBMR(profile);
        double tdee = calorieCalculatorService.calculateTDEE(profile);
        double targetCalories = calorieCalculatorService.calculateTargetCalories(profile);
        double targetProtein = calorieCalculatorService.calculateTargetProtein(profile);
        double targetFat = calorieCalculatorService.calculateTargetFat(profile, targetCalories);
        double targetCarbs = calorieCalculatorService.calculateTargetCarbs(profile, targetCalories, targetProtein, targetFat);

        analysis.setBmr(bmr);
        analysis.setTdee(tdee);
        analysis.setTargetCalories(targetCalories);
        analysis.setTargetProteinG(targetProtein);
        analysis.setTargetFatG(targetFat);
        analysis.setTargetCarbsG(targetCarbs);

        // 2. Fetch Age Nutrition Rule
        AgeNutritionRule ageRule = AgeNutritionRule.getRuleForAge(profile.getAge());
        analysis.setAgeRule(ageRule);

        // Set Micro Targets from Age Rule
        analysis.setTargetFiberG(ageRule.getFiberGrams());
        analysis.setTargetCalciumMg(ageRule.getCalciumMg());
        analysis.setTargetIronMg(ageRule.getIronMg(profile.getGender()));
        analysis.setTargetSodiumMgMax(ageRule.getSodiumMaxMg());
        analysis.setTargetPotassiumMg(ageRule.getPotassiumMg());
        analysis.setTargetVitaminCMg(profile.getAge() <= 18 ? 65.0 : 90.0);
        analysis.setTargetVitaminDMcg(ageRule.getVitaminDMcg());
        analysis.setTargetVitaminB12Mcg(ageRule.getVitaminB12Mcg());

        // 3. Aggregate Consumed Values from Daily Logs
        double eatenCals = 0;
        double eatenProt = 0;
        double eatenCarbs = 0;
        double eatenFat = 0;
        double eatenFiber = 0;
        double eatenCalcium = 0;
        double eatenIron = 0;
        double eatenSodium = 0;
        double eatenPotassium = 0;
        double eatenVitC = 0;
        double eatenVitD = 0;
        double eatenVitB12 = 0;

        for (MealLog log : dailyLogs) {
            if (log.getFoodItem() != null) {
                double q = log.getQuantity();
                eatenCals += log.getFoodItem().getCalories() * q;
                eatenProt += log.getFoodItem().getProteinG() * q;
                eatenCarbs += log.getFoodItem().getCarbsG() * q;
                eatenFat += log.getFoodItem().getFatG() * q;
                eatenFiber += log.getFoodItem().getFiberG() * q;
                eatenCalcium += log.getFoodItem().getCalciumMg() * q;
                eatenIron += log.getFoodItem().getIronMg() * q;
                eatenSodium += log.getFoodItem().getSodiumMg() * q;
                eatenPotassium += log.getFoodItem().getPotassiumMg() * q;
                eatenVitC += log.getFoodItem().getVitaminCMg() * q;
                eatenVitD += log.getFoodItem().getVitaminDMcg() * q;
                eatenVitB12 += log.getFoodItem().getVitaminB12Mcg() * q;
            }
        }

        analysis.setConsumedCalories(eatenCals);
        analysis.setRemainingCalories(targetCalories - eatenCals);
        analysis.setConsumedProteinG(eatenProt);
        analysis.setConsumedCarbsG(eatenCarbs);
        analysis.setConsumedFatG(eatenFat);
        analysis.setConsumedFiberG(eatenFiber);
        analysis.setConsumedCalciumMg(eatenCalcium);
        analysis.setConsumedIronMg(eatenIron);
        analysis.setConsumedSodiumMg(eatenSodium);
        analysis.setConsumedPotassiumMg(eatenPotassium);
        analysis.setConsumedVitaminCMg(eatenVitC);
        analysis.setConsumedVitaminDMcg(eatenVitD);
        analysis.setConsumedVitaminB12Mcg(eatenVitB12);

        // 4. Compute Health Alerts and Physiological Insights
        List<String> alerts = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        int age = profile.getAge();

        // Calorie Check
        if (eatenCals > 0) {
            double calDiffRatio = Math.abs(eatenCals - targetCalories) / targetCalories;
            if (eatenCals > targetCalories + 300) {
                alerts.add("⚠️ Calorie intake is +" + Math.round(eatenCals - targetCalories) + " kcal above daily target.");
            } else if (eatenCals < targetCalories * 0.6 && eatenCals > 200) {
                alerts.add("💡 Currently at only " + Math.round(eatenCals) + " kcal. Ensure sufficient fuel for basal metabolic rate.");
            }
        }

        // Age-Specific Calcium Check
        if (eatenCalcium < ageRule.getCalciumMg() * 0.5 && eatenCals > 500) {
            if (age >= 50 || age <= 18) {
                alerts.add("🚨 Critical Age Alert: Calcium intake (" + Math.round(eatenCalcium) + "mg) is below 50% of the " + Math.round(ageRule.getCalciumMg()) + "mg RDA for " + ageRule.getBracketName() + ". Essential for bone mass preservation.");
            } else {
                alerts.add("🦴 Calcium intake is low (" + Math.round(eatenCalcium) + "mg / " + Math.round(ageRule.getCalciumMg()) + "mg). Add Greek yogurt, fortified plant milk, or tofu.");
            }
        }

        // Age-Specific Sodium Check
        if (eatenSodium > ageRule.getSodiumMaxMg()) {
            if (age >= 51) {
                alerts.add("⚠️ Sodium Alert: " + Math.round(eatenSodium) + "mg exceeds the strict " + Math.round(ageRule.getSodiumMaxMg()) + "mg limit recommended for adults 51+ to prevent vascular stiffness & hypertension.");
            } else {
                alerts.add("🧂 Sodium Warning: " + Math.round(eatenSodium) + "mg is above the " + Math.round(ageRule.getSodiumMaxMg()) + "mg upper daily limit.");
            }
        }

        // Age-Specific Protein Check
        double requiredProteinMin = profile.getWeightKg() * ageRule.getProteinPerKg();
        if (eatenProt >= requiredProteinMin && eatenProt > 0) {
            recommendations.add("✅ Protein Target Met: " + Math.round(eatenProt) + "g meets the " + ageRule.getProteinPerKg() + "g/kg target for " + ageRule.getBracketName() + ".");
        } else if (eatenCals > 1000 && eatenProt < requiredProteinMin * 0.7) {
            alerts.add("💪 Protein Deficit: Currently " + Math.round(eatenProt) + "g vs " + Math.round(requiredProteinMin) + "g recommended for your age/weight.");
        }

        // Age-Specific Vitamin D & B12
        if (age >= 50 && eatenVitD < ageRule.getVitaminDMcg() * 0.5 && eatenCals > 800) {
            recommendations.add("☀️ Vitamin D Advice: Age 50+ synthesis declines by up to 70%. Consider fortified foods, salmon, or safe sun exposure.");
        }
        if (age >= 50 && eatenVitB12 < ageRule.getVitaminB12Mcg() * 0.5 && eatenCals > 800) {
            recommendations.add("🧬 B12 Absorption: Stomach intrinsic factor reduces with age. Prioritize B12-rich fish, eggs, or fortified nutritional yeast.");
        }

        // Fiber Check
        if (eatenFiber >= ageRule.getFiberGrams()) {
            recommendations.add("🌿 Excellent Fiber Intake: " + Math.round(eatenFiber) + "g supports gut microbiome and low glycemic stability.");
        } else if (eatenCals > 1000 && eatenFiber < ageRule.getFiberGrams() * 0.6) {
            recommendations.add("🌾 Boost Fiber: Aim for " + Math.round(ageRule.getFiberGrams()) + "g/day with chia seeds, lentils, berries, and oats.");
        }

        // Actionable age tips
        for (String tip : ageRule.getActionableTips()) {
            if (!recommendations.contains(tip)) {
                recommendations.add(tip);
            }
        }

        analysis.setHealthAlerts(alerts);
        analysis.setRecommendations(recommendations);

        // 5. Compute Nutrition Quality Score (0-100)
        int score = calculateNutritionScore(analysis, profile, ageRule);
        analysis.setNutritionScore(score);
        analysis.setScoreGrade(getGradeForScore(score));

        return analysis;
    }

    private int calculateNutritionScore(NutritionAnalysis a, UserProfile profile, AgeNutritionRule ageRule) {
        if (a.getConsumedCalories() <= 0) {
            return 0; // No meals logged yet
        }

        double score = 0;

        // 1. Calorie Alignment (25 pts max)
        double targetCal = a.getTargetCalories();
        double consumedCal = a.getConsumedCalories();
        double calRatio = consumedCal / (targetCal > 0 ? targetCal : 2000);
        if (calRatio >= 0.85 && calRatio <= 1.15) {
            score += 25;
        } else if (calRatio >= 0.70 && calRatio <= 1.30) {
            score += 18;
        } else if (calRatio >= 0.50 && calRatio <= 1.50) {
            score += 10;
        } else {
            score += 5;
        }

        // 2. Protein Completeness (25 pts max)
        double targetProt = a.getTargetProteinG();
        double consumedProt = a.getConsumedProteinG();
        double protRatio = consumedProt / (targetProt > 0 ? targetProt : 60);
        if (protRatio >= 0.90 && protRatio <= 1.30) {
            score += 25;
        } else if (protRatio >= 0.70) {
            score += 18;
        } else if (protRatio >= 0.50) {
            score += 12;
        } else {
            score += 5;
        }

        // 3. Fiber & Sodium Balance (25 pts max)
        double fiberScore = Math.min(12.5, (a.getConsumedFiberG() / Math.max(1, a.getTargetFiberG())) * 12.5);
        double sodiumScore = 12.5;
        if (a.getConsumedSodiumMg() > a.getTargetSodiumMgMax() * 1.3) {
            sodiumScore = 2.0;
        } else if (a.getConsumedSodiumMg() > a.getTargetSodiumMgMax()) {
            sodiumScore = 6.0;
        }
        score += fiberScore + sodiumScore;

        // 4. Age-Critical Micronutrient Index (Calcium, Iron, Vit D, Potassium) (25 pts max)
        double microIndex = 0;
        microIndex += Math.min(6.25, (a.getConsumedCalciumMg() / Math.max(1, a.getTargetCalciumMg())) * 6.25);
        microIndex += Math.min(6.25, (a.getConsumedIronMg() / Math.max(1, a.getTargetIronMg())) * 6.25);
        microIndex += Math.min(6.25, (a.getConsumedPotassiumMg() / Math.max(1, a.getTargetPotassiumMg())) * 6.25);
        microIndex += Math.min(6.25, (a.getConsumedVitaminCMg() / Math.max(1, a.getTargetVitaminCMg())) * 6.25);
        score += microIndex;

        return (int) Math.min(100, Math.max(10, Math.round(score)));
    }

    private String getGradeForScore(int score) {
        if (score >= 90) return "A+";
        if (score >= 80) return "A";
        if (score >= 70) return "B+";
        if (score >= 60) return "B";
        if (score >= 50) return "C";
        if (score >= 35) return "D";
        return "E";
    }
}
