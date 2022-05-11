package org.opentripplanner.api.common;

import java.util.Locale;
import org.opentripplanner.util.ResourceBundleAdaptor;

/**
 * The purpose of Messages is to read supply Message.properties to underlying calling code... The
 * ENUM's enumerated values should be named to reflect the property names inside of
 * Message.properties
 */
public enum Message {
  // id field is loosely based on HTTP error codes http://en.wikipedia.org/wiki/List_of_HTTP_status_codes
  PLAN_OK(200),
  SYSTEM_ERROR(500),
  GRAPH_UNAVAILABLE(503),

  OUTSIDE_BOUNDS(400),
  PATH_NOT_FOUND(404),
  NO_TRANSIT_TIMES(406),
  REQUEST_TIMEOUT(408),
  BOGUS_PARAMETER(413),
  GEOCODE_FROM_NOT_FOUND(440),
  GEOCODE_TO_NOT_FOUND(450),
  GEOCODE_FROM_TO_NOT_FOUND(460),
  TOO_CLOSE(409),
  LOCATION_NOT_ACCESSIBLE(470),

  UNDERSPECIFIED_TRIANGLE(370),
  TRIANGLE_NOT_AFFINE(371),
  TRIANGLE_OPTIMIZE_TYPE_NOT_SET(372),
  TRIANGLE_VALUES_NOT_SET(373);

  private final ResourceBundleAdaptor config = new ResourceBundleAdaptor(Message.class);
  private final int m_id;

  /** enum constructors are private -- see values above */
  Message(int id) {
    m_id = id;
  }

  public int getId() {
    return m_id;
  }

  public String get(Locale l) {
    try {
      return config.get(name(), l);
    } catch (Exception e) {
      ResourceBundleAdaptor.LOG.warn(
        "No entry in Message.properties file could be found for string " + name()
      );
      return "";
    }
  }

  public String get() {
    return get(Locale.getDefault());
  }
}
