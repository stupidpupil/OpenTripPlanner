package org.opentripplanner.routing.algorithm;

import static org.mockito.Mockito.mock;

import java.time.Instant;
import junit.framework.TestCase;
import org.junit.Assert;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateData;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public class TestDominanceFunction extends TestCase {

  public void testGeneralDominanceFunction() {
    DominanceFunction minimumWeightDominanceFunction = new DominanceFunction.MinimumWeight();
    Vertex fromVertex = mock(TransitStopVertex.class);
    Vertex toVertex = mock(TransitStopVertex.class);
    RoutingRequest request = new RoutingRequest();

    // Test if domination works in the general case

    State stateA = new State(
      fromVertex,
      Instant.EPOCH,
      null,
      StateData.getInitialStateData(request)
    );
    State stateB = new State(toVertex, Instant.EPOCH, null, StateData.getInitialStateData(request));
    stateA.weight = 1;
    stateB.weight = 2;

    Assert.assertTrue(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateA, stateB));
    Assert.assertFalse(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateB, stateA));
  }
  // TODO: Make unit tests for rest of dominance functionality
  // TODO: Make functional tests for concepts covered by dominance with current algorithm
  // (Specific transfers, bike rental, park and ride, turn restrictions)
}
