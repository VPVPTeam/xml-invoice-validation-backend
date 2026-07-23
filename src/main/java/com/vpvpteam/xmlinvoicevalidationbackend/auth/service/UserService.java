package com.vpvpteam.xmlinvoicevalidationbackend.auth.service;

import com.vpvpteam.xmlinvoicevalidationbackend.auth.dto.CreateUserRequest;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.entity.UserEntity;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.mapper.UserMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.model.User;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.repository.UserRepository;
import com.vpvpteam.xmlinvoicevalidationbackend.exceptions.EntityAlreadyExistsException;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(CreateUserRequest request) {
        checkEmailNotTakenOrThrow(request.email());

        UserEntity user = new UserEntity();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setActive(true);

        UserEntity savedUser = userRepository.save(user);
        return userMapper.toModel(savedUser);
    }

    private void checkEmailNotTakenOrThrow(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EntityAlreadyExistsException("User already exists: " + email);
        }
    }
}