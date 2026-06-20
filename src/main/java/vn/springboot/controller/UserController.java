package vn.springboot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.springboot.common.response.ApiResponse;
import vn.springboot.dto.request.user.AssignRolesRequest;
import vn.springboot.dto.request.user.UpdateProfileRequest;
import vn.springboot.dto.request.user.UserSearchRequest;
import vn.springboot.dto.response.PageResponse;
import vn.springboot.dto.response.user.UserResponse;
import vn.springboot.service.UserService;

/**
 * User resource. Three groups:
 *   - Admin (permission-protected): list/detail users, assign roles.
 *   - Self-service ({@code /me}): every authenticated user — including admins —
 *     reads/updates their own profile and avatar here.
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

    // --- Current user (self-service) --------------------------------------

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile() {
        return ApiResponse.success(userService.getMyProfile());
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success("Profile updated", userService.updateMyProfile(request));
    }

    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateMyAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("Avatar updated", userService.updateMyAvatar(file));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    /** Admin: set the roles of a user. */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_WRITE')")
    public ApiResponse<UserResponse> assignRoles(@PathVariable Long id,
                                                 @Valid @RequestBody AssignRolesRequest request) {
        return ApiResponse.success("Roles updated", userService.assignRoles(id, request));
    }
}
