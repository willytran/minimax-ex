package io.github.oliviercailloux.minimax.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The Rounding class provides a method to round a double value, given the
 * rounding mode and the scale. It uses the BigDecimal rounding methods. If the
 * rounding mode is NULL then the value is not rounded.
 */

public class Rounder {

	public static enum Mode {
		ROUND_CEILING, ROUND_DOWN, ROUND_FLOOR, ROUND_HALF_DOWN, ROUND_HALF_EVEN, ROUND_HALF_UP, ROUND_UP, NULL
	}

	private RoundingMode mode;
	private int granularity;

	public static Rounder given(RoundingMode roundingMode, int decimalPlaces) {
		return new Rounder(checkNotNull(roundingMode), decimalPlaces);
	}

	public static Rounder noRounding() {
		return new Rounder(null, 0);
	}

	private Rounder(RoundingMode roundingMode, int decimalPlaces) {
		mode = roundingMode;
		granularity = decimalPlaces;
	}

	public double round(double value) {
		if (mode == null) {
			return value;
		}
		return BigDecimal.valueOf(value).setScale(granularity, mode).doubleValue();
	}

}
