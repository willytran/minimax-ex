package io.github.oliviercailloux.minimax.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Verify.verify;

import java.time.Duration;
import java.util.List;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;
import javax.json.bind.annotation.JsonbTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import io.github.oliviercailloux.minimax.elicitation.Oracle;
import io.github.oliviercailloux.minimax.elicitation.PrefKnowledge;
import io.github.oliviercailloux.minimax.elicitation.Question;
import io.github.oliviercailloux.minimax.elicitation.QuestionType;
import io.github.oliviercailloux.minimax.regret.RegretComputer;
import io.github.oliviercailloux.minimax.regret.Regrets;

@JsonbPropertyOrder({ "oracle", "questions", "times" })
public class Run {
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LoggerFactory.getLogger(Run.class);

	public static Run of(Oracle oracle, List<Long> startTimes, List<Question> questions, long endTime) {
		return new Run(oracle, startTimes, questions, endTime);
	}

	private final Oracle oracle;
	@JsonbTransient
	private final ImmutableList<Long> startTimes;
	private final ImmutableList<Question> questions;
	@JsonbTransient
	private final long endTime;
	@JsonbTransient
	private ImmutableList<Regrets> regrets;

	private Run(Oracle oracle, List<Long> startTimes, List<Question> questions, long endTime) {
		checkArgument(!startTimes.isEmpty());
		checkArgument(startTimes.size() == questions.size());
		checkArgument(questions.size() >= 1);
		this.oracle = oracle;
		this.startTimes = ImmutableList.copyOf(startTimes);
		this.questions = ImmutableList.copyOf(questions);
		this.endTime = endTime;
		this.regrets = null;
		verify((getNbQVoters() + getNbQCommittee()) == questions.size());
		getQuestionTimesMs();
		getTotalTimeMs();
	}

	public Oracle getOracle() {
		return oracle;
	}

	@JsonbTransient
	public int getK() {
		return questions.size();
	}

	/**
	 * @return a list of size k.
	 */
	public ImmutableList<Question> getQuestions() {
		return questions;
	}

	/**
	 * @return a list of size k.
	 */
	@JsonbProperty("timesMs")
	public ImmutableList<Integer> getQuestionTimesMs() {
		final ImmutableList.Builder<Integer> builder = ImmutableList.builder();

		final PeekingIterator<Long> peekingIterator = Iterators.peekingIterator(startTimes.iterator());
		while (peekingIterator.hasNext()) {
			final long start = peekingIterator.next();
			final long end;
			if (peekingIterator.hasNext()) {
				end = peekingIterator.peek();
			} else {
				end = endTime;
			}
			final long diff = end - start;
			/** Using check argument because this is called from the constructor. */
			checkArgument(diff >= 0l);
			final int diffInt = Math.toIntExact(diff);
			builder.add(diffInt);
		}

		final ImmutableList<Integer> times = builder.build();
		verify(times.stream().mapToInt(Integer::intValue).sum() == getTotalTimeMs());
		return times;
	}

	@JsonbTransient
	public int getNbQVoters() {
		return (int) questions.stream().filter((q) -> q.getType().equals(QuestionType.VOTER_QUESTION)).count();
	}

	@JsonbTransient
	public int getNbQCommittee() {
		return (int) questions.stream().filter((q) -> q.getType().equals(QuestionType.COMMITTEE_QUESTION)).count();
	}

	@JsonbTransient
	public double getPropQVoters() {
		return ((double) getNbQVoters()) / (double) (getNbQVoters() + getNbQCommittee());
	}

	@JsonbTransient
	public int getTotalTimeMs() {
		/**
		 * An int can store a time of 20 days, this should be enough for one run.
		 */
		return Math.toIntExact(endTime - startTimes.get(0));
	}

	@JsonbTransient
	public Duration getTotalTime() {
		return Duration.ofMillis(getTotalTimeMs());
	}

	/**
	 * @return a list of size k + 1.
	 */
	@JsonbTransient
	public ImmutableList<Regrets> getMinimalMaxRegrets() {
		if (regrets == null) {
			final PrefKnowledge knowledge = PrefKnowledge.given(oracle.getAlternatives(), oracle.getProfile().keySet());
			final RegretComputer rc = new RegretComputer(knowledge);

			final ImmutableList.Builder<Regrets> builder = ImmutableList.builderWithExpectedSize(questions.size() + 1);
			builder.add(rc.getMinimalMaxRegrets());

			for (Question question : questions) {
				knowledge.update(oracle.getPreferenceInformation(question));
				builder.add(rc.getMinimalMaxRegrets());
			}

			regrets = builder.build();
		}
		return regrets;
	}
}
