package io.github.oliviercailloux.minimax.experiment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apfloat.Apint;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.experiment.Run;
import io.github.oliviercailloux.experiment.Runs;
import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.regret.Regrets;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

class RunnerTests {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(RunnerTests.class);

	@Test
	void test() {
		final Voter v1 = new Voter(1);
		final Alternative a1 = new Alternative(1);
		final Alternative a2 = new Alternative(2);
		final Alternative a3 = new Alternative(3);
		final ImmutableList<Alternative> pref1 = ImmutableList.of(a1, a2, a3);
		final Oracle oracle = Oracle.build(ImmutableMap.of(v1, VoterStrictPreference.given(v1, pref1)),
				PSRWeights.given(ImmutableList.of(1d, 0.4d, 0d)));
		final Run run1 = Run.of(oracle, ImmutableList.of(10l, 11l),
				ImmutableList.of(Question.toVoter(v1, a1, a2), Question.toVoter(v1, a2, a3)), 13l);
		/**
		 * After first question, the regret is still 1d, with the adversary using
		 * weights (1d, 0d, 0d).
		 */
		assertEquals(ImmutableList.of(1d, 1d, 0d), run1.getMMRs().stream()
				.map(Regrets::getMinimalMaxRegretValue).collect(ImmutableList.toImmutableList()));

		final Runs runsSingleton = Runs.of(ImmutableList.of(run1));
		assertEquals(ImmutableList.of(1d, 1d, 0d), runsSingleton.getAverageMinimalMaxRegrets());

		final Run run2 = Run.of(oracle, ImmutableList.of(10l, 11l),
				ImmutableList.of(Question.toVoter(v1, a1, a2), Question.toCommittee(new Apint(1), 1)), 13l);
		assertEquals(ImmutableList.of(1d, 1d, 1d), run2.getMMRs().stream()
				.map(Regrets::getMinimalMaxRegretValue).collect(ImmutableList.toImmutableList()));

		final Runs runsTwo = Runs.of(ImmutableList.of(run1, run2));
		assertEquals(ImmutableList.of(1d, 1d, 0.5d), runsTwo.getAverageMinimalMaxRegrets());

		final Runs runsThree = Runs.of(ImmutableList.of(run1, run2, run1));
		assertEquals(ImmutableList.of(1d, 1d, 0.33333333333333337d), runsThree.getAverageMinimalMaxRegrets());
	}

}
