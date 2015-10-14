package com.verizon.hackathon;

public class Query {
	private String queryId;
	private String queryText;
	
	public String getQueryId() {
		return queryId;
	}
	public void setQueryId(String queryId) {
		this.queryId = queryId;
	}
	public String getQueryText() {
		return queryText;
	}
	public void setQueryText(String queryText) {
		this.queryText = queryText;
	}
	
	public String toString(){
		return("queryId: " + queryId + "; queryText: " + queryText);
	}
}
