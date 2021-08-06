package org.opentripplanner.updater.bike_rental.datasources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationUris;
import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.datasources.params.GbfsBikeRentalDataSourceParameters;
import org.opentripplanner.util.HttpUtils;
import org.opentripplanner.util.NonLocalizedString;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by demory on 2017-03-14.
 *
 * Leaving OTPFeature.FloatingBike turned off both prevents floating bike updaters added to
 * router-config.json from being used, but more importantly, floating bikes added by a
 * BikeRentalServiceDirectoryFetcher endpoint (which may be outside our control) will not be used.
 */
class GbfsBikeRentalDataSource implements BikeRentalDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(GbfsBikeRentalDataSource.class);

    private static final String DEFAULT_NETWORK_NAME = "GBFS";

    // station_information.json required by GBFS spec
    private final GbfsStationDataSource stationInformationSource;

    // station_status.json required by GBFS spec
    private final GbfsStationStatusDataSource stationStatusSource;

    // free_bike_status.json declared OPTIONAL by GBFS spec
    private final GbfsFloatingBikeDataSource floatingBikeSource;

    private final String networkName;

    /** Some car rental systems and flex transit systems work exactly like bike rental, but with cars. */
    private final boolean routeAsCar;

    public GbfsBikeRentalDataSource(GbfsBikeRentalDataSourceParameters parameters) {
        routeAsCar = parameters.routeAsCar();
        stationInformationSource = new GbfsStationDataSource(parameters);
        stationStatusSource = new GbfsStationStatusDataSource(parameters);
        floatingBikeSource = OTPFeature.FloatingBike.isOn()
            ? new GbfsFloatingBikeDataSource(parameters)
            : null;

        configureUrls(parameters.getUrl(), parameters.getHttpHeaders());
        this.networkName = parameters.getNetwork(DEFAULT_NETWORK_NAME);
    }

    private void configureUrls(String url, Map<String, String> headers) {
        GbfsAutoDiscoveryDataSource gbfsAutoDiscoveryDataSource = new GbfsAutoDiscoveryDataSource(url, headers);
        stationInformationSource.setUrl(gbfsAutoDiscoveryDataSource.stationInformationUrl);
        stationStatusSource.setUrl(gbfsAutoDiscoveryDataSource.stationStatusUrl);
        if (OTPFeature.FloatingBike.isOn()) {
            floatingBikeSource.setUrl(gbfsAutoDiscoveryDataSource.freeBikeStatusUrl);
        }
    }

    @Override
    public boolean update() {
        // These first two GBFS files are required.
        boolean updatesFound = stationInformationSource.update();
        updatesFound |= stationStatusSource.update();
        // This floating-bikes file is optional, and does not appear in all GBFS feeds.
        if (OTPFeature.FloatingBike.isOn()) {
            updatesFound |= floatingBikeSource.update();
        }
        // Return true if ANY of the sub-updaters found any updates.
        return updatesFound;
    }

    @Override
    public List<BikeRentalStation> getStations() {

        // Index all the station status entries on their station ID.
        Map<String, BikeRentalStation> statusLookup = new HashMap<>();
        for (BikeRentalStation station : stationStatusSource.getStations()) {
            statusLookup.put(station.id, station);
        }

        // Iterate over all known stations, and if we have any status information add it to those station objects.
        for (BikeRentalStation station : stationInformationSource.getStations()) {
            if (!statusLookup.containsKey(station.id)) continue;
            BikeRentalStation status = statusLookup.get(station.id);
            station.bikesAvailable = status.bikesAvailable;
            station.spacesAvailable = status.spacesAvailable;
        }

        // Copy the full list of station objects (with status updates) into a List, appending the floating bike stations.
        List<BikeRentalStation> stations = new LinkedList<>(stationInformationSource.getStations());
        if (OTPFeature.FloatingBike.isOn()) {
            stations.addAll(floatingBikeSource.getStations());
        }

        // Set identical network ID on all stations
        Set<String> networkIdSet = Sets.newHashSet(this.networkName);
        for (BikeRentalStation station : stations) station.networks = networkIdSet;

        return stations;
    }

    private static class GbfsAutoDiscoveryDataSource {
        private String stationInformationUrl;
        private String stationStatusUrl;
        private String freeBikeStatusUrl;

        public GbfsAutoDiscoveryDataSource(String autoDiscoveryUrl, Map<String, String> headers) {

            try {
                InputStream is = HttpUtils.getData(autoDiscoveryUrl, headers);
                JsonNode node = (new ObjectMapper()).readTree(is);
                JsonNode languages = node.get("data");

                for (JsonNode language : languages) {

                    JsonNode feeds = language.get("feeds");

                    for (JsonNode feed : feeds) {
                        String url = feed.get("url").asText();
                        switch (feed.get("name").asText()) {
                            case "station_information":
                                stationInformationUrl = url;
                                break;
                            case "station_status":
                                stationStatusUrl = url;
                                break;
                            case "free_bike_status":
                                freeBikeStatusUrl = url;
                                break;
                        }
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                LOG.warn("Error reading auto discovery file at {}. Using default values.",
                    autoDiscoveryUrl, e);
                // If the GBFS auto-discovery file (gbfs.json) can't be downloaded, fall back to the
                // v1 logic of finding the files under the given base path.
                var baseUrl = getBaseUrl(autoDiscoveryUrl);
                stationInformationUrl = baseUrl + "station_information.json";
                stationStatusUrl = baseUrl + "station_status.json";
                freeBikeStatusUrl = baseUrl + "free_bike_status.json";
            }
        }

        private String getBaseUrl(String url) {
            String baseUrl = url;
            if (baseUrl.endsWith("gbfs.json")) {
                baseUrl = baseUrl.substring(0, url.length() - "gbfs.json".length());
            }
            if (baseUrl.endsWith("gbfs")) {
                baseUrl = baseUrl.substring(0, url.length() - "gbfs".length());
            }
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }
            return baseUrl;
        }
    }

    class GbfsStationDataSource extends GenericJsonBikeRentalDataSource<GbfsBikeRentalDataSourceParameters> {

        public GbfsStationDataSource (GbfsBikeRentalDataSourceParameters config) {
            super(config, "data/stations");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("station_id").asText();
            brstation.x = stationNode.path("lon").asDouble();
            brstation.y = stationNode.path("lat").asDouble();
            brstation.name =  new NonLocalizedString(stationNode.path("name").asText());
            brstation.isKeepingBicycleRentalAtDestinationAllowed = config.allowKeepingRentedBicycleAtDestination();
            brstation.isCarStation = routeAsCar;

            if (stationNode.has("rental_uris")) {
                var rentalUrisObject = stationNode.path("rental_uris");
                String androidUri = rentalUrisObject.has("android") ? rentalUrisObject.get("android").asText() : null;
                String iosUri = rentalUrisObject.has("ios") ? rentalUrisObject.get("ios").asText() : null;
                String webUri = rentalUrisObject.has("web") ? rentalUrisObject.get("web").asText() : null;
                brstation.rentalUris = new BikeRentalStationUris(androidUri, iosUri, webUri);
            }

            return brstation;
        }
    }

    class GbfsStationStatusDataSource extends GenericJsonBikeRentalDataSource<GbfsBikeRentalDataSourceParameters> {

        public GbfsStationStatusDataSource (GbfsBikeRentalDataSourceParameters config) {
            super(config, "data/stations");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            BikeRentalStation brstation = new BikeRentalStation();
            brstation.id = stationNode.path("station_id").asText();
            brstation.bikesAvailable = stationNode.path("num_bikes_available").asInt();
            brstation.spacesAvailable = stationNode.path("num_docks_available").asInt();
            brstation.isKeepingBicycleRentalAtDestinationAllowed = config.allowKeepingRentedBicycleAtDestination();
            brstation.isCarStation = routeAsCar;
            return brstation;
        }
    }

    // TODO This is not currently safe to use. See javadoc on GbfsBikeRentalDataSource class.
    class GbfsFloatingBikeDataSource extends GenericJsonBikeRentalDataSource<GbfsBikeRentalDataSourceParameters> {

        public GbfsFloatingBikeDataSource (GbfsBikeRentalDataSourceParameters config) {
            super(config, "data/bikes");
        }

        @Override
        public BikeRentalStation makeStation(JsonNode stationNode) {
            if (stationNode.path("station_id").asText().isBlank() &&
                    stationNode.has("lon") &&
                    stationNode.has("lat")
            ) {
                BikeRentalStation brstation = new BikeRentalStation();
                brstation.id = stationNode.path("bike_id").asText();
                brstation.name = new NonLocalizedString(stationNode.path("name").asText());
                brstation.x = stationNode.path("lon").asDouble();
                brstation.y = stationNode.path("lat").asDouble();
                brstation.bikesAvailable = 1;
                brstation.spacesAvailable = 0;
                brstation.allowDropoff = false;
                brstation.isFloatingBike = true;
                brstation.isCarStation = routeAsCar;
                return brstation;
            } else {
                return null;
            }
        }
    }

}
