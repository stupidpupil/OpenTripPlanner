package org.opentripplanner.routing;

import com.google.common.collect.MinMaxPriorityQueue;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Queue;

import static org.opentripplanner.model.PickDrop.NONE;

public class StopTimesHelper {
  /**
   * Fetch upcoming vehicle departures from a stop.
   * It goes though all patterns passing the stop for the previous, current and next service date.
   * It uses a priority queue to keep track of the next departures. The queue is shared between all
   * dates, as services from the previous service date can visit the stop later than the current
   * service date's services. This happens eg. with sleeper trains.
   *
   * TODO: Add frequency based trips
   * @param stop Stop object to perform the search for
   * @param startTime Start time for the search. Seconds from UNIX epoch
   * @param timeRange Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures Number of departures to fetch per pattern
   * @param omitNonPickups If true, do not include vehicles that will not pick up passengers.
   * @param includeCancelledTrips If true, cancelled trips will also be included in result
   */
  public static List<StopTimesInPattern> stopTimesForStop(
      RoutingService routingService,
      TimetableSnapshot timetableSnapshot,
      Stop stop,
      long startTime,
      int timeRange,
      int numberOfDepartures,
      boolean omitNonPickups,
      boolean includeCancelledTrips
  ) {

    //
    // TODO: SIRI: Add support for including cancelled trips
    //


    if (startTime == 0) {
      startTime = System.currentTimeMillis() / 1000;
    }
    List<StopTimesInPattern> ret = new ArrayList<>();
    Date date = new Date(startTime * 1000);
    ServiceDate[] serviceDates = {new ServiceDate(date).previous(), new ServiceDate(date), new ServiceDate(date).next()};

    Collection<TripPattern> patternsForStop = routingService.getPatternsForStop(stop, true);

    for (TripPattern pattern : patternsForStop) {

      Queue<TripTimeOnDate> pq = listTripTimeShortsForPatternAtStop(
          routingService,
          timetableSnapshot,
          stop,
          pattern,
          startTime,
          timeRange,
          numberOfDepartures,
          omitNonPickups,
          serviceDates
      );

      if (pq.size() != 0) {
        StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
        while (pq.size() != 0) {
          stopTimes.times.add(0, pq.poll());
        }
        ret.add(stopTimes);
      }
    }
    return ret;
  }

  /**
   * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when creating complete stop
   * timetables for a single day.
   *
   * @param stop Stop object to perform the search for
   * @param serviceDate Return all departures for the specified date
   */
  public static List<StopTimesInPattern> stopTimesForStop(
      RoutingService routingService,
      Stop stop,
      ServiceDate serviceDate,
      boolean omitNonPickups
  ) {
    List<StopTimesInPattern> ret = new ArrayList<>();

    Collection<TripPattern> patternsForStop = routingService.getPatternsForStop(stop, true);
    for (TripPattern pattern : patternsForStop) {
      StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
      Timetable tt;
      TimetableSnapshot timetableSnapshot = routingService.getTimetableSnapshot();
      if (timetableSnapshot != null){
        tt = timetableSnapshot.resolve(pattern, serviceDate);
      } else {
        tt = pattern.getScheduledTimetable();
      }
      ServiceDay sd = new ServiceDay(routingService.getServiceCodes(), serviceDate, routingService.getCalendarService(), pattern
          .getRoute()
          .getAgency().getId());
      int sidx = 0;
      for (Stop currStop : pattern.getStopPattern().getStops()) {
        if (currStop == stop) {
          if(omitNonPickups && pattern.getStopPattern().getPickup(sidx) == NONE) continue;
          for (TripTimes t : tt.getTripTimes()) {
            if (!sd.serviceRunning(t.getServiceCode())) { continue; }
            stopTimes.times.add(new TripTimeOnDate(t, sidx, stop, sd));
          }
        }
        sidx++;
      }
      ret.add(stopTimes);
    }
    return ret;
  }

  /**
   * Fetch upcoming vehicle departures from a stop for a single pattern, passing the stop for the previous, current and
   * next service date. It uses a priority queue to keep track of the next departures. The queue is shared between all
   * dates, as services from the previous service date can visit the stop later than the current service date's
   * services.
   *
   * TODO: Add frequency based trips
   * @param stop Stop object to perform the search for
   * @param pattern Pattern object to perform the search for
   * @param startTime Start time for the search. Seconds from UNIX epoch
   * @param timeRange Searches forward for timeRange seconds from startTime
   * @param numberOfDepartures Number of departures to fetch per pattern
   * @param omitNonPickups If true, do not include vehicles that will not pick up passengers.
   */
  public static List<TripTimeOnDate> stopTimesForPatternAtStop(
          RoutingService routingService,
          TimetableSnapshot timetableSnapshot,
          Stop stop,
          TripPattern pattern,
          long startTime,
          int timeRange,
          int numberOfDepartures,
          boolean omitNonPickups) {

    if (startTime == 0) {
      startTime = System.currentTimeMillis() / 1000;
    }
    Date date = new Date(startTime * 1000);
    ServiceDate[] serviceDates = {new ServiceDate(date).previous(), new ServiceDate(date), new ServiceDate(date).next()};
    Queue<TripTimeOnDate> pq = listTripTimeShortsForPatternAtStop(
        routingService,
        timetableSnapshot,
        stop,
        pattern,
        startTime,
        timeRange,
        numberOfDepartures,
        omitNonPickups,
        serviceDates
    );

    return new ArrayList<>(pq);
  }

  private static Queue<TripTimeOnDate> listTripTimeShortsForPatternAtStop(
      RoutingService routingService,
      TimetableSnapshot timetableSnapshot,
      Stop stop,
      TripPattern pattern,
      long startTime,
      int timeRange,
      int numberOfDepartures,
      boolean omitNonPickups,
      ServiceDate[] serviceDates
  ) {

    // The bounded priority Q is used to keep a sorted short list of trip times. We can not
    // relay on the trip times to be in order because of real-time updates. This code can
    // probably be optimized, and the trip search in the Raptor search does almost the same
    // thing. This is no part of a routing request, but is a used frequently in some
    // operation like Entur for "departure boards" (apps, widgets, screens on platforms, and
    // hotel lobbies). Setting the numberOfDepartures and timeRange to a big number for a
    // transit hub could result in a DOS attack, but there are probably other more effective
    // ways to do it.
    //
    // The {@link MinMaxPriorityQueue} is marked beta, but we do not have a god alternative.
    MinMaxPriorityQueue<TripTimeOnDate> pq = MinMaxPriorityQueue
            .orderedBy(Comparator.comparing((TripTimeOnDate tts) -> tts.getServiceDay()
                + tts.getRealtimeDeparture()))
            .maximumSize(numberOfDepartures)
            .create();

    // Loop through all possible days
    for (ServiceDate serviceDate : serviceDates) {
      ServiceDay sd = new ServiceDay(routingService.getServiceCodes(), serviceDate, routingService.getCalendarService(), pattern
          .getRoute()
          .getAgency().getId());
      Timetable tt;
      if (timetableSnapshot != null) {
        tt = timetableSnapshot.resolve(pattern, serviceDate);
      } else {
        tt = pattern.getScheduledTimetable();
      }

      if (!tt.temporallyViable(sd, startTime, timeRange, true)) continue;

      int secondsSinceMidnight = sd.secondsSinceMidnight(startTime);
      int sidx = 0;
      for (Stop currStop : pattern.getStopPattern().getStops()) {
        if (currStop == stop) {
          if (omitNonPickups && pattern.getStopPattern().getPickup(sidx) == NONE) continue;
          for (TripTimes t : tt.getTripTimes()) {
            if (!sd.serviceRunning(t.getServiceCode())) continue;
            if (t.getDepartureTime(sidx) != -1 &&
                    t.getDepartureTime(sidx) >= secondsSinceMidnight) {
              pq.add(new TripTimeOnDate(t, sidx, stop, sd));
            }
          }

          // TODO: This needs to be adapted after #1647 is merged
          for (FrequencyEntry freq : tt.getFrequencyEntries()) {
            if (!sd.serviceRunning(freq.tripTimes.getServiceCode())) continue;
            int departureTime = freq.nextDepartureTime(sidx, secondsSinceMidnight);
            if (departureTime == -1) continue;
            int lastDeparture = freq.endTime + freq.tripTimes.getArrivalTime(sidx) -
                    freq.tripTimes.getDepartureTime(0);
            int i = 0;
            while (departureTime <= lastDeparture && i < numberOfDepartures) {
              pq.add(
                      new TripTimeOnDate(
                              freq.materialize(sidx, departureTime, true),
                              sidx,
                              stop,
                              sd
                      )
              );
              departureTime += freq.headway;
              i++;
            }
          }
        }
        sidx++;
      }
    }
    return pq;
  }
}
