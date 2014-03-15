import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.lang.Math;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


import javax.sql.*;

import org.apache.commons.lang.StringUtils;


import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.User;

/*******************************************************************************
 * Class to fill database with tweet details
 *
 ******************************************************************************/
public class DBWriter {
	//Database credentials
	private static String user = "root";
	private static String pw = "root";
	private static String dbUrl = "jdbc:mysql://localhost/tedas";
	private static String dbClass = "com.mysql.jdbc.Driver";
		
	//Database queries
	private static String userdbInsert = "replace into users (userid,screenname,accountAge,favorites,followers,friends,verified,statusCount,lat,lon,profilepic) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
	private static String tweetdbInsert = "replace into tweets (userid,text,created,retweetcount, urls, hashtags, lat, lon, category, tweetid,multipleLocations) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
	private static String insertwithrank = "replace into ranking (tweetid,rtcount,urlpresent,numtags,categorynumber,actage,favs,frnds,verified,statuscount,followers,ranking,confidence) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";

	double[] maxes;
	private static double[] weights = {-0.971984667787,0.691618499624,0.0244684351503,-0.996874292288,2.88294586618,-1.7306140492,-1.59469647505,2.49480069013,1.17966313737,2.54918247915,-3.99597258549};
	HashMap<String,double[]> cities;
	HashMap<String,Double> catmap; 
	public HashSet<String> states;
	int tweetCommitCount;
	int userCommitCount;
	Connection con;
	
	DBWriter(){
		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, user, pw);
			con.setAutoCommit(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		maxes = new double[11];
		
		//Obtain maxes from tweets already present 
		//This will be used to create attribute vector of tweet
		updateMaxes();
		
		//Fill HashMap of cities and their gps coordinates from pre defined citypop.txt
		cities = new HashMap<String,double[]>();
		fillCities("config/citypop.txt");
		//Fill HashSet of states from pre defined states.txt
		fillStates("config/states.txt");
		
		//Fill Hashmap of crime categories
		catmap = new HashMap<String,Double>();
		catmap.put("Major Crime", 0.0);
		catmap.put("Natural Disaster",1.0);
		catmap.put("Shooting/Gun Crime", 2.0);
		catmap.put("Traffic", 3.0);
		catmap.put("Non-Traffic Accident", 4.0);
		catmap.put("Theft/robbery/etc", 5.0);
		catmap.put("Drug Crime", 6.0);
		catmap.put("Investigations", 7.0);
		catmap.put("Other Crime Reports",8.0);
	}
	
	/*****************************************************************
	 * Before starting the new streaming, update the maxes array
	 ******************************************************************/
	public void updateMaxes()
	{
		try{
		String maxTweetQuery = "select max(retweetCount) from tweets";
		String maxUserQuery = "select max(accountAge),max(favorites),max(friends),max(statusCount),max(followers) from users";
		Statement maxTweetStmt = con.createStatement();
		Statement maxUserStmt = con.createStatement();
		ResultSet tweetMaxes = maxTweetStmt.executeQuery(maxTweetQuery);
		ResultSet userMaxes = maxUserStmt.executeQuery(maxUserQuery);
		double maxrt=1.0,maxnt=10.0,maxage=1.0,maxfavs=1.0,maxfrnds=1.0,maxsc=1.0,maxfol=1.0;
		if(tweetMaxes.first())
		{
			maxrt = tweetMaxes.getDouble("max(retweetCount)");
			if (maxrt==0) maxrt=1;
		}
		if(userMaxes.first())
		{
			maxfavs = userMaxes.getDouble("max(favorites)");
			if (maxfavs==0) maxfavs = 1;
			maxfrnds = userMaxes.getDouble("max(friends)");
			if (maxfrnds==0) maxfrnds=1;
			maxsc = userMaxes.getDouble("max(statusCount)");
			if (maxsc == 0) maxsc=1;
			maxage = userMaxes.getDouble("max(accountAge)");
			if (maxage==0) maxage=1;
			maxfol = userMaxes.getDouble("max(followers)");
			if (maxfol==0) maxfol=1;
		}
		maxTweetStmt.close();
		maxUserStmt.close();
		maxes[0] = maxrt;
		maxes[1] = 1.0;
		maxes[2] = maxnt;
		maxes[3] = 8.0;
		maxes[4] = maxage;
		maxes[5] = maxfavs;
		maxes[6] = maxfrnds;
		maxes[7] = 1.0;
		maxes[8] = maxsc;
		maxes[9] = maxfol;
		maxes[10] = 1.0;
		}catch (Exception e){
			System.out.println("uupdate max error");
			e.printStackTrace();
		}
	}
	
	/************************************************************************
	 * Method to fill HashSet of states from predefined states.txt file
	 * @param statefile
	 *************************************************************************/
	public void fillStates(String statefile){
		this.states = new HashSet<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(statefile));
			String line;
			br.readLine();
			while((line = br.readLine()) != null)
			{
				line = line.toLowerCase();
				line = line.replaceAll("\\s","");
				states.add(line);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**********************************************************************
	 * Method to fill HashMap of cities and gps coordinates
	 * from pre defined citypop.txt file
	 * @param cityfile
	 *********************************************************************/
	public void fillCities(String cityfile)
	{
		try {
			BufferedReader br = new BufferedReader(new FileReader(cityfile));
			String line;
			br.readLine();
			while((line = br.readLine()) != null)
			{
				double pop = (double)Integer.parseInt(line.substring(73,82).trim());
				double lat = Double.parseDouble(line.substring(143,153).trim());
				double lon = Double.parseDouble(line.substring(153,164).trim());
				String citystring = line.substring(0,73);
				double[] b = new double[3];
				b[0] = lat;
				b[1] = lon;
				b[2] = pop;
				cities.put(citystring, b);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**************************************************************************
	 * Method to return gps coordinates of the location, by matching with 
	 * the cities hashmap and states hashset
	 * 
	 * @param loc
	 * @param filter
	 * @return
	 **************************************************************************/
	public double[] getGPS(String loc, boolean filter) {
		ArrayList<double[]> matches = new ArrayList<double[]>();
		double[] a = {Double.MAX_VALUE,Double.MAX_VALUE,0.0};
		double[] b;
		int count = 0;		
		if(loc != null && !(loc.equals("null")) && !(loc.equals("")))
		{
			loc = loc.toLowerCase();
			if(StringUtils.contains(loc, "urbana"))
			{
					a[0] = 40.109665;
					a[1] = -88.204247;
					return a;
			}
			if(StringUtils.contains(loc,"aleutian"))
			{
					a[0] = 52.096944;
					a[1] = -173.500556;
					return a;
			}
			if(StringUtils.contains(loc,"alaska"))
			{
					a[0] = 64.52;
					a[1] = 152.7;
					return a;
			}
			if(states.contains(loc) && filter){
				return a;
			}
			
			//For every citi that matches, collect the gps coordinates
			for (String key : cities.keySet()){
				if(StringUtils.contains(key.toLowerCase(), loc))
				{
					count++;
					b = cities.get(key);
					matches.add(b);
				}
				else if(StringUtils.contains(key.toLowerCase(),loc.split(",")[0])){
					count++;
					b = cities.get(key);
					matches.add(b);
				}
			}
		}
		
		//Return the city out of all the matches which has highest population
		if (count > 0)
			a[2] += 100.0;
		double maxpop = 0.0;
		for(double[] match : matches){
			if(match[2] > maxpop)
			{
				maxpop = match[2];
				a[0] = match[0];
				a[1] = match[1];
			}
		}
		return a;
	}	
	
	/**********************************************************************
	 * Method to create a single string from URLs in the tweet
	 * @param urls
	 * @return
	 **********************************************************************/
	public String handleURL(URLEntity[] urls){
		String urlStr = "";
		if (urls != null) {			
			for (URLEntity ue : urls) {
				urlStr = urlStr + " " + ue;
			}			
		}
		return urlStr;
	}

	/*************************************************************************
	 * Method to create single string from hashtags in the tweet
	 * @param htes
	 * @return
	 ***********************************************************************/
	public String handleHashTags(HashtagEntity[] htes)
	{
		String hts="";
		if(htes!=null){
			for(HashtagEntity hte:htes)
				hts=hts+" "+hte.getText();
		}
		return hts;
	}

	/*************************************************************************
	 * Method to add tweet into database table "tweets" and "rankings"
	 * @param s - Status
	 * @param NERloc - Location extracted by Stanford NER from the tweet
	 * @param regLoc - Location matched by the regular expression
	 * @param category - Category of tweet decided by Tweet NER, 
	 * 				     according to pre decided categories
	 * @param confidence - Confidence of classification returned by classifier
	 **************************************************************************/
	public void addTweet(Status s, String NERloc, String regLoc, String category, double confidence)
	{
		//Extract URL entities from the tweet
		URLEntity[] urls = s.getURLEntities();
		String urlStr = handleURL(urls);
		
		//Extract hash tags from tweet
		HashtagEntity[] htes=s.getHashtagEntities();
		String hts = handleHashTags(htes);

		int mulloc = 0;
		double lat=Double.MAX_VALUE,lon=Double.MAX_VALUE;
		Place p;
		//If geo location is not null, get tweet coordinates from that
		if(s.getGeoLocation() != null)
		{
			lat = s.getGeoLocation().getLatitude();
			lon = s.getGeoLocation().getLongitude();
		}
		//Else get tweet coordinates from Place
		else if((p = s.getPlace()) != null)
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
		//Else get coordinates from the location extracted by NER
		else if(NERloc != null)
		{
			double[] loc = getGPS(NERloc,true);
			lat = loc[0];
			lon = loc[1];
			mulloc = (int)loc[2];
			if (mulloc > 1)
				mulloc = 1;
			
		}
		//Else get tweet coordinates from the address matched by regular expression
		else if(regLoc != null)
		{
			double [] loc = getGPS(regLoc,true);
			lat = loc[0];
			lon = loc[1];
			mulloc = (int)loc[2];
			if(mulloc > 1)
				mulloc = 1;
		}

		try {
			tweetCommitCount++;
			PreparedStatement pstmt = con.prepareStatement(tweetdbInsert);
			
			pstmt.setLong(1, s.getUser().getId());
			pstmt.setString(2, s.getText());
			pstmt.setLong(3, s.getCreatedAt().getTime()/1000);
			pstmt.setLong(4, s.getRetweetCount());
			pstmt.setString(5, urlStr);
			pstmt.setString(6,hts);
			pstmt.setDouble(7, lat);
			pstmt.setDouble(8, lon);
			pstmt.setString(9, category);
			pstmt.setLong(10, s.getId());
			pstmt.setInt(11, mulloc);

			PreparedStatement rankstmt = con.prepareStatement(insertwithrank);
			double[] features = getFeatureVector(s,category,confidence);
			rankstmt.setLong(1,s.getId());
			double rank = 0.0;
			for(int i = 0; i < features.length; i++)
			{
					
				double val = (features[i] / maxes[i]);
				rankstmt.setDouble(i+2,val);
				rank += val * weights[i];
			}
			rankstmt.setDouble(12,rank);
			rankstmt.setDouble(13,confidence);
			rankstmt.executeUpdate();	
			rankstmt.close();
			pstmt.executeUpdate();
			pstmt.close();
			
			if(tweetCommitCount > 50)
			{
				tweetCommitCount = 0;
				con.commit();
				updateMaxes();
			}
		} catch (SQLException e) {
			System.out.println("tweets database error");
			System.out.println(Long.toString(s.getId()));
			e.printStackTrace();
		}
	}

	public double[] getFeatureVector(Status s, String category, double confidence)
	{
		double rtc = (double) s.getRetweetCount();
		String urls = handleURL(s.getURLEntities());
		double urlval = urls.equals("") ? 0 : 1;	
		String hts = handleHashTags(s.getHashtagEntities());
		double htscount = (double) hts.split(" ").length;
		double cat = catmap.get(category);
		
		User u = s.getUser();
		double accountage = (double) u.getCreatedAt().getTime()/1000.;
		double favs = (double)u.getFavouritesCount();
		double fol = (double)u.getFollowersCount();
		double frds = (double)u.getFriendsCount();
		double verified = u.isVerified() == true? 1.0 : 0.0;
		double statcount = (double) u.getStatusesCount();
		
		double[] a = new double[11];
		a[0] = rtc;
		a[1] = urlval;
		a[2] = htscount;
		a[3] = cat;
		a[4] = accountage;
		a[5] = favs;
		a[6] = frds;
		a[7] = verified;
		a[8] = statcount;
		a[9] = fol;
		a[10] = confidence;
		return a;
	}

	public void addUser(User u){
		int verified = u.isVerified()? 1 : 0;
		double[] loc = getGPS(u.getLocation(),true);
		try {
			userCommitCount++;
			PreparedStatement pstmt = con.prepareStatement(userdbInsert);
			
			pstmt.setLong(1, u.getId());
			pstmt.setString(2, u.getScreenName());
			pstmt.setLong(3, u.getCreatedAt().getTime()/1000);
			pstmt.setLong(4, u.getFavouritesCount());
			pstmt.setLong(5, u.getFollowersCount());
			pstmt.setLong(6, u.getFriendsCount());
			pstmt.setInt(7, verified);
			pstmt.setLong(8,u.getStatusesCount());
			pstmt.setDouble(9, loc[0]);
			pstmt.setDouble(10, loc[1]);
			pstmt.setString(11, u.getProfileImageURL().toString());
			
			pstmt.executeUpdate();
			pstmt.close();

			if(userCommitCount > 10)
			{
				userCommitCount = 0;
				con.commit();
			}
		} catch (SQLException e) {
			System.out.println("user database error");
			e.printStackTrace();
		}
	}


	
}
	
