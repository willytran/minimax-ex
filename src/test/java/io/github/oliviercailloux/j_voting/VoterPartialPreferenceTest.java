package io.github.oliviercailloux.j_voting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.j_voting.VoterPartialPreference;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

class VoterPartialPreferenceTest {

	@Test
	void test() {
		final VoterPartialPreference p = VoterPartialPreference.about(new Voter(1),
				ImmutableSet.of(new Alternative(1), new Alternative(2), new Alternative(3)));
		testIt(p);
	}

	private void testIt(final VoterPartialPreference p) {
		assertEquals(0, p.asGraph().edges().size());
		assertEquals(0, p.asTransitiveGraph().edges().size());
		p.asGraph().putEdge(new Alternative(1), new Alternative(2));
		assertEquals(1, p.asGraph().edges().size());
		assertEquals(1, p.asTransitiveGraph().edges().size());
		p.asGraph().putEdge(new Alternative(2), new Alternative(3));
		assertEquals(2, p.asGraph().edges().size());
		assertEquals(3, p.asTransitiveGraph().edges().size());
	}

	@Test
	void testCopy() {
		final VoterPartialPreference p = VoterPartialPreference.about(new Voter(1),
				ImmutableSet.of(new Alternative(1), new Alternative(2), new Alternative(3)));
		final VoterPartialPreference p2 = VoterPartialPreference.copyOf(p);
		testIt(p2);
	}

	@Test
	void testAddRemove() {
		final Alternative a2 = new Alternative(2);
		final Alternative a3 = new Alternative(3);
		final Alternative a1 = new Alternative(1);
		final VoterPartialPreference p = VoterPartialPreference.about(new Voter(1), ImmutableSet.of(a1, a2, a3));
		p.asGraph().putEdge(a1, a2);
		p.asGraph().putEdge(a2, a3);
		assertEquals(2, p.asGraph().edges().size());
		assertEquals(3, p.asTransitiveGraph().edges().size());
		p.asGraph().removeEdge(a2, a3);
		assertEquals(1, p.asGraph().edges().size());
		assertEquals(1, p.asTransitiveGraph().edges().size());
	}
}
