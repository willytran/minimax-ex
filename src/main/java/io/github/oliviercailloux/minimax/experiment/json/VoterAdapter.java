package io.github.oliviercailloux.minimax.experiment.json;

import javax.json.bind.adapter.JsonbAdapter;

import io.github.oliviercailloux.y2018.j_voting.Voter;

public class VoterAdapter implements JsonbAdapter<Voter, Integer> {
	@Override
	public Integer adaptToJson(Voter obj) {
		return obj.getId();
	}

	@Override
	public Voter adaptFromJson(Integer obj) {
		return new Voter(obj);
	}
}
