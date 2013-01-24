package org.opentrackingtools.graph.paths.states.impl;

import java.util.List;

import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.util.ObjectUtil;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.opentrackingtools.graph.paths.InferredPath;
import org.opentrackingtools.graph.paths.edges.PathEdge;
import org.opentrackingtools.graph.paths.edges.impl.SimplePathEdge;
import org.opentrackingtools.graph.paths.impl.SimpleInferredPath;
import org.opentrackingtools.graph.paths.states.AbstractPathState;
import org.opentrackingtools.graph.paths.states.PathState;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.statistics.filters.vehicles.road.impl.AbstractRoadTrackingFilter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class SimplePathState extends AbstractPathState {

  private static final long serialVersionUID =
      2846671162796173049L;
  private Vector globalState;
  private Vector groundState;
  private Vector localState;
  private Vector rawState;

  protected SimplePathState(InferredPath path, Vector state) {

    this.path = path;
    this.rawState = state.clone();
    this.globalState = state.clone();
    /*
     * Now make sure the result is on this path.
     */
    if (!path.isNullPath()) {
      this.globalState.setElement(0,
          path.clampToPath(this.globalState.getElement(0)));
    }
  }

  @Override
  public SimplePathState clone() {
    final SimplePathState clone = (SimplePathState) super.clone();
    clone.rawState = ObjectUtil.cloneSmart(this.rawState);
    clone.localState =
        ObjectUtil.cloneSmart(this.localState);
    clone.globalState =
        ObjectUtil.cloneSmart(this.globalState);
    clone.groundState =
        ObjectUtil.cloneSmart(this.groundState);
    return clone;
  }

  @Override
  public int compareTo(PathState o) {
    final CompareToBuilder comparator =
        new CompareToBuilder();
    comparator.append(this.path, o.getPath());
    comparator.append(this.globalState, o.getGlobalState());
    comparator.append(this.rawState, o.getRawState());
    return comparator.toComparison();
  }

  @Override
  public PathEdge getEdge() {
    if (edge == null) {
      Preconditions.checkState(!path.isNullPath());
      this.edge = path.getEdgeForDistance(
                  globalState.getElement(0), false);
    }
    return Preconditions.checkNotNull(this.edge);
  }

  @Override
  public Vector getGlobalState() {
    return globalState;
  }

  @Override
  public Vector getGroundState() {
    if (this.groundState != null)
      return this.groundState;

    this.groundState =
        AbstractRoadTrackingFilter.convertToGroundState(
            this.globalState, this.getEdge(), true);

    return this.groundState;
  }

  @Override
  public Vector getLocalState() {
    if (this.localState != null)
      return this.localState;
    if (this.path.isNullPath()) {
      this.localState = this.globalState;
    } else {
      this.localState =
          this.getEdge().getCheckedStateOnEdge(
              this.globalState,
              AbstractRoadTrackingFilter
                  .getEdgeLengthErrorTolerance(), true);
    }
    return this.localState;
  }

  @Override
  public Vector getRawState() {
    return rawState;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("PathState [path=").append(path)
        .append(", state=").append(globalState).append("]");
    return builder.toString();
  }

  public static SimplePathState getPathState(
    @Nonnull InferredPath path, @Nonnull Vector state) {
    Preconditions.checkArgument(!path.isNullPath()
        || state.getDimensionality() == 4);
    return new SimplePathState(path, state);
  }

  /**
   * Returns a version of this state with a path ending
   * at the current edge.
   * @return
   */
  @Override
  public PathState getTruncatedPathStateBelief() {
    final List<PathEdge> newEdges = Lists.newArrayList();
    for (PathEdge edge1 : this.getPath().getEdges()) {
      newEdges.add(edge1);
      if (edge1.equals(this.getEdge())) {
        break;
      }
    }
    final SimpleInferredPath newPath = SimpleInferredPath.getInferredPath(newEdges, 
        this.getPath().getIsBackward());
    return new SimplePathState(newPath, this.rawState);
  }
}