package com.shop.user.web;

import com.shop.user.dto.*;
import com.shop.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService service;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
}
