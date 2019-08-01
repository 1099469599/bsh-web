package com.callke8.bsh.bshorderlist;

import net.sf.json.JSONObject;

public class TestDemo {

	public static void main(String[] args) {
		
		//BSHCallResultVO callResult = new BSHCallResultVO("123456", "1", "1","");
		
		//System.out.println("callResult: " + callResult);
		
		BSHCallResultVO cr = new BSHCallResultVO();
		
		cr.setOrderId("23432432432423");
		cr.setCallType("1");
		cr.setTime("20190503180900");
		cr.setSign("dsafdsafdsafds");
		cr.setCallResult("9");
		
		JSONObject json = JSONObject.fromObject(cr);
		
		System.out.println(json.toString());
		
		
	}

}
