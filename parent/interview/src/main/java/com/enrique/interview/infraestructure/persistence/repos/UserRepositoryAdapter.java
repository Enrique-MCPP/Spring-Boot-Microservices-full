package com.enrique.interview.infraestructure.persistence.repos;

import com.enrique.interview.application.ports.out.UserRepositoryPort;
import com.enrique.interview.domain.User;
import com.enrique.interview.infraestructure.persistence.entities.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringJpaUserRepository springJpaUserRepository;

    public UserRepositoryAdapter(SpringJpaUserRepository springJpaUserRepository) {
        this.springJpaUserRepository = springJpaUserRepository;
    }

    @Override
    public User save(User user) {
        UserEntity userEntity = new UserEntity(user.id(), user.firstName(), user.lastName());
        UserEntity savedUser = springJpaUserRepository.save(userEntity);
        return new User(savedUser.getId(), savedUser.getFirstName(), savedUser.getLastName());
    }

    @Override
    public Optional<User> findById(Long id) {
        return springJpaUserRepository.findById(id)
                .map(entity -> new User(
                        entity.getId(),
                        entity.getFirstName(),
                        entity.getLastName()
                ));
    }
}
