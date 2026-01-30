package com.enrique.interview.infraestructure.persistence.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "first-name")
    private String firstName;

    @Column(name = "last-name")
    private String lastName;


}
