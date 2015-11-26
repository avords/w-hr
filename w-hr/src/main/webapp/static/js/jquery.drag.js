/**
 * 基于Jquery的元素拖拽
 * 2015-03-19 Tony.zeng (intval@163.com)
**/
;(function($){

	$.fn.zDragging = function(){

		var _is = false, iX, iY, ob = $(this);

		// this 维持链式操作
		return this.each(function(){

			ob.mousedown(function(e){

				_is = true;
				
				// 计算位置
				iX = e.pageX - this.offsetLeft;
				iY = e.pageY - this.offsetTop;
				
				// 不透明 -> 半透明
				ob.fadeTo(20, 0.5);
			});

			$(document).mousemove(function(e){
		
				// 控件新位置
				if(_is) ob.css({'top': (e.pageY - iY), 'left': (e.pageX - iX)});

			}).mouseup(function(){

				_is = false;

				// 半透明 -> 不透明
				ob.fadeTo('fast', 1);
			});
		});
	};

})(jQuery);
