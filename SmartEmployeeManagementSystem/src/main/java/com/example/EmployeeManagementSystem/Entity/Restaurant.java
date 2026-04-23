package com.example.EmployeeManagementSystem.Entity;

import com.example.EmployeeManagementSystem.Enum.MealSlot;
import jakarta.persistence.*;
import java.util.Set;


@Entity
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    private String description;

    @ElementCollection(targetClass = MealSlot.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "restaurant_meal_slots",
            joinColumns = @JoinColumn(name = "restaurant_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_slot")
    private Set<MealSlot> supportedMealSlots;

    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<MealSlot> getSupportedMealSlots() { return supportedMealSlots; }
    public void setSupportedMealSlots(Set<MealSlot> supportedMealSlots) {
        this.supportedMealSlots = supportedMealSlots;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Vendor getVendor() { return vendor; }
    public void setVendor(Vendor vendor) { this.vendor = vendor; }
}