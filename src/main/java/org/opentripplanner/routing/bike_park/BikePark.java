package org.opentripplanner.routing.bike_park;

import static java.util.Locale.ROOT;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;

public class BikePark implements Serializable {
    private static final long serialVersionUID = 8311460609708089384L;

    /**
     * Unique ID of the bike park. Creator should ensure the ID is unique server-wide (prefix by a
     * source ID if there are several sources)
     */
    @JsonSerialize
    public String id;

    @JsonSerialize
    public String name;

    /** Note: x = Longitude, y = Latitude */
    @JsonSerialize
    public double x, y;

    @JsonSerialize
    public int spacesAvailable = Integer.MAX_VALUE;

    /**
     * Whether this station has space available information updated in real-time. If no real-time
     * data, users should take spacesAvailable with a pinch of salt, as they are a crude estimate.
     */
    @JsonSerialize
    public boolean realTimeData = true;

    public boolean equals(Object o) {
        if (!(o instanceof BikePark)) {
            return false;
        }
        BikePark other = (BikePark) o;
        return other.id.equals(id);
    }

    public int hashCode() {
        return id.hashCode() + 1;
    }

    public String toString () {
        return String.format(ROOT, "Bike park %s at %.6f, %.6f", name, y, x);
    }
}
