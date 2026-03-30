package com.qprint.shops.dto;

public record ShopDto(
        String id,
        String name,
        String address,
        int avgWaitMinutes,
        boolean isOpen,
        String distance,
        double rating,
        int totalOrders
) {
}
