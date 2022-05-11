package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.BitSet;
import java.util.Objects;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.ConstrainedBoardingSearch;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.TransferForPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.constrainedtransfer.TransferForPatternByStopPos;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

public class TripPatternWithRaptorStopIndexes {

  private final TripPattern pattern;
  private final int[] stopIndexes;
  private final BitSet boardingPossible;
  private final BitSet alightingPossible;
  private final BitSet wheelchairAccessible;

  /**
   * List of transfers TO this pattern for each stop position in pattern used by Raptor during the
   * FORWARD search.
   */
  private final TransferForPatternByStopPos constrainedTransfersForwardSearch = new TransferForPatternByStopPos();

  /**
   * List of transfers FROM this pattern for each stop position in pattern used by Raptor during the
   * REVERSE search.
   */
  private final TransferForPatternByStopPos constrainedTransfersReverseSearch = new TransferForPatternByStopPos();

  /**
   * We only want to add constrained transfers once, in order not to get duplicates, and to avoid
   * modifying the lists while they are potentially in use in the router. This flag is set to true
   * after the transfers have been looped through once.
   */
  private boolean sealedConstrainedTransfers = false;

  public TripPatternWithRaptorStopIndexes(TripPattern pattern, int[] stopIndexes) {
    this.pattern = pattern;
    this.stopIndexes = stopIndexes;

    final int nStops = stopIndexes.length;
    boardingPossible = new BitSet(nStops);
    alightingPossible = new BitSet(nStops);
    wheelchairAccessible = new BitSet(nStops);

    for (int s = 0; s < nStops; s++) {
      boardingPossible.set(s, pattern.canBoard(s));
      alightingPossible.set(s, pattern.canAlight(s));
      wheelchairAccessible.set(s, pattern.wheelchairAccessible(s));
    }
  }

  public FeedScopedId getId() {
    return pattern.getId();
  }

  public TransitMode getTransitMode() {
    return pattern.getMode();
  }

  public String getNetexSubmode() {
    return pattern.getNetexSubmode();
  }

  public int[] getStopIndexes() {
    return stopIndexes;
  }

  public final TripPattern getPattern() {
    return this.pattern;
  }

  public BitSet getBoardingPossible() {
    return boardingPossible;
  }

  public BitSet getAlightingPossible() {
    return alightingPossible;
  }

  public BitSet getWheelchairAccessible() {
    return wheelchairAccessible;
  }

  /**
   * See {@link RaptorTripPattern#stopIndex(int)}
   */
  public int stopIndex(int stopPositionInPattern) {
    return stopIndexes[stopPositionInPattern];
  }

  public RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> constrainedTransferForwardSearch() {
    return new ConstrainedBoardingSearch(true, constrainedTransfersForwardSearch);
  }

  public RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> constrainedTransferReverseSearch() {
    return new ConstrainedBoardingSearch(false, constrainedTransfersReverseSearch);
  }

  public boolean boardingPossibleAt(int stopPositionInPattern) {
    return boardingPossible.get(stopPositionInPattern);
  }

  public boolean alightingPossibleAt(int stopPositionInPattern) {
    return alightingPossible.get(stopPositionInPattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TripPatternWithRaptorStopIndexes that = (TripPatternWithRaptorStopIndexes) o;
    return getId() == that.getId();
  }

  @Override
  public String toString() {
    return "TripPattern{" + "id=" + getId() + ", transitMode=" + pattern.getMode() + '}';
  }

  /**
   * This is public to allow the mappers to inject transfers
   */
  public void addTransferConstraintsForwardSearch(
    int targetStopPosition,
    TransferForPattern transferForPattern
  ) {
    if (sealedConstrainedTransfers) {
      return;
    }

    constrainedTransfersForwardSearch.add(targetStopPosition, transferForPattern);
  }

  /**
   * This is public to allow the mappers to inject transfers
   */
  public void addTransferConstraintsReverseSearch(
    int targetStopPosition,
    TransferForPattern transferForPattern
  ) {
    if (sealedConstrainedTransfers) {
      return;
    }

    constrainedTransfersReverseSearch.add(targetStopPosition, transferForPattern);
  }

  /**
   * This method should be called AFTER all transfers are added, and before the pattern is used in a
   * Raptor search. This is done to protect against modification when processing real-time updates
   * <p>
   * {@link TripPatternWithRaptorStopIndexes#sealedConstrainedTransfers}
   */
  public void sealConstrainedTransfers() {
    if (sealedConstrainedTransfers) {
      return;
    }

    constrainedTransfersForwardSearch.sortOnSpecificityRanking();
    constrainedTransfersReverseSearch.sortOnSpecificityRanking();
    sealedConstrainedTransfers = true;
  }
}
