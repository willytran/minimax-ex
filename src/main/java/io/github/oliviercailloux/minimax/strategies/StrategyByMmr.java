package io.github.oliviercailloux.minimax.strategies;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.IntStream;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.ImmutableGraph;
import com.google.common.math.IntMath;

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
 * <p>
 * Uses the Regret to get the next question.
 * </p>
 * <p>
 * It is possible to constrain this strategy for asking the first q questions
 * only to voters (or only to committee), then the next q' questions only to
 * committee (or to voters), and so on. BUT this constraint will be lifted if it
 * is impossible to satisfy: for example, if the profile is entirely known and
 * the constraint mandates to ask the next question to voters, this strategy
 * will anyway ask a question to the committee.
 * </p>
 **/
public class StrategyByMmr implements Strategy {

	@JsonbPropertyOrder({ "kind", "number" })
	public static class QuestioningConstraint {
		@JsonbCreator
		public static QuestioningConstraint of(@JsonbProperty("kind") QuestionType kind,
				@JsonbProperty("number") int number) {
			return new QuestioningConstraint(kind, number);
		}

		private final QuestionType kind;
		private final int number;

		private QuestioningConstraint(QuestionType kind, int number) {
			this.kind = checkNotNull(kind);
			checkArgument(number >= 1);
			this.number = number;
		}

		public QuestionType getKind() {
			return kind;
		}

		public int getNumber() {
			return number;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this).add("kind", kind)
					.add("number", number == Integer.MAX_VALUE ? "∞" : number).toString();
		}
	}

	private static class QuestioningConstraints {
		public static QuestioningConstraints of(List<QuestioningConstraint> constraints) {
			return new QuestioningConstraints(constraints);
		}

		private final ImmutableList<QuestioningConstraint> constraints;
		private int asked;

		private QuestioningConstraints(List<QuestioningConstraint> constraints) {
			if (!constraints.isEmpty()) {
				checkArgument(constraints.stream().limit(constraints.size() - 1).map(QuestioningConstraint::getNumber)
						.noneMatch(n -> n == Integer.MAX_VALUE));
			}
			this.constraints = ImmutableList.copyOf(constraints);
			asked = 0;
		}

		public void next() {
			++asked;
		}

		public boolean hasCurrentConstraint() {
			if (!constraints.isEmpty() && constraints.get(constraints.size() - 1).number == Integer.MAX_VALUE) {
				return true;
			}
			final int nbConstraints = constraints.stream().mapToInt(QuestioningConstraint::getNumber).reduce(0,
					IntMath::checkedAdd);
			return asked < nbConstraints;
		}

		public QuestionType getCurrentConstraint() {
			checkState(hasCurrentConstraint());

			int skip = asked;
			final Iterator<QuestioningConstraint> iterator = constraints.iterator();
			QuestioningConstraint current = null;
			while (skip >= 0 && iterator.hasNext()) {
				current = iterator.next();
				skip -= current.getNumber();
			}
			verify(skip < 0);
			assert (current != null);
			return current.getKind();
		}

		public boolean mayAskCommittee() {
			return hasCurrentConstraint() ? getCurrentConstraint().equals(QuestionType.COMMITTEE_QUESTION) : true;
		}

		public boolean mayAskVoters() {
			return hasCurrentConstraint() ? getCurrentConstraint().equals(QuestionType.VOTER_QUESTION) : true;
		}

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyByMmr.class);
	private final StrategyHelper helper;
	private final DoubleBinaryOperator mmrOperator;
	private boolean limited;

	private ImmutableMap<Question, Double> questions;
	private QuestioningConstraints constraints;

	public static StrategyByMmr build() {
		return new StrategyByMmr(MmrOperator.MAX, false, ImmutableList.of());
	}

	public static StrategyByMmr build(DoubleBinaryOperator mmrOperator) {
		return new StrategyByMmr(mmrOperator, false, ImmutableList.of());
	}

	public static StrategyByMmr limited(List<QuestioningConstraint> constraints) {
		return new StrategyByMmr(MmrOperator.MAX, true, constraints);
	}

	private StrategyByMmr(DoubleBinaryOperator mmrOperator, boolean limited, List<QuestioningConstraint> constraints) {
		helper = StrategyHelper.newInstance();
		this.mmrOperator = checkNotNull(mmrOperator);
		this.limited = limited;
		LOGGER.debug("Creating with constraints: {}.", constraints);
		this.constraints = QuestioningConstraints.of(constraints);
	}

	public void setRandom(Random random) {
		helper.setRandom(random);
	}

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		helper.setKnowledge(knowledge);
	}

	public boolean isLimited() {
		return limited;
	}

	public void setLimited(boolean limited) {
		this.limited = limited;
	}

	@Override
	public Question nextQuestion() {
		final int m = helper.getAndCheckM();

		final boolean allowCommittee = (constraints.mayAskCommittee() || helper.getKnowledge().isProfileComplete())
				&& m >= 3;
		final boolean allowVoters = (constraints.mayAskVoters() || m == 2)
				&& !helper.getKnowledge().isProfileComplete();

		LOGGER.debug("Next question, allowing committee? {}; allowing voters? {}.", allowCommittee, allowVoters);

		final ImmutableSet.Builder<Question> questionsBuilder = ImmutableSet.builder();

		if (limited) {
			final ImmutableSetMultimap<Alternative, PairwiseMaxRegret> mmrs = helper.getMinimalMaxRegrets()
					.asMultimap();
			final Alternative xStar = helper.draw(mmrs.keySet());
			final PairwiseMaxRegret pmr = helper.draw(mmrs.get(xStar).stream().collect(ImmutableSet.toImmutableSet()));
			if (allowVoters) {
				final Alternative yBar = pmr.getY();
				helper.getQuestionableVoters().stream().map(v -> getLimitedQuestion(xStar, yBar, v))
						.forEach(q -> questionsBuilder.add(Question.toVoter(q)));
			}

			if (allowCommittee) {
				final PSRWeights wBar = pmr.getWeights();
				final PSRWeights wMin = getMinTauW(pmr);
				final ImmutableSet<Integer> minSpreadRanks = StrategyHelper
						.getMinimalElements(IntStream.rangeClosed(1, m - 2).boxed(), i -> getSpread(wBar, wMin, i));
				final QuestionCommittee qC = helper.getQuestionAboutHalfRange(helper.draw(minSpreadRanks));
				questionsBuilder.add(Question.toCommittee(qC));
			}
		} else {
			if (allowVoters) {
				helper.getPossibleVoterQuestions().stream().forEach(q -> questionsBuilder.add(Question.toVoter(q)));
			}
			if (allowCommittee) {
				helper.getQuestionsAboutLambdaRangesWiderThanOrAll(0.1).stream()
						.forEach(q -> questionsBuilder.add(Question.toCommittee(q)));
			}
		}
		constraints.next();

		questions = questionsBuilder.build().stream().collect(ImmutableMap.toImmutableMap(s -> s, this::getScore));
		verify(!questions.isEmpty());

		final ImmutableSet<Question> bestQuestions = StrategyHelper.getMinimalElements(questions.keySet().stream(),
				questions::get);
		LOGGER.debug("Best questions: {}.", bestQuestions);
		return helper.draw(bestQuestions);
	}

	private double getSpread(PSRWeights wBar, PSRWeights wMin, int i) {
		return IntStream.rangeClosed(0, 2).boxed()
				.mapToDouble(k -> Math.abs(wBar.getWeightAtRank(i + k) - wMin.getWeightAtRank(i + k))).sum();
	}

	public QuestionVoter getLimitedQuestion(Alternative xStar, Alternative yBar, Voter voter) {
		final ImmutableGraph<Alternative> graph = helper.getKnowledge().getPartialPreference(voter).asTransitiveGraph();
		final QuestionVoter question;
		if (!graph.adjacentNodes(xStar).contains(yBar)) {
			if (xStar.equals(yBar)) {
				verify(helper.getMinimalMaxRegrets().getMinimalMaxRegretValue() == 0d);
				/** We do not care which question we ask. */
				question = helper.getPossibleVoterQuestions().iterator().next();
			} else {
				question = QuestionVoter.given(voter, xStar, yBar);
			}
		} else {
			final Alternative tryFirst;
			final Alternative trySecond;
			if (graph.hasEdgeConnecting(xStar, yBar)) {
				tryFirst = xStar;
				trySecond = yBar;
			} else if (graph.hasEdgeConnecting(yBar, xStar)) {
				tryFirst = yBar;
				trySecond = xStar;
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

	public double getScore(Question q) {
		final double yesMMR;
		{
			final PrefKnowledge updatedKnowledge = PrefKnowledge.copyOf(helper.getKnowledge());
			updatedKnowledge.update(q.getPositiveInformation());
			final RegretComputer rc = new RegretComputer(updatedKnowledge);
			yesMMR = rc.getMinimalMaxRegrets().getMinimalMaxRegretValue();
		}

		final double noMMR;
		{
			final PrefKnowledge updatedKnowledge = PrefKnowledge.copyOf(helper.getKnowledge());
			updatedKnowledge.update(q.getNegativeInformation());
			final RegretComputer rc = new RegretComputer(updatedKnowledge);
			noMMR = rc.getMinimalMaxRegrets().getMinimalMaxRegretValue();
		}

		return mmrOperator.applyAsDouble(yesMMR, noMMR);
	}

	ImmutableSet<Question> getQuestions() {
		return questions.keySet();
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
