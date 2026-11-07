package com.shop.user.web;

import com.shop.common.security.CurrentUser;
import com.shop.user.dto.UserResponse;
import com.shop.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @GetMapping("/me")
    public UserResponse me() { return service.getById(CurrentUser.id()); }

    // RBAC + ownership: admin may read anyone, a normal user only himself (authentication.name == JWT sub)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id.toString() == authentication.name")
    public UserResponse get(@PathVariable UUID id) { return service.getById(id); }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> list(@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable p) {
        return service.list(p);
    }

    @PostMapping("/admin/deactivate-dormant")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateDormant(@RequestParam(defaultValue = "90") int days) { service.deactivateDormant(days); }
}
