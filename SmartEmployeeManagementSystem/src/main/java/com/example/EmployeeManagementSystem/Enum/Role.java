package com.example.EmployeeManagementSystem.Enum;

import java.util.Collection;
import java.util.Set;

public enum Role {
    EMPLOYEE(Set.of(Permission.LEAVE_WRITE,
            Permission.LEAVE_CANCEL,
            Permission.SUBSCRIPTION_WRITE,
            Permission.SUBSCRIPTION_UPDATE,
            Permission.RESTAURANTS_READ,
            Permission.SUBSCRIPTION_READ)),
    MANAGER(Set.of(Permission.LEAVE_UPDATE,
            Permission.LEAVE_WRITE,
            Permission.SUBSCRIPTION_WRITE,
            Permission.RESTAURANTS_READ,
            Permission.SUBSCRIPTION_UPDATE,
            Permission.LEAVE_READ,
            Permission.SUBSCRIPTION_READ)),
    VENDOR(Set.of(
            Permission.VENDOR_DELETE,
            Permission.RESTAURANTS_ADD,
            Permission.RESTAURANTS_DELETE,
            Permission.RESTAURANTS_READ
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
            Permission.VENDOR_UPDATE,
            Permission.VENDOR_DELETE
    ));
    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
     return permissions;
    }
}
