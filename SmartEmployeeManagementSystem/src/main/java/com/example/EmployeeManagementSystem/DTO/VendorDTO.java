package com.example.EmployeeManagementSystem.DTO;

import com.example.EmployeeManagementSystem.Enum.Role;

import java.time.LocalDate;

public class VendorDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private LocalDate registeredAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDate getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDate registeredAt) { this.registeredAt = registeredAt; }
}
