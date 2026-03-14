package com.zoick.calculusapi.service;

import com.zoick.calculusapi.dto.response.CalcResponse;
import com.zoick.calculusapi.engine.Differential_and_Integral;
import com.zoick.calculusapi.exception.InvalidExpressionException;
import org.springframework.stereotype.Service;
@Service
public class CalculusService {
    private final Differential_and_Integral engine= new Differential_and_Integral();
    public CalcResponse differentiate(String expression){
        try{
            String result= engine.differentiate(expression);
            return CalcResponse.ok(expression, "DIFFERENTIATE", result);
        }catch (IllegalArgumentException ex){
            throw new InvalidExpressionException(expression, ex.getMessage());
        }catch (UnsupportedOperationException ex){
            throw new InvalidExpressionException(expression, ex.getMessage());
        }
    }
    public CalcResponse differentiateNth(String expression, int order){
        try{
            String result= engine.differentiate(expression, order);
            return CalcResponse.ok(expression, "DIFFERENTIATE_NTH", result);
        }catch (IllegalArgumentException ex){
            throw new InvalidExpressionException(expression, ex.getMessage());
        }catch (UnsupportedOperationException ex){
            throw new InvalidExpressionException(expression, ex.getMessage());
        }
    }
    public CalcResponse integrate(String expression) {
        try {
            String result = engine.integrate(expression);
            if (result.startsWith("Integral(")) {
                return CalcResponse.ok(expression, "INTEGRATE",
                        "Unsupported integral: " + result);
            }
            return CalcResponse.ok(expression, "INTEGRATE", result + " + C");
        } catch (IllegalArgumentException ex) {
            throw new InvalidExpressionException(expression, ex.getMessage());
        } catch (UnsupportedOperationException ex) {
            throw new InvalidExpressionException(expression, ex.getMessage());
        }
    }

    public CalcResponse integrateDefinite(String expression, double lowerBound, double upperBound) {
        try {
            String raw = engine.integrateDefinite(expression, lowerBound, upperBound);
            String result = formatDefiniteResult(raw);
            return CalcResponse.ok(expression, "INTEGRATE_DEFINITE", result);
        } catch (IllegalArgumentException ex) {
            throw new InvalidExpressionException(expression, ex.getMessage());
        } catch (UnsupportedOperationException ex) {
            throw new InvalidExpressionException(expression, ex.getMessage());
        }
    }

    public CalcResponse implicitDifferentiate(String equation) {
        try {
            String result = engine.implicitDifferentiate(equation);
            return CalcResponse.ok(equation, "IMPLICIT_DIFFERENTIATE", result);
        } catch (IllegalArgumentException ex) {
            throw new InvalidExpressionException(equation, ex.getMessage());
        } catch (UnsupportedOperationException ex) {
            throw new InvalidExpressionException(equation, ex.getMessage());
        }
    }
    // Cleans up raw double output from the engine e.g. "9.000000000000002" -> "9.0"
    private String formatDefiniteResult(String raw) {
        try {
            double value = Double.parseDouble(raw);
            if (Math.abs(value - Math.rint(value)) < 1e-9) {
                return String.valueOf((long) Math.rint(value));
            }
            // Round to 6 decimal places to avoid floating point noise
            return String.format("%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", ".0");
        } catch (NumberFormatException e) {
            return raw;
        }
    }
}
