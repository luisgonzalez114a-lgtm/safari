package com.example.productos.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public RedirectView handleDataIntegrity(DataIntegrityViolationException ex,
                                           HttpServletRequest request,
                                           RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error",
                "No se pudo guardar la información porque faltan datos o relaciones válidas en la base de datos.");

        String target = request.getRequestURI().contains("/usuarios") ? "/usuarios" : "/";
        return new RedirectView(target, true);
    }
}
