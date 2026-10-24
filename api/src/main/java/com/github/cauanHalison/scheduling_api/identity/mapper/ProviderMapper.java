package com.github.cauanHalison.scheduling_api.identity.mapper;

import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.dto.ProviderResponse;
import com.github.cauanHalison.scheduling_api.identity.dto.RegisterProviderRequest;
import org.springframework.stereotype.Component;

@Component
public class ProviderMapper {

    public ProviderResponse toResponse(ProviderPrincipal principal) {
        return new ProviderResponse(
                principal.id(),
                principal.name(),
                principal.email(),
                principal.slug(),
                principal.timeZone()
        );
    }

    public ProviderEntity toEntity(RegisterProviderRequest request, String passwordHash) {
        ProviderEntity providerEntity = new ProviderEntity();
        providerEntity.setName(request.name());
        providerEntity.setEmail(request.email());
        providerEntity.setPasswordHash(passwordHash);
        providerEntity.setSlug(request.slug());
        providerEntity.setTimeZone(request.timeZone());
        return providerEntity;
    }
}
