package org.opentripplanner.routing.algorithm;

import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

import java.time.Instant;
import junit.framework.TestCase;
import org.junit.Ignore;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.TestUtils;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class TestAStar extends TestCase {

  public void testBasic() throws Exception {
    GtfsContext context = contextBuilder(ConstantsForTests.CALTRAIN_GTFS).build();

    Graph gg = new Graph();
    GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
    factory.run(gg);
    gg.putService(CalendarServiceData.class, context.getCalendarServiceData());
    RoutingRequest options = new RoutingRequest();

    ShortestPathTree spt;
    GraphPath path = null;

    String feedId = gg.getFeedIds().iterator().next();
    options.setDateTime(TestUtils.dateInstant("America/Los_Angeles", 2009, 8, 7, 12, 0, 0));
    Vertex millbrae = gg.getVertex(feedId + ":Millbrae Caltrain");
    Vertex mountainView = gg.getVertex(feedId + ":Mountain View Caltrain");
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, gg, millbrae, mountainView))
        .getShortestPathTree();
    path = spt.getPath(mountainView);

    long endTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 8, 7, 13, 29, 0);

    assertEquals(path.getEndTime(), endTime);

    /* test backwards traversal */
    options.setArriveBy(true);
    options.setDateTime(Instant.ofEpochSecond(endTime));
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, gg, millbrae, mountainView))
        .getShortestPathTree();
    path = spt.getPath(millbrae);

    long expectedStartTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 8, 7, 12, 39, 0);

    assertTrue(path.getStartTime() - expectedStartTime <= 1);
  }

  public void testMaxTime() {
    Graph graph = ConstantsForTests.getInstance().getCachedPortlandGraph();
    String feedId = graph.getFeedIds().iterator().next();
    Vertex start = graph.getVertex(feedId + ":8371");
    Vertex end = graph.getVertex(feedId + ":8374");

    RoutingRequest options = new RoutingRequest();
    long startTime = TestUtils.dateInSeconds("America/Los_Angeles", 2009, 11, 1, 12, 34, 25);
    options.setDateTime(Instant.ofEpochSecond(startTime));
    // one hour is more than enough time

    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, start, end))
      .getShortestPathTree();
    GraphPath path = spt.getPath(end);
    assertNotNull(path);

    // but one minute is not enough
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, start, end))
        .getShortestPathTree();
    path = spt.getPath(end);
    assertNull(path);
  }
}
