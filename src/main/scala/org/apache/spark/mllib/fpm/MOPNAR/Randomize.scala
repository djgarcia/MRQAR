package org.apache.spark.mllib.fpm.MOPNAR

import scala.util.Random

object Randomize extends Serializable {
  
	var Seed:Long=0;
  val rand:Random= new Random
  
  rand.setSeed(Seed)
  def initRand(s:Long)={
    Seed=s;
    rand.setSeed(s);
  }
	/** Rand computes a psuedo-random float value between 0 and 1, excluding 1 
	 * @return A uniform-distributed real value in [0,1) 
	 */
	def Rand():Double= {
		var t=rand.nextDouble();
    while(t==1){
      t=rand.nextDouble();
    }
    return t;

	}
	/** RandOpen computes a psuedo-random float value between 0 and 1, excluding 0 and 1  
	 * @return A uniform-distributed real value in (0,1)
	 */
	def RandOpen ():Double ={
	   var t=rand.nextDouble();
    while(t==1 || t==0){
      t=rand.nextDouble();
    }
    return t;
	}
	/** RandClosed computes a psuedo-random float value between 0 and 1 inclusive  
	 * @return A uniform-distributed real value in [0,1]
	 */
	def RandClosed ():Double= {
		return rand.nextDouble();
	}

	/** Randint gives an integer value between low and high, excluding high
	 * @param low Lower bound (included)
	 * @param high Upper bound (NOT included)
	 * @return A uniform-distributed integer value in [low,high)
	 */
def Randint (low:Int, high:Int):Int= {
	var t=	low + (high - low);
  return  (t* rand.nextDouble()).intValue();
	}

	/** RandintClosed gives an integer value between low and high inclusive
	 * @param low Lower bound (included)
	 * @param high Upper bound (included)
	 * @return A uniform-distributed integer value in [low,high]
	 */
def RandintClosed (low:Int, high:Int):Int= {
		//since genrand_res53() generates a double in [0,1), we increment
		//high by one, so "high" can appear with same probability as the rest of
		//numbers in the interval
  var t=  (low + (high+1-low) * rand.nextDouble());
 
  return t.intValue();
	}
	/** Randdouble gives an double value between low and high, excluding high
	 * @param low Lower bound (included)
	 * @param high Upper bound (NOT included)
	 * @return A uniform-distributed real value in [low,high)
	 */
def Randdouble (low:Double,high:Double):Double= {
	var t=	(low + (high-low) * rand.nextDouble());

  return t;
	}

	/** RanddoubleClosed gives an double value between low and high inclusive
	 * @param low Lower bound (included)
	 * @param high Upper bound (included)
	 * @return A uniform-distributed real value in [low,high]
	 */
def RanddoubleClosed (low:Double,high:Double):Double= {
		return (low + (high-low) * rand.nextDouble());
	}





}