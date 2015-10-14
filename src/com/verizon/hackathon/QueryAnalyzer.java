package com.verizon.hackathon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class QueryAnalyzer {

	public static void main(String[] args) {
		
		ArrayList<Query> queries = null;
		ArrayList<QueryMetaData> metaDataList = null;
		
		queries = getQueries();
		metaDataList = processQueries(queries);
		displayMetadata(metaDataList);
	}
	
	public static ArrayList<Query> getQueries(){
		System.out.println("In getQueries");
		ArrayList<Query> queries = new ArrayList<Query>();
		String queryStr1 = "INSERT INTO DS1.DT1 (DC1 ,DC2 ,DC3 ) "
				+ " SELECT ST1A.SC1 AS SC1A,  ST2A.SC2, ST2A.SC3 AS SC3A "
					+ " FROM SS1.ST1 ST1A JOIN SS1.ST2 ST2A ON ST1A.WC1 = ST2A.WC2 AND ST1A.WC3 = 10 "
					+ " WHERE ST2A.WC4 ='V1' AND WC5 <> TEXT_1 "
					+ " GROUP BY 1,2,3"
				+ " UNION ALL "
				+ " SELECT SC1A, SC4A AS SC2A, SC3A FROM "
					+ "(SELECT COALESCE(ST1A.SC1,ST1A.SC2,'X') AS SC1A, CASE WHEN ST2A.SC3 = 'I' ST2AC.SC3 ELSE 'Z' END AS SC4A, "
						+ " COALESCE(CASE WHEN ST1A.SC4 = ST2A.SC5 AND ST2A.SC6 = 123 THEN SUBSTR(ST2A.SC6,1,2) ELSE NULL END,ST1A.SC7,'Y') AS SC3A"
						+ " FROM SS1.ST1 ST1A JOIN SS1.ST2 ST2A ON ST1A.WC1 = ST2A.WC2 AND ST1A.WC3 = 10 "
						+ " WHERE ST2A.WC4 ='V1' AND WC5 <> TEXT_1 "
						+ " GROUP BY 1,2,3 QUALIFY RANK(SC2 ASC) = 1) ENT_EQL"
					+ " GROUP BY 1,2,3";
		queryStr1 = queryStr1.toUpperCase();				// To ensure indexOf calls works fine irrespective of case of query
		queryStr1 = queryStr1.replaceAll("\n", " ");		// To ensure all space checks work fine
		
		Query query1 = new Query();
		query1.setQueryId("1");
		query1.setQueryText(queryStr1);
		queries.add(query1);
		
		System.out.println("query1: " + query1);
		return queries;
	}
	
	public static ArrayList<QueryMetaData> processQueries(ArrayList<Query> queries){
		System.out.println("In processQueries");
		ArrayList<QueryMetaData> metaDataList = null;
		
		if (queries != null){
			for (int i=0; i < queries.size(); i++){
				if ( queries.get(i) != null ){
					Query query = queries.get(i);
					if ( query.getQueryText() != null && !"".equals(query.getQueryText().trim()) ){
						String queryTxt = query.getQueryText().trim();
						String queryType = queryTxt.substring(0, queryTxt.indexOf(' '));
						System.out.println("queryType: ." + queryType + ".");
						
						if ("INSERT".equalsIgnoreCase(queryType))
							metaDataList = processInsertQuery(query);
						else if ("UPDATE".equalsIgnoreCase(queryType))
							metaDataList = processUpdateQuery(query);
						else if ("DELETE".equalsIgnoreCase(queryType))
							metaDataList = processDeleteQuery(query);
						else
							System.out.println("Invalid Query");
					}
				}
			}
		}
		return metaDataList;
	}
	
	public static ArrayList<QueryMetaData> processInsertQuery(Query insertQuery){
		System.out.println("In processInsertQuery");
		ArrayList<QueryMetaData> metaDataList = new ArrayList<QueryMetaData>();
		
		String queryTxt = insertQuery.getQueryText();
		String targetTable = queryTxt.substring(queryTxt.indexOf("INTO") + 5, queryTxt.indexOf('('));
		String targetSchema = null;
		if (targetTable != null && !"".equals(targetTable.trim()))
		{
			targetTable = targetTable.trim();
			if (targetTable.contains(".")){
				targetSchema = targetTable.substring(0, targetTable.indexOf('.')).trim();
				targetTable = targetTable.substring(targetTable.indexOf('.')+1).trim();
			}
		}
		System.out.println("targetSchema: ." + targetSchema + ".");
		System.out.println("targetTable: ." + targetTable + ".");
		
		String targetColumns = queryTxt.substring(queryTxt.indexOf('(') + 1, queryTxt.indexOf(')')).trim();
		System.out.println("targetColumns: ." + targetColumns + ".");
		String[] targetColumnsArr = targetColumns.split(",");
		
		String sourceTxt = queryTxt.substring(queryTxt.indexOf(")") + 1).trim() ;
		String sourceType = sourceTxt.substring(0, sourceTxt.indexOf(' '));
		System.out.println("sourceType: ." + sourceType + ".");
		
		// Checking for Select Subquery
		String[] selectSubqueriesArr = null;
		String transformation = null;
		if ("SELECT".equalsIgnoreCase(sourceType)){
			
			// Checking for set operations: UNION [ALL], INTERSECT, MINUS
			if (sourceTxt.contains("UNION ALL ")){
				transformation = "UNION ALL";
				selectSubqueriesArr = sourceTxt.split("UNION ALL");
			}else if (sourceTxt.contains("UNION")){
				transformation = "UNION ";
				selectSubqueriesArr = sourceTxt.split("UNION");
			}else if (sourceTxt.contains("INTERSECT")){
				transformation = "INTERSECT";
				selectSubqueriesArr = sourceTxt.split("INTERSECT");
			}else if (sourceTxt.contains("MINUS")){
				transformation = "MINUS";
				selectSubqueriesArr = sourceTxt.split("MINUS");
			}else{
				selectSubqueriesArr = new String[]{sourceTxt};
			}
			for (int i=0; i < selectSubqueriesArr.length; i++){
				metaDataList = processSelectSubQuery(targetColumnsArr, selectSubqueriesArr[i], metaDataList);
				
				if (i != selectSubqueriesArr.length-1){
					// Adding the transformation for all subqueries except last one
					QueryMetaData qmd = new QueryMetaData();
					qmd.setTransformation(transformation);
					metaDataList.add(qmd);					
				}
			}
			
		} else if ("VALUES".equalsIgnoreCase(sourceType)){
			// To do in future if needed [SCALABILITY]
		}
	
		// The metadata from whereClause will not have the below generic data, so setting them finally 
		for (int i=0; i < metaDataList.size(); i++){
			QueryMetaData qmd = metaDataList.get(i);
			qmd.setQueryId(insertQuery.getQueryId());
			qmd.setQueryType("INSERT");
			qmd.setTargetSchema(targetSchema);
			qmd.setTargetTable(targetTable);
			metaDataList.set(i, qmd);
		}
		
		return metaDataList;
	}

	public static ArrayList<QueryMetaData> processSelectSubQuery(String[] targetColumnsArr, String selectQueryTxt, ArrayList<QueryMetaData> metaDataList){
		System.out.println("In processSelectSubQuery");
		
		String fromClause = null;
		String whereClause = null;
		String groupByClause = null;
		
		// If this select query has a subquery then call processSelectSubQuery for that subquery recursively
		if (selectQueryTxt.indexOf("SELECT", 7) >= 0){
			// Start point is the left brace before select
			int startPtOfSubQuery = selectQueryTxt.indexOf("SELECT",7);
			int startPtOfSubQueryBrace = selectQueryTxt.substring(0, selectQueryTxt.indexOf("SELECT",7)).lastIndexOf('(');
			// End point is the closing brace after select
			int endPtOfSubQuery = QueryUtil.getClosingBraceIndex(selectQueryTxt, ')', startPtOfSubQueryBrace+1);
			metaDataList = processSelectSubQuery(targetColumnsArr,selectQueryTxt.substring(startPtOfSubQuery, endPtOfSubQuery), metaDataList);
			
			selectQueryTxt = selectQueryTxt.substring(0, startPtOfSubQueryBrace) 
								+ selectQueryTxt.substring(endPtOfSubQuery + 1);
		}
		
		// Retrieve from where and group by clause
		if (selectQueryTxt.indexOf("WHERE") >= 0){
			fromClause = selectQueryTxt.substring(selectQueryTxt.indexOf("FROM") + 4, selectQueryTxt.indexOf("WHERE")).trim();
			if (selectQueryTxt.indexOf("GROUP BY") >= 0){
				whereClause = selectQueryTxt.substring(selectQueryTxt.indexOf("WHERE"), selectQueryTxt.indexOf("GROUP BY")).trim();
				groupByClause = selectQueryTxt.substring(selectQueryTxt.indexOf("GROUP BY")).trim();
			}else{
				whereClause = selectQueryTxt.substring(selectQueryTxt.indexOf("WHERE")).trim();
			}
		} else {
			fromClause = selectQueryTxt.substring(selectQueryTxt.indexOf("FROM") + 4).trim();
		}
		System.out.println("fromClause: ." + fromClause + ".");
		System.out.println("whereClause: ." + whereClause + ".");
		System.out.println("groupByClause: ." + groupByClause + ".");
		
		HashMap<String, SourceTable> sourceTableMap = processTableName(fromClause);
		SourceTable sourceTableObj = null;
		String sourceSchema = null;
		String sourceTable = null;
		String transformation = null;
		
		String sourceColumns = selectQueryTxt.substring(selectQueryTxt.indexOf("SELECT") + 6, selectQueryTxt.indexOf("FROM")).trim();
		System.out.println("sourceColumns: ." + sourceColumns + ".");
		String[] sourceColumnsArr = sourceColumns.split(",");
			
		for (int j=0; j < sourceColumnsArr.length; j++){
			if (sourceColumnsArr[j] != null && !"".equals(sourceColumnsArr[j].trim()) ){
				
				String sourceColumn = sourceColumnsArr[j].trim();
				if (sourceColumn.contains(" AS ")){
					System.out.println("sourceColumn contains Alias using AS. Removing it");
					sourceColumn = sourceColumn.substring(0, sourceColumn.indexOf("AS")).trim();
				} else if (sourceColumn.contains(" ")){
					System.out.println("sourceColumn contains Alias without AS. Removing it");
					sourceColumn = sourceColumn.substring(0, sourceColumn.indexOf(" ")).trim();
				}
				System.out.println("sourceColumn with table synonym: ." + sourceColumn + ".");
				
				if (sourceColumn.contains(".")){
					// If Column contains table ref, then use it to identify Source table details
					sourceTableObj 	= sourceTableMap.get(sourceColumn.substring(0, sourceColumn.indexOf(".")).trim() );
					sourceSchema 	= sourceTableObj.getSourceSchema();
					sourceTable 	= sourceTableObj.getSourceTable();
					sourceColumn 	= sourceColumn.substring(sourceColumn.indexOf(".")+1).trim();
					transformation  = sourceTableObj.getTransformation();

				} else if (sourceTableMap.size() == 1){
					// If Column contains just one table in query, then use it as Source table for all source columns
					Iterator iter 	= sourceTableMap.values().iterator();
					sourceSchema 	= ((SourceTable)iter.next()).getSourceSchema();
					sourceTable 	= ((SourceTable)iter.next()).getSourceTable();
					transformation  = ((SourceTable)iter.next()).getTransformation();
					
				} else {
					sourceSchema = "";		//"--No Mapping Available--";
					sourceTable = "";			//"--No Mapping Available--";
				}
				System.out.println("sourceSchema: ." + sourceSchema + ".");
				System.out.println("sourceTable: ." + sourceTable + ".");
				System.out.println("sourceColumn: ." + sourceColumn + ".");
				System.out.println("transformation: ." + transformation + ".");
					
				QueryMetaData qmd = new QueryMetaData();
				qmd.setTargetColumn(targetColumnsArr[j].trim());
				qmd.setSourceSchema(sourceSchema);
				qmd.setSourceTable(sourceTable);
				qmd.setSourceColumn(sourceColumn);
				qmd.setTransformation(transformation);
				metaDataList.add(qmd);		
			}
		}
		
		// Add where clause in transformation section
		if (whereClause != null && !"".equals(whereClause)){
			QueryMetaData qmd = new QueryMetaData();
			qmd.setTransformation(whereClause);
			metaDataList.add(qmd);		
		}
		
		// Add group by clause in transformation section
		if (groupByClause != null && !"".equals(groupByClause)){
			QueryMetaData qmd = new QueryMetaData();
			qmd.setTransformation(groupByClause);
			metaDataList.add(qmd);		
		}

		return metaDataList;
	}
	
public static HashMap<String, SourceTable> processTableName(String fromClause){
		
		System.out.println("In processTableName");
		HashMap<String, SourceTable> sourceTableMap = new HashMap<String, SourceTable>();	// Contains mapping of table alias to table properties object
		String sourceTableTxt = null;
		String sourceTableAlias = null;
		String transformation = null;
		String[] sourceTableList = null;
		if (fromClause.contains("RIGHT JOIN")){
			System.out.println("Tables separated by RIGHT JOIN");
			transformation = "RIGHT JOIN ";
			sourceTableList = fromClause.split("RIGHT JOIN");
		} else if (fromClause.contains("LEFT JOIN")){		// TO DO: Need to check if extra spaces in between LEFT and JOIN. Replace all multiple spaces with single space
			System.out.println("Tables separated by LEFT JOIN");
			transformation = "LEFT JOIN ";
			sourceTableList = fromClause.split("LEFT JOIN");
		} else if (fromClause.contains("JOIN")){
			System.out.println("Tables separated by  JOIN");
			transformation = "JOIN ";
			sourceTableList = fromClause.split("JOIN");
		} else if (fromClause.contains(",")){
			System.out.println("Tables separated by comma");
			sourceTableList = fromClause.split(",");
		} else {
			System.out.println("Just one table.");
			sourceTableList = new String[]{fromClause};
		}
		System.out.println("source Table Count: ." + sourceTableList.length + ".");
		
		if (sourceTableList != null){
			for (int i=0; i < sourceTableList.length; i++){
				sourceTableTxt = sourceTableList[i].trim();
				System.out.println("sourceTable: ." + sourceTableTxt + ".");

				SourceTable sourceTableObj = new SourceTable();
				if (!"NA".equals(transformation) && sourceTableTxt.indexOf("ON") >= 0){
					transformation += sourceTableTxt.substring(sourceTableTxt.indexOf("ON")).trim();
					sourceTableTxt = sourceTableTxt.substring(0, sourceTableTxt.indexOf("ON")).trim();
					
					sourceTableObj.setTransformation(transformation);
				}
					
				if (sourceTableTxt.contains(" AS ")){
					System.out.println("sourceTable contains Alias using AS. Noting it");
					sourceTableAlias = sourceTableTxt.substring(sourceTableTxt.indexOf("AS")+2).trim();
					sourceTableTxt = sourceTableTxt.substring(0, sourceTableTxt.indexOf("AS")).trim();
					
				} else if (sourceTableTxt.contains(" ")){
					System.out.println("sourceTable contains Alias without AS. Noting it");
					sourceTableAlias = sourceTableTxt.substring(sourceTableTxt.indexOf(" ")+1).trim();
					sourceTableTxt = sourceTableTxt.substring(0, sourceTableTxt.indexOf(" ")).trim();

				} else{
					System.out.println("sourceTable contains No Alias.");
					if (sourceTableTxt.contains(".")){
						sourceTableAlias = sourceTableTxt.substring(sourceTableTxt.indexOf(".")+1).trim();
					} else {
						sourceTableAlias = sourceTableTxt;
					}
				}
				if (sourceTableTxt.contains(".")){
					sourceTableObj.setSourceSchema(sourceTableTxt.substring(0, sourceTableTxt.indexOf(".")).trim());
					sourceTableObj.setSourceTable(sourceTableTxt.substring(sourceTableTxt.indexOf(".")+1).trim());	
				} else {
					sourceTableObj.setSourceTable(sourceTableTxt);	
				}
				sourceTableMap.put(sourceTableAlias, sourceTableObj);
			}
		}
		
		System.out.println("sourceTableMap: ." + sourceTableMap + ".");
		
		return sourceTableMap;
	}
	
	public static ArrayList<QueryMetaData> processUpdateQuery(Query updateQuery){
		System.out.println("In processUpdateQuery");
		ArrayList<QueryMetaData> metaDataList = null;
		
		return metaDataList;
	}
	
	public static ArrayList<QueryMetaData> processDeleteQuery(Query deleteQuery){
		System.out.println("In processDeleteQuery");
		ArrayList<QueryMetaData> metaDataList = null;
		
		return metaDataList;
				
	}
	public static void displayMetadata(ArrayList<QueryMetaData> queryMetaData){
		System.out.println("In displayMetadata: ");
		
		System.out.println("queryType \t queryId targetSchema \t targetTable \t targetColumn \t sourceSchema \t sourceTable \t sourceColumn \t transformation: " );
		for (int i=0; i < queryMetaData.size(); i++){
			System.out.println(queryMetaData.get(i));
		}
	}

}
