package vn.springboot.modules.user.service;

import vn.springboot.modules.user.dto.request.UserSearchRequest;
import vn.springboot.common.response.PageResponse;
import vn.springboot.modules.user.dto.response.UserResponse;

public interface UserService {

    PageResponse<UserResponse> search(UserSearchRequest request);

    UserResponse getById(Long id);
}
