package com.example.EmployeeManagementSystem.Enum;

import java.util.Collection;
import java.util.Set;

public enum Role {
    EMPLOYEE(Set.of(Permission.LEAVE_WRITE,
            Permission.LEAVE_CANCEL,
            Permission.SUBSCRIPTION_WRITE,
            Permission.SUBSCRIPTION_UPDATE,
            Permission.RESTAURANTS_READ,
            Permission.SUBSCRIPTION_READ,
            Permission.PROFILE_READ)),
    MANAGER(Set.of(Permission.LEAVE_UPDATE,
            Permission.LEAVE_WRITE,
            Permission.SUBSCRIPTION_WRITE,
            Permission.RESTAURANTS_READ,
            Permission.SUBSCRIPTION_UPDATE,
            Permission.LEAVE_READ,
            Permission.SUBSCRIPTION_READ,
            Permission.PROFILE_READ)),
    FOOD_VENDOR(Set.of(
            Permission.VENDOR_WRITE,
            Permission.VENDOR_DELETE,
            Permission.RESTAURANTS_ADD,
            Permission.RESTAURANTS_DELETE,
            Permission.RESTAURANTS_READ,
            Permission.PROFILE_READ
    )),
    ADMIN(Set.of(
            Permission.LEAVE_READ,
            Permission.LEAVE_WRITE,
            Permission.LEAVE_UPDATE,
            Permission.LEAVE_CANCEL,
            Permission.RESTAURANTS_READ,
            Permission.SUBSCRIPTION_READ,
            Permission.SUBSCRIPTION_WRITE,
            Permission.SUBSCRIPTION_UPDATE,
            Permission.VENDOR_WRITE,
            Permission.VENDOR_UPDATE,
            Permission.VENDOR_DELETE,
            Permission.PROFILE_READ

    )),
    TECH_VENDOR(Set.of()),
    USER(Set.of(Permission.PROFILE_READ));
    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
     return permissions;
    }
}
