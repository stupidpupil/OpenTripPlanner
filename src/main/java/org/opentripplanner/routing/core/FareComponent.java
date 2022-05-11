package org.opentripplanner.routing.core;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.FeedScopedId;

/**
 * <p>
 * FareComponent is a sequence of routes for a particular fare.
 * </p>
 */
public class FareComponent {

  public FeedScopedId fareId;
  public Money price;
  public List<FeedScopedId> routes;

  public FareComponent(FeedScopedId fareId, Money amount) {
    this.fareId = fareId;
    price = amount;
    routes = new ArrayList<>();
  }

  public void addRoute(FeedScopedId routeId) {
    routes.add(routeId);
  }

  public String toString() {
    StringBuilder buffer = new StringBuilder("FareComponent(");
    buffer.append(fareId.toString());
    buffer.append(", ");
    buffer.append(price.toString());
    buffer.append(", ");
    for (FeedScopedId routeId : routes) {
      buffer.append(routeId.toString());
      buffer.append(", ");
    }
    buffer.append(")");
    return buffer.toString();
  }
}
