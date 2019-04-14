package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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

import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.minimax.utils.AggregationOperator;
import io.github.oliviercailloux.minimax.utils.AggregationOperator.AggOps;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class StrategyTwoPhases implements Strategy {

	private PrefKnowledge knowledge;
	public boolean profileCompleted;
	private static AggOps op;
	private static double w1;
	private static double w2;
	private static HashMap<QuestionVoter, Double> voterQuestions;
	private static List<QuestionCommittee> committeeQuestions;
	private static Iterator<QuestionCommittee> i;

	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyMiniMax.class);

	public static StrategyTwoPhases build(PrefKnowledge knowledge) {
		op = AggOps.MAX;
		return new StrategyTwoPhases(knowledge);
	}

	public static StrategyTwoPhases build(PrefKnowledge knowledge, AggOps operator) {
		checkArgument(!operator.equals(AggOps.WEIGHTED_AVERAGE));
		op = operator;
		return new StrategyTwoPhases(knowledge);
	}

	public static StrategyTwoPhases build(PrefKnowledge knowledge, AggOps operator, double w_1, double w_2) {
		checkArgument(operator.equals(AggOps.WEIGHTED_AVERAGE));
		checkArgument(w_1 > 0);
		checkArgument(w_2 > 0);
		op = operator;
		w1 = w_1;
		w2 = w_2;
		return new StrategyTwoPhases(knowledge);
	}

	private StrategyTwoPhases(PrefKnowledge knowledge) {
		this.knowledge = knowledge;
		profileCompleted = false;
		committeeQuestions = new LinkedList<>();
		final ArrayList<Integer> candidateRanks = IntStream.rangeClosed(1, knowledge.getAlternatives().size() - 2)
				.boxed().collect(Collectors.toCollection(ArrayList::new));
		for (int rank : candidateRanks) {
			final Range<Aprational> lambdaRange = knowledge.getLambdaRange(rank);
			double diff = (lambdaRange.upperEndpoint().subtract(lambdaRange.lowerEndpoint())).doubleValue();
			if (diff > 0.1) {
				final Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
						.divide(new Apint(2));
				QuestionCommittee q = QuestionCommittee.given(avg, rank);
				committeeQuestions.add(q);
			}
		}
		i = committeeQuestions.iterator();
	}

	@Override
	public Question nextQuestion() {
		final int m = knowledge.getAlternatives().size();
		checkArgument(m >= 2, "Questions can be asked only if there are at least two alternatives.");

		if (i.hasNext()) {
			return Question.toCommittee(i.next());
		}

		voterQuestions = new HashMap<>();

		for (Voter voter : knowledge.getVoters()) {
			final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();

			for (Alternative a1 : knowledge.getAlternatives()) {
				if (graph.adjacentNodes(a1).size() != m - 1) {
					for (Alternative a2 : knowledge.getAlternatives()) {
						if (!a1.equals(a2) && !graph.adjacentNodes(a1).contains(a2)) {
							QuestionVoter q = QuestionVoter.given(voter, a1, a2);
							double score = getScore(q);
							voterQuestions.put(q, score);
						}
					}
				}
			}
		}

		checkArgument(!voterQuestions.isEmpty(), "No question to ask about weights or voters.");

		QuestionVoter nextQ = voterQuestions.keySet().iterator().next();
		double minScore = voterQuestions.get(nextQ);

		for (QuestionVoter q : voterQuestions.keySet()) {
			double score = voterQuestions.get(q);
			if (score < minScore) {
				nextQ = q;
				minScore = score;
			}
		}
		return Question.toVoter(nextQ);
	}

	public double getScore(QuestionVoter q) {
		PrefKnowledge yesKnowledge = PrefKnowledge.copyOf(knowledge);
		PrefKnowledge noKnowledge = PrefKnowledge.copyOf(knowledge);
		double yesMMR = 0;
		double noMMR = 0;

		Alternative a = q.getFirstAlternative();
		Alternative b = q.getSecondAlternative();

		yesKnowledge.getProfile().get(q.getVoter()).asGraph().putEdge(a, b);
		Regret.getMMRAlternatives(yesKnowledge);
		yesMMR = Regret.getMMR();

		noKnowledge.getProfile().get(q.getVoter()).asGraph().putEdge(b, a);
		Regret.getMMRAlternatives(noKnowledge);
		noMMR = Regret.getMMR();

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
	public static HashMap<QuestionVoter, Double> getVoterQuestions() {
		return voterQuestions;
	}

}
