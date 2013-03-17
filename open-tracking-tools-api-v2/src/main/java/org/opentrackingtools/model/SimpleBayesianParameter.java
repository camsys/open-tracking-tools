package org.opentrackingtools.model;

import java.util.Random;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import gov.sandia.cognition.statistics.ClosedFormDistribution;
import gov.sandia.cognition.statistics.bayesian.BayesianParameter;
import gov.sandia.cognition.statistics.distribution.MultivariateGaussian;
import gov.sandia.cognition.util.AbstractCloneableSerializable;
import gov.sandia.cognition.util.CloneableSerializable;
import gov.sandia.cognition.util.ObjectUtil;

public class SimpleBayesianParameter<ParameterType, ConditionalType extends ClosedFormDistribution<?>, PriorType extends ClosedFormDistribution<ParameterType>>
  extends AbstractCloneableSerializable
  implements BayesianParameter<ParameterType, ConditionalType, PriorType> {

  protected ConditionalType conditional;
  protected PriorType prior;
  protected ParameterType value;
  protected String name = null;
  
  @Override
  public SimpleBayesianParameter<ParameterType, ConditionalType, PriorType> clone() {
    SimpleBayesianParameter<ParameterType, ConditionalType, PriorType> clone = 
        (SimpleBayesianParameter<ParameterType, ConditionalType, PriorType>) super.clone();
    clone.conditional = ObjectUtil.cloneSmart(this.conditional);
    clone.prior = ObjectUtil.cloneSmart(this.prior);
    clone.value = ObjectUtil.cloneSmart(this.value);
    clone.name = this.name;
    return clone;
  }
  
  public void setName(String name) {
    this.name = name;
  }

  public SimpleBayesianParameter(SimpleBayesianParameter<ParameterType, ConditionalType, PriorType> other) {
    super();
    this.conditional = other.conditional;
    this.prior = other.prior;
    this.value = other.value;
  }
  
  public static <Par, C extends ClosedFormDistribution<?>, P extends ClosedFormDistribution<Par>> 
    SimpleBayesianParameter<Par, C, P> create(Par value, C conditional, P prior) {
    return new SimpleBayesianParameter<Par, C, P>(conditional, prior, value);
  }

  public SimpleBayesianParameter(ConditionalType conditional,
    PriorType prior, ParameterType value) {
    super();
    this.conditional = conditional;
    this.prior = prior;
    this.value = value;
  }

  @Override
  public ConditionalType getConditionalDistribution() {
    return this.conditional;
  }
  
  public ConditionalType setConditionalDistribution(ConditionalType conditional) {
    return this.conditional = conditional;
  }

  @Override
  public void setValue(ParameterType value) {
    this.value = value;
  }

  @Override
  public ParameterType getValue() {
    return this.value;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public PriorType getParameterPrior() {
    return this.prior;
  }

  @Override
  public void updateConditionalDistribution(Random random) {
  }

  public void setParameterPrior(
    PriorType prior) {
    this.prior = prior;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result
            + ((conditional == null) ? 0 : conditional.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result =
        prime * result + ((prior == null) ? 0 : prior.hashCode());
    result =
        prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    SimpleBayesianParameter<?,?,?> other = (SimpleBayesianParameter<?,?,?> ) obj;
    if (conditional == null) {
      if (other.conditional != null)
        return false;
    } else if (!conditional.equals(other.conditional))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (prior == null) {
      if (other.prior != null)
        return false;
    } else if (!prior.equals(other.prior))
      return false;
    if (value == null) {
      if (other.value != null)
        return false;
    } else if (!value.equals(other.value))
      return false;
    return true;
  }

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
    builder.append("value", value);
    builder.append("conditional", conditional);
    builder.append("prior", prior);
    builder.append("name", name);
    return builder.toString();
  }

}
