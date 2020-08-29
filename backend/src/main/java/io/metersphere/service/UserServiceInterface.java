package io.metersphere.service;

import io.metersphere.controller.request.member.UserRequest;
import io.metersphere.dto.UserDTO;
import org.springframework.stereotype.Service;

@Service
public interface UserServiceInterface {

    UserDTO insert(UserRequest user);

}
