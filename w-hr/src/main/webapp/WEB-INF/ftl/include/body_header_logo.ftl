<div class="layout-header">
	<div class="container clearfix">
		<h1 class="ico logo"><a href="${domain}/index.htm">标题信息...........</a></h1>
		<div class="ico slogan">slogan</div>
		<div class="header_food_img"><img src="${staticDomain}/youansit/images/front/top156-64.png" width="156" height="64" border="0" alt="">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="${staticDomain}/youansit/images/front/header_food_img.jpg"/></div>
		<div class="searchbar">
			<#include "/common/header_search_form.ftl">
			<div class="keyword" id="rSkuCatSearchsKeywords">
			</div>
			<script>
				$(function(){
					var url = domain + "/loadKeywords.htm?m="+Math.random();
					$.get(url,function(data){
						if(data&&data.length>0){
							$('#rSkuCatSearchsKeywords').html('');
							$.each(data,function(i,n){
								var name = n.aliasName;
								var a = $("<a />");
								a.attr('href',domain+'/search/p.htm?title='+encodeURI(name));
								a.attr('target','_blank');
								a.html(name);
								$('#rSkuCatSearchsKeywords').append(a);
							});
						}
						
					});
				});
			</script>
		</div>
		<div class="others"><img src="${staticDomain}/youansit/images/front/youan_tag.jpg" alt=""></div>
	</div>
</div>