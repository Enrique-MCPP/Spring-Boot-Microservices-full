package com.programmingtechie.productservice.model;

import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;


@Entity
@Table(name="t_clients")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client  extends Base implements Serializable {


    private String email;


    private String name;






}