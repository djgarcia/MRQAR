package org.apache.spark.mllib.fpm.MRQAR

import java.util.ArrayList

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.fpm.MOPNAR.{AssociationRule, Chromosome, MOPNAR, myDataset}
import org.apache.spark.mllib.fpm.util.SupportRule
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.storage.StorageLevel

import scala.collection.mutable.ListBuffer


class MopnarSpark(CM: ConfigMopnar) extends Serializable {
  var conf = CM;
  var GLBLPool: ListBuffer[AssociationRule] = new ListBuffer[AssociationRule];
  var cobertura: Double = 0;

  def run(n: Int, ss: SparkSession) {

    var ciclo = 1;
    var dataO = conf.data.rdd.repartition(n).persist(StorageLevel.MEMORY_AND_DISK_SER)
    var dataT = dataO
    var TRIAL = 0;

    while (TRIAL < conf.nTrialsTotal) {

      var listUP: Array[(Long, AssociationRule)] = Array.empty
      /////////////////Ver a diana respecto al rendom
      conf.seed = conf.seed * ciclo
      ciclo = ciclo + 1

      //MR Primera Fase
      var Map1 = dataT.mapPartitions { x =>
        var myData: myDataset = new myDataset(x.toArray, conf.listTipo, conf.listAtributeName)
        var mpnr: MOPNAR = new MOPNAR(myData, conf.NObjetives, conf.nTrialAlg, conf.H, conf.T, conf.probDelta, conf.nSolution, conf.pMutation, conf.amplitude, conf.pUpdate, conf.seed)
        mpnr.execute()
        mpnr.associationRulesPareto.iterator
      }
        .persist(StorageLevel.MEMORY_AND_DISK_SER)

      //MR2 Segunda fase
      var List = Map1.collect() //Obtengo todas las reglas generadas en cada particion

      /*new*/
      //val filtered = List.filter(r => !this.equalChromotoPop(r, List.to[ListBuffer]))

      var broadcast = ss.sparkContext.broadcast(List.toArray) //para difundir las reglas
      var Map2 = dataO.mapPartitions { x => soporte(x.toList.toArray, broadcast).iterator }.persist(StorageLevel.MEMORY_AND_DISK_SER) //Ejecuta 2da map
      var kv = Map2.keyBy { x => x.id } // Formato (k,v)
      var R2 = kv.reduceByKey(RS) //Funcion Reduce

      //Tercera fase secuencial
      listUP = R2.coalesce(1).collect()

      var max = 0
      listUP.foreach { x =>
        updateGP(x._2)
        if (x._2.trial > max)
          max = x._2.trial
      }
      TRIAL += max

      this.GLBLPool = removeRedundant(this.GLBLPool);

      //Cuarta Fase MR
      var MR4 = dataT.filter { x => !coveredData(x) }.persist(StorageLevel.MEMORY_AND_DISK_SER)
      println("Map 4 finished")
      var notcover = MR4.count().toDouble
      var total: Double = conf.nTrans
      var prcNC: Double = notcover / conf.nTrans
      this.cobertura = 100 - prcNC * 100
      if (prcNC > conf.prcNotCover) {
        dataT = MR4
      } else {
        dataT = dataO;
      }
    }
  }

  def soporte(x: Array[Row], list: Broadcast[Array[AssociationRule]]): List[AssociationRule] = {
    var LB = new ListBuffer[AssociationRule]
    var sizeArray = list.value.length
    println(sizeArray)
    for (i <- 0 until sizeArray) {
      var Ta = list.value(i)
      var antC, conC: Boolean = true;
      var supRule = new SupportRule(0, 0, 0)
      for (j <- 0 until x.length) {
        antC = true
        conC = true
        var example: Array[Double] = new Array[Double](x(j).length)
        for (k <- 0 until x(j).length) {
          example.update(k, x(j).getDouble(k))
        }
        var size = Ta.chrown.genes.length
        var l = 0
        while (l < size && (antC || conC)) {
          var temp = Ta.chrown.genes(l)
          if (temp.ac == 0) {
            if (!temp.isCover(temp.attr, example(temp.attr))) antC = false
          }
          if (temp.ac == 1) {
            if (!temp.isCover(temp.attr, example(temp.attr))) conC = false
          }
          l += 1
        }
        if (antC) supRule.antsSupport += 1
        if (conC) supRule.consSupport += 1
        if (antC && conC) supRule.support += 1
      }
      Ta.metrics(supRule)
      LB.+=:(Ta)
    }
    LB.toList
  }

  def removeRedundant(upop: ListBuffer[AssociationRule]): ListBuffer[AssociationRule] = {
    var i, j: Int = 0;
    var stop: Boolean = false;
    var rule1, rule2: AssociationRule = null;

    Qsort(upop);

    while (i < upop.length) {
      stop = false;
      j = upop.length - 1
      while (j >= 0 && !stop) {
        if (j != i) {
          rule1 = upop(i);
          rule2 = upop(j);
          if (rule1.nAnts == rule2.nAnts) {
            if (rule1.isSubRule(rule2)) {
              if (rule1.CF >= rule2.CF) {
                upop.remove(j);
                if (j < i) i = i - 1;
              } else {
                upop.remove(i);
                i = i - 1;
                stop = true;
              }
            }
          } else if (rule1.nAnts > rule2.nAnts) stop = true;
        }
        j = j - 1;
      }
      i = i + 1;
    }
    return upop;
  }

  def Qsort(xs: ListBuffer[AssociationRule]) {
    def swap(i: Int, j: Int) {
      val t = xs(i);
      xs(i) = xs(j);
      xs(j) = t
    }

    def qsort1(l: Int, r: Int) {
      val pivot = xs((l + r) / 2).nAnts
      var i = l;
      var j = r
      while (i <= j) {
        while (xs(i).nAnts < pivot) i += 1
        while (xs(j).nAnts > pivot) j -= 1
        if (i <= j) {
          swap(i, j)
          i += 1
          j -= 1
        }
      }
      if (l < j) qsort1(l, j)
      if (j < r) qsort1(i, r)
    }

    qsort1(0, (xs.length - 1))
  }

  def updateGP(rule: AssociationRule) = {
    var i, dominance: Int = 0;
    var rule1: AssociationRule = null;
    var add: Boolean = true;

    while (i < this.GLBLPool.length) {
      rule1 = this.GLBLPool(i);
      dominance = check_dominance(rule.chrown, rule1.chrown);

      if (dominance == 1) this.GLBLPool.remove(i); //child dominates chromosome1
      else if (dominance == -1) add = false; //chromosome1 dominates child
      i = i + 1;
    }

    if (add) {
      if (!this.equalChromotoPop(rule, this.GLBLPool)) this.GLBLPool.insert(this.GLBLPool.length, rule);
    }
  }

  def check_dominance(a: Chromosome, b: Chromosome): Int = {
    var i: Int = 0;
    var flag1: Int = 0;
    var flag2: Int = 0;
    for (i <- 0 until conf.NObjetives) {
      if (a.objectives(i) > b.objectives(i)) flag1 = 1;
      else if (a.objectives(i) < b.objectives(i)) flag2 = 1;

    }
    if ((flag1 == 1) && (flag2 == 0)) return (1);
    else if ((flag1 == 0) && (flag2 == 1)) return (-1);
    else return (0);
  }

  def equalChromotoPop(chromo: AssociationRule, pop: ListBuffer[AssociationRule]): Boolean = {
    var i: Int = 0;
    var aux: AssociationRule = null;
    var value: Boolean = false;

    while ((!value) && (i < pop.length)) {
      aux = pop(i);
      if (chromo.equals(aux)) value = true;
      i = i + 1;
    }

    return value;
  }

  def coveredData(x: Row): Boolean = {
    var value = false;
    var j = 0;
    while (j < this.GLBLPool.length && !value) {
      var s = this.GLBLPool(j).chrown.isCovered(x)
      if (s) {
        value = true;
      }
      j = j + 1;
    }
    return value;
  }

  def RS: (AssociationRule, AssociationRule) => AssociationRule = {
    case in@(x, z) =>
      x.antSupport = x.antSupport + z.antSupport
      x.consSupport = x.consSupport + z.consSupport
      x.support = x.support + z.support
      x.compute(conf.nTrans)
      (x)
  }

  def Qsort1(xs: ArrayList[AssociationRule]) {
    def swap(i: Int, j: Int) {
      val t = xs.get(i);
      xs.set(i, xs.get(j));
      xs.set(j, t)
    }

    def qsort1(l: Int, r: Int) {
      val pivot = xs.get((l + r) / 2).nAnts
      var i = l;
      var j = r
      while (i <= j) {
        while (xs.get(i).nAnts < pivot) i += 1
        while (xs.get(j).nAnts > pivot) j -= 1
        if (i <= j) {
          swap(i, j)
          i += 1
          j -= 1
        }
      }
      if (l < j) qsort1(l, j)
      if (j < r) qsort1(i, r)
    }

    qsort1(0, (xs.size() - 1))
  }

  def extract(): ListBuffer[String] = {
    var listOut: ListBuffer[String] = new ListBuffer[String];
    var sumWrac: Double = 0;
    var sumConf: Double = 0;
    var sumLift: Double = 0;
    var metric: Array[Double] = Array.fill(8)(0)

    for (i <- 0 until this.GLBLPool.length) {
      var save = "Rule id=" + i + "  " + this.GLBLPool(i).chrown.getRule(conf.listAtributeName) +
        " Support: " + this.GLBLPool(i).support / conf.nTrans +
        " Confidence: " + this.GLBLPool(i).confidence +
        " Lift: " + this.GLBLPool(i).lift +
      " Conviction: " + this.GLBLPool(i).conv +
      " CF: " + this.GLBLPool(i).CF +
      " Netconf: " + this.GLBLPool(i).netConf +
      " YulesQ: " + this.GLBLPool(i).yulesQ

      listOut += save;
      metric(0) = metric(0) + this.GLBLPool(i).support / conf.nTrans
      metric(1) = metric(1) + this.GLBLPool(i).confidence
      metric(2) = metric(2) + this.GLBLPool(i).lift
      metric(3) = metric(3) + this.GLBLPool(i).conv
      metric(4) = metric(4) + this.GLBLPool(i).CF
      metric(5) = metric(5) + this.GLBLPool(i).netConf
      metric(6) = metric(6) + this.GLBLPool(i).yulesQ
      metric(7) = metric(7) + this.GLBLPool(i).nAnts
    }
    metric(0) = metric(0) / this.GLBLPool.length
    metric(1) = metric(1) / this.GLBLPool.length
    metric(2) = metric(2) / this.GLBLPool.length
    metric(3) = metric(3) / this.GLBLPool.length
    metric(4) = metric(4) / this.GLBLPool.length
    metric(5) = metric(5) / this.GLBLPool.length
    metric(6) = metric(6) / this.GLBLPool.length
    metric(7) = metric(7) / this.GLBLPool.length

    listOut += "Average of Support " + metric(0)
    listOut += "Average of Confidence " + metric(1)
    listOut += "Average of Lift " + metric(2)
    listOut += "Average of Conviction " + metric(3)
    listOut += "Average of Certain Factor " + metric(4)
    listOut += "Average of Netconf " + metric(5)
    listOut += "Average of YulesQ " + metric(6)
    listOut += "Average of Antecedents " + metric(7)// * metric(1)
    listOut += "Covered Records " + this.cobertura + "%";

    return listOut;
  }
}
