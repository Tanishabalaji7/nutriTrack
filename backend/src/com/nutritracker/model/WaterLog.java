package com.nutritracker.model;

import org.json.JSONObject;

public class WaterLog {
    private int id;
    private int userId;
    private String date;
    private int amountMl;
    private String timestamp;

    public WaterLog() {}

    public WaterLog(int id, int userId, String date, int amountMl, String timestamp) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.amountMl = amountMl;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getAmountMl() { return amountMl; }
    public void setAmountMl(int amountMl) { this.amountMl = amountMl; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("userId", userId);
        json.put("date", date);
        json.put("amountMl", amountMl);
        json.put("timestamp", timestamp != null ? timestamp : "");
        return json;
    }
}
