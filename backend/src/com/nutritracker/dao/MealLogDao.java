package com.nutritracker.dao;

import com.nutritracker.database.DatabaseManager;
import com.nutritracker.model.FoodItem;
import com.nutritracker.model.MealLog;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MealLogDao {
    private final DatabaseManager dbManager;
    private final FoodDao foodDao;

    public MealLogDao() {
        this.dbManager = DatabaseManager.getInstance();
        this.foodDao = new FoodDao();
    }

    public MealLog addMealLog(MealLog log) {
        String sql = "INSERT INTO meal_logs (user_id, food_id, meal_type, quantity, date, time, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getUserId() > 0 ? log.getUserId() : 1);
            ps.setInt(2, log.getFoodId());
            ps.setString(3, log.getMealType());
            ps.setDouble(4, log.getQuantity() > 0 ? log.getQuantity() : 1.0);
            ps.setString(5, log.getDate());
            ps.setString(6, log.getTime());
            ps.setString(7, log.getNotes());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getInt(1));
                }
            }
            if (log.getFoodItem() == null) {
                log.setFoodItem(foodDao.getFoodById(log.getFoodId()));
            }
            return log;
        } catch (SQLException e) {
            System.err.println("Error adding meal log: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteMealLog(int id) {
        String sql = "DELETE FROM meal_logs WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting meal log: " + e.getMessage());
            return false;
        }
    }

    public List<MealLog> getLogsForDate(int userId, String date) {
        List<MealLog> logs = new ArrayList<>();
        String sql = "SELECT ml.*, f.name as food_name, f.category, f.serving_size, f.serving_unit, " +
                "f.calories, f.protein_g, f.carbs_g, f.fat_g, f.fiber_g, f.sugar_g, " +
                "f.sodium_mg, f.potassium_mg, f.calcium_mg, f.iron_mg, f.vitamin_c_mg, " +
                "f.vitamin_d_mcg, f.vitamin_b12_mcg, f.is_custom " +
                "FROM meal_logs ml " +
                "JOIN foods f ON ml.food_id = f.id " +
                "WHERE ml.user_id = ? AND ml.date = ? " +
                "ORDER BY ml.id ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MealLog log = new MealLog();
                    log.setId(rs.getInt("id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setFoodId(rs.getInt("food_id"));
                    log.setMealType(rs.getString("meal_type"));
                    log.setQuantity(rs.getDouble("quantity"));
                    log.setDate(rs.getString("date"));
                    log.setTime(rs.getString("time"));
                    log.setNotes(rs.getString("notes"));

                    FoodItem food = new FoodItem(
                            rs.getInt("food_id"),
                            rs.getString("food_name"),
                            rs.getString("category"),
                            rs.getDouble("serving_size"),
                            rs.getString("serving_unit"),
                            rs.getDouble("calories"),
                            rs.getDouble("protein_g"),
                            rs.getDouble("carbs_g"),
                            rs.getDouble("fat_g"),
                            rs.getDouble("fiber_g"),
                            rs.getDouble("sugar_g"),
                            rs.getDouble("sodium_mg"),
                            rs.getDouble("potassium_mg"),
                            rs.getDouble("calcium_mg"),
                            rs.getDouble("iron_mg"),
                            rs.getDouble("vitamin_c_mg"),
                            rs.getDouble("vitamin_d_mcg"),
                            rs.getDouble("vitamin_b12_mcg"),
                            rs.getInt("is_custom") == 1
                    );
                    log.setFoodItem(food);
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting logs for date: " + e.getMessage());
        }
        return logs;
    }

    public List<MealLog> getLogsForDateRange(int userId, String startDate, String endDate) {
        List<MealLog> logs = new ArrayList<>();
        String sql = "SELECT ml.*, f.name as food_name, f.category, f.serving_size, f.serving_unit, " +
                "f.calories, f.protein_g, f.carbs_g, f.fat_g, f.fiber_g, f.sugar_g, " +
                "f.sodium_mg, f.potassium_mg, f.calcium_mg, f.iron_mg, f.vitamin_c_mg, " +
                "f.vitamin_d_mcg, f.vitamin_b12_mcg, f.is_custom " +
                "FROM meal_logs ml " +
                "JOIN foods f ON ml.food_id = f.id " +
                "WHERE ml.user_id = ? AND ml.date >= ? AND ml.date <= ? " +
                "ORDER BY ml.date ASC, ml.id ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, startDate);
            ps.setString(3, endDate);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MealLog log = new MealLog();
                    log.setId(rs.getInt("id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setFoodId(rs.getInt("food_id"));
                    log.setMealType(rs.getString("meal_type"));
                    log.setQuantity(rs.getDouble("quantity"));
                    log.setDate(rs.getString("date"));
                    log.setTime(rs.getString("time"));
                    log.setNotes(rs.getString("notes"));

                    FoodItem food = new FoodItem(
                            rs.getInt("food_id"),
                            rs.getString("food_name"),
                            rs.getString("category"),
                            rs.getDouble("serving_size"),
                            rs.getString("serving_unit"),
                            rs.getDouble("calories"),
                            rs.getDouble("protein_g"),
                            rs.getDouble("carbs_g"),
                            rs.getDouble("fat_g"),
                            rs.getDouble("fiber_g"),
                            rs.getDouble("sugar_g"),
                            rs.getDouble("sodium_mg"),
                            rs.getDouble("potassium_mg"),
                            rs.getDouble("calcium_mg"),
                            rs.getDouble("iron_mg"),
                            rs.getDouble("vitamin_c_mg"),
                            rs.getDouble("vitamin_d_mcg"),
                            rs.getDouble("vitamin_b12_mcg"),
                            rs.getInt("is_custom") == 1
                    );
                    log.setFoodItem(food);
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting logs for date range: " + e.getMessage());
        }
        return logs;
    }
}
