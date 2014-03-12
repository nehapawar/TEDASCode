import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.CfsSubsetEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.GreedyStepwise;
import weka.attributeSelection.PrincipalComponents;
import weka.attributeSelection.Ranker;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.trees.J48;

import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;

/**
 * This class is for training the models(Decision Tree, Naive Bayes and SVM).
 * 
 * @author Tobias Garrick Lei
 * 
 */
public class TweetsClassifierTraining {
	public Instances trainSet;
	public Instances testSet;
	private static final String DATE_FORMAT_NOW = "yyyy_MM_dd";
	public void TweetsClassifierTraining() {

	}

//	public static void main(String arg[]) {
//		try {
//			String time = getTime();			
//			// 1. Load data
//			
//			loadData("train", "indexerData/labeledtweets/l_trainSet_" + time +".arff");
//			loadData("test", "indexerData/labeledtweets/l_testSet_" + time +".arff");
//			/*
//			loadData("test", "indexerData/labeledtweets/l_testSet_" + time +".arff");
//			loadData("train", "indexerData/labeledtweets/l_trainSet_" + time +".arff");
//			*/
//			/*
//			useClassifier(trainSet);
//			useFilter(trainSet);
//			useLowLevel(trainSet);
//			*/
//			// 2. Train Classifiers
//			Classifier nb = train("nb");
//			Classifier dt = train("dt");
//			Classifier svm = train("svm");
//
//			// 3. Test the classifiers
//			test(nb);
//			test(dt);
//			test(svm);
//
//			// 4. Save classifiers to disk
//			saveClassifier("tc_nb.model", nb);
//			saveClassifier("tc_dt.model", dt);
//			saveClassifier("tc_svm.model", svm);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//
//	}

	// Get current time, it will be used as fileName to store tweets in that
	// day.
	private static String getTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}
	
	public void loadData(String type, String fn) throws Exception {
		DataSource source = new DataSource(fn);
		if (type.equals("train")) {
			trainSet = source.getDataSet();
			trainSet.setClassIndex(trainSet.numAttributes() - 1);
		} else {
			testSet = source.getDataSet();
			testSet.setClassIndex(testSet.numAttributes() - 1);
		}
	}

	public Classifier train(String modelName, Instances trainSet) throws Exception {
		Classifier cModel;
		if (modelName.equals("nb")) {
			// Create a nave bayes classifier
			cModel = (Classifier) new NaiveBayes();
		} else if (modelName.equals("dt")) {
			// Create a decision tree(C4.5) classifier
			cModel = (Classifier) new J48();
		} else {
			// Create a SVM
			cModel = (Classifier) new SMO();
		}
		cModel.buildClassifier(trainSet);
		return cModel;
	}
	
	public void deleteAttributes(String[] models, int folds){
		List<double[]> results = new ArrayList<double[]>();
		for(int i = 0; i < trainSet.numAttributes()-1; i++){
			System.out.println(i);
			Instances copy = new Instances(trainSet);
			copy.deleteAttributeAt(i);
			results.add(crossValidate(models,folds,copy));
		}
		double[] maxes = new double[models.length];
		int[] indexes = new int[models.length];
		for(int i = 0; i < results.size(); i++){
			double[] x = results.get(i);
			for(int j = 0; j < models.length; j++){
				if(x[j] > maxes[j]){
					maxes[j] = x[j];
					indexes[j] = i;
				}
			}
		}
		for(double d : maxes)
			System.out.println(d);
		for(int a : indexes)
			System.out.println(a);
		
	}
	
	public void PCA(){
		PrincipalComponents pca = new PrincipalComponents();
		try {
			pca.buildEvaluator(trainSet);
			for(int i = 0; i < trainSet.numAttributes()-1; i++)
				System.out.println(Integer.toString(i) + ": " + pca.evaluateAttribute(i));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void attributeSelection(){
		  Instances data = trainSet;
		  AttributeSelection attsel = new AttributeSelection();  // package weka.attributeSelection!
		  CfsSubsetEval eval = new CfsSubsetEval();
		  GreedyStepwise search = new GreedyStepwise();
		  search.setSearchBackwards(true);
		  attsel.setEvaluator(eval);
		  attsel.setSearch(search);
		  try {
			attsel.SelectAttributes(data);
			  int[] indices = attsel.selectedAttributes();
			  System.out.println(Utils.arrayToString(indices));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public double[] crossValidate(String[] models,int folds,Instances trainSet){
		Instances randData = new Instances(trainSet);
		randData.randomize(new Random(System.currentTimeMillis()));
		double[] falseNegTotals = new double[models.length];
		double[] falsePosTotals = new double[models.length];
		double[] correctness = new double[models.length];
		for(int n = 0; n < folds; n++){
			Instances train = randData.trainCV(folds,n);
			Instances test = randData.testCV(folds, n);
			for(int i = 0; i < models.length; i++){
				Evaluation eval;
				try {
					Classifier cModel = train(models[i],train);
					eval = new Evaluation(test);
					eval.evaluateModel(cModel, test);
					
					falseNegTotals[i] += eval.falseNegativeRate(1);
					falsePosTotals[i] += eval.falsePositiveRate(1);
					correctness[i] += eval.pctCorrect();
				}
				catch (Exception e) {e.printStackTrace();}
			}
		}
		for(int i = 0; i < models.length; i++){
			System.out.println(models[i] + "'s false pos: " + Double.toString(falsePosTotals[i] / folds));
			System.out.println(models[i] + "'s false negs: " + Double.toString(falseNegTotals[i] / folds));
			System.out.println(models[i] + "'s correctness: " + Double.toString(correctness[i] / folds));

		}
		double[] ret = new double[models.length];
		for(int i = 0; i < correctness.length; i++)
			ret[i] = correctness[i] / folds;
		return ret;
	}

	public void test(Classifier cModel) throws Exception {
		// Test the model
		Evaluation eval = new Evaluation(testSet);
		//Evaluation eval = new Evaluation(trainSet);


		// 10-fold cross-validation
		//eval.crossValidateModel(cModel, testSet, 10, new Random(System.currentTimeMillis()));
		//eval.crossValidateModel(cModel, testSet, 10, new Random(System.currentTimeMillis()));
		eval.evaluateModel(cModel,testSet);
		
		System.out.print("false negs: ");
		System.out.println(eval.falseNegativeRate(1));
		System.out.println("false pos: ");
		System.out.println(eval.falsePositiveRate(1));
		// Print out the result/summary
		System.out
				.println(eval
						.toSummaryString(
								"\nResults from "
										+ cModel.toString()
										+ " \n=========================================================================\n",
								false));

		// Get the confusion matrix
		double[][] cmMatrix = eval.confusionMatrix();
		for (int i = 0; i < cmMatrix.length; i++) {
			for (int j = 0; j < cmMatrix[0].length; j++) {
				System.out.print(cmMatrix[i][j] + ", ");
			}
			System.out.println();
		}
		// System.out.println(cmMatrix.length+"\t" + cmMatrix[0].length);
	}

	public void saveClassifier(String name, Classifier c)
			throws Exception {
		weka.core.SerializationHelper.write("classifierData/models/" + name, c);
	}

	/**
	 * uses the meta-classifier
	 */
	protected static void useClassifier(Instances data) throws Exception {
		System.out.println("\n1. Meta-classfier");
		AttributeSelectedClassifier classifier = new AttributeSelectedClassifier();
		CfsSubsetEval eval = new CfsSubsetEval();
		GreedyStepwise search = new GreedyStepwise();
		search.setSearchBackwards(true);
		J48 base = new J48();
		classifier.setClassifier(base);
		classifier.setEvaluator(eval);
		classifier.setSearch(search);
		Evaluation evaluation = new Evaluation(data);
		evaluation.crossValidateModel(classifier, data, 10, new Random(1));
		System.out.println(evaluation.toSummaryString());
	}

	/**
	 * uses the filter
	 */
	protected static void useFilter(Instances data) throws Exception {
		System.out.println("\n2. Filter");
		weka.filters.supervised.attribute.AttributeSelection filter = new weka.filters.supervised.attribute.AttributeSelection();
		CfsSubsetEval eval = new CfsSubsetEval();
		GreedyStepwise search = new GreedyStepwise();
		search.setSearchBackwards(true);
		filter.setEvaluator(eval);
		filter.setSearch(search);
		filter.setInputFormat(data);
		Instances newData = Filter.useFilter(data, filter);
		System.out.println(newData);
	}

	/**
	 * uses the low level approach
	 */
	protected static void useLowLevel(Instances data) throws Exception {
		System.out.println("\n3. Low-level");
		AttributeSelection attsel = new AttributeSelection();
		CfsSubsetEval eval = new CfsSubsetEval();
		GreedyStepwise search = new GreedyStepwise();
		search.setSearchBackwards(true);
		attsel.setEvaluator(eval);
		attsel.setSearch(search);
		attsel.SelectAttributes(data);
		int[] indices = attsel.selectedAttributes();
		System.out.println("selected attribute indices (starting with 0):\n"
				+ Utils.arrayToString(indices));
	}

}
