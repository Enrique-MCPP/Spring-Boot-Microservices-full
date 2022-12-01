package com.programmingtechie.productservice.model;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.*;

@Entity
@Table(name = "t_products")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends Base implements Serializable {


    private String name;


    private String descripcion;

    private BigDecimal price;

}
