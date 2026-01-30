// domain/exception/UserNotFoundException.java
package com.enrique.interview.domain.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("User with id " + id + " not found");
    }
}
