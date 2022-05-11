package org.opentripplanner.inspector;

import java.awt.Color;

/**
 * Convert a scalar value (bounded or unbounded) to a color.
 *
 * @author laurent
 */
public interface ScalarColorPalette {
  Color getColor(double value);
}
