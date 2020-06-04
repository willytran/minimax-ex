package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.Stats;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.PreferenceInformation;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.strategies.Strategy;
import io.github.oliviercailloux.minimax.utils.Generator;

public class Runner {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

	public static final NumberFormat FORMATTER = getFormatter();

	private static NumberFormat getFormatter() {
		final NumberFormat formatter = NumberFormat.getNumberInstance(Locale.ENGLISH);
		formatter.setMinimumFractionDigits(1);
		formatter.setMaximumFractionDigits(1);
		return formatter;
	}

	static Oracle overridingOracle = null;

	/**
	 * Creates a new random oracle; returns a single run of asking k questions with
	 * the given strategy.
	 *
	 * @param strategy should be freshly instanciated
	 * @throws IOException
	 */
	public static Run run(Strategy strategy, int m, int n, int k) throws IOException {
		final Oracle oracle;
		if (overridingOracle == null) {
			oracle = Oracle.build(Generator.genProfile(m, n), Generator.genWeights(m));
		} else {
			oracle = overridingOracle;
		}

		final PrefKnowledge knowledge = PrefKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
		strategy.setKnowledge(knowledge);

		final ImmutableList.Builder<Question> qBuilder = ImmutableList.builder();
		final ImmutableList.Builder<Long> tBuilder = ImmutableList.builder();

		try {
			for (int i = 1; i <= k; i++) {
				final long startTime = System.currentTimeMillis();
				final Question q = strategy.nextQuestion();
				final PreferenceInformation a = oracle.getPreferenceInformation(q);
				knowledge.update(a);
				LOGGER.debug("Asked {}.", q);
				qBuilder.add(q);
				tBuilder.add(startTime);
			}
		} catch (Exception e) {
			Files.writeString(Path.of("oracle-crashed.json"), JsonConverter.toJson(oracle).toString());
			throw e;
		}
		final long endTime = System.currentTimeMillis();

		return Run.of(oracle, tBuilder.build(), qBuilder.build(), endTime);
	}

	public static void show(Run run) {
		for (int i = 0; i < run.getK(); ++i) {
			LOGGER.info("Regret after {} questions: {}.", i,
					run.getMinimalMaxRegrets().get(i).getMinimalMaxRegretValue());
			LOGGER.info("Question {}: {}.", i, run.getQuestions().get(i));
		}
		LOGGER.info("Regret after {} questions: {}.", run.getK(),
				run.getMinimalMaxRegrets().get(run.getK()).getMinimalMaxRegretValue());
	}

	public static void summarize(Run run) {
		final ImmutableMap<Integer, Double> everyFive = IntStream.rangeClosed(0, run.getK()).filter(i -> i % 5 == 0)
				.boxed().collect(ImmutableMap.toImmutableMap(i -> i,
						i -> run.getMinimalMaxRegrets().get(i).getMinimalMaxRegretValue()));
		LOGGER.info("Regrets: {}.", everyFive);
	}

	public static void summarize(Runs runs) {
		final ImmutableMap<Integer, String> everyFive = IntStream.rangeClosed(0, runs.getK()).filter(i -> i % 5 == 0)
				.boxed()
				.collect(ImmutableMap.toImmutableMap(i -> i, i -> asString(runs.getMinimalMaxRegretStats().get(i))));
		LOGGER.info("Regrets: {}.", everyFive);
	}

	public static String asString(Stats stats) {
		return "[" + FORMATTER.format(stats.min()) + "; " + FORMATTER.format(stats.mean()) + "; "
				+ FORMATTER.format(stats.max()) + "]±" + FORMATTER.format(stats.populationStandardDeviation());
	}

}
