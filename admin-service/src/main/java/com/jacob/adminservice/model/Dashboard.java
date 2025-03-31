package com.jacob.adminservice.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class Dashboard {
    private long totalUsers;
    private long totalProducts;
    private long totalOrders;
    private long totalRevenue;
    private long activeUsers;
    private long newOrdersToday;
    private long lowStockProducts;
} 