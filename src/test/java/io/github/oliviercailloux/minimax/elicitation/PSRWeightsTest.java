package io.github.oliviercailloux.minimax.elicitation;

import static org.junit.Assert.assertEquals;

import java.math.RoundingMode;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.Test;

import io.github.oliviercailloux.minimax.utils.Generator;
import io.github.oliviercailloux.minimax.utils.Rounder;

public class PSRWeightsTest {

	@Test
	public void testLambda() {
		Aprational a = new Aprational(new Apint(2), new Apint(3));
		QuestionCommittee qc = QuestionCommittee.given(a, 1);
		Answer answ;
		PSRWeights weights = Generator.genWeights(7, Rounder.given(RoundingMode.HALF_UP, 3));
		double left = (weights.getWeightAtRank(1) - weights.getWeightAtRank(2));
		double right = a.doubleValue() * (weights.getWeightAtRank(2) - weights.getWeightAtRank(3));
		if (left > right) {
			answ = Answer.GREATER;
		} else if (left == right) {
			answ = Answer.EQUAL;
		} else {
			answ = Answer.LOWER;
		}
		System.out.println(weights);
		System.out.println(a.numerator() + " / " + a.denominator() + " = " + a.doubleValue());
		System.out.println(left + " " + answ + " " + right);
		assertEquals(answ, weights.askQuestion(qc));

	}
}
