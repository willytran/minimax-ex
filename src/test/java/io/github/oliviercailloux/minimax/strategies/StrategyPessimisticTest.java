package io.github.oliviercailloux.minimax.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Generator;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyPessimisticTest {

	@Test
	void testOneAlt() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(1), Generator.getVoters(1));
		final StrategyByMmr s = StrategyByMmr.build();
		assertThrows(IllegalArgumentException.class, () -> s.setKnowledge(k));
	}

	@Test
	void testTwoAltsOneVKnown() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(1));
		final StrategyByMmr s = StrategyByMmr.build();
		s.setKnowledge(k);
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		assertThrows(IllegalStateException.class, () -> s.nextQuestion());
	}

	@Test
	void testTwoAltsOneV() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(1));
		final StrategyByMmr s = StrategyByMmr.build();
		s.setKnowledge(k);
		final Question q1 = Question.toVoter(new Voter(1), new Alternative(1), new Alternative(2));
		final Question q2 = Question.toVoter(new Voter(1), new Alternative(2), new Alternative(1));
		s.nextQuestion();
		assertEquals(ImmutableSet.of(q1, q2), s.getQuestions().keySet());
	}

	@Test
	void testTwoAltsTwoVsOneKnown() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(2));
		final StrategyByMmr s = StrategyByMmr.build();
		s.setKnowledge(k);
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		s.nextQuestion();
		final Question q1 = Question.toVoter(new Voter(2), new Alternative(1), new Alternative(2));
		final Question q2 = Question.toVoter(new Voter(2), new Alternative(2), new Alternative(1));
		assertEquals(ImmutableSet.of(q1, q2), s.getQuestions().keySet());
	}

	@Test
	void testTwoAltsTwoVs() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(2));
		final StrategyByMmr s = StrategyByMmr.build();
		s.setKnowledge(k);
		s.nextQuestion();
		final Question q1 = Question.toVoter(new Voter(1), new Alternative(1), new Alternative(2));
		final Question q2 = Question.toVoter(new Voter(2), new Alternative(1), new Alternative(2));
		final Set<Question> expected = new HashSet<>();
		expected.add(q1);
		expected.add(q2);
		assertEquals(ImmutableSet.of(q1, q2), s.getQuestions().keySet());
		for (Question q : s.getQuestions().keySet()) {
			assertEquals(MmrLottery.given(0d, 0d), s.getQuestions().get(q));
		}
	}

	@Test
	void testThreeAltsTwoVs() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(2));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(2), new Alternative(3));
		k.getProfile().get(new Voter(2)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		k.getProfile().get(new Voter(1)).setGraphChanged();
		StrategyByMmr s = StrategyByMmr.build();
		s.setKnowledge(k);
		s.nextQuestion();
		final Question q1 = Question.toVoter(new Voter(2), new Alternative(3), new Alternative(2));
		final Question q2 = Question.toVoter(new Voter(2), new Alternative(2), new Alternative(3));
		final Question q3 = Question.toVoter(new Voter(2), new Alternative(1), new Alternative(3));
		final Question q4 = Question.toVoter(new Voter(2), new Alternative(3), new Alternative(1));
		final Aprational ap = new Aprational(new Apint(3), new Apint(2));
		final Question q5 = Question.toCommittee(ap, 1);
		assertEquals(ImmutableSet.of(q1, q2, q3, q4, q5), s.getQuestions().keySet());
	}

	@Test
	void testThreeAltsTwoVsAndScore() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(3));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(2), new Alternative(3));
		k.getProfile().get(new Voter(2)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		StrategyByMmr s = StrategyByMmr.build();
		s.setKnowledge(k);
		s.nextQuestion();

		for (Question qq : s.getQuestions().keySet()) {
			if (qq.getType().equals(QuestionType.VOTER_QUESTION)) {
				k.getProfile().get(qq.asQuestionVoter().getVoter()).asGraph().putEdge(
						qq.asQuestionVoter().getFirstAlternative(), qq.asQuestionVoter().getSecondAlternative());
				double yesRegret = new RegretComputer(k).getMinimalMaxRegrets().getMinimalMaxRegretValue();
				k.getProfile().get(qq.asQuestionVoter().getVoter()).asGraph().removeEdge(
						qq.asQuestionVoter().getFirstAlternative(), qq.asQuestionVoter().getSecondAlternative());

				k.getProfile().get(qq.asQuestionVoter().getVoter()).asGraph().putEdge(
						qq.asQuestionVoter().getSecondAlternative(), qq.asQuestionVoter().getFirstAlternative());
				double noRegret = new RegretComputer(k).getMinimalMaxRegrets().getMinimalMaxRegretValue();
				k.getProfile().get(qq.asQuestionVoter().getVoter()).asGraph().removeEdge(
						qq.asQuestionVoter().getSecondAlternative(), qq.asQuestionVoter().getFirstAlternative());
				assertEquals(Math.max(yesRegret, noRegret), s.getScore(qq), 0.001);
			}
		}
	}

}
