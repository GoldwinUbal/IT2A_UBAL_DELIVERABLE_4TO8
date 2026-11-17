package com.arms.model;

public record Tenant(int tenantId, int userId, String name, String contact, int unitId, String status) {
}