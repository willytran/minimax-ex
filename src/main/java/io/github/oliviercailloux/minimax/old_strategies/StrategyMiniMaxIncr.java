package io.github.oliviercailloux.minimax.old_strategies;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.jlp.elements.SumTerms;
import io.github.oliviercailloux.jlp.elements.SumTermsBuilder;
import io.github.oliviercailloux.minimax.Strategy;
import io.github.oliviercailloux.minimax.elicitation.ConstraintsOnWeights;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.regret.PairwiseMaxRegret;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.minimax.regret.Regrets;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/** Uses the Regret to get the next question. **/

public class StrategyMiniMaxIncr implements Strategy {

	private PrefKnowledge knowledge;
	public boolean profileCompleted;

	private RegretComputer regretComputer;

	private int questionsToVoters;
	private int questionsToCommittee;
	private boolean committeeFirst;

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyMiniMaxIncr.class);

	public static StrategyMiniMaxIncr build(int questionsToVoters, int questionsToCommittee, boolean committeeFirst) {
		return new StrategyMiniMaxIncr(questionsToVoters, questionsToCommittee, committeeFirst);
	}

	private StrategyMiniMaxIncr(int qToVoters, int qToCommittee, boolean cFirst) {
		questionsToVoters = qToVoters;
		questionsToCommittee = qToCommittee;
		committeeFirst = cFirst;
		profileCompleted = false;
		LOGGER.info("");
	}

	@Override
	public Question nextQuestion() {
		final int m = knowledge.getAlternatives().size();
		checkArgument(m >= 2, "Questions can be asked only if there are at least two alternatives.");
		checkArgument(questionsToVoters != 0 || questionsToCommittee != 0, "No more questions allowed");
		Question q;

		SetMultimap<Alternative, PairwiseMaxRegret> mmr = regretComputer.getMinimalMaxRegrets().asMultimap();
		Alternative xStar = mmr.keySet().iterator().next();
		PairwiseMaxRegret currentSolution = mmr.get(xStar).iterator().next();
		Alternative yBar = currentSolution.getY();
		PSRWeights wBar = currentSolution.getWeights();

		if (committeeFirst) {
			if (questionsToCommittee > 0) {
				q = selectQuestionToCommittee(wBar, xStar, yBar);
				questionsToCommittee--;
			} else {
				q = selectQuestionToVoters();
				questionsToVoters--;
			}
		} else {
			if (questionsToVoters > 0) {
				q = selectQuestionToVoters();
				questionsToVoters--;
			} else {
				q = selectQuestionToCommittee(wBar, xStar, yBar);
				questionsToCommittee--;
			}
		}
		return q;

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
	}

	private Question selectQuestionToCommittee(PSRWeights wBar, Alternative xStar, Alternative yBar) {
		PSRWeights wMin = getMinTauW(xStar, yBar);

		int maxRank = 2;
		double maxDiff = Math.abs(wBar.getWeightAtRank(maxRank) - wMin.getWeightAtRank(maxRank));

		for (int i = maxRank + 1; i <= knowledge.getAlternatives().size(); i++) {
			double diff = Math.abs(wBar.getWeightAtRank(i) - wMin.getWeightAtRank(i));
			if (diff > maxDiff) {
				maxDiff = diff;
				maxRank = i;
			}
		}

		Range<Aprational> lambdaRange = knowledge.getLambdaRange(maxRank - 1);
		Aprational avg = AprationalMath.sum(lambdaRange.lowerEndpoint(), lambdaRange.upperEndpoint())
				.divide(new Apint(2));
		return Question.toCommittee(QuestionCommittee.given(avg, maxRank - 1));
	}

	private Question selectQuestionToVoters() {
		List<Question> questions = questionsVoters();
		int randomPos = (int) (questions.size() * Math.random());
		return questions.get(randomPos);
	}

	private List<Question> questionsVoters() {
		int m = knowledge.getAlternatives().size();
		final ImmutableSet.Builder<Voter> questionableVotersBuilder = ImmutableSet.builder();
		final Map<Voter, Graph<Alternative>> transitiveGraphV = new HashMap<>();
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
		List<Question> questionsV = new LinkedList<>();

		Regrets r = regretComputer.getAllPairwiseMaxRegrets();
		ImmutableMap<Alternative, SortedMap<Double, Set<PairwiseMaxRegret>>> rsort = r.getRegretsSorted();
		Map<Double, Set<PairwiseMaxRegret>> maxPMRS = new HashMap<>();
		Set<PairwiseMaxRegret> maxpmrs = new HashSet<>();
		for (Alternative a : rsort.keySet()) {
			double maxA = rsort.get(a).lastKey();
			maxPMRS.put(maxA, rsort.get(a).get(maxA));
			maxpmrs.addAll(rsort.get(a).get(maxA));
		}

//		ImmutableSortedMap<Double, Set<PairwiseMaxRegret>> sort = maxPMRS.entrySet().stream()
//				.collect(ImmutableSortedMap.toImmutableSortedMap(Comparator.naturalOrder(), Entry::getKey,
//						(e) -> ImmutableSet.copyOf(e.getValue())));
		for (PairwiseMaxRegret pmr : maxpmrs) {
			Alternative x = pmr.getX();
			Alternative y = pmr.getY();
			for (Voter v : knowledge.getVoters()) {
				if (!x.equals(y) && !transitiveGraphV.get(v).adjacentNodes(x).contains(y)) {
					questionsV.add(Question.toVoter(v, x, y));
				}
			}
		}

		return questionsV;
	}

	private PSRWeights getMinTauW(Alternative xOpt, Alternative yAdv) {
		int m = knowledge.getAlternatives().size();
		int[] xrank = new int[m + 1];
		int[] yrank = new int[m + 1];
		Map<Voter, Integer> xRanks = regretComputer.getWorstRanksOfX(xOpt);
		Map<Voter, Integer> yRanks = regretComputer.getBestRanksOfY(xOpt, yAdv);

		for (Voter v : knowledge.getProfile().keySet()) {
			xrank[xRanks.get(v)]++;
			yrank[yRanks.get(v)]++;
		}

		ConstraintsOnWeights cow = ConstraintsOnWeights.copyOf(knowledge.getConstraintsOnWeights());
		SumTermsBuilder sb = SumTerms.builder();

		for (int i = 1; i <= m; i++) {
			sb.add(cow.getTerm(yrank[i] - xrank[i], i));
		}
		SumTerms objective = sb.build();
		cow.minimize(objective);
		PSRWeights p = cow.getLastSolution();
		return p;
	}

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		this.knowledge = knowledge;
		regretComputer = new RegretComputer(knowledge);
	}

//		for (Voter v : knowledge.getVoters()) {
//			for(PairwiseMaxRegret pmr : maxPMRS) {
//				Alternative x = pmr.getX();
//				Alternative y = pmr.getY();
//				if (!x.equals(y) && !transitiveGraphV.get(v).adjacentNodes(x).contains(y)) {
//					questionsV.add(Question.toVoter(v, x, y));
//				}
//			}
//		}

//		final SetMultimap<Alternative, PairwiseMaxRegret> pmrsAlt = regretComputer.getHPmrValues();
//
//		final SetMultimap<Double, Alternative> byPmrValues = MultimapBuilder.treeKeys().linkedHashSetValues().build();
//
//		for (Alternative x : knowledge.getAlternatives()) {
//			byPmrValues.put(pmrsAlt.get(x).iterator().next().getPmrValue(), x);
//		}
//		final SortedMap<Double, Collection<Alternative>> sortedByPmrValues = (SortedMap<Double, Collection<Alternative>>) byPmrValues
//				.asMap();
//		Iterator<Double> it = sortedByPmrValues.keySet().iterator();
//		while (it.hasNext()) {
//			for (Alternative a : sortedByPmrValues.get(it.next())) {
//				for (PairwiseMaxRegret pmrs : pmrsAlt.get(a)) {
	/**
	 * TODO I suspect this whole bunch of code (now commented-out) simply does the
	 * following.
	 *
	 * final PairwiseMaxRegret pmrs =
	 * regretComputer.getMinimalMaxRegrets().values().iterator().next(); Alternative
	 * x = pmrs.getX(); Alternative y = pmrs.getY(); for (Voter v :
	 * knowledge.getVoters()) { if (!x.equals(y) &&
	 * !transitiveGraphV.get(v).adjacentNodes(x).contains(y)) { return
	 * Question.toVoter(v, x, y); } }
	 */

//				}
//			}
//		}

}
