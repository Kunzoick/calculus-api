package com.zoick.calculusapi.exception;

public class InvalidExpressionException  extends RuntimeException{
    private final String expression;
    public InvalidExpressionException(String expression, String message){
        super(message);
        this.expression= expression;
    }
    public String getExpression(){ return expression; }
}
