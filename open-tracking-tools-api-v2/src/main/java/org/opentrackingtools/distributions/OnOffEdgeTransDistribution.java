package org.opentrackingtools.distributions;

import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.math.matrix.VectorFactory;
import gov.sandia.cognition.math.matrix.mtj.DenseMatrix;
import gov.sandia.cognition.statistics.ClosedFormComputableDiscreteDistribution;
import gov.sandia.cognition.statistics.ClosedFormComputableDistribution;
import gov.sandia.cognition.statistics.DiscreteDistribution;
import gov.sandia.cognition.statistics.distribution.DirichletDistribution;
import gov.sandia.cognition.statistics.distribution.MultinomialDistribution;
import gov.sandia.cognition.util.AbstractCloneableSerializable;
import gov.sandia.cognition.util.ObjectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.opentrackingtools.estimators.MotionStateEstimatorPredictor;
import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.graph.InferenceGraphEdge;
import org.opentrackingtools.model.VehicleState;
import org.opentrackingtools.paths.PathState;
import org.opentrackingtools.util.StatisticsUtil;
import org.testng.collections.Sets;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

/**
 * Class representing the transition from one edge to another. For now we use
 * three transition types: 1. off-road to off-road/on-road to on-road 2.
 * off-road to on-road 3. on-road to off-road
 * 
 * @author bwillard
 * 
 */
public class OnOffEdgeTransDistribution extends
    AbstractCloneableSerializable implements
    ClosedFormComputableDiscreteDistribution<InferenceGraphEdge>,
    Comparable<OnOffEdgeTransDistribution> {

  private static final long serialVersionUID = -8329433263373783485L;

  static final Vector stateOffToOff = VectorFactory.getDefault()
      .copyValues(1d, 0d);

  static final Vector stateOffToOn = VectorFactory.getDefault()
      .copyValues(0d, 1d);

  static final Vector stateOnToOff = VectorFactory.getDefault()
      .copyValues(0d, 1d);
  static final Vector stateOnToOn = VectorFactory.getDefault()
      .copyValues(1d, 0d);
  static final Double zeroTolerance = 1e-6;

  /**
   * This method restricts sampling when probabilities have collapsed on one
   * value in the domain. It allows us to emulate a deterministic distribution
   * and eliminate on- or off-roading. Also, it prevents Foundry's gamma sampler
   * from taking forever.
   * 
   * @param dist
   * @param rng
   * @return
   */
  public static Vector checkedSample(
    final ClosedFormComputableDistribution<Vector> dist, Random rng) {

    if (dist instanceof ClosedFormComputableDiscreteDistribution) {
      final ClosedFormComputableDiscreteDistribution<Vector> distDiscrete =
          (ClosedFormComputableDiscreteDistribution<Vector>) dist;
      final Vector one =
          Iterators.find(distDiscrete.getDomain().iterator(),
              new Predicate<Vector>() {
                @Override
                public boolean apply(Vector input) {
                  return Math.abs(1 - dist.getProbabilityFunction()
                      .evaluate(input)) <= OnOffEdgeTransDistribution.zeroTolerance;
                }
              }, null);

      return one == null ? dist.sample(rng) : one;

    } else if (dist instanceof DirichletDistribution) {
      for (int i = 0; i < dist.getMean().getDimensionality(); i++) {
        final double prob = dist.getMean().getElement(i);
        if (Double.isNaN(prob)
            || Math.abs(1 - prob) <= OnOffEdgeTransDistribution.zeroTolerance) {
          final Vector result =
              VectorFactory.getDefault().createVector(
                  dist.getMean().getDimensionality());
          result.setElement(i, 1d);
          return result;
        }
      }
    }

    return dist.sample(rng);
  }

  public static Vector getStateOffToOff() {
    return OnOffEdgeTransDistribution.stateOffToOff;
  }

  public static Vector getStateOffToOn() {
    return OnOffEdgeTransDistribution.stateOffToOn;
  }

  public static Vector getStateOnToOff() {
    return OnOffEdgeTransDistribution.stateOnToOff;
  }

  public static Vector getStateOnToOn() {
    return OnOffEdgeTransDistribution.stateOnToOn;
  }

  public static Vector getTransitionType(InferenceGraphEdge from,
    InferenceGraphEdge to) {
    if (from.isNullEdge()) {
      if (to.isNullEdge()) {
        return OnOffEdgeTransDistribution.stateOffToOff;
      } else {
        return OnOffEdgeTransDistribution.stateOffToOn;
      }
    } else {
      if (!to.isNullEdge()) {
        return OnOffEdgeTransDistribution.stateOnToOn;
      } else {
        return OnOffEdgeTransDistribution.stateOnToOff;
      }
    }
  }

  protected InferenceGraphEdge currentEdge;

  protected VehicleState<?> currentState;

  protected Set<InferenceGraphEdge> domain = null;

  /*
   * Distribution corresponding to edge-movement -> free-movement and
   * edge-movement -> edge-movement
   */
  protected MultinomialDistribution edgeMotionTransProbs =
      new MultinomialDistribution(2, 1);

  /*
   * Distribution corresponding to free-movement -> free-movement and
   * free-movement -> edge-movement
   */
  protected MultinomialDistribution freeMotionTransProbs =
      new MultinomialDistribution(2, 1);

  protected InferenceGraph graph;

  public OnOffEdgeTransDistribution(
    OnOffEdgeTransDistribution onOffEdgeTransProbsDistribution) {
    this.currentEdge = onOffEdgeTransProbsDistribution.currentEdge;
    this.currentState = onOffEdgeTransProbsDistribution.currentState;
    this.edgeMotionTransProbs =
        onOffEdgeTransProbsDistribution.edgeMotionTransProbs;
    this.freeMotionTransProbs =
        onOffEdgeTransProbsDistribution.freeMotionTransProbs;
  }

  public OnOffEdgeTransDistribution(VehicleState<?> currentState,
    InferenceGraphEdge currentEdge, Vector edgeMotionProbs,
    Vector freeMotionProbs) {

    this.currentState = currentState;
    this.graph = currentState.getGraph();
    this.currentEdge = currentEdge;
    
    /*
     * Start by setting the means.
     */
    this.getFreeMotionTransProbs().setParameters(freeMotionProbs);
    this.getEdgeMotionTransProbs().setParameters(edgeMotionProbs);
  }

  @Override
  public OnOffEdgeTransDistribution clone() {
    final OnOffEdgeTransDistribution transDist =
        (OnOffEdgeTransDistribution) super.clone();
    transDist.edgeMotionTransProbs =
        this.getEdgeMotionTransProbs().clone();
    transDist.freeMotionTransProbs =
        this.getFreeMotionTransProbs().clone();
    transDist.currentEdge = ObjectUtil.cloneSmart(this.currentEdge);
    transDist.graph = this.graph;
    return transDist;
  }

  @Override
  public int compareTo(OnOffEdgeTransDistribution o) {
    final CompareToBuilder comparator = new CompareToBuilder();
    comparator.append(this.getEdgeMotionTransProbs().getParameters()
        .toArray(), o.getEdgeMotionTransProbs().getParameters()
        .toArray());
    comparator.append(this.getFreeMotionTransProbs().getParameters()
        .toArray(), o.getFreeMotionTransProbs().getParameters()
        .toArray());
    comparator.append(this.currentEdge, o.currentEdge);
    return comparator.toComparison();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    final OnOffEdgeTransDistribution other =
        (OnOffEdgeTransDistribution) obj;
    if (this.getEdgeMotionTransProbs() == null) {
      if (other.getEdgeMotionTransProbs() != null) {
        return false;
      }
    } else if (!this.getEdgeMotionTransProbs().getParameters()
        .equals(other.getEdgeMotionTransProbs().getParameters())) {
      return false;
    }
    if (this.getFreeMotionTransProbs() == null) {
      if (other.getFreeMotionTransProbs() != null) {
        return false;
      }
    } else if (!this.getFreeMotionTransProbs().getParameters()
        .equals(other.getFreeMotionTransProbs().getParameters())) {
      return false;
    }
    if (this.currentEdge == null) {
      if (other.currentEdge != null) {
        return false;
      }
    } else if (!this.currentEdge.equals(other.currentEdge)) {
      return false;
    }
    return true;
  }
  
  private void getEdgesForLength(InferenceGraphEdge startEdge, double lengthToTravel, Collection<InferenceGraphEdge> edges) {
    if (startEdge.getLength() < lengthToTravel) {
      edges.add(startEdge);
    } else {
      if (lengthToTravel > 0d) {
        for (InferenceGraphEdge edge : this.graph.getOutgoingTransferableEdges(startEdge)) {
          getEdgesForLength(edge, lengthToTravel - startEdge.getLength(), edges);
        }
      } else {
        for (InferenceGraphEdge edge : this.graph.getIncomingTransferableEdges(startEdge)) {
          getEdgesForLength(edge, lengthToTravel + startEdge.getLength(), edges);
        }
      }
    }
  }

  /**
   * Returns the set of possible transition edges given
   * the current state.  <br>
   * Note: it is acceptable that the motion state of the
   * current state be before or past the current edge, in
   * which case the next edges are possible transitions.
   * Otherwise, only the current edge and off-road are possible. 
   * An internal value of initial distance-along-path is kept in
   * order to recompute the domain when applicable.
   * 
   */
  @Override
  public Set<InferenceGraphEdge> getDomain() {
    if (this.domain == null) {
      if (this.currentEdge.isNullEdge()) {
        this.domain = Sets.newHashSet();
        final Vector currentLocation = this.currentState.getMeanLocation();
        final double radius =
            StatisticsUtil
                .getLargeNormalCovRadius((DenseMatrix) 
                    currentState.getObservationCovarianceParam().getValue());
        for (final InferenceGraphEdge edge : this.graph.getNearbyEdges(currentLocation, radius)) {
          this.domain.add(edge);
        }
        // add the off-road possibility
        this.domain.add(this.currentEdge);
      } else {
        Vector currentMotionState = this.currentState.getMotionStateParam().getParameterPrior() .getMean();
        this.getEdgesForLength(currentEdge, currentMotionState.getElement(0), this.domain);
        // add the off-road possibility
        this.domain.add(InferenceGraphEdge.nullGraphEdge);
      }
    }
    return this.domain;
  }

  @Override
  public int getDomainSize() {
    return this.getDomain().size();
  }

  public MultinomialDistribution getEdgeMotionTransProbs() {
    return this.edgeMotionTransProbs;
  }

  public MultinomialDistribution getFreeMotionTransProbs() {
    return this.freeMotionTransProbs;
  }

  @Override
  public OnOffEdgeTransProbabilityFunction getProbabilityFunction() {
    return new OnOffEdgeTransProbabilityFunction(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime
            * result
            + ((this.getEdgeMotionTransProbs() == null) ? 0 : this
                .getEdgeMotionTransProbs().getParameters().hashCode());
    result =
        prime
            * result
            + ((this.getFreeMotionTransProbs() == null) ? 0 : this
                .getFreeMotionTransProbs().getParameters().hashCode());
    result =
        prime
            * result
            + ((this.currentEdge == null) ? 0 : this.currentEdge
                .hashCode());
    return result;
  }

  @Override
  public InferenceGraphEdge sample(Random random) {

    final List<InferenceGraphEdge> tmpDomain =
        Lists.newArrayList(this.getDomain());

    if (this.currentEdge.isNullEdge()) {
      /*
       * We're currently in free-motion. If there are transfer edges, then
       * sample from those.
       */
      if (tmpDomain.isEmpty()) {
        return InferenceGraphEdge.nullGraphEdge;
      } else {
        final Vector sample =
            OnOffEdgeTransDistribution.checkedSample(
                this.getFreeMotionTransProbs(), random);

        if (sample.equals(OnOffEdgeTransDistribution.stateOffToOn)
            && tmpDomain.size() > 0) {
          tmpDomain.remove(InferenceGraphEdge.nullGraphEdge);
          return tmpDomain.get(random.nextInt(tmpDomain.size()));
        } else {
          return InferenceGraphEdge.nullGraphEdge;
        }
      }
    } else {
      /*
       * We're on an edge, so sample whether we go off-road, or transfer/stay on.
       * If the empty edge is contained in the support then we sample for that.
       */
      final Vector sample =
          tmpDomain.contains(InferenceGraphEdge.nullGraphEdge) ? OnOffEdgeTransDistribution
              .checkedSample(this.getEdgeMotionTransProbs(), random)
              : OnOffEdgeTransDistribution.stateOnToOn;

      if (sample.equals(OnOffEdgeTransDistribution.stateOnToOff)
          || tmpDomain.isEmpty()) {
        return InferenceGraphEdge.nullGraphEdge;
      } else {
        tmpDomain.remove(InferenceGraphEdge.nullGraphEdge);
        return tmpDomain.get(random.nextInt(tmpDomain.size()));
      }
    }
  }

  @Override
  public ArrayList<InferenceGraphEdge> sample(Random random,
    int numSamples) {
    final ArrayList<InferenceGraphEdge> samples =
        Lists.newArrayList();
    for (int i = 0; i < numSamples; i++) {
      samples.add(this.sample(random));
    }
    return samples;
  }

  public void setEdgeMotionTransProbs(Vector edgeMotionTransProbs) {
    this.edgeMotionTransProbs.setParameters(edgeMotionTransProbs);
  }

  public void setFreeMotionTransProbs(Vector freeMotionTransProbs) {
    this.freeMotionTransProbs.setParameters(freeMotionTransProbs);
  }

  @Override
  public String toString() {
    return "EdgeTransitionDistributions [freeMotionTransProbs="
        + this.getFreeMotionTransProbs().getParameters()
        + ", edgeMotionTransProbs="
        + this.getEdgeMotionTransProbs().getParameters() + "]";
  }

  @Override
  public InferenceGraphEdge getMean() {
    if (this.currentEdge.isNullEdge()) {
      if (stateOffToOff.dotProduct(this.freeMotionTransProbs.getMean())
          > stateOffToOn.dotProduct(this.freeMotionTransProbs.getMean())) {
        return InferenceGraphEdge.nullGraphEdge;
      } else {
        return Iterables.find(this.getDomain(), new Predicate<InferenceGraphEdge>() {
          @Override
          public boolean apply(InferenceGraphEdge input) {
            return !input.isNullEdge();
          }
        }, InferenceGraphEdge.nullGraphEdge);
      }
    } else {
      if (stateOnToOn.dotProduct(this.edgeMotionTransProbs.getMean())
          > stateOnToOff.dotProduct(this.edgeMotionTransProbs.getMean())) {
        return Iterables.find(this.getDomain(), new Predicate<InferenceGraphEdge>() {
          @Override
          public boolean apply(InferenceGraphEdge input) {
            return !input.isNullEdge();
          }
        }, this.currentEdge);
      } else {
        return InferenceGraphEdge.nullGraphEdge;
      }
    }
  }

  @Deprecated
  @Override
  public Vector convertToVector() {
    return null;
  }

  @Deprecated
  @Override
  public void convertFromVector(Vector parameters) {
  }

}
