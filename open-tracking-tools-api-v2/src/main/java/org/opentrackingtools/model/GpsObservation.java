package org.opentrackingtools.model;

import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.VectorFactory;

import java.util.Date;

import com.google.common.collect.ComparisonChain;
import com.vividsolutions.jts.geom.Coordinate;

public class GpsObservation {

  private final String sourceId;
  private final Date timestamp;
  private final Coordinate coordsLatLon;
  private final Double velocity;
  private final Double heading;
  private final Double accuracy;
  private final int recordNumber;
  private final Vector projPoint;
  private GpsObservation prevObs;
  private final ProjectedCoordinate coordsProjected;

  public GpsObservation(String sourceId, Date timestamp,
    Coordinate coordsLatLon, Double velocity, Double heading,
    Double accuracy, int recordNumber, GpsObservation prevObs, 
    ProjectedCoordinate coordsProjected) {
    this.sourceId = sourceId;
    this.timestamp = timestamp;
    this.coordsLatLon = coordsLatLon;
    this.velocity = velocity;
    this.heading = heading;
    this.accuracy = accuracy;
    this.recordNumber = recordNumber;
    this.projPoint = VectorFactory.getDefault().createVector2D(
                      coordsProjected.x, coordsProjected.y);
    this.prevObs = prevObs;
    this.coordsProjected = coordsProjected;
  }
  
  public Double getAccuracy() {
    return accuracy;
  }
  
  public Double getHeading() {
    return heading;
  }

  public Coordinate getObsCoordsLatLon() {
    return coordsLatLon;
  }
  
  public Date getTimestamp() {
    return timestamp;
  }
  
  public Double getVelocity() {
    return velocity;
  }
  
  public ProjectedCoordinate getObsProjected() {
    return this.coordsProjected;
  }
  
  public GpsObservation getPreviousObservation() {
    return this.prevObs;
  }
  
  public Vector getProjectedPoint() {
    return this.projPoint;
  }
  
  public int getRecordNumber() {
    return this.recordNumber;
  }

  
  public String getSourceId() {
    return sourceId;
  }
  
  public int compareTo(GpsObservation o) {
    return ComparisonChain.start()
        .compare(this.timestamp, o.getTimestamp())
        .compare(this.sourceId, o.getSourceId()).result();
  }
  
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final GpsObservation other = (GpsObservation) obj;
    if (timestamp == null) {
      if (other.timestamp != null) {
        return false;
      }
    } else if (!timestamp.equals(other.timestamp)) {
      return false;
    }
    if (sourceId == null) {
      if (other.sourceId != null) {
        return false;
      }
    } else if (!sourceId.equals(other.sourceId)) {
      return false;
    }
    return true;
  }

  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime
            * result
            + ((timestamp == null) ? 0 : timestamp
                .hashCode());
    result =
        prime
            * result
            + ((sourceId == null) ? 0 : sourceId
                .hashCode());
    return result;
  }
  
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("SimpleObservation [sourceId=").append(sourceId)
        .append(", timestamp=").append(timestamp)
        .append(", coordsLatLon=").append(coordsLatLon)
        .append(", recordNumber=").append(recordNumber)
        .append(", coordsProjected=").append(coordsProjected)
        .append("]");
    return builder.toString();
  }

  public void reset() {
    this.prevObs = null;
  }

}