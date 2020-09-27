package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.QuestioningConstraint;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class StrategyXp {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(StrategyXp.class);

	public static void main(String[] args) throws Exception {
		final int m = 10;
		final int n = 20;
		final int k = 1;
		final long seed = ThreadLocalRandom.current().nextLong();
//		final StrategyFactory factory = StrategyFactory.limited();
		final StrategyFactory factory = StrategyFactory.limited(seed,
				ImmutableList.of(QuestioningConstraint.of(QuestionType.VOTER_QUESTION, Integer.MAX_VALUE)));
//		final StrategyFactory factory = StrategyFactory.byMmrs(seed, MmrLottery.MAX_COMPARATOR);
//		final StrategyFactory factory = StrategyFactory.random();
		runs(factory, m, n, k, 1);
	}

	/**
	 * Repeat (nbRuns times) a run experiment (thus: generate an oracle, ask k
	 * questions).
	 */
	public static void runs(StrategyFactory factory, int m, int n, int k, int nbRuns) throws IOException {
		final Path outDir = Path.of("experiments/");
		Files.createDirectories(outDir);
		final String prefixTemp = factory.getDescription() + ", m = " + m + ", n = " + n + ", k = " + k + ", ongoing";
		final Path tmpJson = outDir.resolve(prefixTemp + ".json");
		final Path tmpCsv = outDir.resolve(prefixTemp + ".csv");

		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		LOGGER.info("Started '{}'.", factory.getDescription());
		for (int i = 0; i < nbRuns; ++i) {
			final Run run = Runner.run(factory, m, n, k);
			LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
			Files.writeString(Path.of("run " + i + ".json"), JsonConverter.toJson(run).toString());
			runsBuilder.add(run);
			final Runs runs = Runs.of(factory, runsBuilder.build());
//			Runner.summarize(runs);
			Files.writeString(tmpJson, JsonConverter.toJson(runs).toString());
			Files.writeString(tmpCsv, ToCsv.toCsv(runs, 1));
		}

		final String prefix = factory.getDescription() + ", m = " + m + ", n = " + n + ", k = " + k + ", nbRuns = "
				+ nbRuns;
		final Path outJson = outDir.resolve(prefix + ".json");
		final Path outCsv = outDir.resolve(prefix + ".csv");
		Files.move(tmpJson, outJson, StandardCopyOption.REPLACE_EXISTING);
		Files.move(tmpCsv, outCsv, StandardCopyOption.REPLACE_EXISTING);
	}
}
