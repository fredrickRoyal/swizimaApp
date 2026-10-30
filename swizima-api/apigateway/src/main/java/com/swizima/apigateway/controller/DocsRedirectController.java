package com.swizima.apigateway.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Opening the gateway root ({@code http://localhost:8080/}) takes you straight to the
 * aggregated Swagger UI.
 */
@Controller
public class DocsRedirectController {

    @GetMapping({"/", "/docs", "/swagger"})
    public String swaggerUi() {
        return "redirect:/swagger-ui.html";
    }
}
