package com.zoick.calculusapi;

import com.zoick.calculusapi.engine.Differential_and_Integral;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CalculusApiApplicationTests {

	private final Differential_and_Integral engine= new Differential_and_Integral();

	@Test
	void differentiateXSquared() {
		assertEquals("2x", engine.differentiate("x^2"));
	}
	@Test
	void differentiateSin() {
		assertEquals("cos(x)", engine.differentiate("sin(x)"));
	}
	@Test
	void differentiateCos() {
		assertEquals("-sin(x)", engine.differentiate("cos(x)"));
	}
	@Test
	void differentiateLn() {
		assertEquals("1/x", engine.differentiate("ln(x)"));
	}
	@Test
	void differentiateXCubed() {
		assertEquals("3(x^2)", engine.differentiate("x^3"));
	}
	@Test
	void differentiateNthOrder() {
		assertEquals("2", engine.differentiate("x^2", 2));
	}
	@Test
	void integrateSin(){
		String result= engine.integrate("sin(x)");
		assertTrue(result.contains("cos"), "Integral of x^2 should contain x^3, got: "+ result);
	}
	@Test
	void integrateXSquared(){
		String result= engine.integrate("x^2");
		assertTrue(result.contains("x^3"), "Integral of x^2 should contain x^3, got: "+ result);
	}
	@Test
	void differentiateArcsin() {
		assertEquals("1/sqrt(1 - x^2)", engine.differentiate("arcsin(x)"));
	}

	@Test
	void differentiateArccos() {
		assertEquals("-(1/sqrt(1 - x^2))", engine.differentiate("arccos(x)"));
	}

	@Test
	void differentiateArctan() {
		assertEquals("1/(1 + x^2)", engine.differentiate("arctan(x)"));
	}
	@Test
	void differentiate2ToTheX() {
		String result = engine.differentiate("2^x");
		System.out.println("2^x derivative: " + result);
		assertTrue(result.contains("2^(x)") && result.contains("ln"),
				"Derivative of 2^x should contain 2^(x) and ln, got: " + result);
	}

	@Test
	void differentiateXToTheX() {
		String result = engine.differentiate("x^x");
		System.out.println("x^x derivative: " + result);
		assertTrue(result.contains("x^(x)") && result.contains("ln"),
				"Derivative of x^x should contain x^(x) and ln, got: " + result);
	}

	@Test
	void differentiatePowerRuleStillWorks() {
		assertEquals("3(x^2)", engine.differentiate("x^3"));
	}

	@Test
	void differentiateXSquaredStillWorks() {
		assertEquals("2x", engine.differentiate("x^2"));
	}
	@Test
	void integrateXSinX() {
		String result = engine.integrate("x*sin(x)");
		System.out.println("x*sin(x) integral: " + result);
		assertTrue(result.contains("sin") && result.contains("cos"),
				"Integral of x*sin(x) should contain sin and cos, got: " + result);
	}

	@Test
	void integrateXCosX() {
		String result = engine.integrate("x*cos(x)");
		System.out.println("x*cos(x) integral: " + result);
		assertTrue(result.contains("sin") && result.contains("cos"),
				"Integral of x*cos(x) should contain sin and cos, got: " + result);
	}

	@Test
	void integrateXLnX() {
		String result = engine.integrate("x*ln(x)");
		System.out.println("x*ln(x) integral: " + result);
		assertTrue(result.contains("ln") && result.contains("x^2"),
				"Integral of x*ln(x) should contain ln and x^2, got: " + result);
	}

	@Test
	void integrateX2SinX() {
		String result = engine.integrate("x^2*sin(x)");
		System.out.println("x^2*sin(x) integral: " + result);
		assertTrue(result.contains("sin") && result.contains("cos"),
				"Integral of x^2*sin(x) should contain sin and cos, got: " + result);
	}

	// Regression — existing integration by parts still works
	@Test
	void integrateXExpX() {
		String result = engine.integrate("x*exp(x)");
		System.out.println("x*exp(x) integral: " + result);
		assertTrue(result.contains("exp"),
				"Integral of x*exp(x) should contain exp, got: " + result);
	}

}
