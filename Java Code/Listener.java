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
import twitter4j.User;

public class Listener implements StatusListener {
	private static final String DATE_FORMAT_NOW = "yyyy-MM-dd";
	String statusDir;
	String userDir;
	String currentFile = "";
	BufferedWriter out = null;
	RealTimeFeatures rtf;
	//RealTimeIndex index;
	DBWriter dbw;
	TweetNER ner;
	
	private int corpcount = 0;
	//HashMap<String,Integer> H = new HashMap<String,Integer>();
	RecentTweetCache rtc;
	
	// Constructor
	public Listener(String statusDir, String userDir) {
		this.statusDir = statusDir;
		this.userDir = userDir;
		this.rtc = new RecentTweetCache("config/corpus.txt","config/stop_words.txt");
		
		this.corpcount = 0;
		rtf = new RealTimeFeatures("config/features.txt");
		//index = new RealTimeIndex("tempindex");
		dbw = new DBWriter();
		ner = new TweetNER("classifiers/all.3class.distsim.crf.ser.gz");
	}

	// Get current time, it will be used as fileName to store tweets in that
	// day.
	private static String getTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	// Check whether is is an English language User
	public boolean validation_Check(User user) {

		int en = user.getLang().compareTo("en");
		return en == 0 && !user.isProtected();

	}
	
	private void writeTextAndData(Status status,Boolean isCrime,double[] sfts){
		try {
			File f = new File("text_social_data/" + getTime());
			f.setReadable(true, false);
			f.setWritable(true, false);
			BufferedWriter labelout = new BufferedWriter(new FileWriter(f, true));
			labelout.write(Long.toString(status.getId()) + ":");
			if(isCrime)
				labelout.write("1");
			else
				labelout.write("0");
			labelout.write(":" + status.getText().replace("\n", "") + "\n");
			for(int i = 0; i < sfts.length; i++){
				labelout.write(Double.toString(sfts[i]) + ",");
			}
			labelout.write(Double.toString(rtc.numNearbyGPS(status)) + ",");
			labelout.write(Double.toString(rtc.similarity(status)) + "\n");
			labelout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	

	// Handle a status, it will be called when a status comes.
	public void onStatus(Status status) {
		
//		this.writeToFile(status);
//		this.corpcount++;

		double sim = rtc.similarity(status);
		double numgps = rtc.numNearbyGPS(status);
		CrimeDetection cd = new CrimeDetection();
		boolean social = cd.classify(status,rtf,"svm",numgps,sim);
		double confidence = cd.getRecentConfidence();
		writeTextAndData(status,social,rtf.getSocialFeatures(status));
		if(social)
		{
			rtc.addTweet(status);
			System.out.println("crime");
			String regaddr = ner.getAddress(status.getText());
			String annotated = null;
			try {
				annotated = ner.annotate(status.getText());
			} catch (IOException e) {
				e.printStackTrace();
			}
			List<String> locs;
			String nerloc = null;
			if( (locs = TweetNER.getLocations(annotated)).size() > 0)
			{
				nerloc = locs.get(0);
				nerloc = nerloc.toLowerCase();
			}
			
			if(regaddr != null)
				regaddr=regaddr.toLowerCase();
			
			String category = ner.categorize(status.getText());
			dbw.addTweet(status, nerloc,regaddr,category,confidence);
		//dbw.addTweet(status, null, null, null, 0);
			dbw.addUser(status.getUser());
		}
		//boolean nonsocial = cd.classify(status, rtf, "nb_ns");
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

	@Override
	public void onScrubGeo(long arg0, long arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStallWarning(StallWarning arg0) {
		// TODO Auto-generated method stub
		
	}
};
