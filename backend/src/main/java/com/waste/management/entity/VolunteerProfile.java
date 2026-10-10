package com.waste.management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "volunteer_profiles")
public class VolunteerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = false;

    @Column(name = "current_latitude", precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 10, scale = 7)
    private BigDecimal currentLongitude;

    @Column(name = "last_location_update")
    private OffsetDateTime lastLocationUpdate;

    @Column(name = "total_assigned", nullable = false)
    private Integer totalAssigned = 0;

    @Column(name = "total_resolved", nullable = false)
    private Integer totalResolved = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public VolunteerProfile() {}

    public VolunteerProfile(User user) {
        this.user = user;
        this.isAvailable = false;
        this.totalAssigned = 0;
        this.totalResolved = 0;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }

    public BigDecimal getCurrentLatitude() { return currentLatitude; }
    public void setCurrentLatitude(BigDecimal currentLatitude) { this.currentLatitude = currentLatitude; }

    public BigDecimal getCurrentLongitude() { return currentLongitude; }
    public void setCurrentLongitude(BigDecimal currentLongitude) { this.currentLongitude = currentLongitude; }

    public OffsetDateTime getLastLocationUpdate() { return lastLocationUpdate; }
    public void setLastLocationUpdate(OffsetDateTime lastLocationUpdate) { this.lastLocationUpdate = lastLocationUpdate; }

    public Integer getTotalAssigned() { return totalAssigned; }
    public void setTotalAssigned(Integer totalAssigned) { this.totalAssigned = totalAssigned; }

    public Integer getTotalResolved() { return totalResolved; }
    public void setTotalResolved(Integer totalResolved) { this.totalResolved = totalResolved; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
