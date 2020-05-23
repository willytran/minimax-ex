package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import java.util.Comparator;
import java.util.Map;
import java.util.function.DoubleBinaryOperator;

import org.apfloat.Aprational;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.github.oliviercailloux.minimax.utils.MmrOperator;
import io.github.oliviercailloux.y2018.j_voting.Alternative;

/**
 * Uses the Regret to get the next question.
 **/
public class StrategyByMmr implements Strategy {

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyByMmr.class);
	private ImmutableMap<Question, Double> questions;
	private final StrategyHelper helper;
	private final DoubleBinaryOperator mmrOperator;

	public static StrategyByMmr build() {
		return new StrategyByMmr(MmrOperator.MAX);
	}

	public static StrategyByMmr build(DoubleBinaryOperator mmrOperator) {
		return new StrategyByMmr(mmrOperator);
	}

	private StrategyByMmr(DoubleBinaryOperator mmrOperator) {
		LOGGER.info("Pessimistic");
		helper = StrategyHelper.newInstance();
		this.mmrOperator = checkNotNull(mmrOperator);
	}

	@Override
	public Question nextQuestion() {
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

		return mmrOperator.applyAsDouble(yesMMR, noMMR);
	}

	@Override
	public void setKnowledge(PrefKnowledge knowledge) {
		helper.setKnowledge(knowledge);
	}

	ImmutableMap<Question, Double> getQuestions() {
		return questions;
	}

	@Override
	public String toString() {
		return "Pessimistic";
	}

}
