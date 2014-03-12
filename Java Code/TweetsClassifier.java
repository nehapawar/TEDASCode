import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * This class is for classifying tweets.
 * 
 * @author Tobias Garrick Lei
 * 
 */
public class TweetsClassifier {
	
	private Classifier cls;
	private double negProb;
	private ArrayList<Classifier> clss;
	private boolean isSMO;

	private static Classifier loadModel(String modelName) throws Exception {
		return (Classifier) weka.core.SerializationHelper.read(modelName
				+ ".model");
	}



	// Created for read-time classifying
	public void init(String modelName) {
		try {
			if (modelName.equals("combine")) {
				clss = new ArrayList<Classifier>();
				clss.add(loadModel("classifierData/models/testnb"));
				clss.add(loadModel("classifierData/models/testdt"));
				clss.add(loadModel("classifierData/models/testsvm"));
			} else if (modelName.equals("nb")) {
				//cls = loadModel(folderName + "_" + modelName);
				cls = loadModel("classifierData/models/testnb_2");
			} else if (modelName.equals("svm")) {
				//cls = loadModel(folderName + "_" + modelName);
				cls = loadModel("explorermodels/smo");
				isSMO = true;
			} else if (modelName.equals("dt")) {
				//cls = loadModel(folderName + "_" + modelName);
				//cls = loadModel("classifierData/models/testdt_2");
				cls = loadModel("explorermodels/dt");
			} else if (modelName.equals("nb_ns")){
				cls = loadModel("classifierData/models/testnb_nonsocial");
			} else {
				System.out.println("[ERROR] Please choose the right model!");
			}
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	// // Set up attributes
	private FastVector createdFastVector(ArrayList<String> featureNames) {
		FastVector atts = new FastVector();
		for (int i = 0; i < featureNames.size() ; i++) {
			atts.addElement(new Attribute(featureNames.get(i)));
		}
						
		return atts;
	}

	// NEED TO TEST THIS METHOD
	private Instances createInstance(double[] tweetData,
			ArrayList<String> featureNames) {
		FastVector fv = createdFastVector(featureNames);
		Instances data = new Instances("isCrime", fv, 0);
		data.setClassIndex(data.numAttributes() - 1);
		data.add(new Instance(1.0, tweetData));		
		//System.out.println(data);
		return data;
	}

	// Created for read-time classifying
	public boolean isCrime(double[] tweetData, ArrayList<String> featureNames)
			throws Exception {
		Instances i = createInstance(tweetData, featureNames);
		if (cls != null) {
			// System.out.println("VAL: " +
			negProb = cls.distributionForInstance(i.firstInstance())[0];
			//negProb = cls.distributionForInstance(i.firstInstance())[1];
			if(isSMO){
				if (negProb <= 0.4) {
					return true;
				} else
					return false;
			}
			if (cls.classifyInstance(i.firstInstance()) == 1.0 || negProb <= 0.4) {
				return true;
			} else
				return false;
		} else {
			int vote = 0;
			for (Classifier c : clss) {
				if (c.classifyInstance(i.instance(0)) == 1.0)
					vote++;
			}
			return (vote > 1);
		}
	}

	public double getNegativeProb(){
		return negProb;
	}	
	public static boolean isfp(double pred, double act){
		return pred>0.0 && act==0.0;
	}
	
}
