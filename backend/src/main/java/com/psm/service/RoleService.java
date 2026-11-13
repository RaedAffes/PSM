package com.psm.service;

import com.psm.dto.RoleRequest;
import com.psm.entity.Role;
import com.psm.repository.RoleRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final String ADMIN = "ADMIN";
    private static final String COLLABORATEUR = "COLLABORATEUR";

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll(Sort.by("libelle"));
    }

    @Transactional(readOnly = true)
    public Role findById(Long id) {
        return roleRepository.findById(id).orElseThrow(() -> new RuntimeException("Role not found"));
    }

    @Transactional
    public Role create(RoleRequest request) {
        String name = normalize(request.getLibelle());
        if (!isBuiltIn(name)) {
            throw new IllegalArgumentException("Only ADMIN and COLLABORATEUR roles are supported");
        }
        if (roleRepository.existsByLibelleIgnoreCase(name)) {
            throw new RuntimeException("Role already exists");
        }
        return roleRepository.save(new Role(name));
    }

    @Transactional
    public Role update(Long id, RoleRequest request) {
        Role role = findById(id);
        String name = normalize(request.getLibelle());
        if (!isBuiltIn(role.getLibelle()) || !isBuiltIn(name)) {
            throw new IllegalArgumentException("Only ADMIN and COLLABORATEUR roles are supported");
        }
        if (!role.getLibelle().equals(name)) {
            throw new IllegalArgumentException("Built-in roles cannot be renamed");
        }
        if (!role.getLibelle().equals(name) && roleRepository.existsByLibelleIgnoreCase(name)) {
            throw new RuntimeException("Role already exists");
        }
        role.setLibelle(name);
        return roleRepository.save(role);
    }

    @Transactional
    public void delete(Long id) {
        Role role = findById(id);
        if (isBuiltIn(role.getLibelle())) {
            throw new IllegalArgumentException("Built-in roles cannot be deleted");
        }
        throw new IllegalArgumentException("Only ADMIN and COLLABORATEUR roles are supported");
    }

    private String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBuiltIn(String name) {
        return ADMIN.equalsIgnoreCase(name) || COLLABORATEUR.equalsIgnoreCase(name);
    }
}
