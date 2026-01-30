package com.enrique.interview.infraestructure.controller;

import com.enrique.interview.application.ports.in.CreateUserUseCase;
import com.enrique.interview.application.service.GetUserUseCase;
import com.enrique.interview.domain.User;
import com.enrique.interview.infraestructure.controller.dto.UserRequest;
import com.enrique.interview.infraestructure.controller.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final GetUserUseCase getUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase, GetUserUseCase getUserUseCase) {
        this.createUserUseCase = createUserUseCase;
        this.getUserUseCase = getUserUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody UserRequest userRequest) {
        User user = new User(null, userRequest.getFirstName(), userRequest.getLastName());
        User savedUser = createUserUseCase.createUser(user);
        return new UserResponse(savedUser.id(), savedUser.firstName(), savedUser.lastName());
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable Long id) {
        User user = getUserUseCase.getUserById(id);
        return new UserResponse(user.id(), user.firstName(), user.lastName());
    }
}
