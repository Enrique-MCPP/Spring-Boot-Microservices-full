package com.programmingtechie.productservice.controller;

import com.programmingtechie.productservice.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin("*")
@RestController
@RequestMapping("api/v1/ecom/clients")
public class ClientController {
    @Autowired
    private ClientService clientService;


    @GetMapping("/")
    @PreAuthorize("hasAnyRole('ROLE_admin')")
    @Transactional
    public ResponseEntity<?> getAll() throws Exception {
        try {
            return ResponseEntity.status(HttpStatus.OK).body(clientService.findAll());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("{\"Mi mensaje get todos\": \"" + e.getMessage() + "\"}");
        }
    }

}