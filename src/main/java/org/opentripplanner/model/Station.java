package org.opentripplanner.model;

import static org.opentripplanner.common.geometry.GeometryUtils.getGeometryFactory;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.locationtech.jts.algorithm.ConvexHull;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A grouping of stops in GTFS or the lowest level grouping in NeTEx. It can be a train station, a
 * bus terminal, or a bus station (with a bus stop at each side of the road). Equivalent to GTFS
 * stop location type 1 or NeTEx monomodal StopPlace.
 */
public class Station extends TransitEntity implements StopCollection {

  private static final long serialVersionUID = 1L;
  public static final StopTransferPriority DEFAULT_PRIORITY = StopTransferPriority.ALLOWED;

  private final I18NString name;

  private final String code;

  private final String description;

  private final WgsCoordinate coordinate;

  private final StopTransferPriority priority;
  /**
   * URL to a web page containing information about this particular station
   */
  private final I18NString url;
  private final TimeZone timezone;

  // We serialize this class to json only for snapshot tests, and this creates cyclical structures
  @JsonBackReference
  private final Set<StopLocation> childStops = new HashSet<>();

  private GeometryCollection geometry;

  public Station(
    FeedScopedId id,
    I18NString name,
    WgsCoordinate coordinate,
    String code,
    String description,
    I18NString url,
    TimeZone timezone,
    StopTransferPriority priority
  ) {
    super(id);
    this.name = name;
    this.coordinate = coordinate;
    this.code = code;
    this.description = description;
    this.url = url;
    this.timezone = timezone;
    this.priority = priority == null ? DEFAULT_PRIORITY : priority;
    // Initialize the geometry with an empty set of children
    this.geometry = computeGeometry(coordinate, Set.of());
  }

  /**
   * Create a minimal Station object for unit-test use, where the test only care about id, name and
   * coordinate. The feedId is static set to "F"
   */
  public static Station stationForTest(String idAndName, double lat, double lon) {
    return new Station(
      new FeedScopedId("F", idAndName),
      new NonLocalizedString(idAndName),
      new WgsCoordinate(lat, lon),
      idAndName,
      "Station " + idAndName,
      null,
      null,
      StopTransferPriority.ALLOWED
    );
  }

  public void addChildStop(Stop stop) {
    this.childStops.add(stop);
    this.geometry = computeGeometry(coordinate, childStops);
  }

  public boolean includes(StopLocation stop) {
    return childStops.contains(stop);
  }

  @Override
  public String toString() {
    return "<Station " + getId() + ">";
  }

  public I18NString getName() {
    return name;
  }

  public Collection<StopLocation> getChildStops() {
    return childStops;
  }

  public double getLat() {
    return coordinate.latitude();
  }

  public double getLon() {
    return coordinate.longitude();
  }

  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  /** Public facing station code (short text or number) */
  public String getCode() {
    return code;
  }

  /** Additional information about the station (if needed) */
  public String getDescription() {
    return description;
  }

  public I18NString getUrl() {
    return url;
  }

  /**
   * The generalized cost priority associated with the stop independently of trips, routes and/or
   * other stops. This is supported in NeTEx, but not in GTFS. This should work by adding adjusting
   * the cost for all board-/alight- events in the routing search.
   * <p/>
   * To not interfere with request parameters this must be implemented in a neutral way. This mean
   * that the {@link StopTransferPriority#ALLOWED} (which is default) should a nett-effect of adding
   * 0 - zero cost.
   */
  public StopTransferPriority getPriority() {
    return priority;
  }

  public TimeZone getTimezone() {
    return timezone;
  }

  /**
   * A geometry collection that contains the center point and the convex hull of all the child
   * stops.
   */
  public GeometryCollection getGeometry() {
    return geometry;
  }

  private static GeometryCollection computeGeometry(
    WgsCoordinate coordinate,
    Set<StopLocation> childStops
  ) {
    Point stationPoint = null;
    var childGeometries = childStops
      .stream()
      .map(StopLocation::getGeometry)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    if (coordinate != null) {
      stationPoint = getGeometryFactory().createPoint(coordinate.asJtsCoordinate());
      childGeometries.add(stationPoint);
    }
    var geometryCollection = getGeometryFactory()
      .createGeometryCollection(childGeometries.toArray(new Geometry[] {}));
    var convexHull = new ConvexHull(geometryCollection).getConvexHull();

    var geometries = stationPoint != null
      ? new Geometry[] { stationPoint, convexHull }
      : new Geometry[] { convexHull };
    return getGeometryFactory().createGeometryCollection(geometries);
  }
}
