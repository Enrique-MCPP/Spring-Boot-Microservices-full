package com.enrique.interview.application.ports.out;

import com.enrique.interview.domain.User;

import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long id);
}
