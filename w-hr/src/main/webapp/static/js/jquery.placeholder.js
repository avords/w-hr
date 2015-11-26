;(function(){

	/**
	 * 基于Jquery的模拟 placeholder效果[IE9&--]
	 * 局限性：密码框[type=password]采用的是背景图切换，需要背景图支持
	 * 2015-05-05 Tony.zeng (intval@163.com)
	 */

	if( !('placeholder' in document.createElement('input')) ){

		// 非密码类型的情况 
		$("input[type='text'][placeholder], textarea[placeholder]").each(function(){

			var _this = $(this), _txt = _this.attr('placeholder'), _classPlaceholder = 'phd-on';
			      
			if(_txt && _this.val().length === 0) _this.val(_txt).addClass(_classPlaceholder);
			 
			_this.focus(function(){

				if(_txt && _this.val() === _txt) _this.val('').removeClass(_classPlaceholder);

			}).blur(function(){

				if(_txt && _this.val().length === 0) _this.val(_txt).addClass(_classPlaceholder);
			        
			}).closest('form').submit(function(){

				if(_txt && _this.val() === _txt) _this.val('');
			});
		});

		// 密码类型解决方案:使用背景图
		$("input[type='password'][placeholder]").each(function(){

			var _this = $(this), _className = _this.attr('data-phd');
			      
			if(_className && _this.val().length === 0) _this.addClass(_className);
			 
			_this.focus(function(){

				if(_this.hasClass(_className)) _this.removeClass(_className);

			}).blur(function(){

				if(_className && _this.val().length === 0) _this.addClass(_className);
			});
		});
	}
})();