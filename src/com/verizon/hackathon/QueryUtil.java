package com.verizon.hackathon;

import java.util.HashMap;

public class QueryUtil {

	public static int getClosingBraceIndex(String str, char endBrace, int fromIndex){
		
		System.out.println("getClosingBraceIndex.str: "+str+", endBrace: "+endBrace+", fromIndex: "+fromIndex);
		
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
	
	public static String getToken(String str, String tokenType){
		
		System.out.println("getToken.str: "+str);
		
		String token = null;
		
		if ("COLUMN".equals(tokenType)){
			// TO DO: Check for braces in future
			for (int i=0; i<str.length(); i++){
				if ( !isCharLabel(str.charAt(i)) ){
					token = str.substring(0,i);
					break;
				}
			}
		}else if ("OPERATOR".equals(tokenType)){
			
			if (str.startsWith("IS NOT")){
				token = str.substring(0,6);
			}else if (str.startsWith("IS")){
				token = str.substring(0,2);
			}else if (str.startsWith("IN")){
				token = str.substring(0,2);
			}else{
				for (int i=0; i<str.length(); i++){
					char ch = str.charAt(i);
					if ( isCharLabel(ch) || isCharSpace(ch) || isCharBrace(ch) || ((int)ch == 60)){
						// TO DO: Hardcoding 60 for '<' as the label is starting with <. Need to identify a better logic
						token = str.substring(0,i);
						break;
					}
				}
			}
		}else if ("VALUE".equals(tokenType)){
			str = str.trim();
			for (int i=0; i<str.length(); i++){
				char ch = str.charAt(i);
				if (isCharBrace(ch)){
					token = str.substring(0,getClosingBraceIndex(str,')',2) + 1);
					break;
				}else{
					if (str.contains("AND")){
						token = str.substring(0,str.indexOf("AND"));
					}else{
						token = str;
					}
				}
			}
		}
		
		System.out.println("token: "+token);
		return token;
	}
	
	public static boolean isCharLabel(char ch){
		boolean flag = false;
		int ascii = ch;
		if ( (ascii >= 65 && ascii <= 90) || (ascii >= 97 && ascii <= 122) || (ascii == 95) || (ascii == 46)){
			flag = true;
		}
		return flag;
	}
	
	public static boolean isCharSpace(char ch){
		boolean flag = false;
		int ascii = ch;
		if ( ascii == 32 ){
			flag = true;
		}
		return flag;
	}
	
	public static boolean isCharBrace(char ch){
		boolean flag = false;
		int ascii = ch;
		if ( ascii == 40 ){
			flag = true;
		}
		return flag;
	}
}
