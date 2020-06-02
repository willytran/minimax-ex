package io.github.oliviercailloux.minimax.experiment.json;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.bind.adapter.JsonbAdapter;

import com.google.common.collect.ImmutableList;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.y2018.j_voting.Alternative;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class PreferenceAdapter implements JsonbAdapter<VoterStrictPreference, JsonObject> {
	@Override
	public JsonObject adaptToJson(VoterStrictPreference obj) {
		return JsonConverter.toJson(obj);
	}

	@Override
	public VoterStrictPreference adaptFromJson(JsonObject obj) {
		final JsonArray preference = obj.getJsonArray("preference");
		final ImmutableList<Alternative> alternatives = preference.stream().map(v -> ((JsonNumber) v).intValue())
				.map(Alternative::new).collect(ImmutableList.toImmutableList());
		return VoterStrictPreference.given(new Voter(obj.getInt("voter")), alternatives);
	}
}
