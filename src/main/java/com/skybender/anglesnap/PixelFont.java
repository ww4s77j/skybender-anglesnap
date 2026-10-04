package com.skybender.anglesnap;

/**
 * Five by seven pixel font used by the dial.
 *
 * <p>Panels draw their labels as filled rectangles rather than text, because the 26.x HUD draw
 * context has no string method, and because the blocky look matches the reference monitor. Glyphs
 * are stored as one five-bit row per scanline, most significant bit leftmost.
 *
 * <p>Pure model: no Minecraft types, so it is unit tested directly.
 */
public final class PixelFont {
	public static final int WIDTH = 5;
	public static final int HEIGHT = 7;
	/** Highest value a row may hold. */
	public static final int ROW_MASK = 0x1F;

	private PixelFont() {
	}

	/**
	 * Rows for a character, or {@code null} when the font has no glyph for it.
	 *
	 * @param character any character; case is ignored
	 * @return {@link #HEIGHT} rows, or {@code null}
	 */
	public static int[] rows(char character) {
		return switch (Character.toUpperCase(character)) {
			case '0' -> new int[] {14, 17, 19, 21, 25, 17, 14};
			case '1' -> new int[] {4, 12, 4, 4, 4, 4, 14};
			case '2' -> new int[] {14, 17, 1, 6, 8, 16, 31};
			case '3' -> new int[] {31, 2, 4, 2, 1, 17, 14};
			case '4' -> new int[] {2, 6, 10, 18, 31, 2, 2};
			case '5' -> new int[] {31, 16, 30, 1, 1, 17, 14};
			case '6' -> new int[] {6, 8, 16, 30, 17, 17, 14};
			case '7' -> new int[] {31, 1, 2, 4, 8, 8, 8};
			case '8' -> new int[] {14, 17, 17, 14, 17, 17, 14};
			case '9' -> new int[] {14, 17, 17, 15, 1, 2, 12};
			case 'A' -> new int[] {14, 17, 17, 31, 17, 17, 17};
			case 'B' -> new int[] {30, 17, 17, 30, 17, 17, 30};
			case 'C' -> new int[] {14, 17, 16, 16, 16, 17, 14};
			case 'D' -> new int[] {30, 17, 17, 17, 17, 17, 30};
			case 'E' -> new int[] {31, 16, 16, 30, 16, 16, 31};
			case 'F' -> new int[] {31, 16, 16, 30, 16, 16, 16};
			case 'I' -> new int[] {14, 4, 4, 4, 4, 4, 14};
			case 'M' -> new int[] {17, 27, 21, 21, 17, 17, 17};
			case 'P' -> new int[] {30, 17, 17, 30, 16, 16, 16};
			case 'R' -> new int[] {30, 17, 17, 30, 20, 18, 17};
			case 'S' -> new int[] {15, 16, 16, 14, 1, 1, 30};
			case 'T' -> new int[] {31, 4, 4, 4, 4, 4, 4};
			case 'U' -> new int[] {17, 17, 17, 17, 17, 17, 14};
			default -> null;
		};
	}

	/** True when a glyph exists for the character. */
	public static boolean supports(char character) {
		return rows(character) != null;
	}

	/** Width in font cells of a string, or zero when any character is unsupported. */
	public static int textWidth(String text) {
		if (text.isEmpty()) {
			return 0;
		}
		for (int i = 0; i < text.length(); i++) {
			if (!supports(text.charAt(i))) {
				return 0;
			}
		}
		return text.length() * WIDTH + (text.length() - 1);
	}
}
