package org.biocaddie.MLExamples;




import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SaveMode;

import edu.emory.mathcs.backport.java.util.Arrays;

public class MergeResults {

	public static void main(String[] args) throws FileNotFoundException {
		// Set up contexts.
		SparkContext sc = getSparkContext();
		SQLContext sqlContext = getSqlContext(sc);

		System.out.println(Arrays.toString(args));
		// read positive data set, cases where the PDB ID occurs in the sentence of the primary citation
		DataFrame set0 = sqlContext.read().parquet(args[0]).cache(); 
		DataFrame subset0 = set0.select("pdbId", "depositionYear", "pmcId", "pmId", "publicationYear").distinct();
		System.out.println("count0: " + subset0.count());
		
		DataFrame set1 = sqlContext.read().parquet(args[1]).cache(); 
		DataFrame subset1 = set1.select("pdbId", "depositionYear", "pmcId", "pmId", "publicationYear").distinct();
		System.out.println("count1: " + subset1.count());
		
		DataFrame set2 = sqlContext.read().parquet(args[2]).cache(); 
		DataFrame subset2 = set2.select("pdbId", "depositionYear", "pmcId", "pmId", "publicationYear").distinct();
		System.out.println("count2: " + subset2.count());
		
		DataFrame set3 = sqlContext.read().parquet(args[3]).cache(); 
		DataFrame subset3 = set3.select("pdbId", "depositionYear", "pmcId", "pmId", "publicationYear").distinct();
		System.out.println("count1: " + subset3.count());
		
		DataFrame set4 = sqlContext.read().parquet(args[4]).cache(); 
		DataFrame subset4 = set4.select("pdbId", "depositionYear", "pmcId", "pmId", "publicationYear").distinct();
		System.out.println("count4: " + subset4.count());
		
		DataFrame set5 = sqlContext.read().parquet(args[5]).cache(); 
		DataFrame subset5 = set5.select("pdbId", "depositionYear", "pmcId", "pmId", "publicationYear").distinct();
		System.out.println("count5: " + subset5.count());
		
		
		long start = System.nanoTime();
		
		DataFrame union = subset0.unionAll(subset1).unionAll(subset2).unionAll(subset3).unionAll(subset4).unionAll(subset5).coalesce(40).cache();
//		union.groupBy("depositionYear").count().show(100);
		union.filter("publicationYear IS NOT null").groupBy("publicationYear").count().show(100);
//		union.groupBy("depositionYear").mean("publicationYear").show(100);
//		union.groupBy("depositionYear").min("publicationYear").show(100);//
//		union.groupBy("depositionYear").max("publicationYear").show(100);
		
//		union.groupBy("pdbId", "pmcId").count().show(100);
		DataFrame unique = union.dropDuplicates();
		Map<String, String> aggregates = new HashMap<>();
		aggregates.put("pdbId", "sum");
//		unique.sort("pdbId").rollup("pdbId","pmcId").count().show(100);
		DataFrame counts = unique.groupBy("pmcId").count().cache();
//		counts.sort("count").show(100);
		counts.sort("count").groupBy("count").count().show(100);

	//	union.groupBy("depositionYear").agg(aggregates).show(100);
		System.out.println("Unique pdbIds: " + union.select("pdbId").distinct().count());
		System.out.println("Unique pmcIds: " + union.select("pmcId").distinct().count());
		
		DataFrame distinct = union.distinct();
		System.out.println("Distinct mentions: " + distinct.count());
		
		 try {
			 DataFrameToDelimitedFileWriter.write(args[6],  "\t", distinct);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		distinct.write().mode(SaveMode.Overwrite).parquet(args[7]);
	    long end = System.nanoTime();
	    System.out.println("Time: " + (end-start)/1E9 + " sec.");
	    
		sc.stop();
	}

	private static SparkContext getSparkContext() {
		Logger.getLogger("org").setLevel(Level.ERROR);
		Logger.getLogger("akka").setLevel(Level.ERROR);

		int cores = Runtime.getRuntime().availableProcessors();
		System.out.println("Available cores: " + cores);
		SparkConf conf = new SparkConf()
		.setMaster("local[" + cores + "]")
		.setAppName(MergeResults.class.getSimpleName())
		.set("spark.driver.maxResultSize", "4g")
		.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
		.set("spark.kryoserializer.buffer.max", "1g");

		SparkContext sc = new SparkContext(conf);

		return sc;
	}
	
	private static SQLContext getSqlContext(SparkContext sc) {
		SQLContext sqlContext = new SQLContext(sc);
		sqlContext.setConf("spark.sql.parquet.compression.codec", "snappy");
		sqlContext.setConf("spark.sql.parquet.filterPushdown", "true");
		return sqlContext;
	}
}