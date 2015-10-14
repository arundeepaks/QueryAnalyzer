package com.verizon.hackathon;

import java.util.HashMap;

public class QueryUtil {

	public static int getClosingBraceIndex(String str, char endBrace, int fromIndex){
		
		System.out.println("str: "+str+", endBrace: "+endBrace+", fromIndex: "+fromIndex);
		
		char startBrace = '(';
		int endBraceIndex = -1;
		int startBraceCnt = 0;
		
		HashMap<String,String> braceTypes = new HashMap<String,String>();
		braceTypes.put(")", "(");
		braceTypes.put("}", "{");
		braceTypes.put("]", "[");
		if (braceTypes.containsKey(String.valueOf(endBrace))){
			startBrace = braceTypes.get(String.valueOf(endBrace)).charAt(0);
			System.out.println("startBrace: "+startBrace);
		}
		
		for (int i=fromIndex; i<str.length(); i++){
			if (str.charAt(i) == endBrace){
				if (startBraceCnt == 0){
					System.out.println("Reached end brace at: "+i);
					endBraceIndex = i;
					break;					
				}
				startBraceCnt--;
				System.out.println("Reached end brace at: "+i+", startBraceCnt = "+startBraceCnt);
			}else if (str.charAt(i) == startBrace){
				startBraceCnt++;
				System.out.println("Reached start Brace at: "+i);
			}
		}
		return endBraceIndex;
	}
}
