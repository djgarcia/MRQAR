package org.apache.spark.mllib.fpm.MOPNAR

import scala.collection.mutable.ListBuffer

class MOPNAR(d:myDataset,no:Int, nT:Int, h:Int, t:Int, prob:Double, NR:Int, p:Double, a:Double, pu:Double, s:Long) extends Serializable{
	/**
	 * <p>
	 * It gathers all the parameters, launches the algorithm, and prints out the results
	 * </p>
	 */

	var dataset:myDataset=d;

var proc:MOPNARProcess=null;
var associationRulesPareto:ListBuffer[AssociationRule]=null;
var Rules:ListBuffer[String]=null
var  nTrials:Int=nT;
var  numObjectives:Int=no;
var  H:Int=h;  //control the population size and weight vectors
var  T:Int=t; //Number of the weight vectors in the neighborhood
var  probDelta:Double=prob; //Probability that parent solutions are selected from the neighborhood
var  nr:Int=NR; //Maximal number of solutions replaced by each child solution
var  pm:Double=p;
var  af:Double=a;
var  percentUpdate:Double=pu;
var seed:Long=s;
var somethingWrong:Boolean = false; //to check if everything is correct.

/**
 * It launches the algorithm
 */


def  execute()= {
	if (somethingWrong) { //We do not execute the program
		System.err.println("An error was found");
		System.err.println("Aborting the program");
		//We should not use the statement: System.exit(-1);
	} 
	else {
		Randomize.initRand(seed)
		this.proc = new MOPNARProcess(this.dataset,this.numObjectives, this.nTrials, this.H, this.T, this.probDelta , this.nr, this.pm, this.af,this.percentUpdate);
		this.proc.run();
		Rules=this.proc.humanRule(dataset.listAtributeName);
    this.associationRulesPareto = this.proc.generateRulesPareto();

		//		try {
		//			var r, i:Int=0;
		//			var gen:Gene=null;
		//			var a_r:AssociationRule=null;
		//
		//			PrintWriter rules_writer = new PrintWriter(this.rulesFilename);
		//			PrintWriter values_writer = new PrintWriter(this.valuesFilename);
		//			PrintWriter pareto_writer = new PrintWriter(this.paretoFilename);
		//
		//			rules_writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		//			rules_writer.println("<association_rules>");        
		//			values_writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		//			values_writer.println("<values>");
		//			pareto_writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		//			pareto_writer.println("<values>");
		//
		//			for (r=0; r < this.associationRulesPareto.size(); r++) {
		//				a_r = this.associationRulesPareto.get(r);
		//
		//				ArrayList<Gene> ant = a_r.getAntecedents();
		//				ArrayList<Gene> cons = a_r.getConsequents();
		//
		//				rules_writer.println("<rule id=\"" + r + "\">");
		//				values_writer.println("<rule id=\"" + r + "\" rule_support=\"" + MOPNARProcess.roundDouble(a_r.getSupport(),2) + "\" antecedent_support=\"" + MOPNARProcess.roundDouble(a_r.getAntSupport(),2) + "\" consequent_support=\"" + MOPNARProcess.roundDouble(a_r.getConsSupport(),2) + "\" confidence=\"" + MOPNARProcess.roundDouble(a_r.getConfidence(),2) +"\" lift=\"" + MOPNARProcess.roundDouble(a_r.getLift(),2) + "\" conviction=\"" + MOPNARProcess.roundDouble(a_r.getConv(),2) + "\" certainFactor=\"" + MOPNARProcess.roundDouble(a_r.getCF(),2) + "\" netConf=\"" + MOPNARProcess.roundDouble(a_r.getNetConf(),2) +  "\" yulesQ=\"" + MOPNARProcess.roundDouble(a_r.getYulesQ(),2) + "\" nAttributes=\"" + (a_r.getnAnts()+1) + "\"/>");
		//				rules_writer.println("<antecedents>");      
		//
		//				for (i=0; i < ant.size(); i++) {
		//					gen = ant.get(i);
		//					createRule(gen, gen.getAttr(), rules_writer);
		//				}
		//
		//				rules_writer.println("</antecedents>");       
		//				rules_writer.println("<consequents>");      
		//
		//				for (i=0; i < cons.size(); i++) {
		//					gen = cons.get(i);
		//					createRule(gen, gen.getAttr(), rules_writer);
		//				}
		//
		//				rules_writer.println("</consequents>");
		//
		//				rules_writer.println("</rule>");          
		//			}
		//
		//			rules_writer.println("</association_rules>");
		//			values_writer.println("</values>");
		//			this.proc.saveReport(this.associationRulesPareto, values_writer);
		//			rules_writer.close();
		//			values_writer.close();
		//
		//			pareto_writer.print(this.proc.getParetos());
		//			pareto_writer.println("</values>");
		//			pareto_writer.close();
		//			totalTime = System.currentTimeMillis() - startTime;
		//			this.writeTime();
		//
		//			System.out.println("Algorithm Finished");
		//		}
		//		catch (FileNotFoundException e)
		//		{
		//			e.printStackTrace();
		//		}
	}
}

//public void writeTime() {
//	long seg, min, hor;
//	String stringOut = new String("");
//
//	stringOut = "" + totalTime / 1000 + "  " + this.namedataset + rulesFilename + "\n";
//	Files.addToFile(this.fileTime, stringOut);
//	totalTime /= 1000;
//	seg = totalTime % 60;
//	totalTime /= 60;
//	min = totalTime % 60;
//	hor = totalTime / 60;
//	stringOut = "";
//
//	if (hor < 10)  stringOut = stringOut + "0"+ hor + ":";
//	else   stringOut = stringOut + hor + ":";
//
//	if (min < 10)  stringOut = stringOut + "0"+ min + ":";
//	else   stringOut = stringOut + min + ":";
//
//	if (seg < 10)  stringOut = stringOut + "0"+ seg;
//	else   stringOut = stringOut + seg;
//
//	stringOut = stringOut + "  " + rulesFilename + "\n";
//	Files.addToFile(this.fileHora, stringOut);
//}
//
//
//private void createRule(Gene g, int id_attr, PrintWriter w) { 
//	w.println("<attribute name=\"" + Attributes.getAttribute(id_attr).getName() + "\" value=\"");
//
//	if (! g.getIsPositiveInterval()) w.print("NOT ");
//
//	if  (this.dataset.getAttributeType(id_attr) == myDataset.NOMINAL) w.print(Attributes.getAttribute(id_attr).getNominalValue ((int)g.getLowerBound()));
//	else w.print("[" + g.getLowerBound() + ", " + g.getUpperBound() + "]");
//
//	w.print("\" />");
//}    


}
