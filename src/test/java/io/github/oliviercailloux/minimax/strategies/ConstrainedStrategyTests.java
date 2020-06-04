package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Verify.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.y2018.j_voting.Generator;

class ConstrainedStrategyTests {

	@Test
	void test() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(1));
		final StrategyFactory factory = StrategyFactory.limitedCommitteeThenVoters(10, 5);
		final Strategy twoPhase = factory.get();
		assertEquals("Limited, constrained to [10c, 5v]", factory.getDescription());
		twoPhase.setKnowledge(k);
		for (int i = 0; i < 10; ++i) {
			verify(twoPhase.nextQuestion().getType().equals(QuestionType.COMMITTEE_QUESTION));
		}
		for (int i = 0; i < 5; ++i) {
			verify(twoPhase.nextQuestion().getType().equals(QuestionType.VOTER_QUESTION));
		}
	}

}
