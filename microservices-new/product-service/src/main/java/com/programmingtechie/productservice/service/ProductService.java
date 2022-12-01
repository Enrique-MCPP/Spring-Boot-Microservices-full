package com.programmingtechie.productservice.service;

import com.programmingtechie.productservice.model.Product;
import com.programmingtechie.productservice.repository.ProductRepository;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProductService extends ServicioGenerico<Product, ProductRepository> {
   @Override
    public Product update(long id, Product product) throws Exception {

        try {
            Optional<Product> entityOptional = this.repository.findById(id);
            product.setId(entityOptional.get().getId());
            product = this.repository.save(product);

            return product;

        } catch (Exception e) {

            throw new Exception(e.getMessage());

        }

    }
}