package com.zoick.calculusapi.dto.request;

import jakarta.validation.constraints.NotBlank;
public class ExpressionRequest {
    @NotBlank(message = "Expression must not be blank")
    private String expression;
    public String getExpression(){ return expression; }
    public void setExpression(String expression){ this.expression= expression; }
}
