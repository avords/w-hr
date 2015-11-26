<#assign domain="${rc.contextPath}" />
<div class="layout-navig">
	<div class="container clearfix">
		<ul class="navig-main <#if notfold==true>notfold</#if>">
			<li class="all <#if notfold==true>notfold</#if>"><a>全部商品分类 <i class="ico"></i></a></li>
			<li><a href="${domain}/index.htm">首页</a></li>
			<#--<li><a href="${domain}/presell.htm">优安预售<i class="ico ico-newest"></i></a></li>-->
			<li><a href="http://info.yaxp.com:8888/data/index.htm" target="_blank">预售<i class="ico ico-newest"></i></a></li>
			<li><a href="${domain}/search/p.htm?catIds=4">冷冻海鲜</a></li>
			<li><a href="${domain}/search/p.htm?catIds=5">生鲜果蔬</a></li>
			<li><a href="${domain}/search/p.htm?catIds=12">酒/饮料</a></li>
			<li><a href="${domain}/search/p.htm?catIds=6">乳制品</a></li>
			<li><a href="${domain}/service/front/serviceInfo.htm">服务信息</a></li>
			<li><a href="${domain}/cms/content/index.htm">优安资讯</a></li>
		</ul>
		<#if menuShow=='all'>
				<div class="navig-sub <#if notfold==true>notfold</#if>">
				<ul>
				<#list indexMap.catList as cat1>
				<#if cat1.catId!=3>
				<li class="level-first">
					<dl>
						<dt>
						<a href="${domain}/channel/${cat1.catCode}.htm" target="_blank">
						${cat1.catName}
						</a>
						</dt>
						<dd>
						<#list cat1.recommendSkuCatMenus as recommend>
							<a href="${domain}/search/p.htm?catIds=${recommend.catId}" target="_blank">${recommend.aliasName}</a>
							<#if recommend_has_next>|</#if>
						</#list>
						</dd>
					</dl>
					<#if cat1.childList?if_exists>
					<div class="level-second">
					<#list cat1.childList as cat2>
					<#if cat2.childList?size!=0>
						<dl class="clearfix">
							<dt>
							<a href="${domain}/search/p.htm?catIds=${cat2.catId}" target="_blank">${cat2.catName}</a>
							</dt>
								<dd>
								<#list cat2.childList as cat3>
									<a href="${domain}/search/p.htm?catIds=${cat3.catId}" target="_blank">${cat3.catName}</a>
									<#if cat3_has_next>|</#if>
								</#list>
								</dd>
						</dl>
					<#elseif cat2.hasProduct == true>
						<dl>
							<dt>
							<a href="${domain}/search/p.htm?catIds=${cat2.catId}" target="_blank">${cat2.catName}</a>
							</dt>
								<dd>&nbsp;</dd>
						</dl>
					</#if>
					</#list>
					</div>
					</#if>
				</li>
				</#if>
				</#list>
				<#list indexMap.catList as cat1>
				<#if cat1.catId==3>
				<li class="level-first">
					<dl>
						<dt>
						<a href="javascript:void(0);">
						${cat1.catName}
						</a>
						</dt>
						<dd>
						<#list cat1.recommendSkuCatMenus as recommend>
							<a href="javascript:void(0);">${recommend.aliasName}</a>
							<#if recommend_has_next>|</#if>
						</#list>
						</dd>
					</dl>
				</li>
				</#if>
				</#list>
				<li class="level-first origin">
						<dl>
							<dt>产地</dt>
						</dl>
				
						<div class="level-second">
						<dl class="clearfix">
							<dd>
							<a href='${domain}/search/p.htm?originId=152'>澳大利亚</a> |
							<a href='${domain}/search/p.htm?originId=163'>法国</a> |
							<a href='${domain}/search/p.htm?originId=178'>日本</a> |
							<a href='${domain}/search/p.htm?originId=115'>美国</a> |
							<a href='${domain}/search/p.htm?originId=80'>意大利</a> |
							<a href='${domain}/search/p.htm?originId=204'>德国</a> |
							<a href='${domain}/search/p.htm?originId=222'>西班牙</a> |
							<a href='${domain}/search/p.htm?originId=94'>新西兰</a> |
							<a href='${domain}/search/p.htm?originId=154'>泰国</a> |
							<a href='${domain}/search/p.htm?originId=209'>韩国</a> |
							<a href='${domain}/search/p.htm?originId=207'>马来西亚</a> |
							<a href='${domain}/search/p.htm?originId=84'>台湾</a> |
							<a href='${domain}/search/p.htm?originId=67'>丹麦</a> |
							<a href='${domain}/search/p.htm?originId=48'>比利时</a> |
							<a href='${domain}/search/p.htm?originId=190'>越南</a> |
							<a href='${domain}/search/p.htm?originId=144'>波兰</a> |
							<a href='${domain}/search/p.htm?originId=214'>保加利亚</a> |
							<a href='${domain}/search/p.htm?originId=179'>爱尔兰</a> |
							<a href='${domain}/search/p.htm?originId=71'>荷兰</a> |
							<a href='${domain}/search/p.htm?originId=151'>瑞士</a> |
							<a href='${domain}/search/p.htm?originId=63'>奥地利</a> |
							<a href='${domain}/search/p.htm?originId=123'>智利</a> |
							<a href='${domain}/search/p.htm?originId=182'>希腊</a> |
							<a href='${domain}/search/p.htm?originId=141'>捷克</a> |
							<a href='${domain}/search/p.htm?originId=126'>加拿大</a> |
							<a href='${domain}/search/p.htm?originId=124'>土耳其</a> |
							<a href='${domain}/search/p.htm?originId=68'>英国</a> |
							<a href='${domain}/search/p.htm?originId=186'>格陵兰（丹）</a> |
							<a href='${domain}/search/p.htm?originId=229'>印度尼西亚</a> |
							<a href='${domain}/search/p.htm?originId=53'>新加坡</a> |
							<a href='${domain}/search/p.htm?originId=200'>牙买加</a> |
							<a href='${domain}/search/p.htm?originId=242'>南非</a> |
							<a href='${domain}/search/p.htm?originId=219'>巴西</a> |
							<a href='${domain}/search/p.htm?originId=22'>莫桑比克</a> |
							<a href='${domain}/search/p.htm?originId=58'>墨西哥</a> |
							<a href='${domain}/search/p.htm?originId=62'>阿根廷</a> |
							<a href='${domain}/search/p.htm?originId=138'>乌拉圭</a> |
							<a href='${domain}/search/p.htm?originId=201'>挪威</a>
							
							<#--<#list indexMap.districts as item>
								<a href="${domain}/search/p.htm?originId=${item.catId}">${item.catName}</a>
								<#if item_has_next>|</#if>
							</#list>-->
							</dd>
						</dl>
					</div>
				</li>
			</ul>
		</div>
		<#elseif menuShow=='levelfistonly'>
				<div class="navig-sub1">
				<ul>
				<#list indexMap.catList as cat1>
				<#if cat1.catId!=3>
					<li class="level-first-only">
						<a href="${domain}/channel/${cat1.catCode}.htm" target="_blank">${cat1.catName}</a>
					</li>
				</#if>
				</#list>
				<#list indexMap.catList as cat1>
				<#if cat1.catId==3>
					<li class="level-first-only">
						<a href="javascript:void(0)">${cat1.catName}</a>
					</li>
				</#if>
				</#list>
				</ul>
				</div>
		</#if>
	</div>
</div>