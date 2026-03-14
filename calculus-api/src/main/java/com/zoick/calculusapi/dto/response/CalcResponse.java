package com.zoick.calculusapi.dto.response;

public class CalcResponse {
    private boolean success;
    private String result;
    private String error;
    private String expression;
    private String operation;

    private CalcResponse() {}
    public static CalcResponse ok(String expression, String operation, String result){
        CalcResponse r= new CalcResponse();
        r.success= true;
        r.result= result;
        r.error= null;
        r.expression= expression;
        r.operation= operation;
        return r;
    }
    public static CalcResponse fail(String expression, String operation, String error){
        CalcResponse r= new CalcResponse();
        r.success= false;
        r.result= null;
        r.error= error;
        r.expression= expression;
        r.operation= operation;
        return r;
    }
    public boolean isSuccess(){ return success; }
    public String getResult(){ return  result; }
    public String getError(){ return error; }
    public String getExpression(){ return expression; }
    public String getOperation(){ return  operation; }
}
