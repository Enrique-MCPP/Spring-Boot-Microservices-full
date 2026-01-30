package com.enrique.interview.application.service;

import com.enrique.interview.domain.User;

public interface GetUserUseCase {

    User getUserById(Long id);

}
