package com.amrut.prabhu.oauth2.client;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@CrossOrigin(originPatterns = "http://192.168.128.1:8087")
public class Controller {
    @CrossOrigin(originPatterns = "http://192.168.128.1:8087")
    @GetMapping("/user")
    public String index(Principal principal) {
        return principal!=null ? principal.getName() : "Hola paco principal es null";
    }
}