package io.github.oliviercailloux.minimax.experiment.json;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apfloat.Apint;
import org.apfloat.Aprational;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.json.PrintableJsonObject;
import io.github.oliviercailloux.json.PrintableJsonValue;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.experiment.Run;
import io.github.oliviercailloux.minimax.experiment.Runs;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class JsonConverterTests {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonConverterTests.class);
	private static final Alternative a1 = new Alternative(1);
	private static final Alternative a2 = new Alternative(2);
	private static final Alternative a3 = new Alternative(3);
	private static final Voter v1 = new Voter(1);
	private static final Voter v2 = new Voter(2);
	private static final ImmutableList<Alternative> p1 = ImmutableList.of(a1, a2, a3);
	private static final ImmutableList<Alternative> p2 = ImmutableList.of(a3, a2, a1);
	private static final PSRWeights w = PSRWeights.given(ImmutableList.of(1d, 0.4d, 0d));
	private static final ImmutableMap<Voter, VoterStrictPreference> profile = ImmutableMap.of(v1,
			VoterStrictPreference.given(v1, p1), v2, VoterStrictPreference.given(v2, p2));
	private static final Oracle oracle = Oracle.build(profile, w);
	private static final Question q1 = Question.toVoter(v1, a1, a2);
	private static final Question q2 = Question.toCommittee(new Aprational(new Apint(1), new Apint(2)), 1);
	private static final Run run = Run.of(oracle, ImmutableList.of(10l, 18l), ImmutableList.of(q1, q2), 20l);

	@Test
	void testVoter() throws Exception {
		final PrintableJsonObject json = JsonConverter.toJson(v1);
		final String expected = Files.readString(Path.of(getClass().getResource("Voter.json").toURI()));
		assertEquals(expected, json.toString());
	}

	/**
	 * TODO ask Jsonb whether I can annotate VSP so that I don’t need to go through
	 * custom adapter for the whole type.
	 */
	@Test
	void testPreference() throws Exception {
		final PrintableJsonObject json = JsonConverter.toJson(VoterStrictPreference.given(v1, p1));
		final String expected = Files.readString(Path.of(getClass().getResource("Strict preference.json").toURI()));
		assertEquals(expected, json.toString());
	}

	@Test
	void testToPreference() throws Exception {
		final String source = Files.readString(Path.of(getClass().getResource("Strict preference.json").toURI()));
		final VoterStrictPreference actual = JsonConverter.toVoterStrictPreference(source);
		assertEquals(VoterStrictPreference.given(v1, p1), actual);
	}

	@Test
	void testProfile() throws Exception {
		final PrintableJsonValue json = JsonConverter.profileToJson(profile);
		final String expected = Files.readString(Path.of(getClass().getResource("Profile.json").toURI()));
		assertEquals(expected, json.toString());
	}

	@Test
	void testWeights() throws Exception {
		final PrintableJsonValue json = JsonConverter.toJson(w);
		final String expected = Files.readString(Path.of(getClass().getResource("Weights.json").toURI()));
		assertEquals(expected, json.toString());
	}

	@Test
	void testOracle() throws Exception {
		final PrintableJsonObject json = JsonConverter.toJson(oracle);
		final String expected = Files.readString(Path.of(getClass().getResource("Oracle.json").toURI()));
		assertEquals(expected, json.toString());
	}

	@Test
	void testRun() throws Exception {
		final PrintableJsonObject json = JsonConverter.toJson(run);
		final String expected = Files.readString(Path.of(getClass().getResource("Run.json").toURI()));
		assertEquals(expected, json.toString());
	}

	@Test
	void testRuns() throws Exception {
		final PrintableJsonObject json = JsonConverter.toJson(Runs.of(ImmutableList.of(run, run)));
		LOGGER.info("Json: {}.", json);
		final String expected = Files.readString(Path.of(getClass().getResource("Runs.json").toURI()));
		assertEquals(expected, json.toString());
	}
}
