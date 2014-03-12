import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumberTools;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.Place;
import twitter4j.Status;
import twitter4j.URLEntity;

/**
 * Real time index class
 * @author ravi
 *
 *I think this is all the index functionality we will need
 *for the project pipeline. probably a large portion of is already mostly
 *implemented in either IndexLines or ruis code i think
 *
 *Its basically IndexLines split in half, because a lot of
 *the functionality IndexLines provides has nothing to do with
 *the index. I'm putting those functionalities in another class
 */
public class RealTimeIndex {
	//whatever class variables you need for the index
	RealTimeFeatures rtf;
	Directory indexDir;
	StandardAnalyzer analyzer;
	IndexWriter indWriter;
	IndexSearcher searcher;
	IndexReader statusIndexReader;
	/*Loads the location of the index, or creates
	 * one there if it doesn't exist. set analyzer 
	 * and do all that lucene junk you need to do
	 */
	
	@SuppressWarnings("deprecation")
	public RealTimeIndex(String indexLocation){
		analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		File file = new File(indexLocation);
		if(file.exists()) {
			
		} else {
		file.mkdir();
		}
		try{
		indexDir = FSDirectory.open(file);
		rtf = new RealTimeFeatures("config/features.txt");
		//rtf = new RealTimeFeatures(config/);
		indWriter = new IndexWriter(indexDir, analyzer, true,
				IndexWriter.MaxFieldLength.UNLIMITED);
		//searcher = new IndexSearcher(indexDir);
		//statusIndexReader = IndexReader.open(indexDir);
		
		
	
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	@SuppressWarnings("deprecation")
	/* Given a twitter4j Status object create a document
	 * with the fields you can get from the status and add it to the index. 
	 * 
	 * (text,location [store as latitude and longitude],place,userid,followers,etc. pretty much everything
	 * that you can call on the status obj thats useful)
	 * 
	 * Also store "from_police"
	 * 
	 * Return the STATUS id
	 */
	public Document addTweet(Status s){
		Document doc = new Document();
		doc.add(new Field(TweetStatusField.USERID, Long.toString(s.getUser().getId()), Field.Store.YES, Field.Index.NOT_ANALYZED));
		doc.add(new Field(TweetStatusField.TEXT, s.getText(), Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.YES));
		setLatLong(s,doc);
		doc.add(new Field(TweetStatusField.ID,Long.toString(s.getId()),Field.Store.YES,Field.Index.NOT_ANALYZED));
		if (s.getCreatedAt()!= null) {
			doc.add(new Field(TweetStatusField.CREATEAT,DateTools.dateToString(s.getCreatedAt(), DateTools.Resolution.SECOND),Field.Store.YES, Field.Index.NOT_ANALYZED));
		}
		doc.add(new Field(TweetStatusField.RETWEETCOUNT,Long.toString(s.getRetweetCount()),Field.Store.YES,Field.Index.NOT_ANALYZED));
		
		URLEntity[] urls = s.getURLEntities();
		String urlStr = "";
		if (urls != null) {
			
			for (URLEntity ue : urls) {
				urlStr = urlStr + " " + ue;
			}			
		}
		doc.add(new Field(TweetStatusField.URL,urlStr,Field.Store.YES,Field.Index.NOT_ANALYZED));
		
		HashtagEntity[] htes=s.getHashtagEntities();
		String hts="";
		if(htes!=null){
			for(HashtagEntity hte:htes)
				hts=hts+" "+hte.getText();
		}
		doc.add(new Field(TweetStatusField.HASHTAGS,hts,Field.Store.YES,Field.Index.ANALYZED));
		try {
			indWriter.addDocument(doc);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//return Long.valueOf(doc.getField(TweetStatusField.ID).stringValue());
		return doc;
//		
//		if (s.getLatitude() != Double.MIN_VALUE) {
//			double latitude = s.getLatitude();
//			double longitude = s.getLongitude();
//			long la = (long) (latitude * 100);
//			long lo = (long) (longitude * 100);
//			doc.add(new Field(TweetStatus.LATITUDE, NumberTools.longToString(la),Field.Store.YES,Field.Index.NOT_ANALYZED));
//			doc.add(new Field(TweetStatus.LONGITUDE, NumberTools.longToString(lo),Field.Store.YES,Field.Index.NOT_ANALYZED));
//		}
	}
	
	public void setLatLong(Status s, Document doc)
	{
		GeoLocation geoloc;
		Place p;
		if ((geoloc = s.getGeoLocation()) != null) {
			//doc.add(new Field(TweetStatus.LOCATION, s.getLocations(), Field.Store.YES,Field.Index.ANALYZED));
			double lat = geoloc.getLatitude();
			double lng = geoloc.getLongitude();
			int la = (int) lat*100;
			int lo = (int) lng*100;
			doc.add(new Field(TweetStatusField.LATITUDE,Integer.toString(la),Field.Store.YES,Field.Index.NOT_ANALYZED));
			doc.add(new Field(TweetStatusField.LONGITUDE,Integer.toString(lo),Field.Store.YES,Field.Index.NOT_ANALYZED));
		}
		else if((p = s.getPlace()) != null)
		{
			if (p.getBoundingBoxCoordinates()!=null) 
			{
				double latsum = 0,longsum = 0;
                GeoLocation[][] box = p.getBoundingBoxCoordinates();    
                for (int i = 0;i < box.length; i++) {
                        for (int j = 0;j < box[0].length; j++) {
                                latsum += box[i][j].getLatitude();
                                longsum += box[i][j].getLongitude();
                        }       
                }
                int la = (int) ((latsum/4) * 100);
                int lo = (int) ((longsum/4) * 100);
                doc.add(new Field(TweetStatusField.LATITUDE,Integer.toString(la),Field.Store.YES,Field.Index.NOT_ANALYZED));
                doc.add(new Field(TweetStatusField.LONGITUDE,Integer.toString(lo),Field.Store.YES,Field.Index.NOT_ANALYZED));

			}
			else if (p.getGeometryCoordinates() != null) {
	        		double latsum = 0,longsum = 0;
	                GeoLocation[][] box = p.getGeometryCoordinates();       
	                for (int i = 0;i < box.length; i++) {
	                        for (int j = 0;j < box[0].length; j++) {
	                        	latsum += box[i][j].getLatitude();
                                longsum += box[i][j].getLongitude();
                            }       
	                }
	                int la = (int) ((latsum/4) * 100);
	                int lo = (int) ((longsum/4) * 100);
	                doc.add(new Field(TweetStatusField.LATITUDE,Integer.toString(la),Field.Store.YES,Field.Index.NOT_ANALYZED));
	                doc.add(new Field(TweetStatusField.LONGITUDE,Integer.toString(lo),Field.Store.YES,Field.Index.NOT_ANALYZED));
	        }
		}
	}
	
	/*
	 * Save the index to disk
	 */
	public void saveIndex()
	{
		
	}
	
	public boolean isPolice(long uid){
		return rtf.isPolice(uid);
	}
	
	
	//addField methods:
	//If you can think of a better way than function overloading to achieve this, go for it
	
	/* STRING fields
	 * Add the field,val pair to the doc in the index
	 * return false if the docid is not in the index already
	 */
	public boolean addField(int docid, String field,String value)
	{
		return fieldCreator(docid, field, value);
	}
	
	/* INT fields
	 */
	public boolean addField(int docid, String field,int value)
	{
		String val = String.valueOf(value);
		return fieldCreator(docid, field, val);
	}
	
	/* BOOLEAN fields
	 */
	public boolean addField(int docid, String field,boolean value)
	{
		String val = String.valueOf(value);
		return fieldCreator(docid, field, val);
		
		
	}
	
	/* double fields (latitudes and longitudes probably)
	 */
	public boolean addField(int docid, String field,double value)
	{
		String val = String.valueOf(value);
		return fieldCreator(docid, field, val);
		
	}
	
	public boolean fieldCreator(int docid, String field,String value){
		try{
			Term indexTerm = new Term(field); 
			TermDocs docs = statusIndexReader.termDocs(indexTerm);
			while (docs.next()){
				if (docs.doc() == docid){
					Document doc = new Document();
					doc.add(new Field(field, value, Field.Store.YES, Field.Index.NOT_ANALYZED));
					return true;
				  }
			}
			
			} catch (CorruptIndexException e) {

				e.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
			}
			
			return false;
	}
	
	//end addfields
	
	/* Return the best lat,long pair of the docid in index
	 * preference order is 
	 * 1. gps, 2. place field, 3. user place setting, 4. NERLoc, 5. RegexLoc
	 */
	public double[] getBestLocation(int docid)
	{
		return null;
	}
	
	/*
	 * Return a serialized object with all the needed stuff
	 * for the UI, maybe JSON or something. worry later
	 */
	public String docToJSON(int docid){
		return null;
	}
	
	//Searching:
	
	/*
	 * Return array of docids of documents whose
	 * tweet field has the word
	 */
	public int[] getWordMatches(String word){
		return null;
	}
	
	//gonna have to support returning a list of docids
	//that match lat/long ranges, date ranges, time ranges,
	//and categories for the web interface
	// I dont know how to do this so i'm going to ignore it
	
	
	
}
