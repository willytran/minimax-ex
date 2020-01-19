package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
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
import io.github.oliviercailloux.minimax.utils.Rounder;
import io.github.oliviercailloux.minimax.utils.AggregationOperator.AggOps;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/** Uses the Regret to get the next question. **/

public class StrategyMiniMaxIncr implements Strategy {

	private PrefKnowledge knowledge;
	public boolean profileCompleted;

	private static double w1;
	private static double w2;
	private static HashMap<Question, Double> questions;
	private static List<Question> nextQuestions;
	private static RegretComputer regretComputer;


	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyMiniMaxIncr.class);

	public static StrategyMiniMaxIncr build(PrefKnowledge knowledge) {
		return new StrategyMiniMaxIncr(knowledge);
	}

	private StrategyMiniMaxIncr(PrefKnowledge knowledge) {
		this.knowledge = knowledge;
		profileCompleted = false;
		LOGGER.info("");
	}

	@Override
	public Question nextQuestion() {
		final int m = knowledge.getAlternatives().size();

		checkArgument(m >= 2, "Questions can be asked only if there are at least two alternatives.");
		
		
		
		
//		questions = new HashMap<>();
//
//		final ArrayList<Integer> candidateRanks = IntStream.rangeClosed(1, m - 2).boxed()
//				.collect(Collectors.toCollection(ArrayList::new));
//		for (int rank : candidateRanks) {
//			final Range<Aprational> lambdaRange = knowledge.getLambdaRange(rank);
//			double diff = (lambdaRange.upperEndpoint().subtract(lambdaRange.lowerEndpoint())).doubleValue();
//			if (diff > 0.1) {
//				final Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
//						.divide(new Apint(2));
//				Question q = Question.toCommittee(avg, rank);
//				double score = getScore(q);
//				questions.put(q, score);
//			}
//		}
//
//		checkArgument(!questions.isEmpty(), "No question to ask about weights or voters.");
//
//		Question nextQ = questions.keySet().iterator().next();
//		double minScore = questions.get(nextQ);
//		nextQuestions = new LinkedList<>();
//		nextQuestions.add(nextQ);
//
//		for (Question q : questions.keySet()) {
//			double score = questions.get(q);
//			if (score < minScore) {
//				nextQ = q;
//				minScore = score;
//				nextQuestions.clear();
//				nextQuestions.add(nextQ);
//			}
//			if (score == minScore) {
//				nextQuestions.add(q);
//			}
//		}
		return questionToVot();
	}

	private Question questionToVot() {
		int m=knowledge.getAlternatives().size();
		final ImmutableSet.Builder<Voter> questionableVotersBuilder = ImmutableSet.builder();
		final Map<Voter, Graph<Alternative>> transitiveGraphV= new HashMap<>();
		for (Voter voter : knowledge.getVoters()) {
			final Graph<Alternative> graph = knowledge.getProfile().get(voter).asTransitiveGraph();
			transitiveGraphV.put(voter, graph);
			if (graph.edges().size() != m * (m - 1) / 2) {
				questionableVotersBuilder.add(voter);
			}
		}
		final ImmutableSet<Voter> questionableVoters = questionableVotersBuilder.build();

		final boolean existsQuestionVoters = !questionableVoters.isEmpty();

		checkArgument(existsQuestionVoters, "No question to ask about voters.");
		
		regretComputer = new RegretComputer(knowledge);
		final SetMultimap<Alternative, PairwiseMaxRegret> pmrsAlt = regretComputer.getHPmrValues();

		final SetMultimap<Double, Alternative> byPmrValues = MultimapBuilder.treeKeys().linkedHashSetValues().build();
		
		for (Alternative x : knowledge.getAlternatives()) {
			byPmrValues.put(pmrsAlt.get(x).iterator().next().getPmrValue(), x);
		}
		final SortedMap<Double, Collection<Alternative>> sortedByPmrValues = (SortedMap<Double, Collection<Alternative>>) byPmrValues
				.asMap();
		Iterator<Double> it = sortedByPmrValues.keySet().iterator();
		while (it.hasNext()) {
			for (Alternative a : sortedByPmrValues.get(it.next())) {
				for (PairwiseMaxRegret pmrs : pmrsAlt.get(a)) {
					Alternative x=pmrs.getX();
					Alternative y=pmrs.getY();
					for (Voter v : knowledge.getVoters()) {
						if (!x.equals(y) && !transitiveGraphV.get(v).adjacentNodes(x).contains(y)) {
							return Question.toVoter(v, x, y);
						}
					}
				}
			}
		}
		return null;
	}
}
