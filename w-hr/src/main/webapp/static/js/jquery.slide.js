;
(function($) {

	$.fn.zSlide = function(arr) {

		var opt = $.extend({
			'timer' : 3000,
			'speed' : 300,
			'size' : 3,
			'margin' : 0,
			'auto' : true
		}, arr || {});

		var ob = $(this), obUl = ob.find('div ul'), obLi = ob.find('div ul li'), len = obLi.length, intv = 0, idx = 0;

		// 补齐滚动尾部的空缺
		//var addLi = '';
		//for ( var i = 0; i < opt.size; i++) {
		//	addLi += '<li>' + obLi.eq(i).html() + '</li>';
		//}
		//obUl.append(addLi);

		// 计算滚动区域的程度
		ob.find('div').css('width',
				((obLi.width() + opt.margin) * opt.size) - opt.margin);
		obUl.css('width', (obLi.width() + opt.margin) * (len + opt.size));

		// 添加左右箭头，且设置初始样式
		ob.append('<span class="arrowL"></span><span class="arrowR"></span>');
		var arrow = ob.find('span');
		arrow.css('opacity', '0.9');

		// 箭头样式变换
		arrow.hover(function() {
			$(this).stop(true, false).animate({
				'opacity' : '0.5'
			}, 100);
			clearInterval(intv);
		}, function() {
			$(this).stop(true, false).animate({
				'opacity' : '0.9'
			}, 100);
			autoSlide();
		});

		// 箭头的点击
		ob.find('span.arrowL').click(function() {
			doSlide(1);
		});
		ob.find('span.arrowR').click(function() {
			doSlide();
		});

		// 初始化滚动
		autoSlide();

		// 鼠标移动和离开的停止和移动
		obUl.hover(function() {
			clearInterval(intv);
		}, function() {
			autoSlide();
		});

		// 间隔自动滚动
		function autoSlide() {
			if (opt.auto) {
				intv = setInterval(function() {
					doSlide();
				}, opt.timer);
			}
		}

		// 计算当前显示的索引
		function resetIdx(type) {
			if (type == 0) {
				idx++;
				idx = idx >= (len + 1) ? 0 : idx;
			} else {
				idx--;
				idx = idx <= -1 ? len : idx;
			}
		}

		// 执行滚动
		function doSlide() {
			var type = arguments[0] || 0;
			resetIdx(type);
			obUl.stop(true, false).animate({
				'left' : (-idx * (obLi.width() + opt.margin))
			}, opt.speed);
		}

	};

})(jQuery);