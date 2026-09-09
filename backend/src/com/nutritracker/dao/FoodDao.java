package com.nutritracker.dao;

import com.nutritracker.database.DatabaseManager;
import com.nutritracker.model.FoodItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FoodDao {
    private final DatabaseManager dbManager;

    public FoodDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public List<FoodItem> getAllFoods() {
        List<FoodItem> foods = new ArrayList<>();
        String sql = "SELECT * FROM foods ORDER BY category, name";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                foods.add(mapResultSetToFood(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all foods: " + e.getMessage());
        }
        return foods;
    }

    public List<FoodItem> searchFoods(String query, String category) {
        List<FoodItem> foods = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM foods WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND name LIKE ?");
            params.add("%" + query.trim() + "%");
        }
        if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All")) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }
        sql.append(" ORDER BY name ASC LIMIT 50");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    foods.add(mapResultSetToFood(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching foods: " + e.getMessage());
        }
        return foods;
    }

    public FoodItem getFoodById(int id) {
        String sql = "SELECT * FROM foods WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFood(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting food by id: " + e.getMessage());
        }
        return null;
    }

    public FoodItem findBestMatchByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = "SELECT * FROM foods WHERE name LIKE ? ORDER BY LENGTH(name) ASC LIMIT 1";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + name.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFood(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding food by name: " + e.getMessage());
        }
        return null;
    }

    public FoodItem createFood(FoodItem food) {
        String sql = "INSERT INTO foods (name, category, serving_size, serving_unit, calories, protein_g, carbs_g, fat_g, fiber_g, sugar_g, sodium_mg, potassium_mg, calcium_mg, iron_mg, vitamin_c_mg, vitamin_d_mcg, vitamin_b12_mcg, is_custom) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, food.getName());
            ps.setString(2, food.getCategory());
            ps.setDouble(3, food.getServingSize());
            ps.setString(4, food.getServingUnit());
            ps.setDouble(5, food.getCalories());
            ps.setDouble(6, food.getProteinG());
            ps.setDouble(7, food.getCarbsG());
            ps.setDouble(8, food.getFatG());
            ps.setDouble(9, food.getFiberG());
            ps.setDouble(10, food.getSugarG());
            ps.setDouble(11, food.getSodiumMg());
            ps.setDouble(12, food.getPotassiumMg());
            ps.setDouble(13, food.getCalciumMg());
            ps.setDouble(14, food.getIronMg());
            ps.setDouble(15, food.getVitaminCMg());
            ps.setDouble(16, food.getVitaminDMcg());
            ps.setDouble(17, food.getVitaminB12Mcg());
            ps.setInt(18, food.isCustom() ? 1 : 0);

            ps.executeUpdate();
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    food.setId(generatedKeys.getInt(1));
                }
            }
            return food;
        } catch (SQLException e) {
            System.err.println("Error creating food: " + e.getMessage());
            return null;
        }
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM foods ORDER BY category";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting categories: " + e.getMessage());
        }
        return categories;
    }

    private FoodItem mapResultSetToFood(ResultSet rs) throws SQLException {
        return new FoodItem(
                rs.getInt("id"),
                rs.getString("name"),
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
    }
}
