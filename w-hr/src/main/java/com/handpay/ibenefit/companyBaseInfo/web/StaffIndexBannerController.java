package com.handpay.ibenefit.companyBaseInfo.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.base.file.service.IFileManager;
import com.handpay.ibenefit.companyBaseInfo.web.form.CompanyCategoryList;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.member.entity.CompanyCategory;
import com.handpay.ibenefit.member.service.ICompanyCategoryManager;
import com.handpay.ibenefit.member.service.ICompanyManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.util.ImageUploadUtils;

@Controller
@RequestMapping("staffIndexBanner")
public class StaffIndexBannerController {
	
	@Reference(version="1.0")
	private IUserManager userManager;
	
	@Reference(version="1.0")
	private ICompanyManager companyManager;
	
	@Reference(version="1.0")
	private ICompanyCategoryManager companyCategoryManager;
	
	@Reference(version="1.0")
    private IFileManager fileManager;

	@RequestMapping(value="showStaffIndexBanner")
	public String showStaffIndexBanner(HttpServletRequest request,ModelMap map){
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		CompanyCategory category = new CompanyCategory();
		category.setCompanyId(companyId);
		List<CompanyCategory> companyCategorys = companyCategoryManager.getBySample(category);
		map.addAttribute("companyCategorys", companyCategorys);
		return "company/staffIndexBanner";
	}
	
	@RequestMapping(value="saveStaffIndexBanner")
	public String saveStaffIndexBanner(HttpServletRequest request,CompanyCategoryList companyCategorys){
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		Long companyId = user.getCompanyId();
		List<CompanyCategory> list = companyCategorys.getCompanyCategorys();
		if(list != null && list.size() > 0){
			for(CompanyCategory c : list){
				c.setCompanyId(companyId);
			}
			this.companyCategoryManager.saveAll(list);
			return "redirect:showStaffIndexBanner";
		}
		this.companyCategoryManager.deletetAllByCompanyId(companyId);
		return "redirect:showStaffIndexBanner";
	}
	
	@RequestMapping(value="updateBanner")
	public String updateBanner(HttpServletRequest request, HttpServletResponse response,ModelMap map) throws Exception {
		String path = null;
		try {
			Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
			path = ImageUploadUtils.saveAttachment(request, fileManager, "bannerImage");
			CompanyCategory category = new CompanyCategory();
			category.setBannerUrl(path);
			category.setCompanyId(companyId);
			category = companyCategoryManager.save(category);
			map.addAttribute("objectId", category.getObjectId());
			map.addAttribute("result", true);
			map.addAttribute("path", path);
			AjaxUtils.doAjaxResponseOfMap(response, map);
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		map.addAttribute("result", false);
		AjaxUtils.doAjaxResponseOfMap(response, map);
		return null;
	}
	
	@RequestMapping(value="deleteBanner/{objectId}")
	public String deleteBanner(@PathVariable Long objectId,ModelMap map){
		companyCategoryManager.delete(objectId);
		map.addAttribute("result", true);
		return "jsonView";
	}
}
