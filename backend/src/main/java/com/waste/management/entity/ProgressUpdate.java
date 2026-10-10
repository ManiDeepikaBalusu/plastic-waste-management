package com.waste.management.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "progress_updates")
public class ProgressUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(name = "previous_status", length = 40)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 40)
    private String newStatus;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // STATUS_CHANGE, NOTE_ADDED, REASSIGNMENT, VERIFICATION

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ProgressUpdate() {}

    public ProgressUpdate(Complaint complaint, User actor, String previousStatus, String newStatus, String actionType, String remarks) {
        this.complaint = complaint;
        this.actor = actor;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actionType = actionType;
        this.remarks = remarks;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }

    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
