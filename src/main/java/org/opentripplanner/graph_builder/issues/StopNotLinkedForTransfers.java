package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public class StopNotLinkedForTransfers implements DataImportIssue {

  public static final String FMT = "Stop %s not near any other stops; no transfers are possible.";

  public static final String HTMLFMT =
    "Stop <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s&layers=T\">\"%s (%s)\"</a> not near any other stops; no transfers are possible.";

  final TransitStopVertex stop;

  public StopNotLinkedForTransfers(TransitStopVertex stop) {
    this.stop = stop;
  }

  @Override
  public String getMessage() {
    return String.format(FMT, stop);
  }

  @Override
  public String getHTMLMessage() {
    return String.format(
      HTMLFMT,
      stop.getLat(),
      stop.getLon(),
      stop.getDefaultName(),
      stop.getStop().getId()
    );
  }

  @Override
  public Vertex getReferencedVertex() {
    return this.stop;
  }
}
