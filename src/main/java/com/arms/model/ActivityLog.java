package com.arms.model;

public record ActivityLog(int id, int userId, String actionType, String description, String timestamp) {
}