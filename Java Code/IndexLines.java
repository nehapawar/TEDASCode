import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.commons.lang.StringUtils;

/**
 * The class indexes lines depending on the keywords specified and output the
 * file train.arff to be used by classifiers.
 * 
 * @author Elisee Habimana modified on 6/5/2011
 */
public class IndexLines {
	private final String DATE_FORMAT_NOW = "yyyy_MM_dd";
	// array list of feature names
	private ArrayList<String> featureNames = new ArrayList<String>();
	// index i of this arraylist contains the various features corresponding to
	// arraylist of feature names
	private ArrayList<ArrayList<String>> featureSets = new ArrayList<ArrayList<String>>();
	private Hashtable<String, String> policeIDs = new Hashtable<String, String>();
	private Set<String> words = new HashSet<String>();
	private String[] arrayOfWords = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", 
	"ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
	"twenty", "thirty", "fourty", "fifty", "sixty", "seventy", "eighty", "ninety", 
	"hundred", "thousand", "million", "billion"};

	// Get current time, it will be used as fileName to store tweets in that
	// day.
	String getTime() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
		return sdf.format(cal.getTime());
	}

	public void test(String setName) throws IOException, ParseException {

		String time = getTime();
		// if (args.length > 0) {
		// String rawData = "indexerData/gdocData/" + setName + "_" + time
		// + ".txt";

		String rawData = "indexerData/gdocData/" + setName + ".txt";

		// 0. Specify the analyzer for tokenizing text.
		// The same analyzer should be used for indexing and searching
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);

		String kwFile = "config/features.txt"; // String kwFile =
		// "config/features.txt";
		String pdfile = "config/policeDept.txt";
		// String rawData = "rawData/2011-06-08"; //String rawData =
		// "6-7\\labeledNotCrime.txt";
		String trainFile = "indexerData/labeledtweets/l_" + setName + "_"
				+ time + ".arff"; // String trainFile =
		// "Labeledtweets\\train.arff";
		String resFile = "indexerData/matchedTweets/m_" + setName + "_" + time
				+ ".txt";
		String classLbl = "?";

		// 1. create index folder if inexistent in order to store the index
		policeIDs = storePolice(pdfile);
		Directory index = createIndexFolder(analyzer, rawData);

		// 3. search

		loadFeatureFile(kwFile);

		PrintWriter myPW = new PrintWriter(new BufferedWriter(new FileWriter(
				trainFile)));
		IndexSearcher searcher = new IndexSearcher(index, true);
		int hitsPerPage = 100000;
		TopScoreDocCollector collector = TopScoreDocCollector.create(
				hitsPerPage, true);
		searcher.search(getQuery(analyzer, myPW), collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		// 4. display matching results
		int frequency[] = new int[featureNames.size()];
		displayMatchingResults(hits, resFile, searcher, frequency, myPW,
				classLbl);
		// searcher can only be closed when there is no need to access the
		// documents any more.
		searcher.close();

		// } // close of if

		// else System.out.println("No argument specified");

	} // end of main

	// returns the Query to be passed, reads from keyword file and updates the
	// train file
	private Query getQuery(StandardAnalyzer analyzer, PrintWriter pw)
			throws IOException, ParseException {

		// BufferedReader br = new BufferedReader(new FileReader(kwFile));
		pw.print("@RELATION isCrime\n\n");
		String querystr = "";
		boolean lines = false;

		for (String str : featureNames) {
			if (lines)
				querystr += "OR (" + str + "*)";
			else
				querystr += "(" + featureNames.get(0) + "*)";
			pw.print("@ATTRIBUTE " + str + " NUMERIC\n");
			lines = true;
		}

		pw.print("@ATTRIBUTE from_police_dept {0,1}\n");
		pw.print("@ATTRIBUTE class    {0,1}\n\n");
		pw.print("@DATA\n");

		// the "TEXT" arg specifies the default field to use
		// when no field is explicitly specified in the query.
		Query qr = new QueryParser(Version.LUCENE_CURRENT, "TEXT", analyzer)
				.parse(querystr);

		return qr;
	}

	// store police departments sites in a hashtable
	private Hashtable<String, String> storePolice(String pdFile)
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

	private void addDoc(IndexWriter w, String value) throws IOException {
		Document doc = new Document();
		doc
				.add(new Field("TEXT", value, Field.Store.YES,
						Field.Index.ANALYZED));
		w.addDocument(doc);
	}

	private void displayMatchingResults(ScoreDoc[] hits, String resFile,
			IndexSearcher searcher, int[] frequency, PrintWriter myPW,
			String classLbl) throws IOException, ParseException {

		File results = new File(resFile);
		if (!results.exists()) {
			results.createNewFile();
			System.out.println("File " + resFile + " created!!");
		}
		PrintWriter pw = new PrintWriter(results);
		System.out.println("Printing results and updating train file...");

		int count = 0;

		addWordsToSet();
		for (int i = 0; i < hits.length; ++i) {
			int docId = hits[i].doc;
			Document d = searcher.doc(docId);

			String line = d.get("TEXT");
			//System.out.println(line);
			pw.println(line);
			// pw.println("***");
			// int[] array = convertTweetToFeatures(featureNames, featureSets,
			// line);
			line = line.toLowerCase();
			for (int j = 0; j < featureNames.size(); j++) {
				for (int k = 0; k < featureSets.get(j).size(); k++) {
					count += StringUtils.countMatches(line,
							(featureSets.get(j)).get(k));
				}
				if (j == 1) count = extractNumbers(line);
				myPW.print(count + ",");
				if (count > 0)
					frequency[j]++;
				count = 0;
			}

			line = line.trim();
			char last = line.charAt(line.length() - 1);
			char slast = line.charAt(line.length() - 3);
			// System.out.println(line);
			// System.out.println(slast + " " + last);

			if (slast == '0' || slast == '1' || slast == '2') {
				myPW.print(((slast - '0') % 2) + ",");
			} else
				myPW.print(classLbl + ",");
			if (last == '0' || last == '1' || last == '2') {
				myPW.println(((last - '0') % 2));
			} else
				myPW.println(classLbl);

		}
		myPW.close();
		pw.close();
		System.out
				.println("Found "
						+ hits.length
						+ " matches.\nTweets that match the criteria, have been saved in "
						+ resFile + " file");

	}
	
	private void addWordsToSet() {
		
		int i;
		for(i = 0; i < arrayOfWords.length; i++) {
			words.add(arrayOfWords[i]);
		}
			
	}
	
	private int extractNumbers(String line) {
		  
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
			while ((num.equals("and") || words.contains(num)) && st.hasMoreTokens()) {
				if (!(num.equals("and"))) { flag = true; }
				num = st.nextToken();
			}
			if (flag) {frequency++; flag = false;}
		}

		return frequency;
		
	}


	private Directory createIndexFolder(StandardAnalyzer analyzer,
			String rawData) throws IOException, ParseException {

		File indexStore = new File("index");
		indexStore.mkdir();

		Directory index = FSDirectory.open(indexStore);

		// create a new index, overwriting any existing index
		IndexWriter w = new IndexWriter(index, analyzer, true,
				IndexWriter.MaxFieldLength.UNLIMITED);

		// reads from a tweet file usually in format of date (YYYY-MM-DD), and
		// index each tweet
		FileReader file = new FileReader(rawData);
		BufferedReader br = new BufferedReader(file);
		boolean isTweet = false;
		if (br.readLine().equals("***"))
			isTweet = true;
		br.close();

		BufferedReader br1 = new BufferedReader(new FileReader(rawData));
		String tweet;
		String userID = " ";
		System.out.println("Reading tweets...");
		while ((tweet = br1.readLine()) != null) {
			System.out.println(tweet);
			if (isTweet && StringUtils.startsWith(tweet, "USERID")) {
				userID = tweet.split("\t")[1].trim();
			}
			if (isTweet && StringUtils.startsWith(tweet, "TEXT")) {
				// addDoc(w, tweet.substring(4).trim());
				if (policeIDs.containsKey(userID)) {
					addDoc(w, tweet.substring(4).trim() + "\t1");
				} else
					addDoc(w, tweet.substring(4).trim() + "\t0");
			} else if (!isTweet)
				addDoc(w, tweet);
		}

		br1.close();
		w.close();

		return index;
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

		/*
		for (int i = 0; i < featureNames.size(); i++) {
			System.out.print(featureNames.get(i) + ": ");
			ArrayList<String> tmp = featureSets.get(i);
			for (String ele : tmp) {
				System.out.print(ele + ",");
			}
			System.out.println();
		}
		*/
		br.close();
	}

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

	public void init() {
		try {
			loadFeatureFile("config/features.txt");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public double[] getTweetData(String tweetContent, long uid) {

		String userID = uid + "";
		double[] freq = new double[featureNames.size() + 2];
		double[] temp = convertTweetToFeatures(tweetContent);
		for (int i = 0; i < temp.length; i++)
			freq[i] = temp[i];
		freq[freq.length - 2] = (policeIDs.contains(userID)) ? 1 : 0;
		freq[freq.length - 1] = -1;
		return freq;
	}

	public ArrayList<String> getFeatureNames() {
		ArrayList<String> fns = new ArrayList<String>(featureNames);
		fns.add("from_police_dept");
		fns.add("class");
		return fns;
	}

	public boolean isPolice(long uid) {
		return policeIDs.contains(uid);
	}

} // end of class IndexLines