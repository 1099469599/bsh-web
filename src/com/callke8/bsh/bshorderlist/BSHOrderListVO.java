package com.callke8.bsh.bshorderlist;

import com.callke8.utils.BlankUtils;
import com.jfinal.core.Controller;

import net.sf.json.JSONObject;

/**
 * 订单数据 VO 实例类, 用于处理提交并接收的数据，存储在该实体类
 * 
 * @author 黄文周
 */
public class BSHOrderListVO {

	/**
	 * 订单ID
	 */
	private String orderId;   
	
	/**
	 * 客户姓名
	 */
	private String customerName;
	
	/**
	 * 客户号码
	 */
	private String customerTel;
	
	/**
	 * 产品名称,现在主要包含19个产品：
	 * 
	 * 1：洗衣机;
	 * 2：干衣机;
	 * 3：冰箱;
	 * 4：酒柜;
	 * 5：吸油烟机;
	 * 6：灶具;
	 * 7：消毒柜;
	 * 8：洗碗机;
	 * 9：微波炉;
	 * 10：烤箱;
	 * 11：咖啡机;
	 * 12：暖碟抽屉;
	 * 13： 洗干一体机;
	 * 14：蒸箱;
	 * 15：饮水机;
	 * 16：电热水器;
	 * 17：对开门冰箱;
	 * 18：多门冰箱;
	 * 19：蒸/烤箱
	 * 
	 */
	private String productName;
	
	/**
	 * 时间类型, 主要用于区分 expectInstallDate 的时间类型，  1：安装日期（默认）   2：送货时间
	 */
	private String timeType;
	
	/**
	 * 预约安装的日期
	 */
	private String expectInstallDate;
	
	/**
	 * 产品品牌，现在是两个品牌， 0：西门子（默认）;  1：博世
	 */
	private String brand;
	
	/**
	 * 销售平台渠道，1：京东（默认）; 2:苏宁; 3：天猫; 4:国美
	 */
	private String channelSource;
	
	/**
	 * 是否有前置流程,  	1、有前置流程->即是需要在主外呼流程前加一个前置流程，主用是用于安装环境的确认，暂时涉及到两个产品，分是灶具（产品编号为 6）和 洗碗机（产品编号为 8）
	 * 			   	2、没有前置流程 直接执行主流程
	 */
	private String isConfirm;
	
	/**
	 * 外呼类型， 1：确认安装外呼数据类型；2：零售核实类外呼数据
	 */
	private String outboundType;

	/**
	 * 根据 jsonContent 创建一个 BSHOrderListVO 实例
	 * 
	 * @param jsonContent
	 * @return
	 */
	public static BSHOrderListVO newInstance(String jsonContent) {
		
		BSHOrderListVO orderList = null;
		
		if(!BlankUtils.isBlank(jsonContent) && jsonContent.length() > 40) {     //必须保证 json 的数理长度大于 40 位
			
			orderList = new BSHOrderListVO();
			
			JSONObject paramJson = JSONObject.fromObject(jsonContent);
			boolean containTimeType = paramJson.containsKey("timeType");    //是否包含日期类型
			
			String orderId = null;
			String customerName = null;
			String customerTel = null;
			String productName = null;
			String timeType = null;
			String expectInstallDate = null;
			String brand = null;
			String channelSource = null;
			String isConfirm = null;
			String outboundType = null;
			
			if(paramJson.containsKey("orderId")) {
				orderId = String.valueOf(paramJson.get("orderId"));
			}
			
			if(paramJson.containsKey("customerName")) {
				customerName = String.valueOf(paramJson.get("customerName"));
			}
			if(paramJson.containsKey("customerTel")) {
				customerTel = String.valueOf(paramJson.get("customerTel"));
			}
			if(paramJson.containsKey("productName")) {
				productName = String.valueOf(paramJson.get("productName"));
			}
			
			if(paramJson.containsKey("timeType")) {               //日期类型，1：安装日期；2：送货日期（国美独有）
				timeType = String.valueOf(paramJson.get("timeType"));
			}else {
				timeType = "1";    				//如果 timeType 为空时，设置一个默认值
			}
			
			
			if(paramJson.containsKey("expectInstallDate")) {
				expectInstallDate = String.valueOf(paramJson.get("expectInstallDate"));
			}
			if(paramJson.containsKey("brand")) {
				brand = String.valueOf(paramJson.get("brand"));
			}
			if(paramJson.containsKey("channelSource")) {
				channelSource = String.valueOf(paramJson.get("channelSource"));
			}
			if(paramJson.containsKey("isConfirm")) {
				isConfirm = String.valueOf(paramJson.get("isConfirm"));
				if(isConfirm.equals("1")) {      //如果 isConfirm 的状态为1时，还要再判断传入的产品 是否为 灶具或是洗碗机，如果不是这两个产品类目，也需要强制将 isConfirm 修改为2
					if(!BlankUtils.isBlank(productName)) {
						if(!productName.equals("6") && !productName.equals("8")) {     //如果产品不为灶具或洗碗机，则强制设置为 2
							isConfirm = "2";
						}
					}
				} else {
					isConfirm = "2";   	 		//如果 isConfirm 的状态不为1，即是不带前置流程，则强制设置为2
				}
			}else {
				isConfirm = "2";                  //给定一个默认值为2，即是不带前置流程
			}
			
			//外呼类型,1:确认安装（默认值）；2：零售核实；
			if(paramJson.containsKey("outboundType")) {   //是否包含outBoundType
			    outboundType = String.valueOf(paramJson.get("outboundType"));
			    if(BlankUtils.isBlank(outboundType) || !outboundType.equals("2")) {  //如果为2时，直接为2，即是这是零售核实数据。如果非2时，则表示这是确认安装数据。
			        outboundType = "1";
			    }
			} else {
			    outboundType = "1";
			}
			
			orderList.setOrderId(orderId);
			orderList.setCustomerName(customerName);
			orderList.setCustomerTel(customerTel);
			orderList.setProductName(productName);
			orderList.setTimeType(timeType);
			orderList.setExpectInstallDate(expectInstallDate);
			orderList.setBrand(brand);
			orderList.setChannelSource(channelSource);
			orderList.setIsConfirm(isConfirm);
			orderList.setOutboundType(outboundType);
			
		}
		
		return orderList;
	}
	
	/**
	 * 根据传入的 controller创建对象, 主要是通过 controller 的 getParam() 创建对象
	 * 
	 * @param controller
	 * @return
	 */
	public static BSHOrderListVO newInstance(Controller controller) {
		
		BSHOrderListVO orderListVO = new BSHOrderListVO();
		boolean containTimeType = !BlankUtils.isBlank(controller.getPara("timeType"));    //是否包含日期类型
		
		orderListVO = new BSHOrderListVO();
		orderListVO.setOrderId(controller.getPara("orderId"));
		orderListVO.setCustomerName(controller.getPara("customerName"));
		orderListVO.setCustomerTel(controller.getPara("customerTel"));
		String productName = controller.getPara("productName");
		orderListVO.setProductName(productName);
		if(containTimeType) {
			orderListVO.setTimeType(controller.getPara("timeType"));
		}else {
			orderListVO.setTimeType("1");      //如果传入的参数 timeType 为空时，设置一个默认值
		}
		orderListVO.setExpectInstallDate(controller.getPara("expectInstallDate"));
		orderListVO.setBrand(controller.getPara("brand"));
		orderListVO.setChannelSource(controller.getPara("channelSource"));
		String isConfirm = controller.getPara("isConfirm");
		if(!BlankUtils.isBlank(isConfirm) && isConfirm.equals("1")) {    
			//如果 isConfirm 的状态为1时，还要再判断传入的产品 是否为 灶具或是洗碗机，如果不是这两个产品类目，也需要强制将 isConfirm 修改为2
			if(!BlankUtils.isBlank(productName)) {
				if(!productName.equals("6") && !productName.equals("8")) {     //如果产品不为灶具或洗碗机，则强制设置为 2
					isConfirm = "2";
				}
			}
			
		}else {          //如果不为1，则强制设置为2
			isConfirm = "2";
		}
		orderListVO.setIsConfirm(isConfirm);
		
		String outboundType = controller.getPara("outboundType");
		if(BlankUtils.isBlank(outboundType) || !outboundType.equals("2")) {
		    outboundType = "1";
		}
		orderListVO.setOutboundType(outboundType);
		
		return orderListVO;
	}
	
	/**
	 * 检查数据的完整性
	 * 
	 * 如果有完信息，则返回失败原因
	 * 
	 * @param orderListVO
	 * @return
	 */
	public static String checkBSHOrderListVO(BSHOrderListVO orderListVO) {
		
		String msg = null;
		
		if(BlankUtils.isBlank(orderListVO.getOrderId())) {
			msg = "订单编号为空!";
		}else if(BlankUtils.isBlank(orderListVO.getCustomerName())) {
			msg = "客户姓名为空!";
		}else if(BlankUtils.isBlank(orderListVO.getCustomerTel())) {
			msg = "客户号码为空!";
		}else if(BlankUtils.isBlank(orderListVO.getExpectInstallDate())) {
			msg = "计划安装日期为空!";
		}else if(BlankUtils.isBlank(orderListVO.getBrand())) {
			msg = "品牌信息为空!";
		}else if(BlankUtils.isBlank(orderListVO.getProductName())) {
			msg = "产品信息为空!";
		}else if(BlankUtils.isBlank(orderListVO.getChannelSource())) {
			msg = "购物平台信息为空!";
		}else if(BlankUtils.isBlank(orderListVO.getIsConfirm())) {
			msg = "前置标记信息为空!";
		}else if(BlankUtils.isBlank(orderListVO.getOutboundType())) {
            msg = "订单数据外呼类型为空!";
        }else if(orderListVO.getOutboundType().equals("2") && !orderListVO.getChannelSource().equals("5")) {
            //如果客户提交的数据的外呼类型为 2 ,即是零售核实类外呼，但是购买平台又不是5，即是非 OIMS 平台时，提示提交数据失败。
            msg = "提交的数据的外呼类型为零售核实(outboundType=2)类外呼，零售核实类外呼仅针对 OIMS平台(channelSource=5)开放！";
        }else if(orderListVO.getTimeType().equals("2") && !orderListVO.getChannelSource().equals("4")) {
            //如果客户提交的数据的时间类型为2，即是送货时间，但是购物平台又不是是，即是非国美平台时，提示提交数据失败。
            msg = "提交的数据的时间类型（timeType=2）为送货时间,时间类型为送货时间只对国美平台（channelSource=4）开放！";
        }
		
		if(!BlankUtils.isBlank(msg)) {
			msg = "接收提交订单数据失败,失败原因：" + msg + "!";
		}
		
		return msg;
	}
	

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getCustomerTel() {
		return customerTel;
	}

	public void setCustomerTel(String customerTel) {
		this.customerTel = customerTel;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getTimeType() {
		return timeType;
	}

	public void setTimeType(String timeType) {
		this.timeType = timeType;
	}

	public String getExpectInstallDate() {
		return expectInstallDate;
	}

	public void setExpectInstallDate(String expectInstallDate) {
		this.expectInstallDate = expectInstallDate;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getChannelSource() {
		return channelSource;
	}

	public void setChannelSource(String channelSource) {
		this.channelSource = channelSource;
	}

	public String getIsConfirm() {
		return isConfirm;
	}

	public void setIsConfirm(String isConfirm) {
		this.isConfirm = isConfirm;
	}

    public String getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(String outboundType) {
        this.outboundType = outboundType;
    }
	
}
