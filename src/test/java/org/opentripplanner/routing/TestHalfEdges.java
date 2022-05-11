package org.opentripplanner.routing;

import static com.google.common.collect.Iterables.filter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.linking.DisposableEdgeCollection;
import org.opentripplanner.graph_builder.linking.SameEdgeAdjuster;
import org.opentripplanner.graph_builder.module.StreetLinkerModule;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.astar.AStarBuilder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.TestUtils;

public class TestHalfEdges {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  Graph graph;
  private StreetEdge top, bottom, left, right, leftBack, rightBack;
  private IntersectionVertex br, tr, bl, tl;
  private TransitStopVertex station1;
  private TransitStopVertex station2;

  public LineString createGeometry(Vertex a, Vertex b) {
    GeometryFactory factory = new GeometryFactory();
    Coordinate[] cs = new Coordinate[2];
    cs[0] = a.getCoordinate();
    cs[1] = b.getCoordinate();
    return factory.createLineString(cs);
  }

  @Before
  public void setUp() {
    graph = new Graph();
    // a 0.1 degree x 0.1 degree square
    tl = new IntersectionVertex(graph, "tl", -74.01, 40.01);
    tr = new IntersectionVertex(graph, "tr", -74.0, 40.01);
    bl = new IntersectionVertex(graph, "bl", -74.01, 40.0);
    br = new IntersectionVertex(graph, "br", -74.00, 40.0);

    top =
      new StreetEdge(
        tl,
        tr,
        GeometryUtils.makeLineString(-74.01, 40.01, -74.0, 40.01),
        "top",
        1500,
        StreetTraversalPermission.ALL,
        false
      );
    bottom =
      new StreetEdge(
        br,
        bl,
        GeometryUtils.makeLineString(-74.01, 40.0, -74.0, 40.0),
        "bottom",
        1500,
        StreetTraversalPermission.ALL,
        false
      );
    left =
      new StreetEdge(
        bl,
        tl,
        GeometryUtils.makeLineString(-74.01, 40.0, -74.01, 40.01),
        "left",
        1500,
        StreetTraversalPermission.ALL,
        false
      );
    right =
      new StreetEdge(
        br,
        tr,
        GeometryUtils.makeLineString(-74.0, 40.0, -74.0, 40.01),
        "right",
        1500,
        StreetTraversalPermission.PEDESTRIAN,
        false
      );

    @SuppressWarnings("unused")
    StreetEdge topBack = new StreetEdge(
      tr,
      tl,
      (LineString) top.getGeometry().reverse(),
      "topBack",
      1500,
      StreetTraversalPermission.ALL,
      true
    );
    @SuppressWarnings("unused")
    StreetEdge bottomBack = new StreetEdge(
      br,
      bl,
      (LineString) bottom.getGeometry().reverse(),
      "bottomBack",
      1500,
      StreetTraversalPermission.ALL,
      true
    );
    leftBack =
      new StreetEdge(
        tl,
        bl,
        (LineString) left.getGeometry().reverse(),
        "leftBack",
        1500,
        StreetTraversalPermission.ALL,
        true
      );
    rightBack =
      new StreetEdge(
        tr,
        br,
        (LineString) right.getGeometry().reverse(),
        "rightBack",
        1500,
        StreetTraversalPermission.ALL,
        true
      );

    Stop s1 = Stop.stopForTest("fleem station", 40.0099999, -74.005);

    Stop s2 = Stop.stopForTest("morx station", 40.0099999, -74.002);

    station1 = new TransitStopVertex(graph, s1, null);
    station2 = new TransitStopVertex(graph, s2, null);
    station1.addMode(TransitMode.RAIL);
    station2.addMode(TransitMode.RAIL);

    //Linkers aren't run otherwise in testNetworkLinker
    graph.hasStreets = true;
    graph.hasTransit = true;
  }

  @Test
  public void testHalfEdges() {
    // the shortest half-edge from the start vertex takes you down, but the shortest total path
    // is up and over

    DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph);

    int nVertices = graph.getVertices().size();
    int nEdges = graph.getEdges().size();

    RoutingRequest options = new RoutingRequest();

    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);
    turns.add(leftBack);

    TemporaryStreetLocation start = StreetVertexIndex.createTemporaryStreetLocationForTest(
      "start",
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false,
      tempEdges
    );

    HashSet<Edge> endTurns = new HashSet<>();
    endTurns.add(right);
    endTurns.add(rightBack);

    TemporaryStreetLocation end = StreetVertexIndex.createTemporaryStreetLocationForTest(
      "end",
      new NonLocalizedString("end"),
      filter(endTurns, StreetEdge.class),
      new LinearLocation(0, 0.8).getCoordinate(right.getGeometry()),
      true,
      tempEdges
    );

    assertTrue(start.getX() < end.getX());
    assertTrue(start.getY() < end.getY());

    Collection<Edge> edges = end.getIncoming();

    assertEquals(2, edges.size());

    long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 11, 1, 12, 34, 25);
    options.setDateTime(Instant.ofEpochSecond(startTime));
    ShortestPathTree spt1 = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, br, end))
      .getShortestPathTree();

    GraphPath pathBr = spt1.getPath(end);
    assertNotNull("There must be a path from br to end", pathBr);

    ShortestPathTree spt2 = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, tr, end))
      .getShortestPathTree();

    GraphPath pathTr = spt2.getPath(end);
    assertNotNull("There must be a path from tr to end", pathTr);
    assertTrue(
      "path from bottom to end must be longer than path from top to end",
      pathBr.getWeight() > pathTr.getWeight()
    );

    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, start, end))
      .getShortestPathTree();

    GraphPath path = spt.getPath(end);
    assertNotNull("There must be a path from start to end", path);

    // the bottom is not part of the shortest path
    for (State s : path.states) {
      assertNotSame(s.getVertex(), graph.getVertex("bottom"));
      assertNotSame(s.getVertex(), graph.getVertex("bottomBack"));
    }

    options.setArriveBy(true);
    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, start, end))
        .getShortestPathTree();

    path = spt.getPath(start);
    assertNotNull("There must be a path from start to end (looking back)", path);

    // the bottom edge is not part of the shortest path
    for (State s : path.states) {
      assertNotSame(s.getVertex(), graph.getVertex("bottom"));
      assertNotSame(s.getVertex(), graph.getVertex("bottomBack"));
    }

    // Number of vertices and edges should be the same as before after a cleanup.
    tempEdges.disposeEdges();
    assertEquals(nVertices, graph.getVertices().size());
    assertEquals(nEdges, graph.getEdges().size());

    /*
     * Now, the right edge is not bikeable. But the user can walk their bike. So here are some tests that prove (a) that walking bikes works, but
     * that (b) it is not preferred to riding a tiny bit longer.
     */

    options = new RoutingRequest(new TraverseModeSet(TraverseMode.BICYCLE));
    start =
      StreetVertexIndex.createTemporaryStreetLocationForTest(
        "start1",
        new NonLocalizedString("start1"),
        filter(turns, StreetEdge.class),
        new LinearLocation(0, 0.95).getCoordinate(top.getGeometry()),
        false,
        tempEdges
      );
    end =
      StreetVertexIndex.createTemporaryStreetLocationForTest(
        "end1",
        new NonLocalizedString("end1"),
        filter(turns, StreetEdge.class),
        new LinearLocation(0, 0.95).getCoordinate(bottom.getGeometry()),
        true,
        tempEdges
      );

    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, start, end))
        .getShortestPathTree();

    path = spt.getPath(start);
    assertNotNull("There must be a path from top to bottom along the right", path);

    // the left edge is not part of the shortest path (even though the bike must be walked along the right)
    for (State s : path.states) {
      assertNotSame(s.getVertex(), graph.getVertex("left"));
      assertNotSame(s.getVertex(), graph.getVertex("leftBack"));
    }

    // Number of vertices and edges should be the same as before after a cleanup.
    tempEdges.disposeEdges();
    assertEquals(nVertices, graph.getVertices().size());
    assertEquals(nEdges, graph.getEdges().size());

    start =
      StreetVertexIndex.createTemporaryStreetLocationForTest(
        "start2",
        new NonLocalizedString("start2"),
        filter(turns, StreetEdge.class),
        new LinearLocation(0, 0.55).getCoordinate(top.getGeometry()),
        false,
        tempEdges
      );
    end =
      StreetVertexIndex.createTemporaryStreetLocationForTest(
        "end2",
        new NonLocalizedString("end2"),
        filter(turns, StreetEdge.class),
        new LinearLocation(0, 0.55).getCoordinate(bottom.getGeometry()),
        true,
        tempEdges
      );

    spt =
      AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(options, graph, start, end))
        .getShortestPathTree();

    path = spt.getPath(start);
    assertNotNull("There must be a path from top to bottom", path);

    // the right edge is not part of the shortest path, e
    for (State s : path.states) {
      assertNotSame(s.getVertex(), graph.getVertex("right"));
      assertNotSame(s.getVertex(), graph.getVertex("rightBack"));
    }

    // Number of vertices and edges should be the same as before after a cleanup.
    tempEdges.disposeEdges();
    assertEquals(nVertices, graph.getVertices().size());
    assertEquals(nEdges, graph.getEdges().size());
  }

  @Test
  public void testRouteToSameEdge() {
    RoutingRequest options = new RoutingRequest();
    DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph);

    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);
    turns.add(leftBack);

    TemporaryStreetLocation start = StreetVertexIndex.createTemporaryStreetLocationForTest(
      "start",
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false,
      tempEdges
    );

    TemporaryStreetLocation end = StreetVertexIndex.createTemporaryStreetLocationForTest(
      "end",
      new NonLocalizedString("end"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.8).getCoordinate(left.getGeometry()),
      true,
      tempEdges
    );

    DisposableEdgeCollection connection = SameEdgeAdjuster.adjust(start, end, graph);

    assertEquals(start.getX(), end.getX(), 0.0001);
    assertTrue(start.getY() < end.getY());

    Collection<Edge> edges = end.getIncoming();

    assertEquals(3, edges.size());

    long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 11, 1, 12, 34, 25);
    options.setDateTime(Instant.ofEpochSecond(startTime));
    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, start, end))
      .getShortestPathTree();

    GraphPath path = spt.getPath(end);
    assertNotNull("There must be a path from start to end", path);
    assertEquals(1, path.edges.size());
    tempEdges.disposeEdges();
    connection.disposeEdges();
  }

  @Test
  public void testRouteToSameEdgeBackwards() {
    RoutingRequest options = new RoutingRequest();
    DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph);

    // Sits only on the leftmost edge, not on its reverse.
    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);

    TemporaryStreetLocation start = StreetVertexIndex.createTemporaryStreetLocationForTest(
      "start",
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.8).getCoordinate(left.getGeometry()),
      false,
      tempEdges
    );

    TemporaryStreetLocation end = StreetVertexIndex.createTemporaryStreetLocationForTest(
      "end",
      new NonLocalizedString("end"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      true,
      tempEdges
    );

    DisposableEdgeCollection connection = SameEdgeAdjuster.adjust(start, end, graph);

    assertEquals(start.getX(), end.getX(), 0.001);
    assertTrue(start.getY() > end.getY());

    Collection<Edge> edges = end.getIncoming();
    assertEquals(1, edges.size());

    long startTime = TestUtils.dateInSeconds("America/New_York", 2009, 11, 1, 12, 34, 25);
    options.setDateTime(Instant.ofEpochSecond(startTime));
    ShortestPathTree spt = AStarBuilder
      .oneToOne()
      .setContext(new RoutingContext(options, graph, start, end))
      .getShortestPathTree();

    GraphPath path = spt.getPath(end);
    assertNotNull("There must be a path from start to end", path);
    assertTrue(path.edges.size() > 1);
    tempEdges.disposeEdges();
    connection.disposeEdges();
  }

  /**
   * Test that alerts on split streets are preserved, i.e. if there are alerts on the street that is
   * split the same alerts should be present on the new street.
   */
  @Test
  public void testStreetSplittingAlerts() {
    DisposableEdgeCollection tempEdges = new DisposableEdgeCollection(graph);

    HashSet<Edge> turns = new HashSet<>();
    turns.add(left);
    turns.add(leftBack);

    StreetNote alert = new StreetNote("This is the alert");
    Set<StreetNote> alerts = new HashSet<>();
    alerts.add(alert);

    graph.streetNotesService.addStaticNote(left, alert, StreetNotesService.ALWAYS_MATCHER);
    graph.streetNotesService.addStaticNote(leftBack, alert, StreetNotesService.ALWAYS_MATCHER);

    TemporaryStreetLocation start = StreetVertexIndex.createTemporaryStreetLocationForTest(
      "start",
      new NonLocalizedString("start"),
      filter(turns, StreetEdge.class),
      new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
      false,
      tempEdges
    );

    // The alert should be preserved
    // traverse the FreeEdge from the StreetLocation to the new IntersectionVertex
    RoutingRequest req = new RoutingRequest();
    State traversedOne = new State(start, req, null);
    State currentState;
    for (Edge e : start.getOutgoing()) {
      currentState = e.traverse(traversedOne);
      if (currentState != null) {
        traversedOne = currentState;
        break;
      }
    }

    assertEquals(alerts, graph.streetNotesService.getNotes(traversedOne));
    assertNotSame(left, traversedOne.getBackEdge().getFromVertex());
    assertNotSame(leftBack, traversedOne.getBackEdge().getFromVertex());

    // now, make sure wheelchair alerts are preserved
    StreetNote wheelchairAlert = new StreetNote("This is the wheelchair alert");
    Set<StreetNote> wheelchairAlerts = new HashSet<>();
    wheelchairAlerts.add(wheelchairAlert);

    graph.streetNotesService.removeStaticNotes(left);
    graph.streetNotesService.removeStaticNotes(leftBack);
    graph.streetNotesService.addStaticNote(
      left,
      wheelchairAlert,
      StreetNotesService.WHEELCHAIR_MATCHER
    );
    graph.streetNotesService.addStaticNote(
      leftBack,
      wheelchairAlert,
      StreetNotesService.WHEELCHAIR_MATCHER
    );

    req.setWheelchairAccessible(true);

    start =
      StreetVertexIndex.createTemporaryStreetLocationForTest(
        "start",
        new NonLocalizedString("start"),
        filter(turns, StreetEdge.class),
        new LinearLocation(0, 0.4).getCoordinate(left.getGeometry()),
        false,
        tempEdges
      );

    traversedOne = new State(start, req, null);
    for (Edge e : start.getOutgoing()) {
      currentState = e.traverse(traversedOne);
      if (currentState != null) {
        traversedOne = currentState;
        break;
      }
    }

    assertEquals(wheelchairAlerts, graph.streetNotesService.getNotes(traversedOne));
    assertNotSame(left, traversedOne.getBackEdge().getFromVertex());
    assertNotSame(leftBack, traversedOne.getBackEdge().getFromVertex());
    tempEdges.disposeEdges();
  }

  @Test
  public void testStreetLocationFinder() {
    RoutingRequest options = new RoutingRequest();
    StreetVertexIndex finder = graph.getStreetIndex();
    Set<DisposableEdgeCollection> tempEdges = new HashSet<>();
    // test that the local stop finder finds stops
    GenericLocation loc = new GenericLocation(40.01, -74.005000001);
    assertTrue(finder.getNearbyTransitStops(loc.getCoordinate(), 100).size() > 0);

    // test that the closest vertex finder returns the closest vertex
    TemporaryStreetLocation some = (TemporaryStreetLocation) finder.getVertexForLocationForTest(
      new GenericLocation(40.00, -74.00),
      options,
      true,
      tempEdges
    );
    assertNotNull(some);

    // test that the closest vertex finder correctly splits streets
    TemporaryStreetLocation start = (TemporaryStreetLocation) finder.getVertexForLocationForTest(
      new GenericLocation(40.004, -74.01),
      options,
      false,
      tempEdges
    );
    assertNotNull(start);
    assertTrue(
      "wheelchair accessibility is correctly set (splitting)",
      start.isWheelchairAccessible()
    );

    Collection<Edge> edges = start.getOutgoing();
    assertEquals(2, edges.size());

    RoutingRequest biking = new RoutingRequest(new TraverseModeSet(TraverseMode.BICYCLE));
    TemporaryStreetLocation end = (TemporaryStreetLocation) finder.getVertexForLocationForTest(
      new GenericLocation(40.008, -74.0),
      biking,
      true,
      tempEdges
    );
    assertNotNull(end);

    edges = end.getIncoming();
    assertEquals(2, edges.size());

    tempEdges.forEach(DisposableEdgeCollection::disposeEdges);
  }

  @Test
  public void testTemporaryVerticesContainer() {
    // test that it is possible to travel between two splits on the same street
    RoutingRequest walking = new RoutingRequest(TraverseMode.WALK);
    walking.from = new GenericLocation(40.004, -74.0);
    walking.to = new GenericLocation(40.008, -74.0);
    try (var container = new TemporaryVerticesContainer(graph, walking)) {
      assertNotNull(container.getFromVertices());
      assertNotNull(container.getToVertices());
      // The visibility for temp edges for start and end is set in the setRoutingContext call
      ShortestPathTree spt = AStarBuilder
        .oneToOne()
        .setContext(new RoutingContext(walking, graph, container))
        .getShortestPathTree();
      GraphPath path = spt.getPath(container.getToVertices().iterator().next());
      for (State s : path.states) {
        assertNotSame(s.getBackEdge(), top);
      }
    }
  }

  @Test
  public void testNetworkLinker() {
    int numVerticesBefore = graph.getVertices().size();
    StreetLinkerModule ttsnm = new StreetLinkerModule();
    ttsnm.buildGraph(graph, new HashMap<>());
    int numVerticesAfter = graph.getVertices().size();
    assertEquals(4, numVerticesAfter - numVerticesBefore);
    Collection<Edge> outgoing = station1.getOutgoing();
    assertEquals(2, outgoing.size());
    Edge edge = outgoing.iterator().next();

    Vertex midpoint = edge.getToVertex();
    assertTrue(Math.abs(midpoint.getCoordinate().y - 40.01) < 0.00000001);

    outgoing = station2.getOutgoing();
    assertEquals(2, outgoing.size());
    edge = outgoing.iterator().next();

    Vertex station2point = edge.getToVertex();
    assertTrue(Math.abs(station2point.getCoordinate().x - -74.002) < 0.00000001);
  }
}
