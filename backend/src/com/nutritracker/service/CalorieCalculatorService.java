package com.nutritracker.service;

import com.nutritracker.model.UserProfile;

public class CalorieCalculatorService {

    /**
     * Calculates Basal Metabolic Rate (BMR) using the Mifflin-St Jeor Equation.
     * Male: 10 * weight(kg) + 6.25 * height(cm) - 5 * age + 5
     * Female: 10 * weight(kg) + 6.25 * height(cm) - 5 * age - 161
     */
    public double calculateBMR(UserProfile profile) {
        double weight = profile.getWeightKg();
        double height = profile.getHeightCm();
        int age = profile.getAge();
        String gender = profile.getGender() != null ? profile.getGender().toLowerCase() : "female";

        double bmr = (10.0 * weight) + (6.25 * height) - (5.0 * age);
        if (gender.equals("male")) {
            bmr += 5;
        } else {
            bmr -= 161;
        }
        return Math.max(800.0, bmr);
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE) based on activity multiplier.
     */
    public double calculateTDEE(UserProfile profile) {
        double bmr = calculateBMR(profile);
        double multiplier = getActivityMultiplier(profile.getActivityLevel());
        return bmr * multiplier;
    }

    public double getActivityMultiplier(String activityLevel) {
        if (activityLevel == null) return 1.375;
        switch (activityLevel.toLowerCase()) {
            case "sedentary":
                return 1.2;
            case "light":
                return 1.375;
            case "active":
                return 1.725;
            case "very_active":
                return 1.9;
            case "moderate":
            default:
                return 1.55;
        }
    }

    /**
     * Computes target daily calories based on user goal (weight loss deficit, muscle gain surplus, or maintenance).
     */
    public double calculateTargetCalories(UserProfile profile) {
        if (profile.getCustomCalorieTarget() > 0) {
            return profile.getCustomCalorieTarget();
        }
        double tdee = calculateTDEE(profile);
        String goal = profile.getGoal() != null ? profile.getGoal().toLowerCase() : "maintain";

        switch (goal) {
            case "weight_loss":
                // 20% deficit or max 500 kcal deficit, but not below safe floor (1200 kcal for female, 1500 for male)
                double deficit = Math.min(500.0, tdee * 0.20);
                double minFloor = "male".equalsIgnoreCase(profile.getGender()) ? 1500 : 1200;
                return Math.max(minFloor, tdee - deficit);
            case "muscle_gain":
                // 15% surplus or +350-400 kcal
                return tdee + 380;
            case "maintain":
            default:
                return tdee;
        }
    }

    /**
     * Computes Target Protein in grams.
     * High protein for muscle gain or cutting, moderate for maintenance.
     */
    public double calculateTargetProtein(UserProfile profile) {
        double weightKg = profile.getWeightKg();
        String goal = profile.getGoal() != null ? profile.getGoal().toLowerCase() : "maintain";
        String pref = profile.getDietaryPref() != null ? profile.getDietaryPref().toLowerCase() : "omnivore";

        double gPerKg;
        if ("muscle_gain".equals(goal)) {
            gPerKg = 2.0;
        } else if ("weight_loss".equals(goal)) {
            gPerKg = 1.8; // higher protein spares lean muscle during deficit
        } else {
            gPerKg = 1.4;
        }

        if (profile.getAge() >= 65) {
            // Older adults require higher protein (1.4-1.6g/kg) to mitigate anabolic resistance
            gPerKg = Math.max(gPerKg, 1.4);
        }

        return Math.round(weightKg * gPerKg);
    }

    /**
     * Computes Target Fat in grams based on calorie percentage.
     */
    public double calculateTargetFat(UserProfile profile, double targetCalories) {
        String pref = profile.getDietaryPref() != null ? profile.getDietaryPref().toLowerCase() : "omnivore";
        double fatCaloriePercent;

        if ("keto".equals(pref)) {
            fatCaloriePercent = 0.70; // 70% calories from fat
        } else {
            fatCaloriePercent = 0.28; // ~28% calories from healthy fats
        }

        double fatCalories = targetCalories * fatCaloriePercent;
        return Math.round(fatCalories / 9.0); // 9 kcal per gram of fat
    }

    /**
     * Computes Target Carbs in grams as remainder of target calories after protein and fat.
     */
    public double calculateTargetCarbs(UserProfile profile, double targetCalories, double targetProteinG, double targetFatG) {
        String pref = profile.getDietaryPref() != null ? profile.getDietaryPref().toLowerCase() : "omnivore";
        if ("keto".equals(pref)) {
            return 30.0; // Net 30g max for keto
        }

        double proteinCalories = targetProteinG * 4.0;
        double fatCalories = targetFatG * 9.0;
        double carbCalories = Math.max(100.0, targetCalories - (proteinCalories + fatCalories));
        return Math.round(carbCalories / 4.0); // 4 kcal per gram of carb
    }
}
