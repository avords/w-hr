/*
 * 选择人员的弹窗
 * intval@163.com >< Tony.zeng
 */
if(POPUP === undefined) var POPUP = {};

// ------------------- 华丽分割线 -------------------------------

// [函数] 根据ids集合和标识及方案
function computeResult(_ob, _chr, _insure){

	// 获取ID集合
	var ids = [];
	_ob.find('a').each(function(){
		ids.push($(this).attr('data-id'));
	});

	console.log(_chr, ids, _insure);

	// ajax 计算保费
	var _url = computeUrl + '?backFunc=?';
	$.ajax({
		async: true, url: _url, type: 'GET', dataType: 'jsonp', 
		data : {'insure':_insure, 'char':_chr,'ids': ids.join(',')}, timeout: 5000,
        success: function(_data){

        	// 联动项目组
			_ob.next('h5').find('i:first').text(ids.length).attr('data-val', _data['price'] + '|' + _data['count']);

        	// 计算总价
			var Ele = $('#j-for-insure > dl.note i');
			Ele.eq(0).text(_data['price'] + '元');
			Ele.eq(1).text(_data['count'] + '人');
			Ele.eq(2).text((_data['price'] * _data['count']) + '元');
        }
    });
}

// [函数] 对象的拷贝
function cloneObject(_obj){ 

	if(typeof(_obj) != 'object') return _obj;

	if(_obj == null) return _obj;  

	var _clone = {};  

	for(var i in _obj) _clone[i] = cloneObject(_obj[i]);

	return _clone;  
}

// [函数] 树节点尾元素增加样式
function addClassLast(_dom){

	_dom.find('h3').each(function(){
		var _this = $(this), _next = _this.next();
		if(_next.length == 0 || (_next.is('div') && _next.next().length == 0)) _this.addClass('lst').next().addClass('lst');
	});
}

// [递归函数] 组建树状结构
function jsonTree(json, pid){

	var _tree = [];

	for(var i in json){

		var rs = json[i];
		
		if(pid == rs['pid']){

			// 去除已经使用的因素，减少下次循环次数
			delete json[i];

			var _sub = arguments.callee(json, i.substr(1));

			_tree.push({'pid': 'p' + rs['pid'], 'id': i, 'name': rs['name'], 'sub': _sub, 'count': rs['count']});
		}
	}
	return _tree;
}

// 递归循环创建HTML文件
function htmlTree(_treeData, chr){

	var _html = '';

	for(var pid in _treeData){

		var rs = _treeData[pid], sub = rs['sub'], name = rs['name'];
		var _val = rs['id'].substr(1) + '|' + rs['pid'].substr(1) + '|' + rs['count'] + '|' + name;
		
		if(sub.length > 0){
			var ipt = chr == 'parent' ? '<input type="checkbox" value="'+ _val +'">' : '';
			_html += '<h3><i class="f-ib">-</i> <label class="f-ib">'+ ipt +' '+ name +'('+ rs['count'] +')</label></h3><div>';
			_html += arguments.callee(sub, chr);
			_html += '</div>';
		}else{
			_html += '<h3><label><input type="checkbox" value="'+ _val +'"> '+ name +'('+ rs['count'] +')</label></h3>';
		}
	}

	return _html;
}

// ------------------- 华丽分割线 -------------------------------

// 选择 人员
POPUP.member = {
	'title': '选择人员', 'width': 860, 'top': 200, 'level' : 2,
	'content': 'memberContent', 'complete': 'memberComplete',
	'sure': {'txt':'确定', 'func': 'memberSure'}, 'cancel': {'txt':'取消'}
};

function memberContent(){
	return	'<div class="pop-tree-plus f-cb j-zTree-plus" data-name="memberpart" id="j-for-member">' +
				'<h2 class="f-cb">'+
					'<input type="text" placeholder="工号 / 姓名" />' +
					'<a href="javascript:void(0);" class="j-sch">搜索</a>'+
				'</h2>'+
				'<h4 class="f-cb">' +
					'<span class="wt1"> 可选项：<em class="f-ib"><input type="checkbox" class="j-is-son" value="0" /> 包含子组织</em></span>' +
					'<span class="wt2 j-count-wat"> 待选人员(0人)</span>' +
					'<span class="wt3 j-count-rst"> 已选人员(0人)</span>' +
				'</h4>'+
				'<fieldset class="lft">'+
					'<u class="j-swit z-on" data-char="part">按部门</u><div class="j-slt cnt"></div>'+
					'<u class="j-swit" data-char="team">按项目组</u><div class="cnt f-dn">sadasdasd</div>'+
				'</fieldset>' +
				'<div class="wait j-wat"></div>' +
				'<div class="ctr">' +
					'<a href="javascript:void(0);" data-type="all" class="j-add f-ib u-top">全部 &gt;&gt;</a>' +
					'<a href="javascript:void(0);" data-type="some" class="j-add f-ib u-top usp">添加 &gt;</a>' +
					'<a href="javascript:void(0);" data-type="some" class="j-del f-ib u-btm">&lt; 移除</a>' +
					'<a href="javascript:void(0);" data-type="all" class="j-del f-ib u-btm">&lt;&lt; 全部</a>' +
				'</div>' +
				'<div class="rgt j-rst"></div>' +
			'</div>';
}

function memberComplete(param){
	
	// 利用拷贝的新对象，防止delete影响原对象
	var _json = cloneObject(jsonData['part']);

	// 先获取递归对象
	var partTeam = jsonTree(_json, 0);

	// 再组装HTML并重新渲染到DOM
	var _dom = $('#j-for-member div.j-slt:first');
	_dom.html(htmlTree(partTeam, 'parent'));

	// 树节点尾元素增加样式
	addClassLast(_dom);

	// 利用拷贝的新对象，防止delete影响原对象
	var _json = cloneObject(jsonData['team']);
	// 先获取递归对象
	var partTeam = jsonTree(_json, 0);

	// 再组装HTML并重新渲染到DOM
	var _dom = $('#j-for-member div.j-slt:first').next('u').next('div');
	_dom.html(htmlTree(partTeam, 'parent'));

	// 树节点尾元素增加样式
	addClassLast(_dom);
}

function memberSure(param){

	var _list = $('#j-slt-member'),
		_parent = $('#j-for-member'),
		_option = _parent.find('div.j-rst:first');

	if(_option.find('input').length <= 0) return true;

	// 取出已存值
	var _has = {};
	_list.find('a').each(function(){
		var _k = $(this).attr('data-id');
		_has['z'+_k] = _k;
	});

	// 组装未存在的值
	var lst = '';
	_option.find('input').each(function(){
		var _this = $(this), _arr = _this.val().split('|');
		if(_has['z'+_arr[0]] === undefined){
			lst += '<a href="javascript:void(0);" data-id="'+ _arr[2] +'" class="f-ib">'+ _arr[0] +'('+ _arr[1] +')<i class="j-slt-del"></i></a>';	
		};
	});

	// 清空原选择区域
	_option.empty();

	// 清零已选
	_parent.find('span.j-count-rst:first').html(' 已选人员(0人)');

	// 渲染到HTML中
	_list.append($(lst)).find('span').hide();

	// 计算总的保费
	computeResult(_list, 'member', param);

	return true;
}

// ------------------- 华丽分割线 -------------------------------

// 选择 部门
POPUP.part = {
	'title': '选择部门', 'width': 700, 'top': 200, 'level' : 2,
	'content': 'partContent',  'complete': 'partComplete',
	'sure': {'txt':'确定', 'func': 'partSure'}, 'cancel': {'txt':'取消'}
};

function partContent(){
	return	'<div class="pop-tree f-cb j-zTree" data-name="part" id="j-for-part">' +
				'<h4 class="f-cb"><span class="wt1"> 可选部门：</span><span class="wt2"> 已选部门：</span></h4>'+
				'<div class="lft j-slt"></div>' +
				'<div class="ctr">' +
					'<a href="javascript:void(0);" class="f-ib u-add j-plus">添加 &gt;&gt;</a>' +
					'<a href="javascript:void(0);" class="f-ib u-clr j-clear">清空</a>' +
				'</div>' +
				'<div class="rgt j-rst"></div>' +
			'</div>';
}

function partComplete(param){
	
	// 利用拷贝的新对象，防止delete影响原对象
	var _json = cloneObject(jsonData['part']);

	// 先获取递归对象
	var partTeam = jsonTree(_json, 0);

	// 再组装HTML并重新渲染到DOM
	var _dom = $('#j-for-part div.j-slt:first');
	_dom.html(htmlTree(partTeam, 'parent'));

	// 树节点尾元素增加样式
	addClassLast(_dom);
}

function partSure(param){

	var _list = $('#j-slt-part'),
		_option = $('#j-for-part div.j-rst:first h5');

	if(_option.length <= 0) return true;

	// 取出已存值
	var _has = {};
	_list.find('a').each(function(){
		var _k = $(this).attr('data-id');
		_has['z'+_k] = _k;
	});

	// 组装未存在的值
	var lst = '';
	_option.each(function(){
		var _this = $(this), _val = _this.attr('data-val'), _arr = _val.split('|'), _txt = _arr[2] + ' - ' + _arr[1] + ' ('+ _arr[3] +'人)';
		if(_has['z'+_arr[0]] === undefined){
			lst += '<a href="javascript:void(0);" data-id="'+ _arr[0] +'" data-count="'+ _arr[3] +'" class="f-ib">'+ _txt +'<i class="j-slt-del"></i></a>';	
		};
	}).remove();

	// 渲染到HTML中
	_list.append($(lst)).find('span').hide();

	// 计算总的保费
	computeResult(_list, 'part', param);

	return true;
}

// ------------------- 华丽分割线 -------------------------------

// 选择 项目组
POPUP.team = {
	'title': '选择项目组', 'width': 700, 'top': 200, 'level' : 2,
	'content': 'teamContent', 'complete': 'teamComplete',
	'sure': {'txt':'确定', 'func': 'teamSure'}, 'cancel': {'txt':'取消'}
};

function teamContent(){
	return	'<div class="pop-tree f-cb j-zTree" data-name="team" id="j-for-team">' +
				'<h4 class="f-cb"><span class="wt1"> 可选项目组：</span><span class="wt2"> 已选项目组：</span></h4>' +
				'<div class="lft j-slt"></div>' +
				'<div class="ctr">' +
					'<a href="javascript:void(0);" class="f-ib u-add j-plus">添加 &gt;&gt;</a>' +
					'<a href="javascript:void(0);" class="f-ib u-clr j-clear">清空</a>' +
				'</div>' +
				'<div class="rgt j-rst"></div>' +
			'</div>';
}

function teamComplete(param){

	// 利用拷贝的新对象，防止delete影响原对象
	var _json = cloneObject(jsonData['team']);

	// 先获取递归对象
	var treeTeam = jsonTree(_json, 0);

	// 再组装HTML并重新渲染到DOM
	var _dom = $('#j-for-team div.j-slt:first');
	_dom.html(htmlTree(treeTeam));

	// 树节点尾元素增加样式
	addClassLast(_dom);
}

function teamSure(param){

	var _list = $('#j-slt-team'),
		_option = $('#j-for-team div.j-rst:first h5');

	if(_option.length <= 0) return true;

	// 取出已存值
	var _has = {};
	_list.find('a').each(function(){
		var _k = $(this).attr('data-id');
		_has['z'+_k] = _k;
	});

	// 组装未存在的值
	var lst = '';
	_option.each(function(){
		var _this = $(this), _val = _this.attr('data-val'), _arr = _val.split('|'), _txt = _arr[2] + ' - ' + _arr[1] + ' ('+ _arr[3] +'人)';
		if(_has['z'+_arr[0]] === undefined){
			lst += '<a href="javascript:void(0);" data-id="'+ _arr[0] +'" data-count="'+ _arr[3] +'" class="f-ib">'+ _txt +'<i class="j-slt-del"></i></a>';	
		};
	}).remove();

	// 渲染到HTML中
	_list.append($(lst)).find('span').hide();

	// 计算总的保费
	computeResult(_list, 'team', param);

	return true;
}

// ------------------- 华丽分割线 -------------------------------

var zTree = 'div.j-zTree'; // 树状结构1(zTree)
var zTreePlus = 'div.j-zTree-plus'; // 树状结构2(zTreePlus)

// [委托函数] 树状结构的折叠 树状结构2(zTreePlus) && 树状结构1(zTree)
$(document.body).on('click', zTree + ' i, '+ zTreePlus +' i', function(){

	var _this = $(this);
	if(_this.text() === '-'){
		_this.text('+').parent().next('div').slideUp();
	}else{
		_this.text('-').parent().next('div').slideDown();
	}

	return false;
});
 
// [委托函数] 清空操作 树状结构1(zTree)
$(document.body).on('click', zTree + ' a.j-clear', function(){

	$(this).parents(zTree).find('div.j-rst:first').empty();

	return false;
});

// [委托函数] 单个删除操作 树状结构1(zTree)
$(document.body).on('click', zTree + ' a.j-del', function(){

	$(this).parent().remove();

	return false;
});

// [委托函数] 树状结构的级联选择 树状结构1(zTree)
// 注：目前只针对 选择部门(part)
$(document.body).on('click', zTree + ' input', function(){

	var _that = $(this);

	if(_that.parents(zTree).attr('data-name') == 'part'){
		var _checked = false;
		if(_that.is(':checked')) _checked = true;
		_that.parent().parent().next('div').find('input').prop('checked', _checked);
	}
});

// [委托函数] 添加操作 树状结构1(zTree)
$(document.body).on('click', zTree + ' a.j-plus', function(){

	var _that = $(this), _parent = _that.parents(zTree);
	var _slt = _parent.find('div.j-slt:first'),
		_rst = _parent.find('div.j-rst:first');

	// 获取此前选中的
	var _wait = _slt.find('input:checked');
	if(_wait.length <= 0) return false;

	// 获取已存在的数组 以免重复
	var _has = {};
	_rst.find('h5').each(function(){
		var _val = $(this).attr('data-val'), _arr = _val.split('|');
		_has['z'+_arr[0]] = _val;
	});

	// 重置所有多选框为未选状态
	_slt.find('input').prop('checked', false);

	var _temp = {}, _n = 0;
	$.each(_wait, function(){
		var _this = $(this), _val = _this.val(), _arr = _val.split('|');
		if(_has['z'+_arr[0]] === undefined){
			var _pus = {'name': _arr[3], 'pid':_arr[1], 'count':_arr[2]};
			_temp['z' + _arr[0]] = _pus;
			_n++;
		}
	});

	// 获取对应的数据
	var _jsonParent = jsonData[_parent.attr('data-name')];

	var _opt = '';
	$.each(_temp, function(i, rs){

		var _fname = '';
		if(rs['pid'] > 0) _pName = _jsonParent['z' + rs['pid']]['name'];
		
		_opt += '<h5 class="f-cb" data-val="'+ i.substr(1) +'|'+ rs['name'] +'|'+ _pName +'|'+ rs['count'] +'">';
		_opt += '<span class="f-fl">'+ _pName +' - '+ rs['name'] +' ('+ rs['count'] +'人)</span>';
		_opt += '<a href="javascript:void(0);" class="j-del f-fr">&nbsp;</a></h5>';
	});

	// 追加[因为是清除了重复]
	_rst.append($(_opt));

	return false;
});
 
// --------- 树状结构2(zTreePlus)

// 点击存储的数据
var clickKeepMember = {}; 

// [委托函数] 人员的选择(基于部门) 树状结构2(zTreePlus)
$(document.body).on('click', zTreePlus + ' div.j-slt:first input', function(){

	var _that = $(this), _val = _that.val();
	var _id = _val.split('|')[0], _checked = _that.is(':checked');

	var _parent = _that.parents(zTreePlus), _chr = _parent.attr('data-name');

	// 部门对应下的人员
	// 是否包含子集
	var _isSon = _parent.find('.j-is-son:first').is(':checked');
	var _divSon = _that.parent().parent().next('div');

	if(_isSon && _divSon.length > 0){

		var _son = _divSon.find('input');

		// 级联选中状态
		if(_checked){
			_son.prop('checked', true);
		}else{
			_son.prop('checked', false);
		}

		// 计算级联的值
		var _temp = [];
		if(jsonData[_chr]['z' + _id] !== undefined) _temp.push(jsonData[_chr]['z' + _id]);

		_son.each(function(){
			var _pid = $(this).val().split('|')[0];
			if(jsonData[_chr]['z' + _pid] !== undefined) _temp.push(jsonData[_chr]['z' + _pid]);
		});

		// 拆算组装新的数据集
		if(_temp.length > 1){
			var _pep = {}, i = 0, n = _temp.length;
			for(; i<n; i++){
				var k, item = _temp[i];
				for(k in item){
					_pep[k] = item[k];
				}
			}
		}else if(_temp.length === 1){
			var _pep = _temp[0];
		}
		
	}else{

		var _pep = jsonData[_chr]['z' + _id];
	}

	if(_pep !== undefined){

		for(var _i in _pep){
		
			if(_checked){
				clickKeepMember['z' + _i] = _pep[_i] + '|' + _i;
			}else{
				delete clickKeepMember['z' + _i];
			}
		}

		var _opt = '', x = 0;
		$.each(clickKeepMember, function(i, vrs){
		
			var _rs = vrs.split('|');
		
			_opt += '<h5><label><input type="checkbox" value="'+ vrs +'"> '+ _rs[1] + ' (工号：' + _rs[0] + ')</label></h5>';
			x++; // 用于计数
		});

		_parent.find('div.j-wat:first').html($(_opt));
		_parent.find('span.j-count-wat:first').html(' 待选人员('+ x +'人)');
	}
});

// [委托函数] 添加人员操作 树状结构2(zTreePlus)
$(document.body).on('click', zTreePlus + ' a.j-add', function(){

	var _that = $(this), _parent = _that.parents(zTreePlus);
	var _slt = _parent.find('div.j-slt:first'),
		_wat = _parent.find('div.j-wat:first'),
		_rst = _parent.find('div.j-rst:first'),
		_watCount = _parent.find('span.j-count-wat:first'),
		_rstCount = _parent.find('span.j-count-rst:first');

	// 分辨 全部 或 多个操作
	var _type = _that.attr('data-type');

	// 先获取当前预操作项
	var _all = _type === 'all' ? _wat.find('input') : _wat.find('input:checked');
	if(_all.length <= 0) return false;

	// 重置 部门 多选框状态
	_slt.find('input').prop('checked', false);

	// 清除缓存数据
	clickKeepMember = {};

	// 先获取已存在的数组 以免重复
	var _has = {};
	_rst.find('input').each(function(){
		var _val = $(this).val(), _arr = _val.split('|');
		_has['z'+_arr[2]] = _val;
	});

	// 仅仅 全部移除的操作
	if(_type === 'all') _wat.empty();

	// 结合已有数据 组装新的数据(忽略重复)
	var _temp = {};
	$.each(_all, function(){

		var _this = $(this), _val = _this.val(), _arr = _val.split('|');

		// 忽略已经存在的[保留新的]
		if(_has['z'+_arr[2]] === undefined) _temp['z' + _arr[2]] = _arr[1] + '|' + _arr[0];

		// 仅仅 多个移除的操作
		if(_type === 'some') _this.parent().parent().remove();
	});

	// 利用新的数据生成HTML
	var _opt = '';
	$.each(_temp, function(i, rs){
		var _id = i.substr(1), _rs = rs.split('|');
		_opt += '<h5><label><input type="checkbox" value="'+ rs + '|' + _id +'"> '+ _rs[0] + ' (工号：' + _rs[1] + ')</label></h5>';
	});

	// 追加到已有的部分[因为过滤了重复]
	_rst.append($(_opt));

	_watCount.html(' 待选人员('+ _wat.find('input').length +'人)');
	_rstCount.html(' 已选人员('+ _rst.find('input').length +'人)');

	return false;
});

// [委托函数] 移除人员操作 树状结构2(zTreePlus)
$(document.body).on('click', zTreePlus + ' a.j-del', function(){

	var _that = $(this), _parent = _that.parents(zTreePlus);
	var _slt = _parent.find('div.j-slt:first'),
		_wat = _parent.find('div.j-wat:first'),
		_rst = _parent.find('div.j-rst:first'),
		_watCount = _parent.find('span.j-count-wat:first'),
		_rstCount = _parent.find('span.j-count-rst:first');

	// 分辨 全部 或 多个操作
	var _type = _that.attr('data-type');

	// 先获取当前预操作项
	var _all = _type === 'all' ? _rst.find('input') : _rst.find('input:checked');

	if(_all.length <= 0) return false;

	// 先获取已存在的数组 以免重复
	var _has = {};
	_wat.find('input').each(function(){
		var _val = $(this).val(), _arr = _val.split('|');
		_has['z'+_arr[2]] = _val;
	});

	// 仅仅 全部移除的操作
	if(_type === 'all') _rst.empty();

	// 结合已有数据 重新新的数据(忽略重复)
	var _temp = {};
	$.each(_all, function(){

		var _this = $(this), _val = _this.val(), _arr = _val.split('|');

		// 忽略已经存在的[保留新的]
		if(_has['z'+_arr[2]] === undefined) _temp['z' + _arr[2]] = _arr;

		// 仅仅 多个移除的操作
		if(_type === 'some') _this.parent().parent().remove();
	});

	// 利用新的数据生成HTML
	var _opt = '';
	$.each(_temp, function(i, rs){
		var val = rs.join('|');
		clickKeepMember['z'+ rs[2]] = val;
		_opt += '<h5><label><input type="checkbox" value="'+ val +'"> '+ rs[0] + ' (工号：' + rs[1] + ')</label></h5>';
	});

	// 追加到已有的部分[因为过滤了重复]
	_wat.append($(_opt));

	_watCount.html(' 待选人员('+ _wat.find('input').length +'人)');
	_rstCount.html(' 已选人员('+ _rst.find('input').length +'人)');

	return false;
});

// 搜索[人员]操作
$(document.body).on('click', zTreePlus + ' a.j-sch', function(){

	var _that = $(this), _parent = _that.parents(zTreePlus);
	var _watCount = _parent.find('span.j-count-wat:first'),
		_wat = _parent.find('div.j-wat:first');

	// 输入框的值
	var _val = $.trim(_that.prev('input').val()), _url = searchUrl + '?backFunc=?';

	$.ajax({
		async: true, url: _url, type: 'GET', dataType: 'jsonp', data : {'keywords': _val}, timeout: 5000,
        success: function(_json){

        	for(var _i in _json) clickKeepMember['z' + _i] = _json[_i] + '|' + _i;

			var _opt = '', x = 0;
			$.each(clickKeepMember, function(i, vrs){
			
				var _rs = vrs.split('|');
				_opt += '<h5><label><input type="checkbox" value="'+ vrs +'"> '+ _rs[1] + ' (工号：' + _rs[0] + ')</label></h5>';
				x++;
			});

			_wat.html($(_opt));

			_watCount.html(' 待选人员('+ x +'人)');
        }
    });

	return false;
});

// 选择切换
$(document.body).on('click', zTreePlus + ' u.j-swit', function(){

	var _that = $(this), _parent = _that.parent('fieldset'), _cnt = _that.next('div'), _on = 'z-on';

	if(_that.hasClass(_on)) return false;

	var chr = _that.attr('data-char');
	$('#j-for-member').attr('data-name', (chr === 'part' ? 'memberpart' : 'memberteam')).find('div.j-wat:first').empty();

	// 清除缓存数据
	clickKeepMember = {};

	_parent.find('u.j-swit').removeClass(_on);
	_that.addClass(_on);

	_parent.find('div.j-slt:first').removeClass('j-slt').slideUp().find('input').prop('checked', false);
	_cnt.addClass('j-slt').slideDown();

	_that.parents(zTreePlus).find('span.j-count-wat:first').html(' 待选人员(0人)');
});