package com.github.cauanHalison.scheduling_api.config.security;

import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final ProviderRepository providerRepository;

    public CustomUserDetailsService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return providerRepository.findByEmail(email)
                .map(ProviderPrincipal::fromEntity)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public UserDetails loadUserById(UUID id) throws UsernameNotFoundException {
        return providerRepository.findById(id)
                .map(ProviderPrincipal::fromEntity)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }
}
