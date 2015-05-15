/*
 * Copyright (c) 2014-2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.openbmp.db_rest;

import java.awt.Window.Type;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;


public class DbUtils {
	private static final int MAX_THREADS = 12;				// Maximum number of threads to run for parallel query
	private static int rows;

	/**
	 * Return SELECT * FROM <tbl> in JSON format
	 * 
	 * 	Generic query to get all columns from a table based on passed WHERE and ORDER BY
	 * 
	 * @param ds			Datasource - must already be initialized, via context.xml or otherwise
	 * @param tableName		Name of the table to query
	 * @param limit			Max number of records to return
	 * @param where			SQL Where string
	 * @param orderby		SQL order by string
	 *  
	 * @return String Json formatted result
	 */
	static public String selectStar_DbToJson(DataSource ds, String tableName, Integer limit, String where, String orderby) {
		
		return selectStar_DbToJson(ds, tableName, limit, where, orderby, 0);
	}
	
	/**
	 * Return SELECT * FROM <tbl> in JSON format
	 * 
	 * 	Generic query to get all columns from a table based on passed WHERE and ORDER BY
	 * 
	 * @param ds			Datasource - must already be initialized, via context.xml or otherwise
	 * @param tableName		Name of the table to query
	 * @param limit			Max number of records to return
	 * @param where			SQL Where string
	 * @param orderby		SQL order by string
	 * @param extraTimeInMs Method time to add to the overall query time in milliseconds
	 * 
	 * @return String Json formatted result
	 */
	static public String selectStar_DbToJson(DataSource ds, String tableName, Integer limit, String where, String orderby,
					long extraTimeInMs) {
		
		String limit_st = " limit 1000";
		String where_st = "";
		String orderby_st = "";
		String output = "{}";
		
		// Set the limit for the query 
		if (limit != null && limit < 40000)
			limit_st = " limit " + limit;
		
		if (where != null)
			where_st = " WHERE " + where;
		
		if (orderby != null)
			orderby_st = " ORDER BY " + orderby;

		// build the query statement
		String select_query = "SELECT * FROM " + tableName + where_st + orderby_st + limit_st;

		System.out.println("QUERY: " + select_query);
	
		// Run the query
		output = select_DbToJson(ds, select_query, extraTimeInMs);
		
		return output;
	}
	
	
	/**
	 * Return Select query in JSON format
	 * 
	 * 	 Select based query, should contain the SELECT statement. 
	 * 
	 * @param ds			Datasource - must already be initialized, via context.xml or otherwise
	 * @param query			Full select query to run (including SELECT all the way to ORDER BY
	 *
	 * @return String Json formatted result
	 */
	static public String select_DbToJson(DataSource ds, String query) {
		return select_DbToJson(ds, query, 0);
	}
	
	/**
	 * Return Select query in JSON format
	 * 
	 * 	 Select based query, should contain the SELECT statement. 
	 * 
	 * @param ds			Datasource - must already be initialized, via context.xml or otherwise
	 * @param query			Full select query to run (including SELECT all the way to ORDER BY
 	 * @param extraTimeInMs Method time to add to the overall query time in milliseconds
	 *
	 * @return String Json formatted result
	 */
	static public String select_DbToJson(DataSource ds, String query, long extraTimeInMs) {
		
		String output = "{}";

		//
		// Run the query
		//
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			long fetchTime = System.currentTimeMillis();
						
			rs = stmt.executeQuery(query);
			output = DbUtils.DbResultsToJson(rs, (System.currentTimeMillis() - fetchTime) + extraTimeInMs);
            
		} catch (SQLException e) {
			e.printStackTrace();
			
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	
		return output;
	}
	
	/**
	 * Return Select query in MAP format
	 * 
	 * 	 Select based query, should contain the SELECT statement. 
	 * 
	 * @param ds			Datasource - must already be initialized, via context.xml or otherwise
	 * @param query			Full select query to run (including SELECT all the way to ORDER BY
	 *
	 * @return Map<String,List<DbColumnDef>> hash array of rows, each row is a list of columns
	 * 			primary key for the HashMap is the value of the concat values of the row minus the
	 * 			sum columns. 
	 */
	static public Map<String,List<DbColumnDef>> select_DbToMap(DataSource ds, String query) {
		
		Map<String,List<DbColumnDef>> results = new HashMap<String,List<DbColumnDef>>();

		//
		// Run the query
		//
		Connection conn = null;
		Statement stmt = null;
		ResultSet rs = null;
		
		try{
			conn = ds.getConnection();
			stmt = conn.createStatement();
			//long fetchTime = System.currentTimeMillis();
						
			rs = stmt.executeQuery(query);
			results = DbUtils.DbResultsToMap(rs, null);
            
		} catch (SQLException e) {
			e.printStackTrace();
			
		} finally {
			try {
				if (rs != null)
					rs.close();
				if (stmt != null)
					stmt.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	
		return results;
	}
	
	/**
	 * Return Select query in JSON format (using partitions)
	 * 
	 * 	 Select based query, should contain the SELECT statement.  
	 *   Expected partitions is 32 partitions normally key=hash_id.  Currently works
	 *   for 'rib' and 'path_attr' tables only. 
	 * 
	 * @param tableName				Table name to use in Json results
	 * @param ds			        Datasource - must already be initialized, via context.xml or otherwise
	 * @param query			        Full select query to run (including SELECT all the way to ORDER BY)
	 * 								Query should include the keyword PARTITION next to the TABLE that will be
	 * 								used for partitions.  PARTITION will be replaced with PARTITION (p<num>)
	 * @param startPartitionNumber  Starting partition number (e.g. 0), null will equal 0
	 * @param endPartitionNumber    Ending partition number (e.g. 31), null will equal 31
	 * @param sumColumns            Array of integers identifying the column number to sum (column must type int)
	 * @param limit                 Limit the results to specific amount
	 *
	 * @return String Json formatted result
	 */
	static public String selectPartitions_DbToJson(String tableName, DataSource ds, String query, 
				Integer startPartitionNumber, Integer endPartitionNumber, List<Integer> sumColumns, Integer limit) {

		long startTime = System.currentTimeMillis();
	    
		Map<String,List<DbColumnDef>> AggRows;
		
		
		AggRows = selectPartitions_DbToMap(ds, query, limit, startPartitionNumber, endPartitionNumber, sumColumns);
		String output = DbMapToJson(tableName, AggRows, (System.currentTimeMillis() - startTime));
        
		return output;
	}

	/**
	 * Same as selectPartitions_DbToJson but without limit
	 */
	static public String selectPartitions_DbToJson(String tableName, DataSource ds, String query, 
			Integer startPartitionNumber, Integer endPartitionNumber, List<Integer> sumColumns) {
			return selectPartitions_DbToJson(tableName, ds, query, 
											 startPartitionNumber, endPartitionNumber, sumColumns, null);
	}
	
	
	/**
	 * Return Select query in MapList format (using partitions)
	 * 
	 * 	 Select based query, should contain the SELECT statement.  
	 *   Expected partitions is 32 partitions normally key=hash_id.  Currently works
	 *   for 'rib' and 'path_attr' tables only. 
	 * 
	 * @param ds			        Datasource - must already be initialized, via context.xml or otherwise
	 * @param query			        Full select query to run (including SELECT all the way to ORDER BY)
	 * 								Query should include the keyword PARTITION next to the TABLE that will be
	 * 								used for partitions.  PARTITION will be replaced with PARTITION (p<num>)
	 * @param limit                 Limit the return map size, null is unlimited
	 * @param startPartitionNumber  Starting partition number (e.g. 0), null will equal 0
	 * @param endPartitionNumber    Ending partition number (e.g. 31), null will equal 31
	 * @param sumColumns            Array of integers identifying the column number to sum (column must type int)
	 *
	 * @return Map<String,List<DbColumnDef>> hash array of rows, each row is a list of columns
	 * 			primary key for the HashMap is the value of the concat values of the row minus the
	 * 			sum columns. 
	 */
	static public Map<String,List<DbColumnDef>> selectPartitions_DbToMap(
								DataSource ds, String query, Integer limit,
								Integer startPartitionNumber, Integer endPartitionNumber, List<Integer> sumColumns) {
		
		Map<String,List<DbColumnDef>> AggRows = new HashMap<String,List<DbColumnDef>>();

		long startTime = System.currentTimeMillis();
		
		
		if (startPartitionNumber == null)
			startPartitionNumber = 0;
		
		if (endPartitionNumber == null)
			endPartitionNumber = 31;
		
		int currentPartition = startPartitionNumber;

        ScheduledExecutorService thrPool = Executors.newScheduledThreadPool(MAX_THREADS);
        
        long queryTime = 0;
        long fetchTime = 0;
        
        int totalPartitions = (endPartitionNumber - startPartitionNumber) + 1;
        int maxThreads = totalPartitions < MAX_THREADS ? totalPartitions : MAX_THREADS;
        
        /*
         * Start the threads
         */
        queryThread thrs[] = new queryThread[maxThreads];
        
        for (int i=0; i < maxThreads; i++) {
        	thrs[i] = new queryThread(query.replace("PARTITION", "partition (p" + currentPartition + ")"),
        							  ds, sumColumns);
        	
        	thrPool.schedule(thrs[i], 0, TimeUnit.MILLISECONDS);
        	currentPartition++;
        }
        
        /*
         * Monitor active threads and start additional ones up to max partitions
         */
        while (currentPartition <= endPartitionNumber) {
        	for (int i=0; i < maxThreads; i++) {
        		if (thrs[i].done && currentPartition <= endPartitionNumber) {
        			// Save the query run time
        			queryTime += thrs[i].getRunTime();
        			
        			DbMapMerge(AggRows, thrs[i].getResults(), limit, sumColumns);
        			
        			// Reset the thread and start a new partition query
        			thrs[i].setDone(false);
        			thrs[i].setResults(null);
                	thrs[i].setQuery(query.replace("PARTITION", "partition (p" + currentPartition + ")"));
        			thrPool.schedule(thrs[i], 0, TimeUnit.MILLISECONDS);
        			
                	currentPartition++;
        		}
        	}
        }
        
        /*
         * Wait for remaining threads to finish
         */
        boolean stillRunning;
        while (thrs != null) {
        	stillRunning = false;
        	for (int i=0; i < maxThreads; i++) {
        		if (thrs[i].isDone() && thrs[i].getResults() != null) {
        			// Save the query run time
        			queryTime += thrs[i].getRunTime();
        			DbMapMerge(AggRows, thrs[i].getResults(), limit, sumColumns);
        			thrs[i].setResults(null);

        			//System.out.println("===== Thread is done");
        			
        		} else if (thrs[i].isDone() == false) {
        			stillRunning = true;
        		}
        	}
        	
        	if (stillRunning == false) {
        		thrs = null;
        	}
        }
        
        //output = dbUtils.DbResultsToJson(rs, System.currentTimeMillis() - fetchTime);
        System.out.println("Total runtime = " + queryTime + " Actual method time = " + (System.currentTimeMillis() - startTime));
        System.out.println("---------------------------------------------------");
        
		return AggRows;		
	}

	/**
	 * Merge DB Map List to a aggregated list.  
	 * 
	 * 	new objects that are not already there will be added.
	 *  Objects that exist will get replaced or if the column is marked
	 *  as summary, then it'll be added. 
	 * 
	 * @param aggListMap		Aggregated list/rows to merge to
	 * @param ListMap			Current list/rows to merge into parent
 	 * @param limit             Limit the return map size, null is unlimited
 	 * @param sumColumns        Array of integers identifying the column number to sum (column must type int)
	 * 
	 * @return parent list with updates made
	 */
	static public Map<String,List<DbColumnDef>> DbMapMerge(
							Map<String,List<DbColumnDef>> aggListMap, 
							Map<String,List<DbColumnDef>> listMap, 
							Integer limit, List<Integer>sumColumns) {

		// Validate limit size range - null is unlimited 
		if (limit != null && (limit < 1 || limit > 500000))
			limit = 1000;
		
		/*
		 * Loop through the current rows hashMap to be merged 
		 */
		for(Map.Entry<String, List<DbColumnDef>> row : listMap.entrySet()) {
			
			if (limit != null && aggListMap.size() >= limit) {
				break;
			}
			
			else if (! aggListMap.containsKey(row.getKey())) {
				// Row does not contain primary key, add row to the aggregated list
				aggListMap.put(row.getKey(), row.getValue());
			}
			
			else if (sumColumns != null){
				// Row exists, aggregate the columns that should be summed
				for (int i=0; i < sumColumns.size(); i++) {
					
					List<DbColumnDef> cols = row.getValue();
					if (sumColumns.get(i) < cols.size()) {
						// Sum column is within the list - sum the aggregate list column with this
						
						BigInteger value_int = null;
					
						// Sum the column or replace based on column type
						switch (aggListMap.get(row.getKey()).get(sumColumns.get(i)).getType()) {
							case Types.BIGINT:
							case Types.INTEGER:
							case Types.DECIMAL:
							case Types.TINYINT:
							case Types.SMALLINT:
								value_int = new BigInteger(aggListMap.get(row.getKey()).get(sumColumns.get(i)).getValue());
								value_int = value_int.add(new BigInteger(cols.get(sumColumns.get(i)).getValue()));
								aggListMap.get(row.getKey()).get(sumColumns.get(i)).setValue(value_int.toString());
								
								//System.out.println("Updated " + aggListMap.get(row.getKey()).get(sumColumns.get(i)).getValue() + " + " + cols.get(sumColumns.get(i)).getValue() +  " = " + value_int);
								break; 
								
							default :
								aggListMap.get(row.getKey()).get(sumColumns.get(i)).setValue(cols.get(sumColumns.get(i)).getValue());
								break;
						}
					}
				}
			}
		}
		
		return aggListMap;
	}
	
	/**
	 * Return database Map results in JSon format
	 * 	
	 * 	Loops through the Map/List results and returns them in string Json format. 
	 * 
	 * JSON Syntax: 
	 * 			{ <table_name> : {
	 * 								cols: <number of columns>,
	 * 								size: <total number of rows,
	 *                              queryTime_ms: <ms value>,
	 *                              fetchTime_ms: <ms value>,
	 *                              data: [ { <col name>:<value>, ... }, ... ],
	 *                           }
	 *          }
	 * 
	 * @param tableName		Table name to use in result
	 * @param rowsMap		Map List of rows to convert to json
	 * @param queryTime_ms	Time it took to run the query
	 * 	
	 * @return JSON string formatted map result
	 */
	static public String DbMapToJson(String tableName, Map<String,List<DbColumnDef>> rowsMap, long queryTime_ms) {
		StringWriter swriter = new StringWriter();
		JsonFactory jfac = new JsonFactory();
		long fetchTime = System.currentTimeMillis();

		try {
			JsonGenerator jgen = jfac.createJsonGenerator(swriter);
			jgen.writeStartObject(); // Root object
			jgen.writeObjectFieldStart(tableName);

			jgen.writeObjectFieldStart("data");
			
			if (rowsMap.size() > 0) {
				jgen.writeNumberField("cols",  rowsMap.entrySet().iterator().next().getValue().size());
			} else {
				jgen.writeNumberField("cols", 0);
			}

			jgen.writeNumberField("size", rowsMap.size());
			jgen.writeNumberField("queryTime_ms", queryTime_ms);
			
			// Begin the data/rows array
			jgen.writeArrayFieldStart("data");

			// Sort the rows map
			Map<String,List<DbColumnDef>> rows = new TreeMap<String,List<DbColumnDef>>(rowsMap); 
	        
			// Loop through all rows
	        for(Map.Entry<String, List<DbColumnDef>> row : rows.entrySet()){
	        	//System.out.println("PriKey=" + row.getKey());

	        	// Begin row object
	        	jgen.writeStartObject();
	        	
	        	List<DbColumnDef> cols = row.getValue();
	        	for (int i=0; i < cols.size(); i++) {
	        		
	        		// Write column and value field
	        		jgen.writeFieldName(cols.get(i).getName());

	        		switch (cols.get(i).getType()) {
						case Types.BIGINT:
							jgen.writeNumber(new BigInteger(cols.get(i).getValue()));
							break;
							
						case Types.TINYINT:
						case Types.INTEGER:
							jgen.writeNumber(new Integer(cols.get(i).getValue()));
							break;
							
						default:
							jgen.writeString(cols.get(i).getValue());
							break;
	        		}
	        		
	        		
	        		//System.out.print(cols.get(i).getName() + " == ");
					//System.out.print(cols.get(i).getValue() + " , ");
				}

				jgen.writeEndObject();
	        	
	        	//System.out.println(" ");
			}

			// end the data array
			jgen.writeEndArray();

			jgen.writeNumberField("fetchTime_ms", System.currentTimeMillis() - fetchTime);

			// end the table object
			jgen.writeEndObject();

			// end the root object
			jgen.writeEndObject();

			jgen.close();

		} catch (JsonGenerationException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		return  swriter.toString();
	}
	
	/**
	 * Return database results in JSon format
	 * 	
	 * 	Loops through the results and returns them in string form. 
	 * 
	 * JSON Syntax: 
	 * 			{ <table_name> : {
	 * 								cols: <number of columns>,
	 * 								rows: [ { <col name>:<value>, ... }, ... ],
	 * 								size: <total number of rows,
	 *                              queryTime_ms: <ms value>,
	 *                              fetchTime_ms: <ms value>
	 *                           }
	 *          }
	 * 
	 * @param db_results		Results from DB query	
	 * @return
	 */
	static public String DbResultsToJson(ResultSet db_results, long queryTime_ms) {
		StringWriter swriter = new StringWriter();
		JsonFactory jfac = new JsonFactory();
		long fetchTime = System.currentTimeMillis();

		try {
			ResultSetMetaData meta = db_results.getMetaData();
			JsonGenerator jgen = jfac.createJsonGenerator(swriter);
			
			jgen.writeStartObject(); // Root object
			if (meta.getTableName(1).length() > 0)
				jgen.writeObjectFieldStart(meta.getTableName(1));
			else if (meta.getColumnCount() > 1 && meta.getTableName(2).length() > 0)
				jgen.writeObjectFieldStart(meta.getTableName(2));
			else
				jgen.writeObjectFieldStart("table");
			
			//jgen.writeObjectFieldStart("data");
			jgen.writeNumberField("cols", meta.getColumnCount());

			//jgen.writeArrayFieldStart("rows");
			jgen.writeArrayFieldStart("data");

			int rows = 0;
			while (db_results.next()) {
				rows++;

				jgen.writeStartObject();

				for (int i = 1; i <= meta.getColumnCount(); i++) {
					jgen.writeFieldName(meta.getColumnLabel(i));

					switch (meta.getColumnType(i)) {
					
						case Types.BIGINT:
							jgen.writeNumber(db_results.getBigDecimal(i));
							break;
							
						case Types.TINYINT:
							jgen.writeNumber(db_results.getShort(i));
							break;
							
						case Types.INTEGER:
							jgen.writeNumber(db_results.getInt(i));
							break;
							
						case Types.BOOLEAN: 
							jgen.writeBoolean(db_results.getBoolean(i));
							break;
							
						default:
							jgen.writeString(db_results.getString(i));
							break;
					}
				}

				jgen.writeEndObject();
			}

			// Add object to files array
			// jgen.writeRawValue(JsonUtil.genPojoJson(ribEntry));

			// end the files array
			jgen.writeEndArray();

			jgen.writeNumberField("size", rows);
			jgen.writeNumberField("queryTime_ms", queryTime_ms);
			jgen.writeNumberField("fetchTime_ms", System.currentTimeMillis() - fetchTime);

			// end the object field v_routes
			jgen.writeEndObject();

			// end the root object
			jgen.writeEndObject();

			jgen.close();

		} catch (SQLException e) {
			e.printStackTrace();

		} catch (JsonGenerationException e) {
			e.printStackTrace();

		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		return  swriter.toString();
	}
	
	/**
	 * Return database results in Map<list> format
	 * 	
	 * 	Loops through the results and returns them in an ArrayList Map format 
	 * 
	 *  The returned result is an Array of Maps.  Each Map represents
	 *  a single row.  Each map key is the column name, and the value is 
	 *  a column definition. 
	 * 
	 * @param db_results	Rsults from DB query	
	 * @param sumColumns    Array of integers identifying the column number to sum (column must type int)
	 *
	 * @return Map<String,List<DbColumnDef>> hash array of rows, each row is a list of columns
	 * 			primary key for the HashMap is the value of the concat values of the row minus the
	 * 			sum columns. 
	 */
	static public Map<String,List<DbColumnDef>> DbResultsToMap(ResultSet db_results, List<Integer> sumColumns) {
		Map<String,List<DbColumnDef>> rows = new HashMap<String,List<DbColumnDef>>();
		
		try {
			ResultSetMetaData meta = db_results.getMetaData();
			
			StringBuilder priKey = new StringBuilder();

			while (db_results.next()) {
				List<DbColumnDef> row = new ArrayList<DbColumnDef>();
				priKey = new StringBuilder();
				
				for (int i = 1; i <= meta.getColumnCount(); i++) {
					DbColumnDef col = new DbColumnDef();
				
					col.setName(meta.getColumnLabel(i));
					col.setType(meta.getColumnType(i));
					col.setValue(db_results.getString(i));
					
					// Add value to primary key if the value is not part of sumColumns
					if (sumColumns == null || ! sumColumns.contains(i - 1)) {
						priKey.append(col.getValue());
						priKey.append(' ');
					}
					
					//System.out.print("Col=" + meta.getColumnLabel(i) + ", value=" + db_results.getString(i) + " ");
					// Add column to row map
					row.add(col);
				}
				
				//System.out.println("\n    priKey = " + priKey.toString());
				
				// Add row to list
				rows.put(priKey.toString(), row);

			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		return rows;
	}

    // Query thread to run in a loop for each partition
    static public class queryThread implements Runnable {
		private Connection conn = null;
		private Statement stmt = null;
		private ResultSet rs = null;
		
		private Map<String,List<DbColumnDef>> results = null;
		
		private boolean done = false;
		private long startTime = 0;
		private long runTime = 0;
		
		private String query;
		private DataSource ds;
		private List<Integer> sumColumns;
		
		queryThread(String query, DataSource ds, List<Integer> sumColumns) {
			this.query = query;
			this.ds = ds;
			this.sumColumns = sumColumns;
		}
		
		public void run() {
			startTime = System.currentTimeMillis();
			try{
				conn = ds.getConnection();
				stmt = conn.createStatement();
				//System.out.println("PQUERY = " + query);
				rs = stmt.executeQuery(query);
				//json = DbUtils.DbResultsToJson(rs, System.currentTimeMillis() - startTime);
				results = DbUtils.DbResultsToMap(rs, sumColumns);
				
			} catch (SQLException e) {
				e.printStackTrace();
				
			} finally {
				try {
					if (rs != null)
						rs.close();
					if (stmt != null)
						stmt.close();
					if (conn != null)
						conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			runTime = System.currentTimeMillis() - startTime;
			done = true;
		}

		public Map<String, List<DbColumnDef>> getResults() {
			return results;
		}

		public void setResults(Map<String, List<DbColumnDef>> results) {
			this.results = results;
		}

		public long getStartTime() {
			return startTime;
		}

		public void setStartTime(long startTime) {
			this.startTime = startTime;
		}

		public long getRunTime() {
			return runTime;
		}

		public void setRunTime(long runTime) {
			this.runTime = runTime;
		}

		public String getQuery() {
			return query;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public boolean isDone() {
			return done;
		}

		public void setDone(boolean done) {
			this.done = done;
		}
	}
	
}
