package com.vpvpteam.xmlinvoicevalidationbackend.auth.controller;

import com.vpvpteam.xmlinvoicevalidationbackend.auth.dto.LoginRequest;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.dto.LoginResponse;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.mapper.UserMapper;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.model.User;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.security.JwtService;
import com.vpvpteam.xmlinvoicevalidationbackend.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(
                principal.getUsername(),
                principal.getUser().getRole().name());

        return new LoginResponse(token);
    }

    @GetMapping("/me")
    public User getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return userMapper.toModel(principal.getUser());
    }
}