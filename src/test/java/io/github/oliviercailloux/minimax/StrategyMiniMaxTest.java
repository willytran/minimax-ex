package io.github.oliviercailloux.minimax;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.Regret;
import io.github.oliviercailloux.minimax.StrategyMiniMax;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.utils.AggregationOperator.AggOps;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Generator;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyMiniMaxTest {

	@Test
	void testOneAlt() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(1), Generator.getVoters(1));
		final StrategyMiniMax s = StrategyMiniMax.build(k);
		assertThrows(IllegalArgumentException.class, () -> s.nextQuestion());
	}

	@Test
	void testTwoAltsOneVKnown() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(1));
		final StrategyMiniMax s = StrategyMiniMax.build(k);
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		assertThrows(IllegalArgumentException.class, () -> s.nextQuestion());
	}

	@Test
	void testTwoAltsOneV() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(1));
		final StrategyMiniMax s = StrategyMiniMax.build(k);
		final Question q1 = Question.toVoter(new Voter(1), new Alternative(1), new Alternative(2));
		final Question q2 = Question.toVoter(new Voter(1), new Alternative(2), new Alternative(1));
		final Set<Question> q = new HashSet<>();
		q.add(q1);
		q.add(q2);
		s.nextQuestion();
		assertEquals(q, StrategyMiniMax.getQuestions().keySet());
	}

	@Test
	void testTwoAltsTwoVsOneKnown() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(2));
		final StrategyMiniMax s = StrategyMiniMax.build(k);
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		s.nextQuestion();
		final Question q1 = Question.toVoter(new Voter(2), new Alternative(1), new Alternative(2));
		final Question q2 = Question.toVoter(new Voter(2), new Alternative(2), new Alternative(1));
		final Set<Question> q = new HashSet<>();
		q.add(q1);
		q.add(q2);
		assertEquals(q, StrategyMiniMax.getQuestions().keySet());
	}

	@Test
	void testTwoAltsTwoVs() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(2), Generator.getVoters(2));
		final StrategyMiniMax s = StrategyMiniMax.build(k);
		s.nextQuestion();
		final Question q1 = Question.toVoter(new Voter(1), new Alternative(1), new Alternative(2));
		final Question q2 = Question.toVoter(new Voter(1), new Alternative(2), new Alternative(1));
		final Question q3 = Question.toVoter(new Voter(2), new Alternative(1), new Alternative(2));
		final Question q4 = Question.toVoter(new Voter(2), new Alternative(2), new Alternative(1));
		final Set<Question> q = new HashSet<>();
		q.add(q1);
		q.add(q2);
		q.add(q3);
		q.add(q4);
		assertEquals(q, StrategyMiniMax.getQuestions().keySet());
		for (Double d : StrategyMiniMax.getQuestions().values()) {
			assertEquals(0d,d,0.0001);
		}
	}

	@Test
	void testThreeAltsTwoVs() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(2));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(2), new Alternative(3));
		k.getProfile().get(new Voter(2)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		k.getProfile().get(new Voter(1)).setGraphChanged();
		StrategyMiniMax s = StrategyMiniMax.build(k, AggOps.MAX);
		s.nextQuestion();
		final Question q1 = Question.toVoter(new Voter(2), new Alternative(3), new Alternative(2));
		final Question q2 = Question.toVoter(new Voter(2), new Alternative(2), new Alternative(3));
		final Question q3 = Question.toVoter(new Voter(2), new Alternative(1), new Alternative(3));
		final Question q4 = Question.toVoter(new Voter(2), new Alternative(3), new Alternative(1));
		final Aprational ap = new Aprational(new Apint(3), new Apint(2));
		final Question q5 = Question.toCommittee(ap, 1);
		final Set<Question> q = new HashSet<>();
		q.add(q1);
		q.add(q2);
		q.add(q3);
		q.add(q4);
		q.add(q5);
		assertEquals(q, StrategyMiniMax.getQuestions().keySet());
	}

	@Test
	void testThreeAltsTwoVsAndScore() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(3), Generator.getVoters(3));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		k.getProfile().get(new Voter(1)).asGraph().putEdge(new Alternative(2), new Alternative(3));
		k.getProfile().get(new Voter(2)).asGraph().putEdge(new Alternative(1), new Alternative(2));
		StrategyMiniMax s = StrategyMiniMax.build(k, AggOps.MAX);
		s.nextQuestion();

		for (Question qq : StrategyMiniMax.getQuestions().keySet()) {
			if (qq.getType().equals(QuestionType.VOTER_QUESTION)) {
				k.getProfile().get(qq.getQuestionVoter().getVoter()).asGraph().putEdge(
						qq.getQuestionVoter().getFirstAlternative(), qq.getQuestionVoter().getSecondAlternative());
				Regret.getMMRAlternatives(k);
				double yesRegret = Regret.getMMR();
				k.getProfile().get(qq.getQuestionVoter().getVoter()).asGraph().removeEdge(
						qq.getQuestionVoter().getFirstAlternative(), qq.getQuestionVoter().getSecondAlternative());

				k.getProfile().get(qq.getQuestionVoter().getVoter()).asGraph().putEdge(
						qq.getQuestionVoter().getSecondAlternative(), qq.getQuestionVoter().getFirstAlternative());
				Regret.getMMRAlternatives(k);
				double noRegret = Regret.getMMR();
				k.getProfile().get(qq.getQuestionVoter().getVoter()).asGraph().removeEdge(
						qq.getQuestionVoter().getSecondAlternative(), qq.getQuestionVoter().getFirstAlternative());
				assertEquals(Math.max(yesRegret, noRegret), s.getScore(qq), 0.001);
			}
		}
	}

	@Test
	void testFourAltsFourVs() {
		final PrefKnowledge k = PrefKnowledge.given(Generator.getAlternatives(4), Generator.getVoters(4));
		k.addConstraint(1, ComparisonOperator.EQ, new Apint(1));
		StrategyMiniMax s = StrategyMiniMax.build(k, AggOps.MAX);
		s.nextQuestion();
		for (Question qq : StrategyMiniMax.getQuestions().keySet()) {
			if (!qq.getType().equals(QuestionType.VOTER_QUESTION)) {
				System.out.println(qq);
			}
		}

	}

}
