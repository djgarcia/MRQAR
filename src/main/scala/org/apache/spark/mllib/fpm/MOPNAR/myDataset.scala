package org.apache.spark.mllib.fpm.MOPNAR

import org.apache.spark.sql.Row

import scala.collection.mutable.ListBuffer

class myDataset(d:Array[Row],l:List[Int], nameA:List[String]) extends Serializable{
	val NOMINAL = 0;
	val INTEGER = 1;
	val REAL = 2;
	val listAtributeName:List[String]=nameA;
	val listTipo:List[Int]=l;
  val dataset=d
	var trueTransactions=null; //true transactions array
	var missing=null; //possible missing values
	var emax:Array[Double]=maxvalue(); //max value of an attribute
	var emin:Array[Double]=minvalue(); //min value of an attribute
	var Amplitude:Array[Double]=coverAmplitude(); //true transactions array
  

	var nTrans:Int=dataset.length; // Number of transactions
	var nInputs:Int=0; // Number of inputs
	var nOutputs:Int=0; // Number of outputs
	var nVars:Int=dataset(0).length; // Number of variables

	def maxvalue():Array[Double]={
      var temp:ListBuffer[Double]=new ListBuffer[Double]
          val cantA=this.dataset(0).length
          temp= ListBuffer.fill(cantA)(Double.MinValue)
          var list=dataset
          var size=list.size
          for(i<-0 until size){
            var x=list(i)
                for(i<-0 until cantA){
                  if(x.getAs[Double](i)>temp(i))
                    temp(i)=x.getAs[Double](i)

                }
          }
  temp.toArray
  }

  def minvalue():Array[Double]={
      var temp:ListBuffer[Double]=new ListBuffer[Double]
          val cantA=this.dataset(0).length
          temp= ListBuffer.fill(cantA)(Double.MaxValue)
          var list=dataset
          var size=list.size
          for(j<-0 until size){
            var x=list(j)
                for(i<-0 until cantA){
                  if(x.getAs[Double](i)<temp(i))
                    temp(i)=x.getAs[Double](i)
                }
          }
  temp.toArray
  }

  def coverAmplitude():Array[Double]={
      var temp:ListBuffer[Double]=new ListBuffer[Double]
          val cantA=this.dataset(0).length
          temp=ListBuffer.fill(cantA)(0)
          for(i<-0 until cantA){
            temp(i)= emax(i) - emin(i)
          }
  println(temp.size)
  temp.toArray
  }


	def getExample(index:Int):Row={
		return dataset(index);
	}

	def getAttributeType(i:Int):Int={
			return listTipo(i);
	}
	def getValue(pos:Int,r:Row):Double={
			var value:Double=0;
	value = r.getAs[Double](pos);

	return value
	}
}