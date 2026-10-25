package com.github.cauanHalison.scheduling_api.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterProviderRequest(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String name,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Formato de email inválido")
        @Size(max = 255, message = "O email deve ter no máximo 255 caracteres")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
        String password,

        @NotBlank(message = "O slug é obrigatório")
        @Size(min = 3, max = 50, message = "O slug deve ter entre 3 e 50 caracteres")
        @Pattern(
                regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "O slug deve conter apenas letras minúsculas, números e hífens (sem espaços, acentos ou hífens nas pontas)"
        )
        String slug,

        @NotBlank(message = "O fuso horário é obrigatório")
        @Size(max = 50, message = "O fuso horário deve ter no máximo 50 caracteres")
        String timeZone
) {
}
