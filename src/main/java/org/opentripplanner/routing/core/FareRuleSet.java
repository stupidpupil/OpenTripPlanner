package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.FeedScopedId;

public class FareRuleSet implements Serializable {

  private static final long serialVersionUID = 7218355718876553028L;

  private final Set<FeedScopedId> routes;
  private final Set<P2<String>> originDestinations;
  private final Set<String> contains;
  private final FareAttribute fareAttribute;
  private final Set<FeedScopedId> trips;

  public FareRuleSet(FareAttribute fareAttribute) {
    this.fareAttribute = fareAttribute;
    routes = new HashSet<>();
    originDestinations = new HashSet<>();
    contains = new HashSet<>();
    trips = new HashSet<>();
  }

  public void addOriginDestination(String origin, String destination) {
    originDestinations.add(new P2<>(origin, destination));
  }

  public Set<P2<String>> getOriginDestinations() {
    return originDestinations;
  }

  public void addContains(String containsId) {
    contains.add(containsId);
  }

  public void addRoute(FeedScopedId route) {
    routes.add(route);
  }

  public Set<FeedScopedId> getRoutes() {
    return routes;
  }

  public FareAttribute getFareAttribute() {
    return fareAttribute;
  }

  public void addTrip(FeedScopedId trip) {
    trips.add(trip);
  }

  public Set<FeedScopedId> getTrips() {
    return trips;
  }

  public boolean matches(
    String startZone,
    String endZone,
    Set<String> zonesVisited,
    Set<FeedScopedId> routesVisited,
    Set<FeedScopedId> tripsVisited
  ) {
    //check for matching origin/destination, if this ruleset has any origin/destination restrictions
    if (originDestinations.size() > 0) {
      P2<String> od = new P2<>(startZone, endZone);
      if (!originDestinations.contains(od)) {
        P2<String> od2 = new P2<>(od.first, null);
        if (!originDestinations.contains(od2)) {
          od2 = new P2<>(null, od.first);
          if (!originDestinations.contains(od2)) {
            return false;
          }
        }
      }
    }

    //check for matching contains, if this ruleset has any containment restrictions
    if (contains.size() > 0) {
      if (!zonesVisited.equals(contains)) {
        return false;
      }
    }

    //check for matching routes
    if (routes.size() != 0) {
      if (!routes.containsAll(routesVisited)) {
        return false;
      }
    }

    //check for matching trips
    if (trips.size() != 0) {
      if (!trips.containsAll(tripsVisited)) {
        return false;
      }
    }

    return true;
  }
}
