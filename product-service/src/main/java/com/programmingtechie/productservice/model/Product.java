package com.programmingtechie.productservice.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(value = "product")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Product {
	@Id
	private String id;

	@Field(name = "name")
	@Indexed(unique = true)
	private String name;

	@Field(name = "descripcion")
	private String descripcion;

	@Field(name = "price")
	private BigDecimal price;

}
