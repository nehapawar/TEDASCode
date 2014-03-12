import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.lang.Math;

import twitter4j.GeoLocation;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.URLEntity;

public class RecentTweetCache {
	private LinkedList<Status> cache = new LinkedList<Status>();
	private static final int GPS_SIZE = 100;
	private static final int CACHE_SIZE = 100;
	private int cacheSize = CACHE_SIZE;
	public static final double CLOSE_DISTANCE = 30.0; //miles
	private HashMap<String, Integer> wordHash = new HashMap<String, Integer>();
	private int count = 0;
	
	private String corpusFile;
	private HashMap<String,Integer> corpusHash = new HashMap<String,Integer>();
	private int corpusSize;
	
	private String stopWordsFile;
	public Set<String> stopWords = new HashSet<String>();
	
	//private LinkedList<Status>[] lats = (LinkedList<Status>[]) new LinkedList[180];
	//private LinkedList<Status>[] lons = (LinkedList<Status>[]) new LinkedList[360];
	private LinkedList<double[]> gpss = new LinkedList<double[]>();

	/**
	 * Fills a hash table for the corpus, fills set for stop words
	 * @param corpusFile
	 * @param stopWordsFile
	 */
	public RecentTweetCache(String corpusFile, String stopWordsFile) {
		super();
		this.corpusFile = corpusFile;
		try {
			//fill corpus hash
			BufferedReader br = new BufferedReader(new FileReader(corpusFile));
			String line = null;
			line = br.readLine();
			line = line.replace("\n", "");
			corpusSize = Integer.parseInt(line);
			while ( (line = br.readLine()) != null){
				String[] wordCouple = line.split("\\s+");
				corpusHash.put(wordCouple[0], Integer.parseInt(wordCouple[1]));	
			}
			
			//fill stop words file
			this.stopWordsFile = stopWordsFile;
			br = new BufferedReader(new FileReader(stopWordsFile));
			while ( (line = br.readLine()) != null)
				stopWords.add(line);
			
		} catch (FileNotFoundException e) {
			System.out.println("Invalid Corpus File");
		} catch (IOException e) {
			System.out.println("Problem with line in corpus file");
		}
	}

	
	public double tfidf(String word, int freq)
	{
		int corpusFrq = 1; //adding 1 prevents divide by zero
		if(corpusHash.containsKey(word))
			corpusFrq += corpusHash.get(word);
		double idf = Math.log((float) corpusSize / (float) corpusFrq);
		return freq*idf;
	}
	
	/*
	 * Return the cosine similarity of s and the cache
	 */
	public double similarity(Status s)
	{
		//get word freqs from status
		HashMap<String,Integer> H = new HashMap<String,Integer>();
		String text = s.getText();
		String[] tweetWords = text.split("\\s+");
		for (String w : tweetWords)
		{
			if(!stopWords.contains(w))
			{
				if(H.containsKey(w))
					H.put(w, H.get(w) + 1);
				else
					H.put(w, 1);
			}
		}
		
		//get attribute vector for cache and status
		// (which is tfidf of all appearing words)
		Set<String> words = wordHash.keySet();
		List<Double> cacheAttributes = new ArrayList<Double>();
		List<Double> statusAttributes = new ArrayList<Double>();
		for (String word : words){
			cacheAttributes.add(tfidf(word,wordHash.get(word)));
			if (H.containsKey(word))
				statusAttributes.add(tfidf(word,H.get(word)));
			else
				statusAttributes.add(0.0);
		}
		
		double dotProduct = 0.0;
		double cacheMag = 0.0;
		double statusMag = 0.0;
		//take dot product, magnitudes
		for(int i = 0; i < cacheAttributes.size();i++)
		{
			dotProduct += cacheAttributes.get(i) * statusAttributes.get(i);
			cacheMag += Math.pow(cacheAttributes.get(i),2.0);
			statusMag += Math.pow(statusAttributes.get(i), 2.0);
		}
		statusMag = Math.sqrt(statusMag);
		cacheMag = Math.sqrt(cacheMag);
		cacheMag = cacheMag <= 0.0 ? 1 : cacheMag;
		statusMag = statusMag <= 0.0 ? 1 : statusMag;

		return dotProduct / (cacheMag * statusMag);
	}
	
	public int numNearbyGPS(Status s)
	{
		double[] gps = getTweetGPS(s);
		if(gps == null) return 0;
		int count = 0;
		for(double[] g : gpss){
			double d = distance(g[0],g[1],gps[0],gps[1],'M');
			System.out.println("DIST: " + Double.toString(d));
			if(d <= CLOSE_DISTANCE)
				count++;
		}
		return count;
	}
	private double distance(double lat1, double lon1, double lat2, double lon2, char unit) {
		  double theta = lon1 - lon2;
		  double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		  dist = Math.acos(dist);
		  dist = rad2deg(dist);
		  dist = dist * 60 * 1.1515;
		  if (unit == 'K') {
		    dist = dist * 1.609344;
		  } else if (unit == 'N') {
		  	dist = dist * 0.8684;
		    }
		  return (dist);
	}
	
	private double deg2rad(double deg) {
	  return (deg * Math.PI / 180.0);
	}
	private double rad2deg(double rad) {
	  return (rad * 180.0 / Math.PI);
	}

	
	public void addTweet(Status s)
	{
		double[] gps = getTweetGPS(s);
		if (gps!= null){
			gpss.add(gps);
			if(gpss.size() >= GPS_SIZE)
				gpss.removeFirst();
		}
		if (count >= cacheSize){
			addHelper(s);
			removeOldest();
		}
		else{
			addHelper(s);
			count++;
		}	
	}
	
	/*
	 * add words in a status to the cache hash
	 */
	private void addHelper(Status s)
	{
		cache.add(s);
		String text = s.getText();
		String[] words = text.split("[\\p{P} \\t\\n\\r]");
		for (String word : words){
			String pretty = formatWord(word);
			if(pretty != null)
				hashAdd(pretty,wordHash);
		}
	}
	
	/*
	 * get rid of hash tags. 
	 * delete mentions (@s)
	 * convert to lower case
	 */
	private String formatWord(String word)
	{
	
		word = word.toLowerCase();
		if(stopWords.contains(word) || word.startsWith("@") || word.equals(""))
			return null;
		if(word.startsWith("#"))
			return word.substring(1);
		return word;
	}
	
	/*
	 * increment count of word in wordHash
	 * add it if it doesn't exist
	 */
	private void hashAdd(String word, HashMap<String,Integer> H){
		if (H.containsKey(word))
			H.put(word, H.get(word) + 1);
		else
			H.put(word, 1);
	}
	
	/* remove oldest status from linked list,
	 * decrement the count of all words in this status,
	 * remove this status from gps arrays,
	 * delete the word from the hash map if it's the only mention
	 */
	private void removeOldest()
	{
		Status s = cache.remove();

		String text = s.getText();
		String[] words = text.split("[\\p{P} \\t\\n\\r]");
		for (String a : words)
		{
			String pretty = formatWord(a);
			if(pretty != null)
				hashRemove(pretty);
		}
	}

	/* remove a instance of a word from table
	 * assumes word in table
	 * If only instance of word, remove word from table
	 */
	private void hashRemove(String word){
		int wordCount = wordHash.get(word);
		if (wordCount > 1)
			wordHash.put(word, wordCount-1);
		else
			wordHash.remove(word);
	}
	
	/*
	 * Return gps coordinates rounded to nearest whole number
	 */
	private double[] getTweetGPS(Status s)
	{
		double lat=0,lon=0;
		GeoLocation geo = s.getGeoLocation();
		Place p;
		if(geo != null){
			lat = geo.getLatitude();
			lon = geo.getLatitude();
		}
		else if( (p=s.getPlace()) != null)
		{
			if (p.getBoundingBoxCoordinates()!=null) 
			{
                GeoLocation[][] box = p.getBoundingBoxCoordinates();    
                for (int i = 0;i < box.length; i++) {
                        for (int j = 0;j < box[0].length; j++) {
                                lat += box[i][j].getLatitude();
                                lon += box[i][j].getLongitude();
                        }       
                }
                lat = lat/4;
                lon = lon/4;
                
			}
			else if (p.getGeometryCoordinates() != null) {
	                GeoLocation[][] box = p.getGeometryCoordinates();       
	                for (int i = 0;i < box.length; i++) {
	                        for (int j = 0;j < box[0].length; j++) {
	                        	lat += box[i][j].getLatitude();
                                lon += box[i][j].getLongitude();
                            }       
	                }
	                lat = lat / 4;
	                lon = lon /4;
	        }
		}
		else return null;
		double[] ret = {lat,lon};
		return ret;

	}
	
	/*
	 * adds a status to the corpus hash
	 */
	public boolean corpusAdd(Status s){
		String text = s.getText();

		String[] words = text.split("[\\p{P} \\t\\n\\r]");
		for (String word : words){
			if(word.equalsIgnoreCase("RT"))
				return false;
		}
		for (String word : words){
			String pretty = formatWord(word);
			if(pretty != null){
				hashAdd(pretty,corpusHash);
				//System.out.println(pretty);
			}
		}
		corpusSize++;
		return true;
	}
	/*
	 * Write the corpus hash to disk
	 */
	public void corpusToDisk(){
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(corpusFile));
			Set<String> keys = corpusHash.keySet();
			out.write(Integer.toString(corpusSize) + "\n");
			for(String key : keys)
				out.write(key + " " + Integer.toString(corpusHash.get(key)) + "\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
