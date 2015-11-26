/**
 * The PopUp based on Jquery
 * Support multi & custom callback function
 * Tony.zeng (intval@163.com)
 */
;$(function(){

	// [Event]
	$(document.body).on('click', '.j-pop-open', function(){

		var _this = $(this),
			_CHAR = _this.attr('data-char');

		var _CFG = POPUP[_CHAR];
		if(typeof(_CFG) === 'undefined'){
			alert('No configuration information about: '+ _CHAR +' !');
			return false;
		}
		_CFG['char'] = _CHAR;

		if(typeof(_CFG['level']) === 'undefined') _CFG['level'] = 1;

		_CFG['param'] = undefined;
		var _param = _this.attr('data-param');
		if(typeof(_param) !== 'undefined') _CFG['param'] = _param;

		popMask(true, _CFG['level']);
		
		actPopUp(true, _CFG);

		if(typeof(_CFG['complete']) !== 'undefined'){
			
			if(typeof(_CFG['param']) !== 'undefined' || _CFG['param'] == ''){
				_CFG['complete'] = _CFG['complete'].replace('()','').replace(';','');
				evalPopUp(_CFG['complete'], _CFG['param']);
			}
		}

		return false;
	});

	$(document.body).on('click', '.j-pop-cancel', function(){

		var _this = $(this), _tag = _this.parents('table.j-pop-win').find('input.pop-char');
		var _CHAR = _tag.val(), _param = _tag.attr('data-param');

		var _CFG = POPUP[_CHAR];
		_CFG['char'] = _CHAR;

		if(typeof(_param) !== 'undefined') _CFG['param'] = _param;

		popMask(false, _CFG['level']);

		actPopUp(false, _CFG);

		if(_this.hasClass('hasfunc')){
			var _func = _CFG['cancel']['func'];
			if(typeof(_func) !== 'undefined' && _func !== ''){
			
				_func = _func.replace('()','').replace(';','');
				if(_param === undefined) _param = '';
				evalPopUp(_func, _param);
			}
		}

		return false;
	});

	// [Event]
	$(document.body).on('click', '.j-pop-sure', function(){

		var _this = $(this), _tag = _this.parents('table.j-pop-win').find('input.pop-char');
		var _CHAR = _tag.val(), _param = _tag.attr('data-param');
		var _call = _this.attr('data-call');

		var _CFG = POPUP[_CHAR];
		_CFG['char'] = _CHAR;

		_CFG['param'] = undefined;
		if(typeof(_param) !== 'undefined') _CFG['param'] = _param;

		if(typeof _call !== 'undefined' && _call !== '') _CFG['sure'] = {'func':_call};

		var _sure = _CFG['sure'], _func = _sure['func'];
		if(typeof(_sure) !== 'undefined' && _func !== ''){

			try{

				_func = _func.replace('()','').replace(';','');
				if(_CFG['param'] === undefined) _CFG['param'] = '';
				var _init = evalPopUp(_func, _CFG['param']);

				if(_init){

					popMask(false, _CFG['level']);

					actPopUp(false, _CFG);
				}

			}catch(e){

				alert(e.message);
				return false;
			}
		}

		return false;
	});
});

// [Variable]
var  _popZindex = 990;

// [function]
function isDefaultPopUpCfg(_obj, _char){
	return (typeof(_obj[_char]) === 'undefined' || (!_obj[_char])) ? true : false;
}

// 模拟eval，用call或apply实现回调
// 参数：对象集，方法体，参数
var evalPopUp = function(func, args){

	var _f = 'function', _un = ') unExists!', _er = 'ERR: ';
	
	var _fn = window[func];
	if (typeof _fn === _f) {
		return _fn.call(null, args + '');
	}else{
		console.log(_er + _f.toUpperCase() + '(' + func + _un);
	}
	_fn = null;
};

// [function]
function actPopUp(_bool, _cfg){

	var _id = _cfg['char'], _pop = $('#j-pop-win-' + _id);

	if(_bool){

		if(_pop.length === 1){

			if(typeof(_cfg['param']) === 'undefined'){

				_pop.show().find('input.pop-char').removeAttr('data-param');

			}else{

				_pop.show().find('input.pop-char').attr('data-param', _cfg['param']);
			}

			if(_cfg['level'] !== 1){

				_pop.css({'z-index':(_popZindex + _cfg['level'] * 2)});
			}

		}else{

			var _skin = isDefaultPopUpCfg(_cfg, 'skin') ? 'skin' : _cfg['skin'];

			var _title = isDefaultPopUpCfg(_cfg, 'title') ? 'PopUp Title' : _cfg['title'];

			var _width = isDefaultPopUpCfg(_cfg, 'width') ? '400' : _cfg['width'];

			var _pos = isDefaultPopUpCfg(_cfg, 'position') ? 'absolute' : ''+ _cfg['position'];

			var _top = isDefaultPopUpCfg(_cfg, 'top') ? 'vertical-align:middle;' : 'vertical-align:top;padding-top:'+ _cfg['top'] + 'px;';

			var _zix = 'z-index:' + (_popZindex + _cfg['level'] * 2) + ';';

			var _sure = (typeof(_cfg['sure']) !== 'undefined') ? '<a href="javascript:void(0);" class="pop-sure j-pop-sure">'+ (typeof(_cfg['sure']['txt']) === 'undefined' ? 'Sure' : _cfg['sure']['txt']) +'</a>' : '';

			var _cancel = (typeof(_cfg['cancel']) !== 'undefined') ? '<a href="javascript:void(0);" class="pop-cancel j-pop-cancel hasfunc">'+ (typeof(_cfg['cancel']['txt']) === 'undefined' ? 'Cancel' : _cfg['cancel']['txt']) +'</a>' : '';

			var _btn = (typeof(_cfg['sure']) === 'undefined' && typeof(_cfg['cancel']) === 'undefined') ? '' : '<div class="pop-btn">'+ _sure + _cancel + '</div>';

			var _param = (typeof(_cfg['param']) === 'undefined') ? '' : ' data-param="'+ _cfg['param'] +'"';

			if(_id === 'confirm'){
				var _content = '<div class="pop-confirm">' + _cfg['msg'] + '</div>';

				_btn = 	'<div class="pop-btn"><a href="javascript:void(0);" class="pop-sure j-pop-sure">'+ _cfg['sure']['txt'] +'</a>' + 
						'<a href="javascript:void(0);" class="pop-cancel j-pop-cancel hasfunc">'+ _cfg['cancel']['txt'] +'</a></div>';
			}else{
				try{
					_cfg['content'] = _cfg['content'].replace('()','').replace(';','');
					var _content = evalPopUp(_cfg['content'], '');
				}catch(e){
					alert(e.message);
					return false;
				}
			}

			var _html = '<table style="'+ _zix +'position:'+ _pos +';" id="j-pop-win-'+ _id +'" class="j-pop-win m-popup"><tr>' +
						'<td style="height:100%;text-align:center;'+_top+'">' +
						'<div style="width:'+ _width +'px;display:inline-block;*display:inline;*zoom:1;" class="pop-'+ _skin +'">' +
						'<input type="hidden" class="pop-char" value="'+ _cfg['char'] +'"'+ _param +' /><div class="pop-tt">' +
						'<span>'+ _title +'</span><a href="javascript:void(0);" class="j-pop-cancel">&times;</a></div>' + _content + _btn + 
						'</div></td></tr></table>';

			$(_html).show().appendTo('body');
		}

	}else{

		_pop.hide();
	}
}

// [function]
function popMask(_bool, _level){

	var _id = 'm-mask', _mask = $('#' + _id);

	_level = parseInt(_level);

	if(_bool){

		var _zix = _popZindex - 1 + _level * 2;

		if(_mask.length === 1){
			if(_level === 1){
				_mask.show();
			}else{
				_mask.css({'z-index':_zix});
			}
		}else{
			var _css = {'position':'fixed','top':0,'right':0,'bottom':0,'left':0,'width':'100%','height':'100%','opacity':'.3','z-index':_zix,'background-color':'#000'};
			$('<div id="'+ _id +'"></div>').css(_css).appendTo('body');
		}

	}else{

		if(_level === 1){
			_mask.hide();
		}else{
			_mask.css({'z-index':(_popZindex - 3 + _level * 2)});
		}
	}
}

// [function]
function removeAllPopUp(){
	$('.j-pop-win').remove();
}

// [function]
function removeMask(){
	$('#m-mask').remove();
}

// [function]
function autoPopUp(_char, _only, _param){

	var _isOnly = false;
	if(typeof _only === 'undefined' || _only === true) _isOnly = true;

	var _CFG = POPUP[_char];
	if(typeof(_CFG) === 'undefined'){
		alert('No configuration information about: '+ _char +' !');
		return false;
	}
	_CFG['char'] = _char;
	_CFG['level'] = 1;

	if(_isOnly){
		_CFG['old_lv'] = _CFG['level'];
		
	}else{
		if(typeof _CFG['old_lv'] !== 'undefined') _CFG['level'] = _CFG['old_lv'];
	}

	_CFG['param'] = undefined;
	if(typeof(_param) !== 'undefined') _CFG['param'] = _param;

	popMask(true, _CFG['level']);
	if(_isOnly) $('#m-mask').css('z-index', (_popZindex + _CFG['level']));
	
	if(_isOnly) removeAllPopUp();
	actPopUp(true, _CFG);

	var _func = _CFG['complete'];
	if(typeof(_func) !== 'undefined'){
		
		if(_CFG['param'] === undefined) _CFG['param'] = '';
		_func = _func.replace('()','').replace(';','');
		evalPopUp(_func, _CFG['param']);
	}

	return false;
}

// [function]
function confirmPopUp(_msg, _call){

	var _char = 'confirm', _CFG = POPUP[_char];
	if(typeof(_CFG) === 'undefined'){
		alert('No configuration information about: '+ _char +' !');
		return false;
	}
	_CFG['char'] = _char;

	_CFG['level'] = 1;
	_CFG['msg'] = _msg;
	_CFG['sure']['func'] = _call + '();';

	popMask(true, _CFG['level']);
	$('#m-mask').css('z-index', (_popZindex + _CFG['level']));
	
	removeAllPopUp();
	actPopUp(true, _CFG);

	return false;
}
function doPopUpV1(_CHAR,_param){
	var _CFG = POPUP[_CHAR];
	if(typeof(_CFG) === 'undefined'){
		//alert('缺少( '+ _CHAR +' )的配置信息');
		return false;
	}
	_CFG['char'] = _CHAR;

	// 默认层级
	if(typeof(_CFG['level']) === 'undefined') _CFG['level'] = 1;

	// 参数
	_CFG['param'] = undefined;
	if(typeof(_param) !== 'undefined') _CFG['param'] = _param;

	// 1. 开启蒙版: 参数传递 实现蒙版遮住非当前弹窗的效果
	popMask(true, _CFG['level']);
	
	// 2. 生成弹窗
	actPopUp(true, _CFG);

	// 3. 渲染后动态加载
	if(typeof(_CFG['complete']) !== 'undefined'){
		
		// 渲染传参
		if(typeof(_CFG['param']) !== 'undefined' || _CFG['param'] == ''){
			var _str = _CFG['complete'].toString(), _len = _str.length;
			eval(_str.substr(0, (_len - 3)) + '("'+ _CFG['param'] +'");');
		}else{
			eval(_CFG['complete']);
		}
	}

}