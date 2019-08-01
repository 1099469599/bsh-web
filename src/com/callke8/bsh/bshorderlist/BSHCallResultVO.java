package com.callke8.bsh.bshorderlist;

import java.util.Date;

import com.callke8.bsh.bshcallparam.BSHCallParamConfig;
import com.callke8.utils.DateFormatUtils;
import com.callke8.utils.Md5Utils;

/**
 *
 * 呼叫结果反馈实体类,用于在呼结束后，将结果反馈给BSH服务器
 * 
 *  参数	说明
 *  
	orderId	订单号id
	callType	外呼类型0.二次未接通1.一次接通/二次接通2放弃呼叫3已过期
	time	时间（yyyyMMddHHmmss）
	sign	签名（全小写）= md5(time + orderId+ key)key为约定好的密钥
	preCallResult 前置外呼结果，0：没有前置; 1：确认; 2：不确认; 3：未接听;
	callResult	外呼结果 1：确认建单   2 暂不安装  3 短信确认   4 工程师电话确认 5 错误回复 ;6:放弃呼叫 ;7:已过期 ;8:外呼失败;9：无回复;10:环境不具备;
 * 
 * @author 黄文周
 *
 */
public class BSHCallResultVO {

	private String orderId;
	
	private String callType;
	
	private String time;
	
	private String sign;
	
	private String preCallResult;
	
	private String callResult;
	
	public BSHCallResultVO() {
		
	}
	
	/**
	 * 构造函数
	 * 
	 * @param orderId
	 * 			订单ID
	 * @param callType
	 * 			外呼类型0.二次未接通1.一次接通/二次接通2放弃呼叫3已过期
	 * @param callResult
	 * 			外呼结果 1：确认建单   2 暂不安装  3 短信确认   4 工程师电话确认 5 错误回复 ;6:放弃呼叫 ;7:已过期 ;8:外呼失败;9：无回复;10:环境不具备;
	 * @param bshCallResultKey
	 * 			呼叫结果反馈密钥
	 */
	public BSHCallResultVO(String orderId,String callType,String callResult,String bshCallResultKey) {
		
		this.orderId = orderId;
		this.callType = callType;
		this.time = DateFormatUtils.formatDateTime(new Date(), "yyyyMMddHHmmss");
		this.sign = Md5Utils.Md5(this.time + this.orderId + bshCallResultKey);
		this.callResult = callResult;
		
	}
	
	/**
	 * 构造函数
	 * 
	 * @param orderId
	 * 			订单ID
	 * @param callType
	 * 			外呼类型0.二次未接通1.一次接通/二次接通2放弃呼叫3已过期
	 * @param preCallResult 前置外呼结果，0：没有前置; 1：确认; 2：不确认; 3：未接听;
	 * @param callResult
	 * 			外呼结果 1：确认建单   2 暂不安装  3 短信确认   4 工程师电话确认 5 错误回复 ;6:放弃呼叫 ;7:已过期 ;8:外呼失败;9：无回复;10:环境不具备;
	 * @param bshCallResultKey
	 * 			呼叫结果反馈密钥
	 */
	public BSHCallResultVO(String orderId,String callType,String preCallResult,String callResult,String bshCallResultKey) {
		
		this.orderId = orderId;
		this.callType = callType;
		this.time = DateFormatUtils.formatDateTime(new Date(), "yyyyMMddHHmmss");
		this.sign = Md5Utils.Md5(this.time + this.orderId + bshCallResultKey);
		this.preCallResult = preCallResult;
		this.callResult = callResult;
		
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getCallType() {
		return callType;
	}

	public void setCallType(String callType) {
		this.callType = callType;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}
	
	public String getPreCallResult() {
		return preCallResult;
	}

	public void setPreCallResult(String preCallResult) {
		this.preCallResult = preCallResult;
	}

	public String getCallResult() {
		return callResult;
	}

	public void setCallResult(String callResult) {
		this.callResult = callResult;
	}
	
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("{");
		sb.append("\"orderId\":" + getOrderId() + ",");
		sb.append("\"callType\":\"" + getCallType() + "\",");
		sb.append("\"time\":\"" + getTime() + "\",");
		sb.append("\"sign\":\"" + getSign() + "\",");
		sb.append("\"preCallResult\":\"" + getPreCallResult() + "\",");
		sb.append("\"callResult\":\"" + getCallResult() + "\"");
		sb.append("}");
		
		return sb.toString();
	}
	
	
}
