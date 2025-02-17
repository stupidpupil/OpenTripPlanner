package org.opentripplanner.model.plan;

/**
 * Represents a turn direction, relative to the current heading.
 * <p>
 * CIRCLE_CLOCKWISE and CIRCLE_CLOCKWISE are used to represent traffic circles.
 */
public enum RelativeDirection {
  DEPART,
  HARD_LEFT,
  LEFT,
  SLIGHTLY_LEFT,
  CONTINUE,
  SLIGHTLY_RIGHT,
  RIGHT,
  HARD_RIGHT,
  CIRCLE_CLOCKWISE,
  CIRCLE_COUNTERCLOCKWISE,
  ELEVATOR,
  UTURN_LEFT,
  UTURN_RIGHT,
}
