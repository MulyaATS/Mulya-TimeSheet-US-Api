package com.mulya.employee.timesheet.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_carry_forward")
public class LeaveCarryForward {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "carry_forward_leaves", nullable = false)
    private int carryForwardLeaves;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public LeaveCarryForward() {}

    public LeaveCarryForward(String userId, int carryForwardLeaves) {
        this.userId = userId;
        this.carryForwardLeaves = carryForwardLeaves;
    }

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getCarryForwardLeaves() {
        return carryForwardLeaves;
    }

    public void setCarryForwardLeaves(int carryForwardLeaves) {
        this.carryForwardLeaves = carryForwardLeaves;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
