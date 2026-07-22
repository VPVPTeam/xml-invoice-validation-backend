package com.vpvpteam.xmlinvoicevalidationbackend.auth.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.auth.dto.CreateUserRequest;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.model.User;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public User createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.createUser(request);
    }
}