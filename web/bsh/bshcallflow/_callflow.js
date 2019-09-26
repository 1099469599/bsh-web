var voiceTypeArr = new Array("开场", "确认安装", "暂不安装", "延后安装", "已经预约", "错误回复",
		"日期", "产品","前置语音","零售核实语音");
var currCreateType = 'voiceFile';
var currentSelectRowData = null;
var currVoiceType = null;
var currVoiceTypeDesc = null;

$(function() {

	$("#bshVoiceDg").datagrid(
			{
				pageSize : 50,
				pagination : true,
				fit : true,
				rowrap : true,
				striped : true,
				singleSelect : true,
				rownumbers : true,
				pageList : [ 10, 30, 50 ],
				url : 'bshVoice/datagrid',
				toolbar : '#bshVoiceDgTool',
				queryParams : {
					voiceType : $('#voiceTypeCombobox').combobox('getValue')
				},
				onSelect : function(rowIndex, rowData) {

					currentSelectRowData = rowData;

					voiceTypeRs = rowData.VOICE_TYPE;
					voiceIndexRs = rowData.VOICE_INDEX;
					voiceDescRs = rowData.VOICE_DESC;

					// $("#voiceType" + voiceTypeRs + "-" +
					// voiceIndex).css('font-size',20);
					$("#voiceType" + voiceTypeRs + "-" + voiceIndexRs).text(
							voiceDescRs);
					$("#voiceType" + voiceTypeRs + "-" + voiceIndexRs).css({
						"color" : "red",
						"font-weight" : "bold"
					});

				},
				onUnselectAll : function(rows) {
					if (currentSelectRowData != null) {
						voiceTypeRs = currentSelectRowData.VOICE_TYPE;
						voiceIndexRs = currentSelectRowData.VOICE_INDEX;
						$("#voiceType" + voiceTypeRs + "-" + voiceIndexRs).css(
								{
									"color" : "black",
									"font-weight" : "normal"
								});
					}
				},
				onLoadSuccess : function(data) { // 加载成功后，需要加入该代码，才可以点击试听
					for (var i = 0; i < data.rows.length; i++) {
						// window.parent.showMessage(data.rows[i].playerFunction);
						eval(data.rows[i].playerFunction); // 播放器设置语音

					}
				}

			});

	$('#ttsContent').keyup(function() {
		// alert("输入了一次");
		var len = $(this).val().length;

		if (len > 199) {
			$(this).val($(this).val().substring(0, 200));
		}

		var lessNum = 200 - len;

		if (lessNum < 0) {
			lessNum = 0;
		}

		$("#ttsContentLengthNotice").html("还能输入 " + lessNum + " 个字");

	});

	$("#createType_voiceFile").bind('click', function() {
		$("#voiceFileDiv").css('display', '');
		$("#ttsDiv").css('display', 'none');
		currCreateType = 'voiceFile';
	});
	$("#createType_tts").bind('click', function() {
		// $("#voiceFileDiv").css('display','none');
		// $("#ttsDiv").css('display','');
		// currCreateType = 'tts';
	});

	$("#bshVoiceDlg").dialog({
		onClose : function() {

			$('#bshVoiceDg').datagrid('loadData', {
				total : 0,
				rows : []
			});

			if (currentSelectRowData != null) {
				voiceTypeRs = currentSelectRowData.VOICE_TYPE;
				voiceIndexRs = currentSelectRowData.VOICE_INDEX;

				$("#voiceType" + voiceTypeRs + "-" + voiceIndexRs).css({
					"color" : "black",
					"font-weight" : "normal"
				});
			}
		}
	});

	$("#bshVoiceFormDlg").dialog({
		onClose : function() {
			$("#bshVoiceForm").form('clear');
		}
	});

});

function voiceManager(vT) {
	     
	//alert(vT);
	currVoiceType = vT;
	
	if(vT == '51') {
		currVoiceTypeDesc = '未购机确认';
	} else if(vT == '52') {
		currVoiceTypeDesc = '确认未购机';
	}else {
		currVoiceTypeDesc = voiceTypeArr[vT];
	}
	$("#addVoiceBtnId").linkbutton({
		text : '增加  ' + currVoiceTypeDesc + " 语音"
	});

	$('#voiceTypeCombobox').combobox('setValue', vT);
	// alert(vT + $('#voiceTypeCombobox').combobox('getValue'));
	$("#bshVoiceDlg").dialog('setTitle', '语音管理').dialog('open');

	$("#bshVoiceDg").datagrid("reload", {
		voiceType : $('#voiceTypeCombobox').combobox('getValue')
	});

}

// 操作：编辑，删除
function rowformatter(value, data, index) {
	/*
	 * "<a href='#' onclick='javascript:voiceEdit(\"" + data.VOICE_ID + "\",\"" +
	 * data.VOICE_TYPE + "\",\"" + data.VOICE_DESC + "\")'><img
	 * src='themes/icons/pencil.png' border='0'>更换语音</a>" +
	 * "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href='#'
	 * onclick='javascript:voiceDel(\"" + data.VOICE_ID +"\")'><img
	 * src='themes/icons/pencil.png' border='0'>删除</a>";
	 */
	return "<a href='#' onclick='javascript:voiceEdit(\""
			+ data.VOICE_ID
			+ "\",\""
			+ data.VOICE_TYPE
			+ "\",\""
			+ data.VOICE_DESC
			+ "\",\""
			+ data.VOICE_NAME
			+ "\")'><img src='themes/icons/pencil.png' border='0'>更换语音</a>"
			+ "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href='#' onclick='javascript:voiceDel(\""
			+ data.VOICE_ID
			+ "\")'><img src='themes/icons/clear.png' border='0'>删除</a>";
}

// 得到当前日期
function pad2(n) {
	return n < 10 ? '0' + n : n
}
function getCurrTimeToString() { // 以 yyyyMMddHHiiss 返回
	var date = new Date();
	return date.getFullYear().toString() + pad2(date.getMonth() + 1)
			+ pad2(date.getDate()) + pad2(date.getHours())
			+ pad2(date.getMinutes()) + pad2(date.getSeconds());
}

function LessThan() {
	// 获得textarea的maxlength属性
	var MaxLength = 200;
	var num = MaxLength - $("#ttsContent").val().length;
	if (num == MaxLength) {
		$('#ttsContentLengthNotice').attr('visi', 'yes').hide();
	} else {
		$('#ttsContentLengthNotice').attr('visi', 'yes').show();
		$('#ttsContentLengthNotice').html(
				"<font font-size='13px'>还能输入：" + num + "字</font>");
	}
	// 返回文本框字符个数是否符号要求的boolean值
	return oTextArea.value.length < oTextArea.getAttribute("maxlength");
}

function voiceAdd() {

	$("#voiceTypeDescId").linkbutton({
		text : currVoiceTypeDesc + ' 语音'
	});
	$("#bshVoiceFormDlg")
			.dialog('setTitle', '增加  ' + currVoiceTypeDesc + ' 语音').dialog(
					'open');

	$("#saveVoiceBtn").attr("onclick", "saveVoiceAdd()");
}

function saveVoiceAdd() {

	var urlInfo = "bshVoice/add";
	var flag = 1; // 何种上传语音方式：1:语音文件；2：tts

	// 取得上传文件内容
	var f = $("#voiceFile").filebox('getValue');
	if (f == null || f.length == 0) { // 如果没有选择语音文件时，设置为0，则表示仅修改语音描述
		$.messager.alert("警告", "语音文件不能为空!", "error");
		return;
	}

	var vd = $("#VOICE_DESC").textbox('getValue'); // 语音描述
	var vn = $("#VOICE_NAME").textbox('getValue'); // 语音命名

	// 取得TTS内容
	var ttsContent = $("#ttsContent").val();
	var ttsContent = encodeURI(encodeURI(ttsContent));

	if (currCreateType == 'voiceFile') { // 如果修改方式为上传文件的方式

		urlInfo = 'bshVoice/add?flag=' + flag + "&voiceType=" + currVoiceType;

	} else { // 如果修改方式为TTS生成语音文件

		// 为了避免上传文件的框中有内容，在上传前，将文件框清空
		$("#voiceFile").filebox('clear');

		urlInfo = "bshVoice/updateForTTS?ttsContent=" + ttsContent;
	}

	if (vd == null || vd.length == 0) {
		$.messager.alert("警告", "语音描述不能为空!", "error");
		return;
	}

	if (vn == null || vn.length == 0) {
		$.messager.alert("警告", "语音命名不能为空!", "error");
		return;
	}

	$("#bshVoiceForm").form("submit", {

		url : urlInfo,
		onSubmit : function() {
			$.messager.progress({
				msg : '系统正在处理，请稍候...',
				interval : 3000
			});
			return $(this).form('validate');
		},
		success : function(data) {

			$.messager.progress("close");

			var result = JSON.parse(data);

			var statusCode = result.statusCode; // 返回结果类型
			var message = result.message; // 返回执行的信息

			window.parent.showMessage(message, statusCode);

			if (statusCode == 'success') {
				$("#bshVoiceDg").datagrid("reload", {
					voiceType : $('#voiceTypeCombobox').combobox('getValue')
				});
				$("#bshVoiceFormDlg").dialog("close");
			}
		}

	});

}

function voiceEdit(voiceId, voiceType, voiceDesc, voiceName) {

	$("#bshVoiceFormDlg")
			.dialog('setTitle', '更换  ' + currVoiceTypeDesc + ' 语音').dialog(
					'open');

	$("#VOICE_ID").val(voiceId);
	$("#VOICE_DESC").textbox('setValue', voiceDesc);
	$("#VOICE_NAME").textbox('setValue', voiceName);
	$("#VOICE_TYPE").combobox('setValue',
			$("#voiceTypeCombobox").combobox('getValue'));
	$("#saveVoiceBtn").attr("onclick", "saveVoiceEdit()");

	$("#voiceFile").filebox({
		buttonText : '选择文件&nbsp;&nbsp;(注：语音编辑时语音文件可以为空!)'
	});

}

// 更改时，可以只修改语音描述，语音可以不变：如果语音为空时，传送0为标识符上去；不为空时，传送1。
function saveVoiceEdit() {
	var urlInfo = "bshVoice/update";
	var flag = 1;

	// 取得上传文件内容
	var f = $("#voiceFile").filebox("getValue");
	if (f == null || f.length == 0) { // 如果没有选择语音文件时，设置为0，则表示仅修改语音描述
		flag = 0;
	}
	var vd = $("#VOICE_DESC").textbox("getValue");
	var voiceId = $("#VOICE_ID").val();

	// 取得TTS内容
	var ttsContent = $("#ttsContent").val();
	ttsContent = encodeURI(encodeURI(ttsContent));

	if (currCreateType == 'voiceFile') { // 如果修改方式为上传文件的方式

		urlInfo = 'bshVoice/update?flag=' + flag;

	} else { // 如果修改方式为TTS生成语音文件

		// 为了避免上传文件的框中有内容，在上传前，将文件框清空
		$("#voiceFile").filebox('clear');

		urlInfo = "bshVoice/updateForTTS?ttsContent=" + ttsContent;
	}

	if (vd == null || vd.length == 0) {
		$.messager.alert("警告", "语音描述不能为空!", "error");
		return;
	}

	if (voiceId == null || voiceId.length == 0) {
		$.messager.alert("警告", "语音ID不能为空!", "error");
		return;
	}
	$("#bshVoiceForm").form("submit", {

		url : urlInfo,
		onSubmit : function() {
			$.messager.progress({
				msg : '系统正在处理，请稍候...',
				interval : 3000
			});
			return $(this).form('validate');
		},
		success : function(data) {

			$.messager.progress("close");

			var result = JSON.parse(data);

			var statusCode = result.statusCode; // 返回结果类型
			var message = result.message; // 返回执行的信息

			window.parent.showMessage(message, statusCode);

			if (statusCode == 'success') {
				$("#bshVoiceDg").datagrid("reload", {
					voiceType : $('#voiceTypeCombobox').combobox('getValue')
				});
				$("#bshVoiceFormDlg").dialog("close");
			}
		}

	});

}

function voiceDel(voiceId) {

	$.messager.confirm("提示", "你确定要删除选中的记录吗?", function(r) {
		if (r) {
			$.ajax({
				type : 'POST',
				dataType : 'json',
				url : 'bshVoice/delete?voiceId=' + voiceId,
				success : function(rs) {

					var statusCode = rs.statusCode; // 返回的结果类型
					var message = rs.message; // 返回执行的信息

					window.parent.showMessage(message, statusCode);

					if (statusCode == 'success') {
						$("#bshVoiceDg").datagrid({
							url : 'bshVoice/datagrid'
						});
					}

				}
			});
		}
	});

}

// 试听
function listenrowformatter(value, data, index) {
	if (data.path == null || "" == data.path) {
		return "<a href='#' style='text-decoration:none'><div style='display:inline;padding-top:6px;' class='easyui-tooltip' title='文件不存在' style='width:100px;padding:5px;float:top;'><img src='themes/icons/no.png' style='margin-top:2px;' border='0'></a>";
	} else {
		return data.playerSkin;
	}
}

// 下载
function downloadrowformatter(value, data, index) {
	if (data.path == null || "" == data.path) {
		return "<a href='#' style='text-decoration:none'><div style='display:inline;padding-top:6px;' class='easyui-tooltip' title='文件不存在' style='width:100px;padding:5px;float:top;'><img src='themes/icons/no.png' style='margin-top:2px;' border='0'></a>";
	} else {
		return "<a href='voice/download?path="
				+ data.path
				+ "' style='text-decoration:none'><div style='display:inline;padding-top:6px;' class='easyui-tooltip' title='下载录音' style='width:100px;padding:5px;float:top;'><img src='themes/icons/download.png' style='margin-top:2px;' border='0'></a>";
	}
}

function voiceCancel() {
	$("#bshVoiceForm").form("clear");
	$("#bshVoiceDlg").dialog("close");
}

function voiceFormCancel() {
	$("#bshVoiceFormDlg").dialog('close');
}