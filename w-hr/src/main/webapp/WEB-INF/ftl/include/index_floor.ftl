<#assign domain = "${rc.contextPath}">
<#assign index = 0>
<#list indexMap.catList as cat1>
<#if cat1.catId!='3'>
<#assign index = index+1>
		
			<#list indexMap.advList as item>
				<#if item.postionCode=='home_floor_'+(cat1_index+1)+'_top'>
				<div class="banner-1200">
				<a href="<#if item.targetUrl?if_exists>${item.targetUrl}</#if>" 
				<#if item.targetWindow?if_exists>
				<#if item.targetWindow==1>target="_blank"</#if>
				</#if> >
				<img src="<@imageDownload imgUrl="${item.imgUrl}" code="1200X75"/>" alt="${item.title}">
				</a>
				</div>
				</#if>
			</#list>
		
			<div class="mod-floor clearfix index-mod-floor">
			<div class="header">
				<h2 class="tit"><span>${index}F</span> ${cat1.catName}</h2>
				<p class="into"><a class="btn" href="${domain}/search/p.htm?catIds=${cat1.catId}">进入${cat1.catName}频道 <i class="ico"></i></a></p>
				<p class="keywords">
					<#list cat1.recommendSkuCatShows as recommend>
					    <a href="${domain}/search/p.htm?catIds=${recommend.catId}" target="_blank">${recommend.aliasName}</a>
						<#if recommend_has_next>&nbsp;&nbsp;|&nbsp;</#if>
					</#list>
				</p>
			</div>
			<div class="content">
				<div class="goods-list">
					<ul class="list">
						<#list cat1.recommendCatPhyPros as item>
							<li pid="${item.productSimpleVo.productId}">
								<div class="wrap clearfix">
									<p class="pic">
									<a target="_blank" href="${domain}/product/physical/${item.productSimpleVo.productId}.htm">
									<#if item.imgUrl?if_exists> <#assign url = item.imgUrl /> <#else> <#assign url = item.productSimpleVo.mainProductImg /></#if>
									<img class="lazyload" src="" data-src="<@imageDownload imgUrl='${url}' code="seller.product.frontIndexFloor"/>" alt="">
									</a>
									</p>
									<p class="tit"><a target="_blank" href="${domain}/product/physical/${item.productSimpleVo.productId}.htm">${item.productSimpleVo.title}</a></p>
									<p class="info">规格：${item.productSimpleVo.productSpec} <br/>产地：${item.productSimpleVo.originName}</p>
									<p class="price">
									<span class="f000">批发价：</span>
									<span class="currprice"></span>
									</p>
								</div>
							</li>
						</#list>
					</ul>
				</div>
				
				<div class="mod-side mod-side-rank">
					<h3 class="title">销售排行榜</h3>
					<div class="content">
						<ul class="list">
						<#list cat1.hotSalePhyPros as item>
							<li <#if item_index==0> class="nofold" pid="${item.productSimpleVo.productId}" </#if> >
								<#if item_index==0>
								<div class="big">
									<p class="ico num num${item_index+1}"><img src="${staticDomain}/youansit/images/front/icon_rank_num1-01.png" alt=""></p>
									<p class="pic"><a target="_blank" href="${domain}/product/physical/${item.productSimpleVo.productId}.htm">
									<#if item.imgUrl?if_exists> <#assign url = item.imgUrl /> <#else> <#assign url = item.productSimpleVo.mainProductImg /></#if>
									<img src="<@imageDownload imgUrl='${url}' code="seller.product.frontIndexFloor"/>" alt="">
									</a>
									</p>
									<p class="tit"><a target="_blank" href="${domain}/product/physical/${item.productSimpleVo.productId}.htm">${item.productSimpleVo.title}</a></p>
									<p class="info">规格：${item.productSimpleVo.productSpec} <br/>产地：${item.productSimpleVo.originName}</p>
									<p class="price"><span class="f000">批发价：</span>
									<span class="currprice"></span>
									<!--￥${item.productSimpleVo.minPrice?string(',##0.00')}-￥${item.productSimpleVo.maxPrice?string(',##0.00')}-->
									</p>
								</div>
								<#else>
								<div class="small">
									<p class="ico num">${item_index+1}</p>
									<p class="tit" style="width:180px;"><a href="${domain}/product/physical/${item.productSimpleVo.productId}.htm" target="_blank">${item.productSimpleVo.title}</a></p>
									<#--<p class="count">${item.productSimpleVo.monthVolume}${item.productSimpleVo.specDictName}</p>-->
								</div>
								</#if>
							</li>
						</#list>
						</ul>
					</div>
				</div>
				<#list indexMap.advList as item>
				<#if item.postionCode=='home_floor_'+(cat1_index+1)+'_right'>
				<div class="banner">
				<a href="<#if item.targetUrl?if_exists>${item.targetUrl}</#if>" 
				<#if item.targetWindow?if_exists>
				<#if item.targetWindow==1>target="_blank"</#if>
				</#if> >
				<img src="<@imageDownload imgUrl="${item.imgUrl}" code="362x204220X154"/>" alt="${item.title}">
				</a>
				</div>
				</#if>
				</#list>
			</div>
		</div>
		</#if>
		</#list>