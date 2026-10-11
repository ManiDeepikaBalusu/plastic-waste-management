package com.waste.management.security;

import com.waste.management.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String fullName;
    private final String role;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;
    private final java.time.OffsetDateTime passwordChangedAt;

    public UserPrincipal(UUID id, String email, String password, String fullName, String role, boolean active, Collection<? extends GrantedAuthority> authorities) {
        this(id, email, password, fullName, role, active, authorities, null);
    }

    public UserPrincipal(UUID id, String email, String password, String fullName, String role, boolean active, Collection<? extends GrantedAuthority> authorities, java.time.OffsetDateTime passwordChangedAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.authorities = authorities;
        this.passwordChangedAt = passwordChangedAt;
    }

    public static UserPrincipal create(User user) {
        String rawRole = user.getRole() == null ? "" : user.getRole().trim().toUpperCase();
        String cleanRole = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + cleanRole),
                new SimpleGrantedAuthority(cleanRole)
        );
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                cleanRole,
                Boolean.TRUE.equals(user.getIsActive()),
                authorities,
                user.getPasswordChangedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public java.time.OffsetDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
