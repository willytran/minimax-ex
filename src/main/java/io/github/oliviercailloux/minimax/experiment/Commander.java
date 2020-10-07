package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.google.common.base.VerifyException;
import com.google.common.math.Stats;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import io.github.oliviercailloux.jaris.exceptions.Unchecker;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.experiment.json.JsonConverter;
import io.github.oliviercailloux.minimax.strategies.StrategyFactory;
import io.github.oliviercailloux.minimax.strategies.StrategyType;

public class Commander {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Commander.class);

	@Parameters
	private static class TimingCommand {
		@Parameter(names = "-m", required = true)
		public int m = 5;
		@Parameter(names = "-n", required = true)
		public int n = 5;
		@Parameter(names = "-k", required = true)
		public int k = 30;
	}

	private static class StrategyTypeConverter implements IStringConverter<StrategyType> {

		@Override
		public StrategyType convert(String value) {
			StrategyType convertedValue = StrategyType.valueOf(value);

			if (convertedValue == null) {
				throw new ParameterException("");
			}
			return convertedValue;
		}
	}

	@Parameters
	private static class StrategyCommand {
		@Parameter(names = "--oracles", required = true)
		public String oracles;
		@Parameter(names = "--family", required = true, converter = StrategyTypeConverter.class)
		public StrategyType family;
		@Parameter(names = "--qC")
		public int qC;
		@Parameter(names = "--qV")
		public int qV;
		@Parameter(names = "-k", required = true)
		public int k;
	}

	public static void main(String[] args) {
		new Commander().proceed(args);
	}

	public void proceed(String[] args) {
		Unchecker.IO_UNCHECKER.call(this::logToUniqueFile);

		try {
			execute(args);
		} catch (Throwable e) {
			LOGGER.error("Fatal", e);
		}
	}

	private void execute(String[] args) throws IOException {
		LOGGER.info("Proceeding with command {}.", Arrays.asList(args));
		final TimingCommand timing = new TimingCommand();
		final StrategyCommand strategy = new StrategyCommand();
		final JCommander jc = JCommander.newBuilder().addCommand("timing", timing).addCommand("strategy", strategy)
				.build();
		jc.parse(args);

		switch (jc.getParsedCommand()) {
		case "timing":
			TimingXp.time(timing.m, timing.n, timing.k);
			break;
		case "strategy":
			strategy(strategy);
			break;
		default:
			break;
		}

	}

	private void strategy(StrategyCommand command) throws IOException {
		if (command.qC != 0 && command.qV != 0) {
			throw new ParameterException("At most one of the qC and qV parameters can be specified.");
		}

		final StrategyFactory factory;
		switch (command.family) {
		case ELITIST:
			factory = StrategyFactory.elitist();
			break;
		case LIMITED:
			if (command.qC != 0) {
				factory = StrategyFactory.limitedCommitteeThenVoters(command.qC);
			} else if (command.qV != 0) {
				factory = StrategyFactory.limitedVotersThenCommittee(command.qV);
			} else {
				factory = StrategyFactory.limited();
			}
			break;
		case RANDOM:
			factory = StrategyFactory.random(0.5d);
			break;
		case TWO_PHASES_HEURISTIC:
		case PESSIMISTIC:
		case PESSIMISTIC_HEURISTIC:
		default:
			throw new VerifyException();
		}

		final Path json = Path.of(command.oracles);
		final List<Oracle> oracles = JsonConverter.toOracles(Files.readString(json));

		final Runs runs = new VariousXps().runs(factory, oracles, command.k);
		final Stats stats = runs.getMinimalMaxRegretStats().get(runs.getK());
		final String descr = Runner.asStringEstimator(stats);
		LOGGER.info("Got final estimator: {}.", descr);
	}

	private void logToUniqueFile() throws IOException {
		final ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
				.getLogger(Logger.ROOT_LOGGER_NAME);

		final FileAppender<ILoggingEvent> constantAppender = (FileAppender<ILoggingEvent>) root.getAppender("File");
		verify(!constantAppender.isAppend());
		final boolean detached = root.detachAppender(constantAppender);
		verify(detached);
		constantAppender.stop();
		final Path constantPath = Path.of(constantAppender.getFile());
		try (FileChannel channel = FileChannel.open(constantPath)) {
			final long size = channel.size();
			verify(size == 0l);
		}
		Files.delete(constantPath);

		final FileAppender<ILoggingEvent> variableAppender = new FileAppender<>();
		variableAppender.setContext(constantAppender.getContext());
		variableAppender.setAppend(false);
		variableAppender.setEncoder(constantAppender.getEncoder());
		variableAppender.setFile(String.format("out-%s.log",
				DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS").format(LocalDateTime.now())));
		root.addAppender(variableAppender);
		variableAppender.start();
	}
}
