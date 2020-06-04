package io.github.oliviercailloux.minimax.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.experiment.other_formats.ToCsv;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class XpSmallSize {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(XpSmallSize.class);

	public static void main(String[] args) throws Exception {
		final int m = 5;
		final int n = 5;
		final int k = 30;
//		final StrategyFactory factory = StrategyFactory.limited();
		final StrategyFactory factory = StrategyFactory.limited();
//		final StrategyFactory factory = StrategyFactory.byMmrs(MmrOperator.MAX);
//		final StrategyFactory factory = StrategyFactory.random();
		runs(factory, m, n, k, 5);
	}

	/**
	 * Repeat (nbRuns times) a run experiment (thus: generate an oracle, ask k
	 * questions).
	 */
	private static void runs(StrategyFactory factory, int m, int n, int k, int nbRuns) throws IOException {
		final Path outDir = Path.of("experiments/Small/");
		Files.createDirectories(outDir);
		final String prefixTemp = factory.getDescription() + ", m = " + m + ", n = " + n + ", k = " + k + ", ongoing";
		final Path tmpJson = outDir.resolve(prefixTemp + ".json");
		final Path tmpCsv = outDir.resolve(prefixTemp + ".csv");

		final ImmutableList.Builder<Run> runsBuilder = ImmutableList.builder();
		LOGGER.info("Started {}.", factory.getDescription());
		for (int i = 0; i < nbRuns; ++i) {
			final Run run = Runner.run(factory.get(), m, n, k);
			LOGGER.info("Time (run {}): {}.", i, run.getTotalTime());
			runsBuilder.add(run);
			final Runs runs = Runs.of(runsBuilder.build());
//			Runner.summarize(runs);
			Files.writeString(tmpJson, JsonConverter.toJson(runs).toString());
			Files.writeString(tmpCsv, ToCsv.toCsv(runs));
		}

		final String prefix = factory.getDescription() + ", m = " + m + ", n = " + n + ", k = " + k + ", nbRuns = "
				+ nbRuns;
		final Path outJson = outDir.resolve(prefix + ".json");
		final Path outCsv = outDir.resolve(prefix + ".csv");
		Files.move(tmpJson, outJson, StandardCopyOption.REPLACE_EXISTING);
		Files.move(tmpCsv, outCsv, StandardCopyOption.REPLACE_EXISTING);
	}
}
