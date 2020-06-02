package io.github.oliviercailloux.minimax.experiment.json;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.adapter.JsonbAdapter;

import com.google.common.base.VerifyException;

import io.github.oliviercailloux.json.JsonbUtils;
import io.github.oliviercailloux.minimax.elicitation.Question;

public class QuestionAdapter implements JsonbAdapter<Question, JsonObject> {
	@Override
	public JsonObject adaptToJson(Question obj) {
		final JsonObjectBuilder builder = Json.createObjectBuilder();
		switch (obj.getType()) {
		case COMMITTEE_QUESTION:
			builder.add("toCommittee", JsonbUtils.toJsonObject(obj.asQuestionCommittee()));
			break;
		case VOTER_QUESTION:
			builder.add("toVoter", JsonbUtils.toJsonObject(obj.asQuestionVoter(), new AlternativeAdapter()));
			break;
		default:
			throw new VerifyException();
		}
		return builder.build();
	}

	@Override
	public Question adaptFromJson(JsonObject obj) {
		throw new UnsupportedOperationException();
	}
}
