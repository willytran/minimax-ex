package io.github.oliviercailloux.minimax.strategies;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.oliviercailloux.j_voting.Generator;
import io.github.oliviercailloux.minimax.elicitation.UpdateablePreferenceKnowledge;
import io.github.oliviercailloux.minimax.experiment.Runner;

public class TimeTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
	
	@Test
	void testStrategyMMR() throws Exception {
		final UpdateablePreferenceKnowledge k = UpdateablePreferenceKnowledge.given(Generator.getAlternatives(10), Generator.getVoters(20));
		final StrategyByMmr s1 = StrategyByMmr.build();
		final StrategyByMmrNew s2 = StrategyByMmrNew.build();
		s1.setKnowledge(k);
		s2.setKnowledge(k);
		LOGGER.info("s1 start");
		s1.nextQuestion();
		LOGGER.info("s1 end");
		LOGGER.info("s2 start");
		s2.nextQuestion();
		LOGGER.info("s2 end");
	}

	
}
