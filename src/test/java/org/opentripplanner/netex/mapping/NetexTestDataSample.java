package org.opentripplanner.netex.mapping;

import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;
import static org.opentripplanner.netex.mapping.MappingSupport.createJaxbElement;
import static org.opentripplanner.netex.mapping.MappingSupport.createWrappedRef;

import com.google.common.collect.ArrayListMultimap;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBElement;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMap;
import org.opentripplanner.netex.index.hierarchy.HierarchicalMapById;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.DestinationDisplayRefStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternRefStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDayRefStructure;
import org.rutebanken.netex.model.PointInLinkSequence_VersionedChildStructure;
import org.rutebanken.netex.model.PointsInJourneyPattern_RelStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.RouteRefStructure;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPointInJourneyPattern;
import org.rutebanken.netex.model.StopPointInJourneyPatternRefStructure;
import org.rutebanken.netex.model.TimetabledPassingTime;
import org.rutebanken.netex.model.TimetabledPassingTimes_RelStructure;
import org.rutebanken.netex.model.Via_VersionedChildStructure;
import org.rutebanken.netex.model.Vias_RelStructure;

class NetexTestDataSample {

  public static final String SERVICE_JOURNEY_ID = "RUT:ServiceJourney:1";
  public static final List<String> DATED_SERVICE_JOURNEY_ID = List.of(
    "RUT:DatedServiceJourney:1",
    "RUT:DatedServiceJourney:2"
  );
  public static final List<String> OPERATING_DAYS = List.of("2022-02-28", "2022-02-29");
  private static final DayType EVERYDAY = new DayType()
    .withId("EVERYDAY")
    .withName(new MultilingualString().withValue("everyday"));

  private final JourneyPattern journeyPattern;
  private final HierarchicalMapById<JourneyPattern> journeyPatternById = new HierarchicalMapById<>();
  private final HierarchicalMapById<DestinationDisplay> destinationDisplayById = new HierarchicalMapById<>();
  private final EntityById<Stop> stopsById = new EntityById<>();
  private final HierarchicalMap<String, String> quayIdByStopPointRef = new HierarchicalMap<>();
  private final List<TimetabledPassingTime> timetabledPassingTimes = new ArrayList<>();
  private final HierarchicalMapById<ServiceJourney> serviceJourneyById = new HierarchicalMapById<>();
  private final HierarchicalMapById<Route> routesById = new HierarchicalMapById<>();
  private final HierarchicalMapById<OperatingDay> operatingDaysById = new HierarchicalMapById<>();
  private final ArrayListMultimap<String, DatedServiceJourney> datedServiceJourneyBySjId = ArrayListMultimap.create();

  private final EntityById<org.opentripplanner.model.Route> otpRouteByid = new EntityById<>();

  NetexTestDataSample() {
    final int[] stopTimes = { 0, 4, 10, 15 };
    final int NUM_OF_STOPS = stopTimes.length;

    Line line = new Line()
      .withId("RUT:Line:1")
      .withName(new MultilingualString().withValue("Line 1"))
      .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS);
    JAXBElement<LineRefStructure> lineRef = createWrappedRef(line.getId(), LineRefStructure.class);

    // Add OTP Route (correspond to Netex Line)
    {
      org.opentripplanner.model.Route otpRoute = new org.opentripplanner.model.Route(
        ID_FACTORY.createId(line.getId())
      );
      otpRouteByid.add(otpRoute);
    }

    // Add Netex Route (not the same as an OTP Route)
    String routeId = "RUT:Route:1";
    routesById.add(new Route().withId(routeId).withLineRef(lineRef));
    RouteRefStructure routeRef = new RouteRefStructure().withRef(routeId);

    // Create timetable with 4 stops using the stopTimes above
    for (int i = 0; i < NUM_OF_STOPS; i++) {
      timetabledPassingTimes.add(createTimetablePassingTime("TTPT-" + (i + 1), 5, stopTimes[i]));
    }

    final String DESTINATION_DISPLAY_ID_1 = "NSR:DestinationDisplay:1";
    final String DESTINATION_DISPLAY_ID_2 = "NSR:DestinationDisplay:2";

    DestinationDisplay destinationBergen = new DestinationDisplay()
      .withId(DESTINATION_DISPLAY_ID_1)
      .withVias(
        new Vias_RelStructure()
          .withVia(List.of(this.createViaDestinationDisplayRef(DESTINATION_DISPLAY_ID_2)))
      )
      .withFrontText(new MultilingualString().withValue("Bergen"));

    DestinationDisplay destinationStavanger = new DestinationDisplay()
      .withId(DESTINATION_DISPLAY_ID_2)
      .withVias(
        new Vias_RelStructure()
          .withVia(List.of(this.createViaDestinationDisplayRef(DESTINATION_DISPLAY_ID_1)))
      )
      .withFrontText(new MultilingualString().withValue("Stavanger"));

    destinationDisplayById.add(destinationBergen);
    destinationDisplayById.add(destinationStavanger);

    List<PointInLinkSequence_VersionedChildStructure> pointsInLink = new ArrayList<>();

    for (int i = 0; i < NUM_OF_STOPS; i++) {
      String stopPointId = "RUT:StopPointInJourneyPattern:" + (i + 1);
      StopPointInJourneyPattern stopPoint = new StopPointInJourneyPattern()
        .withId(stopPointId)
        .withOrder(BigInteger.valueOf(i + 1))
        .withScheduledStopPointRef(createScheduledStopPointRef(stopPointId));

      if (i == 0) stopPoint.setDestinationDisplayRef(
        createDestinationDisplayRef(destinationBergen.getId()).getValue()
      );
      if (i == 2) stopPoint.setDestinationDisplayRef(
        createDestinationDisplayRef(destinationStavanger.getId()).getValue()
      );

      pointsInLink.add(stopPoint);
      timetabledPassingTimes.get(i).setPointInJourneyPatternRef(createStopPointRef(stopPointId));
    }

    // Create Journey Pattern with route and points
    journeyPattern =
      new JourneyPattern()
        .withId("RUT:JourneyPattern:1")
        .withRouteRef(routeRef)
        .withPointsInSequence(
          new PointsInJourneyPattern_RelStructure()
            .withPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern(
              pointsInLink
            )
        );
    journeyPatternById.add(journeyPattern);

    // Create a new Service Journey with line, dayType, journeyPattern and timetable from above
    {
      ServiceJourney serviceJourney = new ServiceJourney()
        .withId(SERVICE_JOURNEY_ID)
        .withLineRef(lineRef)
        .withDayTypes(createEveryDayRefs())
        .withJourneyPatternRef(createJourneyPatternRef(journeyPattern.getId()))
        .withPassingTimes(
          new TimetabledPassingTimes_RelStructure()
            .withTimetabledPassingTime(timetabledPassingTimes)
        );
      serviceJourneyById.add(serviceJourney);

      for (int i = 0; i < DATED_SERVICE_JOURNEY_ID.size(); i++) {
        OperatingDay operatingDay = new OperatingDay()
          .withId(OPERATING_DAYS.get(i))
          .withCalendarDate(
            LocalDate
              .parse(OPERATING_DAYS.get(i), DateTimeFormatter.ofPattern("yyyy-MM-dd"))
              .atStartOfDay()
          );
        operatingDaysById.add(operatingDay);

        DatedServiceJourney datedServiceJourney = new DatedServiceJourney()
          .withId(DATED_SERVICE_JOURNEY_ID.get(i))
          .withServiceAlteration(ServiceAlterationEnumeration.PLANNED)
          .withOperatingDayRef(new OperatingDayRefStructure().withRef(operatingDay.getId()));

        datedServiceJourneyBySjId.put(SERVICE_JOURNEY_ID, datedServiceJourney);
      }
    }

    // Setup stops
    for (int i = 0; i < NUM_OF_STOPS; i++) {
      String stopId = "NSR:Quay:" + (i + 1);
      stopsById.add(Stop.stopForTest(stopId, 60.0, 10.0));
      quayIdByStopPointRef.add(pointsInLink.get(i).getId(), stopId);
    }
  }

  static DayTypeRefs_RelStructure createEveryDayRefs() {
    return new DayTypeRefs_RelStructure()
      .withDayTypeRef(Collections.singleton(createEveryDayRef()));
  }

  HierarchicalMapById<DestinationDisplay> getDestinationDisplayById() {
    return destinationDisplayById;
  }

  EntityById<Stop> getStopsById() {
    return stopsById;
  }

  HierarchicalMap<String, String> getQuayIdByStopPointRef() {
    return quayIdByStopPointRef;
  }

  JourneyPattern getJourneyPattern() {
    return journeyPattern;
  }

  List<TimetabledPassingTime> getTimetabledPassingTimes() {
    return timetabledPassingTimes;
  }

  HierarchicalMapById<ServiceJourney> getServiceJourneyById() {
    return serviceJourneyById;
  }

  HierarchicalMapById<Route> getRouteById() {
    return routesById;
  }

  HierarchicalMapById<JourneyPattern> getJourneyPatternById() {
    return journeyPatternById;
  }

  EntityById<org.opentripplanner.model.Route> getOtpRouteByid() {
    return otpRouteByid;
  }

  HierarchicalMapById<OperatingDay> getOperatingDaysById() {
    return operatingDaysById;
  }

  ArrayListMultimap<String, DatedServiceJourney> getDatedServiceJourneyBySjId() {
    return datedServiceJourneyBySjId;
  }

  private static TimetabledPassingTime createTimetablePassingTime(String id, int hh, int mm) {
    return new TimetabledPassingTime().withId(id).withDepartureTime(LocalTime.of(hh, mm));
  }

  /* private static utility methods */

  private static JAXBElement<ScheduledStopPointRefStructure> createScheduledStopPointRef(
    String id
  ) {
    return createWrappedRef(id, ScheduledStopPointRefStructure.class);
  }

  private static JAXBElement<StopPointInJourneyPatternRefStructure> createStopPointRef(String id) {
    return createWrappedRef(id, StopPointInJourneyPatternRefStructure.class);
  }

  private static JAXBElement<JourneyPatternRefStructure> createJourneyPatternRef(String id) {
    return createWrappedRef(id, JourneyPatternRefStructure.class);
  }

  private static JAXBElement<DestinationDisplayRefStructure> createDestinationDisplayRef(
    String id
  ) {
    return createWrappedRef(id, DestinationDisplayRefStructure.class);
  }

  private static JAXBElement<DayTypeRefStructure> createEveryDayRef() {
    return createJaxbElement(new DayTypeRefStructure().withRef(EVERYDAY.getId()));
  }

  private Via_VersionedChildStructure createViaDestinationDisplayRef(String destinationDisplayId) {
    return new Via_VersionedChildStructure()
      .withDestinationDisplayRef(
        new DestinationDisplayRefStructure().withRef(destinationDisplayId)
      );
  }
}
