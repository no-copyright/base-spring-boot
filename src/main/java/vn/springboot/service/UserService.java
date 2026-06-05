package vn.springboot.service;

import vn.springboot.dto.request.user.UserSearchRequest;
import vn.springboot.dto.response.PageResponse;
import vn.springboot.dto.response.user.UserResponse;

public interface UserService {

    PageResponse<UserResponse> search(UserSearchRequest request);

    UserResponse getById(Long id);
}
