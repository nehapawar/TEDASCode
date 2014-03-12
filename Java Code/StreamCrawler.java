//package crawler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import twitter4j.FilterQuery;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.Configuration;

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
		//StatusListener listener = new Listener("location/", "user/");
		StatusListener listener = new Listener(configs.get(0), configs.get(1));
		this.track = track;
		this.geo = geo;
		this.useGeo = useGeo;
		twitterStream = new TwitterStreamFactory().getInstance();
		twitterStream.addListener(listener);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		FilterQuery query = new FilterQuery();
		// if you want to track geo, add this line/
		if (useGeo)
			query.locations(geo);
		// if you want to track keywords, add this line.
		else
			query.track(track);
		//query.setIncludeEntities(true);
		twitterStream.filter(query);

		// if you do not to focus on geo or keywords
		// use twitterStream.sample(); instead of twitterStream.filter(query);

	}

	public static void main(String[] args) throws IOException {

		// for jar packing purposes, argument "chicago" tracks chicago
		// otherwise use keywords file
		if (args.length == 0) {
			//System.out.println("Please enter filename or chicago");
			
		}
		String[] track;

		// bounding boxes are specified as a comma separate list of
		// longitude/latitude pairs, with the first pair denoting the southwest
		// corner of the box.
		// For example locations=-122.75,36.8,-121.75,37.8 would track tweets
		// from the San Francisco area.
		// this champaign
		double chilat = 41.878114;
		double chilong = -87.629798;
		double[][] geo = { { chilong - .5, chilat - .5 },
				{ chilong + .5, chilat + .5 } };
		// double[][] geo = {{-122.75,36.8 }, {-121.75,37.8}};
		if (args.length==0) {
			StreamCrawler sc = new StreamCrawler(null, geo, true);
			sc.run();
		} else {
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
			StreamCrawler sc = new StreamCrawler(track, geo, false);
			sc.run();
		}
	}

}
