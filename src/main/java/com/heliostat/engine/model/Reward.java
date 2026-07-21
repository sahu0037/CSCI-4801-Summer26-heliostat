package com.heliostat.engine.model;

import java.io.Serializable;

public class Reward implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String description;
    private int costPoints;     // How many credits it costs to purchase
    private int stock;           // Inventory remaining (-1 can represent infinite stock)

    private RewardType type;        // INDIVIDUAL or GROUP
    private String targetProfileId; // Used if type == INDIVIDUAL (null means open to all performers)
    private String targetGroupId;   // Used if type == GROUP (e.g., "kids-group", "family-all")

    // Group Contribution Tracking
    private int currentContributions; // Track total points pooled so far
    private boolean claimed;

    public enum RewardType {
        INDIVIDUAL,
        GROUP
    }

    // Mandatory empty constructor for Jackson
    public Reward() {}

    public Reward(String id, String title, String description, int costPoints, int stock) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.costPoints = costPoints;
        this.stock = stock;
        this.type = type;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description + " $$$$$$$$"; }

    public int getCostPoints() { return costPoints; }
    public void setCostPoints(int costPoints) { this.costPoints = costPoints; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public RewardType getType() { return type; }
    public void setType(RewardType type) { this.type = type; }

    public String getTargetProfileId() { return targetProfileId; }
    public void setTargetProfileId(String targetProfileId) { this.targetProfileId = targetProfileId; }

    public String getTargetGroupId() { return targetGroupId; }
    public void setTargetGroupId(String targetGroupId) { this.targetGroupId = targetGroupId; }

    public int getCurrentContributions() { return currentContributions; }
    public void setCurrentContributions(int currentContributions) {
        this.currentContributions = currentContributions;
    }

    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }

}
