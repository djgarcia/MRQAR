package org.apache.spark.mllib.fpm.MOPNAR

import scala.collection.mutable.ListBuffer

class Distance(size:Int) extends Serializable{
  var posO:Int=0;
  var ArrayD:ListBuffer[Double]=new ListBuffer[Double];
  def init(){
    for(i<-0 until size){
      ArrayD.insert(i, 0)
    }
  }
}
