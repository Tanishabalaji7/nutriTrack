package com.nutritracker.dao;

import com.nutritracker.database.DatabaseManager;
import com.nutritracker.model.ChatMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatDao {
    private final DatabaseManager dbManager;

    public ChatDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public ChatMessage saveMessage(ChatMessage msg) {
        String sql = "INSERT INTO chat_history (user_id, sender, message, action_type, metadata_json) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, msg.getUserId() > 0 ? msg.getUserId() : 1);
            ps.setString(2, msg.getSender());
            ps.setString(3, msg.getMessage());
            ps.setString(4, msg.getActionType());
            ps.setString(5, msg.getMetadataJson());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    msg.setId(rs.getInt(1));
                }
            }
            return msg;
        } catch (SQLException e) {
            System.err.println("Error saving chat message: " + e.getMessage());
            return null;
        }
    }

    public List<ChatMessage> getRecentMessages(int userId, int limit) {
        List<ChatMessage> list = new ArrayList<>();
        String sql = "SELECT * FROM (SELECT * FROM chat_history WHERE user_id = ? ORDER BY id DESC LIMIT ?) ORDER BY id ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit > 0 ? limit : 50);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ChatMessage(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("sender"),
                            rs.getString("message"),
                            rs.getString("action_type"),
                            rs.getString("metadata_json"),
                            rs.getString("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting recent chat messages: " + e.getMessage());
        }
        return list;
    }

    public boolean clearChatHistory(int userId) {
        String sql = "DELETE FROM chat_history WHERE user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            System.err.println("Error clearing chat history: " + e.getMessage());
            return false;
        }
    }
}
