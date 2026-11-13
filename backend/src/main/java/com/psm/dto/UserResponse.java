package com.psm.dto;

import java.util.Set;

public record UserResponse(Long id, String email, String tel, String nom, String prenom, Set<String> roles) {}
