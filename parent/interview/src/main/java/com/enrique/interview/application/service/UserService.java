package com.enrique.interview.application.service;

import com.enrique.interview.application.ports.in.CreateUserUseCase;

import com.enrique.interview.application.ports.out.UserRepositoryPort;
import com.enrique.interview.domain.User;
import com.enrique.interview.domain.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService implements CreateUserUseCase, GetUserUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public UserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public User createUser(User user) {
        return userRepositoryPort.save(user);
    }

    @Override
    public User getUserById(Long id) {
        return userRepositoryPort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}
