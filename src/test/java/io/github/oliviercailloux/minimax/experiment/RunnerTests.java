package io.github.oliviercailloux.minimax.experiment;

import static io.github.oliviercailloux.minimax.Basics.a1;
import static io.github.oliviercailloux.minimax.Basics.a2;
import static io.github.oliviercailloux.minimax.Basics.a3;
import static io.github.oliviercailloux.minimax.Basics.factory;
import static io.github.oliviercailloux.minimax.Basics.p1;
import static io.github.oliviercailloux.minimax.Basics.v1;
import static io.github.oliviercailloux.minimax.Basics.w;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;

import org.apfloat.Apint;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.regret.Regrets;

class RunnerTests {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(RunnerTests.class);

    @Test
    void test() {
	final Oracle oracle = Oracle.build(ImmutableMap.of(v1, VoterStrictPreference.given(v1, p1)), w);
	final Run run1 = Run.of(oracle, ImmutableList.of(10l, 11l),
		ImmutableList.of(Question.toVoter(v1, a1, a2), Question.toVoter(v1, a2, a3)), 13l);
	/**
	 * After first question, the regret is still 1d, with the adversary using
	 * weights (1d, 0d, 0d).
	 */

	assertEquals(ImmutableList.of(1d, 1d, 0d), run1.getMinimalMaxRegrets().stream()
		.map(Regrets::getMinimalMaxRegretValue).collect(ImmutableList.toImmutableList()));

	final Runs runsSingleton = Runs.of(factory, ImmutableList.of(run1));
	assertEquals(ImmutableList.of(1d, 1d, 0d), runsSingleton.getAverageMinimalMaxRegrets());

	final Run run2 = Run.of(oracle, ImmutableList.of(10l, 11l),
		ImmutableList.of(Question.toVoter(v1, a1, a2), Question.toCommittee(new Apint(1), 1)), 13l);
	assertEquals(ImmutableList.of(1d, 1d, 1d), run2.getMinimalMaxRegrets().stream()
		.map(Regrets::getMinimalMaxRegretValue).collect(ImmutableList.toImmutableList()));

	final Runs runsTwo = Runs.of(factory, ImmutableList.of(run1, run2));
	assertEquals(ImmutableList.of(1d, 1d, 0.5d), runsTwo.getAverageMinimalMaxRegrets());

	final Runs runsThree = Runs.of(factory, ImmutableList.of(run1, run2, run1));
	final ImmutableList<Double> avg = runsThree.getAverageMinimalMaxRegrets();
	assertEquals(3, avg.size());
	final Iterator<Double> iterator = avg.iterator();
	assertEquals(1d, iterator.next().doubleValue());
	assertEquals(1d, iterator.next().doubleValue());
	assertEquals(0.3333d, iterator.next().doubleValue(), 0.0001d);
    }

}
