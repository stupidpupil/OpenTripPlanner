package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.trippattern.TripTimes;

/**
 * Used to filter the elements in a {@link org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer}
 * when constructing a {@link RaptorRoutingRequestTransitData} for a request.
 * <p>
 * {@link TripPatternForDate} and {@link TripTimes} are filtered based on the request parameters to
 * only included components which are allowed by the request. Such filters may included bike or
 * wheelchair accessibility, banned routes and transit modes.
 *
 * @see RoutingRequestTransitDataProviderFilter
 */
public interface TransitDataProviderFilter {
  boolean tripPatternPredicate(TripPatternForDate tripPatternForDate);

  boolean tripTimesPredicate(TripTimes tripTimes);

  /**
   * Check if boarding/alighting is possible at each stop. If the values differ from the default
   * input values, create a clone of the bitset and subtract the unavailable stops.
   *
   * @param tripPattern      Trip pattern that should contain boarding/alighting information, e.g.
   *                         wheelchair accessibility for each stop.
   * @param boardingPossible Initial information regarding boarding/alighting possible
   * @return Information if stops are available for boarding or alighting
   */
  BitSet filterAvailableStops(
    TripPatternWithRaptorStopIndexes tripPattern,
    BitSet boardingPossible
  );
}
