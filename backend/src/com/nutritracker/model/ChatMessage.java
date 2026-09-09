package com.nutritracker.model;

import org.json.JSONObject;

public class ChatMessage {
    private int id;
    private int userId;
    private String sender; // "user" or "bot"
    private String message;
    private String actionType; // "advice", "logged_food", "recipe", "risk_alert", "profile_update", "general"
    private String metadataJson;
    private String timestamp;

    public ChatMessage() {}

    public ChatMessage(int id, int userId, String sender, String message, String actionType, String metadataJson, String timestamp) {
        this.id = id;
        this.userId = userId;
        this.sender = sender;
        this.message = message;
        this.actionType = actionType;
        this.metadataJson = metadataJson;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("userId", userId);
        json.put("sender", sender);
        json.put("message", message);
        json.put("actionType", actionType != null ? actionType : "general");
        if (metadataJson != null && !metadataJson.isEmpty()) {
            try {
                json.put("metadata", new JSONObject(metadataJson));
            } catch (Exception e) {
                json.put("metadata", metadataJson);
            }
        }
        json.put("timestamp", timestamp != null ? timestamp : "");
        return json;
    }
}
