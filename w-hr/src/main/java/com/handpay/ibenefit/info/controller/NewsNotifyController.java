/**
 * @Title: NewsNotifyController.java
 * @Package com.handpay.ibenefit.info.controller
 * @Description: TODO
 * Copyright: Copyright (c) 2011 
 * 
 * @author Mac.Yoon
 * @date 2015-7-9 上午9:44:18
 * @version V1.0
 */

package com.handpay.ibenefit.info.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.member.entity.CompanyPublished;
import com.handpay.ibenefit.member.service.ICompanyPublishedManager;
import com.handpay.ibenefit.news.entity.NewsNotify;
import com.handpay.ibenefit.news.service.INewsNotifyManager;
import com.handpay.ibenefit.security.SecurityConstants;

/**
 * @ClassName: NewsNotifyController
 * @Description: TODO
 * @author Mac.Yoon
 * @date 2015-7-9 上午9:44:18
 *
 */
@Controller
@RequestMapping("newsNotify")
public class NewsNotifyController extends BaseController{
	@Reference(version="1.0")
	private INewsNotifyManager newsNotifyManager;
	@Reference(version="1.0")
	private ICompanyPublishedManager companyPublishedManager;
	
	@RequestMapping("list")
	public String list(HttpServletRequest request,HttpServletResponse response,NewsNotify infomation,Integer backPage){
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		CompanyPublished company = companyPublishedManager.getByObjectId(companyId);
		PageSearch page  = PageUtils.preparePage(request,NewsNotify.class,true);
		String cp = request.getParameter("c_p");
		if(company.getAreaId().length()==6){
			page.getFilters().add(new PropertyFilter(NewsNotify.class.getName(),"LIKES_city",company.getAreaId().substring(0,4)));
		}else if(company.getAreaId().length()==4){
			page.getFilters().add(new PropertyFilter(NewsNotify.class.getName(),"LIKES_city",company.getAreaId()));
		}
		page.getFilters().add(new PropertyFilter(NewsNotify.class.getName(),"EQI_status",String.valueOf(IBSConstants.ADVERT_PUBLISHED)));
		Integer currPage = 1;
		if(cp!=null&&!("").equals(cp)){
			currPage = Integer.parseInt(cp);
		}
		String[] sortOrders = {"asc","desc"};
		String[] sortProperties = {"priority","createDate"};
		page.setSortOrders(sortOrders);
		page.setSortProperties(sortProperties);
		page.setCurrentPage(currPage);
		handleFind(page);
		List<NewsNotify> items = page.getList();
		request.setAttribute("items", items);
		request.setAttribute("pageInfomation", page);
		return "info/news";
	}
	
	protected void handleFind(PageSearch page) {
		PageSearch result = newsNotifyManager.getNewsNotify(page);
		page.setTotalCount(result.getTotalCount());
		page.setList(result.getList());
	}
	
	@RequestMapping("detail/{objectId}")
	public String detail(HttpServletRequest request,HttpServletResponse response,@PathVariable Long objectId){
		NewsNotify newsNotify = newsNotifyManager.getByObjectId(objectId);
		request.setAttribute("newsNotify", newsNotify);
		return "info/newsDetail";
	}
}
