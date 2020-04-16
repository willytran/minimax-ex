package io.github.oliviercailloux.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;

import io.github.oliviercailloux.minimax.Strategy;
import io.github.oliviercailloux.minimax.StrategyMiniMaxIncr;
import io.github.oliviercailloux.minimax.StrategyPessimistic;
import io.github.oliviercailloux.minimax.StrategyPessimisticHeuristic;
import io.github.oliviercailloux.minimax.StrategyRandom;
import io.github.oliviercailloux.minimax.StrategyTwoPhasesHeuristic;
import io.github.oliviercailloux.minimax.StrategyTwoPhasesRandom;
import io.github.oliviercailloux.minimax.StrategyType;
import io.github.oliviercailloux.minimax.elicitation.Answer;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.old_strategies.StrategyTaus;
import io.github.oliviercailloux.minimax.old_strategies.StrategyTwoPhases;
import io.github.oliviercailloux.minimax.old_strategies.StrategyTwoPhasesTau;
import io.github.oliviercailloux.minimax.utils.Generator;
import io.github.oliviercailloux.minimax.utils.AggregationOperator.AggOps;

public class Runner {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

	private static final String root = Paths.get("").toAbsolutePath() + "/experiments/";

	public static void main(String[] args) throws IOException {
		final boolean committeeFirst = true;
		final int k = 50; // nbQuestions
		final int nbRuns = 2;
		final int m = 5; // alternatives
		final int n = 5; // agents
		StrategyType st;

//		st = StrategyType.RANDOM;
//		runXP(committeeFirst, k, nbRuns, m, n, st);
		
//		st = StrategyType.PESSIMISTIC_HEURISTIC;
//		runXP(committeeFirst, k, nbRuns, m, n, st);
		
		st = StrategyType.TWO_PHASES_HEURISTIC;
		runXP(committeeFirst, k, nbRuns, m, n, st);
		
	}

	private static void runXP(boolean committeeFirst, int k, int nbRuns, int m, int n, StrategyType st)
			throws IOException {
		final ImmutableMap.Builder<String, ImmutableList<Double>> builder = ImmutableMap.builder();
		String title = root + "m" + m + "n" + n + st + "_" + k;
		int i;
		if (st.equals(StrategyType.TWO_PHASES_HEURISTIC) || st.equals(StrategyType.TWO_PHASES)
				|| st.equals(StrategyType.TWO_PHASES_RANDOM))
			i = 0;
		else
			i = k;
		for (; i <= k; i += 10) {
			final StringBuilder sb = new StringBuilder();
			final Strategy strategy = buildStrategy(st, i, k - i, committeeFirst);
			final Runs runs = runRepeatedly(strategy, k, m, n, nbRuns, title);
			final ImmutableList<Double> regrets = runs.getAverageMinimalMaxRegrets();
			if (st.equals(StrategyType.TWO_PHASES_HEURISTIC) || st.equals(StrategyType.TWO_PHASES)
					|| st.equals(StrategyType.TWO_PHASES_RANDOM)) {
				if(committeeFirst) {
					sb.append("qC = " + (k - i) + " then qV = " + i);
				}else {
					sb.append("qV = " + i + " then qC = " + (k - i));
				}
			} else
				sb.append("nbQst = " + k);
			builder.put(sb.toString(), regrets);
		}
		final ImmutableMap<String, ImmutableList<Double>> results = builder.build();

		final CsvWriter writer = new CsvWriter(Files.newOutputStream(Path.of(title + "_out.csv")),
				new CsvWriterSettings());
		final ImmutableList<String> headers = Stream.concat(Stream.of("i"), results.keySet().stream())
				.collect(ImmutableList.toImmutableList());
		writer.writeHeaders(headers);

		for (int j = 0; j < k + 1; ++j) {
			writer.addValue(j);
			for (int col = 0; col < results.size(); ++col) {
				double avg;
				try {
					avg = results.values().asList().get(col).get(j);
				} catch (Exception e) {
					avg = -1;
				}
				writer.addValue(avg);
			}
			writer.writeValuesToRow();
		}
		writer.close();
	}

	
	private static Strategy buildStrategy(StrategyType st, int nbVotersQuestions, int nbCommitteeQuestions,
			boolean committeeFirst) {
		final Strategy strategy;
		switch (st) {
		case PESSIMISTIC_MAX:
			strategy = StrategyPessimistic.build(AggOps.MAX);
			break;
		case PESSIMISTIC_MIN:
			strategy = StrategyPessimistic.build(AggOps.MIN);
			break;
		case PESSIMISTIC_AVG:
			strategy = StrategyPessimistic.build(AggOps.AVG);
			break;
		case PESSIMISTIC_WEIGHTED_AVG:
			/**
			 * TODO the second weight was: context.getWeights().getWeightAtRank(m - 1) / 2 *
			 * n. This is not permitted, as it makes the strategy depend on the oracle.
			 */
			strategy = StrategyPessimistic.build(AggOps.WEIGHTED_AVERAGE, 1d, 0.5d);
			break;
		case PESSIMISTIC_HEURISTIC:
			strategy = StrategyPessimisticHeuristic.build(AggOps.MAX);
			break;
		case RANDOM:
			strategy = StrategyRandom.build();
			break;
		case TWO_PHASES:
			/** See above about the second weight. */
			strategy = StrategyTwoPhases.build(AggOps.WEIGHTED_AVERAGE, 1d, 0.5d);
			break;
		case TWO_PHASES_TAU:
			strategy = StrategyTwoPhasesTau.build(nbCommitteeQuestions, nbVotersQuestions, committeeFirst);
			break;
		case TWO_PHASES_RANDOM:
			strategy = StrategyTwoPhasesRandom.build(nbCommitteeQuestions, nbVotersQuestions, committeeFirst);
			break;
		case TWO_PHASES_HEURISTIC:
			strategy = StrategyTwoPhasesHeuristic.build(nbVotersQuestions, nbCommitteeQuestions, committeeFirst);
			break;
		case TAU:
			strategy = StrategyTaus.build();
			break;
		case MINIMAX_MIN_INC:
			strategy = StrategyMiniMaxIncr.build(nbVotersQuestions, nbCommitteeQuestions, committeeFirst);
			break;
		default:
			throw new IllegalStateException();
		}
		return strategy;
	}

	private static Runs runRepeatedly(Strategy strategy, int nbQuestions, int m, int n, int nbRuns, String title)
			throws IOException {
		final ImmutableList.Builder<Run> builder = ImmutableList.builder();
		for (int i = 0; i < nbRuns; ++i) {
			final Run run = run(strategy, m, n, nbQuestions);
			builder.add(run);
		}
		final Runs runs = Runs.of(builder.build());
		printQuestions(runs, title, nbQuestions);
		return runs;
	}

	private static void printQuestions(Runs runs, String title, int nbQuestions) throws IOException {
		int nbRuns = runs.nbRuns();

		final CsvWriter qstWriter = new CsvWriter(Files.newOutputStream(Path.of(title + "_questions.csv")),
				new CsvWriterSettings());
		LinkedList<String> head = new LinkedList<>();
		for (int j = 0; j < nbRuns; j++) {
			head.add("qV(1)/qC(0)");
			head.add("Run " + (j + 1) + " of " + nbRuns);
		}
		final ImmutableList<String> qstHeaders = Stream.concat(Stream.of("i"), head.stream())
				.collect(ImmutableList.toImmutableList());
		qstWriter.writeHeaders(qstHeaders);

		for (int i = 0; i < nbQuestions; i++) {
			qstWriter.addValue(i + 1);
			for (int col = 0; col < nbRuns; col++) {
				Question q;
				try {
					q = runs.getRun(col).getQuestions().get(i);
				} catch (Exception e) {
					q = null;
				}
				if (q == null) {
					qstWriter.addValue(-1);
				} else if (q.getType().equals(QuestionType.COMMITTEE_QUESTION)) {
					qstWriter.addValue(0);
				} else {
					qstWriter.addValue(1);
				}
				qstWriter.addValue(q);
			}
			qstWriter.writeValuesToRow();
		}
		qstWriter.close();
	}

	public static Run run(Strategy strategy, int m, int n, int k) {
		final Oracle oracle = Oracle.build(ImmutableMap.copyOf(Generator.genProfile(m, n)), Generator.genWeights(m));

		final PrefKnowledge knowledge = PrefKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
		strategy.setKnowledge(knowledge);

		final ImmutableList.Builder<Question> qBuilder = ImmutableList.builder();
		final ImmutableList.Builder<Long> tBuilder = ImmutableList.builder();
		for (int i = 1; i <= k; i++) {
			final long startTime = System.currentTimeMillis();

			final Question q;
			try {
				q = strategy.nextQuestion();
//				LOGGER.info("Asked {}.", q);
				final Answer a = oracle.getAnswer(q);
				knowledge.update(q, a);
				qBuilder.add(q);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("ERROR " + knowledge.toString());
			}
			tBuilder.add(startTime);
		}

		final long endTime = System.currentTimeMillis();
		return Run.of(oracle, tBuilder.build(), qBuilder.build(), endTime);
	}

}
