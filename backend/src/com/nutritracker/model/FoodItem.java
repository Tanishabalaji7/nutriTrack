package com.nutritracker.model;

import org.json.JSONObject;

public class FoodItem {
    private int id;
    private String name;
    private String category; // "Proteins", "Grains", "Vegetables", "Fruits", "Dairy", "Healthy Fats", "Snacks", "Beverages"
    private double servingSize;
    private String servingUnit; // "g", "ml", "cup", "slice", "piece", "oz"
    private double calories;
    private double proteinG;
    private double carbsG;
    private double fatG;
    private double fiberG;
    private double sugarG;
    private double sodiumMg;
    private double potassiumMg;
    private double calciumMg;
    private double ironMg;
    private double vitaminCMg;
    private double vitaminDMcg;
    private double vitaminB12Mcg;
    private boolean isCustom;

    public FoodItem() {
        this.servingSize = 100.0;
        this.servingUnit = "g";
    }

    public FoodItem(int id, String name, String category, double servingSize, String servingUnit,
                    double calories, double proteinG, double carbsG, double fatG,
                    double fiberG, double sugarG, double sodiumMg, double potassiumMg,
                    double calciumMg, double ironMg, double vitaminCMg, double vitaminDMcg,
                    double vitaminB12Mcg, boolean isCustom) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.servingSize = servingSize;
        this.servingUnit = servingUnit;
        this.calories = calories;
        this.proteinG = proteinG;
        this.carbsG = carbsG;
        this.fatG = fatG;
        this.fiberG = fiberG;
        this.sugarG = sugarG;
        this.sodiumMg = sodiumMg;
        this.potassiumMg = potassiumMg;
        this.calciumMg = calciumMg;
        this.ironMg = ironMg;
        this.vitaminCMg = vitaminCMg;
        this.vitaminDMcg = vitaminDMcg;
        this.vitaminB12Mcg = vitaminB12Mcg;
        this.isCustom = isCustom;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getServingSize() { return servingSize; }
    public void setServingSize(double servingSize) { this.servingSize = servingSize; }

    public String getServingUnit() { return servingUnit; }
    public void setServingUnit(String servingUnit) { this.servingUnit = servingUnit; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public double getProteinG() { return proteinG; }
    public void setProteinG(double proteinG) { this.proteinG = proteinG; }

    public double getCarbsG() { return carbsG; }
    public void setCarbsG(double carbsG) { this.carbsG = carbsG; }

    public double getFatG() { return fatG; }
    public void setFatG(double fatG) { this.fatG = fatG; }

    public double getFiberG() { return fiberG; }
    public void setFiberG(double fiberG) { this.fiberG = fiberG; }

    public double getSugarG() { return sugarG; }
    public void setSugarG(double sugarG) { this.sugarG = sugarG; }

    public double getSodiumMg() { return sodiumMg; }
    public void setSodiumMg(double sodiumMg) { this.sodiumMg = sodiumMg; }

    public double getPotassiumMg() { return potassiumMg; }
    public void setPotassiumMg(double potassiumMg) { this.potassiumMg = potassiumMg; }

    public double getCalciumMg() { return calciumMg; }
    public void setCalciumMg(double calciumMg) { this.calciumMg = calciumMg; }

    public double getIronMg() { return ironMg; }
    public void setIronMg(double ironMg) { this.ironMg = ironMg; }

    public double getVitaminCMg() { return vitaminCMg; }
    public void setVitaminCMg(double vitaminCMg) { this.vitaminCMg = vitaminCMg; }

    public double getVitaminDMcg() { return vitaminDMcg; }
    public void setVitaminDMcg(double vitaminDMcg) { this.vitaminDMcg = vitaminDMcg; }

    public double getVitaminB12Mcg() { return vitaminB12Mcg; }
    public void setVitaminB12Mcg(double vitaminB12Mcg) { this.vitaminB12Mcg = vitaminB12Mcg; }

    public boolean isCustom() { return isCustom; }
    public void setCustom(boolean custom) { isCustom = custom; }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("name", name);
        json.put("category", category != null ? category : "General");
        json.put("servingSize", servingSize);
        json.put("servingUnit", servingUnit != null ? servingUnit : "g");
        json.put("calories", calories);
        json.put("proteinG", proteinG);
        json.put("carbsG", carbsG);
        json.put("fatG", fatG);
        json.put("fiberG", fiberG);
        json.put("sugarG", sugarG);
        json.put("sodiumMg", sodiumMg);
        json.put("potassiumMg", potassiumMg);
        json.put("calciumMg", calciumMg);
        json.put("ironMg", ironMg);
        json.put("vitaminCMg", vitaminCMg);
        json.put("vitaminDMcg", vitaminDMcg);
        json.put("vitaminB12Mcg", vitaminB12Mcg);
        json.put("isCustom", isCustom);
        return json;
    }

    public static FoodItem fromJSON(JSONObject json) {
        FoodItem f = new FoodItem();
        if (json.has("id")) f.setId(json.getInt("id"));
        if (json.has("name")) f.setName(json.getString("name"));
        if (json.has("category")) f.setCategory(json.getString("category"));
        if (json.has("servingSize")) f.setServingSize(json.getDouble("servingSize"));
        if (json.has("servingUnit")) f.setServingUnit(json.getString("servingUnit"));
        if (json.has("calories")) f.setCalories(json.getDouble("calories"));
        if (json.has("proteinG")) f.setProteinG(json.getDouble("proteinG"));
        if (json.has("carbsG")) f.setCarbsG(json.getDouble("carbsG"));
        if (json.has("fatG")) f.setFatG(json.getDouble("fatG"));
        if (json.has("fiberG")) f.setFiberG(json.getDouble("fiberG"));
        if (json.has("sugarG")) f.setSugarG(json.getDouble("sugarG"));
        if (json.has("sodiumMg")) f.setSodiumMg(json.getDouble("sodiumMg"));
        if (json.has("potassiumMg")) f.setPotassiumMg(json.getDouble("potassiumMg"));
        if (json.has("calciumMg")) f.setCalciumMg(json.getDouble("calciumMg"));
        if (json.has("ironMg")) f.setIronMg(json.getDouble("ironMg"));
        if (json.has("vitaminCMg")) f.setVitaminCMg(json.getDouble("vitaminCMg"));
        if (json.has("vitaminDMcg")) f.setVitaminDMcg(json.getDouble("vitaminDMcg"));
        if (json.has("vitaminB12Mcg")) f.setVitaminB12Mcg(json.getDouble("vitaminB12Mcg"));
        if (json.has("isCustom")) f.setCustom(json.getBoolean("isCustom"));
        return f;
    }
}
