package io.github.oliviercailloux.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvWriter;
import org.junit.jupiter.params.shadow.com.univocity.parsers.csv.CsvWriterSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.minimax.Strategy;
import io.github.oliviercailloux.minimax.StrategyTwoPhasesHeuristic;
import io.github.oliviercailloux.minimax.elicitation.Answer;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.utils.Generator;

public class Runner {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

	public static void main(String[] args) throws IOException {
		final boolean committeeFirst = true;
		final int k = 20;
		final int nbRuns = 1;

		final ImmutableMap.Builder<String, ImmutableList<Double>> builder = ImmutableMap.builder();
		for (int i = 0; i <= k; i += 5) {
			final Runs runs = runRepeatedly(i, k - i, committeeFirst, nbRuns);
			final ImmutableList<Double> regrets = runs.getAverageMinimalMaxRegrets();
			builder.put("qC = " + (k - i) + " then qV = " + i, regrets);
		}
		for (int i = 0; i <= k; i += 5) {
			final Runs runs = runRepeatedly(i, k - i, !committeeFirst, nbRuns);
			final ImmutableList<Double> regrets = runs.getAverageMinimalMaxRegrets();
			builder.put("qV = " + i + " then qC = " + (k - i), regrets);
		}
		final ImmutableMap<String, ImmutableList<Double>> results = builder.build();

//		final StringWriter stringWriter = new StringWriter();
		final CsvWriter writer = new CsvWriter(Files.newOutputStream(Path.of("out.csv")), new CsvWriterSettings());
		final ImmutableList<String> headers = Stream.concat(Stream.of("i"), results.keySet().stream())
				.collect(ImmutableList.toImmutableList());
		writer.writeHeaders(headers);
		for (int i = 0; i < k + 1; ++i) {
			writer.addValue(i);
			for (int col = 0; col < results.size(); ++col) {
				final double avg = results.values().asList().get(col).get(i);
				writer.addValue(avg);
			}
			writer.writeValuesToRow();
		}
		writer.close();
	}

	private static Runs runRepeatedly(int qToVoters, int qToCommittee, boolean committeeFirst, int nbRuns) {
		final ImmutableList.Builder<Run> builder = ImmutableList.builder();
		for (int i = 0; i < nbRuns; ++i) {
			final Strategy strategy = StrategyTwoPhasesHeuristic.build(qToVoters, qToCommittee, committeeFirst);
			final Run run = run(strategy, 7, 7, qToVoters + qToCommittee);
			builder.add(run);
		}
		final Runs runs = Runs.of(builder.build());
		return runs;
	}

	public static Run run(Strategy strategy, int m, int n, int k) {
		final Oracle oracle = Oracle.build(ImmutableMap.copyOf(Generator.genProfile(m, n)), Generator.genWeights(m));

		final PrefKnowledge knowledge = PrefKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
		strategy.setKnowledge(knowledge);

		final ImmutableList.Builder<Question> qBuilder = ImmutableList.builder();
		final ImmutableList.Builder<Long> tBuilder = ImmutableList.builder();
		for (int i = 1; i <= k; i++) {
			final long startTime = System.currentTimeMillis();
			final Question q = strategy.nextQuestion();
			LOGGER.info("Asked {}.", q);
			final Answer a = oracle.getAnswer(q);
			knowledge.update(q, a);
			tBuilder.add(startTime);
			qBuilder.add(q);
		}

		final long endTime = System.currentTimeMillis();

		return Run.of(oracle, tBuilder.build(), qBuilder.build(), endTime);
	}
}
