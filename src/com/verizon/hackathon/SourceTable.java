package com.verizon.hackathon;

public class SourceTable {
	private String sourceSchema;
	private String sourceTable;
	private String transformation;
	
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
	public String getTransformation() {
		return transformation;
	}
	public void setTransformation(String transformation) {
		this.transformation = transformation;
	}
	public String toString(){
		return("sourceSchema: " + sourceSchema + "; sourceTable: " + sourceTable + "; transformation: " + transformation);
	}
}
