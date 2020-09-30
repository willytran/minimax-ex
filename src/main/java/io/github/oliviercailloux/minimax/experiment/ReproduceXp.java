package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Verify.verify;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;

public class ReproduceXp {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(ReproduceXp.class);

	public static void main(String[] args) throws Exception {
		reproduce();
	}

	public static void reproduce() throws Exception {
		final Path json = Path.of("experiments/Limited, constrained to [], m = 10, n = 20, k = 10, nbRuns = 2.json");
		final Runs runs = JsonConverter.toRuns(Files.readString(json));
		final StrategyFactory factory = runs.getFactory();
		for (Run run : runs.getRuns()) {
			final Oracle oracle = run.getOracle();
			final int k = run.getK();
			LOGGER.info("Re-running with {}, {}.", oracle, k);
			final Run runAgain = Runner.run(factory, oracle, k);
			LOGGER.info("Questions: {}.", runAgain.getQuestions());
			verify(run.getQuestions().equals(runAgain.getQuestions()),
					String.format("Mismatch with %s, %d.", oracle, k));
		}
	}

}
