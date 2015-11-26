/**
 * 基于Jquery的焦点图
 * 可定义是否自动，间隔时间，速度，是否显示左右按钮
 * 2013-11-5 by zdy(intval@163.com)
 */
;(function($){

	$.fn.zFocus = function(arr){

		// 默认参数
		var opt = $.extend({'timer':3000,'speed':300,'onClass':'Onc','isLR':true,'auto':true}, arr || {});

		// 返回this，维持链式操作
		return this.each(function(){

			// 预缓存DOM对象
			var oF = $(this), oFul = oF.find('ul:first'), oFli = oFul.find('li'), oFn = oFli.length, iK = 0, iNt = 0;

			oFul.css('width', oF.width() * oFn);
			oFli.css('width', oF.width());

			var btn = '<p>';
			for(var i = 0; i < oFn; i++){
				var onC = (i == 0) ? opt.onClass : '';
				//btn += '<i class="'+ onC +'">'+ (i + 1) +'</i>';
				btn += '<i class="f-ib '+ onC +'"></i>';
			}
			btn += '</p>';
			oF.append(btn);
			var oFi = oF.find('p:first i');

			oFi.hover(
				function(){
					clearInterval(iNt);
					iK = oFi.index(this) - 1;
					focusDo();
				},
				function(){focusGo();}
			);

			/* 左右按钮 */
			if(opt.isLR){
				oF.append('<em class="btnL"></em><em class="btnR"></em>');
				//oF.find('em').css('opacity','0.1');

				oF.find('em').hover(
					//function(){clearInterval(iNt); $(this).stop(true,false).animate({'opacity':'0.6'}, 300);},
					//function(){focusGo(); $(this).stop(true,false).animate({'opacity':'0.1'}, 300);}
					function(){clearInterval(iNt);},
					function(){focusGo();}
				);
				oF.find('em.btnL').click(function(){ focusDo(1); });
				oF.find('em.btnR').click(function(){ focusDo(); });
			}

			focusGo();

			oFul.hover(function(){clearInterval(iNt);}, function(){focusGo();});
						
			/* 滚动执行函数 */
			function focusGo(){
				if(opt.auto){ iNt = setInterval(function(){ focusDo(); }, opt.timer); }
			}

			// 计算当前显示的索引
			function resetIdx(type){
				if(type == 0){ iK++; iK = iK >= oFn ? 0 : iK; }else{ iK--; iK = iK <= -1 ? oFn - 1 : iK; }
			}

			function focusDo(){
				var type = arguments[0] || 0;
				resetIdx(type);
				oFul.stop(true,false).animate({'left' : (-iK * oF.width())}, opt.speed);
				oFi.removeClass(opt.onClass).eq(iK).addClass(opt.onClass);
			}
		});
	};
})(jQuery);