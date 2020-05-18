package io.github.oliviercailloux.minimax;

import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;

/**
 *
 * An instance of a strategy must return a next question deterministically,
 * given a knowledge.
 *
 */
public interface Strategy {

	public void setKnowledge(PrefKnowledge knowledge);

	/**
	 * Returns the next question that this strategy thinks is best asking.
	 *
	 * @return a question.
	 * @throws IllegalStateException if there are less than two alternatives or if
	 *                               there are exactly two alternatives and the
	 *                               profile is complete.
	 */
	public Question nextQuestion();

	public StrategyType getStrategyType();
}
