package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.model.BikeAccess;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.WheelchairBoarding;
import org.opentripplanner.model.modes.AllowedTransitMode;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;

public class RoutingRequestTransitDataProviderFilter implements TransitDataProviderFilter {

  private final boolean requireBikesAllowed;

  private final WheelchairAccessibilityRequest wheelchairAccessibility;

  private final boolean includePlannedCancellations;

  private final Predicate<Trip> transitModeIsAllowed;

  private final Set<FeedScopedId> bannedRoutes;

  private final Set<FeedScopedId> bannedTrips;

  public RoutingRequestTransitDataProviderFilter(
    boolean requireBikesAllowed,
    WheelchairAccessibilityRequest accessibility,
    boolean includePlannedCancellations,
    Set<AllowedTransitMode> allowedTransitModes,
    Set<FeedScopedId> bannedRoutes,
    Set<FeedScopedId> bannedTrips
  ) {
    this.requireBikesAllowed = requireBikesAllowed;
    this.wheelchairAccessibility = accessibility;
    this.includePlannedCancellations = includePlannedCancellations;
    this.bannedRoutes = bannedRoutes;
    this.bannedTrips = bannedTrips;
    boolean hasOnlyMainModeFilters = allowedTransitModes
      .stream()
      .noneMatch(AllowedTransitMode::hasSubMode);

    // It is much faster to do a lookup in an EnumSet, so we use it if we don't want to filter
    // using submodes
    if (hasOnlyMainModeFilters) {
      EnumSet<TransitMode> allowedMainModes = allowedTransitModes
        .stream()
        .map(AllowedTransitMode::getMainMode)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(TransitMode.class)));
      transitModeIsAllowed = (Trip trip) -> allowedMainModes.contains(trip.getMode());
    } else {
      transitModeIsAllowed =
        (Trip trip) -> {
          TransitMode transitMode = trip.getMode();
          String netexSubmode = trip.getNetexSubmode();
          return allowedTransitModes.stream().anyMatch(m -> m.allows(transitMode, netexSubmode));
        };
    }
  }

  public RoutingRequestTransitDataProviderFilter(RoutingRequest request, GraphIndex graphIndex) {
    this(
      request.modes.transferMode == StreetMode.BIKE,
      request.wheelchairAccessibility,
      request.includePlannedCancellations,
      request.modes.transitModes,
      request.getBannedRoutes(graphIndex.getAllRoutes()),
      request.bannedTrips
    );
  }

  public static BikeAccess bikeAccessForTrip(Trip trip) {
    if (trip.getBikesAllowed() != BikeAccess.UNKNOWN) {
      return trip.getBikesAllowed();
    }

    return trip.getRoute().getBikesAllowed();
  }

  @Override
  public boolean tripPatternPredicate(TripPatternForDate tripPatternForDate) {
    return routeIsNotBanned(tripPatternForDate);
  }

  @Override
  public boolean tripTimesPredicate(TripTimes tripTimes) {
    final Trip trip = tripTimes.getTrip();
    if (!transitModeIsAllowed.test(trip)) {
      return false;
    }

    if (bannedTrips.contains(trip.getId())) {
      return false;
    }

    if (requireBikesAllowed && bikeAccessForTrip(trip) != BikeAccess.ALLOWED) {
      return false;
    }

    if (wheelchairAccessibility.enabled()) {
      if (
        wheelchairAccessibility.trips().onlyConsiderAccessible() &&
        trip.getWheelchairBoarding() != WheelchairBoarding.POSSIBLE
      ) {
        return false;
      }
    }

    //noinspection RedundantIfStatement
    if (!includePlannedCancellations && trip.getTripAlteration().isCanceledOrReplaced()) {
      return false;
    }

    return true;
  }

  @Override
  public BitSet filterAvailableStops(
    TripPatternWithRaptorStopIndexes tripPattern,
    BitSet boardingPossible
  ) {
    // if the user wants wheelchair-accessible routes and the configuration requires us to only
    // consider those stops which have the correct accessibility values then use only this for
    // checking whether to board/alight
    if (
      wheelchairAccessibility.enabled() && wheelchairAccessibility.stops().onlyConsiderAccessible()
    ) {
      var copy = (BitSet) boardingPossible.clone();
      // Use the and bitwise operator to add false flag to all stops that are not accessible by wheelchair
      copy.and(tripPattern.getWheelchairAccessible());

      return copy;
    }
    return boardingPossible;
  }

  private boolean routeIsNotBanned(TripPatternForDate tripPatternForDate) {
    FeedScopedId routeId = tripPatternForDate.getTripPattern().getPattern().getRoute().getId();
    return !bannedRoutes.contains(routeId);
  }
}
