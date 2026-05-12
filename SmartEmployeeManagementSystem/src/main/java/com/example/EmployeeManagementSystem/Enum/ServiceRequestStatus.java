package com.example.EmployeeManagementSystem.Enum;

public enum ServiceRequestStatus {
    OPEN,
    SENT_FOR_REPAIR,
    APPROVED, //this is for replacement, return, maintainance
    RECEIVED_BY_VENDOR ,
    REPAIR_DONE,
    REJECTED,
    IRREPARABLE,
    CLOSED
}
