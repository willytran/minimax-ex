package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apfloat.Aprational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

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

/**
 * Uses the Regret to get the next question.
 **/
public class StrategyPessimistic implements Strategy {

	private static AggOps op;
	private ImmutableMap<Question, Double> questions;
	private static List<Question> nextQuestions;

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyPessimistic.class);
	private final StrategyHelper helper;

	public static StrategyPessimistic build() {
		op = AggOps.MAX;
		return new StrategyPessimistic();
	}

	public static StrategyPessimistic build(AggOps operator) {
		checkArgument(!operator.equals(AggOps.WEIGHTED_AVERAGE));
		op = operator;
		return new StrategyPessimistic();
	}

	private StrategyPessimistic() {
		LOGGER.info("Pessimistic");
		helper = StrategyHelper.newInstance();
	}

	@Override
	public Question nextQuestion() throws VerifyException {
		helper.getAndCheckSize();

		final Builder<Question, Double> questionsBuilder = ImmutableMap.builder();

		questionsBuilder.putAll(helper.getPossibleVoterQuestions().stream()
				.collect(ImmutableMap.toImmutableMap(q -> Question.toVoter(q), this::getScore)));

		questionsBuilder.putAll(helper.getQuestionsAboutLambdaRangesWiderThanOrAll(0.1).stream()
				.collect(ImmutableMap.toImmutableMap(q -> Question.toCommittee(q), this::getScore)));

		questions = questionsBuilder.build();
		verify(!questions.isEmpty());

		final double bestScore = questions.values().stream().max(Comparator.naturalOrder()).get();
		final ImmutableSet<Question> bestQuestions = questions.entrySet().stream()
				.filter(e -> e.getValue() == bestScore).map(Map.Entry::getKey).collect(ImmutableSet.toImmutableSet());
		final int pos = helper.nextInt(bestQuestions.size());
		return bestQuestions.asList().get(pos);
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

		switch (op) {
		case MAX:
			return AggregationOperator.getMax(yesMMR, noMMR);
		case MIN:
			return AggregationOperator.getMin(yesMMR, noMMR);
		case AVG:
			return AggregationOperator.getAvg(yesMMR, noMMR);
		case WEIGHTED_AVERAGE:
		default:
			throw new IllegalStateException();
		}
	}

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		helper.setKnowledge(knowledge);
	}

	/** only for testing purposes */
	public ImmutableMap<Question, Double> getQuestions() {
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
