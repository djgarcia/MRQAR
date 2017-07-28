package org.apache.spark.mllib.fpm.MOPNAR

import org.apache.spark.sql.Row

class Gene extends Serializable{
	def NOT_INVOLVED = -1;
	def ANTECEDENT = 0;
	def CONSEQUENT = 1;

	var attr=0
			var ac:Int=0;
			var pn:Boolean=true
			var lb:Double=0
			var ub:Double=0
			var min_attr=0.0
			var max_attr=0.0
			var type_g=0

			def Gene() {
	}

	def copy():Gene= {
			var gene:Gene = new Gene();

	gene.attr = this.attr;
	gene.ac = this.ac;
	gene.pn = this.pn;
	gene.lb = this.lb;
	gene.ub = this.ub;
	gene.type_g = this.type_g;
	gene.min_attr = this.min_attr;
	gene.max_attr = this.max_attr;

	return gene;
	}

	def invert(type_g:Int,min_attr: Double,max_attr: Double) {    
		if(this.ac==NOT_INVOLVED)
			this.ac =ANTECEDENT;
		else if(this.ac== ANTECEDENT)
			this.ac =CONSEQUENT;
		else if(this.ac==CONSEQUENT)
			this.ac = NOT_INVOLVED;

		this.pn = !this.pn;

		if ( type_g != 0 ) {
			if ( type_g == 2 ) {
				this.lb = (Randomize.RandClosed() * (this.lb - min_attr) + min_attr);
				this.ub = (Randomize.RandClosed() * (max_attr - this.ub) + this.ub);
			}
			else {
				this.lb = Randomize.RandintClosed(min_attr.intValue(), this.lb.intValue());
				this.ub = Randomize.RandintClosed(this.ub.intValue(), max_attr.intValue());
			}
		}
		else {
			if (this.lb == max_attr) {
				this.lb = this.min_attr.intValue();
				this.ub =this.min_attr.intValue();}
			else {
				this.lb=this.lb+1;
				this.ub=this.ub+1;
			}

		}
	}

	def equals(g:Gene):Boolean= { 
			var lb_posit:Double=0
					var ub_posit:Double=0
					var temp=0;
	if (g.attr == this.attr) {
		if (g.ac == this.ac) {
			if (g.pn == this.pn) {

				if ( Math.abs(g.lb - this.lb)<= 0.00001) {
					if (Math.abs((g.ub - this.ub)) <= 0.00001) return true;
				}
			}
			else {
				if (!g.pn){
					if(g.lb == this.min_attr){
						lb_posit = g.ub;            
						ub_posit = this.max_attr;
						if (g.type_g == 2)  lb_posit+= 0.0001;
						else  lb_posit += 1;

						if (Math.abs(lb_posit - this.lb) <= 0.00001) {
							if (Math.abs(ub_posit - this.ub) <= 0.00001) return true;
						}
					}
					if(g.ub == this.max_attr){
						lb_posit = this.min_attr;
						ub_posit = g.lb;
						if(type_g == 2)  ub_posit -= 0.0001;
						else  ub_posit -= 1;

						if (Math.abs(lb_posit - this.lb) <= 0.00001) {
							if (Math.abs(ub_posit - this.ub) <= 0.00001) return true;
						}
					}

				}
				else {
					if(!this.pn){
						if(this.lb == this.min_attr){
							lb_posit = this.ub;
							ub_posit = this.max_attr;
							if (type_g == 2)  lb_posit+= 0.0001;
							else  lb_posit += 1;

							if (Math.abs(lb_posit - g.lb) <= 0.00001) {
								if (Math.abs(ub_posit - g.ub) <= 0.00001) return true;
							}
						}
						if(this.ub == this.max_attr){
							lb_posit =this.min_attr;
							ub_posit = this.lb;
							if(type_g == 2)  ub_posit -= 0.0001;
							else  ub_posit -= 1;

							if (Math.abs(lb_posit - g.lb) <= 0.00001) {
								if (Math.abs(ub_posit - g.ub) <= 0.00001) return true;
							}
						}
					} 
				}
			}
		}
	}

	return false;
	}

	override def toString():String= {
			var out="Attr: " + attr + "AC: " + ac + "; PN: " + pn + "; LB: " + lb + "; UB: " + ub;
			return out;
	}

	def randAct () :Int={
			return (Randomize.RandintClosed(NOT_INVOLVED, CONSEQUENT));

	}
	def tuneInterval (dataset: myDataset, covered:Array[Int])= {
		var i:Int=0;
	var r:Int=0
			var nData:Int=0;
	var min:Double=0
			var max:Double=0;
	var delta:Double=0;
	var example:Row=null;

	nData = dataset.nTrans;
	if(this.pn){
		min = this.ub;
		max = this.lb;

		for (i<-0 until nData) {
			if (covered(i) > 0) {
				example = dataset.getExample(i);
				var value:Double=0;

				value = example.getAs[Double](this.attr);
				if (value < min)  min = value;
				if (value > max)  max = value;
			}
		}

		this.ub = max;
		this.lb = min;

	} 
	else//negative interval
	{
		if(dataset.getAttributeType(this.attr)== 2) delta = 0.0001;
		else delta = 1.0;

		min = dataset.emax(this.attr) + delta;
		max = dataset.emin(this.attr) - delta;

		for (r<-0 until nData) {
			if (covered(r) > 0) {
				example = dataset.getExample(r);
				var value:Double=0;
	
					value = example.getAs[Double](this.attr);
				
				if ( (value < min) && (value > this.ub) ) min = value;
				if ( (value > max) && (value < this.lb) ) max = value;

			}
		}
		this.lb = (max + delta);
		this.ub = (min - delta);
	}
	}


	def isCover (x:Int, value:Double):Boolean= {
		var covered:Boolean=false;

	if (this.attr != x)  return (false);

	covered = true;

	if  (this.pn) {
		if ((value < this.lb) || (value > this.ub))  covered = false;
	}
	else {
		if ((value >= this.lb) && (value <= this.ub))  covered = false;
	}

	return (covered);
	}

	def isSubGen (g:Gene):Boolean= {

		var lb_posit:Double=0
				var ub_posit:Double=0

				if (this.attr != g.attr)  return (false);
	if (this.ac != g.ac)  return (false);
	if ((this.pn) && (g.pn)){
		if (Math.abs(g.lb - this.lb) > 0.00001)  return (false);
		if (Math.abs(g.ub - this.ub) > 0.00001)  return (false);
	}
	else if ((!this.pn) && (!g.pn)){//negative interval
		if (Math.abs(g.lb - this.lb) > 0.00001)  return (false);
		if (Math.abs(g.ub - this.ub) > 0.00001)  return (false);
	}
	else {
		if(!g.pn){ // g negative interval
			if(g.lb == this.min_attr){
				lb_posit = g.ub;
				ub_posit = this.max_attr;
				if (type_g == 2)  lb_posit+= 0.0001;
				else  lb_posit += 1;

				if (Math.abs(lb_posit - this.lb) > 0.00001)  return (false); 
				if (Math.abs(ub_posit - this.ub) > 0.00001)  return (false);
			}   
			if(g.ub == this.max_attr){
				lb_posit = this.min_attr;
				ub_posit = g.lb;
				if(type_g == 2)  ub_posit -= 0.0001;
				else  ub_posit -= 1;

				if (Math.abs(lb_posit - this.lb) > 0.00001)  return (false); 
				if (Math.abs(ub_posit - this.ub) > 0.00001)  return (false);
			}

		}
		else { // this negative interval
			if(!this.pn){
				if(this.lb == this.min_attr){
					lb_posit = this.ub;
					ub_posit = this.max_attr;
					if(type_g == 2)  lb_posit+= 0.0001;
					else  lb_posit += 1;

					if (Math.abs(lb_posit - g.lb) > 0.00001)  return (false); 
					if (Math.abs(ub_posit - this.ub) > 0.00001) return (false);
				}   
				if(this.ub == this.max_attr){
					lb_posit = this.min_attr;
					ub_posit = this.lb;
					if(type_g == 2)  ub_posit -= 0.0001;
					else  ub_posit -= 1;

					if (Math.abs(lb_posit - g.lb) > 0.00001)  return (false); 
					if (Math.abs(ub_posit - this.ub) > 0.00001)  return (false);
				}
			} 
		}
	} 
	return (true);
	}

}