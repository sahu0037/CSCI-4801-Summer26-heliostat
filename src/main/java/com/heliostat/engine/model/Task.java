package com.heliostat.engine.model;

import java.io.Serializable;

public class Task implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String description;
    private int rewardPoints;
    private String managerId;   // The profile ID that created/approves the task
    private String performerId; // The profile ID assigned to do the work (can be null initially)
    private Status status;

    private boolean requiresReview; // true = Parental Review Required, false = Immediate Confirmation
    private String dueDate;        // ISO Format: "YYYY-MM-DD" or "YYYY-MM-DDTHH:mm:ss"
    private RecurrencePattern recurrence;

    public enum Status {
        AVAILABLE,   // Created, open for anyone to claim
        ASSIGNED,    // In progress by a specific performer
        SUBMITTED,   // Work completed, waiting for Manager approval
        APPROVED     // Verified, points paid out (terminal state)
    }

    public enum RecurrencePattern {
        NONE,      // One-time task
        DAILY,     // Repeats every day
        WEEKLY,    // Repeats every 7 days
        MONTHLY    // Repeats monthly
    }
    // CRITICAL: Empty constructor for Jackson JSON mapping
    public Task() {}

    public Task(String id, String title, String description, int rewardPoints, String managerId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.rewardPoints = rewardPoints;
        this.managerId = managerId;
        this.status = Status.AVAILABLE;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description + "  #####"; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    public String getPerformerId() { return performerId; }
    public void setPerformerId(String performerId) { this.performerId = performerId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isRequiresReview() { return requiresReview; }
    public void setRequiresReview(boolean requiresReview) { this.requiresReview = requiresReview; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public RecurrencePattern getRecurrence() { return recurrence; }
    public void setRecurrence(RecurrencePattern recurrence) { this.recurrence = recurrence; }
}
