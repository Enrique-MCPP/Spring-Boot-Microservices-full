package com.enrique.interview.domain;

import java.util.Optional;

public record User(
        Long id,
        String firstName,
        String lastName
) {

}
