package com.vpvpteam.xmlinvoicevalidationbackend.auth.mapper;

import com.vpvpteam.xmlinvoicevalidationbackend.auth.entity.UserEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.model.User;
import org.springframework.stereotype.Component;

@Component
public final class UserMapper {
    public User toModel(UserEntity entity) {
        User user = new User();

        user.setId(entity.getId());
        user.setEmail(entity.getEmail());
        user.setRole(entity.getRole());
        user.setActive(entity.isActive());
        user.setCreatedAt(entity.getCreatedAt());

        return user;
    }
}