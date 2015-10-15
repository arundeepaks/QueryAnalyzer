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
		
		ArrayList<Query> queries = ExcelProcessor.readQueries();
		
		/*ArrayList<Query> queries = new ArrayList<Query>();
		String queryStr1 = "INSERT INTO DS1.DT1 (DC1 ,DC2 ,DC3 ) "
				+ " SELECT CASE WHEN ST2A.SC3 = 'I' THEN CASE WHEN ST2A.SC6 = 123 THEN ST2A.SC6 ELSE NULL END ELSE 'Z' END AS SC1A,  ST2A.SC2, ST2A.SC3 AS SC3A "
					+ " FROM SS1.ST1 ST1A JOIN SS1.ST2 ST2A ON ST1A.WC1 = ST2A.WC2 AND ST1A.WC3 = 10 "
					+ " WHERE ST2A.WC4 ='V1' AND WC5 <> TEXT_1 "
					+ " GROUP BY 1,2,3"
				+ " UNION ALL "
				+ " SELECT SC1A, SC4A AS SC2A, SC3A FROM "
					+ "(SELECT COALESCE(ST1A.SC1,ST1A.SC2,'X') AS SC1A, CASE WHEN ST2A.SC3 = 'I' THEN ST2A.SC3 ELSE 'Z' END AS SC4A, "
						+ " COALESCE(CASE WHEN ST1A.SC4 = ST2A.SC5 AND ST2A.SC6 = 123 THEN ST2A.SC6 ELSE NULL END,ST1A.SC7,'Y') AS SC3A"
						+ " FROM SS1.ST1 ST1A JOIN SS1.ST2 ST2A ON ST1A.WC1 = ST2A.WC2 AND ST1A.WC3 = 10 "
						+ " WHERE ST2A.WC4 ='V1' AND WC5 <> TEXT_1 "
						+ " GROUP BY 1,2,3 QUALIFY RANK(SC2 ASC) = 1) ENT_EQL"
					+ " GROUP BY 1,2,3";
		queryStr1 = queryStr1.toUpperCase();				// To ensure indexOf calls works fine irrespective of case of query
		queryStr1 = queryStr1.replaceAll("\n", " ");		// To ensure all space checks work fine
		
		Query query1 = new Query();
		query1.setQueryId("1");
		query1.setQueryText(queryStr1);
		queries.add(query1);*/
		
		return queries;
	}
	
	public static ArrayList<QueryMetaData> processQueries(ArrayList<Query> queries){
		System.out.println("In processQueries");
		ArrayList<QueryMetaData> metaDataList = new ArrayList<QueryMetaData>();
		
		if (queries != null){
			//for (int i=0; i < queries.size(); i++){
			for (int i=5; i < 6; i++){
				if ( queries.get(i) != null ){
					Query query = queries.get(i);
					
					if ( query.getQueryText() != null && !"".equals(query.getQueryText().trim()) ){
						String queryTxt = query.getQueryText().trim();
						
						queryTxt = queryTxt.toUpperCase();				// To ensure indexOf calls works fine irrespective of case of query
						queryTxt = queryTxt.replaceAll("\n", " ");		// To ensure all space checks work fine
						
						String queryType = queryTxt.substring(0, queryTxt.indexOf(' '));
						System.out.println("queryType: ." + queryType + ".");
						
						if ("INSERT".equalsIgnoreCase(queryType))
							metaDataList = processInsertQuery(query, metaDataList);
						else if ("UPDATE".equalsIgnoreCase(queryType))
							metaDataList = processUpdateQuery(query, metaDataList);
						else if ("DELETE".equalsIgnoreCase(queryType))
							metaDataList = processDeleteQuery(query, metaDataList);
						else
							System.out.println("Invalid Query");
					}
				}
			}
		}
		return metaDataList;
	}
	
	public static ArrayList<QueryMetaData> processInsertQuery(Query insertQuery, ArrayList<QueryMetaData> metaDataList){
		System.out.println("In processInsertQuery");
		
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
		
		String sourceTxt = queryTxt.substring(queryTxt.indexOf(")") + 1).trim();
		String sourceType = sourceTxt.substring(0, sourceTxt.indexOf(' ')).trim();
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
			if (qmd.getQueryId() == null){
				qmd.setQueryId(insertQuery.getQueryId());
				qmd.setQueryType("INSERT");
				qmd.setTargetSchema(targetSchema);
				qmd.setTargetTable(targetTable);
				metaDataList.set(i, qmd);
			}
		}
		
		return metaDataList;
	}

	public static ArrayList<QueryMetaData> processSelectSubQuery(String[] targetColumnsArr, String selectQueryTxt, ArrayList<QueryMetaData> metaDataList){
		System.out.println("In processSelectSubQuery");
		
		String fromClause = null;
		String whereClause = null;
		String groupByClause = null;
		String transformation = null;
		
		// If this select subquery has another subquery then call processSelectSubQuery for that subquery recursively
		if (selectQueryTxt.indexOf("SELECT", 7) >= 0){
			// Start point is the left brace before select
			int startPtOfSubQuery = selectQueryTxt.indexOf("SELECT",7);
			int startPtOfSubQueryBrace = selectQueryTxt.substring(0, selectQueryTxt.indexOf("SELECT",7)).lastIndexOf('(');
			// End point is the closing brace after select
			int endPtOfSubQuery = QueryUtil.getClosingBraceIndex(selectQueryTxt, ')', startPtOfSubQueryBrace+1);
			String subquery = selectQueryTxt.substring(startPtOfSubQuery, endPtOfSubQuery);
			System.out.println("subquery: ." + subquery + ".");
			
			// Processing Subquery
			metaDataList = processSelectSubQuery(targetColumnsArr,subquery, metaDataList);
			
			selectQueryTxt = selectQueryTxt.substring(0, startPtOfSubQueryBrace) + selectQueryTxt.substring(endPtOfSubQuery + 1);
			transformation = "Takes data from Subquery";
			
			System.out.println("Remaining super query: ." + selectQueryTxt + ".");
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
		} else if (selectQueryTxt.indexOf("GROUP BY") >= 0){
			fromClause = selectQueryTxt.substring(selectQueryTxt.indexOf("FROM") + 4, selectQueryTxt.indexOf("GROUP BY")).trim();
			groupByClause = selectQueryTxt.substring(selectQueryTxt.indexOf("GROUP BY")).trim();
		} else {
			fromClause = selectQueryTxt.substring(selectQueryTxt.indexOf("FROM") + 4).trim();
		}
		System.out.println("fromClause: ." + fromClause + ".");
		System.out.println("whereClause: ." + whereClause + ".");
		System.out.println("groupByClause: ." + groupByClause + ".");
		
		HashMap<String, SourceTable> sourceTableMap = processTableName(fromClause);
		
		String sourceColumns = selectQueryTxt.substring(selectQueryTxt.indexOf("SELECT") + 6, selectQueryTxt.indexOf("FROM")).trim();
		System.out.println("sourceColumns: ." + sourceColumns + ".");
		
		String coalesceSection1 = null;
		String coalesceSection2 = null;
		if (sourceColumns.contains("COALESCE")){
			coalesceSection1 = sourceColumns.substring(sourceColumns.indexOf("COALESCE"), sourceColumns.indexOf(")")+1).trim();
			sourceColumns = sourceColumns.replace(coalesceSection1, "coalesceSection1");
			System.out.println("coalesceSection1: ." + coalesceSection1 + ".");
		}
		if (sourceColumns.contains("COALESCE")){
			coalesceSection2 = sourceColumns.substring(sourceColumns.indexOf("COALESCE"), 
					QueryUtil.getClosingBraceIndex(sourceColumns, ')', sourceColumns.indexOf("COALESCE")+9)+1).trim();
			sourceColumns = sourceColumns.replace(coalesceSection2, "coalesceSection2");
			System.out.println("coalesceSection2: ." + coalesceSection2 + ".");
		}
		System.out.println("sourceColumns after removing CS: ." + sourceColumns + ".");
		
		String[] sourceColumnsArr = sourceColumns.split(",");
			
		for (int j=0; j < sourceColumnsArr.length; j++){
			if (sourceColumnsArr[j] != null && !"".equals(sourceColumnsArr[j].trim()) ){
				
				String sourceColumn = sourceColumnsArr[j].trim();
				
				if (sourceColumn.contains(" AS ")){
					System.out.println("sourceColumn contains Alias using AS. Removing it");
					sourceColumn = sourceColumn.substring(0, sourceColumn.indexOf(" AS ")).trim();
				} else if (sourceColumn.contains(" ")){
					
					// TO DO: Take care of aliases which doesnt use AS
					// System.out.println("sourceColumn contains Alias without AS. Removing it");
					// sourceColumn = sourceColumn.substring(0, sourceColumn.indexOf(" ")).trim();
				}
				System.out.println("sourceColumn with table synonym: ." + sourceColumn + ".");
				
				QueryMetaData qmd = null;
				
				if (sourceColumn.contains("coalesceSection1")){
					System.out.println("Replacing coalesceSection1");
					sourceColumn = sourceColumn.replace("coalesceSection1", coalesceSection1);
				}
				
				if (sourceColumn.contains("coalesceSection2")){
					System.out.println("Replacing coalesceSection2");
					sourceColumn = sourceColumn.replace("coalesceSection2", coalesceSection2);
				}
				
				if (sourceColumn.startsWith("COALESCE")){
					metaDataList = processCoalesceColumn(sourceColumn, targetColumnsArr[j].trim(), sourceTableMap, metaDataList);
				} else if (sourceColumn.startsWith("CASE")){
					metaDataList = processCaseColumn(sourceColumn, targetColumnsArr[j].trim(), sourceTableMap, metaDataList);
				} else {
					qmd = createMetaDataRecord(sourceColumn, targetColumnsArr[j].trim(), sourceTableMap, transformation);
					metaDataList.add(qmd);
				}
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
	
	public static ArrayList<QueryMetaData> processSelectSubQueryForUpdate(String[] targetColumnsArr, String selectQueryTxt, ArrayList<QueryMetaData> metaDataList){
		System.out.println("In processSelectSubQueryForpdate");
		
		String fromClause = null;
		String whereClause = null;
		String groupByClause = null;
		String transformation = null;
		
		// If this select subquery has another subquery then call processSelectSubQuery for that subquery recursively
		if (selectQueryTxt.indexOf("SELECT", 7) >= 0){
			// Start point is the left brace before select
			int startPtOfSubQuery = selectQueryTxt.indexOf("SELECT",7);
			int startPtOfSubQueryBrace = selectQueryTxt.substring(0, selectQueryTxt.indexOf("SELECT",7)).lastIndexOf('(');
			// End point is the closing brace after select
			int endPtOfSubQuery = QueryUtil.getClosingBraceIndex(selectQueryTxt, ')', startPtOfSubQueryBrace+1);
			String subquery = selectQueryTxt.substring(startPtOfSubQuery, endPtOfSubQuery);
			System.out.println("subquery: ." + subquery + ".");
			
			// Processing Subquery
			metaDataList = processSelectSubQueryForUpdate(targetColumnsArr,subquery, metaDataList);
			
			selectQueryTxt = selectQueryTxt.substring(0, startPtOfSubQueryBrace) + selectQueryTxt.substring(endPtOfSubQuery + 1);
			transformation = "Takes data from Subquery";
			
			System.out.println("Remaining super query: ." + selectQueryTxt + ".");
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
		} else if (selectQueryTxt.indexOf("GROUP BY") >= 0){
			fromClause = selectQueryTxt.substring(selectQueryTxt.indexOf("FROM") + 4, selectQueryTxt.indexOf("GROUP BY")).trim();
			groupByClause = selectQueryTxt.substring(selectQueryTxt.indexOf("GROUP BY")).trim();
		} else {
			fromClause = selectQueryTxt.substring(selectQueryTxt.indexOf("FROM") + 4).trim();
		}
		System.out.println("fromClause: ." + fromClause + ".");
		System.out.println("whereClause: ." + whereClause + ".");
		System.out.println("groupByClause: ." + groupByClause + ".");
		
		HashMap<String, SourceTable> sourceTableMap = processTableName(fromClause);
		
		String sourceColumns = selectQueryTxt.substring(selectQueryTxt.indexOf("SELECT") + 6, selectQueryTxt.indexOf("FROM")).trim();
		System.out.println("sourceColumns: ." + sourceColumns + ".");
		
		String coalesceSection1 = null;
		String coalesceSection2 = null;
		if (sourceColumns.contains("COALESCE")){
			coalesceSection1 = sourceColumns.substring(sourceColumns.indexOf("COALESCE"), sourceColumns.indexOf(")")+1).trim();
			sourceColumns = sourceColumns.replace(coalesceSection1, "coalesceSection1");
			System.out.println("coalesceSection1: ." + coalesceSection1 + ".");
		}
		if (sourceColumns.contains("COALESCE")){
			coalesceSection2 = sourceColumns.substring(sourceColumns.indexOf("COALESCE"), 
					QueryUtil.getClosingBraceIndex(sourceColumns, ')', sourceColumns.indexOf("COALESCE")+9)+1).trim();
			sourceColumns = sourceColumns.replace(coalesceSection2, "coalesceSection2");
			System.out.println("coalesceSection2: ." + coalesceSection2 + ".");
		}
		System.out.println("sourceColumns after removing CS: ." + sourceColumns + ".");
		
		String[] sourceColumnsArr = sourceColumns.split(",");
			
		for (int j=0; j < sourceColumnsArr.length; j++){
			if (sourceColumnsArr[j] != null && !"".equals(sourceColumnsArr[j].trim()) ){
				
				String sourceColumn = sourceColumnsArr[j].trim();
				
				if (sourceColumn.contains(" AS ")){
					System.out.println("sourceColumn contains Alias using AS. Removing it");
					sourceColumn = sourceColumn.substring(0, sourceColumn.indexOf(" AS ")).trim();
				} else if (sourceColumn.contains(" ")){
					
					// TO DO: Take care of aliases which doesnt use AS
					// System.out.println("sourceColumn contains Alias without AS. Removing it");
					// sourceColumn = sourceColumn.substring(0, sourceColumn.indexOf(" ")).trim();
				}
				System.out.println("sourceColumn with table synonym: ." + sourceColumn + ".");
				
				QueryMetaData qmd = null;
				
				if (sourceColumn.contains("coalesceSection1")){
					System.out.println("Replacing coalesceSection1");
					sourceColumn = sourceColumn.replace("coalesceSection1", coalesceSection1);
				}
				
				if (sourceColumn.contains("coalesceSection2")){
					System.out.println("Replacing coalesceSection2");
					sourceColumn = sourceColumn.replace("coalesceSection2", coalesceSection2);
				}
				
				if (sourceColumn.startsWith("COALESCE")){
					metaDataList = processCoalesceColumn(sourceColumn, targetColumnsArr[j].trim(), sourceTableMap, metaDataList);
				} else if (sourceColumn.startsWith("CASE")){
					metaDataList = processCaseColumn(sourceColumn, targetColumnsArr[j].trim(), sourceTableMap, metaDataList);
				} else {
					qmd = createMetaDataRecord(sourceColumn, targetColumnsArr[j].trim(), sourceTableMap, transformation);
					metaDataList.add(qmd);
				}
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
	
	public static ArrayList<QueryMetaData> processCoalesceColumn(String sourceColumn, String targetColumn, HashMap<String, SourceTable> sourceTableMap, ArrayList<QueryMetaData> metaDataList){
		System.out.println("In processCoalesceColumn: sourceColumn:"+sourceColumn);
		String concatStr = null;
		String[] concatList = null;
		String transformation = "COALESCE";
		concatStr = sourceColumn.substring(sourceColumn.indexOf('(')+1, 
				QueryUtil.getClosingBraceIndex(sourceColumn, ')', sourceColumn.indexOf('(')+1)).trim();
		System.out.println("concatStr: ." + concatStr + ".");
		
		String caseSection1 = null;
		if (concatStr.contains("CASE")){
			caseSection1 = concatStr.substring(concatStr.indexOf("CASE"), concatStr.indexOf("END")+3).trim();
			concatStr = concatStr.replace(caseSection1, "caseSection1");
			System.out.println("caseSection1: ." + caseSection1 + ".");
		}
		
		concatList = concatStr.split(",");
		System.out.println("concatList length: ." + concatList.length + ".");
		
		for (int j=0; j < concatList.length; j++){
			String concatCol = concatList[j].trim();
			
			if (concatCol.contains("caseSection1"))
				concatCol = concatCol.replace("caseSection1", caseSection1);
			
			if (concatCol.startsWith("CASE")){
				metaDataList = processCaseColumn(concatCol,targetColumn,sourceTableMap,metaDataList);
			}else{
				QueryMetaData qmd = createMetaDataRecord(concatList[j], targetColumn, sourceTableMap, transformation);
				metaDataList.add(qmd);	
			}
		}
		return metaDataList;
	}
	
	public static ArrayList<QueryMetaData> processCaseColumn(String sourceColumn, String targetColumn, HashMap<String, SourceTable> sourceTableMap, ArrayList<QueryMetaData> metaDataList){
		System.out.println("In processCaseColumn: sourceColumn:"+sourceColumn);
		
		// Handling case inside case statements
		// To do: Need to handle when there are 2 cases inside one case
		String innerCase = null;
		if (sourceColumn.indexOf("CASE", sourceColumn.indexOf("CASE") + 4) > -1){
			int startIndex = sourceColumn.indexOf("CASE", sourceColumn.indexOf("CASE") + 4);
			int endIndex = sourceColumn.indexOf("END", sourceColumn.indexOf("CASE") + 4) + 3;
			innerCase = sourceColumn.substring(startIndex, endIndex).trim();
			System.out.println("innerCase: ." + innerCase + ".");
			
			sourceColumn = sourceColumn.substring(0, startIndex) + " INNER_CASE " + sourceColumn.substring(endIndex);
			System.out.println("outerCase: ." + sourceColumn + ".");
		}
		
		String transformation = sourceColumn.substring(sourceColumn.indexOf("CASE"), sourceColumn.indexOf("THEN")).trim();
		System.out.println("transformation: ." + transformation + ".");
		String col1 = sourceColumn.substring(sourceColumn.indexOf("THEN") + 4, sourceColumn.indexOf("ELSE")).trim();
		System.out.println("col1: ." + col1 + ".");
		String col2 = sourceColumn.substring(sourceColumn.indexOf("ELSE") + 4, sourceColumn.indexOf("END")).trim();
		System.out.println("col2: ." + col2 + ".");

		if (col1.startsWith("COALESCE")){
			metaDataList = processCoalesceColumn(col1,targetColumn,sourceTableMap,metaDataList);
		}else if (col1.startsWith("INNER_CASE")){
			metaDataList = processCaseColumn(innerCase, targetColumn, sourceTableMap, metaDataList);
		}else{
			QueryMetaData qmd = createMetaDataRecord(col1, targetColumn, sourceTableMap, transformation + " TRUE");
			metaDataList.add(qmd);
		}
			
		if (col2.startsWith("COALESCE")){
			metaDataList = processCoalesceColumn(col2,targetColumn,sourceTableMap,metaDataList);
		}else if (col2.startsWith("INNER_CASE")){
			metaDataList = processCaseColumn(innerCase, targetColumn, sourceTableMap, metaDataList);
		}else{
			QueryMetaData qmd = createMetaDataRecord(col2, targetColumn, sourceTableMap, transformation + " FALSE");
			metaDataList.add(qmd);
		}
		
		return metaDataList;
	}
	
	public static QueryMetaData createMetaDataRecord(String sourceColumn, String targetColumn, HashMap<String, SourceTable> sourceTableMap, String colTransformation){
		System.out.println("In createMetaDataRecord");
		
		SourceTable sourceTableObj = null;
		String sourceSchema = null;
		String sourceTable = null;
		String transformation = null;
		
		if (sourceColumn.contains(".")){
			// If Column contains table ref, then use it to identify Source table details
			sourceTableObj 	= sourceTableMap.get(sourceColumn.substring(0, sourceColumn.indexOf(".")).trim() );
			sourceSchema 	= sourceTableObj.getSourceSchema();
			sourceTable 	= sourceTableObj.getSourceTable();
			sourceColumn 	= sourceColumn.substring(sourceColumn.indexOf(".")+1).trim();
			transformation  = sourceTableObj.getTransformation();

		} else if (sourceTableMap.size() == 1){
			// If Column contains just one table in query, then use it as Source table for all source columns
			Iterator<SourceTable> iter 	= sourceTableMap.values().iterator();
			sourceTableObj = iter.next();
			sourceSchema 	= sourceTableObj.getSourceSchema();
			sourceTable 	= sourceTableObj.getSourceTable();
			transformation  = sourceTableObj.getTransformation();
			
		} else {
			sourceSchema = "";			//"--No Mapping Available--";
			sourceTable = "";			//"--No Mapping Available--";
		}
		
		if (colTransformation != null){
			transformation = colTransformation;
		}
			
		System.out.println("sourceSchema: ." + sourceSchema + ".");
		System.out.println("sourceTable: ." + sourceTable + ".");
		System.out.println("sourceColumn: ." + sourceColumn + ".");
		System.out.println("transformation: ." + transformation + ".");
			
		QueryMetaData qmd = new QueryMetaData();
		qmd.setTargetColumn(targetColumn);
		qmd.setSourceSchema(sourceSchema);
		qmd.setSourceTable(sourceTable);
		qmd.setSourceColumn(sourceColumn);
		qmd.setTransformation(transformation);
		
		return qmd;
	}
	
	public static HashMap<String, SourceTable> processTableName(String fromClause){
		
		System.out.println("In processTableName(fromClause)");
		return processTableName(fromClause, null);
	}
	
	public static HashMap<String, SourceTable> processTableName(String fromClause, String tableAlias){
		
		System.out.println("In processTableName.fromClause: "+fromClause+"; tableAlias: "+tableAlias);
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
				if (transformation != null && sourceTableTxt.indexOf("ON") >= 0){
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
				if (tableAlias != null){
					sourceTableAlias = tableAlias;
				}
				sourceTableMap.put(sourceTableAlias, sourceTableObj);
			}
		}
		
		System.out.println("sourceTableMap: ." + sourceTableMap + ".");
		
		return sourceTableMap;
	}
	
	public static ArrayList<QueryMetaData> processUpdateQuery(Query updateQuery, ArrayList<QueryMetaData> metaDataList){
		System.out.println("In processUpdateQuery");
		
		String queryTxt = updateQuery.getQueryText();
		String targetTable = null;
		if (queryTxt.indexOf("FROM") > -1) 
			targetTable = queryTxt.substring(queryTxt.indexOf("UPDATE") + 6, queryTxt.indexOf("FROM"));
		else 
			targetTable = queryTxt.substring(queryTxt.indexOf("UPDATE") + 6, queryTxt.indexOf("SET"));
		
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
		
		String whereClause = queryTxt.substring(queryTxt.lastIndexOf("WHERE")).trim();
		System.out.println("whereClause: ." + whereClause + ".");
		
		String fromClause = queryTxt.substring(queryTxt.indexOf("FROM") + 4, queryTxt.indexOf("SET ")).trim();
		System.out.println("fromClause: ." + fromClause + ".");
		
		//Parsing fromClause
		HashMap<String, SourceTable> sourceTableMap = new HashMap<String, SourceTable>();
		HashMap<String, SourceTable> tempSourceTableMap = new HashMap<String, SourceTable>();
		ArrayList<String> fromClauseList = new ArrayList<String>(); 
		String tableAlias = null;
		for (int i=0; i<fromClause.length(); i++){
			if (fromClause.charAt(i)=='('){
				// Select subquery in From Clause
				int subQueryEndPt = QueryUtil.getClosingBraceIndex(fromClause, ')', i+1);
				System.out.println("subQueryEndPt: " + subQueryEndPt);
				fromClauseList.add(fromClause.substring(i+1, subQueryEndPt) );
				System.out.println("fromClause1: " + fromClause.substring(i+1, subQueryEndPt));
				if (fromClause.indexOf(',',subQueryEndPt) > -1){
					tableAlias = fromClause.substring(subQueryEndPt+1, fromClause.indexOf(',',subQueryEndPt)).trim();
					i = fromClause.indexOf(',',subQueryEndPt) + 1;	// +2 to take care of comma if present
				}else{
					tableAlias = fromClause.substring(subQueryEndPt+1).trim();
					i = fromClause.length();
				}
			} else {
				// Direct table name in From Clause
				int tableNameEndPt = fromClause.indexOf(',', i);
				System.out.println("tableNameEndPt: " + tableNameEndPt);
				if (tableNameEndPt > -1){
					fromClauseList.add(fromClause.substring(i, tableNameEndPt) );
					System.out.println("fromClause2: " + fromClause.substring(i, tableNameEndPt));
					i = tableNameEndPt + 2;
				} else {
					fromClauseList.add(fromClause.substring(i) );
					System.out.println("fromClause3: " + fromClause.substring(i));
					i = fromClause.length();
				}
			}
		}
		System.out.println("fromClauseList: ." + fromClauseList + ".");
		
		Iterator<String> iter = fromClauseList.iterator();
		while(iter.hasNext()){
			String fromTxt = iter.next().trim();
			System.out.println("fromTxt: ." + fromTxt + ".");
			if (fromTxt.startsWith("SELECT")){
				// Select subquery in From Clause
				System.out.println("fromClauseForProcess: ." + fromTxt.substring(fromTxt.indexOf("FROM")+4, fromTxt.indexOf("WHERE")).trim() + ".");
				tempSourceTableMap = processTableName(fromTxt.substring(fromTxt.indexOf("FROM")+4, fromTxt.indexOf("WHERE")).trim(), tableAlias);
			}else{
				// Direct table name in From Clause
				tempSourceTableMap = processTableName(fromTxt);
			}
			sourceTableMap.putAll(tempSourceTableMap);
		}
		System.out.println("sourceTableMap: ." + sourceTableMap + ".");
		
		String setClause = queryTxt.substring(queryTxt.indexOf("SET ") + 4, queryTxt.lastIndexOf("WHERE")).trim();
		String[] setClauseList = setClause.split(",");
		String[] targetColumnsArr = new String[setClauseList.length];
		String[] sourceColumnsArr = new String[setClauseList.length];
		for (int i=0; i<setClauseList.length; i++){
			targetColumnsArr[i] = setClauseList[i].substring(0, setClauseList[i].indexOf('=')).trim();
			sourceColumnsArr[i] = setClauseList[i].substring(setClauseList[i].indexOf('=')+1).trim();
			System.out.println("targetColumnsArr["+i+"]: ." + targetColumnsArr[i] + ".");
			System.out.println("sourceColumnsArr["+i+"]: ." + sourceColumnsArr[i] + ".");
			
			QueryMetaData qmd = createMetaDataRecord(sourceColumnsArr[i], targetColumnsArr[i], sourceTableMap, null);
			metaDataList.add(qmd);
		}
		
		
		/*String sourceTxt = queryTxt.substring(queryTxt.indexOf(")") + 1).trim();
		String sourceType = sourceTxt.substring(0, sourceTxt.indexOf(' ')).trim();
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
		} */
		
		// Add where clause in transformation section
		if (whereClause != null && !"".equals(whereClause)){
			QueryMetaData qmd = new QueryMetaData();
			qmd.setTransformation(whereClause);
			metaDataList.add(qmd);		
		}
				
		// The metadata from whereClause will not have the below generic data, so setting them finally 
		for (int i=0; i < metaDataList.size(); i++){
			QueryMetaData qmd = metaDataList.get(i);
			if (qmd.getQueryId() == null){
				qmd.setQueryId(updateQuery.getQueryId());
				qmd.setQueryType("UPDATE");
				qmd.setTargetSchema(targetSchema);
				qmd.setTargetTable(targetTable);
				metaDataList.set(i, qmd);
			}
		}
		
		return metaDataList;
	}
	
	public static ArrayList<QueryMetaData> processDeleteQuery(Query deleteQuery, ArrayList<QueryMetaData> metaDataList){
		System.out.println("In processDeleteQuery");
		
		String queryTxt = deleteQuery.getQueryText();
		String targetTable = null;
		
		if (queryTxt.indexOf("WHERE") > 0)
			targetTable = queryTxt.substring(queryTxt.indexOf("FROM") + 4, queryTxt.indexOf("WHERE"));
		else 
			targetTable = queryTxt.substring(queryTxt.indexOf("FROM") + 4);
			
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
		
		if (queryTxt.indexOf("WHERE") > -1){
			metaDataList = processDeleteColumns(queryTxt.substring(queryTxt.indexOf("WHERE") + 5).trim(), metaDataList);
		}
	
		// The metadata from whereClause will not have the below generic data, so setting them finally 
		for (int i=0; i < metaDataList.size(); i++){
			QueryMetaData qmd = metaDataList.get(i);
			if (qmd.getQueryId() == null){
				qmd.setQueryId(deleteQuery.getQueryId());
				qmd.setQueryType("DELETE");
				qmd.setTargetSchema(targetSchema);
				qmd.setTargetTable(targetTable);
				metaDataList.set(i, qmd);
			}
		}
		
		return metaDataList;
				
	}
	
	public static ArrayList<QueryMetaData> processDeleteColumns(String columnTxt, ArrayList<QueryMetaData> metaDataList){
		System.out.println("In processDeleteColumns: " + columnTxt);
		
		String targetColumn = null;
		String transformation = null;
		String sourceColumn = null;
		
		columnTxt = columnTxt.trim();
		targetColumn = QueryUtil.getToken(columnTxt, "COLUMN").trim();
		columnTxt = columnTxt.replaceFirst(targetColumn, "").trim();
		transformation = QueryUtil.getToken(columnTxt, "OPERATOR").trim();
		columnTxt = columnTxt.replaceFirst(transformation, "").trim();
		sourceColumn = QueryUtil.getToken(columnTxt, "VALUE").trim();
		columnTxt = columnTxt.replaceFirst(sourceColumn, "").trim();
		if (columnTxt.indexOf("AND") > -1)
			columnTxt = columnTxt.substring(columnTxt.indexOf("AND") + 3);
		else
			columnTxt = "";
		
		System.out.println("targetColumn: ." + targetColumn + ".");
		System.out.println("transformation: ." + transformation + ".");
		System.out.println("sourceColumn: ." + sourceColumn + ".");
		System.out.println("columnTxt: ." + columnTxt + ".");
		
		// Create Metadata
		if (sourceColumn.contains("SELECT")){
			sourceColumn = sourceColumn.substring(1, sourceColumn.length()-1);	// Removing the braces
			metaDataList = processSelectSubQuery(new String[]{targetColumn}, sourceColumn, metaDataList);
		}else{
			QueryMetaData qmd = new QueryMetaData();
			qmd.setTargetColumn(targetColumn);
			qmd.setSourceColumn(sourceColumn);
			qmd.setTransformation(transformation);
			metaDataList.add(qmd);
		}
	
		if (!"".equals(columnTxt)){
			metaDataList = processDeleteColumns(columnTxt, metaDataList);
		}
		return metaDataList;
	}
	
	public static void displayMetadata(ArrayList<QueryMetaData> queryMetaData){
		System.out.println("In displayMetadata: ");
		
		System.out.println("queryType \t queryId targetSchema \t targetTable \t targetColumn \t sourceSchema \t sourceTable \t sourceColumn \t transformation: " );
		for (int i=0; i < queryMetaData.size(); i++){
			System.out.println(queryMetaData.get(i));
		}
		
		ExcelProcessor.writeQueries(queryMetaData);
	}

}
