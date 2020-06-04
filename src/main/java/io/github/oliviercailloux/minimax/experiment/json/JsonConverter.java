package io.github.oliviercailloux.minimax.experiment.json;

import java.util.Map;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.json.JsonbUtils;
import io.github.oliviercailloux.json.PrintableJsonObject;
import io.github.oliviercailloux.json.PrintableJsonValue;
import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PSRWeights;
import io.github.oliviercailloux.minimax.experiment.Run;
import io.github.oliviercailloux.minimax.experiment.Runs;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class JsonConverter {
	public static PrintableJsonObject toJson(Voter voter) {
		return JsonbUtils.toJsonObject(voter);
	}

	public static PrintableJsonObject toJson(VoterStrictPreference preference) {
		return JsonbUtils.toJsonObject(preference, new VoterAdapter(), new AlternativeAdapter());
	}

	public static VoterStrictPreference toVoterStrictPreference(String json) {
		return JsonbUtils.fromJson(json, VoterStrictPreference.class, new VoterAdapter(), new AlternativeAdapter());
	}

	/**
	 * TODO call getDelegate() in JsonObjectGeneralWrapper constructor to fail early
	 * in case of non convertible.
	 */
	public static PrintableJsonValue profileToJson(Map<Voter, VoterStrictPreference> profile) {
		return JsonbUtils.toJsonValue(profile.values(), new PreferenceAdapter());
	}

	public static PrintableJsonValue toJson(PSRWeights weights) {
		return JsonbUtils.toJsonValue(weights.getWeights());
	}

	public static PrintableJsonObject toJson(Oracle oracle) {
		return JsonbUtils.toJsonObject(oracle, new ProfileAdapter(), new PreferenceAdapter());
	}

	public static Oracle toOracle(String json) {
		return JsonbUtils.fromJson(json, Oracle.class, new ProfileAdapter(), new PreferenceAdapter(),
				new WeightsAdapter());
	}

	public static PrintableJsonObject toJson(Run run) {
		return JsonbUtils.toJsonObject(run, new ProfileAdapter(), new PreferenceAdapter(), new QuestionAdapter());
	}

	public static Run toRun(String json) {
		return JsonbUtils.fromJson(json, Run.class, new ProfileAdapter(), new PreferenceAdapter(),
				new QuestionAdapter(), new WeightsAdapter());
	}

	public static PrintableJsonObject toJson(Runs run) {
		return JsonbUtils.toJsonObject(run, new ProfileAdapter(), new PreferenceAdapter(), new QuestionAdapter());
	}

	public static Runs toRuns(String json) {
		return JsonbUtils.fromJson(json, Runs.class, new ProfileAdapter(), new PreferenceAdapter(),
				new QuestionAdapter(), new WeightsAdapter(), new FactoryAdapter());
	}
}
