<div class="floater-rightside">
	<div class="qrcode">
		<a href="#" class="qr"><img src="${staticDomain}/youansit/images/front/qr_code.png" alt=""></a>
		<a href="#" class="ico close" title="关闭">关闭</a>
	</div>
	<#if navfloor==true>
	<div class="nav-floor">
		<h3 class="top trigger"><i class="ico"></i></h3>
		<ul class="list">
			<#assign index = 0>
			<#list indexMap.catList as cat1>
			<#if cat1.catId!='3'>
			<#assign index = index+1>
			<li><span class="num">${index}F</span><span class="tit">${cat1.catName}</span></li>
			</#if>
			</#list>
		</ul>
		<div class="bottom trigger"><i class="ico"></i></div>
	</div>
	</#if>
</div>
<div class="goto-top">
	<span class="btn" title="回到顶部"><i class="ico"></i></span>
</div>