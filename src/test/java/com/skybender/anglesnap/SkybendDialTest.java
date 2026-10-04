package com.skybender.anglesnap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkybendDialTest {
	private static final float EPSILON = 1.0e-4f;

	@Test
	void dialHasEveryDigitPlusTheSwitches() {
		List<DialLayout.Panel> panels = DialLayout.panels(1.0f);
		assertEquals(16 + 3, panels.size());

		for (int digit = 0; digit < 16; digit++) {
			DialLayout.Panel panel = panels.get(digit);
			assertEquals(String.valueOf("0123456789ABCDEF".charAt(digit)), panel.label());
			assertEquals(SkybendEncoder.ENCODER_ANGLES[digit][0], panel.yaw(), EPSILON);
			assertEquals(SkybendEncoder.ENCODER_ANGLES[digit][1], panel.pitch(), EPSILON);
		}

		assertTrue(labels(panels).contains("MASTER"));
		assertTrue(labels(panels).contains("FIRE"));
		assertTrue(labels(panels).contains("UP"));
	}

	@Test
	void digitsRunInYawOrderSoTheDialReadsZeroToF() {
		List<DialLayout.Panel> panels = DialLayout.panels(1.0f);
		for (int digit = 1; digit < 16; digit++) {
			assertTrue(panels.get(digit).yaw() > panels.get(digit - 1).yaw(),
				"digit " + digit + " must sit after digit " + (digit - 1));
		}
	}

	@Test
	void switchesAreSmallerThanDigitsAndFirePointsDown() {
		List<DialLayout.Panel> panels = DialLayout.panels(1.0f);
		float digitSize = panels.get(0).size();
		for (DialLayout.Panel panel : panels) {
			if (panel.label().equals("MASTER") || panel.label().equals("FIRE") || panel.label().equals("UP")) {
				assertEquals(digitSize * DialLayout.SWITCH_SCALE, panel.size(), EPSILON);
			} else {
				assertEquals(digitSize, panel.size(), EPSILON);
			}
		}

		assertEquals(DialLayout.FIRE_PITCH, fire(panels).pitch(), EPSILON);
		assertEquals(DialLayout.UP_PITCH, up(panels).pitch(), EPSILON);
		assertEquals(SkybendEncoder.ACTIVATE_ANGLES[0][0], master(panels).yaw(), EPSILON);
		assertEquals(SkybendEncoder.ACTIVATE_ANGLES[0][1], master(panels).pitch(), EPSILON);
	}

	@Test
	void panelSizeFollowsTheScaleSetting() {
		float base = DialLayout.panels(1.0f).get(0).size();
		assertEquals(base * 2.0f, DialLayout.panels(2.0f).get(0).size(), EPSILON);
		assertEquals(base * DialLayout.MAX_SCALE, DialLayout.panels(99.0f).get(0).size(), EPSILON);
		assertEquals(base * DialLayout.MIN_SCALE, DialLayout.panels(0.0f).get(0).size(), EPSILON);
	}

	@Test
	void radiusAndScaleAreClamped() {
		assertEquals(DialLayout.MIN_RADIUS, DialLayout.clampRadius(0.1f), EPSILON);
		assertEquals(DialLayout.MAX_RADIUS, DialLayout.clampRadius(500.0f), EPSILON);
		assertEquals(DialLayout.MIN_SCALE, DialLayout.clampScale(0.0f), EPSILON);
		assertEquals(DialLayout.MAX_SCALE, DialLayout.clampScale(50.0f), EPSILON);
		assertEquals(DialLayout.MIN_RADIUS, DialLayout.clampRadius(Float.NaN), EPSILON);
	}

	@Test
	void panelStraightAheadProjectsToTheCrosshair() {
		DialProjection.Projected projected = DialProjection.project(90.0f, 20.0f, 90.0f, 20.0f, 70.0f, 800, 600, 3.0f, 0.9f);

		assertTrue(projected.visible());
		assertEquals(400.0f, projected.x(), 0.01f);
		assertEquals(300.0f, projected.y(), 0.01f);
		assertEquals(1.0f, projected.shade(), EPSILON);
		assertTrue(projected.size() > 0.0f);
	}

	@Test
	void verticalSwitchesProjectCorrectlyRegardlessOfTheirYaw() {
		DialProjection.Projected fire = DialProjection.project(0.0f, 90.0f, 0.0f, 45.0f, 70.0f, 800, 600, 3.0f, 0.9f);
		DialProjection.Projected fireAtDifferentYaw = DialProjection.project(90.0f, 90.0f, 0.0f, 45.0f, 70.0f, 800, 600, 3.0f, 0.9f);
		DialProjection.Projected up = DialProjection.project(0.0f, -90.0f, 0.0f, -45.0f, 70.0f, 800, 600, 3.0f, 0.9f);

		assertTrue(fire.visible());
		assertTrue(fireAtDifferentYaw.visible());
		assertEquals(fire.x(), fireAtDifferentYaw.x(), 0.01f);
		assertEquals(fire.y(), fireAtDifferentYaw.y(), 0.01f);
		assertTrue(fire.y() > 300.0f, "looking partway down, Fire must remain below the crosshair");
		assertTrue(up.visible());
		assertTrue(up.y() < 300.0f, "looking partway up, Up must remain above the crosshair");
	}

	@Test
	void closerPanelsAreBiggerAndTurningShiftsThem() {
		DialProjection.Projected near = DialProjection.project(0.0f, 0.0f, 0.0f, 0.0f, 70.0f, 800, 600, 2.0f, 0.9f);
		DialProjection.Projected far = DialProjection.project(0.0f, 0.0f, 0.0f, 0.0f, 70.0f, 800, 600, 4.0f, 0.9f);
		assertEquals(near.size(), far.size() * 2.0f, 0.01f);

		DialProjection.Projected right = DialProjection.project(10.0f, 0.0f, 0.0f, 0.0f, 70.0f, 800, 600, 3.0f, 0.9f);
		assertTrue(right.x() > 400.0f, "a panel to the right must draw right of centre");

		DialProjection.Projected below = DialProjection.project(0.0f, 30.0f, 0.0f, 0.0f, 70.0f, 800, 600, 3.0f, 0.9f);
		assertTrue(below.y() > 300.0f, "a panel below the crosshair must draw lower on screen");
	}

	@Test
	void panelsBehindTheCameraAreCulled() {
		assertFalse(DialProjection.project(180.0f, 0.0f, 0.0f, 0.0f, 70.0f, 800, 600, 3.0f, 0.9f).visible());
		assertFalse(DialProjection.project(90.0f, 0.0f, 0.0f, 0.0f, 70.0f, 800, 600, 3.0f, 0.9f).visible());
	}

	@Test
	void shadeDarkensAwayFromTheCrosshair() {
		assertEquals(1.0f, DialProjection.shade(0.0f, 0.0f), EPSILON);
		assertTrue(DialProjection.shade(5.0f, 0.0f) < 1.0f);
		assertTrue(DialProjection.shade(60.0f, 0.0f) < DialProjection.shade(5.0f, 0.0f));
		assertTrue(DialProjection.shade(90.0f, 0.0f) >= 0.0f);
	}

	@Test
	void inputGaugeRunsRedYellowGreen() {
		int start = DialProjection.inputGaugeColor(0.0f);
		int middle = DialProjection.inputGaugeColor(0.5f);
		int ready = DialProjection.inputGaugeColor(1.0f);

		assertTrue(DialProjection.red(start) > DialProjection.green(start));
		assertTrue(DialProjection.green(middle) > DialProjection.green(start));
		assertTrue(DialProjection.green(ready) > DialProjection.red(ready));
		assertEquals(DialProjection.inputGaugeColor(1.0f), DialProjection.inputGaugeColor(2.0f));
	}

	@Test
	void wrapDegreesHandlesTheSeam() {
		assertEquals(-10.0f, DialProjection.wrapDegrees(350.0f), EPSILON);
		assertEquals(170.0f, DialProjection.wrapDegrees(-190.0f), EPSILON);
		assertEquals(0.0f, DialProjection.wrapDegrees(0.0f), EPSILON);
	}

	@Test
	void inputSlotMatchesTheCannonDigitPeriod() {
		assertEquals(20, SkybenderTiming.INPUT_DIGIT_PERIOD);
	}

	@Test
	void pixelFontCoversEveryLabelOnTheDial() {
		for (DialLayout.Panel panel : DialLayout.panels(1.0f)) {
			assertTrue(PixelFont.textWidth(panel.label()) > 0, "missing glyphs for " + panel.label());
		}

		for (char character = '0'; character <= '9'; character++) {
			assertNotNull(PixelFont.rows(character));
		}
		for (char character = 'A'; character <= 'F'; character++) {
			assertNotNull(PixelFont.rows(character));
		}

		int[] glyph = PixelFont.rows('8');
		assertEquals(PixelFont.HEIGHT, glyph.length);
		for (int row : glyph) {
			assertTrue(row >= 0 && row <= PixelFont.ROW_MASK, "row out of range: " + row);
		}

		assertEquals(PixelFont.WIDTH, PixelFont.textWidth("A"));
		assertEquals(0, PixelFont.textWidth(""));
		assertNull(PixelFont.rows('~'));
		assertEquals(0, PixelFont.textWidth("A~"));
	}

	private static String labels(List<DialLayout.Panel> panels) {
		StringBuilder out = new StringBuilder();
		for (DialLayout.Panel panel : panels) {
			out.append(panel.label());
		}
		return out.toString();
	}

	private static DialLayout.Panel master(List<DialLayout.Panel> panels) {
		return find(panels, "MASTER");
	}

	private static DialLayout.Panel fire(List<DialLayout.Panel> panels) {
		return find(panels, "FIRE");
	}

	private static DialLayout.Panel up(List<DialLayout.Panel> panels) {
		return find(panels, "UP");
	}

	private static DialLayout.Panel find(List<DialLayout.Panel> panels, String label) {
		for (DialLayout.Panel panel : panels) {
			if (panel.label().equals(label)) {
				return panel;
			}
		}
		throw new AssertionError("no panel labelled " + label);
	}
}
