import java.io.BufferedWriter;
import java.io.IOException;

import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.URLEntity;
import twitter4j.UserMentionEntity;

public class TweetStatusField {
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
	public static final String LATITUDE = "LATITUDE";
	public static final String LONGITUDE = "LONGITUDE";
	public static final String HASHTAGS = "HASHTAGS";
	public static void writeFile(BufferedWriter out, Status s) {
		try {
		out.write("***\n");
		//Start of the stauts
		out.write(USERID + "\t" + s.getUser().getId()+"\n");
		//Write the ScreenID of the user
		out.write("ID:\t"+s.getId()+"\n");
		//Write the status ID		
		
		out.write(TEXT+"\t"+s.getText()+"\n");
		//Write text
		
		out.write(RETWEETCOUNT+ s.getRetweetCount() +"\n");
		//Write reTweetCount
		out.write(SOURCE + "\t"+s.getSource()+"\n");
		//Write the source of the status
		out.write(LOCATION + "\t" + s.getGeoLocation() + "\n");
		//Write the location of the status
		Place p = s.getPlace();
		if (p == null){
			out.write(PLACE + "\t");
		} else {
			if (p.getBoundingBoxCoordinates()!=null) {

				GeoLocation[][] box = p.getBoundingBoxCoordinates();	
				out.write(PLACE  + "\t");
				for (int i = 0;i < box.length; i++) {
					for (int j = 0;j < box[0].length; j++) {
						out.write(box[i][j].getLatitude() + "\t" +box[i][j].getLongitude() + "\t");
					}	
				}
			}
			if (p.getGeometryCoordinates() != null) {
			
				GeoLocation[][] box = p.getGeometryCoordinates();	
				out.write(PLACE  + "\t");
				for (int i = 0;i < box.length; i++) {
					for (int j = 0;j < box[0].length; j++) {
						out.write(box[i][j].getLatitude() + "\t" +box[i][j].getLongitude() + "\t");
					}	
				}
			}
		} 
		out.write("\n");
		//Write the place of the status			
		out.write(CREATEAT + "\t"+s.getCreatedAt().toString()+"\n");
		//write the time status is created
				
		
		URLEntity[] urls = s.getURLEntities();
		String urlStr = "";
		if (urls != null) {
			
			for (URLEntity ue : urls) {
				urlStr = urlStr + " " + ue;
			}			
		}
		out.write(URL + "\t" + urlStr + "\n");
		
		
		UserMentionEntity[] umes=s.getUserMentionEntities();
		String entityStr="";
		Long it = null;
		if(umes!=null){
			for(UserMentionEntity ume:umes){
				it=ume.getId();
				entityStr=it.toString()+"\t"+entityStr;
			}
		}
		out.write("MentionedEntities: "+entityStr+"\n");
		//write mentioned users in the status
		
		HashtagEntity[] htes=s.getHashtagEntities();
		String hts="";
		if(htes!=null){
			for(HashtagEntity hte:htes)
				hts=hts+" "+hte.getText();
		}
		out.write("Hashtags: "+hts+"\n");
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
}
