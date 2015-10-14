package com.verizon.hackathon;

public class Test {

	public static void main(String[] args) {
		testUtil();
	}
	
	public static void testStringBuffer() {
		StringBuffer sb = new StringBuffer(null);
		System.out.println("sb: "+sb);
	}
	
	public static void testUtil(){
		System.out.println(QueryUtil.getClosingBraceIndex("a(hdb(bsdb(snsbdb)bn(cb)bb)hhh)ss", ')', 2));
	}

}
