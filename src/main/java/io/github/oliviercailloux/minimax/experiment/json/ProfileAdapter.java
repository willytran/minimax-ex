package io.github.oliviercailloux.minimax.experiment.json;

import java.util.Map;
import java.util.Set;

import javax.json.bind.adapter.JsonbAdapter;

import com.google.common.collect.ImmutableSet;

import io.github.oliviercailloux.j_voting.VoterStrictPreference;
import io.github.oliviercailloux.y2018.j_voting.Voter;

public class ProfileAdapter implements JsonbAdapter<Map<Voter, VoterStrictPreference>, Set<VoterStrictPreference>> {
	@Override
	public Set<VoterStrictPreference> adaptToJson(Map<Voter, VoterStrictPreference> obj) {
		return ImmutableSet.copyOf(obj.values());
	}

	@Override
	public Map<Voter, VoterStrictPreference> adaptFromJson(Set<VoterStrictPreference> obj) {
//		final PreferenceAdapter preferenceAdapter = new PreferenceAdapter();
//		final ImmutableList<VoterStrictPreference> preferences = obj.stream().map(JsonValue::asJsonObject)
//				.map(preferenceAdapter::adaptFromJson).collect(ImmutableList.toImmutableList());
//		return preferences.stream().collect(ImmutableMap.toImmutableMap(VoterStrictPreference::getVoter, p -> p));
		throw new UnsupportedOperationException();
	}
}
