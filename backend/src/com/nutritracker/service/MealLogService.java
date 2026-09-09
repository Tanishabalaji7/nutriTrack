package com.nutritracker.service;

import com.nutritracker.dao.FoodDao;
import com.nutritracker.dao.MealLogDao;
import com.nutritracker.dao.UserDao;
import com.nutritracker.dao.WaterLogDao;
import com.nutritracker.model.FoodItem;
import com.nutritracker.model.MealLog;
import com.nutritracker.model.NutritionAnalysis;
import com.nutritracker.model.UserProfile;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MealLogService {
    private final MealLogDao mealLogDao;
    private final UserDao userDao;
    private final FoodDao foodDao;
    private final WaterLogDao waterLogDao;
    private final AgeProfileAnalysisService ageProfileAnalysisService;

    public MealLogService() {
        this.mealLogDao = new MealLogDao();
        this.userDao = new UserDao();
        this.foodDao = new FoodDao();
        this.waterLogDao = new WaterLogDao();
        this.ageProfileAnalysisService = new AgeProfileAnalysisService();
    }

    public JSONObject getDailySummary(int userId, String date) {
        if (date == null || date.isEmpty()) {
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        UserProfile user = userDao.getUserProfile(userId);
        List<MealLog> logs = mealLogDao.getLogsForDate(userId, date);
        int totalWaterMl = waterLogDao.getTotalWaterForDate(userId, date);

        NutritionAnalysis analysis = ageProfileAnalysisService.performFullAnalysis(user, logs);

        // Group logs by meal type
        JSONObject mealBuckets = new JSONObject();
        mealBuckets.put("breakfast", new JSONArray());
        mealBuckets.put("lunch", new JSONArray());
        mealBuckets.put("dinner", new JSONArray());
        mealBuckets.put("snack", new JSONArray());

        JSONObject mealCalTotals = new JSONObject();
        mealCalTotals.put("breakfast", 0.0);
        mealCalTotals.put("lunch", 0.0);
        mealCalTotals.put("dinner", 0.0);
        mealCalTotals.put("snack", 0.0);

        for (MealLog log : logs) {
            String type = log.getMealType() != null ? log.getMealType().toLowerCase() : "snack";
            if (!mealBuckets.has(type)) {
                mealBuckets.put(type, new JSONArray());
                mealCalTotals.put(type, 0.0);
            }
            mealBuckets.getJSONArray(type).put(log.toJSON());
            double currentCal = mealCalTotals.getDouble(type);
            mealCalTotals.put(type, Math.round((currentCal + log.getTotalCalories()) * 10.0) / 10.0);
        }

        JSONObject result = new JSONObject();
        result.put("date", date);
        result.put("user", user.toJSON());
        result.put("analysis", analysis.toJSON());
        result.put("mealBuckets", mealBuckets);
        result.put("mealCalTotals", mealCalTotals);
        result.put("waterLoggedMl", totalWaterMl);
        result.put("waterGoalMl", (int)(user.getWaterGoalLiters() * 1000));
        result.put("totalLogsCount", logs.size());

        return result;
    }

    public JSONObject getWeeklyTrends(int userId) {
        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(6);

        String startDate = sevenDaysAgo.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        UserProfile user = userDao.getUserProfile(userId);
        List<MealLog> allLogs = mealLogDao.getLogsForDateRange(userId, startDate, endDate);

        JSONArray days = new JSONArray();

        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String dStr = d.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String dayName = d.getDayOfWeek().toString().substring(0, 3); // "MON", "TUE", etc.

            List<MealLog> dayLogs = new ArrayList<>();
            for (MealLog l : allLogs) {
                if (dStr.equals(l.getDate())) {
                    dayLogs.add(l);
                }
            }

            int waterMl = waterLogDao.getTotalWaterForDate(userId, dStr);
            NutritionAnalysis a = ageProfileAnalysisService.performFullAnalysis(user, dayLogs);

            JSONObject dayObj = new JSONObject();
            dayObj.put("date", dStr);
            dayObj.put("dayName", dayName);
            dayObj.put("calories", Math.round(a.getConsumedCalories()));
            dayObj.put("targetCalories", Math.round(a.getTargetCalories()));
            dayObj.put("protein", Math.round(a.getConsumedProteinG()));
            dayObj.put("carbs", Math.round(a.getConsumedCarbsG()));
            dayObj.put("fat", Math.round(a.getConsumedFatG()));
            dayObj.put("waterMl", waterMl);
            dayObj.put("score", a.getNutritionScore());
            days.put(dayObj);
        }

        JSONObject response = new JSONObject();
        response.put("weeklyData", days);
        return response;
    }

    public MealLog logFoodItem(int userId, int foodId, String mealType, double quantity, String date, String time, String notes) {
        if (date == null || date.isEmpty()) {
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        MealLog log = new MealLog();
        log.setUserId(userId);
        log.setFoodId(foodId);
        log.setMealType(mealType != null ? mealType.toLowerCase() : "snack");
        log.setQuantity(quantity > 0 ? quantity : 1.0);
        log.setDate(date);
        log.setTime(time != null ? time : "");
        log.setNotes(notes != null ? notes : "");

        return mealLogDao.addMealLog(log);
    }

    public boolean deleteLog(int logId) {
        return mealLogDao.deleteMealLog(logId);
    }
}
