package com.programmingtechie.productservice.controller;

import com.programmingtechie.productservice.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.transaction.Transactional;
import java.util.List;


@CrossOrigin("*")
@Controller
@RequestMapping("api/v1/ecom/suppliers")
public class SupplierController {

@Autowired
SupplierService supplierService;

@GetMapping("/")
@PreAuthorize("hasAnyRole('ROLE_admin')")
@Transactional
    public ResponseEntity<?> getAll() throws Exception {
        try{
            List<Object> supplierList=supplierService.consumeSuppliers();
            return ResponseEntity.status(HttpStatus.OK).body(supplierList);
 
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body("{\"Mi mensaje get todos\": \"" + e.getMessage() + "\"}");        }
            }

}

