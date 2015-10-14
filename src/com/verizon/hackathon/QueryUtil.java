package com.verizon.hackathon;

import java.util.HashMap;

public class QueryUtil {

	public static int getClosingBraceIndex(String str, char endBrace, int fromIndex){
		
		System.out.println("str: "+str+", endBrace: "+endBrace+", fromIndex: "+fromIndex);
		HashMap<String,String> braceTypes = new HashMap<String,String>();
		braceTypes.put(")", "(");
		braceTypes.put("}", "{");
		braceTypes.put("]", "[");
		
		int endBraceIndex = -1;
		int nextStartBraceIndex = -1;
		char startBrace;
		
		endBraceIndex = str.indexOf(endBrace, fromIndex);
		System.out.println("endBraceIndex: "+endBraceIndex);
		
		if (braceTypes.containsKey(String.valueOf(endBrace))){
			System.out.println("Checking startBrace ");
			startBrace = braceTypes.get(String.valueOf(endBrace)).charAt(0);
			nextStartBraceIndex = str.indexOf(startBrace, fromIndex);
			System.out.println("startBrace: "+startBrace);
			System.out.println("nextStartBraceIndex: "+nextStartBraceIndex);
		}
				
		if (endBraceIndex != -1 && nextStartBraceIndex > -1 && endBraceIndex > nextStartBraceIndex){
			System.out.println("Recursive call");
			return getClosingBraceIndex(str, endBrace, endBraceIndex+1);
		}
		
		return endBraceIndex;
	}
}
