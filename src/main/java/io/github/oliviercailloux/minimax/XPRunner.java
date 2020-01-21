package io.github.oliviercailloux.minimax;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apfloat.Aprational;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import com.google.common.graph.MutableGraph;
import com.google.common.math.Stats;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.jlp.elements.ComparisonOperator;
import io.github.oliviercailloux.minimax.elicitation.Answer;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionCommittee;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.elicitation.QuestionVoter;
import io.github.oliviercailloux.minimax.old_strategies.StrategyPessimistic;
import io.github.oliviercailloux.minimax.old_strategies.StrategyTaus;
import io.github.oliviercailloux.minimax.old_strategies.StrategyTwoPhases;
import io.github.oliviercailloux.minimax.old_strategies.StrategyTwoPhasesTau;
import io.github.oliviercailloux.minimax.regret.PairwiseMaxRegret;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.minimax.utils.AggregationOperator.AggOps;
import io.github.oliviercailloux.minimax.utils.Generator;
import io.github.oliviercailloux.minimax.utils.Rounder;
import io.github.oliviercailloux.minimax.utils.Statistic;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class XPRunner {

	static BufferedWriter bwStats;
	static BufferedWriter bwQst;

	static String root;

	// for strategyTwoPhasesTau
	static int nbCommitteeQuestions = 10;
	static int nbVotersQuestions = 20;
	static boolean committeeFirst = true;

	public static void main(String[] args) throws IOException {
		int n = 0, m = 0;

		root = Paths.get("").toAbsolutePath() + "/experiments/";

		int minQuestions = 10;
		int maxQuestions = 15;
		int runs = 10;

		// for on m and n
		n = 5;
		m = 5;
		StrategyType st = StrategyType.PESSIMISTIC_MAX;

		serialExp(m, n, st, minQuestions, maxQuestions, runs);
	}

	/**
	 * It executes (maxQuestions-minQuestions)*runs experiments: Starting with zero
	 * knowledge, it asks minQuestions, it saves the final regret and repeat this
	 * process for a number of times specified by the variable "runs". At the end,
	 * it computes the avg of the statistics and increases the number of questions
	 * that can be asked until maxQuestions.
	 * 
	 * A run with a fixed number of questions can be performed by using
	 * minQuestions=maxQuestions 
	 * 
	 * A single run can be performed by using runs=1
	 */
	private static void serialExp(int m, int n, StrategyType st, int minQuestions, int maxQuestions, int runs)
			throws IOException {
		checkArgument(minQuestions <= maxQuestions);
		NumberFormat formatter = new DecimalFormat("#0.00000");
		NumberFormat formatterPerc = new DecimalFormat("#0.0%");

		createFiles(st, m, n, maxQuestions, runs);

		ArrayList<Double> questionSeriesMean = new ArrayList<>();
		ArrayList<Double> regretSeriesMean = new ArrayList<>();
		ArrayList<Double> avgLossSeriesMean = new ArrayList<>();
		ArrayList<Double> regretSeriesSD = new ArrayList<>();
		ArrayList<Double> avgLossSeriesSD = new ArrayList<>();
		int regret2 = -1, regret4 = -1, regret8 = -1;
		int avgLoss2 = -1, avgLoss4 = -1, avgLoss8 = -1;

		double initialRegret = n; // with 0 information the initial max regret is when s(y)=1*n and s(x)=0*n
		double initialAvgLoss = 1;

		for (int nbquest = minQuestions; nbquest <= maxQuestions; nbquest++) {
			List<Double> avglosses = new LinkedList<>();
			List<Double> regrets = new LinkedList<>();
			List<Double> percVotQuest = new LinkedList<>();
			Statistic stat;
			for (int run = 0; run < runs; run++) {
				bwQst.write("Run " + run + ": ");
				bwQst.flush();
				stat = run(m, n, st, nbquest);
				regrets.add(stat.getRegret());
				avglosses.add(stat.getAvgLoss());
				percVotQuest.add(stat.getPercentageQstVoters());
				bwQst.write("Duration " + formatter.format(stat.getTime() / 1000d) + " seconds \n\n");
				bwQst.flush();
			}
			Stats percQuest = Stats.of(percVotQuest);
			double percQstMean = percQuest.mean();

			Stats regretStats = Stats.of(regrets);
			double regretMean = regretStats.mean();
			double regretSD = regretStats.populationStandardDeviation();

			Stats lossStats = Stats.of(avglosses);
			double lossMean = lossStats.mean();
			double lossSD = lossStats.populationStandardDeviation();

			questionSeriesMean.add(percQstMean);
			regretSeriesMean.add(regretMean);
			avgLossSeriesMean.add(lossMean);
			regretSeriesSD.add(regretSD);
			avgLossSeriesSD.add(lossSD);

			if (nbquest == 1) {
				assert initialRegret == regretMean;
				initialAvgLoss = lossMean;
			}
			if (regretMean <= (initialRegret / 2) && regret2 < 0) {
				regret2 = nbquest;
			}
			if (regretMean <= (initialRegret / 4) && regret4 < 0) {
				regret4 = nbquest;
			}
			if (regretMean <= (initialRegret / 8) && regret8 < 0) {
				regret8 = nbquest;
			}
			if (lossMean <= (initialAvgLoss / 2) && avgLoss2 < 0) {
				avgLoss2 = nbquest;
			}
			if (lossMean <= (initialAvgLoss / 4) && avgLoss4 < 0) {
				avgLoss4 = nbquest;
			}
			if (lossMean <= (initialAvgLoss / 8) && avgLoss8 < 0) {
				avgLoss8 = nbquest;
			}
		}
		bwStats.write("Mean of Regret reduced by half in " + regret2 + " questions \n");
		bwStats.write("Mean of Regret reduced by four in " + regret4 + " questions \n");
		bwStats.write("Mean of Regret reduced by eight in " + regret8 + " questions \n");
		bwStats.write("Mean of Average Loss reduced by half in " + avgLoss2 + " questions \n");
		bwStats.write("Mean of Average Loss reduced by four in " + avgLoss4 + " questions \n");
		bwStats.write("Mean of Average Loss reduced by eight in " + avgLoss8 + " questions \n");
		bwStats.flush();

		bwStats.write("k \t Mean of Regrets \n");
		bwStats.write("0 \t" + initialRegret + "\n");
		for (int i = 0; i <= maxQuestions - minQuestions; i++) {
			bwStats.write((minQuestions + i) + "\t" + regretSeriesMean.get(i) + "\n");
		}
		bwStats.flush();

		bwStats.write("k \t Mean of Average Losses \n");
		bwStats.write("0 \t" + initialAvgLoss + "\n");
		for (int i = 0; i <= maxQuestions - minQuestions; i++) {
			bwStats.write((minQuestions + i) + "\t" + avgLossSeriesMean.get(i) + "\n");
		}
		bwStats.flush();

		bwStats.write("k \t Standard Deviations of Regrets \n");
		for (int i = 0; i <= maxQuestions - minQuestions; i++) {
			bwStats.write((minQuestions + i) + "\t" + regretSeriesSD.get(i) + "\n");
		}
		bwStats.flush();

		bwStats.write("k \t Standard Deviations of Average Losses \n");
		for (int i = 0; i <= maxQuestions - minQuestions; i++) {
			bwStats.write((minQuestions + i) + "\t" + regretSeriesSD.get(i) + "\n");
		}

		bwStats.write("k \t Percentage of Questions to the Voters \n");
		for (int i = 0; i <= maxQuestions - minQuestions; i++) {
			bwStats.write((minQuestions + i) + "\t" + formatterPerc.format(questionSeriesMean.get(i)) + "\n");
		}

		bwStats.flush();
		bwStats.close();

		bwQst.flush();
		bwQst.close();

	}

	private static void createFiles(StrategyType st, int m, int n, int maxQuestions, int runs) throws IOException {
		bwStats = initFile(root + "m" + m + "n" + n + st + "_stats.txt");
		bwStats.write(st + "\n");
		bwStats.write(n + " Voters " + m + " Alternatives \n");
		bwStats.write(maxQuestions + " Questions for " + runs + " runs \n");
		bwStats.flush();

		bwQst = initFile(root + "m" + m + "n" + n + st + "_questions.txt");
		bwQst.write(st + "\n");
		bwQst.write(n + " Voters " + m + " Alternatives \n");
		bwQst.flush();
	}

	private static Statistic run(int m, int n, StrategyType st, int nbquest) throws IOException {
		bwQst.write(nbquest + " questions:" + "\n");
		final long startTime = System.currentTimeMillis();

		Set<Alternative> alternatives = new HashSet<>();
		for (int i = 1; i <= m; i++) {
			alternatives.add(new Alternative(i));
		}
		Set<Voter> voters = new HashSet<>();
		for (int i = 1; i <= n; i++) {
			voters.add(new Voter(i));
		}
		Oracle context = Oracle.build(ImmutableMap.copyOf(Generator.genProfile(n, m)), Generator.genWeights(m));
		Map<Double, List<Alternative>> trueWinners = computeTrueWinners(context);
		double trueWinScore = trueWinners.keySet().iterator().next();

		PrefKnowledge knowledge = PrefKnowledge.given(alternatives, voters);

		Strategy strategy = null;
		switch (st) {
		case PESSIMISTIC_MAX:
			strategy = StrategyPessimistic.build(knowledge, AggOps.MAX);
			break;
		case PESSIMISTIC_MIN:
			strategy = StrategyPessimistic.build(knowledge, AggOps.MIN);
			break;
		case PESSIMISTIC_AVG:
			strategy = StrategyPessimistic.build(knowledge, AggOps.AVG);
			break;
		case PESSIMISTIC_WEIGHTED_AVG:
			strategy = StrategyPessimistic.build(knowledge, AggOps.WEIGHTED_AVERAGE, 1d,
					context.getWeights().getWeightAtRank(m - 1) / 2 * n);
			break;
		case PESSIMISTIC_HEURISTIC:
			strategy = StrategyPessimisticHeuristic.build(knowledge, AggOps.MAX);
			break;
		case RANDOM:
			strategy = StrategyRandom.build(knowledge);
			break;
		case TWO_PHASES:
			strategy = StrategyTwoPhases.build(knowledge, AggOps.WEIGHTED_AVERAGE, 1d,
					context.getWeights().getWeightAtRank(m - 1) / 2 * n);
			break;
		case TWO_PHASES_TAU:
			strategy = StrategyTwoPhasesTau.build(knowledge, nbCommitteeQuestions, nbVotersQuestions, committeeFirst);
			break;
		case TWO_PHASES_RANDOM:
			strategy = StrategyTwoPhasesRandom.build(knowledge, nbCommitteeQuestions, nbVotersQuestions,
					committeeFirst);
			break;
		case TAU:
			strategy = StrategyTaus.build(knowledge);
			break;
//		case MINIMAX_MIN_INC:
//			strategy = StrategyMiniMaxIncr.build(knowledge);
//			break;
		default:
			throw new IllegalStateException();
		}

		double qstVot = 0, qstCom = 0;

		for (int k = 1; k <= nbquest; k++) {
			Question q;
			try {
				q = strategy.nextQuestion();
				bwQst.write(q.toString() + "\n");
				if (q.getType() == QuestionType.COMMITTEE_QUESTION) {
					qstCom++;
				} else {
					qstVot++;
				}
				Answer a = context.getAnswer(q);
				knowledge = updateKnowledge(knowledge, q, a);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("ERROR " + knowledge.toString());
				break;
			}
		}
		bwQst.write("Questions to the voters: " + qstVot + " Question to the committee: " + qstCom + "\n");
		bwQst.flush();

		// compute the regret after nbquest questions
		RegretComputer rc = new RegretComputer(knowledge);
		SetMultimap<Alternative, PairwiseMaxRegret> mmrs = rc.getMinimalMaxRegrets();
		Set<Alternative> winners = mmrs.keySet();
		double regret = mmrs.get(mmrs.keySet().iterator().next()).iterator().next().getPmrValue();

		// compute the avgloss of the winners
		List<Double> losses = new LinkedList<>();
		for (Alternative alt : winners) {
			double approxTrueScore = 0;
			for (VoterStrictPreference vsp : context.getProfile().values()) {
				int rank = vsp.asStrictPreference().getAlternativeRank(alt);
				approxTrueScore += context.getWeights().getWeightAtRank(rank);
			}
			losses.add(trueWinScore - approxTrueScore);
		}
		double avgloss = 0;
		for (double loss : losses) {
			avgloss += loss;
		}
		avgloss = avgloss / losses.size();
		// compute the percentage of question to voters
		double perc = qstVot / (qstVot + qstCom);

		final long endTime = System.currentTimeMillis();

		return Statistic.build(endTime - startTime, regret, avgloss, perc);
	}

	private static BufferedWriter initFile(String tfile) {
		BufferedWriter b = null;
		try {
			File file = new File(tfile);
			if (file.exists()) {
				file.delete();
			}
			file.createNewFile();
			b = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return b;
	}

	private static PrefKnowledge updateKnowledge(PrefKnowledge knowledge, Question qt, Answer answ) {
		switch (qt.getType()) {
		case VOTER_QUESTION:
			QuestionVoter qv = qt.getQuestionVoter();
			Alternative a = qv.getFirstAlternative();
			Alternative b = qv.getSecondAlternative();
			MutableGraph<Alternative> graph = knowledge.getProfile().get(qv.getVoter()).asGraph();
			switch (answ) {
			case GREATER:
				graph.putEdge(a, b);
				knowledge.getProfile().get(qv.getVoter()).setGraphChanged();
				break;
			case LOWER:
				graph.putEdge(b, a);
				knowledge.getProfile().get(qv.getVoter()).setGraphChanged();
				break;
			// $CASES-OMITTED$
			default:
				throw new IllegalStateException();
			}
			break;
		case COMMITTEE_QUESTION:
			QuestionCommittee qc = qt.getQuestionCommittee();
			Aprational lambda = qc.getLambda();
			int rank = qc.getRank();
			switch (answ) {
			case EQUAL:
				knowledge.addConstraint(rank, ComparisonOperator.EQ, lambda);
				break;
			case GREATER:
				knowledge.addConstraint(rank, ComparisonOperator.GE, lambda);
				break;
			case LOWER:
				knowledge.addConstraint(rank, ComparisonOperator.LE, lambda);
				break;
			default:
				throw new IllegalStateException();
			}
			break;
		default:
			throw new IllegalStateException();
		}
		return knowledge;
	}

	private static Map<Double, List<Alternative>> computeTrueWinners(Oracle context) {
		int m = context.getAlternatives().size();
		double[] sumOfRanks = new double[m];
		List<Alternative> trueWin = new LinkedList<>();
		for (int i = 0; i < m; i++) {
			Alternative a = new Alternative(i + 1);
			double sum = 0;
			for (Voter v : context.getProfile().keySet()) {
				int rank = context.getProfile().get(v).asStrictPreference().getAlternativeRank(a);
				sum += context.getWeights().getWeightAtRank(rank);
			}
			sumOfRanks[i] = sum;
		}
		double max = sumOfRanks[0];
		trueWin.add(new Alternative(1));
		for (int i = 1; i < sumOfRanks.length; i++) {
			if (sumOfRanks[i] == max) {
				trueWin.add(new Alternative(i + 1));
			}
			if (sumOfRanks[i] > max) {
				trueWin.clear();
				max = sumOfRanks[i];
				trueWin.add(new Alternative(i + 1));
			}
		}
		Map<Double, List<Alternative>> tw = new HashMap<>();
		tw.put(max, trueWin);
		return tw;
	}

}
