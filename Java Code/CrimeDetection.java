import twitter4j.Status;

public class CrimeDetection {

	/******************************************************************************
	 * 
	 * real-time classifying for social classifier
	 * Status s - the tweet to be classified
	 * RealTimeFeatures rtf - features of the tweet
	 * String model - model of the classifier
	 * double recgps - Similarity of tweet with gps cache
	 * double rectext - similarity of tweet with tweets cache
	 * 
	 ******************************************************************************/
	public boolean classify(Status s, RealTimeFeatures rtf, String model, double recgps, double rectext) {
		
		//Get tweet content
		String tweetContent = s.getText();
		tweetContent = tweetContent.toLowerCase();
		
		//Initialize classifier model
		TweetsClassifier tc = new TweetsClassifier();
		tc.init(model);
		
		//Populate x array with features of tweet
		double[] tweetData = null;
		double[] x = null;
		boolean result;
		tweetData = rtf.getAllFeatures(s);
		x = new double[tweetData.length + 3];
		x[tweetData.length-3] = recgps;
		x[tweetData.length-2] = rectext;
		x[tweetData.length-1] = -1;
		for(int i = 0; i < tweetData.length; i++)
			x[i] = tweetData[i];
		
		//Classify the tweet using features and feature names
		try {
			result = tc.isCrime(x, rtf.getFeatureNames());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}		
	}
}
