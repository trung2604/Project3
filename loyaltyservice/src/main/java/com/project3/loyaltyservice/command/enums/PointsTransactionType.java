package com.project3.loyaltyservice.command.enums;

public enum PointsTransactionType {
    EARNED,          // Tích điểm từ đơn hàng
    REDEEMED,        // Đổi điểm lấy voucher
    EXPIRED,         // Điểm hết hạn
    ADJUSTED,        // Điều chỉnh điểm (admin)
    BONUS            // Điểm thưởng đặc biệt
}

