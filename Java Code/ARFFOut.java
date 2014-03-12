import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;


public class ARFFOut {
	
	private String infile = null;
	private RealTimeFeatures rtf = null;
	private BufferedWriter out;
	private List<Long> ids = new ArrayList<Long>();
	private List<Boolean> labels = new ArrayList<Boolean>();
	private List<Long> uids = new ArrayList<Long>();
	private List<String> texts = new ArrayList<String>();
	List<List<Double>> sftss = new ArrayList<List<Double>>();
	RecentTweetCache rtc = new RecentTweetCache("config/corpus.txt","config/stop_words.txt");

	
	/**
	 * @param outfile
	 * @param infile
	 */
	public ARFFOut(String infile, String featfile) {
		super();
		this.infile = infile;
		this.rtf = new RealTimeFeatures(featfile);
		try {
			BufferedReader br = new BufferedReader(new FileReader(infile));
			String line = null;
			String[] piece = null;
			while((line = br.readLine()) != null){
				piece = line.split(":",4);
				ids.add(Long.parseLong(piece[1]));
				if (piece[0].equals("0"))
					labels.add(false);
				else
					labels.add(true);
				uids.add(Long.parseLong(piece[2]));
				texts.add(piece[3]);
				
				line = br.readLine(); //sfts line
				piece = line.split(",");
				List<Double> nested = new ArrayList<Double>();
				for(String p : piece)
					nested.add(Double.parseDouble(p));
				sftss.add(nested);
			}
		}
		catch (FileNotFoundException e) {e.printStackTrace();} 
		catch (IOException e) {e.printStackTrace();}
	}

	public void generateARFF(String outfile, String nonSocialFile){
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outfile));
			BufferedWriter nonSocialOut = new BufferedWriter(new FileWriter(nonSocialFile));
			//generateHeader(out,rtf.getFeatureNames());
			//generateHeader(nonSocialOut,rtf.getFeatureNamesNonSocial());
			generateData(out,nonSocialOut);
			out.close();
			nonSocialOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void generateHeader(BufferedWriter out, List<String> names) throws IOException{
		out.write("@RELATION CrimeTweets\n");
		for(String name : names){
			out.write("@ATTRIBUTE " + name);
			if(!name.equals("class"))
				out.write(" NUMERIC\n");
			else
				out.write(" {0,1}\n");
		}
		out.write("\n");
	}
	
	private void generateData(Status s, boolean label, BufferedWriter out) throws IOException{
		double[] feats = rtf.getAllFeatures(s);
		// temporal features here
		
		if(label)
			rtc.addTweet(s);
		for(double d : feats){
			out.write(Double.toString(d));
			out.write(",");
		}
		out.write(Integer.toString(rtc.numNearbyGPS(s)) + ",");
		out.write(Double.toString(rtc.similarity(s)) + ",");
		if (label)
			out.write("1\n");
		else
			out.write("0\n");
	}
	
	private void generateDataNonSocial(Status s, boolean label, BufferedWriter out) throws IOException{
		double[] feats = rtf.getFeaturesNonSocial(s.getText(), s.getUser().getId()); 
		for(double d : feats){
			out.write(Double.toString(d));
			out.write(",");
		}
		if (label)
			out.write("1\n");
		else
			out.write("0\n");
	}
		
	private void generateData(BufferedWriter out, BufferedWriter nonSocialOut) throws IOException{
		out.write("@DATA\n");
		nonSocialOut.write("@DATA\n");
		Twitter twitter = new TwitterFactory().getInstance();
		Status s = null;
		for (int i = 700; i < ids.size(); i++)
		{
			try 
			{
//				System.out.println(twitter.getRateLimitStatus().getSecondsUntilReset());
//				System.out.println(twitter.getRateLimitStatus().getRemainingHits());
				s = twitter.showStatus(ids.get(i));
				writeTextAndData(s, labels.get(i), rtf.getSocialFeatures(s));
				generateData(s,labels.get(i),out);
				generateDataNonSocial(s, labels.get(i), nonSocialOut);	
			}
			catch (TwitterException e) 
			{
				e.printStackTrace();
				if(e.exceededRateLimitation())
				{
					System.out.println("sleeping for: " + Integer.toString(e.getRateLimitStatus().getSecondsUntilReset()));
					try {Thread.sleep(e.getRateLimitStatus().getSecondsUntilReset()*1000);
					} catch (InterruptedException e1) {	e1.printStackTrace();}
				}
			} 
		}
	}
	private void writeTextAndData(Status status,Boolean isCrime,double[] sfts){
		try {
			BufferedWriter labelout = new BufferedWriter(new FileWriter("900data.txt",true));
			if(isCrime)
				labelout.write("1:");
			else
				labelout.write("0:");
			labelout.write(Long.toString(status.getId()) + ":");
			labelout.write(Long.toString(status.getUser().getId()));
			labelout.write(":" + status.getText().replace("\n", "") + "\n");
			for(int i = 0; i < sfts.length; i++)
				labelout.write(Double.toString(sfts[i]) + ",");
			labelout.write(Double.toString(rtc.numNearbyGPS(status)) + ",");
			labelout.write(Double.toString(rtc.similarity(status)) + "\n");
			labelout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void socialTextLabelsToArff(String labelfile) throws IOException{
		ARFFOut ao = new ARFFOut(labelfile,"config/features.txt");
		BufferedWriter out = new BufferedWriter(new FileWriter("train.arff"));
		BufferedWriter nonSocialOut = new BufferedWriter(new FileWriter("train_nonsocial.arff"));
		ao.generateHeader(out,ao.rtf.getFeatureNames());
		ao.generateHeader(nonSocialOut,ao.rtf.getFeatureNamesNonSocial());
		out.write("@DATA\n");
		nonSocialOut.write("@DATA\n");
		for(int i = 0; i < ao.ids.size(); i++){
			double[] f = ao.rtf.getFeaturesNonSocial(ao.texts.get(i), ao.uids.get(i));
			int len = ao.sftss.get(i).size();
			for(int j = 0; j < len - 2; j++)
				out.write(Double.toString(ao.sftss.get(i).get(j)) + ",");
			for(double d : f){
				out.write(Double.toString(d) + ",");
				nonSocialOut.write(Double.toString(d) + ",");
			}
			out.write(Double.toString(ao.sftss.get(i).get(len - 2)) + ",");
			out.write(Double.toString(ao.sftss.get(i).get(len - 1)) + ",");
			if (ao.labels.get(i)){
				out.write("1\n");
				nonSocialOut.write("1\n");
			}
			else{
				out.write("0\n");
				nonSocialOut.write("0\n");
			}
		}
		out.close();
		nonSocialOut.close();
	}
	

	/**
	 * @param args
	 * @throws TwitterException 
	 * @throws IOException 
	 * @throws Exception 
	 */
	public static void main(String[] args) throws TwitterException, IOException 
	{
		//ARFFOut ao = new ARFFOut("tweets/labeledtweets.txt","config/features.txt");
		//ao.generateARFF("config/train.arff","config/train_nonsocial.arff");
		socialTextLabelsToArff("labels.txt");
	}
}
//		Twitter twitter = new TwitterFactory().getInstance();		


//		
//		TweetsClassifierTraining tct = new TweetsClassifierTraining();
//		try {
//			tct.loadData("train", "train.arff");//"config/save/train_900_reduced_features.arff");
////			Instances randData = new Instances(tct.trainSet);
////			randData.randomize(new Random(System.currentTimeMillis()));
////			Instances train = randData.trainCV(5,0);
////			Instances test = randData.testCV(5, 0);
//
//			Classifier svm = tct.train("svm",tct.trainSet);
//			Classifier nb = tct.train("nb",tct.trainSet);
//			Classifier dt = tct.train("dt",tct.trainSet);
//			tct.saveClassifier("testnb_2.model", nb);
//			tct.saveClassifier("testsvm_2.model", svm);
//			tct.saveClassifier("testdt_2.model", dt);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		TweetsClassifierTraining tct = new TweetsClassifierTraining();		
//		try {
//			String[] x = {"nb","dt","svm"};
//			tct.loadData("train", "train.arff");
//			tct.crossValidate(x, 5, tct.trainSet);
//			tct.loadData("train", "train_nonsocial.arff");
//			System.out.println("nonsocial");
//			tct.crossValidate(x, 5, tct.trainSet);		
//		} catch (Exception e) {
//			e.printStackTrace();
//		}

			//System.out.println("social");
			//tct.loadData("train", "config/save/900train.arff");
			//tct.attributeSelection();
//			String[] s = {"nb","dt","svm"};
//			tct.deleteAttributes(s, 5);
//			System.out.println("non social");
//			tct.loadData("train", "config/save/900train_nonsocial.arff");
//			tct.deleteAttributes(s, 5);
