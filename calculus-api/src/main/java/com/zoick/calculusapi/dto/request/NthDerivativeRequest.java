package com.zoick.calculusapi.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
public class NthDerivativeRequest {
    @NotBlank(message = "Expression must not be blank")
    private String expression;

    @Min(value = 1, message = "Order must be at least 1")
    @Max(value = 10, message = "Order must not exceed 10")
    private int order;

    public String getExpression(){ return expression; }
    public void setExpression(String expression){ this.expression= expression; }

    public int getOrder(){ return order; }
    public void setOrder(int order){ this.order= order; }
}
