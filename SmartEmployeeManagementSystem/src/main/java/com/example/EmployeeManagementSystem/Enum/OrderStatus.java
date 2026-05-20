package com.example.EmployeeManagementSystem.Enum;

public enum OrderStatus {
    PLACED,       // Employee has placed the order
    CONFIRMED,    // Vendor has confirmed and ordered from external service
    ARRIVED,      // Vendor marked the food as arrived at the office
    COLLECTED     // Employee has collected the meal
}
