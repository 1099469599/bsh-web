import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;

import com.callke8.bsh.bshcallparam.BSHCallParamConfig;
import com.callke8.bsh.bshorderlist.BSHHttpRequestThread;
import com.callke8.bsh.bshorderlist.BSHOrderList;
import com.callke8.bsh.bshvoice.BSHVoice;
import com.callke8.bsh.bshvoice.BSHVoiceConfig;
import com.callke8.predialqueuforbsh.BSHLaunchDialService;
import com.callke8.pridialqueueforbshbyquartz.BSHPredial;
import com.callke8.utils.BlankUtils;
import com.callke8.utils.DateFormatUtils;
import com.callke8.utils.StringUtil;
import com.jfinal.plugin.activerecord.Record;

public class BSHCallFlowAgi_bak20190829 extends BaseAgiScript {

	private Log log = LogFactory.getLog(BSHCallFlowAgi_bak20190829.class);
	
	@Override
	public void service(AgiRequest request, AgiChannel channel) {
		try {
			
			channel.exec("Noop","执行到了 BSHCallFlowAgi-----");
			//执行到这里之后，由于 Avaya 传过来的状态有可能出现这种情况：通道已接听，但是在 BSHLaunchDialService 的返回的呼叫状态结果中可能还是出现了 NOANSWER 状态
			
			
			String bshOrderListId = channel.getVariable("bshOrderListId");
			StringUtil.writeString("/opt/dial-log.log",DateFormatUtils.getCurrentDate() + ",/(流程执行):bshOrderListId " + bshOrderListId + ",通道标识:" + channel.getName(), true);
			
			//List<Record> playList = new ArrayList<Record>();		 		//定义一个List，用于储存插入列表 
			
			StringBuilder readFileSb = new StringBuilder();				    //定义一个用于储存read命令所需语音文件字符串
			
			List<Record> respond1PlayList = new ArrayList<Record>();        //定义一个当回复1，即是确认安装后播放的语音列表
			List<Record> respond2PlayList = new ArrayList<Record>();        //定义一个当回复2，即是暂不安装后播放的语音列表
			List<Record> respond3PlayList = new ArrayList<Record>();        //定义一个当回复3，即是延后安装后播放的语音列表
			List<Record> respond4PlayList = new ArrayList<Record>();        //定义一个当回复4，即是已经预约后播放的语音列表
			List<Record> respondErrorPlayList = new ArrayList<Record>();        //定义一个当无回复或是错误回复，播放的语音列表
			
			exec("Noop","bshOrderListId-----" + bshOrderListId);
			//从数据表中取出订单信息
			BSHOrderList bshOrderList = BSHOrderList.dao.getBSHOrderListById(bshOrderListId);
			//System.out.println("取出的订单信息为---=========：" + bshOrderList);
			exec("Noop","AGI流程得到的订单ID为：" + bshOrderListId + ",订单详情：" + bshOrderList);
			//StringUtil.writeString("/opt/dial-log.log",DateFormatUtils.getCurrentDate() + ",FastAGI11111(流程执行)：" + bshOrderList.getStr("CUSTOMER_TEL") + ",通道标识:" + channel.getName(), true);
			
			/**
			 * 2018-09-06 强行加入逻辑，用于处理，提交上来的订单，如果平台渠道为非国美平台，但是日期类型却又是送货日期时，需要强行将送货日期改为安装日期
			 */
			int channelSource = bshOrderList.getInt("CHANNEL_SOURCE");           //购物平台，1：京东；2：苏宁；3：天猫；4：国美
			int timeType = bshOrderList.getInt("TIME_TYPE");                     //日期类型，1：安装日期；2：送货日期
			if(timeType==2) {    //即等于2，送货日期时
				if(channelSource!=4) {  //而购物平台却又非国美时（因为只有国美是送货日期类型）
					bshOrderList.set("TIME_TYPE", 1);
				}
			}
			
			//20190522新增加,主要是用于判断是否带有前置流程
			int productNameValue = bshOrderList.getInt("PRODUCT_NAME");    //取出产品类目
			int isConfirm = bshOrderList.getInt("IS_CONFIRM");             //取出是否带有前置流程，1：带前置流程；2：不带前置流程
			if(isConfirm == 1) {    //如果 isConfirm 为1，就表示带有前置流程
				//即使这里标识带有前置流程，也要直接执行，还要再判断产品类目是否为： 6（灶具）或 8（洗碗机），只有这两个类目，才需要进入前置流程
				if(productNameValue==6 || productNameValue==8) {   
					
					exec("Noop","客户 " + bshOrderList.getStr("CUSTOMER_TEL") + ",正准备执行前置流程...");
					StringUtil.log(this, "客户 " + bshOrderList.getStr("CUSTOMER_TEL") + ",正准备执行前置流程...");
					
					//进入前置流程执行
					String prefixCallFlowRespond = execPrefixCallFlow(bshOrderList,channel);     //执行前置流程，并返回客户回复按键
					if(!prefixCallFlowRespond.equals("1")) {     //如果客户回复的按键不为1，则表示安装环境不具备,需要提前返回该通话，并将客户外呼结果为成功，客户回复状态为10，即是环境不具备
						
						/*
						 * 如果客户有前置流程中回复的结果值不为1时，有三种情况 
							(1)回复值为2，即是环境不具备，直接将外呼结果返回即可
							(2)错误的按键值
							(3)无回复值
						   不过可能将这三种情况，归为两类：
						   (1)回复值为2
						   (2)错误回复或是无回复
						  
						     针对这两类回复处理如下
						    (1)回复值为2：直接将外呼结果存入订单记录表，并将外呼结果返回给DOB接口
						    		  存入订单记录表数据为：
						    		  	  respond=10 即是环境不具备
						    		  	  keyValue=2 直接反馈按键结果
						    		 返回给DOB的数据如下：
						    		 	　　orderId 订单编号
						    		 	　　callType １（一次或两次呼通）
						    		 	　　preCallResult 前置外呼结果，2：不确认;
						    		 	　　callResult　10(环境不具备)
						    (2)错误回复或是无回复： 
						    	分两种情况 ，当前的外呼是第一次还是第二次
						    		如果是第一次呼叫，则返回数据到订单列表，并将订单记录设置为待重呼
						    			   返回订单记录数据为：
						    			   respond=10 即环境不具备 
						    			   keyValue=(错误回复就返回直接按键值，如果是无回复就返回无回复)
						    		如果是第二次呼叫,则返回返回数据到订单列表，并将订单记录设置为外呼成功
						    
						*/
					
						
						//如果客户返回的按键为2，即是环境不具备时，不再往下执行。
						//在返回之前，保存外呼的结果
						String keyValue = prefixCallFlowRespond;
						if(BlankUtils.isBlank(prefixCallFlowRespond)) {
							keyValue = "无回复";
						}
						
						//无论客户回复的按键是什么，都需要返回外呼结果给订单列表
						BSHOrderList.dao.updateBSHOrderListRespondAndBillsec(bshOrderList.get("ID").toString(), "10", Integer.valueOf(channel.getVariable("CDR(billsec)")), keyValue);
						
						if(keyValue.equals("2")) {      //如果客户的回复结果为2，即是环境不具备，将外呼结果返回订单列表，将外呼结果返回给DOB接口
							
							//在返回外呼结果给DOB服务器时，还需要加入一个前置流程的外呼结果
							//前置外呼结果，0：没有前置; 1：确认; 2：不确认; 3：未接听;
							//由于这里外呼失败，所以前置外呼结果只能是：2：不确认;
							String preCallResult = "2";
		
							BSHHttpRequestThread httpRequestT = new BSHHttpRequestThread(bshOrderList.get("ID").toString(),bshOrderList.getStr("ORDER_ID"), "1",preCallResult,String.valueOf(10),"1");
							//BSHHttpRequestThread httpRequestT = new BSHHttpRequestThread(bshOrderList.get("ID").toString(),bshOrderList.getStr("ORDER_ID"), "1",String.valueOf(10));
							Thread httpRequestThread = new Thread(httpRequestT);
							httpRequestThread.start();
							
							//无论是否回复什么结果，或是没有回复结果,在这里表示外呼已经结束，需要将活跃通道减掉一个
							if(BSHPredial.activeChannelCount > 0) {        
								BSHPredial.activeChannelCount--;
							}
							
							return;	
							
						}else {				//回复错误或是无回复时
							
							//判断当前的外呼是否为第一次外呼
							int retried = bshOrderList.getInt("RETRIED");
							
							if(retried < BSHCallParamConfig.getRetryTimes()) {      //如果已重试次数小于限定的重试次数时,设置为重呼
								
								//暂不将外呼结果返回给DOB系统，而是将该记录设置为待重呼
								//设置当前号码的状态为重试状态
								BSHOrderList.dao.updateBSHOrderListStateToRetry(bshOrderList.getInt("ID"), "3", BSHCallParamConfig.getRetryInterval(), "前置流程错误回复或无回复");
								
							} else {                                               //如果重试次数已经达到最大的次数之后
								
								//将处呼结果返回给
								//在返回外呼结果给DOB服务器时，还需要加入一个前置流程的外呼结果
								//前置外呼结果，0：没有前置; 1：确认; 2：不确认; 3：未接听;
								//由于这里外呼失败，所以前置外呼结果只能是：2：不确认;
								String preCallResult = "2";
								
								String callResultValue = "5";      //我们重新定义这个外呼结果的值，默认设置为 5，即是错误回复；如果客户是无回复时，再将这个值设置为9，即是无回复
								if(keyValue.equals("无回复")) {
									callResultValue = "9";
								}
			
								BSHHttpRequestThread httpRequestT = new BSHHttpRequestThread(bshOrderList.get("ID").toString(),bshOrderList.getStr("ORDER_ID"), "1",preCallResult,callResultValue,"1");
								//BSHHttpRequestThread httpRequestT = new BSHHttpRequestThread(bshOrderList.get("ID").toString(),bshOrderList.getStr("ORDER_ID"), "1",String.valueOf(10));
								Thread httpRequestThread = new Thread(httpRequestT);
								httpRequestThread.start();
							}
							
							//无论是否回复什么结果，或是没有回复结果,在这里表示外呼已经结束，需要将活跃通道减掉一个
							if(BSHPredial.activeChannelCount > 0) {        
								BSHPredial.activeChannelCount--;
							}
							
							return;
							
						}

					}
				}
			}
			
			//20190522新增加结束
			
			//playList = getPlayList(bshOrderList);     //组织播放开始语音
			String readVoiceFileList = getReadVoiceFileToString(bshOrderList);   //Read应用所需语音文件
			
			respond1PlayList = getRespond1PlayList(bshOrderList);
			respond2PlayList = getRespond2PlayList(bshOrderList);
			respond3PlayList = getRespond3PlayList(bshOrderList);
			respond4PlayList = getRespond4PlayList(bshOrderList);
			respondErrorPlayList = getRespondErrorPlayList(bshOrderList);
				
			//如果开始播放列表不为空时
			if(!BlankUtils.isBlank(readVoiceFileList)) {
				
				exec("Noop","Read播放文件列表内容:" + readVoiceFileList);
				exec("Wait","1");
				
				execRead(readVoiceFileList,respond1PlayList, respond2PlayList, respond3PlayList, respond4PlayList,respondErrorPlayList,bshOrderList, channel);     //执行调查操作
				
				StringUtil.writeString("/opt/dial-log.log",DateFormatUtils.getCurrentDate() + ",FastAGI22222(流程执行结束)：" + bshOrderList.getStr("CUSTOMER_TEL") + ",通道标识:" + channel.getName(), true);
				
			}else {
				exec("Noop","Read播放文件列表内容为空!系统无法调查客户!");
			}
		}catch(AgiException agiE) {   //如果出现问题，则需要将号码置为失败或是重试
			agiE.printStackTrace();
		}
		
	}
	
	/**
	 * 执行前置流程
	 * 
	 * @param bshOrderList
	 * 				传入的订单数据
	 */
	public String execPrefixCallFlow(BSHOrderList bshOrderList,AgiChannel channel) {
		
		String prefixCallFlowRespond = "";
		
		try {
			String voicePath = BSHCallParamConfig.getVoicePathSingle();   //取出配置的语音文件（单声道）路径
			
			//根据订单信息，查询前置流程的开场语音
			String prefixCallFlowReadVoiceFile = getPrefixCallFlowReadVoiceFileToString(bshOrderList); 
			
			//返回前置流程执行时，如果客户回复了2，即表示环境不具备，需要播放指定的语音文件，在这里先查询出来
			String prefixCallFLowRespond2VoiceFile = getPrefixCallFlowRespond2VoiceFile(bshOrderList);
			
			exec("Read","prefixCallFlowRespond," + prefixCallFlowReadVoiceFile + ",1,,1,8");
			
			//取得客户回复结果
			prefixCallFlowRespond = channel.getVariable("prefixCallFlowRespond");
			StringUtil.log(BSHCallFlowAgi_bak20190829.class, "客户 " + bshOrderList.get("CUSTOMER_TEL") + " 在前置流程,第1次回复输入：" + prefixCallFlowRespond);
			//如果第一次回复为空，或是不为1 或 2 时，再播放一次
			if(BlankUtils.isBlank(prefixCallFlowRespond) || !(prefixCallFlowRespond.equalsIgnoreCase("1") || prefixCallFlowRespond.equalsIgnoreCase("2"))) {
				
				//if(!BlankUtils.isBlank(prefixCallFlowRespond)) {    //如果客户回复不为空，但是按键又不为  1，2，3，4 时
				String inputErrorVoice = voicePath + "/" + BSHVoiceConfig.getVoiceMap().get("response_error_for_first_time");
				exec("PlayBack",inputErrorVoice);         //提示输入有误
				//}
				
				//进入第二次
				exec("Read","prefixCallFlowRespond," + prefixCallFlowReadVoiceFile + ",1,,1,8");
				prefixCallFlowRespond = channel.getVariable("prefixCallFlowRespond");
				StringUtil.log(BSHCallFlowAgi_bak20190829.class, "客户 " + bshOrderList.get("CUSTOMER_TEL") + " 在前置流程,第2次回复输入：" + prefixCallFlowRespond);
			}
			
			//如果两次客户都没有输入，或是输入不为1，则强制设置为输入2
			//if(BlankUtils.isBlank(prefixCallFlowRespond) || !prefixCallFlowRespond.equals("1")) {
			//	prefixCallFlowRespond = "2";
			//}
			
			if(prefixCallFlowRespond.equals("1")) {        //如果客户输入1，系统将进入下一个流程
				exec("Noop","客户 " + bshOrderList.get("CUSTOMER_TEL") + " 执行前置流程，客户回复了1，表示安装环境已具备！");
				//暂不做任何操作
				//理论上需要播放如下提示：安装环境已具备，系统将进入安装日期确认流程，请稍候...
			}else {										   //如果客户输入2，系统将结束通话，不再进入下一个流程
				
				/**
				 * 客户环境不具备时，主要包括以下两条语音内容：
				 * 		回复2语音:（灶具类目前置流程回复2）
				 * 			prefix_productName_6_repond_2：
				 * 				为了避免漏气，安装灶具时必须要做漏气检测，通常要先开通气源，再上门安装。灶具面板上有一个二维码，气源开通后，请您扫码预约安装服务，非常抱歉给您带去不便，感谢您的配合，再见。
				 * 		回复2语音:（洗碗机类目前置流程回复2 ）
				 * 			prefix_productName_8_repond_2：
				 * 				为了一次上门就能装好，需要您准备好门板之后再预约安装，洗碗机门体内侧有一个二维码，请您扫码预约安装服务，还可以方便的获取使用指南，非常抱歉给您带去不便，感谢您的配合，再见
				 */
				
				if(!BlankUtils.isBlank(prefixCallFlowRespond) && prefixCallFlowRespond.equals("2")) {
					exec("Noop","客户 " + bshOrderList.get("CUSTOMER_TEL") + " 执行前置流程，客户回复按键2，表示安装环境不具备,将不再执行安装确认流程！");
					exec("PlayBack",prefixCallFLowRespond2VoiceFile);    
				}else {    //客户回复不为2时，则提示：对不起，输入有误，我们可能会再次与您联系，再见！
					String responseErrorVoice = voicePath + "/" + BSHVoiceConfig.getVoiceMap().get("respond_error");
					
					exec("Noop","客户 " + bshOrderList.get("CUSTOMER_TEL") + " 执行前置流程，客户无回复、错误回复,系统将播放：对不起，输入有误，我们可能会再次与您联系，再见！");
					exec("PlayBack",responseErrorVoice); 
				}
			}
		
		} catch (AgiException e) {
			e.printStackTrace();
		}
		
		return prefixCallFlowRespond;
	}
	
	public void execRead(String readVoiceFileList,List<Record> respond1PlayList,List<Record> respond2PlayList,List<Record> respond3PlayList,List<Record> respond4PlayList,List<Record> respondErrorPlayList,BSHOrderList bshOrderList,AgiChannel channel) {
		
		try {
			
			String voicePath = BSHCallParamConfig.getVoicePathSingle();   //取出配置的语音文件（单声道）路径
			
			exec("Read","respond," + readVoiceFileList + ",1,,1,8");
			
			String respond = channel.getVariable("respond");     //取得回复结果
			StringUtil.log(BSHCallFlowAgi_bak20190829.class, "客户 " + bshOrderList.get("CUSTOMER_TEL") + " 第1次回复输入：" + respond);
			
			//一共要求两次，如果客户第一次回复为空或是错误回复时，再执行一次。
			if(BlankUtils.isBlank(respond) || !(respond.equalsIgnoreCase("1") || respond.equalsIgnoreCase("2") || respond.equalsIgnoreCase("3") || respond.equalsIgnoreCase("4"))) {
				
				if(!BlankUtils.isBlank(respond)) {    //如果客户回复不为空，但是按键又不为  1，2，3，4 时
					String inputErrorVoice = voicePath + "/" + BSHVoiceConfig.getVoiceMap().get("response_error_for_first_time");
					exec("PlayBack",inputErrorVoice);         //提示输入有误
				}
				
				exec("Read","respond," + readVoiceFileList + ",1,,1,8");
				respond = channel.getVariable("respond");     //再次取得回复结果
				StringUtil.log(BSHCallFlowAgi_bak20190829.class, "客户 " + bshOrderList.get("CUSTOMER_TEL") + " 第2次回复输入：" + respond);
			}
			
			String keyValue = null;    //按键值
			if(!BlankUtils.isBlank(respond)) {    
				keyValue = respond;
			}else {
				keyValue = "无回复";
			}
			if(!BlankUtils.isBlank(respond)) {      //客户回复不为空时
				
				if(respond.equalsIgnoreCase("1")) {     		//如果回复的是1时,确认安装
					
					exec("Noop","客户" + bshOrderList.get("CUSTOMER_TEL") + "回复1,即确认安装");
					//更改客户回复的同时，将呼叫状态更改为2，即是外呼成功
					//BSHOrderList.dao.updateBSHOrderListRespondAndBillsec(bshOrderList.get("ID").toString(), respond,Integer.valueOf(channel.getVariable("CDR(billsec)")));
					
					execPlayBack(respond1PlayList);     //回复后，还需要将结果播放回去
				}else if(respond.equalsIgnoreCase("2")) {		//如果回复的是2时，暂不安装
					
					exec("Noop","客户" + bshOrderList.get("CUSTOMER_TEL") + "回复2,即暂不安装");
					//更改客户回复的同时，将呼叫状态更改为2，即是外呼成功
					//BSHOrderList.dao.updateBSHOrderListRespondAndBillsec(bshOrderList.get("ID").toString(), respond,Integer.valueOf(channel.getVariable("CDR(billsec)")));
				
					execPlayBack(respond2PlayList);     //回复后，还需要将结果播放回去
					
				}else if(respond.equalsIgnoreCase("3")) {       //如果回复的是3时，延后安装
					exec("Noop","客户" + bshOrderList.get("CUSTOMER_TEL") + "回复3,即延后安装");
					//更改客户回复的同时，将呼叫状态更改为2，即是外呼成功
					//BSHOrderList.dao.updateBSHOrderListRespondAndBillsec(bshOrderList.get("ID").toString(), respond,Integer.valueOf(channel.getVariable("CDR(billsec)")));
					
					execPlayBack(respond3PlayList);     //回复后，还需要将结果播放回去
					
				}else if(respond.equalsIgnoreCase("4")) {       //如果回复的是4时,表示已预约
					exec("Noop","客户" + bshOrderList.get("CUSTOMER_TEL") + "回复4,即已经预约");
					execPlayBack(respond4PlayList);     //回复后，还需要将结果播放回去
				}else {                       //如果回复的是其他按键时,按回复
					exec("Noop","客户 " + bshOrderList.get("CUSTOMER_TEL") + "回复" + respond + ",即为错误回复");
					respond = "5";            //强制为错误回复
					//更改客户回复的同时，将呼叫状态更改为2，即是外呼成功
					//BSHOrderList.dao.updateBSHOrderListRespondAndBillsec(bshOrderList.get("ID").toString(), respond,Integer.valueOf(channel.getVariable("CDR(billsec)")));
					
					execPlayBack(respondErrorPlayList);     //回复后，还需要将结果播放回去
				}
				
			}else {
				exec("Noop","客户" + bshOrderList.get("CUSTOMER_TEL") + "无回复任何");
				respond = "9";
				//respond = "5";
				
				execPlayBack(respondErrorPlayList);     //回复后，还需要将结果播放回去
			}
			
			BSHOrderList.dao.updateBSHOrderListRespondAndBillsec(bshOrderList.get("ID").toString(), respond,Integer.valueOf(channel.getVariable("CDR(billsec)")),keyValue);
			
			//需要将客户回复结果返回给BSH服务器
			//同时，将呼叫成功结果反馈给 BSH 服务器
			
			//在返回外呼结果给DOB服务器时，还需要加入一个前置流程的外呼结果
			//前置外呼结果，0：没有前置; 1：确认; 2：不确认; 3：未接听;
			//由于这里外呼失败，所以前置外呼结果只能是： 0 （没有前置）或是1(确认);
			String preCallResult = "0";
			int isConfirm = bshOrderList.getInt("IS_CONFIRM");
			int productName = bshOrderList.getInt("PRODUCT_NAME");
			if(isConfirm==1 && (productName==6 || productName==8)) {
				//如果 isConfirm==1 且 产品类目为 6（灶具）或是 8（洗碗机）时，表示有前置流程
				preCallResult = "1";
			}
			
			BSHHttpRequestThread httpRequestT = new BSHHttpRequestThread(bshOrderList.get("ID").toString(),bshOrderList.getStr("ORDER_ID"), "1", preCallResult,String.valueOf(respond),"1");
			//BSHHttpRequestThread httpRequestT = new BSHHttpRequestThread(bshOrderList.get("ID").toString(),bshOrderList.getStr("ORDER_ID"), "1",String.valueOf(respond));
			Thread httpRequestThread = new Thread(httpRequestT);
			httpRequestThread.start();
			
			//无论是否回复什么结果，或是没有回复结果,在这里表示外呼已经结束，需要将活跃通道减掉一个
			if(BSHPredial.activeChannelCount > 0) {        
				BSHPredial.activeChannelCount--;
			}
			
		} catch (AgiException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 执行播放
	 * 
	 * @param playList
	 * @param bshOrderListId
	 * @param channel
	 */
	public void execPlay(List<Record> playList,List<Record> respond1PlayList,List<Record> respond2PlayList,List<Record> respond3PlayList,List<Record> respond4PlayList,BSHOrderList bshOrderList,AgiChannel channel) {
		
		//如果插入列表不为空时
		if(!BlankUtils.isBlank(playList) && playList.size()>0) {
			
			for(Record record:playList) {
				
				String action = record.get("action");
				String path = record.get("path");
				
				if(action.equalsIgnoreCase("Read")) {      //如果为Read的应用时，表示需要等待客户回复
					
					try {
						
						exec(action,path);           //执行播放并等待客户回复
						
						String respond = channel.getVariable("respond");
						
						if(!BlankUtils.isBlank(respond)) {      //客户回复不为空时
							
							if(respond.equalsIgnoreCase("1")) {     		//如果回复的是1时,确认安装
								
								exec("Noop","客户" + bshOrderList.get("CUSTOMER_TEL") + "回复1,即确认安装");
								//更改客户回复的同时，将呼叫状态更改为2，即是外呼成功
								BSHOrderList.dao.updateBSHOrderListRespond(bshOrderList.get("ID").toString(), respond);
								
								execPlayBack(respond1PlayList);     //回复后，还需要将结果播放回去
							}else if(respond.equalsIgnoreCase("2")) {		//如果回复的是2时，暂不安装
								
								exec("Noop","客户" + bshOrderList.get("CUSTOMER_TEL") + "回复2,即暂不安装");
								//更改客户回复的同时，将呼叫状态更改为2，即是外呼成功
								BSHOrderList.dao.updateBSHOrderListRespond(bshOrderList.get("ID").toString(), respond);
							
								execPlayBack(respond2PlayList);     //回复后，还需要将结果播放回去
								
							}else if(respond.equalsIgnoreCase("3")) {       //如果回复的是3时，延后安装
								exec("Noop","客户" + bshOrderList.get("CUSTOMER_TEL") + "回复3,即延后安装");
								//更改客户回复的同时，将呼叫状态更改为2，即是外呼成功
								BSHOrderList.dao.updateBSHOrderListRespond(bshOrderList.get("ID").toString(), respond);
								
								execPlayBack(respond3PlayList);     //回复后，还需要将结果播放回去
								
							}else {                       //如果回复的是其他按键时,按回复
								exec("Noop","客户 " + bshOrderList.get("CUSTOMER_TEL") + "回复" + respond + ",即为错误回复");
								respond = "4";            //强制为错误回复
								//更改客户回复的同时，将呼叫状态更改为2，即是外呼成功
								BSHOrderList.dao.updateBSHOrderListRespond(bshOrderList.get("ID").toString(), respond);
								
								execPlayBack(respond4PlayList);     //回复后，还需要将结果播放回去
							}
							
						}else {
							exec("Noop","客户" + bshOrderList.get("CUSTOMER_TEL") + "无回复任何");
							respond = "4";
							//更改客户回复的同时，将呼叫状态更改为2，即是外呼成功
							BSHOrderList.dao.updateBSHOrderListRespond(bshOrderList.get("ID").toString(), respond);
							
							execPlayBack(respond4PlayList);     //回复后，还需要将结果播放回去
						}
						
						//无论是否回复什么结果，或是没有回复结果,在这里表示外呼已经结束，需要将活跃通道减掉一个
						BSHPredial.activeChannelCount--;
						
					} catch (AgiException e) {
						e.printStackTrace();
					}

				}else {					//如果PlayBack 就执行插放操作
					try {
						exec(action,path);
					} catch (AgiException e) {
						e.printStackTrace();
					}
				}
				
			}
			
		}
	}
	
	/**
	 * 播放语音
	 * @param playList
	 */
	public void execPlayBack(List<Record> playList) {
		
		//如果插入列表不为空时
		if(!BlankUtils.isBlank(playList) && playList.size()>0) {
			for(Record record:playList) {
				String action = record.get("action");
				String path = record.get("path");
				
				try {
					exec(action,path);
				} catch (AgiException e) {
					e.printStackTrace();
				}
				
			}
		}
		
	}
	
	/**
	 * 取得前置流程 开场语音 Read 命令所需语音文件名字符串
	 * 
	 * 前置流程Read，主要包括以下两组语音内容：
	 * 			开场1：（灶具的前置流程）
	 * 				prefix_1_brand_0_productName_6：
	 * 					 您好，这里是西门子家电客服中心，来电跟您确认灶具的安装服务，请问您家里的气源开通了吗？已经开通请按1;还没有开通或者不清楚，请按2。
	 * 				prefix_1_brand_1_productName_6
	 * 					 您好，这里是博世家电客服中心，来电跟您确认灶具的安装服务，请问您家里的气源开通了吗？已经开通请按1;还没有开通或者不清楚，请按2。
	 * 
	 * 			开场2:（洗碗机的前置流程）
	 * 				prefix_1_brand_0_productName_8：
	 * 					您好，这里是西门子家电客服中心，来电跟您确认洗碗机的安装服务，上门安装时，需要把橱柜门板装到洗碗机上，请问这块门板准备好了吗？已经准备好了请按1;还没有准备好或者不清楚，请按2.
	 * 				prefix_1_brand_1_productName_8：
	 * 					您好，这里是博世家电客服中心，来电跟您确认洗碗机的安装服务，上门安装时，需要把橱柜门板装到洗碗机上，请问这块门板准备好了吗？已经准备好了请按1;还没有准备好或者不清楚，请按2.
	 * 
	 * @param bshOrderList
	 * @return
	 */
	public String getPrefixCallFlowReadVoiceFileToString(BSHOrderList bshOrderList) {
		
		String voicePath = BSHCallParamConfig.getVoicePathSingle();   //取出配置的语音文件（单声道）路径
		String voiceFile = null;
		
		int brand = bshOrderList.getInt("BRAND");                            //品牌，0：西门子；1：博世
		int productName = bshOrderList.getInt("PRODUCT_NAME");               //产品名称
		
		String voiceNameForPrefixCallFlow = "prefix_1_brand_" + brand + "_productName_" + productName;
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameForPrefixCallFlow)) {
			voiceFile = voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForPrefixCallFlow);
		}
		
		return voiceFile;
	}
	
	/**
	 * 执行了前置流程的 read 应用问询后，如果客户回复了1，则表示环境具备
	 * 如果回复了2或是其他的按键，则表示环境不具备，需要播放环境不具备对应的语音文件
	 * 
	 * 客户环境不具备时，主要包括以下两条语音内容：
	 * 		回复2语音:（灶具类目前置流程回复2）
	 * 			prefix_productName_6_repond_2：
	 * 				为了避免漏气，安装灶具时必须要做漏气检测，通常要先开通气源，再上门安装。灶具面板上有一个二维码，气源开通后，请您扫码预约安装服务，非常抱歉给您带去不便，感谢您的配合，再见。
	 * 		回复2语音:（洗碗机类目前置流程回复2 ）
	 * 			prefix_productName_8_repond_2：
	 * 				为了一次上门就能装好，需要您准备好门板之后再预约安装，洗碗机门体内侧有一个二维码，请您扫码预约安装服务，还可以方便的获取使用指南，非常抱歉给您带去不便，感谢您的配合，再见。
	 * 
	 * @param bshOrderList
	 * @return
	 */
	public String getPrefixCallFlowRespond2VoiceFile(BSHOrderList bshOrderList) {
		
		String voicePath = BSHCallParamConfig.getVoicePathSingle();   //取出配置的语音文件（单声道）路径
		String voiceFile = null;
		
		int productName = bshOrderList.getInt("PRODUCT_NAME");               //产品名称
		
		String voiceNameForPrefixCallFlowRespond2 = "prefix_productName_" + productName + "_respond_2";
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameForPrefixCallFlowRespond2)) {
			voiceFile = voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForPrefixCallFlowRespond2);
		}
		
		return voiceFile;
	}
	
	/**
	 * 取得 Read 命令所需语音文件名字符串
	 * 一次性取得多个文件，形成完整的调查语音
	 * 
	 * 开场分两种情况：
	 * 
	 * 		开场1：您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据(京东/苏宁/国美/天猫)平台传来的信息，
	 * 		                 我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。 

                               开场2：您好，这里是(西门子/博世)家电客服中心。您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，
                                                 需要您进一步确认。确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
                  
                   语音列表如下：
                   
          begin_1_brand_0_timeType_1：您好，这里是西门子家电客服中心，来电跟您确认
          begin_1_brand_1_timeType_1：您好，这里是博世家电客服中心，来电跟您确认
          begin_1_brand_0_timeType_2：您好，这里是西门子家电客服中心
          begin_1_brand_1_timeType_2：您好，这里是博世家电客服中心
          
          		  begin_2_timeType_1：的安装日期
                  begin_2_timeType_2：您在国美选购的
          
          	 begin_3_channelSource_1：根据京东平台传来的信息，我们将于
             begin_3_channelSource_2：根据苏宁平台传来的信息，我们将于
		     begin_3_channelSource_3：根据天猫平台传来的信息，我们将于
		     begin_3_channelSource_4：根据国美平台传来的信息，我们将于
		          begin_3_timeType_2：将于
		          
		          begin_4_timeType_1：上门安装
                  begin_4_timeType_2：送货，我们将于送货当天上门安装，需要您进一步确认
                  
                  begin_5_timeType_1：确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
                  begin_5_timeType_2：确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
          
	 * 
	 * 
	 * @param bshOrderList
	 * @return
	 */
	public String getReadVoiceFileToString(BSHOrderList bshOrderList) {
		
		StringBuilder sb = new StringBuilder();
		String voicePath = BSHCallParamConfig.getVoicePathSingle();   //取出配置的语音文件（单声道）路径
		
		int brand = bshOrderList.getInt("BRAND");                            //品牌，0：西门子；1：博世
		int channelSource = bshOrderList.getInt("CHANNEL_SOURCE");           //购物平台，1：京东；2：苏宁；3：天猫；4：国美
		int timeType = bshOrderList.getInt("TIME_TYPE");                     //日期类型，1：安装日期；2：送货日期
		int productName = bshOrderList.getInt("PRODUCT_NAME");               //产品名称
		
		int isConfirm = bshOrderList.getInt("IS_CONFIRM");
		if(isConfirm == 1 && (productName==6 || productName==8)) {    //如果 isConfirm 为1，就表示带有前置流程
			//如果客户执行到这里，表示环境具备，不需要再加入前面的
			//您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据(京东/苏宁/国美/天猫)平台传来的信息，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。 
			//中的 您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。
			//和 您好，这里是(西门子/博世)家电客服中心。您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，需要您进一步确认。确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
			//中的 您好，这里是(西门子/博世)家电客服中心。
		}else {
		
				/** 一、组织第一条语音
				  begin_1_brand_0_timeType_1：您好，这里是西门子家电客服中心，来电跟您确认
		          begin_1_brand_1_timeType_1：您好，这里是博世家电客服中心，来电跟您确认
		          begin_1_brand_0_timeType_2：您好，这里是西门子家电客服中心
		          begin_1_brand_1_timeType_2：您好，这里是博世家电客服中心
				 */
				String voiceNameFor1 = "begin_1_brand_" + brand + "_timeType_" + timeType;
				if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameFor1)) {
					sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameFor1));
				}
				
				/**
				 * 二、再根据日期类型,决定直接报产品名称，还是报：您在国美选购的
				 */
				if(timeType==1) {      //表示安装日期，需要直接报出产品的名称
					/**
					 * 整句即是：
					 * produceName_*:洗衣机   
					 * begin_2_timeType_1：的安装日期
					 */
					String voiceNameForProductName = "productName_" + productName;
					if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameForProductName)) {
						sb.append("&");
						sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForProductName));
					}
					//紧接着第二条语音: 的安装日期
					String voiceNameFor2 = "begin_2_timeType_1";
					if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameFor2)) {
						sb.append("&");
						sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameFor2));
					}
					
				}else {              //如果日期类型为送货日期，则需要先报出： 您在国美选购的
					/**
					 * 整句为：
					 * begin_2_timeType_2：您在国美选购的
					 * productName_*:  洗衣机
					 */
					//先紧接着第二条语音: 您在国美选购的
					String voiceNameFor2 = "begin_2_timeType_2";
					if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameFor2)) {
						sb.append("&");
						sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameFor2));
					}
					
					//产品语音播报
					String voiceNameForProductName = "productName_" + productName;
					if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameForProductName)) {
						sb.append("&");
						sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForProductName));
					}
					
				}
		}
		
		/**
		 * 三、组织第三条语音
		 * 
		    begin_3_channelSource_1：根据京东平台传来的信息，我们将于
            begin_3_channelSource_2：根据苏宁平台传来的信息，我们将于
		    begin_3_channelSource_3：根据天猫平台传来的信息，我们将于
		    begin_3_channelSource_4：根据国美平台传来的信息，我们将于
		    begin_3_timeType_2：将于
		 */
		if(timeType==1) {     //日期类型为：安装日期
			String voiceNameFor3 = "begin_3_channelSource_" + channelSource;   
			if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameFor3)) {
				sb.append("&");
				sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameFor3));
			}
		}else {               //日期类型为：送货日期
			String voiceNameFor3 = "begin_3_timeType_2";   
			if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameFor3)) {
				sb.append("&");
				sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameFor3));
			}
		}
		
		/**
		 * 安装日期或是送货日期 组织
		 * 
		 * 还有一种情况需要考虑：
		 * 如果安装/送货日期为明天（即是第二天时），即无需报出具体时间，只需要播报”明天“即可
		 * 
		 */
		String expectInstallDate = bshOrderList.getDate("EXPECT_INSTALL_DATE").toString();      //取出期望安装日期
		
		boolean b = checkInstallDateIsNextDay(expectInstallDate);
		
		if(b) {
			//System.out.println("安装日期为明天");
			String voiceNameForDate = "tomorrow";
			if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameForDate)) {
				sb.append("&");
				sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForDate));
			}
		}else {
			//System.out.println("安装日期不是明天,而是" + expectInstallDate);
			Date installDate = DateFormatUtils.parseDateTime(expectInstallDate, "yyyy-MM-dd");
			String monthStr = DateFormatUtils.formatDateTime(installDate, "MM");
			String dayStr = DateFormatUtils.formatDateTime(installDate,"dd");
			String voiceNameForMonth = "month_" + monthStr;
			String voiceNameForDay = "day_" + dayStr;
			if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameForMonth)) {
				sb.append("&");
				sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForMonth));
			}
			if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameForDay)) {
				sb.append("&");
				sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForDay));
			}
		}
		
		/**
		 * 四、组织第四条语音
		 *
		   begin_4_timeType_1：上门安装
           begin_4_timeType_2：送货，我们将于送货当天上门安装，需要您进一步确认
		 */
		String voiceNameFor4 = "begin_4_timeType_" + timeType;
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameFor4)) {
			sb.append("&");
			sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameFor4));
		}
		
		/**
		 * 五、组织第五条语音
		 * begin_5_timeType_1：确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
           begin_5_timeType_2：确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
		 */
		String voiceNameFor5 = "begin_5_timeType_" + timeType;
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameFor5)) {
			sb.append("&");
			sb.append(voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameFor5));
		}
		
		return sb.toString();
		
	}
	
	/**
	 * 根据传入的订单信息，生成播放列表
	 * 
	 * 完整的调查流程是这样的：
	 * 您好，这里是博世家电客服中心,来电跟您确认 洗衣机 的安装日期。 根据京东平台传来的信息，我们将于 12月10号 上门安装。
	        确认安装请按“1”，暂不安装请按“2”，如需改约到后面3天，请按“3”。
	 * 
	 * 不过语音并不是一整段的，需要重新拼接
	 * 
	 * （1）您好，这里是博世家电客服中心,来电跟您确认 （2）洗衣机 （3）的安装日期。 （4）根据京东平台传来的信息，我们将于（5） 12月10号 （6）上门安装。
	        （7）确认安装请按“1”，暂不安装请按“2”，如需改约到后面3天，请按“3”。
	 * 
	 * @param bshOrderList
	 * @return
	 */
	public List<Record> getPlayList(BSHOrderList bshOrderList) {
		
		String voicePath = BSHCallParamConfig.getVoicePathSingle();
		List<Record> list = new ArrayList<Record>();        //新建一个List，用于储存语音
		//(1)您好，这里是西门子家电客服中心,来电跟您确认
		//	 您好，这里是博世家电客服中心,来电跟您确认
		int brand = bshOrderList.getInt("BRAND");           //取得品牌
		String voiceId1 = "Brand_" + brand;
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceId1)) {
			list.add(setRecord("wait","1"));         //先停顿1秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceId1))); 
		}
		//(2)产品语音
		int productName = bshOrderList.getInt("PRODUCT_NAME");       //取得产品
		String voiceId2 = "ProductName_" + productName;
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceId2)) {
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceId2))); 
		}
		//(3)的安装日期
		String voiceId3 = "Notice_1";
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceId3)) {
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceId3))); 
		}
		//(4)根据京东平台传来的信息，我们将于
		int channelSource = bshOrderList.getInt("CHANNEL_SOURCE");   //取出平台信息
		String voiceId4 = "ChannelSource_" + channelSource;
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceId4)) {
			list.add(setRecord("wait","0.5"));         //先停顿1秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceId4)));
			//list.add(setRecord("wait","1"));         //先停顿1秒
		}
		//(5)5月12号
		String expectInstallDate = bshOrderList.getDate("EXPECT_INSTALL_DATE").toString();      //取出期望安装日期
		Date installDate = DateFormatUtils.parseDateTime(expectInstallDate, "yyyy-MM-dd");
		String monthStr = DateFormatUtils.formatDateTime(installDate, "MM");
		String dayStr = DateFormatUtils.formatDateTime(installDate,"dd");
		String voiceIdForMonth = "Month_" + monthStr;
		String voiceIdForDay = "Day_" + dayStr;
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceIdForMonth)) {
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceIdForMonth))); 
		}
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceIdForDay)) {
			list.add(setRecord("wait","0.5"));         //先停顿0.5秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceIdForDay))); 
			//list.add(setRecord("wait","0.5"));         //先停顿0.5秒
		}
		
		//(6)上门安装
		String voiceId6 = "Notice_2";
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceId6)) {
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceId6))); 
		}
		
		//（7）确认安装请按“1”，暂不安装请按“2”，如需改约到后面3天，请按“3”。
		String voiceId7 = "ComfirmVoice";
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceId7)) {
			String comfirmVoicePath = voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceId7);
			list.add(setRecord("wait","1"));         //先停顿1秒
			list.add(setRecord("Read","respond," + comfirmVoicePath + ",1,,2,8"));    //一个回复按键，2次播放，等待8秒
		}
		
		return list;
		
	}
	
	/**
	 * 客户回复1，即确认安装时
	 * 
	 * 场景1：京东、苏宁、天猫
			    您的机器安装日期已确认为12月10号，工程师最迟会在当天早上9:30之前与您联系具体上门时间。感谢您的配合，再见。

	         场景2：国美
                                     您的机器安装日期已确认为12月10号，工程师最迟会在当天早上9:30之前与您联系具体上门时间。为确保您的权益，请认准(西门子/博世)厂家的专业工程师。感谢您的配合，再见。
	 * 
	 * 并非一整段语音
	 * 
	                      respond_1_1: 您的机器安装日期已确认为
	           respond_1_2_timeType_1: 工程师最迟会在当天早上9点半之前与您联系具体上门时间，感谢您的配合，再见。
	   respond_1_2_timeType_2_brand_0: 工程师最迟会在当天早上9点半之前与您联系具体上门时间，为确保您的权益，请认准西门子厂家的专业工程师，感谢您的配合，再见。
	   respond_1_2_timeType_2_brand_1: 工程师最迟会在当天早上9点半之前与您联系具体上门时间，为确保您的权益，请认准博世厂家的专业工程师，感谢您的配合，再见。
	   
	 * @param bshOrderList
	 * @return
	 */
	public List<Record> getRespond1PlayList(BSHOrderList bshOrderList) {
		
		String voicePath = BSHCallParamConfig.getVoicePathSingle();
		List<Record> list = new ArrayList<Record>();
		
		//（1）您的机器安装日期已确认为
		String voiceName = "respond_1_1";
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceName)) {
			list.add(setRecord("wait","1"));         //先停顿1秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceName))); 
			//list.add(setRecord("wait","1"));         //先停顿1秒
		}
		
		//（2）12月10号
		String expectInstallDate = bshOrderList.getDate("EXPECT_INSTALL_DATE").toString();      //取出期望安装日期
		Date installDate = DateFormatUtils.parseDateTime(expectInstallDate, "yyyy-MM-dd");
		String monthStr = DateFormatUtils.formatDateTime(installDate, "MM");
		String dayStr = DateFormatUtils.formatDateTime(installDate,"dd");
		
		String voiceNameForMonth = "month_" + monthStr;
		String voiceNameForDay = "day_" + dayStr;
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameForMonth)) {
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForMonth))); 
		}
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceNameForDay)) {
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForDay))); 
		}
		
		//（3）
		//     A:工程师最迟会在当天早上9点半之前与您联系具体上门时间，感谢您的配合，再见。
		//     B:工程师最迟会在当天早上9点半之前与您联系具体上门时间，为确保您的权益，请认准西门子厂家的专业工程师，感谢您的配合，再见。
		//     C:工程师最迟会在当天早上9点半之前与您联系具体上门时间，为确保您的权益，请认准博世厂家的专业工程师，感谢您的配合，再见。
		int timeType = bshOrderList.getInt("TIME_TYPE");     //日期类型: 1:安装日期；  2：送货日期
		int brand = bshOrderList.getInt("BRAND");            //品牌： 0：西门子；  1：博世
		int channelSource = bshOrderList.getInt("CHANNEL_SOURCE");   //购物平台：1：京东 2：苏宁  3：天猫 4：国美
		
		if(timeType==1) {        //日期类型为安装日期
			String voiceNameForTimeType1 = "respond_1_2_timeType_1";
			if(channelSource==4) {   //如果购物平台为国美
				voiceNameForTimeType1 = "respond_1_2_timeType_2_brand_" + brand;
			}
			list.add(setRecord("wait","0.5"));      //先停半秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForTimeType1)));
		}else {                  //日期类型为送货日期
			
			String voiceNameForTimeType2 = "respond_1_2_timeType_2_brand_" + brand;
			list.add(setRecord("wait","0.5"));      //先停半秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceNameForTimeType2)));
		}
		
		return list;
	}
	
	/**
	 * 回复2，即是暂不安装
	 * 
	 * 您的机器，暂时将不会安排上门安装。如后期仍然需要安装，欢迎拨打400-889-9999，或者关注西门子家电微信公众号预约。感谢您的配合，再见。
	 * 您的机器，暂时将不会安排上门安装。如后期仍然需要安装，欢迎拨打 400-885-5888 或者关注 “博世家电” 微信公众号预约。感谢您的配合，再见。
	 * 
	 * 根据品牌组织语音
	 * 
	 * @param bshOrderList
	 * @return
	 */
	public List<Record> getRespond2PlayList(BSHOrderList bshOrderList) {
		
		String voicePath = BSHCallParamConfig.getVoicePathSingle();
		List<Record> list = new ArrayList<Record>();
		
		int brand = bshOrderList.getInt("BRAND");           //取得品牌
		String voiceName = "respond_2_brand_" + brand;				
		
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceName)) {
			list.add(setRecord("wait","0.5"));         //先停顿1秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceName))); 
		}
		
		return list;
	}
	
	/**
	 * 客户回复 3，延后安装
	 * 
	 * 稍后您会收到1条确认短信，请您按短信提示，直接回复数字即可。感谢您的配合，再见。
	 * @param bshOrderList
	 * @return
	 */
	public List<Record> getRespond3PlayList(BSHOrderList bshOrderList) {
		
		String voicePath = BSHCallParamConfig.getVoicePathSingle();
		List<Record> list = new ArrayList<Record>();
		
		//(1) 
		String voiceName = "respond_3";
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceName)) {
			list.add(setRecord("wait","0.5"));         //先停顿1秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceName))); 
		}
		
		return list;
	}
	
	public List<Record> getRespond4PlayList(BSHOrderList bshOrderList) {
		
		String voicePath = BSHCallParamConfig.getVoicePathSingle();
		List<Record> list = new ArrayList<Record>();
		
		int brand = bshOrderList.getInt("BRAND");           //取得品牌
		String voiceName = "respond_4_brand_" + brand;	
		
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceName)) {
			list.add(setRecord("wait","0.5"));         //先停顿1秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceName))); 
		}
		
		return list;
	}
	
	public List<Record> getRespondErrorPlayList(BSHOrderList bshOrderList) {
		
		String voicePath = BSHCallParamConfig.getVoicePathSingle();
		List<Record> list = new ArrayList<Record>();
		
		String voiceName = "respond_error";
		
		if(BSHVoiceConfig.getVoiceMap().containsKey(voiceName)) {
			list.add(setRecord("wait","0.5"));         //先停顿1秒
			list.add(setRecord("PlayBack",voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(voiceName))); 
		}
		
		return list;
	}
	
	/**
	 * 检查安装/送货日期是否为第二天
	 * 
	 * @param expectInstallDate
	 * 				安装/送货 日期，格式：yyyy-MM-dd,如:2018-12-10
	 * 
	 * @return
	 * 		是：返回true; 否: 返回 false
	 */
	public boolean checkInstallDateIsNextDay(String expectInstallDate) {
		
		//先判断当天日期与安装日期是否相差一天
		String installDateTime = expectInstallDate + " 00:00:00";
		String currDateTime = DateFormatUtils.formatDateTime(new Date(), "yyyy-MM-dd") + " 00:00:00";
				
		Date installDate = DateFormatUtils.parseDateTime(installDateTime, "yyyy-MM-dd HH:mm:ss");
		Date currDate = DateFormatUtils.parseDateTime(currDateTime, "yyyy-MM-dd HH:mm:ss");
		
		long installDateTimes = installDate.getTime();
		long currDateTimes = currDate.getTime();
		
		long intervalTimes = installDateTimes - currDateTimes;
		
		System.out.println("安装日期：expectInstallDate 为 " + expectInstallDate + ",与今天相差毫秒数：" + intervalTimes);
		
		if(intervalTimes == 24 * 60 * 60 * 1000) {
			return true;
		}else {
			return false;
		}
		
	}
	
	public Record setRecord(String action,String path) {
		
		Record record = new Record();
		
		record.set("action", action);
		record.set("path", path);
		
		return record;
		
	}
	
	

}
