package com.enrique.interview.application.ports.in;

import com.enrique.interview.domain.User;

import java.util.Optional;

public interface CreateUserUseCase {
    User createUser(User user);

}
