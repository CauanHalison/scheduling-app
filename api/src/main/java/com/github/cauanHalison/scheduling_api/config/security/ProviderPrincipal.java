package com.github.cauanHalison.scheduling_api.config.security;

import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record ProviderPrincipal(
        UUID id,
        String name,
        String email,
        String passwordHash,
        String slug,
        String timeZone
) implements UserDetails {

    public static ProviderPrincipal fromEntity(ProviderEntity provider) {
        return new ProviderPrincipal(
                provider.getId(),
                provider.getName(),
                provider.getEmail(),
                provider.getPasswordHash(),
                provider.getSlug(),
                provider.getTimeZone()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_PROVIDER"));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.id.toString();
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
        return true;
    }

    @Override
    public String toString() {
        return "ProviderPrincipal[id=" + id + ", name=" + name + ", email=" + email + ", slug=" + slug + ", timeZone=" + timeZone + "]";
    }
}
