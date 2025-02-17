package org.opentripplanner.ext.flex.flexpathcalculator;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * StreetFlexPathCalculator calculates the driving times and distances based on the street network
 * using the AStar algorithm.
 * <p>
 * Note that it caches the whole ShortestPathTree the first time it encounters a new fromVertex.
 * Subsequent requests from the same fromVertex can fetch the path to the toVertex from the existing
 * ShortestPathTree. This one-to-many approach is needed to make the performance acceptable.
 * <p>
 * Because we will have lots of searches with the same origin when doing access searches and a lot
 * of searches with the same destination when doing egress searches, the calculator needs to be
 * configured so that the caching is done with either the origin or destination vertex as the key.
 * The one-to-many search will then either be done in the forward or the reverse direction depending
 * on this configuration.
 */
public class StreetFlexPathCalculator implements FlexPathCalculator {

  private static final Duration MAX_FLEX_TRIP_DURATION = Duration.ofMinutes(45);

  private final Graph graph;
  private final Map<Vertex, ShortestPathTree> cache = new HashMap<>();
  private final boolean reverseDirection;

  public StreetFlexPathCalculator(Graph graph, boolean reverseDirection) {
    this.graph = graph;
    this.reverseDirection = reverseDirection;
  }

  @Override
  public FlexPath calculateFlexPath(Vertex fromv, Vertex tov, int fromStopIndex, int toStopIndex) {
    // These are the origin and destination vertices from the perspective of the one-to-many search,
    // which may be reversed
    Vertex originVertex = reverseDirection ? tov : fromv;
    Vertex destinationVertex = reverseDirection ? fromv : tov;

    ShortestPathTree shortestPathTree;
    if (cache.containsKey(originVertex)) {
      shortestPathTree = cache.get(originVertex);
    } else {
      shortestPathTree = routeToMany(originVertex);
      cache.put(originVertex, shortestPathTree);
    }

    GraphPath path = shortestPathTree.getPath(destinationVertex);
    if (path == null) {
      return null;
    }

    int distance = (int) path.getDistanceMeters();
    int duration = path.getDuration();

    // computing the linestring from the graph path is a surprisingly expensive operation
    // so we delay it until it's actually needed. since most flex paths are never shown to the user
    // this improves performance quite a bit.
    return new FlexPath(distance, duration, path::getGeometry);
  }

  private ShortestPathTree routeToMany(Vertex vertex) {
    RoutingRequest routingRequest = new RoutingRequest(TraverseMode.CAR);
    routingRequest.arriveBy = reverseDirection;
    RoutingContext rctx;
    if (reverseDirection) {
      rctx = new RoutingContext(routingRequest, graph, null, vertex);
    } else {
      rctx = new RoutingContext(routingRequest, graph, vertex, null);
    }

    return AStarBuilder
      .allDirectionsMaxDuration(MAX_FLEX_TRIP_DURATION)
      .setDominanceFunction(new DominanceFunction.EarliestArrival())
      .setContext(rctx)
      .getShortestPathTree();
  }
}
