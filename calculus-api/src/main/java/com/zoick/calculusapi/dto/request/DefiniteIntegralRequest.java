package com.zoick.calculusapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
public class DefiniteIntegralRequest {
    @NotBlank(message = "Expression must not be blank")
    private String expression;

    @NotNull(message = "Lower bound must not be null")
    private Double lowerBound;

    @NotNull(message = "Upper bound must not be null")
    private Double upperBound;

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public Double getLowerBound() { return lowerBound; }
    public void setLowerBound(Double lowerBound) { this.lowerBound = lowerBound; }

    public Double getUpperBound() { return upperBound; }
    public void setUpperBound(Double upperBound) { this.upperBound = upperBound; }
}
