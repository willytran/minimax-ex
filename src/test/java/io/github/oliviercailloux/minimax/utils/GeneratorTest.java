package io.github.oliviercailloux.minimax.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class GeneratorTest {

	@Test
	public void testGenWeights() {
		int m = 9;
		final PSRWeights weights = Generator.genWeights(m);
		assertEquals(m, weights.getWeights().size());
	}

	@Test
	public void testGenProfile() {
		int m = 3;
		int n = 2;
		final Map<Voter, VoterStrictPreference> rv = Generator.genProfile(n, m);

		final List<Alternative> alt = new LinkedList<>();
		for (int i = 1; i <= m; i++) {
			alt.add(new Alternative(i));
		}

		assertEquals(n, rv.size());
		for (VoterStrictPreference vpref : rv.values()) {
			assertEquals(m, vpref.asStrictPreference().getAlternatives().size());
			assertTrue(vpref.asStrictPreference().getAlternatives().containsAll(alt));
		}

	}

}
