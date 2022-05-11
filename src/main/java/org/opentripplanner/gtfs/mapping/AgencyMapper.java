package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.util.MapUtils;

/** Responsible for mapping GTFS Agency into the OTP model. */
class AgencyMapper {

  private final Map<org.onebusaway.gtfs.model.Agency, Agency> mappedAgencies = new HashMap<>();

  private final String feedId;

  public AgencyMapper(String feedId) {
    this.feedId = feedId;
  }

  Collection<Agency> map(Collection<org.onebusaway.gtfs.model.Agency> agencies) {
    return MapUtils.mapToList(agencies, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Agency map(org.onebusaway.gtfs.model.Agency orginal) {
    return orginal == null ? null : mappedAgencies.computeIfAbsent(orginal, this::doMap);
  }

  private Agency doMap(org.onebusaway.gtfs.model.Agency rhs) {
    Agency lhs = new Agency(
      new FeedScopedId(feedId, rhs.getId()),
      rhs.getName(),
      rhs.getTimezone()
    );

    lhs.setUrl(rhs.getUrl());
    lhs.setLang(rhs.getLang());
    lhs.setPhone(rhs.getPhone());
    lhs.setFareUrl(rhs.getFareUrl());
    lhs.setBrandingUrl(rhs.getBrandingUrl());

    return lhs;
  }
}
