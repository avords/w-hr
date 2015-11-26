/* intval@163.com <Tony.zeng> */
$(function() {
	
	// 预定义
	var _speed = 750, _top = 0, _c = 0, _x = 0;
	var _box = $('.gw-box'), _n = _box.length;
	var _btn = $('.j-btn > a');

	// 鼠标滚屏事件
	_box.mousewheel(function(event, delta){

		// 控制滚轮的滚轮事件范围
		if(!(delta === -1 || delta === 1)) return false;

		// 控制滚轮的多次触发事件
		if(++_x > 1) return false;

		var _this = $(this), _i = parseInt(_this.attr('data-i'));

		// 位置处于最顶部且上滚动 或 位置处于最底部且下滚动 时 无操作
		if((_i == 0 && delta == 1) || ((_i == (_n - 1)) && delta == -1)){
			_x = 0;
			return false;
		}

		// 计算CSS样式
		var _height = _this.height();
		var _css = {'top':(delta == 1 ? (-_height) : _height), height: _height, 'display':'block'};

		// 当前页 上下滚动
		var _thisTop = delta === -1 ? 0 - _height : _height;
		_this.stop(false, false).animate({top:_thisTop}, _speed, 'swing', function(){$(this).hide();});

		// 预显示页 上下滚动
		var _ob = delta == 1 ? _this.prev('div.gw-box') : _this.next('div.gw-box');
		_ob.css(_css).stop(false, false).animate({top:_top}, _speed, 'swing', function(){_x = 0;});

		// 当前索引值
		_c = delta == 1 ? (_i - 1) : (_i + 1);
		if(delta == -1 && (_i == 0)) _c = 1;
		if(delta == 1 && (_i == (_n - 1))) _c = _n - 2;

		// 控制菜单样式
		eval('doNav('+ _c +');');
		
		// 按钮的样式变换
		_btn.removeClass('z-on').eq(_c).addClass('z-on');

		return false;
	});

	// 按钮点击事件
	_btn.click(function(){

		// 获取按钮索引值
		var _this = $(this), _i = _btn.index(_this);
		
		// 当前按钮点击无效
		if(_i == _c) return false;

		// 控制菜单样式
		eval('doNav('+ _i +');');

		// 按钮样式变换
		_btn.removeClass('z-on').eq(_i).addClass('z-on');

		// 当前页 上下滚动
		var _then = _box.eq(_c), _hig = _then.height();
		var _thisTop = (_i > _c) ? 0 - _hig : _hig;
		_then.stop(false, false).animate({top:_thisTop}, _speed, 'swing', function(){$(this).hide();});

		// 预显示页 上下滚动
		var _ob = _box.eq(_i), _height = _ob.height();
		var _hig = (_i > _c) ? _height : (-_height);
		_ob.css({'top':_hig, 'display':'block'}).animate({'top':_top}, _speed);

		// 重置索引
		_c = _i;
	});

	// 控制菜单样式
	function doNav(_i){
		var _nav = $('.j-nav'), _logo = $('.j-logo');
		if(_i == '0'){
			var _height = _nav.height()
			if(_height != 82) _nav.removeClass('gt').stop(false,false).animate({height:82}, 400);
			_logo.stop(false,false).animate({width:96,height:94}, 400);
		}else{
			var _height = _nav.height()
			if(_height != 60) _nav.addClass('gt').stop(false,false).animate({height:60}, 400);
			_logo.stop(false,false).animate({width:72,height:70}, 400);
		}
	}

	// 控制福励方案的显示和隐藏
	$('.j-cl-img').hover(
		function(){
			var _i = $(this).attr('data-i');
			$('#j-cl-show' + _i).stop(false, false).fadeIn();
		},
		function(){
			var _i = $(this).attr('data-i');
			$('#j-cl-show' + _i).stop(false, false).fadeOut(100);
		}
	);

	// 平台优势的JS
	var _sld = $('.j-sld'), _sldDl = $('.j-sld-f');

	_sld.hover(
		function(){
			var _this = $(this), _dt = _this.parent(), _dd = _dt.next(), _dl = _dt.parent();
			_dl.addClass('z-on').stop(false, false).animate({paddingTop:0}, 300);
			_this.stop(false, false).animate({width:'50%'}, 300);
			_dd.stop(false, false).fadeIn();
		}, function(){}
	);

	_sldDl.hover(
		function(){},
		function(){
			var _dl = $(this);
			_dl.removeClass('z-on').stop(false, false).animate({paddingTop:'4%'}, 300);
			_dl.find('dt > img').stop(false, false).animate({width:'70%'}, 500);
			_dl.find('dd').stop(false, false).fadeOut();
		}
	);

	// 点击微信弹窗的点击和关闭
	$('.j-char-s').click(function(){ $(this).prev('span').fadeIn(); return false; });
	$('.j-char-c').click(function(){ $(this).parent('span').fadeOut(); return false; });
	$(document).click(function(){ $('.j-char-c').parent('span').fadeOut(); });

	// 服务保障的淡入淡出切换
	var _fdB = $('.j-fg-b > a'), _fdI = $('.j-fg-i > li');
	$('.j-fg-b > a').click(function(){

		var _this = $(this);
		if(_this.hasClass('z-on')) return false;

		_fdB.filter('.z-on').removeClass('z-on');
		_this.addClass('z-on');
		
		var _i = _fdB.index(_this);
		_fdI.not(':hidden').stop(false,false).fadeOut();
		_fdI.eq(_i).stop(false,false).fadeIn();
	});

	/* 第一屏的左右滚动 */ 
	var _ii = _inte = 0, _tt = 4000, _ss = 800;
	var _fBtn = $('.j-fos-btn > a'), _nn = _fBtn.length;
	var _fLi = $('.j-fos-img > li');

	// 按钮(小点)点击事件
	_fBtn.click(function(){

		var _this = $(this);
		if(_this.hasClass('z-on')) return false;

		// 先关闭定时
		clearInterval(_inte);

		// 滚动操作
		_ii = _fBtn.index(_this) - 1;
		doFos();

		// 再开启定时
		_inte = setInterval(function(){ doFos(); }, _tt);

	});

	// 定时操作
	_inte = setInterval(function(){ doFos(); }, _tt);

	// 滚动操作(匿名函数)
	var doFos = function(){

		// 先加再比较
		if(++_ii >= _nn) _ii = 0;

		// 按钮(小点变换)
		_fBtn.eq(_ii).addClass('z-on').siblings().removeClass('z-on');

		// 图片位移
		var _width = _fLi.eq(0).width(), _left = _ii != 0 ? _width : (0 - _width);
		_fLi.eq(_ii).css({'display':'block','left':_left}).stop(false,false).animate({'left':0}, _ss).siblings().fadeOut(_ss);
	}

	/* 第四屏的左右滚动 */ 
	var _iii = _intv = 0, _ttt = 5000, _sss = 800, _a = false;
	var _sBtn = $('.j-sld-btn > a'), _nnn = _sBtn.length;
	var _sDiv = $('.j-sld-img > div');

	// 按钮(小点)点击事件
	_sBtn.click(function(){

		var _this = $(this);
		if(_this.hasClass('z-on')) return false;

		// 先关闭定时
		clearInterval(_intv);

		// 滚动操作
		_iii = _sBtn.index(_this) - 1;
		doSld();

		// 再开启定时
		if(_a) _intv = setInterval(function(){ doSld(); }, _ttt);

	});

	// 定时操作
	if(_a) _intv = setInterval(function(){ doSld(); }, _ttt);

	// 滚动操作(匿名函数)
	var doSld = function(){

		// 先加再比较
		if(++_iii >= _nnn) _iii = 0;

		// 按钮(小点变换)
		_sBtn.eq(_iii).addClass('z-on').siblings().removeClass('z-on');

		// 图片位移
		var _width = _sDiv.eq(0).width(), _left = _iii != 0 ? _width : (0 - _width);
		_sDiv.eq(_iii).css({'display':'block','left':_left}).stop(false,false).animate({'left':0}, _sss).siblings().fadeOut(_sss);
	}
});