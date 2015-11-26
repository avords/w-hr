<#assign domain="${rc.contextPath}" />
<#macro sit title="" mod="index" header="/frontend/header.ftl" 
js=[] 
css=[] 
component=[] 
cache=false nav=true navfloor=false notfold=false menuShow='all' skin="" headerIndex=1 bottom=1 rightside=false pageClass="">
<!doctype html>
<html>
	<#include header>
	<body class="page-index ${skin}">
		<#include "/common/member_toolbar.ftl"/>
		<#if headerIndex==1>
		<#include "/frontend/body_header_logo.ftl"/>
		<#elseif headerIndex==2>
		<script src="${domain}/static/lib/jquery/jquery-ui.min.js"></script>
		<script src="${domain}/static/js/youanAjaxLogin.js"></script>
		<#include "/frontend/shop/body_header_logo.ftl"/>
		<#elseif headerIndex==3>
		<#include "/frontend/buyer/body_header_logo.ftl"/>
		<#elseif headerIndex==4>
		<#include "/frontend/seller/body_header_logo.ftl"/>
		<#elseif headerIndex==5>
		<#include "/frontend/purchase/body_header_logo.ftl"/>
		</#if>
		<#if nav==true>
		<#include "/frontend/navigation.ftl"/>
		</#if>
		<#nested>
		<#include "/frontend/body_footer.ftl"/>
		<#if rightside==true>
		<#include "/frontend/body_rightside.ftl"/>
		</#if>
	</body>
</html>
</#macro>

<#macro advWord adv="" code="empty">
	<a <#if adv.adHot=='H'>class="hot"<#elseif adv.adHot=='N'>class="new"</#if> href="<#if adv.targetUrl?if_exists>${adv.targetUrl}</#if>" <#if adv.targetWindow?if_exists><#if adv.targetWindow==1>target="_blank"</#if></#if>>
		${adv.title}
		<#if adv.adHot=='H'|| adv.adHot=='N'><i class="ico"></i></#if>
	</a>
</#macro>

<#macro advPic adv="" code="empty">
	<a href="<#if adv.targetUrl?if_exists>${adv.targetUrl}</#if>" <#if adv.targetWindow?if_exists><#if adv.targetWindow==1>target="_blank"</#if></#if>>
		<img src="<@imageDownload imgUrl="${adv.imgUrl}" code="${code}"/>" alt="${adv.title}">
	</a>
</#macro>


<#macro sit1 title="" mod="index" header="/frontend/header.ftl" 
js=[] 
css=[] 
component=[] 
cache=false nav=true navfloor=false notfold=false menuShow='all' headerIndex=1 >
<!doctype html>
<html>
	<#include header>
	<body class="page-index">
		<#include "/common/member_toolbar.ftl"/>
		<#if headerIndex==1>
		<#include "/frontend/body_header_logo_simple.ftl"/>
		<#elseif headerIndex==2>
		<#include "/frontend/shop/body_header_logo.ftl"/>
		<#elseif headerIndex==3>
		<#include "/frontend/buyer/body_header_logo.ftl"/>
		<#elseif headerIndex==4>
		<#include "/frontend/seller/body_header_logo.ftl"/>
		</#if>
		<#if nav==true>
		<#include "/frontend/navigation_simple.ftl"/>
		</#if>
		<#nested>
		<#include "/frontend/body_footer_simple.ftl"/>
	</body>
</html>
</#macro>

<#macro sit2 title="" mod="index" header="/frontend/header.ftl" 
js=[] 
css=[] 
component=[] 
cache=false nav=true navfloor=false notfold=false menuShow='all' headerIndex=1 >
<!doctype html>
<html>
	<#include header>
	<body class="page-index">
		<#include "/common/member_toolbar.ftl"/>
		
	</body>
</html>
</#macro>