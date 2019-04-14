package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.minimax.utils.AggregationOperator;
import io.github.oliviercailloux.minimax.utils.Rounder;
import io.github.oliviercailloux.minimax.utils.AggregationOperator.AggOps;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/** Uses the Regret to get the next question. **/

public class StrategyMiniMax implements Strategy {

	private PrefKnowledge knowledge;
	public boolean profileCompleted;
	private static AggOps op;
	private static double w1;
	private static double w2;
	private static HashMap<Question, Double> questions;
	private static List<Question> nextQuestions;
	private static RegretComputer regretComputer;
	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyMiniMax.class);

	public static StrategyMiniMax build(PrefKnowledge knowledge) {
		op = AggOps.MAX;
		return new StrategyMiniMax(knowledge);
	}

	public static StrategyMiniMax build(PrefKnowledge knowledge, AggOps operator) {
		checkArgument(!operator.equals(AggOps.WEIGHTED_AVERAGE));
		op = operator;
		return new StrategyMiniMax(knowledge);
	}

	public static StrategyMiniMax build(PrefKnowledge knowledge, AggOps operator, double w_1, double w_2) {
		checkArgument(operator.equals(AggOps.WEIGHTED_AVERAGE));
		checkArgument(w_1 > 0);
		checkArgument(w_2 > 0);
		op = operator;
		w1 = w_1;
		w2 = w_2;
		return new StrategyMiniMax(knowledge);
	}

	private StrategyMiniMax(PrefKnowledge knowledge) {
		this.knowledge = knowledge;
		profileCompleted = false;
	}

	public void setRounder(Rounder r) {
		regretComputer.setRounder(r);
	}
	
	@Override
	public Question nextQuestion() {
		final int m = knowledge.getAlternatives().size();

		checkArgument(m >= 2, "Questions can be asked only if there are at least two alternatives.");

		questions = new HashMap<>();

		for (Voter voter : knowledge.getVoters()) {
			final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();

			for (Alternative a1 : knowledge.getAlternatives()) {
				if (graph.adjacentNodes(a1).size() != m - 1) {
					for (Alternative a2 : knowledge.getAlternatives()) {
						if (!a1.equals(a2) && !graph.adjacentNodes(a1).contains(a2)) {
							Question q = Question.toVoter(voter, a1, a2);
							double score = getScore(q);
							questions.put(q, score);
						}
					}
				}
			}
		}

		final ArrayList<Integer> candidateRanks = IntStream.rangeClosed(1, m - 2).boxed()
				.collect(Collectors.toCollection(ArrayList::new));
		for (int rank : candidateRanks) {
			final Range<Aprational> lambdaRange = knowledge.getLambdaRange(rank);
			double diff = (lambdaRange.upperEndpoint().subtract(lambdaRange.lowerEndpoint())).doubleValue();
			if (diff > 0.1) {
				final Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
						.divide(new Apint(2));
				Question q = Question.toCommittee(avg, rank);
				double score = getScore(q);
				questions.put(q, score);
			}
		}

		checkArgument(!questions.isEmpty(), "No question to ask about weights or voters.");

		Question nextQ = questions.keySet().iterator().next();
		double minScore = questions.get(nextQ);
		nextQuestions= new LinkedList<>();
		nextQuestions.add(nextQ);
		
		for (Question q : questions.keySet()) {
			double score = questions.get(q);
			if (score < minScore) {
				nextQ = q;
				minScore = score;
				nextQuestions.clear();
				nextQuestions.add(nextQ);
			}
			if (score == minScore) {
				nextQuestions.add(q);
			}
		}
		return nextQ;
	}

	public double getScore(Question q) {
		PrefKnowledge yesKnowledge = PrefKnowledge.copyOf(knowledge);
		PrefKnowledge noKnowledge = PrefKnowledge.copyOf(knowledge);
		double yesMMR = 0;
		double noMMR = 0;
		if (q.getType().equals(QuestionType.VOTER_QUESTION)) {
			QuestionVoter qv = q.getQuestionVoter();
			Alternative a = qv.getFirstAlternative();
			Alternative b = qv.getSecondAlternative();

			yesKnowledge.getProfile().get(qv.getVoter()).asGraph().putEdge(a, b);
			Regret.getMMRAlternatives(yesKnowledge);
			yesMMR = Regret.getMMR();

			noKnowledge.getProfile().get(qv.getVoter()).asGraph().putEdge(b, a);
			Regret.getMMRAlternatives(noKnowledge);
			noMMR = Regret.getMMR();
		} else if (q.getType().equals(QuestionType.COMMITTEE_QUESTION)) {

			QuestionCommittee qc = q.getQuestionCommittee();
			Aprational lambda = qc.getLambda();
			int rank = qc.getRank();

			yesKnowledge.addConstraint(rank, ComparisonOperator.GE, lambda);
			noKnowledge.addConstraint(rank, ComparisonOperator.LE, lambda);

			Regret.getMMRAlternatives(yesKnowledge);
			yesMMR = Regret.getMMR();

			Regret.getMMRAlternatives(noKnowledge);
			noMMR = Regret.getMMR();
		}

		switch (op) {
		case MAX:
			return AggregationOperator.getMax(yesMMR, noMMR);
		case MIN:
			return AggregationOperator.getMin(yesMMR, noMMR);
		case WEIGHTED_AVERAGE:
			return AggregationOperator.weightedAvg(yesMMR, noMMR, w1, w2);
		case AVG:
			return AggregationOperator.getAvg(yesMMR, noMMR);
		default:
			throw new IllegalStateException();
		}
	}

	/** only for testing purposes */
	public static HashMap<Question, Double> getQuestions() {
		return questions;
	}
	
	/** only for testing purposes */
	public static List<Question> getNextQuestions() {
		return nextQuestions;
	}

}
