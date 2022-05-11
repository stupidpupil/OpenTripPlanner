package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;

/**
 * A trivial heuristic that always returns 0, which is always admissible. For use in testing,
 * troubleshooting, and spatial analysis applications where there is no target.
 */
public class TrivialRemainingWeightHeuristic implements RemainingWeightHeuristic {

  private static final long serialVersionUID = 1L;

  @Override
  public void initialize(RoutingContext rctx) {}

  @Override
  public double estimateRemainingWeight(State s) {
    return 0;
  }
}
