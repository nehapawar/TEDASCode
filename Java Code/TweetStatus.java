

import java.util.Date;

public class TweetStatus {
	public static final String TEXT = "TEXT";
	public static final String ID = "ID";
	public static final String FAVORITED = "FAVORITED";
	public static final String RETWEETCOUNT = "RETWEET_COUNT";
	public static final String CREATEAT = "TIME";
	public static final String PLACE = "PLACE";
	public static final String LOCATION = "LOCATION";
	public static final String USERID = "USERID";
	public static final String SOURCE = "SOURCE";
	public static final String URL = "URLS";
	public static final String LONGITUDE = "long";
	public static final String LATITUDE = "la";
	
	private String text;
	private String userID;
	private Date time;
	private String tags;
	private String locations;
	private double  longitude;
	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	private double  latitude;
	public TweetStatus(String t, String id) {
		this.text = t;
		this.userID = id;
		this.latitude = Double.MIN_VALUE;
		this.longitude = Double.MIN_VALUE;
	}
	
	public String getText() {
		return text;
	}
	
	public String getUserID() {
		return userID;
	}
	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getLocations() {
		return locations;
	}

	public void setLocations(String locations) {
		this.locations = locations;
	}

	
	
}
