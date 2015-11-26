
// 蒙版
function maskAlert(_bool){

	var _id = 'mask-alert', _mask = $('#' + _id);
	if(_bool){
		if(_mask.length === 1){
			_mask.show();
		}else{
			$('<div id="'+ _id +'"></div>').appendTo('body').css('opacity', '.2');
		}
	}else{
		_mask.hide();
	}
}

// 弹窗
function winAlert(_msg,_callback){
	
	var _bool = (typeof(_msg) === 'undefined' || _msg == '') ? false : true;

	maskAlert(_bool);

	var _id = 'win-alert', _win = $('#' + _id);
	_win.remove();
	if(_bool){
		if(_win.length === 1){

			_win.show().find('.u-cnt').empty().html(_msg);

		}else{
			if(!_callback){
				_callback ="";
			}
			var _html = '<table class="m-alert" id="'+ _id +'"><tr><td style="text-align:center">'
				+ '<div style="width:500px;" class="z-ib"><h4 class="u-tit">提示信息</h4>'
				+ '<h5 class="u-close"><a href="javascript:void(0);" data-pop="test1" class="j-pop-cancel">×</a></h5>'
				+ '<div class="u-cnt">'+ _msg +'</div>'
				+ '<h6 class="u-btn"><a href="javascript:void(0);" class="u-sub j-pop-cancel" onclick="' + _callback + '">确定</a></h6>'
				+ '</div></td></tr></table>';

			$(_html).appendTo('body');
		}

	}else{

		_win.hide().find('.j-alert-cnt').empty();
	}
}

function winAlertReload(_msg){
    var _bool = (typeof(_msg) === 'undefined' || _msg == '') ? false : true;

    maskAlert(_bool);

    var _id = 'win-alert', _win = $('#' + _id);
    _win.remove();
    if(_bool){
        if(_win.length === 1){

            _win.show().find('.j-alert-cnt').empty().html(_msg);

        }else{

            var _html = '<table class="m-alert" id="'+ _id +'"><tr><td style="text-align:center">'
                + '<div style="width:500px;" class="z-ib"><h4 class="u-tit">提示信息</h4>'
                + '<h5 class="u-close"><a href="javascript:void(0);" data-pop="test1" class="j-pop-cancel">×</a></h5>'
                + '<div class="u-cnt">'+ _msg +'</div>'
                + '<h6 class="u-btn"><a href="javascript:void(0);" class="u-sub j-pop-reload">确定</a></h6>'
                + '</div></td></tr></table>';

            $(_html).appendTo('body');
        }

    }else{

        _win.hide().find('.j-alert-cnt').empty();
    }
}

$(function(){

	// 确定和关闭操作
	$(document).on('click', '.j-pop-cancel', function(){

		winAlert('', false);
	});
	$(document).on('click', '.j-pop-reload', function(){
        window.location.reload();
        winAlertReload('', false);
    });
});
