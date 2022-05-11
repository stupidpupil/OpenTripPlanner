package org.opentripplanner.model;

import java.util.HashSet;
import java.util.Set;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.util.I18NString;

/**
 * A group of stopLocations, which can share a common Stoptime
 */
public class FlexLocationGroup extends TransitEntity implements StopLocation {

  private static final long serialVersionUID = 1L;
  private final Set<StopLocation> stopLocations = new HashSet<>();
  private I18NString name;
  private GeometryCollection geometry = new GeometryCollection(
    null,
    GeometryUtils.getGeometryFactory()
  );

  private Point centroid;

  public FlexLocationGroup(FeedScopedId id) {
    super(id);
  }

  @Override
  public I18NString getName() {
    return name;
  }

  public void setName(I18NString name) {
    this.name = name;
  }

  @Override
  public String getDescription() {
    return null;
  }

  @Override
  public I18NString getUrl() {
    return null;
  }

  @Override
  public String getFirstZoneAsString() {
    return null;
  }

  /**
   * Returns the centroid of all stops and areas belonging to this location group.
   */
  @Override
  public WgsCoordinate getCoordinate() {
    return new WgsCoordinate(centroid.getY(), centroid.getX());
  }

  @Override
  public Geometry getGeometry() {
    return geometry;
  }

  @Override
  public boolean isPartOfStation() {
    return false;
  }

  @Override
  public boolean isPartOfSameStationAs(StopLocation alternativeStop) {
    return false;
  }

  /**
   * Adds a new location to the location group. This should ONLY be used during the graph build
   * process.
   */
  public void addLocation(StopLocation location) {
    stopLocations.add(location);

    int numGeometries = geometry.getNumGeometries();
    Geometry[] newGeometries = new Geometry[numGeometries + 1];
    for (int i = 0; i < numGeometries; i++) {
      newGeometries[i] = geometry.getGeometryN(i);
    }
    if (location instanceof Stop) {
      WgsCoordinate coordinate = location.getCoordinate();
      Envelope envelope = new Envelope(coordinate.asJtsCoordinate());
      double xscale = Math.cos(coordinate.latitude() * Math.PI / 180);
      envelope.expandBy(100 / xscale, 100);
      newGeometries[numGeometries] = GeometryUtils.getGeometryFactory().toGeometry(envelope);
    } else if (location instanceof FlexStopLocation) {
      newGeometries[numGeometries] = location.getGeometry();
    } else {
      throw new RuntimeException("Unknown location type");
    }
    geometry = new GeometryCollection(newGeometries, GeometryUtils.getGeometryFactory());
    centroid = geometry.getCentroid();
  }

  /**
   * Returns all the locations belonging to this location group.
   */
  public Set<StopLocation> getLocations() {
    return stopLocations;
  }
}
