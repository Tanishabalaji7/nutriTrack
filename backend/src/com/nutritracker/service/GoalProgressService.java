package com.nutritracker.service;

import com.nutritracker.dao.MealLogDao;
import com.nutritracker.dao.UserDao;
import com.nutritracker.dao.WaterLogDao;
import com.nutritracker.model.MealLog;
import com.nutritracker.model.NutritionAnalysis;
import com.nutritracker.model.UserProfile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class GoalProgressService {
    private final UserDao userDao;
    private final MealLogDao mealLogDao;
    private final WaterLogDao waterLogDao;
    private final AgeProfileAnalysisService ageProfileAnalysisService;

    public GoalProgressService() {
        this.userDao = new UserDao();
        this.mealLogDao = new MealLogDao();
        this.waterLogDao = new WaterLogDao();
        this.ageProfileAnalysisService = new AgeProfileAnalysisService();
    }

    public JSONObject calculateGoalProgress(int userId, String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        UserProfile user = userDao.getUserProfile(userId);
        LocalDate targetDate = LocalDate.parse(dateStr);
        LocalDate startDate = targetDate.minusDays(6);

        String startStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE);

        List<MealLog> allLogs = mealLogDao.getLogsForDateRange(userId, startStr, endStr);

        // Daily Targets
        double targetCalories = computeTargetCalories(user);
        double targetProtein = computeTargetProtein(user, targetCalories);
        int targetWaterMl = (int) (user.getWaterGoalLiters() * 1000);
        if (targetWaterMl <= 0) targetWaterMl = 2500;

        // Collect 7 days data
        JSONArray dayHistory = new JSONArray();
        double totalWeekProtein = 0;
        double totalWeekWater = 0;
        int proteinDaysMet = 0;
        int waterDaysMet = 0;
        int calorieDaysInZone = 0;
        int totalWeekDeviation = 0;
        int daysWithLogs = 0;

        double todayCalories = 0;
        double todayProtein = 0;
        int todayWater = 0;

        for (int i = 6; i >= 0; i--) {
            LocalDate d = targetDate.minusDays(i);
            String dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String dayName = d.getDayOfWeek().toString().substring(0, 3); // "MON", "TUE"

            List<MealLog> dayLogs = new ArrayList<>();
            for (MealLog log : allLogs) {
                if (dStr.equals(log.getDate())) {
                    dayLogs.add(log);
                }
            }

            int dayWater = waterLogDao.getTotalWaterForDate(userId, dStr);
            NutritionAnalysis a = ageProfileAnalysisService.performFullAnalysis(user, dayLogs);

            double dayCal = a.getConsumedCalories();
            double dayProt = a.getConsumedProteinG();

            if (dStr.equals(dateStr)) {
                todayCalories = dayCal;
                todayProtein = dayProt;
                todayWater = dayWater;
            }

            if (dayLogs.size() > 0 || dayWater > 0) {
                daysWithLogs++;
            }

            // Evaluation
            double calVariance = Math.abs(dayCal - targetCalories);
            boolean calMet = dayCal > 0 && calVariance <= Math.max(250.0, targetCalories * 0.15);
            boolean protMet = dayProt >= (targetProtein * 0.85);
            boolean waterMet = dayWater >= (targetWaterMl * 0.85);

            if (calMet) calorieDaysInZone++;
            if (protMet) proteinDaysMet++;
            if (waterMet) waterDaysMet++;

            totalWeekProtein += dayProt;
            totalWeekWater += dayWater;
            if (dayCal > 0) totalWeekDeviation += calVariance;

            // Day score (0 - 100)
            int dayScore = 0;
            if (calMet) dayScore += 40;
            else if (dayCal > 0 && calVariance <= targetCalories * 0.25) dayScore += 25;

            if (protMet) dayScore += 35;
            else if (dayProt > 0) dayScore += (int) Math.min(30, (dayProt / targetProtein) * 35);

            if (waterMet) dayScore += 25;
            else if (dayWater > 0) dayScore += (int) Math.min(20, ((double) dayWater / targetWaterMl) * 25);

            JSONObject dayObj = new JSONObject();
            dayObj.put("date", dStr);
            dayObj.put("dayName", dayName);
            dayObj.put("calories", Math.round(dayCal));
            dayObj.put("protein", Math.round(dayProt));
            dayObj.put("waterMl", dayWater);
            dayObj.put("calMet", calMet);
            dayObj.put("protMet", protMet);
            dayObj.put("waterMet", waterMet);
            dayObj.put("score", Math.min(100, dayScore));
            dayObj.put("isToday", dStr.equals(dateStr));
            dayHistory.put(dayObj);
        }

        // 1. Weight Goal Progress
        JSONObject weightGoal = new JSONObject();
        double currentWeight = user.getWeightKg();
        double startingWeight = user.getStartingWeightKg();
        double targetWeight = user.getTargetWeightKg();
        String goalType = user.getGoal() != null ? user.getGoal().toLowerCase() : "maintain";

        double progressPercent = 0.0;
        double remainingKg = 0.0;
        String weightStatus = "";

        if ("weight_loss".equals(goalType)) {
            double totalToLose = startingWeight - targetWeight;
            if (totalToLose <= 0) totalToLose = 5.0; // fallback buffer
            double lost = startingWeight - currentWeight;
            progressPercent = Math.min(100.0, Math.max(0.0, (lost / totalToLose) * 100.0));
            remainingKg = Math.max(0.0, currentWeight - targetWeight);
            weightStatus = remainingKg <= 0.1 ? "Goal Reached! 🏆" : String.format("%.1f kg to target", remainingKg);
        } else if ("muscle_gain".equals(goalType)) {
            double totalToGain = targetWeight - startingWeight;
            if (totalToGain <= 0) totalToGain = 4.0;
            double gained = currentWeight - startingWeight;
            progressPercent = Math.min(100.0, Math.max(0.0, (gained / totalToGain) * 100.0));
            remainingKg = Math.max(0.0, targetWeight - currentWeight);
            weightStatus = remainingKg <= 0.1 ? "Muscle Goal Reached! 💪" : String.format("%.1f kg to gain", remainingKg);
        } else {
            progressPercent = 100.0;
            remainingKg = 0.0;
            weightStatus = "Weight Maintained ⚖️";
        }

        weightGoal.put("currentKg", Math.round(currentWeight * 10.0) / 10.0);
        weightGoal.put("startingKg", Math.round(startingWeight * 10.0) / 10.0);
        weightGoal.put("targetKg", Math.round(targetWeight * 10.0) / 10.0);
        weightGoal.put("progressPercent", Math.round(progressPercent));
        weightGoal.put("remainingKg", Math.round(remainingKg * 10.0) / 10.0);
        weightGoal.put("goalType", goalType);
        weightGoal.put("status", weightStatus);

        // 2. Protein Target Object
        JSONObject proteinGoal = new JSONObject();
        double todayProtPercent = Math.min(150.0, Math.round((todayProtein / targetProtein) * 100.0));
        double weeklyAvgProt = Math.round((totalWeekProtein / 7.0) * 10.0) / 10.0;
        String protStatus = todayProtein >= targetProtein ? "Target Met! 🥩" : String.format("%.0fg left", Math.max(0, targetProtein - todayProtein));

        proteinGoal.put("targetG", Math.round(targetProtein));
        proteinGoal.put("todayG", Math.round(todayProtein));
        proteinGoal.put("todayPercent", (int) todayProtPercent);
        proteinGoal.put("weeklyAvgG", weeklyAvgProt);
        proteinGoal.put("daysMet", proteinDaysMet);
        proteinGoal.put("totalDays", 7);
        proteinGoal.put("status", protStatus);

        // 3. Water Target Object
        JSONObject waterGoal = new JSONObject();
        double todayWaterPercent = Math.min(150.0, Math.round(((double) todayWater / targetWaterMl) * 100.0));
        int weeklyAvgWater = (int) Math.round(totalWeekWater / 7.0);
        String waterStatus = todayWater >= targetWaterMl ? "Hydration Met! 💧" : String.format("%dml to goal", Math.max(0, targetWaterMl - todayWater));

        waterGoal.put("targetMl", targetWaterMl);
        waterGoal.put("todayMl", todayWater);
        waterGoal.put("todayPercent", (int) todayWaterPercent);
        waterGoal.put("weeklyAvgMl", weeklyAvgWater);
        waterGoal.put("daysMet", waterDaysMet);
        waterGoal.put("totalDays", 7);
        waterGoal.put("status", waterStatus);

        // 4. Calorie Consistency Object
        JSONObject calorieConsistency = new JSONObject();
        int avgDeviation = daysWithLogs > 0 ? (int) Math.round((double) totalWeekDeviation / daysWithLogs) : 0;
        int consistencyPercent = Math.min(100, Math.max(20, (calorieDaysInZone * 14) + (daysWithLogs * 5)));
        String consistencyRating = consistencyPercent >= 80 ? "High Consistency 🔥" : (consistencyPercent >= 50 ? "Moderate Consistency ⚡" : "Building Habit 🌱");

        calorieConsistency.put("targetKcal", Math.round(targetCalories));
        calorieConsistency.put("todayKcal", Math.round(todayCalories));
        calorieConsistency.put("daysInZone", calorieDaysInZone);
        calorieConsistency.put("totalDays", 7);
        calorieConsistency.put("consistencyPercent", consistencyPercent);
        calorieConsistency.put("avgDeviationKcal", avgDeviation);
        calorieConsistency.put("rating", consistencyRating);

        // 5. Weekly Adherence Composite
        int adherenceScore = (int) Math.round((calorieDaysInZone * 5.0) + (proteinDaysMet * 4.5) + (waterDaysMet * 3.5) + (daysWithLogs * 1.5));
        adherenceScore = Math.min(100, Math.max(15, adherenceScore));

        String grade = "A+";
        if (adherenceScore < 60) grade = "C";
        else if (adherenceScore < 75) grade = "B";
        else if (adherenceScore < 88) grade = "A";

        // Badges
        JSONArray badges = new JSONArray();
        badges.put(new JSONObject().put("id", "streak").put("name", "Streak Master").put("icon", "🔥").put("desc", daysWithLogs + "/7 Days Active").put("unlocked", daysWithLogs >= 4));
        badges.put(new JSONObject().put("id", "protein").put("name", "Protein Anchor").put("icon", "🥩").put("desc", proteinDaysMet + "/7 Days Hit").put("unlocked", proteinDaysMet >= 4));
        badges.put(new JSONObject().put("id", "water").put("name", "Hydration Hero").put("icon", "💧").put("desc", waterDaysMet + "/7 Days Hit").put("unlocked", waterDaysMet >= 4));
        badges.put(new JSONObject().put("id", "cal").put("name", "Calorie Sniper").put("icon", "🎯").put("desc", calorieDaysInZone + "/7 Days In Zone").put("unlocked", calorieDaysInZone >= 4));

        // Actionable AI Tip
        String aiTip = generateTip(proteinDaysMet, waterDaysMet, calorieDaysInZone, goalType);

        JSONObject weeklyAdherence = new JSONObject();
        weeklyAdherence.put("overallScore", adherenceScore);
        weeklyAdherence.put("grade", grade);
        weeklyAdherence.put("streakDays", daysWithLogs);
        weeklyAdherence.put("daysHistory", dayHistory);
        weeklyAdherence.put("aiTip", aiTip);
        weeklyAdherence.put("badges", badges);

        // Overall Response
        JSONObject res = new JSONObject();
        res.put("success", true);
        res.put("date", dateStr);
        res.put("weightGoal", weightGoal);
        res.put("proteinGoal", proteinGoal);
        res.put("waterGoal", waterGoal);
        res.put("calorieConsistency", calorieConsistency);
        res.put("weeklyAdherence", weeklyAdherence);

        return res;
    }

    private double computeTargetCalories(UserProfile p) {
        if (p.getCustomCalorieTarget() > 0) return p.getCustomCalorieTarget();

        double bmr = (10 * p.getWeightKg()) + (6.25 * p.getHeightCm()) - (5 * p.getAge());
        if ("male".equalsIgnoreCase(p.getGender())) bmr += 5;
        else bmr -= 161;

        double mult = 1.55;
        String act = p.getActivityLevel() != null ? p.getActivityLevel().toLowerCase() : "moderate";
        if ("sedentary".equals(act)) mult = 1.2;
        else if ("light".equals(act)) mult = 1.375;
        else if ("active".equals(act)) mult = 1.725;
        else if ("very_active".equals(act)) mult = 1.9;

        double tdee = bmr * mult;
        String goal = p.getGoal() != null ? p.getGoal().toLowerCase() : "maintain";
        if ("weight_loss".equals(goal)) return Math.max(1200, tdee - Math.min(500, tdee * 0.2));
        if ("muscle_gain".equals(goal)) return tdee + 380;
        return tdee;
    }

    private double computeTargetProtein(UserProfile p, double targetCalories) {
        String goal = p.getGoal() != null ? p.getGoal().toLowerCase() : "maintain";
        double multiplier = "muscle_gain".equals(goal) ? 2.0 : ("weight_loss".equals(goal) ? 1.8 : 1.4);
        double protG = p.getWeightKg() * multiplier;
        if (p.getAge() >= 65) protG = Math.max(protG, p.getWeightKg() * 1.4);
        return Math.max(50.0, Math.round(protG));
    }

    private String generateTip(int protDays, int waterDays, int calDays, String goal) {
        if (protDays < 3) {
            return "🥩 Add a high-protein anchor (eggs, Greek yogurt, lentils, or paneer) to breakfast to reach your protein goal effortlessly.";
        }
        if (waterDays < 3) {
            return "💧 Keep a 750ml water bottle at your workspace and sip between meals to easily hit your hydration target!";
        }
        if (calDays < 3) {
            return "🎯 Calorie intake fluctuates across days. Pre-logging your lunch or dinner in the morning creates steady, effortless adherence.";
        }
        if ("weight_loss".equals(goal)) {
            return "🔥 Outstanding weekly adherence! You're consistently maintaining your clinical deficit while protecting lean muscle mass.";
        }
        return "🌟 Excellent overall consistency! You are building sustainable nutrition habits across all key health pillars.";
    }
}
