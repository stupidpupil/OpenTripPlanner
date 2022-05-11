/* This file is based on code copied from project OneBusAway, see the LICENSE file for further information. */
package org.opentripplanner.model;

public final class Pathway extends TransitEntity {

  private static final long serialVersionUID = -2404871423254094109L;

  private int pathwayMode;

  private StationElement fromStop;

  private StationElement toStop;

  private String name;

  private String reversedName;

  private int traversalTime;

  private double length;

  private int stairCount;

  private double slope;

  private boolean isBidirectional;

  public Pathway(FeedScopedId id) {
    super(id);
  }

  public int getPathwayMode() {
    return pathwayMode;
  }

  public void setPathwayMode(int pathwayMode) {
    this.pathwayMode = pathwayMode;
  }

  public StationElement getFromStop() {
    return fromStop;
  }

  public void setFromStop(StationElement fromStop) {
    this.fromStop = fromStop;
  }

  public StationElement getToStop() {
    return toStop;
  }

  public void setToStop(StationElement toStop) {
    this.toStop = toStop;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getReversedName() {
    return reversedName;
  }

  public void setReversedName(String reversedName) {
    this.reversedName = reversedName;
  }

  public int getTraversalTime() {
    return traversalTime;
  }

  public void setTraversalTime(int traversalTime) {
    this.traversalTime = traversalTime;
  }

  public double getLength() {
    return length;
  }

  public void setLength(double length) {
    this.length = length;
  }

  public boolean isBidirectional() {
    return isBidirectional;
  }

  public void setBidirectional(boolean bidirectional) {
    isBidirectional = bidirectional;
  }

  public int getStairCount() {
    return stairCount;
  }

  public void setStairCount(int stairCount) {
    this.stairCount = stairCount;
  }

  public double getSlope() {
    return slope;
  }

  public void setSlope(double slope) {
    this.slope = slope;
  }

  @Override
  public String toString() {
    return "<Pathway " + getId() + ">";
  }

  public boolean isPathwayModeWheelchairAccessible() {
    return getPathwayMode() != 2 && getPathwayMode() != 4;
  }
}
