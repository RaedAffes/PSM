package com.psm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    @NotBlank
    @Email
    private String email;

    private String password;

    @NotBlank
    private String tel;

    @NotBlank
    private String nom;

    @NotBlank
    private String prenom;

    @NotEmpty
    private Set<@NotBlank String> roles;
}
