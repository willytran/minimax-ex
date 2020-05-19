package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.minimax.regret.PairwiseMaxRegret;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.minimax.utils.AggregationOperator;
import io.github.oliviercailloux.minimax.utils.AggregationOperator.AggOps;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/**
 * Uses the Regret to get the next question.
 **/
public class StrategyPessimistic implements Strategy {

	public static ImmutableSet<EndpointPair<Alternative>> getIncomparablePairs(Graph<Alternative> graph) {
		final Set<Alternative> alternatives = graph.nodes();
		final ImmutableSet.Builder<EndpointPair<Alternative>> builder = ImmutableSet.builder();
		for (Alternative a1 : alternatives) {
			final Set<Alternative> known = graph.adjacentNodes(a1);
			alternatives.stream().filter(a2 -> !known.contains(a2)).filter(a2 -> !a2.equals(a1))
					.forEach(a2 -> builder.add(EndpointPair.unordered(a1, a2)));
		}
		return builder.build();
	}

	private PrefKnowledge knowledge;
	private static AggOps op;
	private static double w1;
	private static double w2;
	private HashMap<Question, Double> questions;
	private static List<Question> nextQuestions;

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyPessimistic.class);

	public static StrategyPessimistic build() {
		op = AggOps.MAX;
		return new StrategyPessimistic();
	}

	public static StrategyPessimistic build(AggOps operator) {
		checkArgument(!operator.equals(AggOps.WEIGHTED_AVERAGE));
		op = operator;
		return new StrategyPessimistic();
	}

	public static StrategyPessimistic build(AggOps operator, double w_1, double w_2) {
		checkArgument(operator.equals(AggOps.WEIGHTED_AVERAGE));
		checkArgument(w_1 > 0);
		checkArgument(w_2 > 0);
		op = operator;
		w1 = w_1;
		w2 = w_2;
		return new StrategyPessimistic();
	}

	private StrategyPessimistic() {
		LOGGER.info("Pessimistic");
	}

	@Override
	public Question nextQuestion() throws VerifyException {
		final int m = knowledge.getAlternatives().size();
		verify(m >= 2);
		if (m == 2) {
			checkState(!knowledge.isProfileComplete());
		}
		questions = new HashMap<>();

		for (Voter voter : knowledge.getVoters()) {
			final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();
			getIncomparablePairs(graph).stream().<Question>map(p -> Question.toVoter(voter, p.nodeU(), p.nodeV()))
					.collect(ImmutableMap.toImmutableMap(q -> q, this::getScore));
			for (Alternative a1 : knowledge.getAlternatives()) {
				if (graph.adjacentNodes(a1).size() != m - 1) {
					final Set<Alternative> known = graph.adjacentNodes(a1);
					final ImmutableMap<Question, Double> questionsToVoters = knowledge.getAlternatives().stream()
							.filter(a2 -> !known.contains(a2)).filter(a2 -> !a2.equals(a1))
							.map(a2 -> Question.toVoter(voter, a1, a2))
							.collect(ImmutableMap.toImmutableMap(q -> q, this::getScore));
					questions.putAll(questionsToVoters);
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

		if (questions.isEmpty()) {
			for (int rank : candidateRanks) {
				final Range<Aprational> lambdaRange = knowledge.getLambdaRange(rank);

				assert (!lambdaRange.lowerEndpoint().equals(lambdaRange.upperEndpoint()));

				final Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
						.divide(new Apint(2));
				Question q = Question.toCommittee(avg, rank);
				double score = getScore(q);
				questions.put(q, score);

			}
		}

		assert !questions.isEmpty();

		Question nextQ = questions.keySet().iterator().next();
		double minScore = questions.get(nextQ);
		nextQuestions = new LinkedList<>();
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

		if (q.getType().equals(QuestionType.VOTER_QUESTION)) {
			QuestionVoter qv = q.asQuestionVoter();
			Alternative a = qv.getFirstAlternative();
			Alternative b = qv.getSecondAlternative();
			yesKnowledge.getProfile().get(qv.getVoter()).asGraph().putEdge(a, b);
			noKnowledge.getProfile().get(qv.getVoter()).asGraph().putEdge(b, a);
		} else if (q.getType().equals(QuestionType.COMMITTEE_QUESTION)) {
			QuestionCommittee qc = q.asQuestionCommittee();
			Aprational lambda = qc.getLambda();
			int rank = qc.getRank();
			yesKnowledge.addConstraint(rank, ComparisonOperator.GE, lambda);
			noKnowledge.addConstraint(rank, ComparisonOperator.LE, lambda);
		}

		RegretComputer rcYes = new RegretComputer(yesKnowledge);
		SetMultimap<Alternative, PairwiseMaxRegret> mmrYes = rcYes.getMinimalMaxRegrets().asMultimap();
		Alternative xStarYes = mmrYes.keySet().iterator().next();
		double yesMMR = mmrYes.get(xStarYes).iterator().next().getPmrValue();

		RegretComputer rcNo = new RegretComputer(noKnowledge);
		SetMultimap<Alternative, PairwiseMaxRegret> mmrNo = rcNo.getMinimalMaxRegrets().asMultimap();
		Alternative xStarNo = mmrNo.keySet().iterator().next();
		double noMMR = mmrNo.get(xStarNo).iterator().next().getPmrValue();

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

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		checkArgument(knowledge.getAlternatives().size() >= 2);
		this.knowledge = checkNotNull(knowledge);
	}

	/** only for testing purposes */
	public HashMap<Question, Double> getQuestions() {
		return questions;
	}

	/** only for testing purposes */
	public static List<Question> getNextQuestions1() {
		return nextQuestions;
	}

	@Override
	public String toString() {
		return "Pessimistic";
	}

	@Override
	public StrategyType getStrategyType() {
		switch (op) {
		case MAX:
			return StrategyType.PESSIMISTIC_MAX;
		case MIN:
			return StrategyType.PESSIMISTIC_MIN;
		case WEIGHTED_AVERAGE:
			return StrategyType.PESSIMISTIC_WEIGHTED_AVG;
		case AVG:
			return StrategyType.PESSIMISTIC_AVG;
		default:
			throw new IllegalStateException();
		}
	}

}
