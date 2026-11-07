package com.arms.model;

public record Payment(int paymentId, int tenantId, int unitId, double amountPaid,
                      String paymentDate, String paymentStatus, String receiptNumber) {
}