package com.nutritracker.database;

import com.nutritracker.model.FoodItem;
import com.nutritracker.model.UserProfile;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_DIR = "backend/data";
    private static final String DB_PATH = "backend/data/nutrition_tracker.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static DatabaseManager instance;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            DriverManager.registerDriver(new org.sqlite.JDBC());
        } catch (Exception e) {
            System.err.println("Warning registering SQLite JDBC Driver: " + e.getMessage());
        }
    }

    private DatabaseManager() {
        initDatabase();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initDatabase() {
        try {
            File dir = new File(DB_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                // 1. Users Table with authentication
                stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, " +
                        "email TEXT UNIQUE NOT NULL, " +
                        "password TEXT DEFAULT '', " +
                        "age INTEGER NOT NULL, " +
                        "gender TEXT NOT NULL, " +
                        "height_cm REAL NOT NULL, " +
                        "weight_kg REAL NOT NULL, " +
                        "activity_level TEXT NOT NULL, " +
                        "goal TEXT NOT NULL, " +
                        "dietary_pref TEXT NOT NULL, " +
                        "custom_calorie_target INTEGER DEFAULT 0, " +
                        "water_goal_liters REAL DEFAULT 2.5, " +
                        "starting_weight_kg REAL DEFAULT 0, " +
                        "target_weight_kg REAL DEFAULT 0, " +
                        "gemini_api_key TEXT DEFAULT '', " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ");");

                // Migration if gemini_api_key or weight goal columns do not exist
                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN gemini_api_key TEXT DEFAULT '';");
                } catch (Exception ignored) {}
                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN starting_weight_kg REAL DEFAULT 0;");
                } catch (Exception ignored) {}
                try {
                    stmt.execute("ALTER TABLE users ADD COLUMN target_weight_kg REAL DEFAULT 0;");
                } catch (Exception ignored) {}

                // 2. Foods Table
                stmt.execute("CREATE TABLE IF NOT EXISTS foods (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL COLLATE NOCASE, " +
                        "category TEXT NOT NULL, " +
                        "serving_size REAL NOT NULL, " +
                        "serving_unit TEXT NOT NULL, " +
                        "calories REAL NOT NULL, " +
                        "protein_g REAL NOT NULL, " +
                        "carbs_g REAL NOT NULL, " +
                        "fat_g REAL NOT NULL, " +
                        "fiber_g REAL DEFAULT 0, " +
                        "sugar_g REAL DEFAULT 0, " +
                        "sodium_mg REAL DEFAULT 0, " +
                        "potassium_mg REAL DEFAULT 0, " +
                        "calcium_mg REAL DEFAULT 0, " +
                        "iron_mg REAL DEFAULT 0, " +
                        "vitamin_c_mg REAL DEFAULT 0, " +
                        "vitamin_d_mcg REAL DEFAULT 0, " +
                        "vitamin_b12_mcg REAL DEFAULT 0, " +
                        "is_custom INTEGER DEFAULT 0" +
                        ");");

                // 3. Meal Logs Table
                stmt.execute("CREATE TABLE IF NOT EXISTS meal_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "food_id INTEGER NOT NULL, " +
                        "meal_type TEXT NOT NULL, " +
                        "quantity REAL NOT NULL DEFAULT 1.0, " +
                        "date TEXT NOT NULL, " +
                        "time TEXT, " +
                        "notes TEXT, " +
                        "FOREIGN KEY(user_id) REFERENCES users(id), " +
                        "FOREIGN KEY(food_id) REFERENCES foods(id)" +
                        ");");

                // 4. Water Logs Table
                stmt.execute("CREATE TABLE IF NOT EXISTS water_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "date TEXT NOT NULL, " +
                        "amount_ml INTEGER NOT NULL, " +
                        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY(user_id) REFERENCES users(id)" +
                        ");");

                // 5. Chat History Table
                stmt.execute("CREATE TABLE IF NOT EXISTS chat_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "user_id INTEGER NOT NULL, " +
                        "sender TEXT NOT NULL, " +
                        "message TEXT NOT NULL, " +
                        "action_type TEXT, " +
                        "metadata_json TEXT, " +
                        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "FOREIGN KEY(user_id) REFERENCES users(id)" +
                        ");");

                // Check and Seed Default User
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO users (name, email, password, age, gender, height_cm, weight_kg, activity_level, goal, dietary_pref, custom_calorie_target, water_goal_liters) " +
                            "VALUES ('Alex Morgan', 'alex@example.com', 'password123', 28, 'female', 168.0, 64.0, 'moderate', 'maintain', 'omnivore', 0, 2.5);");
                }

                // Sync / Seed Foods (inserts all foods that are not yet in the table)
                syncSeedFoods(conn);

                System.out.println("SQLite database initialized and synchronized successfully at: " + DB_PATH);
            }
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncSeedFoods(Connection conn) {
        String checkSql = "SELECT COUNT(*) FROM foods WHERE name = ?";
        String insertSql = "INSERT INTO foods (name, category, serving_size, serving_unit, calories, protein_g, carbs_g, fat_g, fiber_g, sugar_g, sodium_mg, potassium_mg, calcium_mg, iron_mg, vitamin_c_mg, vitamin_d_mcg, vitamin_b12_mcg, is_custom) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)";

        List<Object[]> foods = getSeedFoodList();
        int addedCount = 0;

        try (PreparedStatement checkPs = conn.prepareStatement(checkSql);
             PreparedStatement insertPs = conn.prepareStatement(insertSql)) {

            for (Object[] f : foods) {
                checkPs.setString(1, (String) f[0]);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        for (int i = 0; i < f.length; i++) {
                            insertPs.setObject(i + 1, f[i]);
                        }
                        insertPs.addBatch();
                        addedCount++;
                    }
                }
            }

            if (addedCount > 0) {
                insertPs.executeBatch();
                System.out.println("Synchronized and seeded " + addedCount + " new foods into SQLite database.");
            } else {
                System.out.println("All " + foods.size() + " seed foods are already present in SQLite.");
            }
        } catch (SQLException e) {
            System.err.println("Error synchronizing seed foods: " + e.getMessage());
        }
    }

    private List<Object[]> getSeedFoodList() {
        List<Object[]> list = new ArrayList<>();
        // Format: name, category, servingSize, servingUnit, cal, prot, carb, fat, fiber, sugar, sodium, potassium, calcium, iron, vitC, vitD, vitB12

        // ==========================================
        // --- GLOBAL & WESTERN PROTEINS ---
        // ==========================================
        list.add(new Object[]{"Grilled Chicken Breast", "Proteins", 100.0, "g", 165.0, 31.0, 0.0, 3.6, 0.0, 0.0, 74.0, 256.0, 15.0, 1.0, 0.0, 0.1, 0.3});
        list.add(new Object[]{"Atlantic Salmon (Baked)", "Proteins", 100.0, "g", 208.0, 20.4, 0.0, 13.4, 0.0, 0.0, 59.0, 363.0, 9.0, 0.3, 3.7, 11.0, 3.2});
        list.add(new Object[]{"Boiled Whole Egg (Large)", "Proteins", 50.0, "large egg", 72.0, 6.3, 0.4, 4.8, 0.0, 0.2, 71.0, 69.0, 28.0, 0.9, 0.0, 1.1, 0.5});
        list.add(new Object[]{"Egg Whites (Liquid/Cooked)", "Proteins", 100.0, "g", 52.0, 10.9, 0.7, 0.2, 0.0, 0.7, 166.0, 163.0, 7.0, 0.1, 0.0, 0.0, 0.1});
        list.add(new Object[]{"Lean Ground Turkey (93/7)", "Proteins", 100.0, "g", 152.0, 22.0, 0.0, 7.0, 0.0, 0.0, 78.0, 240.0, 18.0, 1.4, 0.0, 0.4, 1.2});
        list.add(new Object[]{"Grass-Fed Sirloin Steak", "Proteins", 100.0, "g", 183.0, 26.0, 0.0, 8.5, 0.0, 0.0, 55.0, 340.0, 16.0, 2.7, 0.0, 0.1, 2.1});
        list.add(new Object[]{"Canned Light Tuna in Water", "Proteins", 100.0, "g", 116.0, 25.5, 0.0, 0.8, 0.0, 0.0, 247.0, 237.0, 11.0, 1.3, 0.0, 2.0, 2.5});
        list.add(new Object[]{"Organic Firm Tofu", "Proteins", 100.0, "g", 83.0, 10.0, 2.3, 5.3, 1.0, 0.6, 14.0, 121.0, 282.0, 2.0, 0.2, 0.0, 0.0});
        list.add(new Object[]{"Tempeh (Fermented Soy)", "Proteins", 100.0, "g", 192.0, 20.3, 7.6, 10.8, 4.8, 0.0, 9.0, 412.0, 111.0, 2.7, 0.0, 0.0, 0.1});
        list.add(new Object[]{"Cooked Brown Lentils", "Proteins", 100.0, "g", 116.0, 9.0, 20.1, 0.4, 7.9, 1.8, 2.0, 369.0, 19.0, 3.3, 1.5, 0.0, 0.0});
        list.add(new Object[]{"Cooked Chickpeas (Garbanzo)", "Proteins", 100.0, "g", 164.0, 8.9, 27.4, 2.6, 7.6, 4.8, 7.0, 291.0, 49.0, 2.9, 1.3, 0.0, 0.0});
        list.add(new Object[]{"Black Beans (Boiled)", "Proteins", 100.0, "g", 132.0, 8.9, 23.7, 0.5, 8.7, 0.3, 1.0, 355.0, 27.0, 2.1, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Steamed Edamame", "Proteins", 100.0, "g", 121.0, 11.9, 8.9, 5.2, 5.2, 2.2, 6.0, 436.0, 63.0, 2.3, 6.1, 0.0, 0.0});
        list.add(new Object[]{"Whey Protein Isolate Powder", "Proteins", 30.0, "scoop", 110.0, 25.0, 1.0, 0.5, 0.0, 0.5, 50.0, 140.0, 120.0, 0.2, 0.0, 0.0, 0.8});
        list.add(new Object[]{"Plant Protein Powder (Pea/Rice)", "Proteins", 30.0, "scoop", 120.0, 24.0, 2.0, 2.0, 1.5, 0.0, 220.0, 110.0, 45.0, 5.0, 0.0, 0.0, 1.2});
        list.add(new Object[]{"Grilled Shrimp (Jumbo)", "Proteins", 100.0, "g", 99.0, 24.0, 0.2, 0.3, 0.0, 0.0, 111.0, 259.0, 70.0, 0.5, 0.0, 0.1, 1.1});

        // ==========================================
        // --- GLOBAL GRAINS & COMPLEX CARBS ---
        // ==========================================
        list.add(new Object[]{"Cooked Brown Rice", "Grains", 100.0, "g", 123.0, 2.7, 25.6, 1.0, 1.6, 0.4, 3.0, 86.0, 10.0, 0.5, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Cooked Jasmine White Rice", "Grains", 100.0, "g", 130.0, 2.4, 28.2, 0.3, 0.4, 0.1, 1.0, 35.0, 10.0, 0.2, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Cooked Organic Quinoa", "Grains", 100.0, "g", 120.0, 4.4, 21.3, 1.9, 2.8, 0.9, 7.0, 172.0, 17.0, 1.5, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Rolled Oats (Dry)", "Grains", 50.0, "g", 189.0, 6.6, 33.5, 3.3, 5.1, 0.5, 3.0, 181.0, 26.0, 2.1, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Baked Sweet Potato", "Grains", 100.0, "g", 90.0, 2.0, 20.7, 0.2, 3.3, 6.5, 36.0, 475.0, 38.0, 0.7, 19.6, 0.0, 0.0});
        list.add(new Object[]{"Boiled Russet Potato (with skin)", "Grains", 100.0, "g", 87.0, 1.9, 20.1, 0.1, 1.8, 0.9, 4.0, 379.0, 5.0, 0.3, 13.0, 0.0, 0.0});
        list.add(new Object[]{"100% Whole Wheat Bread", "Grains", 40.0, "slice", 92.0, 4.0, 17.0, 1.1, 3.0, 1.8, 145.0, 85.0, 30.0, 1.1, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Artisan Sourdough Bread", "Grains", 50.0, "slice", 120.0, 4.5, 23.0, 0.8, 1.2, 1.0, 210.0, 60.0, 15.0, 1.3, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Cooked Whole Grain Pasta", "Grains", 100.0, "g", 148.0, 6.0, 30.7, 0.8, 3.9, 0.8, 4.0, 88.0, 18.0, 1.5, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Plain Whole Wheat Bagel", "Grains", 95.0, "item", 245.0, 10.2, 48.0, 1.5, 4.0, 4.5, 430.0, 150.0, 40.0, 2.8, 0.0, 0.0, 0.0});

        // ==========================================
        // --- FRESH VEGETABLES ---
        // ==========================================
        list.add(new Object[]{"Steamed Broccoli", "Vegetables", 100.0, "g", 35.0, 2.4, 7.2, 0.4, 2.6, 1.4, 41.0, 293.0, 47.0, 0.7, 64.9, 0.0, 0.0});
        list.add(new Object[]{"Fresh Baby Spinach", "Vegetables", 100.0, "g", 23.0, 2.9, 3.6, 0.4, 2.2, 0.4, 79.0, 558.0, 99.0, 2.7, 28.1, 0.0, 0.0});
        list.add(new Object[]{"Raw Tuscan Kale", "Vegetables", 100.0, "g", 49.0, 4.3, 8.8, 0.9, 3.6, 2.3, 38.0, 447.0, 150.0, 1.5, 120.0, 0.0, 0.0});
        list.add(new Object[]{"Red Bell Pepper (Sliced)", "Vegetables", 100.0, "g", 31.0, 1.0, 6.0, 0.3, 2.1, 4.2, 4.0, 211.0, 7.0, 0.4, 127.7, 0.0, 0.0});
        list.add(new Object[]{"Fresh Carrots (Raw)", "Vegetables", 100.0, "g", 41.0, 0.9, 9.6, 0.2, 2.8, 4.7, 69.0, 320.0, 33.0, 0.3, 5.9, 0.0, 0.0});
        list.add(new Object[]{"Roasted Asparagus", "Vegetables", 100.0, "g", 22.0, 2.4, 4.1, 0.2, 2.0, 1.3, 14.0, 224.0, 24.0, 2.1, 7.7, 0.0, 0.0});
        list.add(new Object[]{"Steamed Cauliflower", "Vegetables", 100.0, "g", 25.0, 1.9, 5.0, 0.3, 2.0, 1.9, 30.0, 299.0, 22.0, 0.4, 48.2, 0.0, 0.0});
        list.add(new Object[]{"Fresh Sliced Tomatoes", "Vegetables", 100.0, "g", 18.0, 0.9, 3.9, 0.2, 1.2, 2.6, 5.0, 237.0, 10.0, 0.3, 13.7, 0.0, 0.0});
        list.add(new Object[]{"English Cucumber (with peel)", "Vegetables", 100.0, "g", 15.0, 0.7, 3.6, 0.1, 0.5, 1.7, 2.0, 147.0, 16.0, 0.3, 2.8, 0.0, 0.0});
        list.add(new Object[]{"Roasted Brussels Sprouts", "Vegetables", 100.0, "g", 43.0, 3.4, 9.0, 0.3, 3.8, 2.2, 25.0, 389.0, 42.0, 1.4, 85.0, 0.0, 0.0});
        list.add(new Object[]{"Sauteed Button Mushrooms", "Vegetables", 100.0, "g", 28.0, 2.2, 4.3, 0.5, 1.5, 2.0, 5.0, 356.0, 3.0, 0.5, 2.1, 0.2, 0.1});
        list.add(new Object[]{"Grilled Zucchini", "Vegetables", 100.0, "g", 17.0, 1.2, 3.1, 0.3, 1.0, 2.5, 8.0, 261.0, 16.0, 0.4, 17.9, 0.0, 0.0});

        // ==========================================
        // --- FRESH FRUITS ---
        // ==========================================
        list.add(new Object[]{"Medium Banana", "Fruits", 118.0, "medium", 105.0, 1.3, 27.0, 0.3, 3.1, 14.4, 1.0, 422.0, 6.0, 0.3, 10.3, 0.0, 0.0});
        list.add(new Object[]{"Crisp Honeycrisp Apple", "Fruits", 150.0, "medium", 78.0, 0.4, 21.0, 0.2, 3.6, 15.6, 1.5, 160.0, 9.0, 0.2, 6.9, 0.0, 0.0});
        list.add(new Object[]{"Fresh Blueberries", "Fruits", 100.0, "g", 57.0, 0.7, 14.5, 0.3, 2.4, 9.96, 1.0, 77.0, 6.0, 0.3, 9.7, 0.0, 0.0});
        list.add(new Object[]{"Fresh Strawberries", "Fruits", 100.0, "g", 32.0, 0.7, 7.7, 0.3, 2.0, 4.9, 1.0, 153.0, 16.0, 0.4, 58.8, 0.0, 0.0});
        list.add(new Object[]{"Navel Orange (Peeled)", "Fruits", 130.0, "medium", 62.0, 1.2, 15.4, 0.2, 3.1, 12.2, 0.0, 237.0, 52.0, 0.1, 69.7, 0.0, 0.0});
        list.add(new Object[]{"Fresh Hass Avocado", "Healthy Fats", 100.0, "g", 160.0, 2.0, 8.5, 14.7, 6.7, 0.7, 7.0, 485.0, 12.0, 0.6, 10.0, 0.0, 0.0});
        list.add(new Object[]{"Sweet Tropical Mango", "Fruits", 100.0, "g", 60.0, 0.8, 15.0, 0.4, 1.6, 13.7, 1.0, 168.0, 11.0, 0.2, 36.4, 0.0, 0.0});
        list.add(new Object[]{"Fresh Pineapple Chunks", "Fruits", 100.0, "g", 50.0, 0.5, 13.1, 0.1, 1.4, 9.9, 1.0, 109.0, 13.0, 0.3, 47.8, 0.0, 0.0});
        list.add(new Object[]{"Fresh Watermelon Slices", "Fruits", 100.0, "g", 30.0, 0.6, 7.6, 0.2, 0.4, 6.2, 1.0, 112.0, 7.0, 0.2, 8.1, 0.0, 0.0});
        list.add(new Object[]{"Red Seedless Grapes", "Fruits", 100.0, "g", 69.0, 0.7, 18.1, 0.2, 0.9, 15.5, 2.0, 191.0, 10.0, 0.4, 3.2, 0.0, 0.0});
        list.add(new Object[]{"Papaya Slices", "Fruits", 100.0, "g", 43.0, 0.5, 10.8, 0.3, 1.7, 7.8, 8.0, 182.0, 20.0, 0.3, 60.9, 0.0, 0.0});
        list.add(new Object[]{"Fresh Guava (Amrood)", "Fruits", 100.0, "g", 68.0, 2.6, 14.3, 1.0, 5.4, 8.9, 2.0, 417.0, 18.0, 0.3, 228.3, 0.0, 0.0});
        list.add(new Object[]{"Fresh Pomegranate Arils", "Fruits", 100.0, "g", 83.0, 1.7, 18.7, 1.2, 4.0, 13.7, 3.0, 236.0, 10.0, 0.3, 10.2, 0.0, 0.0});

        // ==========================================
        // --- DAIRY & HEALTHY ALTERNATIVES ---
        // ==========================================
        list.add(new Object[]{"Plain Non-Fat Greek Yogurt", "Dairy", 150.0, "container", 88.0, 15.0, 5.4, 0.6, 0.0, 5.0, 54.0, 212.0, 165.0, 0.1, 0.0, 0.0, 1.1});
        list.add(new Object[]{"Whole Milk (Cow's)", "Dairy", 240.0, "cup", 149.0, 7.7, 11.7, 8.0, 0.0, 12.3, 105.0, 322.0, 276.0, 0.1, 0.0, 2.9, 1.1});
        list.add(new Object[]{"Unsweetened Almond Milk", "Dairy", 240.0, "cup", 30.0, 1.0, 1.0, 2.5, 1.0, 0.0, 170.0, 160.0, 450.0, 0.7, 0.0, 2.5, 1.5});
        list.add(new Object[]{"Oat Milk (Fortified)", "Dairy", 240.0, "cup", 120.0, 3.0, 16.0, 5.0, 2.0, 7.0, 100.0, 350.0, 350.0, 1.8, 0.0, 2.5, 1.2});
        list.add(new Object[]{"Low-Fat Cottage Cheese (2%)", "Dairy", 100.0, "g", 84.0, 11.0, 4.3, 2.3, 0.0, 4.1, 330.0, 125.0, 111.0, 0.2, 0.0, 0.1, 0.6});
        list.add(new Object[]{"Aged Cheddar Cheese", "Dairy", 28.0, "oz", 115.0, 7.0, 0.4, 9.4, 0.0, 0.1, 180.0, 27.0, 204.0, 0.2, 0.0, 0.2, 0.2});
        list.add(new Object[]{"Fresh Mozzarella Cheese", "Dairy", 30.0, "oz", 85.0, 6.3, 0.9, 6.3, 0.0, 0.3, 138.0, 24.0, 143.0, 0.1, 0.0, 0.1, 0.5});
        list.add(new Object[]{"Plain Probiotic Dahi (Curd)", "Dairy", 150.0, "bowl", 95.0, 5.5, 7.0, 4.5, 0.0, 6.0, 60.0, 230.0, 200.0, 0.1, 0.5, 0.2, 0.8});

        // ==========================================
        // --- HEALTHY FATS, NUTS & SEEDS ---
        // ==========================================
        list.add(new Object[]{"Raw Almonds", "Healthy Fats", 28.0, "handful (23 nuts)", 164.0, 6.0, 6.1, 14.2, 3.5, 1.2, 1.0, 208.0, 76.0, 1.0, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Raw English Walnuts", "Healthy Fats", 28.0, "handful (14 halves)", 185.0, 4.3, 3.9, 18.5, 1.9, 0.7, 1.0, 125.0, 28.0, 0.8, 0.4, 0.0, 0.0});
        list.add(new Object[]{"Organic Chia Seeds", "Healthy Fats", 15.0, "tbsp", 73.0, 2.5, 6.3, 4.6, 5.1, 0.1, 2.0, 61.0, 95.0, 1.1, 0.2, 0.0, 0.0});
        list.add(new Object[]{"Natural Peanut Butter", "Healthy Fats", 32.0, "2 tbsp", 188.0, 8.0, 7.0, 16.0, 2.0, 3.0, 5.0, 208.0, 17.0, 0.6, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Extra Virgin Olive Oil", "Healthy Fats", 14.0, "tbsp", 119.0, 0.0, 0.0, 13.5, 0.0, 0.0, 0.3, 0.1, 0.1, 0.1, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Pure Desi Cow Ghee", "Healthy Fats", 14.0, "tbsp", 123.0, 0.0, 0.0, 14.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.2, 0.0});
        list.add(new Object[]{"Ground Flaxseeds", "Healthy Fats", 10.0, "tbsp", 55.0, 1.9, 3.0, 4.3, 2.8, 0.2, 3.0, 84.0, 26.0, 0.6, 0.1, 0.0, 0.0});
        list.add(new Object[]{"Roasted Pumpkin Seeds (Pepitas)", "Healthy Fats", 28.0, "handful", 158.0, 8.6, 4.2, 13.9, 1.7, 0.4, 5.0, 261.0, 15.0, 2.3, 0.5, 0.0, 0.0});
        list.add(new Object[]{"Roasted Cashews (Kaju)", "Healthy Fats", 28.0, "handful", 160.0, 5.2, 8.6, 12.4, 0.9, 1.7, 3.0, 187.0, 10.0, 1.9, 0.0, 0.0, 0.0});

        // ==========================================
        // --- PREPARED COMPLETE MEALS & DISHES ---
        // ==========================================
        list.add(new Object[]{"Avocado Toast with Poached Egg", "Prepared Meals", 180.0, "serving", 320.0, 14.0, 28.0, 18.0, 6.0, 2.0, 360.0, 480.0, 65.0, 2.5, 8.0, 1.1, 0.6});
        list.add(new Object[]{"Grilled Chicken Caesar Salad", "Prepared Meals", 300.0, "bowl", 390.0, 34.0, 12.0, 23.0, 4.0, 3.0, 680.0, 540.0, 180.0, 2.2, 24.0, 0.3, 0.8});
        list.add(new Object[]{"Salmon & Brown Rice Quinoa Bowl", "Prepared Meals", 350.0, "bowl", 520.0, 36.0, 48.0, 20.0, 7.0, 3.5, 420.0, 780.0, 60.0, 3.1, 15.0, 11.2, 3.4});
        list.add(new Object[]{"Lentil Dal with Basmati Rice", "Prepared Meals", 320.0, "bowl", 380.0, 16.0, 64.0, 6.0, 11.0, 3.0, 480.0, 520.0, 65.0, 4.2, 12.0, 0.0, 0.0});
        list.add(new Object[]{"Tofu Veggie Stir-Fry with Rice", "Prepared Meals", 320.0, "bowl", 340.0, 18.0, 42.0, 12.0, 6.5, 5.0, 510.0, 610.0, 220.0, 3.5, 45.0, 0.0, 0.0});
        list.add(new Object[]{"Berry Protein Smoothie", "Prepared Meals", 350.0, "smoothie (16 oz)", 280.0, 28.0, 32.0, 4.5, 6.0, 18.0, 160.0, 520.0, 320.0, 1.5, 45.0, 2.5, 1.6});
        list.add(new Object[]{"Chicken Burrito Bowl with Black Beans", "Prepared Meals", 400.0, "bowl", 560.0, 42.0, 58.0, 18.0, 10.0, 4.0, 740.0, 820.0, 95.0, 3.8, 28.0, 0.2, 0.7});
        list.add(new Object[]{"Oatmeal with Blueberries & Almond Butter", "Prepared Meals", 250.0, "bowl", 340.0, 10.0, 48.0, 13.0, 8.0, 11.0, 45.0, 320.0, 80.0, 2.4, 8.0, 0.0, 0.0});

        // ==========================================
        // --- INDIAN BREADS, ROTIS & FLATBREADS ---
        // ==========================================
        list.add(new Object[]{"Whole Wheat Roti / Chapati", "Grains", 40.0, "roti", 104.0, 3.2, 22.0, 0.5, 3.5, 0.2, 2.0, 90.0, 15.0, 1.2, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Ghee Phulka / Ghee Roti", "Grains", 45.0, "roti", 135.0, 3.2, 22.0, 4.0, 3.5, 0.2, 2.0, 90.0, 15.0, 1.2, 0.0, 0.1, 0.0});
        list.add(new Object[]{"Tandoori Roti (Clay Oven)", "Grains", 50.0, "roti", 120.0, 3.8, 25.0, 0.6, 3.8, 0.2, 10.0, 95.0, 16.0, 1.3, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Tandoori Butter Naan", "Grains", 90.0, "piece", 260.0, 7.5, 45.0, 6.5, 2.0, 2.0, 320.0, 95.0, 40.0, 1.8, 0.0, 0.1, 0.1});
        list.add(new Object[]{"Garlic Naan with Butter", "Grains", 95.0, "piece", 275.0, 7.8, 46.0, 7.0, 2.2, 2.0, 330.0, 105.0, 42.0, 1.9, 1.5, 0.1, 0.1});
        list.add(new Object[]{"Crispy Laccha Paratha", "Grains", 80.0, "paratha", 240.0, 4.5, 32.0, 11.0, 2.0, 0.5, 180.0, 90.0, 25.0, 1.4, 0.0, 0.1, 0.0});
        list.add(new Object[]{"Aloo Paratha (Stuffed Potato)", "Prepared Meals", 120.0, "paratha", 290.0, 6.0, 42.0, 11.0, 4.5, 1.5, 280.0, 290.0, 30.0, 2.1, 8.0, 0.0, 0.0});
        list.add(new Object[]{"Paneer Paratha (High Protein)", "Prepared Meals", 130.0, "paratha", 340.0, 14.0, 36.0, 16.0, 4.0, 1.2, 260.0, 220.0, 240.0, 2.2, 2.0, 0.2, 0.4});
        list.add(new Object[]{"Gobi Paratha (Cauliflower)", "Prepared Meals", 120.0, "paratha", 240.0, 5.5, 36.0, 8.5, 4.8, 1.4, 250.0, 230.0, 35.0, 1.8, 18.0, 0.0, 0.0});
        list.add(new Object[]{"Methi Paratha (Fenugreek)", "Prepared Meals", 70.0, "paratha", 180.0, 4.2, 26.0, 6.5, 3.8, 0.4, 180.0, 140.0, 45.0, 2.2, 6.0, 0.0, 0.0});
        list.add(new Object[]{"Gujarati Methi Thepla", "Grains", 50.0, "thepla", 140.0, 4.0, 22.0, 4.5, 3.2, 0.5, 160.0, 130.0, 45.0, 2.0, 4.0, 0.0, 0.0});
        list.add(new Object[]{"Jowar Roti (Sorghum Millet)", "Grains", 50.0, "roti", 125.0, 3.6, 26.0, 0.8, 4.2, 0.1, 2.0, 120.0, 18.0, 1.8, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Bajra Roti (Pearl Millet)", "Grains", 55.0, "roti", 145.0, 4.1, 28.0, 1.8, 4.5, 0.2, 3.0, 145.0, 22.0, 2.8, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Makki di Roti (Cornmeal)", "Grains", 60.0, "roti", 160.0, 3.5, 30.0, 3.2, 3.8, 0.8, 5.0, 110.0, 12.0, 1.4, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Poori / Puri (Puffed)", "Grains", 35.0, "puri", 140.0, 2.2, 18.0, 7.0, 1.2, 0.2, 90.0, 50.0, 10.0, 0.8, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Bhatura / Bhature", "Grains", 80.0, "bhatura", 290.0, 6.0, 42.0, 11.0, 1.5, 1.0, 240.0, 80.0, 20.0, 1.5, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Kerala Malabar Parotta", "Grains", 90.0, "parotta", 300.0, 5.5, 44.0, 12.0, 1.8, 1.2, 280.0, 70.0, 22.0, 1.3, 0.0, 0.0, 0.0});

        // ==========================================
        // --- SOUTH INDIAN BREAKFAST & STAPLES ---
        // ==========================================
        list.add(new Object[]{"Steamed Idli (2 pieces)", "Grains", 70.0, "serving (2 idlis)", 130.0, 4.0, 26.0, 0.5, 2.0, 0.2, 180.0, 110.0, 18.0, 1.0, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Rava Idli (Semolina Steamed)", "Grains", 80.0, "serving (2 idlis)", 150.0, 4.5, 28.0, 2.5, 1.8, 0.5, 190.0, 120.0, 22.0, 1.2, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Crispy Plain Dosa", "Grains", 90.0, "dosa", 168.0, 4.2, 29.0, 4.0, 1.8, 0.4, 190.0, 120.0, 20.0, 1.1, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Masala Dosa with Potato Filling", "Prepared Meals", 180.0, "dosa", 320.0, 7.0, 52.0, 9.5, 4.0, 1.8, 380.0, 280.0, 45.0, 2.4, 8.5, 0.0, 0.0});
        list.add(new Object[]{"Mysore Masala Dosa", "Prepared Meals", 200.0, "dosa", 360.0, 7.5, 54.0, 13.0, 4.2, 2.0, 420.0, 310.0, 48.0, 2.6, 9.0, 0.0, 0.0});
        list.add(new Object[]{"Onion Rava Dosa", "Grains", 120.0, "dosa", 240.0, 5.2, 38.0, 7.5, 2.5, 1.2, 260.0, 160.0, 30.0, 1.5, 4.0, 0.0, 0.0});
        list.add(new Object[]{"Set Dosa / Sponge Dosa (2 pcs)", "Grains", 120.0, "serving (2 pcs)", 210.0, 5.0, 40.0, 3.5, 2.2, 0.5, 220.0, 140.0, 24.0, 1.3, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Neer Dosa (Light Rice Crepe, 2 pcs)", "Grains", 100.0, "serving (2 pcs)", 160.0, 3.0, 32.0, 2.0, 1.0, 0.2, 140.0, 90.0, 12.0, 0.8, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Ragi Dosa (Finger Millet Dosa)", "Grains", 100.0, "dosa", 175.0, 5.2, 32.0, 3.5, 4.5, 0.4, 180.0, 190.0, 180.0, 2.5, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Crispy Medu Vada (Urad Dal)", "Snacks", 60.0, "vada", 145.0, 4.5, 16.0, 7.5, 2.8, 0.2, 170.0, 160.0, 30.0, 1.8, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Onion Tomato Uttapam", "Prepared Meals", 150.0, "uttapam", 230.0, 6.0, 40.0, 5.0, 3.8, 2.8, 280.0, 240.0, 40.0, 1.9, 12.0, 0.0, 0.0});
        list.add(new Object[]{"Poha with Peanuts & Veggies", "Prepared Meals", 180.0, "bowl", 250.0, 6.0, 42.0, 7.0, 3.5, 1.5, 260.0, 210.0, 35.0, 4.5, 14.0, 0.0, 0.0});
        list.add(new Object[]{"Vegetable Upma (Semolina)", "Prepared Meals", 180.0, "bowl", 220.0, 5.5, 38.0, 5.5, 3.2, 1.0, 240.0, 180.0, 28.0, 1.6, 9.0, 0.0, 0.0});
        list.add(new Object[]{"Ven Pongal (Ghee Lentil Rice)", "Prepared Meals", 200.0, "bowl", 310.0, 8.5, 46.0, 11.0, 3.5, 0.5, 280.0, 240.0, 45.0, 2.2, 2.0, 0.0, 0.0});
        list.add(new Object[]{"Appam (Fermented Rice Pancake)", "Grains", 70.0, "appam", 120.0, 2.2, 22.0, 2.5, 1.2, 1.5, 120.0, 80.0, 15.0, 0.7, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Puttu (Steamed Rice & Coconut)", "Grains", 100.0, "serving", 190.0, 3.8, 38.0, 2.8, 2.5, 1.0, 110.0, 95.0, 18.0, 1.1, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Adai Dosa (Mixed Lentil Crepe)", "Proteins", 110.0, "dosa", 210.0, 9.5, 32.0, 5.0, 5.2, 0.8, 210.0, 260.0, 45.0, 2.8, 3.0, 0.0, 0.0});
        list.add(new Object[]{"Ragi Mudde (Finger Millet Ball)", "Grains", 150.0, "ball", 190.0, 4.8, 39.0, 1.5, 6.2, 0.2, 10.0, 280.0, 220.0, 3.2, 0.0, 0.0, 0.0});

        // ==========================================
        // --- INDIAN DALS, LENTILS & CURRIES ---
        // ==========================================
        list.add(new Object[]{"Yellow Moong Dal Tadka", "Proteins", 200.0, "bowl", 170.0, 10.5, 24.0, 4.0, 6.5, 1.2, 340.0, 420.0, 40.0, 2.8, 6.0, 0.0, 0.0});
        list.add(new Object[]{"Toor Dal Fry (Homestyle)", "Proteins", 200.0, "bowl", 185.0, 11.0, 26.0, 4.5, 6.0, 1.5, 360.0, 440.0, 42.0, 3.0, 7.0, 0.0, 0.0});
        list.add(new Object[]{"Dal Makhani (Creamy Black Lentils)", "Proteins", 200.0, "bowl", 280.0, 11.0, 28.0, 14.0, 7.5, 2.0, 460.0, 490.0, 90.0, 3.5, 4.0, 0.2, 0.3});
        list.add(new Object[]{"Punjabi Chole / Chana Masala", "Proteins", 220.0, "bowl", 260.0, 12.0, 38.0, 7.5, 9.5, 4.0, 420.0, 460.0, 70.0, 4.2, 15.0, 0.0, 0.0});
        list.add(new Object[]{"Rajma Masala (Red Kidney Beans)", "Proteins", 220.0, "bowl", 240.0, 13.0, 36.0, 5.5, 10.0, 3.5, 390.0, 580.0, 65.0, 4.0, 12.0, 0.0, 0.0});
        list.add(new Object[]{"South Indian Sambar", "Vegetables", 200.0, "bowl", 140.0, 6.5, 22.0, 3.0, 5.5, 3.0, 410.0, 380.0, 50.0, 2.2, 18.0, 0.0, 0.0});
        list.add(new Object[]{"Tomato Rasam (Spiced Pepper Broth)", "Vegetables", 180.0, "bowl", 65.0, 2.1, 11.0, 1.5, 2.0, 2.5, 380.0, 290.0, 30.0, 1.6, 22.0, 0.0, 0.0});
        list.add(new Object[]{"Gujarati Kadhi (Spiced Yogurt)", "Dairy", 200.0, "bowl", 130.0, 5.0, 14.0, 6.0, 1.0, 8.0, 340.0, 220.0, 150.0, 0.8, 2.0, 0.1, 0.5});
        list.add(new Object[]{"Punjabi Pakora Kadhi", "Prepared Meals", 220.0, "bowl", 260.0, 8.5, 24.0, 14.5, 3.2, 4.0, 460.0, 280.0, 160.0, 1.8, 4.0, 0.1, 0.5});
        list.add(new Object[]{"Dal Khichdi (Moong Dal & Rice)", "Prepared Meals", 250.0, "bowl", 290.0, 11.0, 50.0, 5.0, 6.0, 1.5, 320.0, 310.0, 45.0, 2.6, 4.0, 0.1, 0.0});
        list.add(new Object[]{"Bisi Bele Bath (Karnataka Lentil Rice)", "Prepared Meals", 250.0, "bowl", 330.0, 9.5, 54.0, 8.5, 6.5, 2.5, 420.0, 340.0, 55.0, 2.8, 12.0, 0.1, 0.0});
        list.add(new Object[]{"Lobia Masala (Black Eyed Peas)", "Proteins", 200.0, "bowl", 210.0, 11.5, 32.0, 4.5, 7.8, 3.0, 360.0, 450.0, 60.0, 3.6, 10.0, 0.0, 0.0});
        list.add(new Object[]{"Kala Chana Masala (Brown Chickpeas)", "Proteins", 200.0, "bowl", 230.0, 12.5, 34.0, 5.0, 9.0, 2.8, 380.0, 490.0, 68.0, 4.8, 11.0, 0.0, 0.0});
        list.add(new Object[]{"Panchmel Dal (Five Lentil Curry)", "Proteins", 200.0, "bowl", 195.0, 12.0, 26.0, 5.0, 7.2, 1.8, 380.0, 460.0, 52.0, 3.4, 8.0, 0.0, 0.0});

        // ==========================================
        // --- INDIAN PANEER & VEGETARIAN SABZIS ---
        // ==========================================
        list.add(new Object[]{"Fresh Paneer (Indian Cottage Cheese)", "Proteins", 100.0, "g", 296.0, 18.3, 4.1, 22.8, 0.0, 2.5, 22.0, 110.0, 480.0, 0.7, 0.0, 0.5, 0.7});
        list.add(new Object[]{"Palak Paneer (Spinach Cottage Cheese)", "Prepared Meals", 220.0, "bowl", 290.0, 16.0, 10.0, 21.0, 5.0, 3.0, 440.0, 520.0, 480.0, 4.5, 24.0, 0.4, 0.6});
        list.add(new Object[]{"Paneer Butter Masala", "Prepared Meals", 220.0, "bowl", 380.0, 15.0, 16.0, 29.0, 3.0, 7.0, 520.0, 340.0, 420.0, 1.8, 14.0, 0.5, 0.7});
        list.add(new Object[]{"Kadai Paneer with Bell Peppers", "Prepared Meals", 220.0, "bowl", 320.0, 16.0, 14.0, 23.0, 4.0, 4.5, 480.0, 380.0, 440.0, 2.2, 45.0, 0.4, 0.6});
        list.add(new Object[]{"Matar Paneer (Peas & Cottage Cheese)", "Prepared Meals", 220.0, "bowl", 275.0, 14.0, 16.0, 17.5, 4.5, 4.0, 420.0, 360.0, 360.0, 2.0, 16.0, 0.4, 0.6});
        list.add(new Object[]{"Shahi Paneer (Cashew Saffron Gravy)", "Prepared Meals", 220.0, "bowl", 395.0, 14.5, 18.0, 30.0, 2.5, 6.5, 490.0, 320.0, 380.0, 1.6, 8.0, 0.5, 0.7});
        list.add(new Object[]{"Spiced Paneer Bhurji", "Proteins", 180.0, "serving", 310.0, 18.0, 8.0, 23.0, 2.5, 3.0, 380.0, 260.0, 460.0, 1.6, 18.0, 0.5, 0.7});
        list.add(new Object[]{"Tandoori Paneer Tikka", "Proteins", 180.0, "serving", 280.0, 19.0, 9.0, 19.0, 2.2, 2.5, 420.0, 290.0, 490.0, 1.8, 12.0, 0.4, 0.6});
        list.add(new Object[]{"Malai Kofta (Cream Gravy Dumplings)", "Prepared Meals", 220.0, "bowl", 420.0, 9.0, 32.0, 29.0, 3.5, 6.0, 540.0, 310.0, 140.0, 2.0, 8.0, 0.2, 0.3});
        list.add(new Object[]{"Aloo Gobi (Potato Cauliflower)", "Vegetables", 180.0, "bowl", 160.0, 4.0, 24.0, 6.0, 4.5, 3.5, 310.0, 450.0, 40.0, 1.8, 42.0, 0.0, 0.0});
        list.add(new Object[]{"Bhindi Masala (Spiced Okra)", "Vegetables", 150.0, "serving", 130.0, 3.5, 16.0, 6.5, 5.0, 2.5, 280.0, 380.0, 90.0, 1.5, 22.0, 0.0, 0.0});
        list.add(new Object[]{"Baingan Bharta (Roasted Eggplant)", "Vegetables", 180.0, "bowl", 140.0, 3.0, 18.0, 7.0, 6.0, 5.5, 290.0, 420.0, 45.0, 1.4, 16.0, 0.0, 0.0});
        list.add(new Object[]{"Sarson Ka Saag (Mustard Greens)", "Vegetables", 200.0, "bowl", 170.0, 5.5, 16.0, 9.5, 6.8, 2.0, 380.0, 490.0, 180.0, 3.8, 55.0, 0.0, 0.0});
        list.add(new Object[]{"Dum Aloo (Kashmiri Spiced Potato)", "Vegetables", 200.0, "bowl", 240.0, 4.2, 34.0, 10.5, 4.0, 3.0, 410.0, 460.0, 38.0, 1.9, 14.0, 0.0, 0.0});
        list.add(new Object[]{"Lauki Chana Dal (Bottle Gourd Lentil)", "Vegetables", 200.0, "bowl", 145.0, 7.5, 22.0, 3.5, 5.2, 2.0, 310.0, 360.0, 42.0, 2.4, 15.0, 0.0, 0.0});
        list.add(new Object[]{"Mixed Vegetable Korma", "Vegetables", 200.0, "bowl", 210.0, 5.5, 20.0, 12.5, 4.8, 4.0, 390.0, 370.0, 65.0, 2.1, 20.0, 0.0, 0.0});
        list.add(new Object[]{"Moong Dal Cheela (Lentil Protein Crepe)", "Proteins", 100.0, "cheela", 165.0, 9.5, 24.0, 3.5, 4.8, 1.0, 190.0, 280.0, 35.0, 2.6, 6.0, 0.0, 0.0});
        list.add(new Object[]{"Besan Chilla (Gram Flour Crepe)", "Proteins", 100.0, "chilla", 175.0, 8.5, 26.0, 4.2, 4.5, 1.2, 210.0, 290.0, 40.0, 2.8, 8.0, 0.0, 0.0});

        // ==========================================
        // --- INDIAN NON-VEGETARIAN DELICACIES ---
        // ==========================================
        list.add(new Object[]{"Butter Chicken (Murgh Makhani)", "Prepared Meals", 250.0, "bowl", 440.0, 32.0, 14.0, 28.0, 2.5, 6.0, 580.0, 440.0, 90.0, 2.2, 16.0, 0.3, 0.8});
        list.add(new Object[]{"Tandoori Chicken Tikka (6 pcs)", "Proteins", 180.0, "serving (6 pcs)", 260.0, 38.0, 4.0, 10.0, 1.0, 1.0, 460.0, 390.0, 60.0, 1.8, 8.0, 0.2, 0.6});
        list.add(new Object[]{"Homestyle Indian Chicken Curry", "Prepared Meals", 250.0, "bowl", 310.0, 34.0, 8.0, 16.0, 2.0, 3.0, 490.0, 460.0, 45.0, 2.4, 14.0, 0.3, 0.7});
        list.add(new Object[]{"Chicken Chettinad (Spiced Pepper)", "Prepared Meals", 250.0, "bowl", 340.0, 35.0, 9.0, 18.0, 2.5, 2.0, 520.0, 480.0, 48.0, 2.6, 12.0, 0.3, 0.7});
        list.add(new Object[]{"Chicken Korma (Yogurt Almond Gravy)", "Prepared Meals", 250.0, "bowl", 390.0, 31.0, 12.0, 24.0, 2.0, 4.5, 480.0, 420.0, 65.0, 2.0, 6.0, 0.2, 0.6});
        list.add(new Object[]{"Chicken Saagwala (Palak Chicken)", "Prepared Meals", 250.0, "bowl", 320.0, 36.0, 7.0, 16.0, 4.5, 2.0, 460.0, 560.0, 180.0, 3.5, 22.0, 0.3, 0.7});
        list.add(new Object[]{"Mutton Rogan Josh", "Prepared Meals", 220.0, "bowl", 390.0, 28.0, 8.0, 27.0, 2.0, 2.5, 510.0, 380.0, 40.0, 3.8, 6.0, 0.2, 2.5});
        list.add(new Object[]{"Hyderabadi Chicken Dum Biryani", "Prepared Meals", 350.0, "bowl", 560.0, 36.0, 68.0, 16.0, 4.5, 3.0, 680.0, 510.0, 75.0, 3.2, 12.0, 0.2, 0.7});
        list.add(new Object[]{"Hyderabadi Mutton Dum Biryani", "Prepared Meals", 350.0, "bowl", 640.0, 32.0, 66.0, 26.0, 4.0, 2.8, 710.0, 480.0, 70.0, 4.1, 8.0, 0.2, 2.4});
        list.add(new Object[]{"Kerala Coconut Fish Curry", "Prepared Meals", 220.0, "bowl", 290.0, 26.0, 6.0, 18.0, 2.0, 1.5, 440.0, 480.0, 35.0, 1.6, 18.0, 8.5, 2.8});
        list.add(new Object[]{"Bengali Macher Jhol (Fish Curry)", "Prepared Meals", 220.0, "bowl", 230.0, 25.0, 7.0, 11.0, 1.8, 1.5, 390.0, 420.0, 30.0, 1.5, 14.0, 7.5, 2.6});
        list.add(new Object[]{"Goan Prawn Curry (Coconut Kokum)", "Prepared Meals", 220.0, "bowl", 270.0, 22.0, 8.0, 16.0, 1.5, 2.0, 460.0, 390.0, 55.0, 1.8, 16.0, 1.2, 1.4});
        list.add(new Object[]{"Tawa Fish Fry (Spiced Fillet)", "Proteins", 150.0, "serving", 240.0, 28.0, 4.0, 12.0, 1.0, 0.5, 380.0, 340.0, 32.0, 1.4, 8.0, 6.5, 2.4});
        list.add(new Object[]{"Dhaba Egg Curry (2 Boiled Eggs)", "Proteins", 200.0, "bowl", 250.0, 15.0, 10.0, 16.5, 2.0, 3.5, 420.0, 280.0, 70.0, 2.4, 10.0, 2.2, 1.1});
        list.add(new Object[]{"Spiced Egg Bhurji (Scramble)", "Proteins", 150.0, "serving", 210.0, 14.0, 5.0, 15.0, 1.5, 2.0, 360.0, 240.0, 65.0, 2.0, 15.0, 2.2, 1.0});
        list.add(new Object[]{"Egg Dum Biryani", "Prepared Meals", 320.0, "bowl", 460.0, 17.0, 64.0, 14.0, 3.5, 2.5, 540.0, 380.0, 60.0, 2.6, 8.0, 2.0, 1.0});

        // ==========================================
        // --- INDIAN RICE DISHES & PULAOS ---
        // ==========================================
        list.add(new Object[]{"Steamed Basmati Rice", "Grains", 150.0, "cup", 195.0, 4.0, 44.0, 0.4, 1.0, 0.1, 2.0, 55.0, 15.0, 0.8, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Jeera Rice (Cumin Basmati)", "Grains", 150.0, "bowl", 230.0, 4.2, 45.0, 4.0, 1.5, 0.2, 140.0, 75.0, 35.0, 1.5, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Vegetable Dum Biryani", "Prepared Meals", 300.0, "bowl", 410.0, 9.0, 66.0, 12.0, 6.0, 4.0, 510.0, 360.0, 80.0, 2.8, 16.0, 0.1, 0.2});
        list.add(new Object[]{"Vegetable Pulao (Peas & Veggies)", "Prepared Meals", 250.0, "bowl", 310.0, 6.5, 54.0, 7.5, 4.5, 3.0, 360.0, 280.0, 45.0, 2.1, 14.0, 0.0, 0.0});
        list.add(new Object[]{"Curd Rice / Thayir Sadam", "Prepared Meals", 200.0, "bowl", 240.0, 7.5, 36.0, 7.5, 1.2, 4.5, 260.0, 240.0, 210.0, 0.6, 2.0, 0.2, 0.8});
        list.add(new Object[]{"Lemon Rice (South Indian Chitranna)", "Grains", 200.0, "bowl", 280.0, 5.5, 48.0, 7.5, 2.8, 1.0, 290.0, 180.0, 35.0, 1.8, 16.0, 0.0, 0.0});
        list.add(new Object[]{"Tamarind Rice (Puliyogare)", "Grains", 200.0, "bowl", 320.0, 6.0, 52.0, 10.0, 3.5, 3.5, 340.0, 210.0, 40.0, 2.4, 6.0, 0.0, 0.0});

        // ==========================================
        // --- INDIAN SNACKS, STREET FOODS & SUPERFOODS ---
        // ==========================================
        list.add(new Object[]{"Crispy Vegetable Samosa", "Snacks", 80.0, "piece", 260.0, 4.0, 32.0, 13.0, 3.0, 1.8, 310.0, 180.0, 25.0, 1.5, 5.0, 0.0, 0.0});
        list.add(new Object[]{"Khaman Dhokla (Steamed)", "Snacks", 80.0, "2 pieces", 150.0, 6.0, 22.0, 4.0, 3.5, 3.0, 290.0, 190.0, 35.0, 1.8, 3.0, 0.0, 0.0});
        list.add(new Object[]{"Pav Bhaji (with 2 Pavs)", "Prepared Meals", 320.0, "plate", 520.0, 11.0, 72.0, 21.0, 8.5, 6.5, 780.0, 540.0, 95.0, 3.8, 35.0, 0.2, 0.2});
        list.add(new Object[]{"Vada Pav (Mumbai Potato Slider)", "Snacks", 120.0, "piece", 290.0, 5.5, 42.0, 11.5, 3.2, 2.5, 380.0, 220.0, 35.0, 1.8, 8.0, 0.0, 0.0});
        list.add(new Object[]{"Misal Pav (Sprouted Beans)", "Prepared Meals", 300.0, "plate", 430.0, 14.0, 58.0, 16.0, 9.0, 4.0, 680.0, 480.0, 75.0, 4.5, 20.0, 0.0, 0.0});
        list.add(new Object[]{"Bhel Puri (Tangy & Crispy)", "Snacks", 120.0, "plate", 210.0, 5.0, 38.0, 4.5, 4.0, 6.0, 410.0, 220.0, 40.0, 2.5, 8.0, 0.0, 0.0});
        list.add(new Object[]{"Pani Puri / Golgappa (6 puris)", "Snacks", 150.0, "plate (6 puris)", 180.0, 3.5, 32.0, 4.0, 2.5, 3.5, 460.0, 180.0, 30.0, 1.9, 12.0, 0.0, 0.0});
        list.add(new Object[]{"Dahi Puri (Spiced Curd Puris)", "Snacks", 160.0, "plate", 260.0, 6.5, 36.0, 10.5, 3.0, 8.0, 380.0, 240.0, 140.0, 1.8, 10.0, 0.1, 0.4});
        list.add(new Object[]{"Roasted Masala Makhana (Foxnuts)", "Snacks", 30.0, "bowl", 120.0, 3.2, 20.0, 3.5, 4.0, 0.5, 140.0, 120.0, 45.0, 1.2, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Sprouted Moong Chaat (High Fiber)", "Snacks", 150.0, "bowl", 160.0, 11.0, 26.0, 1.8, 7.5, 3.0, 180.0, 410.0, 45.0, 3.5, 24.0, 0.0, 0.0});
        list.add(new Object[]{"Crispy Onion Pakoda / Bhajiya", "Snacks", 80.0, "serving", 230.0, 4.2, 24.0, 13.5, 3.0, 2.0, 280.0, 160.0, 28.0, 1.4, 6.0, 0.0, 0.0});

        // ==========================================
        // --- INDIAN BEVERAGES & TRADITIONAL DRINKS ---
        // ==========================================
        list.add(new Object[]{"Masala Chai (Cardamom Ginger)", "Beverages", 150.0, "cup", 95.0, 3.2, 12.0, 3.5, 0.0, 10.0, 45.0, 160.0, 110.0, 0.2, 1.0, 0.4, 0.4});
        list.add(new Object[]{"South Indian Filter Coffee", "Beverages", 150.0, "cup", 110.0, 3.8, 14.0, 4.0, 0.0, 11.0, 50.0, 170.0, 125.0, 0.2, 0.0, 0.4, 0.5});
        list.add(new Object[]{"Salted Masala Chaas / Buttermilk", "Beverages", 200.0, "glass", 55.0, 3.6, 5.0, 2.0, 0.5, 4.5, 280.0, 210.0, 135.0, 0.2, 4.0, 0.2, 0.6});
        list.add(new Object[]{"Sweet Mango Lassi", "Beverages", 240.0, "glass", 230.0, 6.5, 38.0, 6.0, 1.5, 32.0, 95.0, 320.0, 210.0, 0.4, 18.0, 0.5, 0.8});
        list.add(new Object[]{"Plain Sweet Lassi", "Beverages", 240.0, "glass", 190.0, 7.2, 28.0, 5.5, 0.0, 24.0, 90.0, 310.0, 240.0, 0.2, 1.0, 0.5, 0.8});
        list.add(new Object[]{"Badam Milk (Warm Almond Saffron)", "Beverages", 200.0, "glass", 180.0, 6.5, 20.0, 8.5, 1.2, 16.0, 85.0, 290.0, 250.0, 0.8, 1.5, 0.5, 0.7});
        list.add(new Object[]{"Sattu Sharbat (Protein Cooler)", "Beverages", 250.0, "glass", 150.0, 8.5, 24.0, 2.2, 5.0, 3.0, 210.0, 280.0, 45.0, 2.8, 12.0, 0.0, 0.0});
        list.add(new Object[]{"Turmeric Golden Milk (Haldi Doodh)", "Beverages", 200.0, "cup", 140.0, 6.2, 14.0, 6.5, 0.5, 12.0, 80.0, 270.0, 220.0, 0.5, 1.0, 0.4, 0.7});

        // ==========================================
        // --- INDIAN SWEETS & DESSERTS ---
        // ==========================================
        list.add(new Object[]{"Gulab Jamun (2 pieces)", "Snacks", 70.0, "2 pieces", 280.0, 4.0, 44.0, 10.0, 0.5, 36.0, 90.0, 80.0, 85.0, 0.5, 0.0, 0.1, 0.2});
        list.add(new Object[]{"Spongy Rasgulla (2 pieces)", "Snacks", 80.0, "2 pieces", 210.0, 5.5, 42.0, 2.5, 0.0, 38.0, 65.0, 90.0, 110.0, 0.4, 0.0, 0.2, 0.4});
        list.add(new Object[]{"Kesar Rasmalai (2 pieces)", "Snacks", 100.0, "2 pieces", 250.0, 7.5, 32.0, 10.5, 0.5, 26.0, 85.0, 190.0, 180.0, 0.6, 0.5, 0.3, 0.6});
        list.add(new Object[]{"Gajar Ka Halwa (Carrot Pudding)", "Snacks", 100.0, "bowl", 260.0, 4.5, 34.0, 12.0, 2.5, 26.0, 75.0, 220.0, 120.0, 0.8, 4.0, 0.2, 0.3});
        list.add(new Object[]{"Rice Kheer / Payasam", "Snacks", 120.0, "bowl", 220.0, 5.5, 32.0, 8.0, 0.5, 24.0, 80.0, 210.0, 160.0, 0.4, 0.5, 0.3, 0.6});
        list.add(new Object[]{"Kaju Katli (Cashew Fudge, 2 pcs)", "Snacks", 40.0, "2 pieces", 180.0, 4.2, 22.0, 8.5, 1.2, 16.0, 15.0, 110.0, 20.0, 1.1, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Mysore Pak (Gram Flour Sweet)", "Snacks", 40.0, "piece", 210.0, 2.5, 24.0, 12.0, 0.8, 18.0, 25.0, 60.0, 15.0, 0.6, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Besan Ladoo (1 piece)", "Snacks", 40.0, "piece", 190.0, 3.8, 22.0, 10.0, 1.8, 15.0, 20.0, 95.0, 22.0, 1.0, 0.0, 0.0, 0.0});
        list.add(new Object[]{"Crispy Jalebi (3 spirals)", "Snacks", 60.0, "serving (3 pcs)", 240.0, 1.8, 44.0, 6.8, 0.2, 34.0, 35.0, 45.0, 18.0, 0.5, 0.0, 0.0, 0.0});

        return list;
    }
}
