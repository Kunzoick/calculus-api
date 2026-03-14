package com.zoick.calculusapi.dto.request;

import jakarta.validation.constraints.NotBlank;
public class ImplicitDiffRequest {
    @NotBlank(message = "Equation must not be blank")
    private String equation;

    public String getEquation() { return equation; }
    public void setEquation(String equation) { this.equation = equation; }
}
