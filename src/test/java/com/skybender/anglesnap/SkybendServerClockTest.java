package com.skybender.anglesnap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkybendServerClockTest {
	@Test
	void advancesAtTheMeasuredRate() {
		SkybendServerClock clock = new SkybendServerClock();
		for (int i = 0; i < 20; i++) {
			clock.advance(0.05, 20.0, false);
		}
		assertEquals(20.0, clock.ticks(), 1.0e-9);
	}

	@Test
	void laggingServerStretchesEachTick() {
		SkybendServerClock clock = new SkybendServerClock();
		for (int i = 0; i < 20; i++) {
			clock.advance(0.05, 10.0, false);
		}
		assertEquals(10.0, clock.ticks(), 1.0e-9);
	}

	@Test
	void stalledServerFreezesTheClock() {
		SkybendServerClock clock = new SkybendServerClock();
		clock.advance(1.0, 20.0, true);
		assertEquals(0.0, clock.ticks(), 1.0e-9);

		clock.advance(0.05, 20.0, false);
		assertEquals(1.0, clock.ticks(), 1.0e-9);
	}

	@Test
	void ignoresNonPositiveAndNonFiniteElapsedTime() {
		SkybendServerClock clock = new SkybendServerClock();
		clock.advance(0.0, 20.0, false);
		clock.advance(-1.0, 20.0, false);
		clock.advance(Double.NaN, 20.0, false);
		clock.advance(Double.POSITIVE_INFINITY, 20.0, false);
		assertEquals(0.0, clock.ticks(), 1.0e-9);
	}

	@Test
	void resetDropsProgress() {
		SkybendServerClock clock = new SkybendServerClock();
		clock.advance(0.05, 20.0, false);
		clock.reset();
		assertEquals(0.0, clock.ticks(), 1.0e-9);
	}
}
