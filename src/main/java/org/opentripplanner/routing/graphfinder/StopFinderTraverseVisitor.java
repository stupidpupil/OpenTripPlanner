package org.opentripplanner.routing.graphfinder;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.algorithm.astar.strategies.SkipEdgeStrategy;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

// TODO Seems like this should be merged with the PlaceFinderTraverseVisitor

/**
 * A TraverseVisitor used in finding stops while walking the street graph.
 */
public class StopFinderTraverseVisitor implements TraverseVisitor {

  private final double radiusMeters;
  /** A list of closest stops found while walking the graph */
  public final List<NearbyStop> stopsFound = new ArrayList<>();

  public StopFinderTraverseVisitor(double radiusMeters) {
    this.radiusMeters = radiusMeters;
  }

  @Override
  public void visitEdge(Edge edge) {}

  // Accumulate stops into ret as the search runs.
  @Override
  public void visitVertex(State state) {
    Vertex vertex = state.getVertex();
    if (vertex instanceof TransitStopVertex) {
      stopsFound.add(NearbyStop.nearbyStopForState(state, ((TransitStopVertex) vertex).getStop()));
    }
  }

  @Override
  public void visitEnqueue() {}

  /**
   * @return A SkipEdgeStrategy that will stop exploring edges after the distance radius has been
   * reached.
   */
  public SkipEdgeStrategy getSkipEdgeStrategy() {
    return (current, edge) -> current.getWalkDistance() > radiusMeters;
  }
}
