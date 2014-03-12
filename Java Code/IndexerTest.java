import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

public class IndexerTest {
	public static void main(String[] args) {
		IndexLines indexer = new IndexLines();
		try {
			// trainSet / testSet 
			indexer.test("trainSet");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
