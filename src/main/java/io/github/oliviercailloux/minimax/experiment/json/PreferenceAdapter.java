package io.github.oliviercailloux.minimax.experiment.json;

import javax.json.JsonObject;
import javax.json.bind.adapter.JsonbAdapter;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;

public class PreferenceAdapter implements JsonbAdapter<VoterStrictPreference, JsonObject> {
	@Override
	public JsonObject adaptToJson(VoterStrictPreference obj) {
		return JsonConverter.toJson(obj);
	}

	@Override
	public VoterStrictPreference adaptFromJson(JsonObject obj) {
		throw new UnsupportedOperationException();
	}
}
