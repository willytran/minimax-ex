package io.github.oliviercailloux.minimax.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.oliviercailloux.j_voting.Alternative;
import io.github.oliviercailloux.j_voting.Voter;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;

public class GeneratorTest {

	@Test
	public void testGenWeights() {
		int m = 9;
		final PSRWeights weights = Generator.genWeightsWithUnbalancedDistribution(m);
		assertEquals(m, weights.getWeights().size());
	}

	@Test
	public void testGenProfile() {
		int m = 3;
		int n = 2;
		final Map<Voter, VoterStrictPreference> rv = Generator.genProfile(m, n);

		final List<Alternative> alt = new LinkedList<>();
		for (int i = 1; i <= m; i++) {
			alt.add(Alternative.withId(i));
		}

		assertEquals(n, rv.size());
		for (VoterStrictPreference vpref : rv.values()) {
			assertEquals(m, vpref.asStrictPreference().getAlternatives().size());
			assertTrue(vpref.asStrictPreference().getAlternatives().containsAll(alt));
		}

	}

}
