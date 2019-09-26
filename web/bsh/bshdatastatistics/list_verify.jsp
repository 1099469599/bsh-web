<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>博西数据统计</title>
	<style>
		.font17{
			font-size: 17px;
		}
	</style>
	<link rel="stylesheet" type="text/css" href="themes/default/easyui.css">
	<link rel="stylesheet" type="text/css" href="themes/color.css">
	<link rel="stylesheet" type="text/css" href="themes/icon.css">
	<link rel="stylesheet" type="text/css" href="demo.css">
	<link rel="stylesheet" type="text/css" href="jplayer/dist/skin/blue.monday/css/jplayer.blue.monday.hwzcustom.css">
	<link rel="stylesheet" type="text/css" href="iconfont/iconfont.css">
	<script src="echarts/echarts.min.js"></script><!-- 引入Echarts JS文件 -->
	<script src="iconfont/iconfont.js"></script>
	<script type="text/javascript" src="jquery.min.js"></script>
	<script type="text/javascript" src="jquery.easyui.min.js"></script>
	<script type="text/javascript" src="js.date.utils.js"></script>
    <script type="text/javascript" src="jplayer/dist/jplayer/jquery.jplayer.hwzcustom.js"></script>
    <script type="text/javascript" src="locale/easyui-lang-zh_CN.js"></script>
    
    <script type="text/javascript">
    
        var myChart = null;
        var startTime = null;
        var endTime = null;
       	var conditionState = null;
       	var conditionRespond = null;
        
        //各项数据的数据量及占比
        var totalCount = 0;
        var totalRate = 100;
        var state1Count = 0;
        var state1Rate = 0;
        var state2Count = 0;
        var state2Rate = 0;
        var state3Count = 0;
        var state3Rate = 0;
        var state4Count = 0;
        var state4Rate = 0;
        var state5Count = 0;
        var state5Rate = 0;
        var state6Count = 0;
        var state6Rate = 0;
   
        var respond11Count = 0;    //确认购买过
        var respond11Rate = 0;      
        var respond12Count = 0;    //没有购买过
        var respond12Rate = 0;
        var respond5Count = 0;     //错误回复
        var respond5Rate = 0;
        var respond9Count = 0;     //无回复
        var respond9Rate = 0;
        
    
    	$(function(){
    		//初始化搜索日期
    		$('#startTime').datetimebox('setValue',getCurrDate() + ' 00:00:00');
    		$('#endTime').datetimebox('setValue',getDateAfter(1) + ' 00:00:00');
    		
    		var channelSourceComboboxDataFor1 = eval('${channelSourceComboboxDataFor1}');
    		
    		//购物平台Combobox
    		$("#channelSource").combobox({    
				valueField:'id',
    			textField:'text',
    			panelHeight:'auto'
			}).combobox('loadData',channelSourceComboboxDataFor1).combobox('setValue',"empty");
    		
    		$("#timeInterval").combobox({
    			onChange:function(newValue,oldValue){
    				//alert("newValue:" + newValue + ";oldValue:" + nv + ";getDateBefore:" + getDateBefore(newValue));
    				$('#startTime').datetimebox('setValue',getDateBefore(newValue-1) + ' 00:00:00');
    				$('#endTime').datetimebox('setValue',getDateAfter(1) + ' 00:00:00');
    			}
    		}).combobox('setValue','1');
    		
    		reloadStatistics();
    		
    		$("#bshOrderListDg").datagrid({
    			pageSize:30,
    			pagination:true,
    			fit:true,
    			rowrap:true,
    			striped:true,
    			pageList:[10,20,30],
    			url:'bshOrderList/datagrid',
    			toolbar:'#orderListDgTool',
    			queryParams:{
    				orderId:null,
    				channelSource:$('#channelSource').combobox('getValue'),
    				customerName:null,
    				customerTel:null,
    				brand:null,
    				productName:null,
    				state:-1,
    				respond:-1,
    				outboundType:-1,
    				startTime:$("#startTime").datetimebox('getValue'),
    				endTime:$("#endTime").datetimebox('getValue'),
    				dateTimeType:1
    			}
    		});
    		
    		$("#orderListDlg").dialog({
    			onClose:function() {
    				$('#bshOrderListDg').datagrid('loadData',{total:0,rows:[]});
    			}
    		});
    		
    		
    		
    	});
    	
    	function findData() {
    		$("#bshOrderListDg").datagrid('reload',{
    			orderId:null,
    			channelSource:$('#channelSource').combobox('getValue'),
				customerName:null,
				customerTel:null,
				brand:null,
				productName:null,
				state:conditionState,
				respond:conditionRespond,
				outboundType:2,
				startTime:$("#startTime").datebox('getValue'),
				endTime:$("#endTime").datebox('getValue'),
				dateTimeType:1
    		});
    	}
    	
    	//重载统计数据
    	function reloadStatistics() {
    		totalCount = 0;
    		startTime = $("#startTime").datetimebox('getValue');
    		endTime = $("#endTime").datetimebox('getValue');
    		channelSource = $('#channelSource').combobox('getValue');
    		$.messager.progress({
				msg:'系统正在处理，请稍候...',
				interval:3000
			});
    		
    		$.ajax({

				url:'bshVerifyDataStatistics/reloadStatistics?startTime=' + startTime + '&endTime=' + endTime + '&channelSource=' + channelSource,
				method:'post',
				dataType:'json',
				success:function(rs) {
					$.messager.progress("close");
					var legendData = [];
					var seriesData1 = [];
					var seriesData2 = [];
					var j = 0;
					var k = 0;
					
					for(var i=0;i<rs.length;i++) {
						legendData[i]=rs[i].name;  //将数据推给定义的数组对象
						var name = rs[i].name;
						
						var map = {};
						map.name = rs[i].name;
						map.value = rs[i].value;
						
						if(name=='已载入' || name=='已成功' || name=='待重呼' || name=='已失败' || name=='已过期' || name=='放弃呼叫') {
							totalCount += rs[i].value;
							seriesData1[j] = map;
							j++;
						}
						
						if(name=='已载入' || name=='确认购买过' || name=='没有购买过' || name=='错误回复' || name=='无回复' || name=='待重呼' || name=='已失败' || name=='已过期' || name=='放弃呼叫') {
							seriesData2[k] = map;
							k++;
						}
					
					}

					myChart.setOption({
						title:{
							subtext:'时间区间:' + startTime + '至 ' + endTime,
						},
						legend:{
							data:legendData,
							textStyle: {
								fontSize:14
							},
							formatter: function(name) {
								var pvV;
								var ppV;
								var space = null;
								
								if(name=='已载入') {   pvV = state1Count;  ppV = state1Rate;   space='       '}
								else if(name=='已成功') {pvV = state2Count;  ppV = state2Rate; space='       '}
								else if(name=='待重呼') {pvV = state3Count;  ppV = state3Rate; space='        '}
								else if(name=='已失败') {pvV = state4Count;  ppV = state4Rate; space='        '}
								else if(name=='已过期') {pvV = state5Count;  ppV = state5Rate; space='        '}
								else if(name=='放弃呼叫') {pvV = state6Count;  ppV = state6Rate; space='     '}
								else if(name=='确认购买过') {pvV = respond11Count;  ppV = respond11Rate;space='    '}
								else if(name=='没有购买过') {pvV = respond12Count;  ppV = respond12Rate; space='    '}
								else if(name=='错误回复') {pvV = respond5Count;  ppV = respond5Rate; space='   '}
								else if(name=='无回复') {pvV = respond9Count;  ppV = respond9Rate; space='   '}
								return  name + space + "(占比 ：" + ppV + "%)";
							}
						},
						series:[{
							data:seriesData1
						},{
							data:seriesData2
						}]
					});
					
					//填充汇总数据
					$('#summaryDg').datagrid('loadData',getSummaryData());
					
					//修改条目中的数据
					
					
					if(totalCount == 0) {
						window.parent.showMessage("温馨提示：时间区间内，暂未查询到数据，数据总量为空!","ERROR");
					}
					
				}
				
			});
    	}
    	
    	//*根据各项数据的数据量及占比情况，组织汇总数据，并在 summaryDg 的 datagrid 中显示
    	function getSummaryData() {
    		
    		var summaryData = '{"total":2,"rows":[';
    		summaryData += '{"category":"数量","totalData":' + totalCount + ',"state1Data":' + state1Count + ',"state2Data":' + state2Count + ',"state3Data":' + state3Count + ',"state4Data":' + state4Count + ',"state5Data":' + state5Count + ',"state6Data":' + state6Count + ',"respond11Data":' + respond11Count + ',"respond12Data":' + respond12Count + ',"respond5Data":' + respond5Count + ',"respond9Data":' + respond9Count + '},';
    		summaryData += '{"category":"占比","totalData":"' + totalRate + '%' + '","state1Data":"' + state1Rate  + '%' + '","state2Data":"' + state2Rate  + '%' + '","state3Data":"' + state3Rate  + '%' + '","state4Data":"' + state4Rate  + '%' + '","state5Data":"' + state5Rate  + '%' + '","state6Data":"' + state6Rate  + '%' + '","respond11Data":"' + respond11Rate  + '%' + '","respond12Data":"' + respond12Rate  + '%' + '","respond5Data":"' + respond5Rate  + '%' + '","respond9Data":"' + respond9Rate  + '%' + '"}';
    		summaryData += "]}";
    		
    		return JSON.parse(summaryData);
    	}
    	
		function orderListExport() {
    		
    		$("#exportForm").form('submit',{
    			
    			url:'bshOrderList/exportExcel',
    			onSubmit:function(param) {
    				param.orderId = null,
    				param.channelSource = null,
    				param.customerName = null,
    				param.customerTel = null,
    				param.brand = null,
    				param.productName = null,
    				param.state = conditionState,
    				param.respond = conditionRespond,
    				param.startTime = $("#startTime").datebox('getValue'),
    				param.endTime = $("#endTime").datebox('getValue'),
    				param.dateTimeType = 1
    			},
    			success:function(data) {
    				
    			}
    			
    		});
    		
    		
    	}
		
		function summaryExport() {
			$("#exportForm").form('submit',{
				url:'bshOrderList/exportExcelForSummaryData',
				onSubmit:function(param) {
					param.totalCount = totalCount,
					param.totalRate = totalRate,
					param.state1Count = state1Count,
					param.state1Rate = state1Rate,
					param.state2Count = state2Count,
					param.state2Rate = state2Rate,
					param.state3Count = state3Count,
					param.state3Rate = state3Rate,
					param.state4Count = state4Count,
					param.state4Rate = state4Rate,
					param.state5Count = state5Count,
					param.state5Rate = state5Rate,
					param.state6Count = state6Count,
					param.state6Rate = state6Rate,
					param.respond11Count = respond11Count,
					param.respond11Rate = respond11Rate,
					param.respond12Count = respond12Count,
					param.respond12Rate = respond12Rate,
					param.respond5Count = respond5Count,
					param.respond5Rate = respond5Rate,
					param.respond9Count = respond9Count,
					param.respond9Rate = respond9Rate,
					param.startTime = $("#startTime").datebox('getValue'),
    				param.endTime = $("#endTime").datebox('getValue')
				},
				success:function(data) {
					
				}
			});
		}
		
		//状态描述设置颜色
    	function stateformatter(value,data,index) {
    		
    		state = data.STATE;     //状态
    		
    		if(state==1) {    		//已载入（黄色）
	    		return "<span style='color:#f8d013'>" + data.STATE_DESC + "</span>";
    		}else if(state==2) {    //已成功  （绿色）
	    		return "<span style='color:#00ff00'>" + data.STATE_DESC + "</span>";
    		}else if(state==3) {	//待重试   （紫色）
	    		return "<span style='color:#fc00ff'>" + data.STATE_DESC + "</span>";
    		}else if(state==4||state==5||state==6) {     //失败、过期、放弃呼叫  （红色）
	    		return "<span style='color:#ff0000'>" + data.STATE_DESC + "</span>";
    		}else {					//新建	（黑色）
	    		return "<span>" + data.STATE_DESC + "</span>";
    		}
    		
    	}
    	
    	//设置回复的字体颜色
    	function respondformatter(value,data,index) {
    		respond = data.RESPOND;
    		
    		if(respond==1) {		//同意安装	(绿色)
    			return "<span style='color:#00ff00'>" + data.RESPOND_DESC + "</span>";
    		}else {
    			return "<span>" + data.RESPOND_DESC + "</span>";
    		}
    	}
    	
    	//设置前轩流程标识符的颜色
        function isConfirmFormatter(value,data,index) {
            isConfirm = data.IS_CONFIRM;
            if(isConfirm==1) {       //有前置流程
                return "<span style='color:#ff0000'>" + data.IS_CONFIRM_DESC + "</span>";
            }else {
                return "<span>" + data.IS_CONFIRM_DESC + "</span>";
            }
        }
        
        //设置外呼类型的颜色
        function outboundTypeFormatter(value,data,index) {
            outboundType = data.OUTBOUND_TYPE;
            if(outboundType==1) {       //确认安装
                return "<span>" + data.OUTBOUND_TYPE_DESC + "</span>";
            }else {
                return "<span style='color:#ff0000'>" + data.OUTBOUND_TYPE_DESC + "</span>";
            }
        }
    	
    	function tota1DataFormatter(value,data,index){ return "<span style='font-weight:bolder'>" + value + "</span>"; }
    	function state1DataFormatter(value,data,index){ return "<span style='font-weight:bolder'>" + value + "</span>"; }
    	function state2DataFormatter(value,data,index){ return "<span style='color:#00ff00;font-weight:bolder'>" + value + "</span>"; }
    	function state3DataFormatter(value,data,index){ return "<span style='font-weight:bolder'>" + value + "</span>"; }
    	function state4DataFormatter(value,data,index){ return "<span style='color:#ff0000;font-weight:bolder'>" + value + "</span>"; }
    	function state5DataFormatter(value,data,index){ return "<span style='font-weight:bolder'>" + value + "</span>"; }
    	function state6DataFormatter(value,data,index){ return "<span style='font-weight:bolder'>" + value + "</span>"; }
    	function respond11DataFormatter(value,data,index){ return "<span style='color:#00ff00;font-weight:bolder'>" + value + "</span>"; }
    	function respond12DataFormatter(value,data,index){ return "<span style='font-weight:bolder'>" + value + "</span>"; }
    	function respond5DataFormatter(value,data,index){ return "<span style='font-weight:bolder'>" + value + "</span>"; }
    	function respond9DataFormatter(value,data,index){ return "<span style='font-weight:bolder'>" + value + "</span>"; }
    </script>
</head>
<body>

<!-- 页面加载效果 -->
<%@ include file="/base_loading.jsp" %>

<!-- 页面内容区 -->
<div data-options="fit:true" class="easyui-layout">
	
	<!-- 顶部查询区 -->
	<div data-options="region:'north',split:true,border:true" style="height:45px;padding-top:5px;padding-left:5px;">
		<table>
			<tr style="vertial-align:top;">
				<td>
					外呼时间：
					<input id="startTime" name="startTime" class="easyui-datetimebox" style="width:200px;" required="true"/><span style="padding-left:37px;padding-right:37px;">至</span> <input id="endTime" name="endTime" class="easyui-datetimebox" style="width:200px;" required="true"/>
					<span style="padding-left:20px;">
						时间间隔：
						<select class="easyui-combobox" id="timeInterval" name="timeInterval" style="width:80px;">
							<option value="1">1天</option>
							<option value="2">2天</option>
							<option value="3">3天</option>
							<option value="4">4天</option>
							<option value="5">5天</option>
							<option value="6">6天</option>
							<option value="7">7天</option>
							<option value="8">8天</option>
							<option value="9">9天</option>
							<option value="10">10天</option>
						</select>
					</span>
					<span style="padding-left:20px;">
						购物平台：<select class="easyui-combobox" id="channelSource" name="channelSource" style="width:200px;"></select>
					</span>
					<span style="padding-left:146px;"><a href="javascript:reloadStatistics()" class="easyui-linkbutton" style="width:155px;" data-options="iconCls:'icon-search'">查询</a></span>
				</td>
			</tr>
		</table>
	</div>

	<!-- 数据显示区 -->
	<div data-options="region:'center',split:true,border:false">
		<div id="container" style="height:650px;width:1200px;"></div>
		
		<!-- 数据汇总区 -->
		<div class="easyui-tabs" style="width:1220px;height:180px;margin-left:10px;">
			<div title="数据统计汇总" style="padding:10px">
				
			<table id="summaryDg" class="easyui-datagrid" data-options="fit:true,singleSelect:true,rownumbers:false" toolbar='#summaryDgTool'>
				<thead>
					<tr style="font-weight: bold;">
						<th data-options="field:'category',width:75,align:'center'"></th>
						<th data-options="field:'totalData',width:100,align:'center',formatter:tota1DataFormatter">数据总量</th>
						<th data-options="field:'state1Data',width:90,align:'center',formatter:state1DataFormatter">已载入</th>
						<th data-options="field:'state2Data',width:90,align:'center',formatter:state2DataFormatter">已成功</th>
						<th data-options="field:'state3Data',width:90,align:'center',formatter:state3DataFormatter">待重呼</th>
						<th data-options="field:'state4Data',width:90,align:'center',formatter:state4DataFormatter">已失败</th>
						<th data-options="field:'state5Data',width:90,align:'center',formatter:state5DataFormatter">已过期</th>
						<th data-options="field:'state6Data',width:90,align:'center',formatter:state6DataFormatter">放弃呼叫</th>
						<th data-options="field:'respond11Data',width:90,align:'center',formatter:respond11DataFormatter">确认购买过</th>
						<th data-options="field:'respond12Data',width:90,align:'center',formatter:respond12DataFormatter">没有购买过</th>
						<th data-options="field:'respond5Data',width:90,align:'center',formatter:respond5DataFormatter">错误回复</th>
						<th data-options="field:'respond9Data',width:90,align:'center',formatter:respond9DataFormatter">无回复</th>
					</tr>
				</thead>
			</table>
				
			</div>
		</div>
		
	</div>

</div>

<div id="orderListDlg" class="easyui-dialog" style="width:1200px;height:800px;padding:10px 20px;" modal="true" closed="true">
		<!-- 包含表单 -->
		<%@ include file="/bsh/bshdatastatistics/_orderlist.jsp"%>
</div>
<div id="orderListDgTool" style="padding:5px;">
	<a href="#" id="easyui-export" onclick="orderListExport()" class="easyui-linkbutton" iconCls='icon-redo' plain="true">导出订单数据</a>
</div>

<div id="summaryDgTool" style="padding:5px;">
	<a href="#" id="easyui-export" onclick="summaryExport()" class="easyui-linkbutton" iconCls='icon-redo' plain="true">导出汇总数据</a>
</div>

<form id="exportForm" action="#">
</form>

       <script type="text/javascript" src="http://echarts.baidu.com/gallery/vendors/echarts/echarts.min.js"></script>
       <script type="text/javascript">
var dom = document.getElementById("container");
myChart = echarts.init(dom);
var app = {};
option = null;
app.title = '嵌套环形图';

option = {
	color: ['#f8d013','#00ff00', '#fc00ff', '#ff0000', '#fb5c5c','#fa1616',  '#04b904', '#555555','#666666', '#9c9c9c',"#c7c7c7"],
	title:{
		text:'BSH外呼系统时间区间内的外呼情况展示',
		subtext:'时间区间:2018-05-01 00:00:00 至 2018-05-02 00:00:00',
		show:true,
		x:'center',
		align:'center',
		textStyle:{
			fontSize:40
		},
		subtextStyle:{
			fontSize:20,
			color:'red',
			align:'center'
		}
	},
	toolbox:{
		feature:{
			saveAsImage:{
				type:'png',
				name:'pie-nest',
				show:true,
				title:'保存',
				pixelRatio:3,
				iconStyle:{
					normal:{
						textPosition:'top',
						textAlign:'left'
					}
					
				}
			}
		}
	},
	tooltip: {
        trigger: 'item',
        formatter: "{a} <br/>{b}: {c} ({d}%)"
    },
    legend: {
        orient: 'vertical',
        x: 'left',
        data:['已载入','已成功','确认购买过','没有购买过','错误回复','无回复','待重呼','已失败','已过期','放弃呼叫']
    },
    series: [
        {
            name:'访问来源',
            type:'pie',
            selectedMode: 'single',
            radius: [0, '30%'],
			center:['600px','350px'],
            label: {
                normal: {
                    position: 'center',
                    show: true,
                    //formatter:'{b}:{c}{d}%'
                    formatter:function(params) {
                    	pn = params['name'];
                    	pv = params['value'];
                    	pp = params['percent'];
                    	pd = params['data'];
                    	pd0 = pd['0'];
                    	
                    	//为数据统计汇总表格赋值
                    	if(pn == '已载入') {
                    		state1Count = pv;
                    		state1Rate = pp;
                    	}else if(pn == '已成功') {
                    		state2Count = pv;
                    		state2Rate = pp;
                    	}else if(pn == '待重呼') {
                    		state3Count = pv;
                    		state3Rate = pp;
                    	}else if(pn == '已失败') {
                    		state4Count = pv;
                    		state4Rate = pp;
                    	}else if(pn == '已过期') {
                    		state5Count = pv;
                    		state5Rate = pp;
                    	}else if(pn == '放弃呼叫') {
                    		state6Count = pv;
                    		state6Rate = pp;
                    	}
              
                    	
                    	if(pn == '已成功') {
                    		//return pn + ":" + pv + "\n 成功率:" + pp + "%";
                    		//return "{title|" + pn + "}";
                    		//
                    		//return "总呼叫量:300 \n 呼叫成功:200 \n 成功率: 33.33% \n 数量:" + pd0;
                    		//return '{title|情况汇总{abg|}} \n {stateHead|状态}{valueHead|数量}{rateHead|占比} \n {hr|}';
                    		return '{abg|成功率情况}\n  {totalHead|总量:' + totalCount + '}{successHead|成功:' + pv + '}{rateHead|成功率:' + pp + '%} \n{hr|}\n'
                    	}else {
                    		return "";
                    	}
                    },
                    width: 300,
                    backgroundColor: '#fff',
                    borderColor: '#777',
                    borderWidth: 1,
                    borderRadius: 4,
                    rich: {
                        abg: {
                            backgroundColor: '#00ff00',
                            width: 300,
                            align: 'center',
                            height: 35,
                            borderRadius: [4, 4, 0, 0],
                            color: '#ffffff',
                            textShadowColor: '#666666',
                            textShadowOffsetX: 2,
                            textShadowOffsetY: 1,
                            fontWeight: 'bolder',
                            fontSize: 16
                        },
                        
                        hr: {
                            borderColor: '#777',
                            width: 300,
                            borderWidth: 0.5,
                            height: 0,
                            align: 'left'
                        },
                        totalHead: {
                            color: '#333',
                            height: 24,
                            align: 'left',
                            fontSize: 16,
                            fontWeight: 'bolder',
                            //color: '#00ff00'
                        },
                        successHead: {
                            color: '#333',
                            width: 20,
                            padding: [0, 20, 0, 30],
                            fontSize: 16,
                            fontWeight: 'bolder',
                            //color: '#00ff00'
                        },
                        
                        rateHead: {
                            color: '#333',
                            width: 40,
                            align: 'center',
                            padding: [0, 20, 0, 50],
                            fontSize: 16,
                            fontWeight: 'bolder',
                            color: '#00ff00'
                        }
                    }
                }
            },
            labelLine: {
                normal: {
                    show: false
                }
            },
            data:[
                {value:0, name:'已载入'},
                {value:0, name:'已成功'},
                {value:0, name:'待重呼'},
                {value:0, name:'已失败'},
                {value:0, name:'已过期'},
                {value:0, name:'放弃呼叫'}
            ]
        },
        {
            name:'访问来源',
            type:'pie',
            radius: ['40%', '55%'],
            center:['600px','350px'],
            label: {
                normal: {
                	//formatter: '{a|{a}}{abg|}\n{hr|}\n  {b|{b}：}{c}  {per|{d}%}  ',
                	formatter: function(params) {
                    	pn = params['name'];
                    	pv = params['value'];
                    	pp = params['percent'];
                    	pa = params['data']['a'];
                    	
                    	var pnEnglish = null;    //项目转英文翻译
                    	
                    	if(pn=='确认购买过') {
                    		respond11Count = pv;
                    		respond11Rate = pp;
                    		pnEnglish = 'Confirm Purchase';
                    	}else if(pn=='没有购买过') {
                    		respond12Count = pv;
                    		respond12Rate = pp;
                    		pnEnglish = 'Not Purchase';
                    	}else if(pn=='错误回复') {
                    		respond5Count = pv;
                    		respond5Rate = pp;
                    		pnEnglish = 'Wrong Reply';
                    	}else if(pn=='无回复') {
                    		respond9Count = pv;
                    		respond9Rate = pp;
                    		pnEnglish = 'No Reply';
                    	}else if(pn=='已载入') {
                    		pnEnglish = 'Loaded';
                    	}else if(pn=='待重呼') {
                    		pnEnglish = 'Waiting For Recall';
                    	}else if(pn=='已失败') {
                    		pnEnglish = 'Failed Recall';
                    	}else if(pn=='已过期') {
                    		pnEnglish = 'Out Of Time';
                    	}else if(pn=='放弃呼叫') {
                    		pnEnglish = 'Give Up Call';
                    	}
                    	
                    	
                    	return '{a|' + pnEnglish + '}{abg|}\n{hr|}\n  {b|' + pn + '：}' + pv + '  {per|' + pp + '%}  ';
                    	
                    },
                    backgroundColor: '#eee',
                    borderColor: '#aaa',
                    borderWidth: 1,
                    borderRadius: 4,
                    // shadowBlur:3,
                    // shadowOffsetX: 2,
                    // shadowOffsetY: 2,
                    // shadowColor: '#999',
                    // padding: [0, 7],
                    rich: {
                        a: {
                        	fontSize: 16,
                            lineHeight: 33,
                            align: 'center'
                        },
                        // abg: {
                        //     backgroundColor: '#333',
                        //     width: '100%',
                        //     align: 'right',
                        //     height: 22,
                        //     borderRadius: [4, 4, 0, 0]
                        // },
                        hr: {
                            borderColor: '#aaa',
                            width: '100%',
                            borderWidth: 0.5,
                            height: 0
                        },
                        b: {
                            fontSize: 16,
                            lineHeight: 33
                        },
                        per: {
                            color: '#eee',
                            backgroundColor: '#334455',
                            padding: [2, 4],
                            borderRadius: 2
                        }
                    }
                }
            },
            data:[
                {value:0, name:'已载入'},
                {value:0, name:'确认购买过'},
                {value:0, name:'没有购买过'},
                {value:0, name:'错误回复'},
                {value:0, name:'无回复'},
                {value:0, name:'待重呼'},
                {value:0, name:'已失败'},
                {value:0, name:'已过期'},
                {value:0, name:'放弃呼叫'}
            ]
        }
    ]
};;
if (option && typeof option === "object") {
    myChart.setOption(option, true);
}

myChart.on('dblclick',function(params){
	
	var name = params.name;
	var value = params.value;
	
	//为了减少系统的开支,对于数据值为0时,不查询数据列表
	if(value == 0) {
		window.parent.showMessage("温馨提示：当前选择的 " + name + " 数据量为0,订单列表暂不显示!","ERROR");
		return;
	}
	
	var title = "订单列表,时间区间： " + startTime + "至" + endTime;
	
	if(name=='已载入') {
		title += ",呼叫状态：已载入";
		conditionState = 1;
		conditionRespond = null;
	}else if(name=='已成功') {
		title += ",呼叫状态：已成功";
		conditionState = 2;
		conditionRespond = null;
	}else if(name=='待重呼') {
		title += ",呼叫状态：待重呼";
		conditionState = 3;
		conditionRespond = null;
	}else if(name=='已失败') {
		title += ",呼叫状态：已失败";
		conditionState = 4;
		conditionRespond = null;
	}else if(name=='已过期') {
		title += ",呼叫状态：已过期";
		conditionState = 5;
		conditionRespond = null;
	}else if(name=='放弃呼叫') {
		title += ",呼叫状态：放弃呼叫";
		conditionState = 6;
		conditionRespond = null;
	}else if(name=='确认购买过') {
		title += ",客户回复：确认购买过";
		conditionState = 2;
		conditionRespond = 11;
	}else if(name=='没有购买过') {
		title += ",客户回复：没有购买过";
		conditionState = 2;
		conditionRespond = 12;
	}else if(name=='错误回复') {
		title += ",客户回复：错误回复";
		conditionState = 2;
		conditionRespond = 5;
	}else if(name=='无回复') {
		title += ",客户回复：无回复";
		conditionState = 2;
		conditionRespond = 9;
	}
	
	var channelSourceText = $("#channelSource").combobox('getText');
	if(channelSourceText=='请选择') {
		title += ", 购物平台: 全部";
	}else {
		title += ", 购物平台: " + channelSourceText;
	}
	
	findData();
	
	$("#orderListDlg").dialog('setTitle',title).dialog('open');		
	
});

       </script>

	
</body>
</html>