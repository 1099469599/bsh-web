<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>博世呼叫流程</title>
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
	<script src="iconfont/iconfont.js"></script>
	<script type="text/javascript" src="jquery.min.js"></script>
	<script type="text/javascript" src="jquery.easyui.min.js"></script>
	<script type="text/javascript" src="js.date.utils.js"></script>
    <script type="text/javascript" src="jplayer/dist/jplayer/jquery.jplayer.hwzcustom.js"></script>
    <script type="text/javascript" src="locale/easyui-lang-zh_CN.js"></script>
    
    <script type="text/javascript" src="bsh/bshcallflow/_callflow.js"></script>
</head>
<body>

<!-- 页面加载效果 -->
<%@ include file="/base_loading.jsp" %>

<div style="display: none;">
	<select id="voiceTypeCombobox" class="easyui-combobox" style="width:150px;">
		<option value="0">开场语音</option>
		<option value="1">确认安装</option>
		<option value="2">暂不安装</option>
		<option value="3">延后安装</option>
		<option value="4">已经预约</option>
		<option value="5">错误回复</option>
		<option value="6">日期语音</option>
		<option value="7">产品语音</option>
	</select>
</div>
<div id="callFlowPanel" class="easyui-panel" title="呼叫流程图" style="width:1250px;height:700px;padding:10px;background:url('themes/icons/bsh_callflow.png') no-repeat;">
	<div style="background-color: #fffff;height:128px;width:640px;margin-top:52px;margin-left:216px;position: absolute;">
		<span>开场1：您好，这里是(西门子/博世)家电客服中心，来电跟您确认(洗衣机/XXX)的安装日期。根据(京东/苏宁/国美/天猫)平台传来的信息，我们将于(明天/12月10号)上门安装。确认请按1，暂不安装请按2，如需改约到后面3天，请按3,如果您已经提前预约好服务，请按4。</span>
		<span><br><br>开场2：您好，这里是(西门子/博世)家电客服中心。您在国美选购的(洗衣机/XXX)将于(明天/12月10号)送货，我们将于送货当天上门安装，需要您进一步确认。确认送货当天安装请按1，暂不安装请按2，如需改约到后面3天请按3,如果您已经提前预约好服务,请按4。</span>
	</div>
	<div style="height:50px;width:50px;margin-top:150px;margin-left:820px;position: absolute;font-size:15px;">
		<i class="iconfont icon-green" id="welcomeVoiceIcon" onclick="voiceManager(0)" style="color:#0076ff;font-size:30px;">&#xe601;</i>
	</div>
	
	<div style="background-color: #fffff;height:140px;width:185px;margin-top:418px;margin-left:5px;position: absolute;">
		<span>对不起，输入有误。我们可能会再次和您联系，再见。</span>
	</div>
	<div style="height:50px;width:50px;margin-top:525px;margin-left:155px;position: absolute;font-size:15px;">
		<i class="iconfont icon-green" id="wrongRespondIcon" onclick="voiceManager(5)" style="color:#0076ff;font-size:30px;">&#xe601;</i>
	</div>
	
	<div style="background-color: #fffff;height:140px;width:395px;margin-top:418px;margin-left:205px;position: absolute;">
		<span>场景1：京东、苏宁、天猫<br>您的机器安装日期已确认为12月10号，工程师最迟会在当天早上9:30之前与您联系具体上门时间。感谢您的配合，再见。<br></span>
		<br>
		<span>场景2：国美<br>您的机器安装日期已确认为12月10号，工程师最迟会在当天早上9:30之前与您联系具体上门时间。为确保您的权益，请认准(西门子/博世)厂家的专业工程师。感谢您的配合，再见。</span>
	</div>
	<div style="height:50px;width:50px;margin-top:525px;margin-left:560px;position: absolute;font-size:15px;">
		<i class="iconfont icon-green" id="comfirmInstallIcon" onclick="voiceManager(1)" style="color:#0076ff;font-size:30px;">&#xe601;</i>
	</div>
	
	<div style="background-color: #fffff;height:140px;width:185px;margin-top:418px;margin-left:620px;position: absolute;font-size: 15px;">
		<!-- 
		<span>您的机器，暂时将不会安排上门安装。如后期仍有需要，欢迎您拨打(4008899999/4008855888)，或者关注“西门子家电/博世家电”微信公众号预约。感谢您的配合，再见。</span>
		 -->
		<span>您的机器暂时将不会安装上门安装，如后期仍有需要，欢迎扫描机器上的二维码，或者关注“西门子家电/博世家电”微信公众号预约，感谢您的配合，再见</span>
	</div>
	<div style="height:50px;width:50px;margin-top:525px;margin-left:770px;position: absolute;font-size:15px;">
		<i class="iconfont icon-green" id="notInstallIcon" onclick="voiceManager(2)" style="color:#0076ff;font-size:30px;">&#xe601;</i>
	</div>
	
	<div style="background-color: #fffff;height:140px;width:185px;margin-top:418px;margin-left:825px;position: absolute;">
		<span>稍后您会收到1条确认短信，请您按短信提示，直接回复数字即可。感谢您的配合，再见。</span>
	</div>
	<div style="height:50px;width:50px;margin-top:525px;margin-left:979px;position: absolute;font-size:15px;">
		<i class="iconfont icon-green" id="delayInstallIcon" onclick="voiceManager(3)" style="color:#0076ff;font-size:30px;">&#xe601;</i>
	</div>
	
	<div style="background-color: #fffff;height:140px;width:185px;margin-top:418px;margin-left:1030px;position: absolute;">
		<span>我们会按您提前预约好的日期上门。 感谢您选购博世家电，再见。</span>
	</div>
	<div style="height:50px;width:50px;margin-top:525px;margin-left:1180px;position: absolute;font-size:15px;">
		<i class="iconfont icon-green" id="delayInstallIcon" onclick="voiceManager(4)" style="color:#0076ff;font-size:30px;">&#xe601;</i>
	</div>
	
	<div style="background-color: #fffff;height:140px;width:185px;margin-top:200px;margin-left:1290px;position: absolute;">
		<span class="font17">日期语音</span>
	</div>
	<div style="height:50px;width:50px;margin-top:193px;margin-left:1250px;position: absolute;font-size:15px;">
		<i class="iconfont icon-green" id="dateIcon" onclick="voiceManager(6)" style="color:#0076ff;font-size:30px;">&#xe601;</i>
	</div>
	
	<div style="background-color: #fffff;height:140px;width:185px;margin-top:260px;margin-left:1290px;position: absolute;">
		<span class="font17">产品语音</span>
	</div>
	<div style="height:50px;width:50px;margin-top:253px;margin-left:1250px;position: absolute;font-size:15px;">
		<i class="iconfont icon-green" id="productIcon" onclick="voiceManager(7)" style="color:#0076ff;font-size:30px;">&#xe601;</i>
	</div>
	
</div>

<div id="bshVoiceDlg" class="easyui-dialog" style="width:1200px;height:400px;padding:5px;" modal="false" closed="true">

		<table class="easyui-datagrid" id="bshVoiceDg">
		    <thead>
				<tr>
					<th data-options="field:'VOICE_DESC',width:400,align:'right'">语音描述</th>
					<th data-options="field:'VOICE_NAME',width:150,align:'center'">语音命名</th>
					<th data-options="field:'VOICE_TYPE_DESC',width:100,align:'center'">语音类型</th>
					<th data-options="field:'CREATE_TIME',width:150,align:'center'">创建时间</th>
					<th data-options="field:'listen',width:40,align:'center',formatter:listenrowformatter">试听</th>
					<th data-options="field:'download',width:50,align:'center',formatter:downloadrowformatter">下载</th>
					<th data-options="field:'edit',width:150,align:'center',formatter:rowformatter">编辑</th>
				</tr>
		    </thead>
		</table>
</div>	

<div id="bshVoiceFormDlg" class="easyui-dialog" style="width:700px;height:400px;padding:5px;" modal="true" closed="true" buttons="#addVoiceBtn">
		<!-- 包含语音信息的表单 -->
		<%@ include file="/bsh/bshcallflow/_form.jsp" %>
</div>

<div id="bshVoiceDgTool" style="padding:5px;">
	<a href="#" id="addVoiceBtnId" onclick="voiceAdd()" class="easyui-linkbutton" iconCls='icon-add' plain="true">增加语音</a>
</div>
	
</body>
</html>