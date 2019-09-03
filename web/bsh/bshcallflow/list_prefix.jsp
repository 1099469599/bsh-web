<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>前置流程</title>

	<style>
		.font17{
			font-size: 16px;
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

	<style type="text/css">
		
		/*前置流程图样式表*/
		#prefixCallFlowPanel {
			font-size: 17px;
			height:700px;
			padding:10px auto;
			background: url("themes/icons/bsh_prefix_callflow.png") no-repeat;
			padding-top: 15px;
			position: relative;
		}
		
		.zhaojuquestion,.xiwanjiquestion {
			/*font-size: 17px;*/
			width: 250px;
			height: 170px;
			position: absolute;
			top: 45px;
			left: 190px;
		}
		
		.xiwanjiquestion {
			left: 810px;
		}
		
		.zhaojunotready,.xiwanjinotready {
			width: 160px;
			height: 200px;
			position: absolute;
			top: 278px;
			left: 325px;
		}
		
		.xiwanjinotready {
			left: 775px;
		}
		
		.playbackIcon li {
			list-style: none;
			text-decoration: none;
			font-size: 50px;
			position: absolute;
			top: 460px;
			left: 600px;
		}
		
		.playbackIcon li:hover {
			cursor: pointer;
		}
		
	</style>
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
			<option value="8">前置语音</option>
		</select>
	</div>
	
	<div id="prefixCallFlowPanel" class="easyui-panel font17" title="前置流程图" style="width: 1300px;">
		<div class="zhaojuquestion font17">
			您好，这里是（博世、西门子）家电客服中心，来电跟您确认灶具的安装服务，请问您家里的气源开通了吗？<br/><br/>已经开通请按“1”;还没有开通或者不清楚请按“2”。
		</div>
		<div class="xiwanjiquestion font17">
			您好，这里是(博世、西门子)家电客服中心，来电跟您确认洗碗机的安装服务，上门安装时，需要把橱柜门板装到洗碗机上，请问这块门板准备好了吗？<br/><br/>已经准备好了请按1;还没有准备好或者不清楚，请按2.
		</div>
		<div class="zhaojunotready font17">
			为了避免漏气，安装灶具时必须要做漏气检测，通常要先开通气源，再上门安装。灶具面板上有一个二维码，气源开通后，请您扫码预约安装服务，非常抱歉给您带去不便，感谢您的配合，再见。
		</div>
		<div class="xiwanjinotready font17">
			为了一次上门就能装好，需要您准备好门板之后再预约安装，洗碗机门体内侧有一个二维码，请您扫码预约安装服务，还可以方便的获取使用指南，非常抱歉给您带去不便，感谢您的配合，再见。
		</div>
		
		<div class="playbackIcon">
			<ul>
				<li class="iconfont icon-green" id="notInstallIcon" onclick="voiceManager(8)" style="color:#0076ff;" title="语音列表">&#xe601;</li>
			</ul>
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