package org.opentripplanner.updater.vehicle_rental.datasources.params;

import java.util.Map;
import org.opentripplanner.updater.DataSourceType;

public class GbfsVehicleRentalDataSourceParameters extends VehicleRentalDataSourceParameters {

  private final boolean allowKeepingRentedVehicleAtDestination;
  private final String language;

  public GbfsVehicleRentalDataSourceParameters(
    String url,
    String language,
    boolean allowKeepingRentedVehicleAtDestination,
    Map<String, String> httpHeaders
  ) {
    super(DataSourceType.GBFS, url, httpHeaders);
    this.language = language;
    this.allowKeepingRentedVehicleAtDestination = allowKeepingRentedVehicleAtDestination;
  }

  public boolean allowKeepingRentedVehicleAtDestination() {
    return allowKeepingRentedVehicleAtDestination;
  }

  public String language() {
    return language;
  }
}
