package org.apache.spark.mllib.fpm.util

import java.io.Serializable

import org.apache.spark.mllib.fpm.MOPNAR.AssociationRule

class SupportRule(ant:Double,cons:Double,Sup:Double) extends Serializable {
  var ar: AssociationRule = null;
  var antsSupport: Double = 0;
  var consSupport: Double = 0;
  var support: Double = 0;
 }
