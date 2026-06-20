package vn.springboot.service;

import org.springframework.web.multipart.MultipartFile;
import vn.springboot.dto.request.user.AssignRolesRequest;
import vn.springboot.dto.request.user.UpdateProfileRequest;
import vn.springboot.dto.request.user.UserSearchRequest;
import vn.springboot.dto.response.PageResponse;
import vn.springboot.dto.response.user.UserResponse;

public interface UserService {

    PageResponse<UserResponse> search(UserSearchRequest request);

    UserResponse getById(Long id);

    /** Admin: replace a user's roles with the given role ids. */
    UserResponse assignRoles(Long userId, AssignRolesRequest request);

    // --- Current user (self-service) --------------------------------------

    UserResponse getMyProfile();

    UserResponse updateMyProfile(UpdateProfileRequest request);

    /** Upload + set the current user's avatar, replacing any previous one. */
    UserResponse updateMyAvatar(MultipartFile file);
}
