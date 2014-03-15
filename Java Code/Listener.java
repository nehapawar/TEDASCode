//package crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;

public class Listener implements StatusListener {
	private static final String DATE_FORMAT_NOW = "yyyy-MM-dd";
	String statusDir;
	String userDir;
	String currentFile = "";
	BufferedWriter out = null;
	RealTimeFeatures rtf;
	DBWriter dbw;
	TweetNER ner;
	private int corpcount = 0;
	RecentTweetCache rtc;
	
	/**********************************************************************
	 * Listener class which serves as the listener for tweets received on
	 * TwitterStream 
	 *********************************************************************/
	public Listener(String statusDir, String userDir) {
		this.statusDir = statusDir;
		this.userDir = userDir;
		
		//Create a recent tweets cache, starting with a corpus file 
		//containing select starting words and their count, 
		//and skipping stop words
		this.rtc = new RecentTweetCache("config/corpus.txt","config/stop_words.txt");
		
		this.corpcount = 0;
		
		//features.txt contains some common words seen 
		//in crime related tweets and their variants
		rtf = new RealTimeFeatures("config/features.txt");
	
		//Database writer to handle database inserts
		dbw = new DBWriter();
		
		//Stanford NER's classifier to detect location entities
		ner = new TweetNER("classifiers/all.3class.distsim.crf.ser.gz");
	}

	/*******************************************************************
	* Get current time, it will be used as fileName to store tweets 
	* of that particular day.
	********************************************************************/
	private static String getTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	/*********************************************************************
	 * Write the tweet found into a file, along with its classification label
	 * and its social features
	 *********************************************************************/
	private void writeTextAndData(Status status,Boolean isCrime,double[] sfts){
		try {
			//Create file with timestamp for writing tweets of that day
			File f = new File("text_social_data/" + getTime());
			f.setReadable(true, false);
			f.setWritable(true, false);
			BufferedWriter labelout = new BufferedWriter(new FileWriter(f, true));
			
			//Write the tweet id and classification label
			labelout.write(Long.toString(status.getId()) + ":");
			if(isCrime)
				labelout.write("1");
			else
				labelout.write("0");
			
			//Write tweet text
			labelout.write(":" + status.getText().replace("\n", "") + "\n");
			
			//Write tweet social features
			for(int i = 0; i < sfts.length; i++){
				labelout.write(Double.toString(sfts[i]) + ",");
			}
			//Write tweet features such as similarity attribute and nearby tweets attribute
			labelout.write(Double.toString(rtc.numNearbyGPS(status)) + ",");
			labelout.write(Double.toString(rtc.similarity(status)) + "\n");
			labelout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	/************************************************************************
	 * Handle each status as and when it comes
	 ***********************************************************************/
	public void onStatus(Status status) 
	{
		//Calculate similarity of the received tweet with previously correctly
		//classified tweets
		double sim = rtc.similarity(status);
		
		//Calculate the number of tweets from the recent cache which are within 
		// a set range of the tweet
		double numgps = rtc.numNearbyGPS(status);
		
		//Creating an object of the CrimeDetection class which contains
		//the classifier to classify the tweet whether it is related to crime or not
		CrimeDetection cd = new CrimeDetection();
		
		//Classify the tweet using the status, the recent tweets features, and the 
		//similarity and nearby-tweets metrics calculated, with the help of an svm model
		boolean social = cd.classify(status,rtf,"svm",numgps,sim);
		
		//Write the tweet information to a file called text_social/timestamp
		writeTextAndData(status,social,rtf.getSocialFeatures(status));
		
		//If the classifier returns a positive
		if(social)
		{
			//Add the tweet to the recent tweets cache so that 
			//it contributes to the tweets henceforth
			rtc.addTweet(status);
			
			//Extract addresses which match a regular expression
			String regaddr = ner.getAddress(status.getText());
			
			//Annotate the tweet into locations
			String annotated = null;
			try {
				annotated = ner.annotate(status.getText());
			} catch (IOException e) {
				e.printStackTrace();
			}
			List<String> locs;
			
			//Extract the location from the annotation
			String nerloc = null;
			if( (locs = TweetNER.getLocations(annotated)).size() > 0)
			{
				nerloc = locs.get(0);
				nerloc = nerloc.toLowerCase();
			}
			
			if(regaddr != null)
				regaddr=regaddr.toLowerCase();
			
			//Get the category of the tweet fro a set of predefined categories of crime
			String category = ner.categorize(status.getText());
			
			//Add tweet information to the tables "tweets" and "rankings"
			dbw.addTweet(status, nerloc,regaddr,category,0);
			//Add the user information to the table "users"
			dbw.addUser(status.getUser());
		}
	}

	public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
	}
	public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
	}
	public void onException(Exception ex) {
		ex.printStackTrace();
	}
	public void onScrubGeo(int arg0, long arg1) {
	}
	public void onScrubGeo(long arg0, long arg1) {
	}
	public void onStallWarning(StallWarning arg0) {
	}
};
