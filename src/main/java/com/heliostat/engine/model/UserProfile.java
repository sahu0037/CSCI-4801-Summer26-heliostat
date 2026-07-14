package com.heliostat.engine.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class UserProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String pinHash;
    private int balance;
    private boolean isActive;
    private Set<Role> roles = new HashSet<>();

    public enum Role { OWNER, MANAGER, PERFORMER }

    // CRITICAL: Empty constructor required by Jackson for JSON deserialization
    public UserProfile() {}

    public UserProfile(String id, String name, String pinHash) {
        this.id = id;
        this.name = name;
        this.pinHash = pinHash;
        this.balance = 0;
        this.isActive = true;
    }

    // Getters and Setters for all fields...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = balance; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}