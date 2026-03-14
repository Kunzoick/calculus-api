package com.zoick.calculusapi.controller;

import com.zoick.calculusapi.dto.request.DefiniteIntegralRequest;
import com.zoick.calculusapi.dto.request.ExpressionRequest;
import com.zoick.calculusapi.dto.request.ImplicitDiffRequest;
import com.zoick.calculusapi.dto.request.NthDerivativeRequest;
import com.zoick.calculusapi.dto.response.CalcResponse;
import com.zoick.calculusapi.service.CalculusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calc")
public class CalculusController {
    private final CalculusService service;

    public CalculusController(CalculusService service){
        this.service= service;
    }
    @PostMapping("/differentiate")
    public ResponseEntity<CalcResponse> differentiate(@Valid @RequestBody ExpressionRequest request){
       return ResponseEntity.ok(service.differentiate(request.getExpression()));
    }
    @PostMapping("/differentiate/nth")
    public ResponseEntity<CalcResponse> differentiateNth(@Valid @RequestBody NthDerivativeRequest request) {
        return ResponseEntity.ok(service.differentiateNth(request.getExpression(), request.getOrder()));
    }
    @PostMapping("/integrate")
    public ResponseEntity<CalcResponse> integrate(@Valid @RequestBody ExpressionRequest request) {
        return ResponseEntity.ok(service.integrate(request.getExpression()));
    }
    @PostMapping("/integrate/definite")
    public ResponseEntity<CalcResponse> integrateDefinite(@Valid @RequestBody DefiniteIntegralRequest request) {
        return ResponseEntity.ok(service.integrateDefinite(
                request.getExpression(),
                request.getLowerBound(),
                request.getUpperBound()));
    }
    @PostMapping("/differentiate/implicit")
    public ResponseEntity<CalcResponse> implicitDifferentiate(@Valid @RequestBody ImplicitDiffRequest request) {
        return ResponseEntity.ok(service.implicitDifferentiate(request.getEquation()));
    }
}

