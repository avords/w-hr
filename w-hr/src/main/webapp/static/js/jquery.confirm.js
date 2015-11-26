/**
 * 基于Jquery的弹窗 需加载对应的样式
 * 2015-03-11 Tony.zeng (intval@163.com)
 */
 ;(function(){

 	var _Zindex = 990;

 	$(document.body).on('click', '.j-confirm-open',function(){

 		var _this = $(this), _data = _this.attr('data-val').split('|');

 		// 1.蒙版
		confirmMask(true);

		// 2.生成弹窗
		confirmWindow(true, _data);

 		// 
 		return false;
	});

	// [确定 | 取消 | 关闭] 操作
	$(document.body).on('click', '.j-confirm-btn',function(){

		var _this = $(this), _act = _this.attr('data-act');
		var _data = $('#j-confirm-win .j-confirm-val').val(), _arr = _data.split('|');

		// 回调
		var _id = typeof(_arr[2]) !== 'undefined' ? _arr[2] : '0';

		if(_act === 'sure'){

			eval("confirmSure_"+ _arr[3] +"('" + _id + "');");

		}else if(_act === 'cancel'){
			if(_arr[1] === '1') eval("confirmCancel_"+ _arr[3] +"('" + _id + "');");
		}

 		// 1.蒙版
		confirmMask(false);

		// 2.生成弹窗
		confirmWindow(false);

 		// 阻止冒泡
 		return false;
	});

	function confirmMask(_bool, _level){

		var _id = 'j-mask', _mask = $('#' + _id);

		if(_bool){
			if(_mask.length === 1){
				_mask.show();
			}else{
				$('<div id="'+ _id +'"></div>').css({'position':'fixed','left':0,'top':0,'width':'100%','height':'100%','opacity':'.3','z-index':_Zindex,'background-color':'#000'}).appendTo('body');
			}
		}else{
			_mask.hide();
		}
	}

	function confirmWindow(_bool, _data){

		var _id = 'j-confirm-win', _win = $('#' + _id);

		if(_bool){

			if(_win.length === 1){

				_win.show().find('.j-confirm-val').val(_data.join('|'));
				_win.find('.j-confirm-txt').html(_data[0]);

			}else{

				// 组装基本HTML
				var _html = '<table class="m-confirm" id="'+ _id +'" style="z-index:'+ (_Zindex + 2) +';"><tr>';
					_html+= '<td style="text-align:center;vertical-align:middle;">\n<div style="width:400px;" class="ib">\n';
					_html+= '<h4 class="tt"><span>操作确认</span><a href="javascript:void(0);" data-act="" class="j-confirm-btn">&times;</a>';
					_html+= '<input type="hidden" class="j-confirm-val" value="'+ _data.join('|') +'" />\n</h4>\n<p class="j-confirm-txt">' + _data[0] + '</p>\n';
					_html+= '<h6 class="btn">\n<a href="javascript:void(0);" data-act="sure" class="sub ib j-confirm-btn">确定</a>\n';
					_html+= '<a href="javascript:void(0);" data-act="cancel" class="rst ib j-confirm-btn">取消</a>\n</h6>\n</div>\n</td></tr></table>\n';

				// 添加到body中
				$(_html).show().appendTo('body');
			}

		}else{

			// 关闭
			_win.hide();
		}
	}

 })();