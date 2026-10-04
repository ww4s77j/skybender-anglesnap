package com.skybender.anglesnap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkybendTpsModelTest {
	/** A vanilla server broadcasts its time packet every 20 of its own ticks. */
	private static final long TICKS_PER_BROADCAST = 20L;

	@Test
	void steadyServerMeasuresTwentyTps() {
		SkybendTpsModel model = new SkybendTpsModel();
		feed(model, 20.0, 40);
		assertEquals(20.0, model.measuredTps(), 1.0e-9);
		assertEquals(20.0, model.tps(), 1.0e-9);
		assertEquals(50.0, model.tickMillis(), 1.0e-9);
	}

	@Test
	void laggingServerMeasuresItsRealRate() {
		SkybendTpsModel model = new SkybendTpsModel();
		feed(model, 10.0, 40);
		assertEquals(10.0, model.measuredTps(), 1.0e-9);
		assertEquals(100.0, model.tickMillis(), 1.0e-9);
	}

	@Test
	void fasterServerIsMeasuredAboveTwenty() {
		SkybendTpsModel model = new SkybendTpsModel();
		feed(model, 40.0, 40);
		assertEquals(40.0, model.measuredTps(), 1.0e-9);
	}

	@Test
	void estimateFallsBackToDefaultDuringWarmup() {
		SkybendTpsModel model = new SkybendTpsModel();
		model.onTimePacket(0L, 0L);
		model.onTimePacket(TICKS_PER_BROADCAST, 100L);
		assertEquals(SkybendTpsModel.DEFAULT_TPS, model.measuredTps(), 1.0e-9);
		assertTrue(model.hasHeardFromServer());
	}

	@Test
	void batchedPacketDoesNotSkewTheWindow() {
		SkybendTpsModel model = new SkybendTpsModel();
		long gameTime = 0L;
		long wall = 0L;
		model.onTimePacket(gameTime, wall);
		for (int i = 0; i < 19; i++) {
			gameTime += TICKS_PER_BROADCAST;
			wall += 1000L;
			model.onTimePacket(gameTime, wall);
		}
		// One packet arrives late and fast, as TCP coalescing produces after a hitch.
		gameTime += TICKS_PER_BROADCAST;
		wall += 200L;
		model.onTimePacket(gameTime, wall);

		assertTrue(Math.abs(model.measuredTps() - 20.0) < 1.0, "tps=" + model.measuredTps());
	}

	@Test
	void duplicateAndOutOfOrderSamplesAreIgnored() {
		SkybendTpsModel model = new SkybendTpsModel();
		feed(model, 20.0, 10);
		int samples = model.sampleCount();

		model.onTimePacket(200L, 10_000L);
		model.onTimePacket(150L, 11_000L);
		assertEquals(samples, model.sampleCount());
	}

	@Test
	void overrideTakesPrecedenceAndIsClamped() {
		SkybendTpsModel model = new SkybendTpsModel();
		feed(model, 20.0, 40);
		assertFalse(model.isOverridden());

		model.setOverride(15.0);
		assertTrue(model.isOverridden());
		assertEquals(15.0, model.tps(), 1.0e-9);
		assertEquals(20.0, model.measuredTps(), 1.0e-9);

		model.setOverride(1000.0);
		assertEquals(SkybendTpsModel.MAX_TPS, model.tps(), 1.0e-9);
		model.setOverride(0.0);
		assertEquals(SkybendTpsModel.MIN_TPS, model.tps(), 1.0e-9);

		model.clearOverride();
		assertFalse(model.isOverridden());
		assertEquals(20.0, model.tps(), 1.0e-9);
	}

	@Test
	void resetClearsSamplesAndKeepsOverride() {
		SkybendTpsModel model = new SkybendTpsModel();
		feed(model, 10.0, 40);
		model.setOverride(17.5);

		model.reset();
		assertEquals(0, model.sampleCount());
		assertFalse(model.hasHeardFromServer());
		assertEquals(-1L, model.millisSinceLastPacket(999L));
		assertTrue(model.isOverridden());
		assertEquals(17.5, model.tps(), 1.0e-9);
		assertEquals(SkybendTpsModel.DEFAULT_TPS, model.measuredTps(), 1.0e-9);
	}

	@Test
	void reportsTimeSinceLastPacket() {
		SkybendTpsModel model = new SkybendTpsModel();
		model.onTimePacket(0L, 5_000L);
		assertEquals(500L, model.millisSinceLastPacket(5_500L));
		assertEquals(0L, model.millisSinceLastPacket(4_000L));
	}

	/** Feeds evenly spaced broadcasts for a server running at the given rate. */
	private static void feed(SkybendTpsModel model, double tps, int broadcasts) {
		long gameTime = 0L;
		long wallMillis = 0L;
		model.onTimePacket(gameTime, wallMillis);
		for (int i = 0; i < broadcasts; i++) {
			gameTime += TICKS_PER_BROADCAST;
			wallMillis += Math.round(TICKS_PER_BROADCAST * 1000.0 / tps);
			model.onTimePacket(gameTime, wallMillis);
		}
	}
}
