package com.zoick.calculusapi;

import com.zoick.calculusapi.engine.Differential_and_Integral;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


class CalculusApiApplicationTests {

	private final Differential_and_Integral engine= new Differential_and_Integral();

	@Test
	void differentiateXsquared() {
		String result= engine.differentiate("x^2");
		assertEquals("2x", result);
	}

}
