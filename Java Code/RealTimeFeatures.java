import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;

/**
 * Provides functionality for generating feature vectors for real time classification
 * @author ravi
 *
 *AKA The leftovers from gutting indexlines
 */
public class RealTimeFeatures {

	private ArrayList<String> featureNames = new ArrayList<String>();
	public ArrayList<String> socialFeatureNames = new ArrayList<String>();

	private ArrayList<ArrayList<String>> featureSets = new ArrayList<ArrayList<String>>();
	public Hashtable<String, String> policeIDs = new Hashtable<String, String>();
	
	private static Set<String> numberWords = new HashSet<String>();
	private static String[] numberWordsArray = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", 
		"ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
		"twenty", "thirty", "fourty", "fifty", "sixty", "seventy", "eighty", "ninety", 
		"hundred", "thousand", "million", "billion"};
	private static String pdfile = "config/policeDept.txt";
	
	//Load the feature file, add the numberwords to the set
	//set the policeIDs hashtable
	public RealTimeFeatures(String featureFileName){
		try {
			loadFeatureFile(featureFileName);
			policeIDs = storePolice(pdfile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		addWordsToSet();
		
		//status features
		socialFeatureNames.add("contributors");
		socialFeatureNames.add("retweet_count");
		socialFeatureNames.add("mentions");
		socialFeatureNames.add("hashtags");
		socialFeatureNames.add("urls");
		socialFeatureNames.add("favorited");
		socialFeatureNames.add("media_entities");

		//usr features
		socialFeatureNames.add("user_fav_count");
		socialFeatureNames.add("follower_count");
		socialFeatureNames.add("friend_count");
		socialFeatureNames.add("public_list_count");
		socialFeatureNames.add("status_count");
		socialFeatureNames.add("verified");
	}
	
	public boolean isPolice(long uid) {
		return policeIDs.contains(uid);
	}
	
	// store police departments sites in a hashtable
	private static Hashtable<String, String> storePolice(String pdFile)
			throws IOException {
		Hashtable<String, String> pdsites = new Hashtable<String, String>();
		BufferedReader pd = new BufferedReader(new FileReader(pdFile));
		String polInfo;
		while ((polInfo = pd.readLine()) != null) {
			String[] info = polInfo.split("\t");
			pdsites.put(info[1].trim(), info[0]);
		}
		return pdsites;
	}
	
	private void addWordsToSet() {
		int i;
		for(i = 0; i < numberWordsArray.length; i++) {
			numberWords.add(numberWordsArray[i]);
		}
	}
	
	public int extractNumbers(String line) {
		int frequency = 0;
		char last = line.charAt(line.length()-1); 
		char slast = line.charAt(line.length()-2);
		char tlast = line.charAt(line.length()-3);
		char flast = line.charAt(line.length()-4);
		if ((slast == ' ' || slast == '\t') && (flast == ' ' || flast == '\t')) {
			if (last == '0' || last == '1' || last == '2') {
				frequency--;
			}
			if (tlast == '0' || tlast == '1' || tlast == '2') {
				frequency--;
			}
		}
		line = line.replaceAll("http://[^\\s]*", " ");
		Pattern numPattern = Pattern.compile("[0-9]+");
		Matcher numMatcher = numPattern.matcher(line);
		//int number;
		while (numMatcher.find()) {
			//System.out.println(numMatcher.group());
			frequency++; 
		}
		line = line.toLowerCase();
		line = line.replaceAll("[^a-z\\s]", " ");
		StringTokenizer st = new StringTokenizer(line);
		String num = "";
		boolean flag = false;
		while (st.hasMoreTokens()) {
			num = st.nextToken();
			while ((num.equals("and") || numberWords.contains(num)) && st.hasMoreTokens()) {
				if (!(num.equals("and"))) { flag = true; }
				num = st.nextToken();
			}
			if (flag) {frequency++; flag = false;}
		}

		return frequency;
	}
	
	private void loadFeatureFile(String fn) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(fn));
		String line;
		// reading in features into featureNames and featureSets
		while ((line = br.readLine()) != null) {
			if (line.length() > 0) {
				// System.out.println(line);
				String[] splits = StringUtils.split(line, '|');
				featureNames.add(splits[0]);
				String[] set = StringUtils.split(splits[1], ',');
				ArrayList<String> list = new ArrayList<String>();
				for (int i = 0; i < set.length; i++) {
					list.add(set[i].toLowerCase());
				}
				featureSets.add(list);
			}
		}
		br.close();
	}
	
	/*nonsocial features except police id*/
	public double[] convertTweetToFeatures(String line) {
		double frequency[] = new double[featureSets.size()];
		int j, k;
		line = line.toLowerCase();
		for (j = 0; j < featureSets.size(); j++) {
			for (k = 0; k < featureSets.get(j).size(); k++) {
				frequency[j] += StringUtils.countMatches(line, (featureSets
						.get(j)).get(k));
			}
		}

		// for(j = 0; j < frequency.length; j++)
		// System.out.print(frequency[j] + " ");
		return frequency;
	}
	
	/*every feature for nonsocial*/
	public double[] getFeaturesNonSocial(String tweetContent, long uid) {
		String userID = uid + "";
		double[] freq = new double[featureNames.size() + 1];
		double[] temp = convertTweetToFeatures(tweetContent);
		for (int i = 0; i < temp.length; i++)
			freq[i] = temp[i];
		freq[freq.length - 1] = (policeIDs.contains(userID)) ? 1 : 0;
		return freq;
	}
	
	/*Everthing for nonsocial including police plus tag*/
	public double[] getFeaturesNonSocialPlusTag(String tweetContent, long uid){
		double[] f = getFeaturesNonSocial(tweetContent,uid);
		double[] ret = new double[f.length + 1];
		for(int i = 0; i < f.length; i++)
			ret[i] = f[i];
		ret[ret.length-1] = -1;
		return ret;
	}
	
	public double[] getSocialFeatures(Status s){
		User u = s.getUser();
		double[] freq = new double[socialFeatureNames.size()];
		freq[0] = s.getContributors() == null? 0 : s.getContributors().length;
		freq[1] = s.getRetweetCount();
		freq[2] = s.getUserMentionEntities()==null? 0 : s.getUserMentionEntities().length;
		freq[3] = s.getHashtagEntities()==null? 0 :s.getHashtagEntities().length;
		freq[4] = s.getURLEntities()==null? 0 : s.getURLEntities().length;
		freq[5] = s.isFavorited()? 1 : 0;
		freq[6] = s.getMediaEntities()==null? 0 : s.getMediaEntities().length;
		freq[7] = u.getFavouritesCount();
		freq[8] = u.getFollowersCount();
		freq[9] = u.getFriendsCount();
		freq[10] = u.getListedCount();
		freq[11] = u.getStatusesCount();
		freq[12] = u.isVerified()? 1:0;
		return freq;
	}
	
	
	/*
	 * Does not include recent gps/text features!
	 */
	public double[] getAllFeatures(Status s){
		//2 is for police and class
		double[] freq = new double[socialFeatureNames.size() + featureNames.size() + 1];
		double[] sfreq = getSocialFeatures(s);
		double[] tfreq = convertTweetToFeatures(s.getText());
		for(int i = 0; i < sfreq.length; i++)
			freq[i] = sfreq[i];
		for(int i = sfreq.length; i < freq.length-2; i++)
			freq[i] = tfreq[i - sfreq.length];
		freq[freq.length - 1] = (policeIDs.contains(s.getUser().getId())) ? 1 : 0; //is_police
		return freq;
	}
	
	public double[] getAllFeaturesPlusTag(Status s){
		double[] f = getAllFeatures(s);
		double[] ret = new double[f.length + 1];
		for(int i = 0; i < f.length; i++)
			ret[i] = f[i];
		ret[ret.length-1] = -1;
		return ret;
	}
	
	public ArrayList<String> getFeatureNames() {
		ArrayList<String> ret = new ArrayList<String>();
		ArrayList<String> sfns = new ArrayList<String>(socialFeatureNames);
		ArrayList<String> fns = new ArrayList<String>(featureNames);
		ret.addAll(sfns);
		ret.addAll(fns);
		ret.add("from_police_dept");
		ret.add("recent_gps");
		ret.add("recent_text");
		ret.add("class");
		return ret;
	}
	
	public ArrayList<String> getFeatureNamesNonSocial(){
		ArrayList<String> fns = new ArrayList<String>(featureNames);
		fns.add("from_police_dept");
		fns.add("class");
		return fns;
	}
}