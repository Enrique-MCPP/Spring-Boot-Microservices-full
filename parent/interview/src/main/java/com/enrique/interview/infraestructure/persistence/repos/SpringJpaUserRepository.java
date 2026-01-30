package com.enrique.interview.infraestructure.persistence.repos;

import com.enrique.interview.infraestructure.persistence.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringJpaUserRepository extends JpaRepository<UserEntity, Long> {
}
