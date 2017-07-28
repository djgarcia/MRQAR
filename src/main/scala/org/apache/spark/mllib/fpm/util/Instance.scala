package org.apache.spark.mllib.fpm.util

import scala.Vector;

class Instance(Pattrs: Array[Double]) {

  /** attributes */
  var attrs: Array[Double] = Pattrs;

  /**
   * Return the attribute at the specified position
   *
   * @param index
   *          position of the attribute to retrieve
   * @return value of the attribute
   */
  def get(index: Int): Double = {
    return attrs(index);
  }

  /**
   * Return all the input values.
   * @return a double[] with all input values
   */
  def get(): Array[Double] = {
    return attrs.clone();
  }

  /**
   * Set the value at the given index
   *
   * @param value
   *          a double value to set
   */
  def set(index: Int, value: Double) {
    attrs(index) = value;
  }

  def equal(obj: Object): Boolean = {
    if (this == obj) {
      return true;
    }
    if (!(obj.isInstanceOf[Instance])) {
      return false;
    }

    var instance: Instance = obj.asInstanceOf[Instance];

    return /*id == instance.id &&*/ attrs.equals(instance.attrs);

  }

  override def hashCode(): Int = {
    return /*id +*/ attrs.hashCode();
  }
}
