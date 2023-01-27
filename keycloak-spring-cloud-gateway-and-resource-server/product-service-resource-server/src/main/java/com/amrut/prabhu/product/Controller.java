package com.amrut.prabhu.product;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;
import java.security.Principal;

@RestController
@CrossOrigin(origins = "*")
public class Controller {
    @CrossOrigin(origins = "*")
    @GetMapping("/product")
    @PreAuthorize("hasRole('ROLE_NORMAL_USER')")
    public String getProduct(Principal principal) {
    return principal!=null ?"Response from Product Service, User Id:" + principal.getName():"Aisss paco";
    }
}
