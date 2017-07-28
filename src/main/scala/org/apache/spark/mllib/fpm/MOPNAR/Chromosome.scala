package org.apache.spark.mllib.fpm.MOPNAR

import org.apache.spark.sql.Row

import scala.collection.mutable.ListBuffer


class Chromosome(genesin:Array[Gene], numObjectivesin:Int,Tin:Int) extends Serializable{

	var genes= genesin;
	var objectives= initObjetive(numObjectivesin);
	var weightVector= new ListBuffer[Double];
	var vectorsNeighbors= new ListBuffer[Int];
	var numObjectives:Int=numObjectivesin;
	var T:Int=Tin;
	var support:Double = 0;
	var antsSupport:Double = 0;
	var consSupport:Double = 0;
	var confidence:Double = 0;
	var conv:Double = 0;
	var CF:Double = 0;
  var Lift:Double = 0;
	var netConf:Double = 0;
	var yulesQ:Double = 0;
	var sumInterval:Double = 0;
	var n_e:Boolean = true;
	var rank:Int = -1;
	var nAnts:Int=numAnt();

	def initObjetive(size:Int):ListBuffer[Double]={
			var list= new ListBuffer[Double]();
			for(i<-0 until size){
				list.insert(i, 0)
			}
			return list;
	}

	def numAnt():Int ={
			var n:Int=0;
	var i=0;
	var size=this.genesin.length;    
	while (i< size) {
		if (this.genesin(i).ac == 0)  n=n+1;
		i=i+1;
	}
	return n;


	}
	def getRule(list:List[String]):String={
			var antecedente=" "
					var consecuente=" "
					var objetivos=""
          var i =0;
          
					while(i< this.genes.length){
           var is="";
            if(!genes(i).pn){
              is=" NOT "
            }
						if(genes(i).ac==1){
							var line=list(i);
							line=line+":";
							if(genes(i).lb!=genes(i).ub){
								line=line+is+genes(i).lb+"_"+genes(i).ub+" ";
							}
							else{
								line=line+is+genes(i).lb+" ";
							}
							consecuente=consecuente+line;
						}
						else  if(genes(i).ac==0){
							var line=list(i);
							line=line+":";
							if(genes(i).lb!=genes(i).ub){
								line=line+is+genes(i).lb+"_"+genes(i).ub+" ";
							}
							else{
								line=line+is+genes(i).lb+" ";
							}
							antecedente=antecedente+line;
						}
            i=i+1;
					}

			return antecedente+"=> "+consecuente+" // ";
	}


	def copy():Chromosome= {
			var chromo:Chromosome = new Chromosome(this.genes, this.numObjectives, this.T)

	for(i<-0 until this.numObjectives)  chromo.objectives(i)=this.objectives(i) ;

			for(i<-0 until this.numObjectives)  chromo.weightVector+=this.weightVector(i);

					for(i<-0 until this.T)  chromo.vectorsNeighbors+=this.vectorsNeighbors(i);

							chromo.support = this.support;
							chromo.antsSupport = this.antsSupport;
							chromo.consSupport = this.consSupport;
							chromo.confidence = this.confidence;
							chromo.CF = this.CF;
							chromo.netConf = this.netConf;
							chromo.conv = this.conv;
              chromo.Lift=this.Lift
							chromo.yulesQ = this.yulesQ;
							chromo.nAnts = this.nAnts;
							chromo.sumInterval = this.sumInterval;
							chromo.rank = this.rank;
							chromo.n_e = this.n_e;

							return chromo;
	}

	def forceConsistency()= {
		var i, n_ant, n_cons, pos, count:Int=0;



		for (i<-0 until genes.length) {
			if (genes(i).ac == 0)  n_ant=n_ant+1;
			if (genes(i).ac ==1)  n_cons=n_cons+1;
		}

		if (n_cons > 1) {
			pos = Randomize.RandintClosed(1, n_cons);
			count=1;
			for (i<-0 until genes.length) {
				if (genes(i).ac ==1) {
					if (count != pos) {
						genes(i).ac=0;
						n_ant=n_ant+1;
					}
					count=count+1;
				}
			}
		}
		else if (n_cons < 1) {
			if (n_ant > 1) {
				pos = Randomize.RandintClosed(1, n_ant);
				count=1;
				while(i< genes.length && count<=pos) {
					if (genes(i).ac == 0) {
						if (count == pos) {
							genes(i).ac=1;
							n_ant=n_ant-1;
						}
						count=count+1;
					}
					i=i+1;
				}
			}
			else {
				pos=Randomize.Randint(0, genes.length);
				while ( genes(pos).ac != -1){
					genes(pos).ac=1;
					pos=Randomize.Randint(0, genes.length);
				}
			}
			n_cons=n_cons+1;
		}

		if (n_ant < 1) {
			pos=Randomize.Randint(0, genes.length);
			while ( genes(pos).ac != -1){
				genes(pos).ac=0;
				pos=Randomize.Randint(0, genes.length);
			}
		}
	}

	def equals(chr:Chromosome ):Boolean= {
		var i:Int=0;;

		for (i<-0 until this.genes.length) {
			if ((this.genes(i).ac != -1) && (chr.genes(i).ac == -1))  return (false);
			if ((this.genes(i).ac == -1) && (chr.genes(i).ac != -1))  return (false);
			if ((this.genes(i).ac != -1) && (chr.genes(i).ac != -1)) {
				if (!chr.genes(i).equals(this.genes(i)))  return (false);
			}
		}

		return (true);
	}

	override def toString():String= {

			var str:String = "Lift:  " + this.objectives(0)+ "; Rule Support: " + this.support + "; Rule Confidence: " + this.CF + "\n" ;
	for( j<-0 until this.numObjectives){
		str=str + "Objetives:" + "["+ j + "]"+ this.objectives(j) + "\n";
	}
	return str;
	}

	def isCovered(example:Row):Boolean= {
			var i:Int=0;
	var covered:Boolean=true;

	covered = true;

	while( i < this.genes.length && covered) {
		var value=example.getDouble(i);
		if (this.genes(i).ac != -1)  covered = this.genes(i).isCover(i, value);
		i=i+1;
	}

	return covered;
	}

	def computeObjetives (dataset:myDataset)= {
		var i, j, nTrans, nVars, nCons:Int=0;
		var antsCover, consCover:Boolean=true;
		var amp, interval, lift, numeratorYules, denominatorYules:Double=0;
		var example:Row=null;
		var covered:Array[Int]=null;

		nTrans = dataset.nTrans;
		nVars = dataset.nVars;
		this.antsSupport = 0;
		this.consSupport = 0;
		this.support = 0;
		this.confidence = 0;
		this.nAnts = 0;
		this.sumInterval = 0;
		nCons = 0;

		covered = new Array[Int](nTrans);
		covered=Array.fill(nTrans)(0)
		this.objectives=ListBuffer.fill(this.numObjectives)(0)

		while(i< nTrans) {
			example = dataset.getExample(i);
			antsCover = true;
			consCover = true;
      j=0
			while(j< nVars && (antsCover || consCover)) {
				var value:Double=0;

			value = example.getAs[Double](j);
			
			if (this.genes(j).ac == 0 && antsCover) {

				if (!this.genes(j).isCover(j, value))  antsCover = false;

			}
			else if (this.genes(j).ac == 1 && consCover) {
				if (!this.genes(j).isCover(j, value))  consCover = false;
			}
			j=j+1;
			}

			if (antsCover)  this.antsSupport=this.antsSupport+1;
			if (consCover)  this.consSupport=this.consSupport+1;
			if (antsCover && consCover) {
				this.support=this.support+1;
				covered(i) = 1;
			}
      i=i+1;
		}

		this.antsSupport /= nTrans;
		this.consSupport /= nTrans;
		this.support /= nTrans;
		if (this.antsSupport > 0)  this.confidence = this.support / this.antsSupport;
		i=0;
		while (i< nVars) {
			if (this.genes(i).ac == 0)  this.nAnts=this.nAnts+1;
			if (this.genes(i).ac == 1)  nCons=nCons+1;
			if (this.genes(i).ac != -1) {
				this.genes(i).tuneInterval(dataset, covered);
				amp = this.genes(i).ub - this.genes(i).lb;
				if(this.genes(i).pn){
					interval = amp/dataset.Amplitude(i)
				}
				else{
					interval = (dataset.Amplitude(i) - amp) /dataset.Amplitude(i);
				}
				this.sumInterval=this.sumInterval+ interval;

			}
			i+=1;
		}     
		this.sumInterval /= (this.nAnts + nCons);

		//compute lift
		if((this.antsSupport == 0)||(this.consSupport == 0))
			lift = 1;
		else lift = this.support/(this.antsSupport * this.consSupport);
    this.Lift=lift;
		//compute conviction
		if((this.consSupport == 1)|| (this.antsSupport == 0))
			this.conv = 1;
		else 
			this.conv = (this.antsSupport*(1-this.consSupport))/(this.antsSupport-this.support);

		//compute netconf
		if ((this.antsSupport == 0)||(this.antsSupport == 1)||(Math.abs((this.antsSupport * (1-this.antsSupport))) <= 0.001))
			this.netConf = 0;
		else
			this.netConf = (this.support - (this.antsSupport*this.consSupport))/(this.antsSupport * (1-this.antsSupport));

		//compute yulesQ
		numeratorYules = ((this.support * (1 - this.consSupport - this.antsSupport + this.support)) - ((this.antsSupport - this.support)* (this.consSupport - this.support)));
		denominatorYules = ((this.support * (1 - this.consSupport - this.antsSupport + this.support)) + ((this.antsSupport - this.support)* (this.consSupport - this.support)));

		if ((this.antsSupport == 0)||(this.antsSupport == 1)|| (this.consSupport == 0)||(this.consSupport == 1)||(Math.abs(denominatorYules) <= 0.001))
			yulesQ = 0;
		else yulesQ = numeratorYules/denominatorYules;

		// compute Certain Factor
		if ((Math.abs(this.confidence - this.consSupport) <= 0.001) || (this.consSupport >= 0.98))
          this.CF = 0;
	    else
		  if(this.confidence > this.consSupport)
			  CF = (this.confidence - this.consSupport)/(1-this.consSupport); 
		  else 
			if(this.confidence < this.consSupport)
				CF = (this.confidence - this.consSupport)/(this.consSupport); 

		if (this.numObjectives > 0)  this.objectives(0) = this.Lift;
		if (this.numObjectives > 1)  this.objectives(1)= this.CF * this.support;  // performance
		if (this.numObjectives > 2)  this.objectives(2) = 1.0 / this.nAnts;  // number of variables in the antecedent

		this.n_e = false;
	}

	def computeDistance(weightVector_B:ListBuffer[Double]):Double= {
		var result:Double = 0;

	for (i<-0 until this.weightVector.length)  result=result+ Math.pow (this.weightVector(i) - weightVector_B(i), 2.0);

			return (result);
	}


	def isSubChromo (chromo2:Chromosome):Boolean={
		val i:Int=0;
	var gen:Gene=null;

	if (this.support < chromo2.support)  return (false);

	for (i<-0 until genes.length) {
		gen = chromo2.genes(i);

		if ((genes(i).ac != -1) && (gen.ac == -1))  return (false);
		if ((genes(i).ac == -1) && (gen.ac != -1))  return (false);
		if ((genes(i).ac != -1) && (gen.ac != -1)) {
			if (!genes(i).isSubGen(gen))  return (false);
		}
	}
	return (true);
	}

	def calculateSumInterval(dataset:myDataset)= {
		var i, nVars, nCons:Int=0;

		nVars = dataset.nVars;
		nCons = 0;

		this.sumInterval = 0.0;

		for (i<-0 until nVars) {
			if (this.genes(i).ac == 1)  nCons=nCons+1;
			if (this.genes(i).ac != -1)  this.sumInterval=this.sumInterval+ ((this.genes(i).ub - this.genes(i).lb) / dataset.Amplitude(i));
		}     

		this.sumInterval /= (this.nAnts + nCons);
	}

	def getCoveredTIDs (dataset:myDataset):ListBuffer[Int]={
		var i:Int =0;
	var example:Row=null;
	var TIDs:ListBuffer[Int]=null;

	TIDs = new ListBuffer[Int];

	for (i<-0 until dataset.nTrans) {
		example = dataset.getExample(i);
		if (this.isCovered(example))  TIDs+=i;
	}

	return (TIDs);
	}


	def compareTo(chr:Chromosome):Int= {
		if (chr.nAnts > this.nAnts)  return -1;
		else if (chr.nAnts <  this.nAnts)  return 1;
		else  return 0;
	}
}