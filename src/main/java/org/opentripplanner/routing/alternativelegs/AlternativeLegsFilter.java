package org.opentripplanner.routing.alternativelegs;

import java.util.function.Function;
import java.util.function.Predicate;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.plan.Leg;

public enum AlternativeLegsFilter {
  NO_FILTER((Leg leg) -> (TripPattern tripPattern) -> true),
  SAME_AGENCY((Leg leg) ->
    (TripPattern tripPattern) -> leg.getAgency().equals(tripPattern.getRoute().getAgency())
  ),
  SAME_ROUTE((Leg leg) -> (TripPattern tripPattern) -> leg.getRoute().equals(tripPattern.getRoute())
  ),
  SAME_MODE((Leg leg) ->
    (TripPattern tripPattern) -> leg.getTrip().getMode().equals(tripPattern.getMode())
  );

  public final Function<Leg, Predicate<TripPattern>> predicateGenerator;

  AlternativeLegsFilter(Function<Leg, Predicate<TripPattern>> predicateGenerator) {
    this.predicateGenerator = predicateGenerator;
  }

  public Predicate<TripPattern> getFilter(Leg leg) {
    return predicateGenerator.apply(leg);
  }
}
