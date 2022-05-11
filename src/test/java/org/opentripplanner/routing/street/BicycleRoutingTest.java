package org.opentripplanner.routing.street;

import static org.opentripplanner.test.support.PolylineAssert.assertThatPolylinesAreEqual;

import io.micrometer.core.instrument.Metrics;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.algorithm.mapping.AlertToLegMapper;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.PolylineEncoder;

public class BicycleRoutingTest {

  static final Instant dateTime = Instant.now();
  Graph herrenbergGraph = ConstantsForTests.buildOsmGraph(ConstantsForTests.HERRENBERG_OSM);

  /**
   * https://www.openstreetmap.org/way/22392895 is access=destination which means that both bicycles
   * and motor vehicles must not pass through.
   */
  @Test
  public void shouldRespectGeneralNoThroughTraffic() {
    var mozartStr = new GenericLocation(48.59713, 8.86107);
    var fritzLeharStr = new GenericLocation(48.59696, 8.85806);

    var polyline1 = computePolyline(herrenbergGraph, mozartStr, fritzLeharStr);
    assertThatPolylinesAreEqual(polyline1, "_srgHutau@h@B|@Jf@BdAG?\\JT@jA?DSp@_@fFsAT{@DBpC");

    var polyline2 = computePolyline(herrenbergGraph, fritzLeharStr, mozartStr);
    assertThatPolylinesAreEqual(polyline2, "{qrgH{aau@CqCz@ErAU^gFRq@?EAkAKUeACg@A_AM_AEDQF@H?");
  }

  /**
   * Tests that https://www.openstreetmap.org/way/35097400 is allowed for cars due to
   * motor_vehicle=destination being meant for cars only.
   */
  @Test
  public void shouldNotRespectMotorCarNoThru() {
    var schiessmauer = new GenericLocation(48.59737, 8.86350);
    var zeppelinStr = new GenericLocation(48.59972, 8.86239);

    var polyline1 = computePolyline(herrenbergGraph, schiessmauer, zeppelinStr);
    assertThatPolylinesAreEqual(polyline1, "otrgH{cbu@S_AU_AmAdAyApAGDs@h@_@\\_ClBe@^?S");

    var polyline2 = computePolyline(herrenbergGraph, zeppelinStr, schiessmauer);
    assertThatPolylinesAreEqual(polyline2, "ccsgH{|au@?Rd@_@~BmB^]r@i@FExAqAlAeAT~@R~@");
  }

  private static String computePolyline(Graph graph, GenericLocation from, GenericLocation to) {
    RoutingRequest request = new RoutingRequest();
    request.setDateTime(dateTime);
    request.from = from;
    request.to = to;
    request.bicycleOptimizeType = BicycleOptimizeType.QUICK;

    request.streetSubRequestModes = new TraverseModeSet(TraverseMode.BICYCLE);
    var temporaryVertices = new TemporaryVerticesContainer(graph, request);
    RoutingContext routingContext = new RoutingContext(request, graph, temporaryVertices);

    var gpf = new GraphPathFinder(new Router(graph, RouterConfig.DEFAULT, Metrics.globalRegistry));
    var paths = gpf.graphPathFinderEntryPoint(routingContext);

    GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
      graph.getTimeZone(),
      new AlertToLegMapper(graph.getTransitAlertService()),
      graph.streetNotesService,
      graph.ellipsoidToGeoidDifference
    );

    var itineraries = graphPathToItineraryMapper.mapItineraries(paths);
    temporaryVertices.close();

    // make sure that we only get BICYLE legs
    itineraries.forEach(i ->
      i.legs.forEach(l -> Assertions.assertEquals(l.getMode(), TraverseMode.BICYCLE))
    );
    Geometry legGeometry = itineraries.get(0).legs.get(0).getLegGeometry();
    return PolylineEncoder.encodeGeometry(legGeometry).points();
  }
}
