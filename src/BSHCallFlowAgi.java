import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;

import com.callke8.bsh.bshcallparam.BSHCallParamConfig;
import com.callke8.bsh.bshorderlist.BSHHttpRequestThread;
import com.callke8.bsh.bshorderlist.BSHOrderList;
import com.callke8.bsh.bshvoice.BSHVoiceConfig;
import com.callke8.pridialqueueforbshbyquartz.BSHPredial;
import com.callke8.utils.BlankUtils;
import com.callke8.utils.DateFormatUtils;
import com.callke8.utils.StringUtil;

public class BSHCallFlowAgi extends BaseAgiScript {
    
    /**
     * 提示音：对不起，输入有误。
     */
    static String inputErrorVoiceFile = null;
    
    /**
     * 提示音：对不起，输入有误，如有必要，我们可能会再次与您联系。
     *      主要是针对 OIMS 平台时，两次未输入或是错误输入时，的专用语音。
     *      
     *      其他四大平台（京东、天猫、苏宁、国美）继续使用原来的输入错误提示音：
     *      对不起，输入有误，我们可能会再次与您联系，再见
     *      
     */
    //static String respondErrorVoiceFileForChannelSource5 = null;
    
    static {
        inputErrorVoiceFile = getVoiceFile("response_error_for_first_time");    //客户第一次回复错误，提示：对不起，输入有误。
        //respondErrorVoiceFileForChannelSource5 = getVoiceFile("verify_respond_error");    //对不起，输入有误，如有必要，我们可能会再次与您联系。
    }
    
	@Override
	public void service(AgiRequest request, AgiChannel channel) {
		
	    try {
			
			String bshOrderListId = channel.getVariable("bshOrderListId");
			//StringUtil.writeString("/opt/dial-log.log",DateFormatUtils.getCurrentDate() + ",/(流程执行):bshOrderListId " + bshOrderListId + ",通道标识:" + channel.getName(), true);
			
			//从数据表中取出订单信息
			BSHOrderList bshOrderList = BSHOrderList.dao.getBSHOrderListById(bshOrderListId);
			
			int channelSource = bshOrderList.getInt("CHANNEL_SOURCE");           //购物平台，1：京东；2：苏宁；3：天猫；4：国美；5：OIMS
			int isConfirm = bshOrderList.getInt("IS_CONFIRM");                   //取出是否带有前置流程，1：带前置流程；2：不带前置流程
			int outboundType = bshOrderList.getInt("OUTBOUND_TYPE");             //取出外呼类型，1：确认安装；2：零售核实
			
			/**
			 系统一共有三类流程，但是总共包含六种流程：
			                  一、确认安装类流程：callFlow1() 方法
			        (1) 京东、苏宁、天猫 平台的确认安装流程：
			                                    您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据(京东/苏宁/国美/天猫)平台传来的信息，我们将于(明天/12月10号)上门安装。
			                                    确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
			        (2) 国美平台的确认安装流程：
			                                    您好，这里是(西门子/博世)家电客服中心。您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，需要您进一步确认。
			                                    确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
			        (3) OIMS平台的流程：
			                                     您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据促销员为您填报的信息，我们将于(明天/12月10号)上门安装。
			                                     确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。没有购买过,请按“5”
			                  二、前置类流程：callFlow2() 方法
			                         前置类流程只针对产品类目为： 6（灶具）或 8（洗碗机）这两种产品类目才有可能有前置的流程。且前置流程执行结束后，如果环境具备时，还需要继续执行确认安装类流程。
			        (4) 灶具类前置流程：
			                                    您好，这里是（博世、西门子）家电客服中心，来电跟您确认灶具的安装服务，请问您家里的气源开通了吗？
                                                                        已经开通请按“1”;还没有开通或者不清楚请按“2”。
                    (5) 洗碗机类前置流程：
                                                                        您好，这里是(博世、西门子)家电客服中心，来电跟您确认洗碗机的安装服务，上门安装时，需要把橱柜门板装到洗碗机上，请问这块门板准备好了吗？
                                                                        已经准备好了请按1;还没有准备好或者不清楚，请按2.
                                                     三、零售核实：callFlow3() 方法
                                                            零售核实类流程：零售核实只有OIMS平台可以执行该流程
                    (6) 您好，这里是（博世、西门子）家电客服中心，来电主要想了解您的购机情况。请问您有没有购买（博世、西门子）品牌的对开门冰箱？
                                                                        确认购买请按“1”，没有购买过，请按“2”。      
			*/  
			
			StringBuilder logSb = new StringBuilder();
			logSb.append("BSH系统的AGI流程开始执行。");
			logSb.append("客户号码：" + bshOrderList.get("CUSTOMER_TEL") + ",订单ID为：" + bshOrderListId + "，订单详情：" + bshOrderList);
			
			if(outboundType ==2 && channelSource == 5) {       //零售核实流程
			    logSb.append("系统进入--->零售核实流程！");
			    printLog(logSb.toString());
			    
			    callFlow3(bshOrderList, channel);
			} else if(isConfirm == 1) {                        //执行前置流程        
			    logSb.append("系统进入--->前置流程！");
			    printLog(logSb.toString());
                
			    callFlow2(bshOrderList, channel);
			} else if(isConfirm == 2) {                        //执行安装确认流程
			    logSb.append("系统进入--->安装确认流程！");
			    printLog(logSb.toString());
                
			    callFlow1(bshOrderList, channel, 1);
			}
			
			//无论是否回复什么结果，或是没有回复结果,在这里表示外呼已经结束，需要将活跃通道减掉一个
            if(BSHPredial.activeChannelCount > 0) {        
                BSHPredial.activeChannelCount--;
            }
			
	    } catch(AgiException agiE) {
	        agiE.printStackTrace();
	    }
	}
	
	/**
	 * 流程1：确认安装流程
	 *     您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据(京东/苏宁/国美/天猫)平台传来的信息，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
	 *     您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据促销员为您填报的信息，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。没有购买过,请按“5”
	 *     您好，这里是(西门子/博世)家电客服中心。您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，需要您进一步确认。确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
	 *     
	 * @param bshOrderList
	 * @param channel
	 * @param flag
	 *           flag: 确认安装流程的类型。1：一般确认安装流程；2：由前置流程转入的流程。默认值为1。
	 */
	public void callFlow1(BSHOrderList bshOrderList,AgiChannel channel,int flag) {
	    
	    int channelSource = bshOrderList.getInt("CHANNEL_SOURCE");           //购物平台，1：京东；2：苏宁；3：天猫；4：国美；5：OIMS
	    int brand = bshOrderList.getInt("BRAND");                            //品牌，0：西门子；1：博世
        int isConfirm = bshOrderList.getInt("IS_CONFIRM");                   //取出是否带有前置流程，1：带前置流程；2：不带前置流程                  
        //如果有前置流程（即是isConfirm == 1），但是执行到这里，表示安装环境已经具备
        String preCallResult = isConfirm == 1?"1":"0"; //preCallResult 前置流程外呼结果， 0：没有前置； //指没有前置流程 1：确认； //有前置流程，且安装环境确认已经具备 2：不确认； //有前置流程，但安装环境不具备 3：未接听； //没有接听
        
	    // 一、组织语音文件
	    /**
	                   第一步：获取确认安装流程的Read的文件列表
	     */
	    String readVoiceFileList = getCallFlow1ReadVoiceFile(bshOrderList, flag);
	    
	    /**
	                  第二步：客户回复的语音播放列表
	    /**
	     (1) 回复错误/无回复
	                          对不起，输入有误，我们可能会再次与您联系，再见
	     */
	    String respondErrorVoiceFile = getVoiceFile("respond_error");    
	    
	    /**
	     (2) 回复1（确认安装）的播放列表
	                         场景1：京东、苏宁、天猫、OIMS
                                                您的机器安装日期已确认为12月10号，工程师最迟会在当天早上9:30之前与您联系具体上门时间。感谢您的配合，再见。
                                    场景2：国美
                                                您的机器安装日期已确认为12月10号，工程师最迟会在当天早上9:30之前与您联系具体上门时间。为确保您的权益，请认准(西门子/博世)厂家的专业工程师。感谢您的配合，再见。
	     */
	    String respond1VoiceFile = getRespond1PlayList(bshOrderList);
	    
	    /**
	     (3) 回复2（暂不安装）的播放列表
	                           您的机器暂时将不会安装上门安装，如后期仍有需要，欢迎扫描机器上的二维码，或者关注“西门子家电/博世家电”微信公众号预约，感谢您的配合，再见
	     */
	    String respond2VoiceFile = getVoiceFile("respond_2_brand_" + brand);
	    
	    /**
	     (4) 回复3（延后安装）的播放列表
	                         稍后您会收到1条确认短信，请您按短信提示，直接回复数字即可。感谢您的配合，再见。
	     */
	    String respond3VoiceFile = getVoiceFile("respond_3");
	    
	    /**
	     (5) 回复4（提前预约）的播放列表
	                         我们会按您提前预约好的日期上门。 感谢您选购（西门子/博世）家电，再见。
	     */
	    String respond4VoiceFile = getVoiceFile("respond_4_brand_" + brand);
	    
	    /**
	     (6) 回复5(没有购买过): OIMS 平台独有
	         verify_respond_2：确认没有买过，请按1，返回上级菜单，请按2。
	         verify_confirm_respond_2: 非常感谢您的反馈，我们会对相关信息做进一步核实，抱歉给你带来不便，再见。
	     */
	    String verifyRespond2VoiceFile = getVoiceFile("verify_respond_2");
	    String verifyConfirmRespond2VoiceFile = getVoiceFile("verify_confirm_respond_2");
	    	    
	    // 二、执行安装确认流程
	    String respond = execRead(readVoiceFileList, 1, 1, 8, channel);    //播放一次，并收集客户的回复
	    if(!isRespondError(bshOrderList, respond)) {                       //如果输入错误，提示输入错误，并再次输入
	        //提示：对不起，输入有误。
	        execPlayBack(inputErrorVoiceFile);
	        
	        respond = execRead(readVoiceFileList, 1, 1, 8, channel);    //再播放一次，并收集客户的回复
	    }
	    
	    
	    if(!isRespondError(bshOrderList, respond)) {                      //如果两次输入均为错误
	        
	        String keyValue = BlankUtils.isBlank(respond)?"无回复":respond;  //
	        String respondResult = BlankUtils.isBlank(respond)?"9":"5";    //回复结果，5：错误回复；9：无回复。
	        
	        if(channelSource == 5) {     //如果 channelSource = 5 , 即是 OIMS 平台时，提示语音为： 对不起，输入有误，如有必要，我们可能会再次与您联系。
	            hangupCallAndFeedBackResult(bshOrderList, preCallResult, respondResult, keyValue, getVoiceFile("verify_respond_error"), channel);     //提示： 对不起，输入有误。我们可能会再次与您联系，再见。
	        } else {                     //如果 channelSource 非 OIMS 平台，提示语音为：对不起，输入有误，我们可能会再次与您联系，再见
	            hangupCallAndFeedBackResult(bshOrderList, preCallResult, respondResult, keyValue, respondErrorVoiceFile, channel);     //提示： 对不起，输入有误。我们可能会再次与您联系，再见。
	        }
            return;
	    }
	    
	    if(respond.equals("1")) {              //如果回复是1，即确认安装
	        printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 确认安装流程，回复确认安装，keyValue=" + respond);
	        hangupCallAndFeedBackResult(bshOrderList, preCallResult, respond, respond, respond1VoiceFile, channel);
            return;
	    } else if(respond.equals("2")) {       //如果回复是2，暂不安装
	        printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 确认安装流程，回复暂不安装，keyValue=" + respond);
            hangupCallAndFeedBackResult(bshOrderList, preCallResult, respond, respond, respond2VoiceFile, channel);
            return;
	    } else if(respond.equals("3")) {       //如果回复是3，延后安装
	        printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 确认安装流程，回复延后安装，keyValue=" + respond);
	        hangupCallAndFeedBackResult(bshOrderList, preCallResult, respond, respond, respond3VoiceFile, channel);
            return;
	    } else if(respond.equals("4")) {       //如果回复是4,提前预约
	        printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 确认安装流程，回复提前预约，keyValue=" + respond);
	        hangupCallAndFeedBackResult(bshOrderList, preCallResult, respond, respond, respond4VoiceFile, channel);
	        return;
	    } else if(respond.equals("5")) {       //如果客户回复是5，那是OIMS平台，需要再次跟客户确认是否没有购买过
	        
	        String respond2 = execRead(verifyRespond2VoiceFile, 1, 1, 8, channel);    //播放：确认没有买过，请按1，返回上级菜单，请按2。
	        if(BlankUtils.isBlank(respond2) || !(respond2.equals("1") || respond2.equals("2"))) {     //客户回复为非 1，2 时。
	            //提示：对不起，输入有误。
	            execPlayBack(inputErrorVoiceFile);
	            respond2 = execRead(verifyRespond2VoiceFile, 1, 1, 8, channel);    //再一次播放：确认没有买过，请按1，返回上级菜单，请按2。
	        }
	        
	        if(BlankUtils.isBlank(respond2) || !(respond2.equals("1") || respond2.equals("2"))) {     //客户回复为非 1，2 时。
	            
	            String keyValue = BlankUtils.isBlank(respond2)?"无回复":respond2;  //
	            printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 确认安装流程，回复没有购买过，不过确认时没有回复1或2，keyValue=" + keyValue);
	            String respondResult = BlankUtils.isBlank(respond2)?"9":"5";    //回复结果，5：错误回复；9：无回复。
	            
	            hangupCallAndFeedBackResult(bshOrderList, preCallResult, respondResult, "5-" + keyValue, getVoiceFile("verify_respond_error"), channel);   //提示：对不起，输入有误，如有必要，我们可能会再次与您联系。
	            return;
	        }
	        
	        if(respond2.equals("1")) {                 //如果客户回复的是1，则表示客户没有购买过
	            
	            String keyValue = respond2;
	            String respondResult = "12";           //12:没有购买过 
	            printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 确认安装流程，回复没有购买过，keyValue=" + keyValue);
	            
	            hangupCallAndFeedBackResult(bshOrderList, preCallResult, respondResult, "5-" + keyValue, verifyConfirmRespond2VoiceFile, channel);  //非常感谢您的反馈，我们会对相关信息做进一步核实，抱歉给你带来不便，再见。
	            return;
	        } else if(respond2.equals("2")) {          //如果客户回复的是2，则需要返回上一步
	            
	            printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 确认安装流程，选择返回上一步，系统准备返回上一步！keyValue=" + respond2);
	            callFlow1(bshOrderList, channel, 2);   //注意这里需要将 flag 设置为2，即是去掉前面的东西。
 	        }
	        
	    }
	    
	}
	
	/**
	 * 流程2： 前置流程
	 *     您好，这里是（博世、西门子）家电客服中心，来电跟您确认灶具的安装服务，请问您家里的气源开通了吗？已经开通请按“1”;还没有开通或者不清楚请按“2”。
	 *         回复2：为了避免漏气，安装灶具时必须要做漏气检测，通常要先开通气源，再上门安装。灶具面板上有一个二维码，气源开通后，请您扫码预约安装服务，非常抱歉给您带去不便，感谢您的配合，再见。
	 *     您好，这里是(博世、西门子)家电客服中心，来电跟您确认洗碗机的安装服务，上门安装时，需要把橱柜门板装到洗碗机上，请问这块门板准备好了吗？已经准备好了请按1;还没有准备好或者不清楚，请按2.
	 *         回复1：为了一次上门就能装好，需要您准备好门板之后再预约安装，洗碗机门体内侧有一个二维码，请您扫码预约安装服务，还可以方便的获取使用指南，非常抱歉给您带去不便，感谢您的配合，再见。
	 * @param bshOrderList
	 * @param channel
	 */
	public void callFlow2(BSHOrderList bshOrderList, AgiChannel channel) {
	    
	    int brandValue = bshOrderList.getInt("BRAND");                            //品牌，1：西门子家电； 2：博世家电
	    int productNameValue = bshOrderList.getInt("PRODUCT_NAME");               //取出产品类目
	    
	    //一、组织语音文件
	    /**
	                    第一步：收集前置流程客户安装环境是否具备的 Read 的文件列表
	                                 （1）第一条语音：
	               prefix_1_brand_0_productName_6： 您好，这里是西门子家电客服中心，来电跟您确认灶具的安装服务，请问您家里的气源开通了吗？已经开通请按1;还没有开通或者不清楚，请按2。
	               prefix_1_brand_1_productName_6： 您好，这里是博世家电客服中心，来电跟您确认灶具的安装服务，请问您家里的气源开通了吗？已经开通请按1;还没有开通或者不清楚，请按2。
	               prefix_1_brand_0_productName_8： 您好，这里是西门子家电客服中心，来电跟您确认洗碗机的安装服务，上门安装时，需要把橱柜门板装到洗碗机上，请问这块门板准备好了吗？已经准备好了请按1;还没有准备好或者不清楚，请按2.
                   prefix_1_brand_1_productName_8： 您好，这里是博世家电客服中心，来电跟您确认洗碗机的安装服务，上门安装时，需要把橱柜门板装到洗碗机上，请问这块门板准备好了吗？已经准备好了请按1;还没有准备好或者不清楚，请按2.
	     */
	     String prefix = "prefix_1_brand_" + brandValue + "_productName_" + productNameValue;
	     
	     String prefixVoiceFile = getVoiceFile(prefix);
	     
	     /**
	                     第二步：客户的回复
	             prefix_productName_6_respond_2： 为了避免漏气，安装灶具时必须要做漏气检测，通常要先开通气源，再上门安装。灶具面板上有一个二维码，气源开通后，请您扫码预约安装服务，非常抱歉给您带去不便，感谢您的配合，再见。
	             prefix_productName_8_respond_2： 为了一次上门就能装好，需要您准备好门板之后再预约安装，洗碗机门体内侧有一个二维码，请您扫码预约安装服务，还可以方便的获取使用指南，非常抱歉给您带去不便，感谢您的配合，再见。
	      */
	     
	      String prefixRespond2 = "prefix_productName_" + productNameValue + "_respond_2";
	      
	      String prefixRespond2VoiceFile = getVoiceFile(prefixRespond2);
	      
	      // 二、执行前置流程
	      String respond = execRead(prefixVoiceFile, 1, 1, 8, channel);    //播放一次，并收集客户的回复
	      if(BlankUtils.isBlank(respond) || !(respond.equals("1") || respond.equals("2"))) {      // 如果第一次客户的回复值为空或是回复值非1、2时
              
              //提示：对不起，输入有误。
              execPlayBack(inputErrorVoiceFile);
              respond = execRead(prefixVoiceFile.toString(), 1, 1, 8, channel);    //再播放一次，并收集客户的回复
          }
	      
	      //经过两次收集客户的回复,再进行判断
	      if(!BlankUtils.isBlank(respond) && respond.equals("1")) {        //如果客户回复的是1，即是环境具备，则进入下一个确认安装的流程
	          callFlow1(bshOrderList,channel,2);
	      } else {                                                         //如果客户回复为空，回复错误或是非1时，表示环境不具备
	          
	          /**
	           * 进行外呼结果反馈
	           */
	          String keyValue = BlankUtils.isBlank(respond)?"无回复":respond;
	          printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 前置流程，无回复/错误回复，keyValue=" + keyValue);
	          String preCallResult = "2";        // 2：不确认； //有前置流程，但安装环境不具备 
	          String respondResult = "10";       // 10:环境不具备
	          hangupCallAndFeedBackResult(bshOrderList, preCallResult, respondResult, keyValue, prefixRespond2VoiceFile, channel);  //提示环境不具备的语音
	          
	          return;
	      }
	    
	}
	
	/**
	 * 流程3：零售核实
	 *     您好，这里是（博世、西门子）家电客服中心。来电主要想核实您的购机情况，请问您近期有没有购买过（西门子/博世）品牌的对开门冰箱？确认购买过，请按1；没有购买过，请按2.
	 *     回复1：非常感谢您选购我们的产品，后期如有需要，请扫描机身二维码或关注“(西门子/博世)家电”微信公众号预约服务、获取使用指南。祝您生活愉快，再见。
	 *     回复2：确认没有买过，请按1，返回上级菜单，请按2。回复2：返回再播放一次；回复1：非常感谢您的反馈，我们会对相关信息做进一步核实，抱歉给你带来不便，再见。
	 *     回复错误：对不起，输入有误。我们可能会再次和您联系，再见。
	 * @param bshOrderList
	 * @param channel
	 */
	public void callFlow3(BSHOrderList bshOrderList, AgiChannel channel) {
	    
	    //int channelSourceValue = bshOrderList.getInt("CHANNEL_SOURCE");           //购物平台，1：京东；2：苏宁；3：天猫；4：国美；5：OIMS
	    int brandValue = bshOrderList.getInt("BRAND");                              //品牌，1：西门子家电； 2：博世家电
        int productNameValue = bshOrderList.getInt("PRODUCT_NAME");                 //取出产品类目
	    
        
        // 一 、组织语音文件
        
        /**
                                第一步： 收集零售核实是否购买过的Read的文件列表
                (1) 第一条语音： 
                    verify_begin_1_brand_0: 您好，这里是西门子家电客服中心。来电主要想核实您的购机情况，请问您近期有没有购买过西门子品牌的
                    verify_begin_1_brand_1: 您好，这里是博世家电客服中心。来电主要想核实您的购机情况，请问您近期有没有购买过博世品牌的
                (2) 第二条语音：产品语音
                    productName_N：洗衣机、干衣机、冰箱 ....
                (3) 第三条语音：
                    verify_begin_2: 确认购买过，请按1；没有购买过，请按2.
                    
         */
        StringBuilder beginVoiceListSb = new StringBuilder();  //零售核实开始的语音列表，主要是使用 read 收集客户的按键回复
        String verify_begin_1 = "verify_begin_1_brand_" + brandValue;     //第一条语音
        String productName = "productName_" + productNameValue;           //第二条语音
        String verify_begin_2 = "verify_begin_2";                         //第三条语音
        
        beginVoiceListSb.append(getVoiceFile(verify_begin_1));
        beginVoiceListSb.append("&");
        beginVoiceListSb.append(getVoiceFile(productName));
        beginVoiceListSb.append("&");
        beginVoiceListSb.append(getVoiceFile(verify_begin_2));
        
        /**
                                第二步：组织客户回复
                (1) 客户回复1：
                    verify_respond_1_brand_0: 非常感谢您选购我们的产品，后期如有需要，请扫描机身二维码或关注“西门子家电”微信公众号预约服务、获取使用指南。祝您生活愉快，再见。
                    verify_respond_1_brand_1: 非常感谢您选购我们的产品，后期如有需要，请扫描机身二维码或关注“博世家电”微信公众号预约服务、获取使用指南。祝您生活愉快，再见。
                (2) 客户回复2：
                    verify_respond_2: 确认没有买过，请按1，返回上级菜单，请按2。
                (3) 没有回复或是错误回复：
                    response_error_for_first_time: 对不起，输入有误。
                    verify_respond_error: 对不起，输入有误。我们可能会再次与您联系，再见。
         */
        String verify_respond_1 = "verify_respond_1_brand_" + brandValue;   //客户回复1，即是客户确实购买过。通过 PlayBack 播报给客户听。
        String verify_respond_2 = "verify_respond_2";                       //客户回复2，用于使用 Read 让客户确认是否没有购买过:确实没有买过，请按“1”，返回上级菜单，请按“2”。
        String verify_respond_error = "verify_respond_error";               //客户回复错误：对不起，输入有误。我们可能会再次与您联系，再见。
        
        String respond1VoiceFile = getVoiceFile(verify_respond_1);
        String respond2VoiceFile = getVoiceFile(verify_respond_2);
        String respondErrorVoiceFile = getVoiceFile(verify_respond_error);
        
        /**
                                  第三步：如果客户在第二步中回复2，即是没有购买过,系统将会再次与客户进行确认：确认没有买过，请按1，返回上级菜单，请按2。
                                               若客户回复的是1（即是确实没有购买过），则播放：
                (1) verify_confirm_respond_2:  非常感谢您的反馈，我们会对相关信息做进一步核实，抱歉给你带来不便，再见。  
                                              若客户回复的非1，则播放上一步中的 verify_respond_error：对不起，输入有误。我们可能会再次与您联系，再见。
         */
        String verify_confirm_respond_2 = "verify_confirm_respond_2";
        
        String confirmRespond2VoiceFile = getVoiceFile(verify_confirm_respond_2);
        
        
        // 二、执行零售核实流程
        // (1) 播放语音并接收客户的回复
            String respond = execRead(beginVoiceListSb.toString(), 1, 1, 8, channel);    //播放一次，并收集客户的回复
            if(BlankUtils.isBlank(respond) || !(respond.equals("1") || respond.equals("2"))) {      // 如果第一次客户的回复值为空或是回复值非1、2时
                
                //提示：对不起，输入有误。
                execPlayBack(inputErrorVoiceFile);
                respond = execRead(beginVoiceListSb.toString(), 1, 1, 8, channel);    //再播放一次，并收集客户的回复
            }
            
            if(BlankUtils.isBlank(respond) || !(respond.equals("1") || respond.equals("2"))) {      // 如果第二次客户的回复值为空或是回复值非1、2时
                
                /**
                 * 进行外呼结果反馈
                 */
                String keyValue = BlankUtils.isBlank(respond)?"无回复":respond;       //实际按键值
                printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 零售流程无回复/错误回复,keyValue=" + keyValue);
                String respondResult = !BlankUtils.isBlank(respond)?"5":"9";         //外呼结果，5：错误回复；9：无回复。
                hangupCallAndFeedBackResult(bshOrderList, "0", respondResult, keyValue,getVoiceFile("verify_respond_error"),channel);   //对不起，输入有误，如有必要，我们可能会再次与您联系。
                return;
            }
         
        // (2) 如果客户回复的是 1 或是 2
            if(respond.equals("1")) {     //如果返回的结果为1时，则表示客户确认购买过
                
                /**
                 * 进行外呼结果反馈
                 */
                printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 零售流程回复1，即是确认购买过。keyValue=" + respond);
                String respondResult = "11";    //如果客户回复的是1，即表示客户购买过。需要将回复结果设置为11，即是确认购买过。
                hangupCallAndFeedBackResult(bshOrderList, "0", respondResult,"1", respond1VoiceFile, channel);  //提示：非常感谢您选购我们的产品，后期如有需要，请扫描机身二维码或关注“(西门子/博世)家电”微信公众号预约服务、获取使用指南。祝您生活愉快，再见。
                
                return;
            } else if(respond.equals("2")) {      //如果返回的结果为2，则表示客户回复的是没有购买过
                
                String respond2 = execRead(respond2VoiceFile, 1, 1, 8, channel);    //播放一次：确实没有买过，请按“1”，返回上级菜单，请按“2”。并接收客户的回复
                if(BlankUtils.isBlank(respond2) || !(respond2.equals("1") || respond2.equals("2"))) {     //客户回复为非 1，2 时。
                    //提示：对不起，输入有误。
                    execPlayBack(inputErrorVoiceFile);
                    respond2 = execRead(respond2VoiceFile, 1, 1, 8, channel);      //再一次播放一次：确实没有买过，请按“1”，返回上级菜单，请按“2”。并接收客户的回复
                }
                
                if(BlankUtils.isBlank(respond2) || !(respond2.equals("1") || respond2.equals("2"))) {     //客户回复为非 1，2 时。
                    
                    /**
                     * 进行外呼结果反馈
                     */
                    String keyValue = BlankUtils.isBlank(respond2)?"无回复":respond2;       //实际按键值
                    printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 零售流程，无回复/错误回复。keyValue=" + keyValue);
                    String respondResult = !BlankUtils.isBlank(respond)?"5":"9";         //外呼结果，5：错误回复；9：无回复。
                    hangupCallAndFeedBackResult(bshOrderList, "0", respondResult, "2-" + keyValue,getVoiceFile("verify_respond_error"),channel);   ////提示：对不起，输入有误，如有必要，我们可能会再次与您联系。
                    return;
                }
                
                if(respond2.equals("2")) {          //如果客户回复为2，即是返回上级菜单，则再次执行该流程
                    printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 零售流程，选择返回上一步，系统准备返回上一步！keyValue=" + respond2);
                    callFlow3(bshOrderList, channel);
                } else if(respond2.equals("1")) {   //如果客户回复1，即是确实没有购买过
                    printLog("客户：" + bshOrderList.get("CUSTOMER_TEL") + " 零售流程，回复确认没有购买过。keyValue=" + 2);
                    String respondResult = "12";    //如果客户回复的是1，即表示客户没有购买过。需要将回复结果设置为12，即是没有购买过。
                    hangupCallAndFeedBackResult(bshOrderList, "0", respondResult, "2-1", confirmRespond2VoiceFile, channel);  //非常感谢您的反馈，我们会对相关信息做进一步核实，抱歉给你带来不便，再见。
                    return;
                }
                
            }
        
	}
	
	
	/**
	 * 取得流程1，Read命令所需语音文件名字符串
	 * 
	         开场分三种情况：
	                      开场1：您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据(京东/苏宁/国美/天猫)平台传来的信息，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
                                  开场2[OIMS平台]：您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据促销员的报单，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，改约到后面3天，请按3,如果您已经提前预约好服务，请按4。没有买过这台机器，请按5。
                                  开场3：您好，这里是(西门子/博世)家电客服中心。您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，需要您进一步确认。确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
                                   
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
          begin_3_channelSource_5：根据促销员的报单，我们将于
          begin_3_timeType_2：将于
          
          begin_4_timeType_1：上门安装
          begin_4_timeType_2：送货，我们将于送货当天上门安装，需要您进一步确认
          
          begin_5_timeType_1：确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
          begin_5_timeType_2：确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
          begin_5_timeType_1_channelSource_5：确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。没有购买过，请按5。
	 
	 * @param bshOrderList
	 * @param flag
	 *          flag: 语音的完整度。1：完整的语音列表；2：忽略部分语音内容。默认为：1。
	 *          flag==2，出现的原因主要有两个：
	 *                 （1）前置流程转入到该流程时，需要去掉前面的部分内容。
	 *                 （2）OIMS平台在选择返回上级菜单时，会去掉前面的部分内容。
	 *             
	 * @return
	 */
	public String getCallFlow1ReadVoiceFile(BSHOrderList bshOrderList,int flag) {
	    
	    int brand = bshOrderList.getInt("BRAND");                            //品牌，0：西门子；1：博世
        int channelSource = bshOrderList.getInt("CHANNEL_SOURCE");           //购物平台，1：京东；2：苏宁；3：天猫；4：国美；5:OIMS.
        int timeType = bshOrderList.getInt("TIME_TYPE");                     //日期类型，1：安装日期；2：送货日期
        int productName = bshOrderList.getInt("PRODUCT_NAME");               //产品名称
        String expectInstallDate = bshOrderList.getDate("EXPECT_INSTALL_DATE").toString();      //取出期望安装日期
        
        StringBuilder sb = new StringBuilder();
        
        /**
           flag=1 时，完整的语音内容如下：
                                    开场1：您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据(京东/苏宁/国美/天猫)平台传来的信息，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
                                     开场2[OIMS平台]：您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据促销员的报单，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，改约到后面3天，请按3,如果您已经提前预约好服务，请按4。没有买过这台机器，请按5。
                                     开场3：您好，这里是(西门子/博世)家电客服中心。您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，需要您进一步确认。确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
           flag=2 时，需要去掉前面一句。
                                    开场1需要去掉：您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。  
                                                       保留：根据(京东/苏宁/国美/天猫)平台传来的信息，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
                                    开场2需要去掉：您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。
                                                       保留：根据促销员的报单，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，改约到后面3天，请按3,如果您已经提前预约好服务，请按4。没有买过这台机器，请按5。
                                    开场3需要去掉：您好，这里是(西门子/博世)家电客服中心。
                                                       保留：您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，需要您进一步确认。确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。                                                                                                                                                                                                      
         */
        
        /**
         * 三个开场看似比较复杂，其实前两个开场是可以整合为一个,即主要的开场就是两种
         *        
         * 两种开场，主要是通过时间类型来区分，时间类型为送货日期时，只针对国美平台开放
         *   
         */
        if(timeType == 1) {    //时间类型为安装日期时，主要是针对 5大平台的安装日期的调查
            
            if(flag == 1) {    // 如果 flag==1 时，第一句是加入的。如果 flag==2时，第一句需要忽略。
                /**
                 * 第一句：您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。  
                 */
                          /**
                           (1)
                           begin_1_brand_0_timeType_1：您好，这里是西门子家电客服中心，来电跟您确认
                           begin_1_brand_1_timeType_1：您好，这里是博世家电客服中心，来电跟您确认
                           */
                          String begin_1 = "begin_1_brand_" + brand + "_timeType_" + timeType;
                          sb.append(getVoiceFile(begin_1));
                          
                          /**
                           (2)
                           productName_1:洗衣机; 
                           productName_2:干衣机；
                                                                         产品名称(1.洗衣机2. 干衣机3.冰箱4.酒柜5.吸油烟机6. 灶具7.消毒柜8.洗碗机9.微波炉10.烤箱11.咖啡机12.暖碟抽屉13.洗干一体机14.蒸箱15.饮水机16.电热水器17.对开门冰箱18.多门冰箱19蒸/烤箱)
                           */
                          sb.append("&");
                          sb.append(getVoiceFile("productName_" + productName));      //产品XXX
                          
                          /**
                           (3)
                           begin_2_timeType_1：的安装日期；
                           */
                          sb.append("&");
                          sb.append(getVoiceFile("begin_2_timeType_1"));              //的安装日期
            }    
             /**
              * 第二句：
              *     京东/苏宁/国美/天猫 平台：根据(京东/苏宁/国美/天猫)平台传来的信息，我们将于(明天/12月10号)上门安装。
              *               OIMS平台：根据促销员的报单，我们将于(明天/12月10号)上门安装。
              */
                      /**
                      (1) 
                      begin_3_channelSource_1：根据京东平台传来的信息，我们将于
                      begin_3_channelSource_2：根据苏宁平台传来的信息，我们将于
                      begin_3_channelSource_3：根据天猫平台传来的信息，我们将于
                      begin_3_channelSource_4：根据国美平台传来的信息，我们将于
                      begin_3_channelSource_5：根据促销员的报单，我们将于
                     */
                     sb.append("&");
                     sb.append(getVoiceFile("begin_3_channelSource_" + channelSource));     
                     
                     /**
                      (2)明天或是日期
                      tomorrow: 明天
                      month_01: 1月; month_02: 2月   其他的以此类推
                      day_01: 1日； day_02： 2日  其他的以此类推
                      */
                     boolean b = checkInstallDateIsNextDay(expectInstallDate);
                     if(b) {
                         sb.append("&");
                         sb.append(getVoiceFile("tomorrow"));
                     } else {
                         Date installDate = DateFormatUtils.parseDateTime(expectInstallDate, "yyyy-MM-dd");
                         String monthStr = DateFormatUtils.formatDateTime(installDate, "MM");
                         String dayStr = DateFormatUtils.formatDateTime(installDate,"dd");
                         String voiceNameForMonth = "month_" + monthStr;
                         String voiceNameForDay = "day_" + dayStr;
                         
                         sb.append("&");
                         sb.append(getVoiceFile(voiceNameForMonth));
                         sb.append("&");
                         sb.append(getVoiceFile(voiceNameForDay));
                     }
                     
                     /**
                      (3)
                      begin_4_timeType_1：上门安装
                      */
                     sb.append("&");
                     sb.append(getVoiceFile("begin_4_timeType_1"));
             /**
             * 第三句：
                                                    确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
                                                    确认请按1，暂不安装请按2，改约到后面3天，请按3,如果您已经提前预约好服务，请按4。没有买过这台机器，请按5。                                   
             */
                     /**
                      (1)
                      begin_5_timeType_1: 确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。
                      begin_5_timeType_1_channelSource_5：确认请按1，暂不安装请按2，改约到后面3天，请按3,如果您已经提前预约好服务，请按4。没有买过这台机器，请按5。  
                      */
                     if(channelSource == 5) {
                         sb.append("&");
                         sb.append(getVoiceFile("begin_5_timeType_1_channelSource_5"));
                     } else {
                         sb.append("&");
                         sb.append(getVoiceFile("begin_5_timeType_1"));
                     }
                   
            
        } else if(timeType == 2 && channelSource == 4) {
            
            //您好，这里是(西门子/博世)家电客服中心。您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，需要您进一步确认。确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。
            if(flag == 1) {    // 如果 flag==1 时，第一句是加入的。如果 flag==2时，第一句需要忽略。
                /**
                 * 第一句：
                 *   您好，这里是(西门子/博世)家电客服中心。
                 */
                /**
                     (1)
                     begin_1_brand_0_timeType_2:您好，这里是西门子家电客服中心.
                     begin_1_brand_1_timeType_2:您好，这里是博世家电客服中心.
                 */
                sb.append(getVoiceFile("begin_1_brand_" + brand + "_timeType_" + timeType));
            }
            
            /**
             * 第二句：
             *   您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，需要您进一步确认。
             */
                    /**
                     (1)
                     begin_2_timeType_2:您在国美选购的
                     */
                    sb.append("&");
                    sb.append(getVoiceFile("begin_2_timeType_" + timeType));
                    
                    /**
                     (2)
                     begin_3_timeType_2:将于
                     */
                    sb.append("&");
                    sb.append(getVoiceFile("begin_3_timeType_2"));
                    
                     /**
                      (3)明天或是日期
                      tomorrow: 明天
                      month_01: 1月; month_02: 2月   其他的以此类推
                      day_01: 1日； day_02： 2日  其他的以此类推
                      */
                     boolean b = checkInstallDateIsNextDay(expectInstallDate);
                     if(b) {
                         sb.append("&");
                         sb.append(getVoiceFile("tomorrow"));
                     } else {
                         Date installDate = DateFormatUtils.parseDateTime(expectInstallDate, "yyyy-MM-dd");
                         String monthStr = DateFormatUtils.formatDateTime(installDate, "MM");
                         String dayStr = DateFormatUtils.formatDateTime(installDate,"dd");
                         String voiceNameForMonth = "month_" + monthStr;
                         String voiceNameForDay = "day_" + dayStr;
                         
                         sb.append("&");
                         sb.append(getVoiceFile(voiceNameForMonth));
                         sb.append("&");
                         sb.append(getVoiceFile(voiceNameForDay));
                     }
                     
                     /**
                      (4)
                      begin_4_timeType_2: 送货，我们将于送货当天上门安装，需要您进一步确认
                      */
                     sb.append("&");
                     sb.append(getVoiceFile("begin_4_timeType_2"));
                 
              /**
               * 第三句：
               * 确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。  
               */
                    /**
                     (1)
                     begin_5_timeType_2:确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。  
                     */
                     sb.append("&");
                     sb.append(getVoiceFile("begin_5_timeType_2"));
        }
        
        return sb.toString();
	}
	
	/**
	 * 获取客户回复1（确认安装）的播放列表
	 *     场景1：京东、苏宁、天猫、OIMS
                                                您的机器安装日期已确认为12月10号，工程师最迟会在当天早上9:30之前与您联系具体上门时间。感谢您的配合，再见。
                                场景2：国美
                                                您的机器安装日期已确认为12月10号，工程师最迟会在当天早上9:30之前与您联系具体上门时间。为确保您的权益，请认准(西门子/博世)厂家的专业工程师。感谢您的配合，再见。
                                                
           respond_1_1：您的机器安装日期已确认为
           respond_1_2_timeType_1：工程师最迟会在当天早上9点半之前与您联系具体上门时间，感谢您的配合，再见。
           respond_1_2_timeType_2_brand_0：工程师最迟会在当天早上9点半之前与您联系具体上门时间，为确保您的权益，请认准西门子厂家的专业工程师，感谢您的配合，再见。
           respond_1_2_timeType_2_brand_1：工程师最迟会在当天早上9点半之前与您联系具体上门时间，为确保您的权益，请认准博世厂家的专业工程师，感谢您的配合，再见。
           
	 * @param bshOrderList
	 * @return
	 */
	public String getRespond1PlayList(BSHOrderList bshOrderList) {
	    
	    StringBuilder sb = new StringBuilder();
	    
	    int timeType = bshOrderList.getInt("TIME_TYPE");    //日期类型，1：安装日期；2：送货日期
	    int brand = bshOrderList.getInt("BRAND");                            //品牌，0：西门子；1：博世
	    
	    /**
	     * (1) 您的机器安装日期已确认为
	     */
	    sb.append(getVoiceFile("respond_1_1"));
	    
	    /**
	     * (2) 安装日期：如12月10号
	     */
	    String expectInstallDate = bshOrderList.getDate("EXPECT_INSTALL_DATE").toString();      //取出期望安装日期
        Date installDate = DateFormatUtils.parseDateTime(expectInstallDate, "yyyy-MM-dd");
        String monthStr = DateFormatUtils.formatDateTime(installDate, "MM");
        String dayStr = DateFormatUtils.formatDateTime(installDate,"dd");
        
        String voiceNameForMonth = "month_" + monthStr;
        String voiceNameForDay = "day_" + dayStr;
        
        sb.append("&");
        sb.append(getVoiceFile(voiceNameForMonth));
        sb.append("&");
        sb.append(getVoiceFile(voiceNameForDay));
        
        /**
         * (3) 
           respond_1_2_timeType_1：工程师最迟会在当天早上9点半之前与您联系具体上门时间，感谢您的配合，再见。
           respond_1_2_timeType_2_brand_0：工程师最迟会在当天早上9点半之前与您联系具体上门时间，为确保您的权益，请认准西门子厂家的专业工程师，感谢您的配合，再见。
           respond_1_2_timeType_2_brand_1：工程师最迟会在当天早上9点半之前与您联系具体上门时间，为确保您的权益，请认准博世厂家的专业工程师，感谢您的配合，再见。
         */
        if(timeType == 1) {
            sb.append("&");
            sb.append(getVoiceFile("respond_1_2_timeType_1"));
        } else {
            sb.append("&");
            sb.append(getVoiceFile("respond_1_2_timeType_2_brand_" + brand));
        }
        
        return sb.toString();
	    
	}
	
	
	/**
	 * 执行 Read 操作，用于播放语音文件列表，并收集客户回复的结果，输入按键可打断播放语音
	 * 
	 * @param voiceFileList
	 *             文件列表：多个文件时使用 & 连接
	 * @param maxDigits
	 *             最大输入几位数字
	 * @param maxTimes
	 *             最大的播放次数，如果在超时时间内没有输入任何按键回复时，系统会再次播放，最低为1次
	 * @param timeout
	 *             超时时间（单位：秒），等待用户输入的时间
	 * @param channel
	 *             通道
	 * @return
	 */
	public String execRead(String voiceFileList,int maxDigits,int maxTimes,int timeout,AgiChannel channel) {
	    
	    String respond = null;
	    try {
            exec("Read","respond," + voiceFileList + "," + maxDigits + ",," + maxTimes + "," + timeout);
            respond = channel.getVariable("respond");    //取得客户的回复结果
        } catch (AgiException e) {
            e.printStackTrace();
        }
	    return respond;
	}
	
	/**
	 * 执行 PlayBack 操作，用于播放语音文件列表
	 * 
	 * @param voiceFileList
	 *             文件列表：多个文件时使用 & 连接
	 */
	public void execPlayBack(String voiceFileList) {
	    try {
            exec("PlayBack",voiceFileList);
        } catch (AgiException e) {
            e.printStackTrace();
        }
	}
	
	/**
	 * 从 BSHVoiceConfig 的配置对象中，取出完整文件名：路径 + 文件名
	 * 
	 * 注：系统在启动时，会将bsh_voice表中所有的文件名都加载到内存，所以通过文件名，就可以直接从内存中取出完整文件名
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getVoiceFile(String fileName) {
	    
	    String voicePath = BSHCallParamConfig.getVoicePathSingle();   //取出配置的语音文件（单声道）路径
	    String voiceFile = null;
	    
	    if(BSHVoiceConfig.getVoiceMap().containsKey(fileName)) {
	        voiceFile = voicePath + "/" + BSHVoiceConfig.getVoiceMap().get(fileName);
        }
	    
	    return voiceFile;
	}
	
	 /**
     * 检查安装/送货日期是否为第二天
     * 
     *      如果送货日期为第二天时，播报时，就以明天来代替日期。否则就直接播放日期。
     * 
     * @param expectInstallDate
     *              安装/送货 日期，格式：yyyy-MM-dd,如:2018-12-10
     * 
     * @return
     *      是：返回true; 否: 返回 false
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
    
    /**
     * 判断客户输入的回复按键是否为错误
     *      条件如下：
     *          （1）回复值为空，即是无回复时，即表示回复错误
     *          （2）回复值不为空，但是回复值不为1、2、3、4(京东、天猫、苏宁、国美) 或是 1、2、3、4、5（OIMS平台），也表示客户输入错误
     * @param bshOrderList
     * @param respond
     * @return
     */
    public boolean isRespondError(BSHOrderList bshOrderList,String respond) {
        
        if(BlankUtils.isBlank(respond)) {
            return false;
        }
        
        int channelSource = bshOrderList.getInt("CHANNEL_SOURCE");           //购物平台，1：京东；2：苏宁；3：天猫；4：国美；5:OIMS.
        if(channelSource == 5) {
            
            if(!(respond.equals("1") || respond.equals("2") || respond.equals("3") || respond.equals("4") || respond.equals("5"))) {
                return false;
            }
            
        } else {
            
            if(!(respond.equals("1") || respond.equals("2") || respond.equals("3") || respond.equals("4"))) {
                return false;
            }
        }
        
        return true;
        
    }
    
    /**
     * 反馈外呼结果方法
     * 
     * @param bshOrderList
     * @param respondResult
     *             1:确认建单->即是确认安装
     *             2:暂不安装
     *             3:短信确认->即是延后安装
     *             4:工程师电话确认->即是提前预约
     *             5:错误回复->
     *             6:放弃呼叫
     *             7:已过期
     *             8:外呼失败
     *             9:无回复
     *             10:环境不具备
     *             11:确认购买过
     *             12:没有购买过 
     * @param preCallResult
     *             前置流程外呼结果，
     *                  0：没有前置；          //指没有前置流程
     *                  1：确认；                  //有前置流程，且安装环境确认已经具备
     *                  2：不确认；              //有前置流程，但安装环境不具备
     *                  3：未接听；             //没有接听
     */
    public static void feedBackCallResult(BSHOrderList bshOrderList,String respondResult,String preCallResult) {
        
        int outboundType = bshOrderList.getInt("OUTBOUND_TYPE");             //取出外呼类型，1：确认安装；2：零售核实
        
        BSHHttpRequestThread httpRequest = new BSHHttpRequestThread(bshOrderList.get("ID").toString(),bshOrderList.getStr("ORDER_ID"), "1",preCallResult,respondResult,String.valueOf(outboundType));
        
        Thread httpRequestThread = new Thread(httpRequest);
        
        httpRequestThread.start();
    }
    
    /**
     * 保存外呼结果到数据库
     * 
     * @param bshOrderList
     *          订单对象
     * @param respondResult
     *             1:确认建单->即是确认安装
     *             2:暂不安装
     *             3:短信确认->即是延后安装
     *             4:工程师电话确认->即是提前预约
     *             5:错误回复->
     *             6:放弃呼叫
     *             7:已过期
     *             8:外呼失败
     *             9:无回复
     *             10:环境不具备
     *             11:确认购买过
     *             12:没有购买过 
     * @param keyValue 
     *             客户回复的真实按键值
     * @param channel
     */
    public static void saveCallResult(BSHOrderList bshOrderList,String respondResult,String keyValue,AgiChannel channel) {
        
        try {
            String bshOrderListId = bshOrderList.get("ID").toString();                          //订单ID值
            int billsec = Integer.valueOf(channel.getVariable("CDR(billsec)"));                 //通话时长
            
            BSHOrderList.dao.updateBSHOrderListRespondAndBillsec(bshOrderListId, respondResult,billsec,keyValue);    //执行保存
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (AgiException e) {
            e.printStackTrace();
        }     
        
    }
    
    /**
     * 挂断通话，并返回结果
     * 
     * 主要在这里执行三个操作：
     *      （1）将外呼结果和通话时长存储到数据库
     *      （2）播放指定的播放列表
     *      （3）将外呼结果反馈给DOB系统提供的接口
     * 
     * @param bshOrderList
     *             订单对象 
     * @param preCallResult
     *             前置流程外呼结果，
     *                  0：没有前置；          //指没有前置流程
     *                  1：确认；                  //有前置流程，且安装环境确认已经具备
     *                  2：不确认；              //有前置流程，但安装环境不具备
     *                  3：未接听；             //没有接听
     * @param respondResult
     *             1:确认建单->即是确认安装
     *             2:暂不安装
     *             3:短信确认->即是延后安装
     *             4:工程师电话确认->即是提前预约
     *             5:错误回复->
     *             6:放弃呼叫
     *             7:已过期
     *             8:外呼失败
     *             9:无回复
     *             10:环境不具备
     *             11:确认购买过
     *             12:没有购买过 
     * @param keyValue
     *             客户回复的真实按键值
     * @param voiceFileList
     * 
     *             
     */
    public void hangupCallAndFeedBackResult(BSHOrderList bshOrderList,String preCallResult,String respondResult,String keyValue,String voiceFileList,AgiChannel channel) {
        
        //(1) 将外呼结果和通话时长存储到数据库
        saveCallResult(bshOrderList, respondResult, keyValue, channel);
        
        //(2) 播放指定的播放列表
        if(!BlankUtils.isBlank(voiceFileList)) {
            execPlayBack(voiceFileList);
        }
        
        //(3) 将外呼结果反馈给DOB接口
        feedBackCallResult(bshOrderList, respondResult, preCallResult);
        
    }
    
    
    /**
     * 打印日志，主要是有两个地方要输出日志
     *      (1) Tomcat 日志
     *      (2) Asterisk 日志
     * @param log
     */
    public void printLog(String log) {
        try {
            exec("Noop",log);
            StringUtil.log(this,log);
        } catch (AgiException e) {
            e.printStackTrace();
        }
    }
	
}
