package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.IntStream;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.ImmutableGraph;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.jlp.elements.SumTerms;
import io.github.oliviercailloux.minimax.elicitation.ConstraintsOnWeights;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.minimax.regret.PairwiseMaxRegret;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

/**
 * Uses the Regret to get the next question.
 **/
public class StrategyByMmr implements Strategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyByMmr.class);
	private ImmutableMap<Question, Double> questions;
	private final StrategyHelper helper;
	private final DoubleBinaryOperator mmrOperator;
	private final boolean limited;

	public static StrategyByMmr build() {
		return new StrategyByMmr(MmrOperator.MAX);
	}

	public static StrategyByMmr build(DoubleBinaryOperator mmrOperator) {
		return new StrategyByMmr(mmrOperator);
	}

	private StrategyByMmr(DoubleBinaryOperator mmrOperator) {
		helper = StrategyHelper.newInstance();
		this.mmrOperator = checkNotNull(mmrOperator);
		limited = false;
	}

	public void setRandom(Random random) {
		helper.setRandom(random);
	}

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		helper.setKnowledge(knowledge);
	}

	@Override
	public Question nextQuestion() {
		final int m = helper.getAndCheckM();

		final Builder<Question, Double> questionsBuilder = ImmutableMap.builder();

		if (limited) {
			final ImmutableSetMultimap<Alternative, PairwiseMaxRegret> mmrs = helper.getMinimalMaxRegrets()
					.asMultimap();
			final Alternative xStar = helper.draw(mmrs.keySet());
			final PairwiseMaxRegret pmr = helper.draw(mmrs.get(xStar).stream().collect(ImmutableSet.toImmutableSet()));
			final Alternative yBar = pmr.getY();
			helper.getQuestionableVoters().stream().map(v -> getLimitedQuestion(xStar, yBar, v))
					.forEach(q -> questionsBuilder.put(Question.toVoter(q), getScore(q)));

			final PSRWeights wBar = pmr.getWeights();
			final PSRWeights wMin = getMinTauW(pmr);
			final ImmutableSetMultimap<Double, Integer> ranksBySpread = IntStream.rangeClosed(1, m).boxed()
					.collect(ImmutableSetMultimap.toImmutableSetMultimap(
							i -> Math.abs(wBar.getWeightAtRank(i) - wMin.getWeightAtRank(i)), i -> i));
			final double minSpread = ranksBySpread.keySet().stream().min(Comparator.naturalOrder()).get();
			final ImmutableSet<Integer> minSpreadRanks = ranksBySpread.get(minSpread);
			final QuestionCommittee qC = helper.getQuestionAboutHalfRange(helper.draw(minSpreadRanks));
			questionsBuilder.put(Question.toCommittee(qC), getScore(qC));
		} else {
			questionsBuilder.putAll(helper.getPossibleVoterQuestions().stream()
					.collect(ImmutableMap.toImmutableMap(q -> Question.toVoter(q), this::getScore)));

			questionsBuilder.putAll(helper.getQuestionsAboutLambdaRangesWiderThanOrAll(0.1).stream()
					.collect(ImmutableMap.toImmutableMap(q -> Question.toCommittee(q), this::getScore)));
		}

		questions = questionsBuilder.build();
		verify(!questions.isEmpty());

		final double bestScore = questions.values().stream().min(Comparator.naturalOrder()).get();
		final ImmutableSet<Question> bestQuestions = questions.entrySet().stream()
				.filter(e -> e.getValue() == bestScore).map(Map.Entry::getKey).collect(ImmutableSet.toImmutableSet());
		LOGGER.debug("Best questions: {}.", bestQuestions);
		return helper.draw(bestQuestions);
	}

	public QuestionVoter getLimitedQuestion(Alternative xStar, Alternative yBar, Voter voter) {
		final ImmutableGraph<Alternative> graph = helper.getKnowledge().getPartialPreference(voter).asTransitiveGraph();
		final QuestionVoter question;
		if (!graph.adjacentNodes(xStar).contains(yBar)) {
			question = QuestionVoter.given(voter, xStar, yBar);
		} else {
			final Alternative tryFirst;
			final Alternative trySecond;
			if (graph.hasEdgeConnecting(xStar, yBar)) {
				tryFirst = xStar;
				trySecond = yBar;
			} else if (graph.hasEdgeConnecting(yBar, xStar)) {
				tryFirst = xStar;
				trySecond = yBar;
			} else {
				throw new VerifyException(String.valueOf(xStar.equals(yBar))
						+ " Should reach here only when profile is complete or some weights are known to be equal, which we suppose will not happen.");
			}
			question = getQuestionAboutIncomparableTo(voter, graph, tryFirst)
					.or(() -> getQuestionAboutIncomparableTo(voter, graph, trySecond))
					.orElseGet(() -> getQuestionAbout(voter, helper.draw(StrategyHelper.getIncomparablePairs(graph))));
		}
		return question;
	}

	private Optional<QuestionVoter> getQuestionAboutIncomparableTo(Voter voter, Graph<Alternative> graph,
			Alternative a) {
		final ImmutableSet<Alternative> incomparables = StrategyHelper.getIncomparables(graph, a)
				.collect(ImmutableSet.toImmutableSet());
		return incomparables.isEmpty() ? Optional.empty()
				: Optional.of(QuestionVoter.given(voter, a, helper.draw(incomparables)));
	}

	private QuestionVoter getQuestionAbout(Voter voter, EndpointPair<Alternative> incomparablePair) {
		return QuestionVoter.given(voter, incomparablePair.nodeU(), incomparablePair.nodeV());
	}

	public double getScore(QuestionVoter q) {
		return getScore(Question.toVoter(q));
	}

	public double getScore(QuestionCommittee q) {
		return getScore(Question.toCommittee(q));
	}

	public double getScore(Question q) {
		PrefKnowledge yesKnowledge = PrefKnowledge.copyOf(helper.getKnowledge());
		PrefKnowledge noKnowledge = PrefKnowledge.copyOf(helper.getKnowledge());

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

		return mmrOperator.applyAsDouble(yesMMR, noMMR);
	}

	ImmutableMap<Question, Double> getQuestions() {
		return questions;
	}

	private PSRWeights getMinTauW(PairwiseMaxRegret pmr) {
		final ImmutableSortedMultiset<Integer> multiSetOfRanksOfX = ImmutableSortedMultiset
				.copyOf(pmr.getRanksOfX().values());
		final ImmutableSortedMultiset<Integer> multiSetOfRanksOfY = ImmutableSortedMultiset
				.copyOf(pmr.getRanksOfY().values());

		final RegretComputer regretComputer = helper.getRegretComputer();

		final SumTerms sumTerms = regretComputer.getTermScoreYMinusScoreX(multiSetOfRanksOfY, multiSetOfRanksOfX);
		final ConstraintsOnWeights cow = helper.getKnowledge().getConstraintsOnWeights();
		cow.minimize(sumTerms);
		return cow.getLastSolution();
	}

}
