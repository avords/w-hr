/**
 * @Title: InfomationController.java
 * @Package com.handpay.ibenefit.info.controller
 * @Description: TODO
 * Copyright: Copyright (c) 2011
 *
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:54:13
 * @version V1.0
 */

package com.handpay.ibenefit.info.controller;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.entity.ForeverEntity;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.framework.util.UeditorUtils;
import com.handpay.ibenefit.info.entity.Infomation;
import com.handpay.ibenefit.info.entity.InfomationNotice;
import com.handpay.ibenefit.info.service.IInfomationManager;
import com.handpay.ibenefit.info.service.IInfomationNoticeManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;

/**
 * @ClassName: InfomationController
 * @Description: TODO
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:54:13
 *
 */
@Controller
@RequestMapping("infomation")
public class InfomationController extends BaseController{

	@Reference(version="1.0")
	private IInfomationManager infomationManager;
	
	@Reference(version="1.0")
	private IInfomationNoticeManager infomationNoticeManager;
	
	@Reference(version="1.0")
	private IUserManager userManager;
	
	@RequestMapping("index")
	public String index(HttpServletRequest request,HttpServletResponse response){
		return "info/index";
	}
	
	
	@RequestMapping("listNotice")
	public String listNotice(HttpServletRequest request,HttpServletResponse response,Infomation infomation,Integer backPage){
		infomationManager.updateValidateInfomation();
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		PageSearch page  = PageUtils.preparePage(request,Infomation.class,true);
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		page.getFilters().add(new PropertyFilter(Infomation.class.getName(),"EQL_companyId",companyId.toString()));
		if(IBSConstants.USER_TYPE_COMPANY_HR == user.getType()){
			page.getFilters().add(new PropertyFilter(Infomation.class.getName(),"EQL_createdBy",user.getObjectId().toString()));
		}
		String cp = request.getParameter("c_p");
		Integer currPage = 1;
		if(cp!=null&&!("").equals(cp)){
			currPage = Integer.parseInt(cp);
		}
		page.setSortOrder("asc");
		page.setSortProperty("createdOn");
		page.setCurrentPage(currPage);
		handleFind(page);
		List<Infomation> items = page.getList();
		request.setAttribute("items", items);
		request.setAttribute("pageInfomation", page);
		return "info/listNotice";
	}
	
	protected void handleFind(PageSearch page) {
		page.getFilters().add(new PropertyFilter(Infomation.class.getName(),"EQI_deleted",ForeverEntity.DELETED_NO + ""));
		PageSearch result = infomationManager.findList(page);
		page.setTotalCount(result.getTotalCount());
		page.setList(result.getList());
	}

	@RequestMapping("addNotice")
	public String addNotice(HttpServletRequest request,HttpServletResponse response){
		request.setAttribute("type", IBSConstants.HR_NOTICE);
		return getFileBasePath()+"addNotice";
	}
	
	@RequestMapping("saveNotice")
	public String saveNotice(HttpServletRequest request,HttpServletResponse response,Infomation t){
		User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		t.setType(IBSConstants.HR_NOTICE);
		t.setCreatedOn(new Date());
		t.setCreatedBy(user.getObjectId());
		t.setCompanyId(user.getCompanyId());
		if(IBSConstants.USER_TYPE_COMPANY_ADMIN != user.getType()){
			t.setOrganizationId(user.getOrganizationId());
		}
		t.setContent(UeditorUtils.convertSpace(t.getContent()));
		if(IBSConstants.PLATFORM_NOTICE_ALL_USER.equals(t.getFaceMember())){
			t.setFacePeople(IBSConstants.STATUS_YES);
		}else{
			t.setFacePeople(IBSConstants.STATUS_NO);
		}
		t.setTitle(t.getTitle().trim());
		t=infomationManager.save(t);
		InfomationNotice delInfomationNotice = new InfomationNotice();
		delInfomationNotice.setInfomationId(t.getObjectId());
		infomationNoticeManager.deleteBySample(delInfomationNotice);
		if(IBSConstants.PLATFORM_NOTICE_ALL_USER.equals(t.getFaceMember())){
			InfomationNotice infomationNotice = new InfomationNotice();
			infomationNotice.setInfomationId(t.getObjectId());
			infomationNotice.setAllUser(IBSConstants.PLATFORM_NOTICE_ALL_USER);
			infomationNotice.setCompanyId(user.getCompanyId());
			infomationNoticeManager.save(infomationNotice);
		}else{
			String[] members = request.getParameterValues("userId");
			for(String memberId : members){
				InfomationNotice infomationNotice = new InfomationNotice();
				infomationNotice.setUserId(Long.parseLong(memberId));
				infomationNotice.setInfomationId(t.getObjectId());
				infomationNotice.setCompanyId(user.getCompanyId());
				infomationNoticeManager.save(infomationNotice);
			}
		}
		return "redirect:listNotice";
	}
	
	@RequestMapping("addActivity")
	public String addActivity(HttpServletRequest request,HttpServletResponse response){
		request.setAttribute("type", IBSConstants.HR_NOTICE);
		return getFileBasePath()+"addActivity";
	}
	
	@RequestMapping("saveActivity")
	public String saveActivity(HttpServletRequest request,HttpServletResponse response,Infomation t){
		User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		t.setType(IBSConstants.HR_NOTICE);
		t.setCreatedOn(new Date());
		t.setCreatedBy(user.getObjectId());
		t.setCompanyId(user.getCompanyId());
		if(IBSConstants.USER_TYPE_COMPANY_ADMIN != user.getType()){
			t.setOrganizationId(user.getOrganizationId());
		}
		t.setTitle(t.getTitle().trim());
		t.setContent(UeditorUtils.convertSpace(t.getContent()));
		if(IBSConstants.PLATFORM_NOTICE_ALL_USER.equals(t.getFaceMember())){
			t.setFacePeople(IBSConstants.STATUS_YES);
		}else{
			t.setFacePeople(IBSConstants.STATUS_NO);
		}
		t=infomationManager.save(t);
		InfomationNotice delInfomationNotice = new InfomationNotice();
		delInfomationNotice.setInfomationId(t.getObjectId());
		infomationNoticeManager.deleteBySample(delInfomationNotice);
		if(IBSConstants.PLATFORM_NOTICE_ALL_USER.equals(t.getFaceMember())){
			InfomationNotice infomationNotice = new InfomationNotice();
			infomationNotice.setInfomationId(t.getObjectId());
			infomationNotice.setAllUser(IBSConstants.PLATFORM_NOTICE_ALL_USER);
			infomationNotice.setCompanyId(user.getCompanyId());
			infomationNoticeManager.save(infomationNotice);
		}else{
			String[] members = request.getParameterValues("userId");
			for(String memberId : members){
				InfomationNotice infomationNotice = new InfomationNotice();
				infomationNotice.setUserId(Long.parseLong(memberId));
				infomationNotice.setInfomationId(t.getObjectId());
				infomationNotice.setCompanyId(user.getCompanyId());
				infomationNoticeManager.save(infomationNotice);
			}
		}
		return "redirect:listNotice";
	}
	
	@RequestMapping("edit/{objectId}/{type}")
	public String saveActivity(HttpServletRequest request,HttpServletResponse response,@PathVariable Long objectId,@PathVariable Integer type){
		Infomation infomation = infomationManager.getByObjectId(objectId);
		infomation.setTitle(infomation.getTitle().trim());
		request.setAttribute("infomation", infomation);
		InfomationNotice sample = new InfomationNotice();
		sample.setInfomationId(objectId);
		List<InfomationNotice> notices = infomationNoticeManager.getBySample(sample);
		request.setAttribute("notices", notices);
		if(IBSConstants.HR_ACTIVITY == type){
			return getFileBasePath()+"addActivity";
		}else if(IBSConstants.HR_NOTICE == type){
			return getFileBasePath()+"addNotice";
		}else{
			return "redirect:detail/"+objectId;
		}
	}
	
	/**
	  * repeal
	  *
	  * @Title: repeal
	  * @Description: TODO (撤销)
	  * @param @param request
	  * @param @param response
	  * @param @param noticeId
	  * @param @return    设定文件
	  * @return String    返回类型
	  * @throws
	 */
	@RequestMapping("repeal/{noticeId}")
	public String repeal(HttpServletRequest request,HttpServletResponse response,@PathVariable Long noticeId){
		Infomation infomation = infomationManager.getByObjectId(noticeId);
		infomation.setStatus(IBSConstants.INFOMATION_DRAFT);
		infomationManager.save(infomation);
		return "redirect:/infomation/listNotice";
	}

	/**
	  * repeal
	  *
	  * @Title: repeal
	  * @Description: TODO (发布)
	  * @param @param request
	  * @param @param response
	  * @param @param noticeId
	  * @param @return    设定文件
	  * @return String    返回类型
	  * @throws
	 */
	@RequestMapping("releaseInfo/{noticeId}")
	public String releaseInfo(HttpServletRequest request,HttpServletResponse response,@PathVariable Long noticeId){
		Infomation infomation = infomationManager.getByObjectId(noticeId);
		infomation.setStatus(IBSConstants.INFOMATION_PUBLISH);
		infomationManager.save(infomation);
		return "redirect:/infomation/listNotice";
	}	

	@RequestMapping("detail/{noticeId}")
	public String detail(HttpServletRequest request,HttpServletResponse response,@PathVariable Long noticeId){
		Infomation infomation = infomationManager.getByObjectId(noticeId);
		User user = userManager.getUserByUserId(infomation.getCreatedBy());
		infomation.setCreateUserName(user.getUserName());
		request.setAttribute("infomation", infomation);
		
		InfomationNotice sample = new InfomationNotice();
		sample.setInfomationId(noticeId);
		List<InfomationNotice> notices = infomationNoticeManager.getBySample(sample);
		request.setAttribute("notices", notices);
		return getFileBasePath()+"detail";
	}
	
	@RequestMapping("delete/{noticeId}")
	public String delete(HttpServletRequest request,HttpServletResponse response,@PathVariable Long noticeId){
		infomationManager.delete(noticeId);
		return "redirect:/infomation/listNotice";
	}

	@RequestMapping("delBatch")
	@ResponseBody
	public String delete(HttpServletRequest request,HttpServletResponse response){
		String noticeIds = request.getParameter("noticeIds");
		noticeIds = noticeIds.substring(0, noticeIds.length()-1);
		String[] noticeid = noticeIds.split(",");
		int count = 0;
		for(int i=0;i<noticeid.length;i++){
			Long objectid = Long.parseLong(noticeid[i]);  
			Infomation infomation = infomationManager.getByObjectId(objectid);
			if(infomation!=null){
				if(infomation.getStatus()!=1){
					continue;
				}
				infomationManager.delete(objectid);
				count = count+1;
			}else{
				continue;
			}
		}
		return String.valueOf(count);
	}

	@RequestMapping("repealBatch")
	@ResponseBody
	public String repealBatch(HttpServletRequest request,HttpServletResponse response){
		String noticeIds = request.getParameter("noticeIds");
		noticeIds = noticeIds.substring(0, noticeIds.length()-1);
		String[] noticeid = noticeIds.split(",");
		int count = 0;
		for(int i=0;i<noticeid.length;i++){
			Long objectid = Long.parseLong(noticeid[i]);  
			Infomation infomation = infomationManager.getByObjectId(objectid);
			if(infomation!=null){
				if(infomation.getStatus()!=2){
					continue;
				}
				infomation.setStatus(IBSConstants.INFOMATION_DRAFT);
				infomationManager.save(infomation);
				count = count+1;
			}else{
				continue;
			}
		}
		return String.valueOf(count);
	}
	
	public String getFileBasePath() {
		return "info/";
	}
}
