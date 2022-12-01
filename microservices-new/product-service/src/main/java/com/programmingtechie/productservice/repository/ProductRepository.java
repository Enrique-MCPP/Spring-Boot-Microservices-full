package com.programmingtechie.productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.programmingtechie.productservice.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

}
