package org.apache.spark.mllib.fpm.MOPNAR

import java.math.BigDecimal
import java.util.ArrayList

import org.apache.spark.sql.Row

import scala.collection.mutable.ListBuffer

class MOPNARProcess( datasetin:myDataset, numObjectivesin:Int, nTrialsin:Int, Hin:Int, Tin:Int, probDeltain:Double, nrin:Int, pmin:Double, afin:Double, percentUpdatein:Double)  extends Serializable{
	/**
	 * <p>
	 * It provides the implementation of the algorithm to be run in a process
	 * </p>
	 */

	val UPDATE_SOLUTION_NEIGHBOR = 1;
	val UPDATE_SOLUTION_WHOLEPOP = 2;


	var paretos:String= new String("");;
	var dataset:myDataset=datasetin;
	var numObjectives:Int=numObjectivesin;
	var nTrials:Int=nTrialsin;
	var H:Int=Hin; 
	var T:Int=Tin;
	var probDelta:Double=probDeltain; 
	var nr:Int=nrin; 
	var pm:Double=pmin;    
	var af:Double=afin;
	var max_rank:Int=0;
	var updatePop:Int=0;
	var minSupport:Double=0.01;
	var percentUpdate:Double=percentUpdatein;


	var nAttr:Int=datasetin.nVars;
	var nTrans:Int=datasetin.nTrans;
	var trials:Int=0;
	var uPop:ArrayList[Chromosome]=new ArrayList[Chromosome];
	var EP:ArrayList[Chromosome]=new ArrayList[Chromosome]; //an external population (EP), which is used to store non dominated solutions found during the search
	var Z:Array[Double]=new Array[Double](this.numObjectives); // Z= (z1,z2...zm) where Zi is the best (max) value found for objective fi  
	var Z_min:Array[Double]=new Array[Double](this.numObjectives); // Z_min= (z1,z2...zm) where Zi is the min value found for objective fi  

	/**
	 * <p>
	 * It creates a new process for the algorithm by setting up its parameters
	 * </p>
	 * @param dataset The instance of the dataset for dealing with its records
	 * @param numObjectives The number of objectives to be optimized
	 * @param nTrials The maximum number of generations to reach before completing the whole evolutionary learning
	 * @param H  The parameter to control the population size and weight vectors
	 * @param T  The number of the weight vectors in the neighborhood
	 * @param probDelta  The probability that parent solutions are selected from the neighborhood
	 * @param nr  The maximal number of solutions replaced by each child solution
	 * @param pm The probability for the mutation operator
	 * @param af The factor of amplitude for each attribute of the dataset
	 * @param percentUpdate The difference threshold to restart the population
	 */



	/**
	 * <p>
	 * It runs the evolutionary learning for mining association rules
	 * </p>
	 */
	def run()={
		var nGn:Int = 0;
	this.trials = 0;
	this.paretos = new String("");

	println("Initialization");
	this.initializePopulation();

	do {
	
		this.updatePop = 0;
		this.diffEvolution(); 
		this.verifyRestartPop();

		nGn=nGn+1;
   
	} while (this.trials < this.nTrials);

	this.EP=this.removeRedundant(this.EP);

	System.out.println("done.\n");
	}

	def verifyRestartPop()={
		var percentUpdate:Double = (this.uPop.size()* this.percentUpdate) / 100.0;

	if(this.updatePop < percentUpdate){
		this.restartPop();
	}
	}


	/**
	 * Non-domination Selection procedure to filter out the dominated
	 * individuals from the given list.
	 * 
	 * @param collection the collection of Chromosome object need to be filtered.
	 * @return the reference copy of the non-dominating individuals
	 */
	def nonDominateSelection( collection:ArrayList[Chromosome]):ArrayList[Chromosome]= {
		var counter = 1;
		var dominance:Int=0;
		var chromosome1, chromosome2:Chromosome=null;
		var result:ArrayList[Chromosome]=null;

		result = new ArrayList[Chromosome];
		result.add(collection.get(0));


		while (counter < collection.size()) {
			var jj:Int = 0;
		chromosome2 = collection.get(counter);
		var resultsize:Int = result.size();
		var remove:Array[Boolean] = new Array[Boolean](resultsize);

		while (jj < resultsize) {
			chromosome1 = result.get(jj);
			dominance = check_dominance(chromosome2, chromosome1);

			if (dominance == 1) { //chromosome2 dominates chromosome1 
				remove(jj) = true;

			} else if (dominance == -1) { //chromosome1 dominates chromosome2
				counter=counter+1;
        jj=resultsize

			}
			jj=jj+1;
		}
		var i:Int=remove.length-1;
		while(i>=0){
			if (remove(i))
				result.remove(i);
			i=i-1;
		}
		var index=result.size;

		result.add(chromosome2);
		counter=counter+1;
		}
		return result;
	}

	/**
	 * Non-domination Selection procedure to filter out the dominated
	 * individuals from the given list.
	 * 
	 * @param collection the collection of Chromosome object need to be filtered.
	 * @return the reference copy of the non-dominating individuals
	 */
	def updateEP ( child:Chromosome)= {
		var i, dominance:Int=0;
		var chromosome1:Chromosome=null;   
		var add:Boolean = true;

		while (i< this.EP.size()) {
			chromosome1 = this.EP.get(i);
			dominance = check_dominance(child, chromosome1);

			if (dominance == 1)  this.EP.remove(i);  //child dominates chromosome1 
			else if (dominance == -1)  add = false; //chromosome1 dominates child
			i=i+1;
		}

		if (add) {
			if(!this.equalChromotoPop (child, this.EP))  this.EP.add(child);
		}
	}

	def diffEvolution()={
		var i, tipo:Int=0; 

		while(i< this.uPop.size()){
			if (Randomize.Rand() < this.probDelta)  tipo = this.UPDATE_SOLUTION_NEIGHBOR;
			else  tipo = this.UPDATE_SOLUTION_WHOLEPOP;
     
			this.crossover(this.uPop.get(i), this.uPop.get(this.matingselection(i,tipo)), tipo, i);
			i=i+1;
		}

		//update EP
   
		for(i<-0 until this.uPop.size()){  this.updateEP(this.uPop.get(i));}

		this.EP=this.removeRedundant(this.EP); 
   
	}

	def crossover( dad:Chromosome, mom:Chromosome, tipo:Int, pos_chromo:Int) ={
		var i:Int=0;
	var genesSon1:Array[Gene]=new Array[Gene](0);
	var genesSon2:Array[Gene]=new Array[Gene](0);
	var son1, son2:Chromosome=null;


	genesSon1 =new Array[Gene](this.nAttr);
	genesSon2 =new Array[Gene](this.nAttr);

	for (i<-0 until this.nAttr) {
		if ((dad.genes(i).ac == 1) || (mom.genes(i).ac==1)) {
			genesSon1(i) = dad.genes(i).copy();
			genesSon2(i) = mom.genes(i).copy();
		}
	}

	for (i<-0 until this.nAttr) { 
		if ((dad.genes(i).ac != 1) && (mom.genes(i).ac != 1)) {
			if (Randomize.Rand() < 0.5) {
				genesSon1(i) = dad.genes(i).copy();
				genesSon2(i) = mom.genes(i).copy();
			}
			else {
				genesSon1(i) = mom.genes(i).copy();
				genesSon2(i) = dad.genes(i).copy();
			}
		}
	}

	son1 = new Chromosome(genesSon1, this.numObjectives, this.T);
	son2 = new Chromosome(genesSon2, this.numObjectives, this.T);

	if (Randomize.Rand() < this.pm)  this.mutate (son1);
	if (Randomize.Rand() < this.pm)  this.mutate (son2);

	son1.forceConsistency();
	son2.forceConsistency();

	son1.computeObjetives (this.dataset);

	son2.computeObjetives (this.dataset);
	this.trials=this.trials+ 2;

	if((!this.equalChromotoPop(son1, this.uPop)) && (son1.support > this.minSupport) && (!(son1.support > (1.0 - this.minSupport))) && (son1.CF > 0)){
		this.update_Z(son1);
		this.updateSolutions(son1, tipo, pos_chromo);
	} 
	if((!this.equalChromotoPop(son2, this.uPop)) && (son2.support > this.minSupport) && (!(son2.support > (1.0 - this.minSupport))) && (son2.CF > 0)){
		this.update_Z(son2);
		this.updateSolutions(son2, tipo, pos_chromo);
	}

	}


	def  mutate ( chr:Chromosome)= {
		var i:Int=0;
	var type_attr, min_attr, max_attr, top:Double=0;
	var gene:Gene=null;

	i = Randomize.Randint(0, this.nAttr);
	gene = chr.genes(i);

	type_attr = this.dataset.getAttributeType(i);
	min_attr = this.dataset.emin(i);
	max_attr = this.dataset.emax(i);

	if (type_attr != 0) {
		if (type_attr == 2) {
			if (Randomize.Rand() < 0.5) {
				if (Randomize.Rand() < 0.5) {
					top = Math.max(gene.ub - (this.dataset.Amplitude(i) / this.af), min_attr);
					gene.lb=Randomize.RanddoubleClosed(top, gene.lb);
				}
				else  gene.lb=Randomize.Randdouble(gene.lb, gene.ub);
			}
			else {
				if (Randomize.Rand() < 0.5) {
					top = Math.min(gene.lb + (this.dataset.Amplitude(i) / this.af), max_attr);
					gene.ub=Randomize.RanddoubleClosed(gene.ub, top);
				}
				else  gene.ub=Randomize.RanddoubleClosed(gene.lb+0.0001,gene.ub);
			}         
		}
		else {
			if (Randomize.Rand() < 0.5) {
				if (Randomize.Rand() < 0.5) {
					top = Math.max(gene.ub - (this.dataset.Amplitude(i) / this.af), min_attr);
					gene.lb=Randomize.RandintClosed(top.intValue(),gene.lb.intValue());
				}
				else  gene.lb=Randomize.Randint(gene.lb.intValue(), gene.ub.intValue());
			}
			else {
				if (Randomize.Rand < 0.5) {
					top = Math.min(gene.lb + (this.dataset.Amplitude(i) / this.af), max_attr);
					gene.ub=Randomize.RandintClosed(gene.ub.intValue(), top.intValue());
				}
				else  gene.ub=Randomize.RandintClosed(gene.lb.intValue() + 1, gene.ub.intValue());
			}
		}
	}
	else {
		top = Randomize.RandintClosed(min_attr.intValue(), max_attr.intValue());
		gene.lb=top;
		gene.ub=top;
	}
	if(Randomize.RandintClosed(0, 1) == 1){
		gene.pn=true;
	}
	else{
		gene.pn=false;
	}  
	gene.ac=gene.randAct();
	}

	def matingselection( pos_chr:Int,tipo:Int):Int={
		// pos_chr  : the id of current subproblem
		// type : 1 - neighborhood; otherwise - whole population

		if (tipo == this.UPDATE_SOLUTION_NEIGHBOR)  return (Randomize.Randint(0, this.T));
		else  return (Randomize.Randint(0, this.uPop.size()));
	}

	def random_permutation(size:Int):Array[Int] ={
		var i, j, tmp:Int=0;
		var index = new Array[Int](size);

		for(i<-0 until size)  index(i) = i;
				for(i<-0 until size) {
					j = Randomize.Randint(0, size);
					tmp = index(i);
					index(i) = index(j);
					index(j) = tmp;
				}

				return  index;
	} 

	def  setWeightVectorsToEachChromo()={
		var i,j:Int=0;
		var weightVector:ListBuffer[Double]=null;
		var chr:Chromosome=null;
		var rnd_genes:Array[Gene]=null;
		rnd_genes = new Array[Gene](this.nAttr);
		weightVector = new ListBuffer[Double];

		for(k<-0 until this.nAttr){
			rnd_genes(k) = new Gene();
		} 
		var count=0;

		while(i <= H){
			weightVector = new ListBuffer[Double];
			j=0
					while(j<=(this.H - i)) {
						weightVector = new ListBuffer[Double];
						weightVector.insert(0,(1.0 * i) / (1.0 * this.H));
						weightVector.insert(1,(1.0 * j) / (1.0 * this.H));
						weightVector.insert(2,(1.0 * (this.H - i - j)) / (1.0 * this.H));

						chr = new Chromosome(rnd_genes, this.numObjectives, this.T);
						chr.weightVector=weightVector;

						this.uPop.add(chr);
						count=count+1;
						j=j+1;
					}
			i=i+1;
		}
	}


	/**
	 * Compute euclidean distance between any two weight vector to find 
       the number of closet weight vectors(neighbors vectors) to each weight vector 
	 */

	def  computeEuclidean():ListBuffer[Distance]= {
			var distance:ListBuffer[Distance] = new ListBuffer[Distance];
	for(i<-0 until this.uPop.size()){
		distance.insert(i, new Distance(this.uPop.size()));
		distance(i).init();
	}

	for(i<-0 until this.uPop.size()){
		for(j<-(i+1) until this.uPop.size()){
			var x=this.uPop.get(i).computeDistance(this.uPop.get(j).weightVector)
					distance(i).ArrayD.update(j, x)
					distance(j).ArrayD.update(i, x)
		}
	}

	return distance;
	}

	def closestVector(dist:ListBuffer[Double], vector:Int):ListBuffer[Int] ={
			var i, j, temp:Int=0;
			var closest:ListBuffer[Int] = new ListBuffer[Int];
			var index:Array[Int] = new Array[Int](dist.length);

			for (i<-0 until index.length)  index(i) = i;
					i=1;		
					while (i< index.length) {
						j=0;
						while (j<(index.length-i)) {
							if (dist(index(j+1)) < dist(index(j))) {
								temp = index(j+1);
								index.update((j+1),index(j));
								index.update(j,temp);
							}
							j=j+1;
						}
						i=i+1;
					}
					i=0;
					j=0;
					while(i<this.T){
						if (index(j) != vector) {
							closest.insert(i,index(j));
							i=i+1;
						}
						j=j+1;
					}

					return (closest);
	}

	def computeWeightVectorsNeighbors()={
		var distance:ListBuffer[Distance]=null;
	distance = this.computeEuclidean();

	for (i<-0 until this.uPop.size())
		this.uPop.get(i).vectorsNeighbors=this.closestVector(distance(i).ArrayD, i);

	}   

	def bestObjectiveValue(numberObj:Int):Double={
		var i:Int=0;
	var bestValue:Double=0;

	if (numberObj > 0)  bestValue = 1.0;
	else{
		bestValue = this.uPop.get(0).objectives(numberObj);
		for(i<-1 until this.uPop.size()) {
			if(bestValue < this.uPop.get(i).objectives(numberObj))  bestValue = this.uPop.get(i).objectives(numberObj);
		}
	}
	return bestValue;
	}

	def minObjectiveValue(numberObj:Int):Double={
		var minValue:Double=0;

	if (numberObj == 1)  minValue = -1.0;
	else  minValue = 0.0;

	return minValue;
	}

	def initialize_Z()={
		for(i<-0 until this.numObjectives){
			this.Z(i) = this.bestObjectiveValue(i);
			this.Z_min(i) = this.minObjectiveValue(i);
		}
	}

	def initializePopulation()= {
		var i, k, pos:Int=0;
		var tr_not_marked:ArrayList[Int]=new ArrayList(this.nTrans)
		var chromo:Chromosome=null;


		this.uPop.clear();

		this.setWeightVectorsToEachChromo();
		this.computeWeightVectorsNeighbors();

		//initialize population
		this.trials = 0;

		for(i<-0 until this.nTrans){ tr_not_marked.add(i)}

		k = 0;
		while(k < this.uPop.size()) {
			if(tr_not_marked.size() == 0)
				for(i<-0 until this.nTrans){ tr_not_marked.add(i)}

			pos =  tr_not_marked.get((Randomize.Randint(0, tr_not_marked.size())));
			chromo = this.generateChromoCoveredPosNeg(pos);
			chromo.weightVector=this.uPop.get(k).weightVector;
			chromo.vectorsNeighbors=(this.uPop.get(k).vectorsNeighbors);
			if((!this.equalChromotoPop(chromo, this.uPop))){
       	if(chromo.support > this.minSupport){
					if(!(chromo.support > (1.0 - this.minSupport))){
						if(chromo.CF > 0) {
       
							this.uPop.set(k, chromo.copy());
							this.deleteTransCovered(this.uPop.get(k), tr_not_marked);
							k=k+1;
						}}}}
		}

		this.initialize_Z();
		this.EP = this.nonDominateSelection(this.uPop);
	}

	def generateChromoCoveredPosNeg(pos_example:Int):Chromosome={
		var i, j,tmp, nAnts:Int=0;
		var lbt, ubt, value:Double=0;
		var example:Row=null;
		var sample:Array[Int]=null;
		var rnd_genes:Array[Gene]=null;
		var chromo:Chromosome=null;

		example = this.dataset.getExample(pos_example);

		rnd_genes = new Array[Gene](this.nAttr);
		sample = new Array[Int](this.nAttr);

		for(i<-0 until this.nAttr){  rnd_genes(i) = new Gene();}

		for(i<-0 until this.nAttr){  sample(i) = i;}

		//create chromo
		rnd_genes = new Array[Gene](this.nAttr);
		for(i<-0 until this.nAttr){  rnd_genes(i) = new Gene();}

		for(i<-0 until this.nAttr) {
			j = Randomize.Randint(0, this.nAttr);
			tmp = sample(i);
			sample(i) = sample(j);
			sample(j) = tmp;
		}

		nAnts = Randomize.Randint(1, this.nAttr);

		// Antecedent
		while(i< nAnts) {
			rnd_genes(sample(i)).attr=sample(i); 
			rnd_genes(sample(i)).ac= 0;  
			if(Randomize.RandintClosed(0, 1) == 1){
				rnd_genes(sample(i)).pn=true;
			}
			else{
				rnd_genes(sample(i)).pn=false;
			}
			value = example.getAs[Double]((sample(i)));

			if  (this.dataset.getAttributeType(sample(i)) != 0) {
				if  (this.dataset.getAttributeType(sample(i)) == 2) {
					if(rnd_genes(sample(i)).pn){
						lbt = Math.max(value - (this.dataset.Amplitude(sample(i)) / (this.af * 4)), this.dataset.emin(sample(i)));
						ubt = Math.min(value + (this.dataset.Amplitude(sample(i)) / (this.af * 4)), this.dataset.emax(sample(i)));
					}
					else{ //is negative
						if((value -  this.dataset.emin(sample(i))) > (this.dataset.emax(sample(i)) - value)) { // left of the value
							lbt = value - ((value -  this.dataset.emin(sample(i))) - this.dataset.Amplitude(sample(i)) / (this.af * 4));
							ubt = this.dataset.emin(sample(i)) + ((value -  this.dataset.emin(sample(i))) - this.dataset.Amplitude(sample(i)) / (this.af * 4));                
						}
						else { // right of the value 
							lbt = this.dataset.emax(sample(i)) - ((this.dataset.emax(sample(i)) -  value) - this.dataset.Amplitude(sample(i)) / (this.af * 4));
							ubt = value + ((this.dataset.emax(sample(i)) -  value) - this.dataset.Amplitude(sample(i)) / (this.af * 4));               
						}
					}
				}
				else {
					if(rnd_genes(sample(i)).pn){
						lbt = Math.max( (value - (this.dataset.Amplitude(sample(i)) / (this.af * 4))), this.dataset.emin(sample(i)).intValue());
						ubt = Math.min( (value + (this.dataset.Amplitude(sample(i)) / (this.af * 4))), this.dataset.emax(sample(i)).intValue());
					}
					else{ //is negative
						if((value -  this.dataset.emin(sample(i))) > (this.dataset.emax(sample(i)) - value)) { // left of the value
							lbt =  (value - ((value -  this.dataset.emin(sample(i))) - this.dataset.Amplitude(sample(i)) / (this.af * 4))).intValue();
							ubt =( this.dataset.emin(sample(i)) + ((value -  this.dataset.emin(sample(i))) - this.dataset.Amplitude(sample(i)) / (this.af * 4))).intValue();                
						}
						else { // right of the value 
							lbt =  (this.dataset.emax(sample(i)) - ((this.dataset.emax(sample(i)) -  value) - this.dataset.Amplitude(sample(i)) / (this.af * 4))).intValue();
							ubt =  (value + ((this.dataset.emax(sample(i)) -  value) - this.dataset.Amplitude(sample(i)) / (this.af * 4))).intValue();               
						}
					}
				}
			}
			else {
				lbt = value;
				ubt = value;
			}

			rnd_genes(sample(i)).lb=lbt;
			rnd_genes(sample(i)).ub=ubt;
			rnd_genes(sample(i)).min_attr=this.dataset.emin(sample(i));
			rnd_genes(sample(i)).max_attr=this.dataset.emax(sample(i));
			rnd_genes(sample(i)).type_g=this.dataset.getAttributeType(sample(i));
			i=i+1;
		}

		// Consequent
		rnd_genes(sample(i)).attr=sample(i); 
		rnd_genes(sample(i)).ac=1;  

		value = example.getAs[Double]((sample(i)));

		if  (this.dataset.getAttributeType(sample((i))) != 0) {
			if  (this.dataset.getAttributeType(sample(i)) == 2) {
				lbt = Math.max(value - (this.dataset.Amplitude(sample(i)) / (this.af * 4)), this.dataset.emin(sample(i)));
				ubt = Math.min(value + (this.dataset.Amplitude(sample(i)) / (this.af * 4)), this.dataset.emax(sample(i)));
			}
			else {
				lbt = Math.max( (value - (this.dataset.Amplitude(sample(i)) / (this.af * 4))), this.dataset.emin(sample(i)));
				ubt = Math.min( (value + (this.dataset.Amplitude(sample(i)) / (this.af * 4))), this.dataset.emax(sample(i)));
			}
		}
		else {
			lbt =value;
			ubt =  value;
		}
		rnd_genes(sample(i)).lb=lbt;
		rnd_genes(sample(i)).ub=ubt;
		rnd_genes(sample(i)).type_g=this.dataset.getAttributeType(sample(i));
		rnd_genes(sample(i)).min_attr=this.dataset.emin(sample(i));
		rnd_genes(sample(i)).max_attr=this.dataset.emax(sample(i));
		if(Randomize.RandintClosed(0, 1) == 1){
			rnd_genes(sample(i)).pn=true;
		}
		else{
			rnd_genes(sample(i)).pn=false;
		}


		// Rest of the rule
		for (i <- nAnts + 1 until this.nAttr) {
			rnd_genes(sample(i)).attr=sample(i); 
			rnd_genes(sample(i)).ac=(-1);  

			if  (this.dataset.getAttributeType(sample(i)) != 0) {
				if  (this.dataset.getAttributeType(sample(i)) == 2) {
					value = Randomize.RanddoubleClosed (this.dataset.emin(sample(i)), this.dataset.emax(sample(i)));
					lbt = Math.max(value - (this.dataset.Amplitude(sample(i)) / (this.af * 4)), this.dataset.emin(sample(i)));
					ubt = Math.min(value + (this.dataset.Amplitude(sample(i)) / (this.af * 4)), this.dataset.emax(sample(i)));
				}
				else {
					value = Randomize.RandintClosed (this.dataset.emin(sample(i)).intValue(), this.dataset.emax(sample(i)).intValue());
					lbt = Math.max((value - (this.dataset.Amplitude(sample(i))) / (this.af * 4)), this.dataset.emin(sample(i)));
					ubt = Math.min((value + (this.dataset.Amplitude(sample(i))) / (this.af * 4)), this.dataset.emax(sample(i)));
				}
			}
			else {
				value = Randomize.RandintClosed (this.dataset.emin(sample(i)).intValue(), this.dataset.emax(sample(i)).intValue());
				lbt =value
						ubt =value;
			}

			rnd_genes(sample(i)).lb=lbt;
			rnd_genes(sample(i)).ub=ubt;
			rnd_genes(sample(i)).type_g=this.dataset.getAttributeType(sample(i));
			rnd_genes(sample(i)).min_attr=this.dataset.emin(sample(i));
			rnd_genes(sample(i)).max_attr=this.dataset.emax(sample(i));
			if(Randomize.RandintClosed(0, 1) == 1){
				rnd_genes(sample(i)).pn=true;
			}
			else{
				rnd_genes(sample(i)).pn=false;
			}

		}

		chromo = new Chromosome(rnd_genes, this.numObjectives, this.T);        
		chromo.computeObjetives(this.dataset);
		this.trials=this.trials+1;

		return chromo;
	}

	def deleteTransCovered ( chromo:Chromosome, tr_not_marked:ArrayList[Int])={
		var i:Int=0;
	var example:Row=null;
	i = tr_not_marked.size-1;
	while( i >= 0) {
		example = this.dataset.getExample(tr_not_marked.get(i));
		if (chromo.isCovered(example))  tr_not_marked.remove(i); 
		i=i-1;
	}
	i=i-1;
	} 

	def tr_notCovered_NoDominateSolutions():ArrayList[Int]={ 
			var i:Int=0;
	var tr_not_marked:ArrayList[Int] = new ArrayList[Int](this.nTrans);

	for(i<-0 until this.nTrans)  tr_not_marked.add(i,i);
			for(i<-0 until this.EP.size())  this.deleteTransCovered(this.EP.get(i), tr_not_marked);

					return tr_not_marked;
	}

	def restartPop()={
    println("Restart Pop, please wait")
		var i, k, pos, cont:Int=0;
		var tr_not_marked:ArrayList[Int]=new ArrayList[Int](this.nTrans);
		var chromo:Chromosome=null;

		tr_not_marked = this.tr_notCovered_NoDominateSolutions();

		this.uPop.clear();

		this.setWeightVectorsToEachChromo();
		this.computeWeightVectorsNeighbors();

		//initialize population
		k = 0;
		cont = 0;
		while(k < this.uPop.size()) {
			if(tr_not_marked.size == 0)  
				for(i<-0 until this.nTrans)  tr_not_marked.add(i,i);

						//create chromo
						pos = tr_not_marked.get(Randomize.Randint(0, tr_not_marked.size()));
						chromo = this.generateChromoCoveredPosNeg(pos);
						chromo.weightVector=this.uPop.get(k).weightVector;
						chromo.vectorsNeighbors=this.uPop.get(k).vectorsNeighbors;

						if((!this.equalChromotoPop(chromo, this.uPop)) && (chromo.support > this.minSupport) && (!(chromo.support > (1.0 - this.minSupport))) && (chromo.CF > 0)) {
					              this.uPop.set(k, chromo.copy());
							this.deleteTransCovered(this.uPop.get(k), tr_not_marked);
							this.update_Z(chromo);
							this.updateEP(chromo);
							k=k+1;
							cont = 0;
						}
						else if (cont > 1000) {
							tr_not_marked.clear();
							cont = 0;
						}
						else  cont=cont+1;
		}
	}


	def update_Z( chr:Chromosome)={
		var i:Int=0;

	for(i<-0 until this.numObjectives) {
		if(this.Z(i) < chr.objectives(i))  this.Z(i) = chr.objectives(i);
		if(this.Z_min(i) > chr.objectives(i)) this.Z_min(i) = chr.objectives(i);
	}
	}

	def updateSolutions(child:Chromosome, tipo:Int, r1:Int)={
		var i, c, l, numIndP:Int=0; 
		var perm:Array[Int]=null;

		if(tipo == this.UPDATE_SOLUTION_NEIGHBOR) numIndP = this.T;
		else numIndP = this.uPop.size();

		perm = random_permutation(numIndP);

		while((c <= this.nr) && (i < numIndP)) {
			if(tipo == this.UPDATE_SOLUTION_NEIGHBOR)  l = this.uPop.get(r1).vectorsNeighbors(i);
			else  l = perm(i);

			child.weightVector=this.uPop.get(l).weightVector;
			child.vectorsNeighbors=this.uPop.get(l).vectorsNeighbors;

			if(this.tchebycheffApproach(child) <= this.tchebycheffApproach(this.uPop.get(l))) {
				this.uPop.set(l, child.copy());
				c=c+1;
			}
			i=i+1;
		}
		if (c > 0)  this.updatePop=this.updatePop+1;
	}


	def tchebycheffApproach( chr:Chromosome):Double={
		var g_max, g:Double=0;
		var i:Int=0;

		g_max = chr.weightVector(0) * (Math.abs(this.Z(0) - chr.objectives(0)) / (this.Z(0) - this.Z_min(0)));

		for(i<-1 until this.numObjectives){
			g = chr.weightVector(i) * (Math.abs(this.Z(i) - chr.objectives(i) / (this.Z(i) - this.Z_min(i))));
			if(g > g_max)  g_max = g;
		}

		return g_max;
	}

	def equalChromotoPop( chromo:Chromosome, pop:ArrayList[Chromosome]):Boolean={
		var i:Int=0;
	var aux:Chromosome=null;
	var  value:Boolean = false;

	while( (!value) && (i < pop.size())) {
		aux = pop.get(i);
		if(chromo.equals(aux))  value = true;
		i=i+1;
	}

	return value;
	}


	def roundDouble(number:Double, decimalPlace:Int):Double={
		var numberRound:Double=0;

	if(!(Double.PositiveInfinity==number|| Double.NegativeInfinity==number)&&(!(Double.NaN==number))){
		var bd:BigDecimal = BigDecimal.valueOf(number);
	bd = bd.setScale(decimalPlace, BigDecimal.ROUND_UP);
	numberRound = bd.doubleValue();
	return numberRound;
	}else return number;
	}
	/*
	def saveReport( rules:ListBuffer[AssociationRule], w:PrintWriter)= {
		int i, j, r, cnt_cov_rec;
		double avg_sup = 0.0, avg_conf = 0.0, avg_ant_length = 0.0, avg_lift = 0.0, avg_conv = 0.0, avg_CF = 0.0, avg_netConf = 0.0, avg_yulesQ = 0.0;
		int[] covered;   
		AssociationRule rule;

		covered = new int[this.nTrans];
		for (i=0; i < this.nTrans; i++)  covered[i] = 0;

		for (r=0; r < rules.size(); r++) {
			rule = rules.get(r);

			avg_sup += rule.getSupport();
			avg_conf += rule.getConfidence();
			avg_lift += rule.getLift();
			avg_ant_length += (rule.getnAnts()+1);
			avg_conv += rule.getConv();
			avg_CF += rule.getCF();
			avg_netConf += rule.getNetConf();
			avg_yulesQ += rule.getYulesQ();

			for (j=0; j < this.nTrans; j++) {
				if (covered[j] < 1) {
					if (rule.isCovered(this.dataset.getExample(j)))  covered[j] = 1;
				}
			}
		}

		cnt_cov_rec = 0;
		for (i=0; i < this.nTrans; i++)  cnt_cov_rec += covered[i];

		w.println("\nNumber of Frequent Itemsets found: " + "-"); 
		System.out.println("\nNumber of Frequent Itemsets found: " + "-");
		w.println("\nNumber of Association Rules generated: " + rules.size());  
		System.out.println("Number of Association Rules generated: " + rules.size());

		if (! rules.isEmpty()) {
			w.println("Average Support: " + roundDouble(( avg_sup / rules.size() ),2));
			System.out.println("Average Support: " + roundDouble(( avg_sup / rules.size() ),2));
			w.println("Average Confidence: " + roundDouble(( avg_conf / rules.size() ),2));
			System.out.println("Average Confidence: " + roundDouble(( avg_conf / rules.size() ),2));
			w.println("Average Lift: " + roundDouble(( avg_lift / rules.size() ),2));
			System.out.println("Average Lift: " + roundDouble(( avg_lift / rules.size() ),2));
			w.println("Average Conviction: " + roundDouble(( avg_conv / rules.size() ),2));
			System.out.println("Average Conviction: " + roundDouble(( avg_conv/ rules.size() ),2));
			w.println("Average Certain Factor: " + roundDouble(( avg_CF/ rules.size() ),2));
			System.out.println("Average Certain Factor: " + roundDouble(( avg_CF/ rules.size()),2));
			w.println("Average Netconf: " + roundDouble(( avg_netConf/ rules.size() ),2));
			System.out.println("Average Netconf: " + roundDouble(( avg_netConf/ rules.size()),2));
			w.println("Average YulesQ: " + roundDouble(( avg_yulesQ/ rules.size() ),2));
			System.out.println("Average YulesQ: " + roundDouble(( avg_yulesQ/ rules.size()),2));
			w.println("Average Number of Antecedents: " + roundDouble(( avg_ant_length / rules.size() ),2));
			System.out.println("Average Number of Antecedents: " + roundDouble(( avg_ant_length / rules.size() ),2));
			w.println("Number of Covered Records (%): " + roundDouble((100.0 * cnt_cov_rec) / this.nTrans, 2));
			System.out.println("Number of Covered Records (%): " + roundDouble((100.0 * cnt_cov_rec) / this.nTrans, 2));
		}
	}
	 */ 


	def removeRedundant (upop:ArrayList[Chromosome]):ArrayList[Chromosome]= {
		var i, j:Int=0;
		var stop:Boolean=false;
		var chromo1, chromo2:Chromosome=null;
		Qsort(upop);
		while (i<upop.size()) {
			stop = false;
			j = upop.size()-1
					while (j >=0 && !stop) {
						if (j != i) {
							chromo1 = upop.get(i);
							chromo2 = upop.get(j);
							if (chromo1.nAnts == chromo2.nAnts) {
								if (chromo1.isSubChromo(chromo2)) {
									if (chromo1.CF >= chromo2.CF) {
										upop.remove(j);
										if (j < i) i=i-1;
									}
									else {
										upop.remove(i);
										i=i-1;
										stop = true;
									}
								}
							}
							else if (chromo1.nAnts > chromo2.nAnts) stop = true;
						}
						j=j-1;
					}
			i=i+1;
		}
		return upop;
	} 


	def printPareto()= {
		var i:Int=0;
	var stop:Boolean=false;
	var chromo:Chromosome=null;
	this.paretos += "";
	this.paretos += ("Support\tantecedent_support\tconsequent_support\tConfidence\tLift\tConv\tCF\tNetConf\tYulesQ\tnAttributes\n");

	while(i < this.EP.size() && !stop) {
		chromo = this.EP.get(i);
		this.paretos += ("" + roundDouble(chromo.support,2) + "\t" + roundDouble(chromo.antsSupport,2) + "\t" + roundDouble(chromo.consSupport,2) + "\t" + roundDouble(chromo.confidence,2) + "\t" + roundDouble(chromo.objectives(0),2) + "\t" + roundDouble(chromo.conv,2) + "\t" + roundDouble(chromo.CF,2) + "\t" + roundDouble(chromo.netConf,2) + "\t" + roundDouble(chromo.yulesQ,2) + "\t" + (chromo.nAnts+1) + "\n");
		i=i+1;
	}
	}

	def generateRulesPareto(): ListBuffer[AssociationRule]= {
			var i:Int=0;
	var chromo:Chromosome=null;
	var rulesPareto:ListBuffer[AssociationRule] = new ListBuffer[AssociationRule];

	for (i<-0 until this.EP.size()) {
		chromo = this.EP.get(i);
		rulesPareto+=new AssociationRule(chromo, this.trials);
	}
	return rulesPareto;
	}

	def getParetos():String= {
			return (this.paretos);
	}

	def check_dominance ( a:Chromosome, b:Chromosome):Int={
			var i:Int=0;
	var flag1:Int=0;
	var flag2:Int=0;

	for (i<-0 until this.numObjectives){
		if (a.objectives(i) > b.objectives(i))  flag1 = 1;
		else if (a.objectives(i) < b.objectives(i))  flag2 = 1;
	}

	if ((flag1 == 1) && (flag2 == 0))  return (1);
	else if ((flag1 == 0) && (flag2 == 1))  return (-1);
	else  return (0);
	}
	def humanRule(list:List[String]):ListBuffer[String]={
			var listOut:ListBuffer[String]=new ListBuffer[String];
	var sumSop:Double=0;
	var sumConf:Double=0;
	var sumLift:Double=0;
		var sumConv:Double=0
		var sumCF:Double=0
		var sumNC:Double=0
		var sumYulesQ:Double=0
	for(i<-0 until EP.size()){
		listOut.insert(i,"Rule id="+i+"  "++this.EP.get(i).getRule(list));
		sumSop=sumSop+this.EP.get(i).support;
		sumConf=sumConf+this.EP.get(i).confidence;
		sumLift=sumLift+ +this.EP.get(i).objectives(0);
		sumConv=sumConv+this.EP.get(i).conv
		sumCF=sumCF+this.EP.get(i).CF
		sumNC=sumNC+this.EP.get(i).netConf
		sumYulesQ=sumYulesQ+this.EP.get(i).yulesQ
	}
	sumSop=sumSop/this.EP.size();
	sumConf=sumConf/this.EP.size();
	sumLift=sumLift/this.EP.size();
		sumConv=sumConv/this.EP.size()
		sumCF=sumCF/this.EP.size()
		sumNC=sumNC/this.EP.size()
		sumYulesQ=sumYulesQ/this.EP.size()
	listOut.insert(listOut.length, "Promedio del Soporte"+sumSop);
	listOut.insert(listOut.length, "Promedio de la Confianza"+sumConf);
	listOut.insert(listOut.length, "Promedio del Lift"+sumLift);
		listOut.insert(listOut.length, "Promedio del Conviction"+sumConv);
		listOut.insert(listOut.length, "Promedio del Certain Factor"+sumCF);
		listOut.insert(listOut.length, "Promedio del Netconf"+sumNC);
		listOut.insert(listOut.length, "Promedio del YulesQ"+sumYulesQ);
	return listOut;
	}

	def Qsort(xs: ArrayList[Chromosome]) {
		def swap(i: Int, j: Int) {
			val t = xs.get(i);
      xs.set(i,xs.get(j));
      xs.set(j,t)
		}
		def qsort1(l: Int, r: Int) {
			val pivot = xs.get((l + r) / 2).nAnts
					var i = l; var j = r
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
		qsort1(0, xs.size() - 1)
	}
}
