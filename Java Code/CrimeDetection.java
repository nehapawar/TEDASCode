import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import twitter4j.Status;

/**
 * A Driver for each components
 * 
 * @author Tobias Garrick Lei
 * 
 */

public class CrimeDetection {
	private static final String DATE_FORMAT_NOW = "yyyy-MM-dd";
	private double recentConfidence;

	// Get current time, it will be used as fileName to store tweets in that
	// day.
	private static String getTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	public double getRecentConfidence(){
		return this.recentConfidence;
	}

	// real-time classifying for social classifier
	public boolean classify(Status s, RealTimeFeatures rtf, String model, double recgps, double rectext) {
		String tweetContent = s.getText();
		long uid = s.getUser().getId();
		TweetsClassifier tc = new TweetsClassifier();
		tc.init(model);
		tweetContent = tweetContent.toLowerCase();
		double[] tweetData = null;
		double[] x = null;
		boolean result;
		tweetData = rtf.getAllFeatures(s);//plus tag?
		x = new double[tweetData.length + 3];
		x[tweetData.length-3] = recgps;
		x[tweetData.length-2] = rectext;
		x[tweetData.length-1] = -1;
		//System.out.println(x.length);
		//System.out.println(rtf.getFeatureNames().size());
		for(int i = 0; i < tweetData.length; i++)
			x[i] = tweetData[i];
		try {
			result = tc.isCrime(x, rtf.getFeatureNames());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}		

	}

	
	// real-time classifying
	public boolean classify(Status s, RealTimeFeatures rtf, String model) {
		String tweetContent = s.getText();
		long uid = s.getUser().getId();
		TweetsClassifier tc = new TweetsClassifier();
		try {
			tc.init(model);
			tweetContent = tweetContent.toLowerCase();
			double[] tweetData = null;
			boolean result;
			tweetData = rtf.getFeaturesNonSocialPlusTag(tweetContent, uid);
			result = tc.isCrime(tweetData, rtf.getFeatureNamesNonSocial());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}


	public static void main(String[] args) {
		RealTimeFeatures rtf = new RealTimeFeatures("config/features.txt");
		CrimeDetection cd = new CrimeDetection();
//		boolean b = cd.classify("rt @music_producer_: @youngdonny 16 fire beats http://limelinx.com/files/a427dbc8e9915c10e265166722aa1a06f",0,rtf);
//		if(b)
//			System.out.println("true");
//		else
//			System.out.println("false");
//		cd
//				.classify(
//						"severe thunderstorm warning for mississippi county in ar until 1:30pm cdt. turn to local radio/tv for updates #arwx",
//						181818181);
	}
}
