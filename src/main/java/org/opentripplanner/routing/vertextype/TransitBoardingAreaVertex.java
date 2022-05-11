package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.BoardingArea;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.WheelchairBoarding;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

public class TransitBoardingAreaVertex extends Vertex {

  private static final long serialVersionUID = 1L;

  private final boolean wheelchairAccessible;

  private final BoardingArea boardingArea;

  /**
   * @param boardingArea The transit model boarding area reference.
   */
  public TransitBoardingAreaVertex(Graph graph, BoardingArea boardingArea) {
    super(
      graph,
      boardingArea.getId().toString(),
      boardingArea.getCoordinate().longitude(),
      boardingArea.getCoordinate().latitude(),
      boardingArea.getName()
    );
    this.boardingArea = boardingArea;
    this.wheelchairAccessible =
      boardingArea.getWheelchairBoarding() != WheelchairBoarding.NOT_POSSIBLE;
    //Adds this vertex into graph envelope so that we don't need to loop over all vertices
    graph.expandToInclude(
      boardingArea.getCoordinate().longitude(),
      boardingArea.getCoordinate().latitude()
    );
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }

  public BoardingArea getBoardingArea() {
    return this.boardingArea;
  }

  @Override
  public StationElement getStationElement() {
    return this.boardingArea;
  }
}
