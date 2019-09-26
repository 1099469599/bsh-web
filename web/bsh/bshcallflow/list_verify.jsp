<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<title>零售核实流程</title>

	<style>
		.font17{
			font-size: 12px;
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
		
		/*零售核实流程图样式表*/
		#prefixCallFlowPanel {
			font-size: 17px;
			height:650px;
			padding:10px auto;
			background: url("themes/icons/bsh_callflow_verify.png") no-repeat;
			padding-top: 15px;
			position: relative;
		}
		
		.verify_question {
			/*font-size: 17px;*/
			width: 430px;
			height: 170px;
			position: absolute;
			top: 45px;
			left: 280px;
		}
		
		#wrongRespond,#wrongRespond2 {
			  background-color: #fffff;
			  height:140px;
			  width:110px;
			  margin-top:272px;
			  margin-left:80px;
			  position: absolute;
		}
		
		#wrongRespond2 {
		  margin-top:400px;
		  margin-left:760px;
		}
		
		#respond1 {
		      background-color: #fffff;
              height:140px;
              width:160px;
              margin-top:272px;
              margin-left:310px;
              position: absolute;
		}
		
		#respond2 {
		      background-color: #fffff;
              height:140px;
              width:105px;
              margin-top:272px;
              margin-left:680px;
              position: absolute;
		}
		
		#confirmRespond2 {
		      background-color: #fffff;
              height:140px;
              width:110px;
              margin-top:400px;
              margin-left:590px;
              position: absolute;
		}
		
		.playbackIcon li {
			list-style: none;
			text-decoration: none;
			font-size: 50px;
			position: absolute;
			top: 580px;
			left: 900px;
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
			<option value="9">零售核实语音</option>
		</select>
	</div>
	
	<div id="prefixCallFlowPanel" class="easyui-panel font17" title="销售核实流程图" style="width: 1000px;">
		<div class="verify_question font17">
			您好，这里是（博世、西门子）家电客服中心。来电主要想核实您的购机情况，请问您近期有没有购买过（西门子/博世）品牌的对开门冰箱？<br/><br/>
			确认购买过，请按1；没有购买过，请按2.
		</div>
		
		<div id="wrongRespond">
            <span>对不起，输入有误，如有必要，我们可能会再次与您联系。</span>
        </div>
		<div id="wrongRespond2">
            <span>对不起，输入有误，如有必要，我们可能会再次与您联系。</span>
        </div>
        
		<div id="respond1">
            <span>非常感谢您选购我们的产品，后期如有需要，请扫描机身二维码或关注“(西门子/博世)家电”微信公众号预约服务、获取使用指南。祝您生活愉快，再见。</span>
        </div>
        
		<div id="respond2">
            <span>确认没有买过，请按1，返回上级菜单，请按2。</span>
        </div>
        
		<div id="confirmRespond2">
            <span>非常感谢您的反馈，我们会对相关信息做进一步核实，抱歉给你带来不便，再见。</span>
        </div>
        
		<div class="playbackIcon">
			<ul>
				<li class="iconfont icon-green" id="notInstallIcon" onclick="voiceManager(9)" style="color:#0076ff;" title="语音列表">&#xe601;</li>
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