//package crawler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import twitter4j.FilterQuery;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;



/**************************************************************************
 * StreamCrawler.java is the file from where the code flow begins
 **************************************************************************/
public class StreamCrawler implements Runnable {
	private TwitterStream twitterStream;
	private String track[];
	private double[][] geo;
	private boolean useGeo;

	public StreamCrawler(String[] track, double[][] geo, boolean useGeo)
			throws IOException {
		BufferedReader br = new BufferedReader(new FileReader("config/crawler.conf"));
		ArrayList<String> configs = new ArrayList();
		String tmp;
		while((tmp = br.readLine())!=null){
			configs.add(StringUtils.split(tmp, "=")[1]);
		}
		
		//Set up the listener for tweets and add it to the twitter stream
		StatusListener listener = new Listener(configs.get(0), configs.get(1));
		this.track = track;
		this.geo = geo;
		this.useGeo = useGeo;
		twitterStream = new TwitterStreamFactory().getInstance();
		twitterStream.addListener(listener);
	}


	/**************************************************************************
	 * 
	 * This function sets up the filter query for TwitterStream 
	 * with keywords and geo coordinates
	 * 
	 **************************************************************************/
	public void run() {
		
		FilterQuery query = new FilterQuery();
		// if you want to track geo, add this line/
		if (useGeo)
			query.locations(geo);
		// if you want to track keywords, add this line.
		else
			query.track(track);
		
		//Start filtering twitter real time stream according to the filters
		twitterStream.filter(query);
	}

	
	/**************************************************************************
	 * 
	 * This is the entry point of the code
	 * Takes in arguments
	 * 	- keywords file
	 * 	- no arguments
	 * 
	 **************************************************************************/
	
	public static void main(String[] args) throws IOException {

		String[] track;

		// bounding boxes are specified as a comma separate list of
		// longitude/latitude pairs, with the first pair denoting the southwest
		// corner of the box.
		// For example locations=-122.75,36.8,-121.75,37.8 would track tweets
		// from the San Francisco area.
		// The below latitude longitude tracks chicago 
		double chilat = 41.878114;
		double chilong = -87.629798;
		double[][] geo = { { chilong - .5, chilat - .5 },
				{ chilong + .5, chilat + .5 } };
		
		//Default operation - no keywords specified, just track bounding box of Chicago
		if (args.length==0) {
			//Create object of StreamCrawler with specified geo co-ordinates
			StreamCrawler sc = new StreamCrawler(null, geo, true);
			//Calling run method to start filtering tweets
			sc.run();
		}
		//Keywords mode - filter tweets by keywords
		else {
			//Read keywords from file supplied as argument and store into arraylist
			FileReader file = new FileReader(args[0]);
			BufferedReader br = new BufferedReader(file);
			ArrayList<String> kws = new ArrayList<String>();
			String kw;
			int count = 0;
			while ((kw = br.readLine()) != null) {
				if (kw.length() > 0) {
					kws.add(kw);
					count++;
				}
			}
			br.close();
			track = new String[count];
			int index = 0;
			for (String word : kws) {
				track[index] = word;
				index++;
			}
			//Create object of StreamCrawler with keywords array
			StreamCrawler sc = new StreamCrawler(track, geo, false);
			//Calling run method to start filtering tweets
			sc.run();
		}
	}

}
