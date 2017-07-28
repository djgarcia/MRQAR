package org.apache.spark.mllib.fpm.MRQAR

import org.apache.spark.sql.{Dataset, Row, SparkSession}

import scala.collection.mutable.ListBuffer
class ConfigMopnar(dataset: Dataset[Row], nObj: Int, nTT: Int, nTA: Int, h: Int, t: Int, probD: Double, nS: Int, pM: Double, amp: Double, pU: Double, S: Long, l: List[Int], nc: Double,ss:SparkSession) extends Serializable {
  var data: Dataset[Row] = dataset;
  var NObjetives: Int = nObj;
  var nTrialsTotal = nTT;
  var nTrialAlg = nTA;
  var H: Int = h;
  var T: Int = t;
  var probDelta: Double = probD;
  var nSolution: Int = nS;
  var pMutation: Double = pM;
  var amplitude: Double = amp;
  var pUpdate: Double = pU;
  var seed: Long = S;
  var listAtributeName: List[String] = atributeName()
  var listTipo:List[Int] =l
  var nTrans: Int = dataset.count().intValue(); // Number of transactions
  var nInputs: Int = 0; // Number of inputs
  var nOutputs: Int = 0; // Number of outputs
  var nVars: Int = l.size; // Number of variables
  var prcNotCover: Double = nc;

  def atributeName(): List[String] = {
    var temp: ListBuffer[String] = new ListBuffer[String]
    for (i <- 0 until data.schema.fieldNames.length) {
      temp.+=(data.schema.fieldNames(i))
    }
    temp.toList
  }

  def getAttributeType(i: Int): Int = {
    return listTipo(i);
  }
  def getValue(pos: Int, r: Row): Double = {
    var value: Double = 0;
    value = r.getAs[Double](pos);

    return value
  }
}
