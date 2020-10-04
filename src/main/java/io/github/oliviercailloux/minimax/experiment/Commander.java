package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Verify.verify;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import io.github.oliviercailloux.jaris.exceptions.Unchecker;

public class Commander {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Commander.class);

	@Parameters
	private static class TimingCommand {
		@Parameter(names = "-m")
		public int m = 5;
		@Parameter(names = "-n")
		public int n = 5;
		@Parameter(names = "-k")
		public int k = 30;
	}

	public static void main(String[] args) {
		new Commander().proceed(args);
	}

	public void proceed(String[] args) {
		Unchecker.IO_UNCHECKER.call(this::logToUniqueFile);

		try {
			LOGGER.info("Proceeding with command {}.", Arrays.asList(args));
			final TimingCommand timing = new TimingCommand();
			final JCommander jc = JCommander.newBuilder().addCommand("timing", timing).build();
			jc.parse(args);
			verify(jc.getParsedCommand().equals("timing"));

			TimingXp.time(timing.m, timing.n, timing.k);
		} catch (Throwable e) {
			LOGGER.error("Fatal", e);
		}
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
