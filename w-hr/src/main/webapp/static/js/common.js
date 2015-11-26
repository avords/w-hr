Date.prototype.format = function(mask) {   
    var d = this;   
    var zeroize = function (value, length) {   
        if (!length) length = 2;   
        value = String(value);   
        for (var i = 0, zeros = ''; i < (length - value.length); i++) {   
            zeros += '0';   
        }   
        return zeros + value;   
    };     

    return mask.replace(/"[^"]*"|'[^']*'|\b(?:d{1,4}|m{1,4}|yy(?:yy)?|([hHMstT])\1?|[lLZ])\b/g, function($0) {   
        switch($0) {   
            case 'd':   return d.getDate();   
            case 'dd': return zeroize(d.getDate());   
            case 'ddd': return ['Sun','Mon','Tue','Wed','Thr','Fri','Sat'][d.getDay()];   
            case 'dddd':    return ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'][d.getDay()];   
            case 'M':   return d.getMonth() + 1;   
            case 'MM': return zeroize(d.getMonth() + 1);   
            case 'MMM': return ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][d.getMonth()];   
            case 'MMMM':    return ['January','February','March','April','May','June','July','August','September','October','November','December'][d.getMonth()];   
            case 'yy': return String(d.getFullYear()).substr(2);   
            case 'yyyy':    return d.getFullYear();   
            case 'h':   return d.getHours() % 12 || 12;   
            case 'hh': return zeroize(d.getHours() % 12 || 12);   
            case 'H':   return d.getHours();   
            case 'HH': return zeroize(d.getHours());   
            case 'm':   return d.getMinutes();   
            case 'mm': return zeroize(d.getMinutes());   
            case 's':   return d.getSeconds();   
            case 'ss': return zeroize(d.getSeconds());   
            case 'l':   return zeroize(d.getMilliseconds(), 3);   
            case 'L':   var m = d.getMilliseconds();   
                    if (m > 99) m = Math.round(m / 10);   
                    return zeroize(m);   
            case 'tt': return d.getHours() < 12 ? 'am' : 'pm';   
            case 'TT': return d.getHours() < 12 ? 'AM' : 'PM';   
            case 'Z':   return d.toUTCString().match(/[A-Z]+$/);   
            // Return quoted strings with the surrounding quotes removed   
            default:    return $0.substr(1, $0.length - 2);   
        }   
    });   
}; 

String.prototype.trim = function () {
    return this.replace(/(^\s*)|(\s*$)/g, "");
};

if (typeof String.prototype.endsWith !== 'function') {
    String.prototype.endsWith = function(suffix) {
        return this.indexOf(suffix, this.length - suffix.length) !== -1;
    };
}

jQuery.prototype.serializeObject=function(){  
    var obj=new Object();  
    $.each(this.serializeArray(),function(index,param){  
        if(!(param.name in obj)){  
            obj[param.name]=param.value;  
        }  
    });  
    return obj;  
};

$.ajaxSetup ({
    cache: false, //close AJAX cache
    error: function (x, status, error) {
        if (x.status == 403) {
        	winAlert("你没有权限访问此页面");
        } else {
        	try{
        		console.log("An error occurred: " + status + "\nError: " + error);
        	}catch(e){}
        }
        //ignore the error
        return "true";
    }
});


/**
 * 获取上传文件的大小，针对IE10以下的，无法获得文件大小返回-1
 * @param target
 * @returns {Number} 单位:KB
 */
function getUploadFileSize(id){
	target = document.getElementById(id);
	var fileSize = -1;
	if(target){
		try{
			if (target.files) {          
		    	fileSize = target.files[0].size || target.files[0].fileSize;
		    } 
		}catch(e){
		}
	}
    return fileSize;
}