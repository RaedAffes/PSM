package com.psm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/", "/login", "/register", "/dashboard", "/clients", "/projects", "/tasks", "/invoices", "/users"})
    public String forwardToFrontend() {
        return "forward:/index.html";
    }
}
