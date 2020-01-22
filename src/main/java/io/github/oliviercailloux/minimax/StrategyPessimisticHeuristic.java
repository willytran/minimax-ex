package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.apfloat.AprationalMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.Graph;

import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.jlp.elements.SumTerms;
import io.github.oliviercailloux.jlp.elements.SumTermsBuilder;
import io.github.oliviercailloux.minimax.elicitation.ConstraintsOnWeights;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
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

/** Uses the Regret to get the next question. **/

public class StrategyPessimisticHeuristic implements Strategy {

	private PrefKnowledge knowledge;
	public boolean profileCompleted;
	private static AggOps op;
	private static double w1;
	private static double w2;

	private static Set<Question> questions;
	private static List<Question> nextQuestions;
	private static RegretComputer rc;

	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyPessimisticHeuristic.class);

	public static StrategyPessimisticHeuristic build(PrefKnowledge knowledge) {
		op = AggOps.MAX;
		return new StrategyPessimisticHeuristic(knowledge);
	}

	public static StrategyPessimisticHeuristic build(PrefKnowledge knowledge, AggOps operator) {
		checkArgument(!operator.equals(AggOps.WEIGHTED_AVERAGE));
		op = operator;
		return new StrategyPessimisticHeuristic(knowledge);
	}

	public static StrategyPessimisticHeuristic build(PrefKnowledge knowledge, AggOps operator, double w_1, double w_2) {
		checkArgument(operator.equals(AggOps.WEIGHTED_AVERAGE));
		checkArgument(w_1 > 0);
		checkArgument(w_2 > 0);
		op = operator;
		w1 = w_1;
		w2 = w_2;
		return new StrategyPessimisticHeuristic(knowledge);
	}

	private StrategyPessimisticHeuristic(PrefKnowledge knowledge) {
		setKnowledge(knowledge);
		profileCompleted = false;
		LOGGER.info("");
	}

	@Override
	public Question nextQuestion() {
		final int m = knowledge.getAlternatives().size();
		checkArgument(m >= 2, "Questions can be asked only if there are at least two alternatives.");

		questions = selectQuestions();
		checkArgument(!questions.isEmpty(), "No question to ask about weights or voters.");

		Question nextQ = questions.iterator().next();
		double minScore = getScore(nextQ);
		nextQuestions = new LinkedList<>();
		nextQuestions.add(nextQ);

		for (Question q : questions) {
			double score = getScore(q);
			if (score < minScore) {
				nextQ = q;
				minScore = score;
				nextQuestions.clear();
				nextQuestions.add(nextQ);
			} else {
				if (score == minScore && !nextQuestions.contains(q)) {
					nextQuestions.add(q);
				}
			}
		}
		int randomPos = (int) (nextQuestions.size() * Math.random());
		return nextQuestions.get(randomPos);
	}

	private Set<Question> selectQuestions() {
		SetMultimap<Alternative, PairwiseMaxRegret> mmr = rc.getMinimalMaxRegrets();
		Alternative xStar = mmr.keySet().iterator().next();
		PairwiseMaxRegret currentSolution = mmr.get(xStar).iterator().next();
		Alternative yBar = currentSolution.getY();
		PSRWeights wBar = currentSolution.getWeights();
		Set<Question> quests = new HashSet<>();
		quests.addAll(selectQuestionsVoters(xStar, yBar));
		quests.add(selectQuestionCommittee(wBar, xStar, yBar));
		return quests;
	}

	private Question selectQuestionCommittee(PSRWeights wBar, Alternative xStar, Alternative yBar) {
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

	private Set<Question> selectQuestionsVoters(Alternative xStar, Alternative yBar) {
		HashSet<Question> questv = new HashSet<>();
		for (Voter v : knowledge.getVoters()) {
			Question qv;
			Graph<Alternative> vpref = knowledge.getProfile().get(v).asTransitiveGraph();
			HashSet<Alternative> questionable;
			HashSet<Alternative> U = new HashSet<>();
			for (Alternative a : vpref.nodes()) {
				if (vpref.inDegree(a) == 0 && vpref.outDegree(a) == 0) {
					U.add(a);
				}
			}
			if (xStar.equals(yBar)) { // we ask something we don't know
				questionable = new HashSet<>(vpref.nodes());
				questionable.remove(xStar);
				questionable.removeAll(vpref.adjacentNodes(xStar));
			} else {
				if (vpref.hasEdgeConnecting(xStar, yBar)) { // questionable = D U C
					questionable = new HashSet<>(vpref.nodes());
					questionable.remove(xStar);
					questionable.remove(yBar);
					questionable.removeAll(vpref.predecessors(xStar));
					questionable.removeAll(vpref.successors(yBar));
					questionable.removeAll(U);
					HashSet<Alternative> B = new HashSet<>(vpref.successors(xStar));
					B.retainAll(vpref.predecessors(yBar));
					questionable.removeAll(B);
				} else {
					if (vpref.hasEdgeConnecting(yBar, xStar)) { // questionable = E U F
						questionable = new HashSet<>(vpref.nodes());
						questionable.remove(xStar);
						questionable.remove(yBar);
						questionable.removeAll(vpref.predecessors(yBar));
						questionable.removeAll(vpref.successors(xStar));
						questionable.removeAll(U);
						HashSet<Alternative> B = new HashSet<>(vpref.successors(yBar));
						B.retainAll(vpref.predecessors(xStar));
						questionable.removeAll(B);
					} else { // x and y are not comparable
						questionable = new HashSet<>();
						questionable.remove(xStar);
						questionable.add(yBar);
					}
				}
			}
			if (questionable.isEmpty()) {
				questionable = new HashSet<>();
				questionable.addAll(U);
				questionable.remove(xStar);
			}
			// if it is still empty it means we know everything about this voter so we don't
			// ask him anything
			if (!questionable.isEmpty()) {
				Alternative questAlt = questionable.iterator().next();
				if (questAlt.equals(yBar)) {
					qv = Question.toVoter(v, xStar, yBar);
				} else {
					if (vpref.hasEdgeConnecting(xStar, questAlt) || vpref.hasEdgeConnecting(questAlt, xStar)) {
						// questAlt is in D or in E
						qv = Question.toVoter(v, questAlt, yBar); // d>y or e>y
					} else {
						// either exist vpref.hasEdgeConnecting(questAlt, yBar) and questAlt is in C or
						// in F, or questAlt is in U
						qv = Question.toVoter(v, xStar, questAlt); // x>c, x>f or x>u
					}
				}
				assert (!vpref.hasEdgeConnecting(qv.getQuestionVoter().getFirstAlternative(),
						qv.getQuestionVoter().getSecondAlternative()));
				questv.add(qv);
			}
		}
		return questv;
	}

	private PSRWeights getMinTauW(Alternative xOpt, Alternative yAdv) {
		int m = knowledge.getAlternatives().size();
		int[] xrank = new int[m + 1];
		int[] yrank = new int[m + 1];
		Map<Voter, Integer> xRanks = rc.getWorstRanksOfX(xOpt);
		Map<Voter, Integer> yRanks = rc.getBestRanksOfY(xOpt, yAdv);

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

	private double getScore(Question q) {
		PrefKnowledge yesKnowledge = PrefKnowledge.copyOf(knowledge);
		PrefKnowledge noKnowledge = PrefKnowledge.copyOf(knowledge);

		if (q.getType().equals(QuestionType.VOTER_QUESTION)) {
			QuestionVoter qv = q.getQuestionVoter();
			Alternative a = qv.getFirstAlternative();
			Alternative b = qv.getSecondAlternative();
			yesKnowledge.getProfile().get(qv.getVoter()).asGraph().putEdge(a, b);
			noKnowledge.getProfile().get(qv.getVoter()).asGraph().putEdge(b, a);
		} else if (q.getType().equals(QuestionType.COMMITTEE_QUESTION)) {
			QuestionCommittee qc = q.getQuestionCommittee();
			Aprational lambda = qc.getLambda();
			int rank = qc.getRank();
			yesKnowledge.addConstraint(rank, ComparisonOperator.GE, lambda);
			noKnowledge.addConstraint(rank, ComparisonOperator.LE, lambda);
		}

		RegretComputer rcYes = new RegretComputer(yesKnowledge);
		SetMultimap<Alternative, PairwiseMaxRegret> mmrYes = rcYes.getMinimalMaxRegrets();
		Alternative xStarYes = mmrYes.keySet().iterator().next();
		double yesMMR = mmrYes.get(xStarYes).iterator().next().getPmrValue();

		RegretComputer rcNo = new RegretComputer(noKnowledge);
		SetMultimap<Alternative, PairwiseMaxRegret> mmrNo = rcNo.getMinimalMaxRegrets();
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
		this.knowledge = knowledge;
		rc = new RegretComputer(knowledge);
	}

	/** only for testing purposes */
	public static Set<Question> getQuestions() {
		return questions;
	}

}
