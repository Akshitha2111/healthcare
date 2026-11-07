package com.healthcare.security;

import com.healthcare.entity.Role;

/** The authenticated principal placed in the SecurityContext (never contains the password hash). */
public record AuthUser(Long id, String email, Role role) {}
