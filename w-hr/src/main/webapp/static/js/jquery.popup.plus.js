/**
 * 基于Jquery的弹窗 需加载对应的样式
 * 2015-03-11 Tony.zeng (intval@163.com)
 */
// 弹窗的z-index
	var _popZindex = 1000;
;$(function(){
	
	//层级、宽度、0(上下居中，否top),是否显示确定按扭（关联回调）、组别
	// 开启弹窗
	$(document).on('click', '.j-pop-open',function(){
		try{
			var _this = $(this), _data = _this.attr('data-pop').split(',');
			
			return popup(_data);
		}catch(e){
			console.log(e);
		}
		
	});
	
	// 弹窗的关闭按钮事件
	$(document).on('click', '.j-pop-cancel', function(){
		try{
			// 获取隐藏域的值(关联标识, 层级:涉及关闭遮罩)
			var _this = $(this), _data = _this.parents('table.j-pop-up').find('.j-pop-data').val().split(',');
	
			// 关闭遮罩
			doMask(false, _data[0]);
	
			// 关闭弹窗
			doPopUp(false, _data);
			
			var _parent = _this.parents('table.j-pop-up');
			var _tagCancel = _parent.find('.j-cancel');
			if(_tagCancel.length > 0){
				var _func = _tagCancel.attr('data-func');
				var _ext = _tagCancel.attr('data-ext');
				if(typeof(_ext) !== 'undefined'){
					eval(_func + '('+ _ext +');');
				}else{
					eval(_func + '();');
				}
			}
		}catch(e){
		}

		return false;
	});

	// 弹窗的确定按钮事件
	$(document).on('click', '.j-pop-sure', function(){

		// 获取隐藏域的值(关联标识, 层级:涉及关闭遮罩)
		try{
			var _this = $(this), _data = _this.parents('table.j-pop-up').find('.j-pop-data').val().split(',');
			// 是否有确定按钮：关联确定回调函数
			if(_data[3] === '1'){

				// 1.弹窗开启的初始操作
				var _funcStr = popSure('sure' + _data[4] + _data[0]);

				if(_funcStr === undefined){

					alert('无确定按钮的处理函数');
					return false;

				}else{

					// 执行函数体
					eval('var _sure = ' + _funcStr + '();');

					if(_sure === false){
						return false;
					}else{

						// 关闭遮罩
						doMask(false, _data[0]);

						// 关闭弹窗
						doPopUp(false, _data);
					}
				}
			}
		}catch(e){
		}
		return false;
	});
	
	
});

//执行弹窗
function popup(_data){
	// 1.弹窗开启的初始操作
	var _funcStr = popInit('init' + _data[4] + _data[0]);
	if(_funcStr == undefined){

		//alert('无初始化函数');
		return false;

	}else{
		// 执行函数体
		eval('var _init = ' + _funcStr + '();');
		if(_init === false){
			//alert('初始化函数加载失败');
			return false;
		}
	}

	// 2.开启遮罩
	doMask(true, _data[0]);

	// 3.组装弹窗内容且显示
	doPopUp(true, _data, _init);
	
	// 4.内容组装完成 动态加入数据
	try{
		var _funcStr = popFinish('finish' + _data[4] + _data[0]);
		if(_funcStr !== undefined){

			// 执行函数体
			eval('var _finish = ' + _funcStr + '();');

			if(_finish === false){
				alert('初始化完成函数加载失败');
				return false;
			}
		}
	}catch(e){
	}
	
	// 5.控制树的样式
	$('table.j-pop-up div.lft h3,table.j-pop-up div.cnt h3').each(function(){
		var _parent = $(this).parent(), _last = _parent.children(':last');
		_last.addClass('lst');
		if(_parent.children('div').length > 0) _last.prev('h3').addClass('lst');
	});
	
	return false;
}

//操作弹窗[注意z-index的计算]
function doPopUp(_bool, _data, _init){

	var _id = _data[0], _pop = $('#j-pop-up' + _data[4] + _id);

	if(_bool){

		if(_pop.length === 1){

			_pop.show();

		}else{

			// 判断上下居中问题
			var _css = _data[2] === '0' ? 'vertical-align:middle;' : 'vertical-align:top;padding-top:'+ _data[2] + 'px;';

			// 弹窗的层级
			var _zix = _popZindex + (parseInt(_data[0]) * 2);

			// 是否显示确定按钮
			var _sure = _data[3] === '1' ? '<a href="javascript:void(0);" class="u-sub j-pop-sure">'+ (typeof(_init['btn'])==='undefined'?'确定':_init['btn']) +'</a>\n' : '';

			// 如果是移动设备 则设为 : position:absolute
			var _wapPos = navigator.userAgent.match(/mobile/i) ? 'position:absolute;top:5%;' : '';
			
			// 组装基本HTML
			var _html = '<table class="z-pop-up j-pop-up" id="j-pop-up'+ _data[4] + _id +'" style="'+_wapPos+'z-index:'+ _zix +';"><tr><td style="text-align:center;'+ _css +'">\n';
				_html+= '<div style="width:'+ _data[1] +'px;" class="z-ib">\n';
				_html+= '<h4 class="u-tit"><span>'+ _init['title'] +'</span><a href="javascript:void(0);" class="j-pop-cancel">&times;</a>';
				_html+= '<input type="hidden" class="j-pop-data" value="'+ _data.join(',') +'" />\n</h4>\n';
				_html+= '<div class="pop-'+ _data[4] + _id +'">\n' + _init['html'] + '</div>\n';
				_html+= '<h6 class="u-btn">\n'+ _sure +'<a href="javascript:void(0);" class="u-rst j-pop-cancel">取消</a>\n</h6>\n';
				_html+= '</div>\n</td></tr></table>\n';

			// 添加到body中
			$(_html).show().appendTo('body');
		}

	}else{

		// 关闭
		_pop.hide();
	}
}


// 操作遮罩[注意z-index的计算]
function doMask(_bool, _level){
	var _id = 'm-mask', _mask = $('#' + _id);
	if(_bool){

		var _zix = _popZindex - 1 + parseInt(_level) * 2;

		if(_mask.length === 1){
			if(_level === '1'){
				_mask.show();
			}else{
				_mask.css({'z-index':_zix});
			}
		}else{
			$('<div id="'+ _id +'"></div>').css({'position':'fixed','left':0,'top':0,'width':'100%','height':'100%','opacity':'.3','z-index':_zix,'background-color':'#000'}).appendTo('body');
		}

	}else{

		if(_level === '1'){
			_mask.hide();
		}else{
			_mask.css({'z-index':(_popZindex - 3 + parseInt(_level) * 2)});
		}
	}
}

function doRePopup(_data){
	$('#j-pop-up' + _data[4] + _data[0]).remove();
	popup(_data);
}