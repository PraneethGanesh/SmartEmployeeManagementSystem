package com.example.EmployeeManagementSystem.Enum;

import java.util.Collection;
import java.util.Set;

import static com.example.EmployeeManagementSystem.Enum.Permission.*;


public enum Role {
    EMPLOYEE(Set.of(LEAVE_WRITE,
           LEAVE_CANCEL,
            SUBSCRIPTION_WRITE,
           SUBSCRIPTION_UPDATE,
            RESTAURANTS_READ,
            SUBSCRIPTION_READ,
            PROFILE_READ)),
    MANAGER(Set.of(LEAVE_UPDATE,
            LEAVE_WRITE,
            SUBSCRIPTION_WRITE,
            RESTAURANTS_READ,
            SUBSCRIPTION_UPDATE,
            LEAVE_READ,
            SUBSCRIPTION_READ,
            PROFILE_READ)),
    FOOD_VENDOR(Set.of(
            VENDOR_WRITE,
            VENDOR_DELETE,
            RESTAURANTS_ADD,
            RESTAURANTS_DELETE,
            RESTAURANTS_READ,
            PROFILE_READ
    )),
    ADMIN(Set.of(
            LEAVE_READ,
            LEAVE_WRITE,
            LEAVE_UPDATE,
            LEAVE_CANCEL,
            RESTAURANTS_READ,
            SUBSCRIPTION_READ,
            SUBSCRIPTION_WRITE,
            SUBSCRIPTION_UPDATE,
            VENDOR_WRITE,
            VENDOR_UPDATE,
            VENDOR_DELETE,
            PROFILE_READ

    )),
    TECH_VENDOR(Set.of(VENDOR_WRITE)),
    USER(Set.of(PROFILE_READ));
    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
     return permissions;
    }
}
