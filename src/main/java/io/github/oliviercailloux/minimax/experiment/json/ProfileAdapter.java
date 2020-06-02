package io.github.oliviercailloux.minimax.experiment.json;

import java.util.Map;

import javax.json.JsonArray;
import javax.json.bind.adapter.JsonbAdapter;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class ProfileAdapter implements JsonbAdapter<Map<Voter, VoterStrictPreference>, JsonArray> {
	@Override
	public JsonArray adaptToJson(Map<Voter, VoterStrictPreference> obj) {
		return JsonConverter.profileToJson(obj).asJsonArray();
	}

	@Override
	public Map<Voter, VoterStrictPreference> adaptFromJson(JsonArray obj) {
		throw new UnsupportedOperationException();
	}
}
