package org.apache.spark.mllib.fpm.MOPNAR

/**
 * @author SamySF
 */

import org.apache.spark.mllib.fpm.util.SupportRule

import scala.collection.mutable.ListBuffer
class AssociationRule(chr:Chromosome, T:Int) extends Serializable
{
var id=Randomize.rand.nextLong()
var trial=T
var chrown:Chromosome=chr
var antecedent:ListBuffer[Gene]=assignamentGene(0);
var consequent:ListBuffer[Gene]=assignamentGene(1);
var  antSupport:Double=chr.antsSupport;
var  consSupport:Double=chr.consSupport;
var  support:Double=chr.support;
var  supportRule:Double=0.0;
var  confidence:Double=chr.confidence;
var  lift:Double=chr.objectives(0);
var  conv:Double=chr.conv;
var  CF:Double=chr.CF;
var  netConf:Double=chr.netConf;
var  yulesQ:Double=chr.yulesQ;
var  nAnts:Int=chr.nAnts;
var  sumInterval:Double=chr.sumInterval;
var rank:Int=chr.rank;


/**
 * <p>
 * It creates a new association rule by setting up the chromosome which is based on
 * </p>
 * @param chr The chromosome which this association rule is based on
 */
def assignamentGene(tipo:Int):ListBuffer[Gene]= {
		var list:ListBuffer[Gene]= new ListBuffer[Gene];
if(chrown==null){
	return list;
}
for (i<-0 until chrown.genes.length) {
	var gen:Gene = chr.genes(i);
if (gen.ac == tipo)  list += gen.copy();
}
return list;
}
def isSubRule (x:AssociationRule):Boolean={
    val i:Int=0;
var gen:Gene=null;

if (this.support < x.support)  return (false);

for (i<-0 until chrown.genes.length) {
  gen = x.chrown.genes(i);

  if ((chrown.genes(i).ac != -1) && (gen.ac == -1))  return (false);
  if ((chrown.genes(i).ac == -1) && (gen.ac != -1))  return (false);
  if ((chrown.genes(i).ac != -1) && (gen.ac != -1)) {
    if (!chrown.genes(i).isSubGen(gen))  return (false);
  }
}
return (true);
}
def metrics(S:SupportRule){
   this.antSupport=S.antsSupport
   this.consSupport=S.consSupport
   this.support=S.support
   
}
  def compute(total:Int){
    var AS=this.antSupport/total
    var CS=this.consSupport/total
    var S:Double=this.support/total
    this.supportRule=S
    if (AS > 0)  this.confidence = S / AS;
//compute lift
    if((AS == 0)||(CS == 0))
      this.lift = 1;
    else this.lift = S/(AS * CS);
  
    //compute conviction
    if((CS == 1)|| (AS == 0))
      this.conv = 1;
    else 
      this.conv = (AS*(1-CS))/(AS-S);

    //compute netconf
    if ((AS == 0)||(AS == 1)||(Math.abs((AS * (1-AS))) <= 0.001))
      this.netConf = 0;
    else
      this.netConf = (S - (AS*CS))/(AS * (1-AS));

    //compute yulesQ
    var numeratorYules = ((S * (1 - CS - AS + S)) - ((AS - S)* (CS - S)));
    var denominatorYules = ((S * (1 - CS - AS + S)) + ((AS - S)* (CS - S)));

    if ((AS == 0)||(AS == 1)|| (CS == 0)||(CS == 1)||(Math.abs(denominatorYules) <= 0.001))
      yulesQ = 0;
    else yulesQ = numeratorYules/denominatorYules;

    // compute Certain Factor
    if ((Math.abs(this.confidence - CS) <= 0.001) || (CS >= 0.98))
       this.CF = 0;
	else
      if(this.confidence > CS)
        CF = (this.confidence - CS)/(1-CS); 
      else 
        if(this.confidence < CS)
          CF = (this.confidence - CS)/(CS); 

  
    if (this.chrown.numObjectives > 0)  this.chrown.objectives(0) = this.lift;//accuracy
    if (this.chrown.numObjectives > 1)  this.chrown.objectives(1) = this.CF*S;  // performance/*this.CF  S;*/  
    if (this.chrown.numObjectives> 2)  this.chrown.objectives(2) = 1.0 / this.nAnts;  // number of variables in the antecedent

  }
/**
 * <p>
 * It allows to clone correctly an association rule
 * </p>
 * @return A copy of the association rule
 */
def copy():AssociationRule ={
var rule:AssociationRule=null;

rule = new AssociationRule(null,trial);

rule.antecedent = new ListBuffer[Gene];
rule.consequent = new ListBuffer[Gene];

for (i<-0 to this.antecedent.length)  rule.antecedent+=this.antecedent(i).copy();
for (i<-0 to this.consequent.length)  rule.consequent+=this.consequent(i).copy();

rule.antSupport = this.antSupport;
rule.consSupport = this.consSupport;
rule.support = this.support;
rule.confidence = this.confidence;
rule.lift = this.lift;
rule.CF = this.CF;
rule.conv = this.conv;
rule.netConf = this.netConf;
rule.yulesQ = this.yulesQ;
rule.nAnts = this.nAnts;
rule.sumInterval = this.sumInterval;
rule.rank = this.rank;

return rule;
}

/**
 * <p>
 * It retrieves the antecedent part of an association rule
 * </p>
 * @return An array of genes only representing antecedent attributes
 */

def isCovered ( example:Array[Double]):Boolean ={
	var i:Int=0;
	var covered:Boolean=true;
	var gen:Gene=null;

	while(i < this.antecedent.length && covered) {
		gen = this.antecedent(i);
		if (!gen.isCover(gen.attr, example(gen.attr)))  covered = false;
    i=i+1;
	}
  i=0;
	while(i < this.consequent.length && covered) {
		gen = this.consequent(i);
		if (!gen.isCover(gen.attr, example(gen.attr)))  covered = false;
	}

	return covered;
}

/**
 * <p>
 * It returns a raw string representation of an association rule
 * </p>
 * @return A raw string representation of the association rule
 */  

override def toString():String=
{
	return ( this.antecedent.toString() + "-> " + this.consequent.toString() + ": " + this.antSupport + "; " + this.support + "; " + this.confidence + "; " + this.lift + "; " + this.nAnts + "; " + this.sumInterval + "; " );//+ this.rank);
}


}