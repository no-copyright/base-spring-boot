package vn.springboot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.springboot.common.response.ApiResponse;
import vn.springboot.dto.request.user.UserSearchRequest;
import vn.springboot.dto.response.PageResponse;
import vn.springboot.dto.response.user.UserResponse;
import vn.springboot.service.UserService;

/**
 * Example of a permission-protected resource. Access requires the
 * {@code USER_READ} authority (granted via a role) — see {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ApiResponse<PageResponse<UserResponse>> search(@ModelAttribute UserSearchRequest request) {
        return ApiResponse.success(userService.search(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }
}
