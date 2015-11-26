// 正则格式验证(匿名函数)
var utils = function(){

	var nReg = /^[\d|\.|,]+$/, iReg = /\D+/, tReg = /^[\d|\-|\s|\_]+$/, dReg = /^\d{4}-\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}$/;
	var eReg = /^\w+([-+.]\w+)*@\w+([-.]\w+)*\.\w+([-.]\w+)*$/, mReg = /^0?1[3|4|5|7|8][0-9]\d{8}$/,zReg=/^[1-9][0-9]{5}$/ ;	
	return {
		trim : function(str){ return str.replace(/^\s*|\s*$/g, ''); }, 
		isNumber : function(val){ return nReg.test(val); },
		isInt : function(val){ return !iReg.test(val); }, 
		isEmail : function(mail){ return eReg.test(mail); },
		isMobile : function(mail){ return mReg.test(mail); },
		isTel : function(tel){ return tReg.test(tel); }, 
		isZipCode : function(zipcode){ return zReg.test(zipcode); }, 
		isTime : function(time){ return dReg.test(time); }
	};
}();

