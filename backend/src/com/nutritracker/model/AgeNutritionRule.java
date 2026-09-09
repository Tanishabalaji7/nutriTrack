package com.nutritracker.model;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AgeNutritionRule {
    private String bracketName; // e.g. "Young Adult (19-30)"
    private int minAge;
    private int maxAge;
    private double proteinPerKg; // g/kg body weight
    private double fiberGrams;
    private double calciumMg;
    private double vitaminDMcg;
    private double vitaminB12Mcg;
    private double ironMgMale;
    private double ironMgFemale;
    private double sodiumMaxMg;
    private double potassiumMg;
    private String physiologicalFocus;
    private List<String> priorityNutrients;
    private List<String> recommendedFoods;
    private List<String> ageSpecificRisks;
    private List<String> actionableTips;

    public AgeNutritionRule(String bracketName, int minAge, int maxAge, double proteinPerKg,
                            double fiberGrams, double calciumMg, double vitaminDMcg,
                            double vitaminB12Mcg, double ironMgMale, double ironMgFemale,
                            double sodiumMaxMg, double potassiumMg, String physiologicalFocus) {
        this.bracketName = bracketName;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.proteinPerKg = proteinPerKg;
        this.fiberGrams = fiberGrams;
        this.calciumMg = calciumMg;
        this.vitaminDMcg = vitaminDMcg;
        this.vitaminB12Mcg = vitaminB12Mcg;
        this.ironMgMale = ironMgMale;
        this.ironMgFemale = ironMgFemale;
        this.sodiumMaxMg = sodiumMaxMg;
        this.potassiumMg = potassiumMg;
        this.physiologicalFocus = physiologicalFocus;
        this.priorityNutrients = new ArrayList<>();
        this.recommendedFoods = new ArrayList<>();
        this.ageSpecificRisks = new ArrayList<>();
        this.actionableTips = new ArrayList<>();
    }

    public static AgeNutritionRule getRuleForAge(int age) {
        AgeNutritionRule rule;
        if (age <= 3) {
            rule = new AgeNutritionRule("Toddler (1-3 yrs)", 1, 3, 1.1, 19, 700, 15, 0.9, 7, 7, 1200, 2000,
                    "Rapid brain & physical growth, fat for myelin sheath synthesis, high calcium for teeth/bone formation.");
            rule.priorityNutrients.add("DHA & Healthy Fats");
            rule.priorityNutrients.add("Calcium (700mg)");
            rule.priorityNutrients.add("Iron (7mg)");
            rule.recommendedFoods.add("Whole milk & fortified yogurt");
            rule.recommendedFoods.add("Avocado & pureed beans");
            rule.recommendedFoods.add("Soft eggs & sweet potatoes");
            rule.ageSpecificRisks.add("Choking hazards, excess fruit juice sugar, iron deficiency anemia.");
            rule.actionableTips.add("Encourage nutrient-dense finger foods; avoid added salt and high-fructose syrups.");
        } else if (age <= 8) {
            rule = new AgeNutritionRule("Child (4-8 yrs)", 4, 8, 0.95, 25, 1000, 15, 1.2, 10, 10, 1500, 2300,
                    "Sustained linear growth, cognitive development, immune system fortification, habit formation.");
            rule.priorityNutrients.add("Calcium (1000mg)");
            rule.priorityNutrients.add("Vitamin C & Zinc");
            rule.priorityNutrients.add("Fiber (25g)");
            rule.recommendedFoods.add("Fortified dairy/plant milk, berries, oatmeal, lean poultry, carrots");
            rule.ageSpecificRisks.add("Ultra-processed snack addiction, low vegetable acceptance, dental caries from sugary drinks.");
            rule.actionableTips.add("Make meals colorful; pair fiber-rich fruits with protein for sustained school energy.");
        } else if (age <= 18) {
            rule = new AgeNutritionRule("Adolescent (9-18 yrs)", 9, 18, 1.0, 31, 1300, 15, 2.4, 11, 15, 2200, 3000,
                    "Peak bone mass accretion (90% of adult skeleton built here), hormonal shifts, rapid growth spurts.");
            rule.priorityNutrients.add("Calcium (1300mg - Highest in Life)");
            rule.priorityNutrients.add("Iron (15mg for females)");
            rule.priorityNutrients.add("Protein & Zinc");
            rule.recommendedFoods.add("Greek yogurt, spinach, lentils, eggs, tofu, nuts, lean beef, salmon");
            rule.ageSpecificRisks.add("Skipping breakfast, energy drink consumption, low calcium impairing future bone density.");
            rule.actionableTips.add("Pair plant-based iron with Vitamin C for absorption; prioritize calcium in every meal.");
        } else if (age <= 30) {
            rule = new AgeNutritionRule("Young Adult (19-30 yrs)", 19, 30, 1.2, 34, 1000, 15, 2.4, 8, 18, 2300, 3400,
                    "Peak metabolic rate, muscle mass optimization, reproductive health, cognitive endurance & stress resilience.");
            rule.priorityNutrients.add("Quality Protein (1.2-1.6g/kg)");
            rule.priorityNutrients.add("Folate & B-Complex");
            rule.priorityNutrients.add("Magnesium & Omega-3s");
            rule.recommendedFoods.add("Quinoa, chia seeds, wild salmon, broccoli, walnuts, chicken breast, dark leafy greens");
            rule.ageSpecificRisks.add("Irregular eating, high dining-out sodium, alcohol-induced nutrient depletion, high caffeine.");
            rule.actionableTips.add("Focus on whole food macro balance and hydration; meal prep to avoid convenience fast food.");
        } else if (age <= 50) {
            rule = new AgeNutritionRule("Adult (31-50 yrs)", 31, 50, 1.1, 30, 1000, 15, 2.4, 8, 18, 2300, 3400,
                    "Metabolism slowdown (2-3% per decade), visceral fat management, cellular repair & cardiovascular protection.");
            rule.priorityNutrients.add("Soluble Fiber (30g)");
            rule.priorityNutrients.add("Antioxidants (Vitamins C & E)");
            rule.priorityNutrients.add("Potassium for blood pressure");
            rule.recommendedFoods.add("Berries, extra virgin olive oil, oats, edamame, turmeric, fatty fish, almonds");
            rule.ageSpecificRisks.add("Insulin resistance, rising LDL cholesterol, sedentary muscle loss (sarcopenia onset).");
            rule.actionableTips.add("Shift carb intake toward low glycemic legumes and vegetables; keep sodium under 2300mg.");
        } else if (age <= 70) {
            rule = new AgeNutritionRule("Mature Adult (51-70 yrs)", 51, 70, 1.25, 28, 1200, 20, 2.4, 8, 8, 1500, 3400,
                    "Bone density preservation, hormone fluctuations (post-menopause), cardiovascular elasticity, reduced stomach acid.");
            rule.priorityNutrients.add("Calcium (1200mg)");
            rule.priorityNutrients.add("Vitamin D (20mcg / 800 IU)");
            rule.priorityNutrients.add("Vitamin B12 (Bioavailable)");
            rule.recommendedFoods.add("Fortified nutritional yeast, sardines with bones, kale, kefir, flaxseed, citrus, lean proteins");
            rule.ageSpecificRisks.add("Osteopenia/osteoporosis, reduced B12 absorption, arterial stiffness from sodium.");
            rule.actionableTips.add("Limit sodium to 1500mg/day; ensure daily Vitamin D and weight-bearing exercise protein.");
        } else {
            rule = new AgeNutritionRule("Senior / Elderly (71+ yrs)", 71, 120, 1.35, 25, 1200, 20, 2.4, 8, 8, 1500, 3400,
                    "Countering muscle wasting (sarcopenia), cognitive preservation, kidney function support, hydration awareness.");
            rule.priorityNutrients.add("High Bioavailability Protein (1.3-1.5g/kg)");
            rule.priorityNutrients.add("Hydration & Electrolytes");
            rule.priorityNutrients.add("Vitamin D & B12");
            rule.priorityNutrients.add("Lutein & Zeaxanthin (Eye Health)");
            rule.recommendedFoods.add("Soft scrambled eggs, smoothies with whey/plant protein, avocado, fortified cereals, soups");
            rule.ageSpecificRisks.add("Blunted thirst sensation leading to chronic dehydration, protein malnutrition, medication nutrient interactions.");
            rule.actionableTips.add("Eat protein at every meal (25-30g per meal); sip water consistently throughout the day.");
        }
        return rule;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("bracketName", bracketName);
        json.put("minAge", minAge);
        json.put("maxAge", maxAge);
        json.put("proteinPerKg", proteinPerKg);
        json.put("fiberGrams", fiberGrams);
        json.put("calciumMg", calciumMg);
        json.put("vitaminDMcg", vitaminDMcg);
        json.put("vitaminB12Mcg", vitaminB12Mcg);
        json.put("ironMgMale", ironMgMale);
        json.put("ironMgFemale", ironMgFemale);
        json.put("sodiumMaxMg", sodiumMaxMg);
        json.put("potassiumMg", potassiumMg);
        json.put("physiologicalFocus", physiologicalFocus);
        json.put("priorityNutrients", new JSONArray(priorityNutrients));
        json.put("recommendedFoods", new JSONArray(recommendedFoods));
        json.put("ageSpecificRisks", new JSONArray(ageSpecificRisks));
        json.put("actionableTips", new JSONArray(actionableTips));
        return json;
    }

    public String getBracketName() { return bracketName; }
    public double getProteinPerKg() { return proteinPerKg; }
    public double getFiberGrams() { return fiberGrams; }
    public double getCalciumMg() { return calciumMg; }
    public double getVitaminDMcg() { return vitaminDMcg; }
    public double getVitaminB12Mcg() { return vitaminB12Mcg; }
    public double getIronMg(String gender) { return "female".equalsIgnoreCase(gender) ? ironMgFemale : ironMgMale; }
    public double getSodiumMaxMg() { return sodiumMaxMg; }
    public double getPotassiumMg() { return potassiumMg; }
    public String getPhysiologicalFocus() { return physiologicalFocus; }
    public List<String> getPriorityNutrients() { return priorityNutrients; }
    public List<String> getRecommendedFoods() { return recommendedFoods; }
    public List<String> getAgeSpecificRisks() { return ageSpecificRisks; }
    public List<String> getActionableTips() { return actionableTips; }
}
