package com.nutritracker.model;

import org.json.JSONObject;

public class MealLog {
    private int id;
    private int userId;
    private int foodId;
    private FoodItem foodItem;
    private String mealType; // "breakfast", "lunch", "dinner", "snack"
    private double quantity; // multiplier of serving size (e.g. 1.0, 1.5, 2.0)
    private String date; // "YYYY-MM-DD"
    private String time; // "HH:MM"
    private String notes;

    public MealLog() {}

    public MealLog(int id, int userId, int foodId, FoodItem foodItem, String mealType,
                   double quantity, String date, String time, String notes) {
        this.id = id;
        this.userId = userId;
        this.foodId = foodId;
        this.foodItem = foodItem;
        this.mealType = mealType;
        this.quantity = quantity;
        this.date = date;
        this.time = time;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getFoodId() { return foodId; }
    public void setFoodId(int foodId) { this.foodId = foodId; }

    public FoodItem getFoodItem() { return foodItem; }
    public void setFoodItem(FoodItem foodItem) { this.foodItem = foodItem; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Computed nutrition for this specific log based on quantity
    public double getTotalCalories() {
        return foodItem != null ? Math.round(foodItem.getCalories() * quantity * 10.0) / 10.0 : 0;
    }
    public double getTotalProtein() {
        return foodItem != null ? Math.round(foodItem.getProteinG() * quantity * 10.0) / 10.0 : 0;
    }
    public double getTotalCarbs() {
        return foodItem != null ? Math.round(foodItem.getCarbsG() * quantity * 10.0) / 10.0 : 0;
    }
    public double getTotalFat() {
        return foodItem != null ? Math.round(foodItem.getFatG() * quantity * 10.0) / 10.0 : 0;
    }
    public double getTotalFiber() {
        return foodItem != null ? Math.round(foodItem.getFiberG() * quantity * 10.0) / 10.0 : 0;
    }
    public double getTotalSodium() {
        return foodItem != null ? Math.round(foodItem.getSodiumMg() * quantity * 10.0) / 10.0 : 0;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("userId", userId);
        json.put("foodId", foodId);
        json.put("mealType", mealType);
        json.put("quantity", quantity);
        json.put("date", date);
        json.put("time", time != null ? time : "");
        json.put("notes", notes != null ? notes : "");
        if (foodItem != null) {
            json.put("food", foodItem.toJSON());
            json.put("computedCalories", getTotalCalories());
            json.put("computedProtein", getTotalProtein());
            json.put("computedCarbs", getTotalCarbs());
            json.put("computedFat", getTotalFat());
            json.put("computedFiber", getTotalFiber());
            json.put("computedSodium", getTotalSodium());
        }
        return json;
    }
}
