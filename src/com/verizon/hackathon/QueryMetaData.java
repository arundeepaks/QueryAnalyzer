package com.verizon.hackathon;

public class QueryMetaData {
	private String queryType;
	private String queryId;
	private String targetSchema;
	private String targetTable;
	private String targetColumn;
	private String sourceSchema;
	private String sourceTable;
	private String sourceColumn;
	private String transformation;
	
	public String getQueryType() {
		return queryType;
	}
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}
	public String getQueryId() {
		return queryId;
	}
	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}
	public String getTargetSchema() {
		return targetSchema;
	}
	public void setTargetSchema(String targetSchema) {
		this.targetSchema = targetSchema;
	}
	public String getTargetTable() {
		return targetTable;
	}
	public void setTargetTable(String targetTable) {
		this.targetTable = targetTable;
	}
	public String getTargetColumn() {
		return targetColumn;
	}
	public void setTargetColumn(String targetColumn) {
		this.targetColumn = targetColumn;
	}
	public String getSourceSchema() {
		return sourceSchema;
	}
	public void setSourceSchema(String sourceSchema) {
		this.sourceSchema = sourceSchema;
	}
	public String getSourceTable() {
		return sourceTable;
	}
	public void setSourceTable(String sourceTable) {
		this.sourceTable = sourceTable;
	}
	public String getSourceColumn() {
		return sourceColumn;
	}
	public void setSourceColumn(String sourceColumn) {
		this.sourceColumn = sourceColumn;
	}
	public String getTransformation() {
		return transformation;
	}
	public void setTransformation(String transformation) {
		this.transformation = transformation;
	}
	
	public String toString(){
		/*String returnStr = "queryType: " + queryType + "; queryId: " + queryId + "; targetSchema: " + targetSchema  + "; targetTable: " + targetTable + "; targetColumn: " + targetColumn 
				+ "; sourceSchema: " + sourceSchema  + "; sourceTable: " + sourceTable + "; sourceColumn: " + sourceColumn  + "; transformation: " + transformation;*/
		String returnStr = queryType + " \t\t " + queryId + " \t " + targetSchema  + " \t\t " + targetTable + " \t\t " 
				+ (targetColumn==null?"":targetColumn) + " \t\t " 
				+ (sourceSchema==null?"":sourceSchema)  + " \t\t " 
				+ (sourceTable==null?"":sourceTable) + " \t\t " 
				+ (sourceColumn==null?"":sourceColumn)  + " \t\t " 
				+ (transformation==null?"NA":transformation);
		return returnStr;
	}
}
