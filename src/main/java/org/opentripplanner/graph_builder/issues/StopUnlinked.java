package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

public class StopUnlinked implements DataImportIssue {

  public static final String FMT = "Stop %s not near any streets; it will not be usable.";
  public static final String HTMLFMT =
    "Stop <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s&layers=T\">\"%s\" (%s)</a> not near any streets; it will not be usable.";

  final TransitStopVertex stop;

  public StopUnlinked(TransitStopVertex stop) {
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
      stop.getStop().getLat(),
      stop.getStop().getLon(),
      stop.getDefaultName(),
      stop.getStop().getId()
    );
  }

  @Override
  public Vertex getReferencedVertex() {
    return this.stop;
  }
}
