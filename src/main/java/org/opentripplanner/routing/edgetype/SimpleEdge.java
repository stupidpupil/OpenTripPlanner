package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Vertex;

/** Appears to be used only in tests. */
public class SimpleEdge extends FreeEdge {

  private static final long serialVersionUID = 1L;
  private final double weight;
  private final int seconds;

  public SimpleEdge(Vertex v1, Vertex v2, double weight, int seconds) {
    super(v1, v2);
    this.weight = weight;
    this.seconds = seconds;
  }

  @Override
  public State traverse(State s0) {
    StateEditor s1 = s0.edit(this);
    s1.incrementTimeInSeconds(seconds);
    s1.incrementWeight(weight);
    // SimpleEdges don't concern themselves with mode
    return s1.makeState();
  }
}
