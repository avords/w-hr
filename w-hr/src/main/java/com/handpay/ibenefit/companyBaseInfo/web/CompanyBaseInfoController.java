package com.handpay.ibenefit.companyBaseInfo.web;


import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.base.area.entity.Area;
import com.handpay.ibenefit.base.area.entity.AreaInfo;
import com.handpay.ibenefit.base.area.service.IAreaManager;
import com.handpay.ibenefit.base.file.service.IFileManager;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.member.entity.Company;
import com.handpay.ibenefit.member.entity.CompanyPublished;
import com.handpay.ibenefit.member.service.ICompanyManager;
import com.handpay.ibenefit.member.service.ICompanyPublishedManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.util.ImageUploadUtils;
@Controller
@RequestMapping("companyBaseInfo")
public class CompanyBaseInfoController extends BaseController{
	
	@Reference(version="1.0")
	private IUserManager userManager;
	
	@Reference(version="1.0")
	private ICompanyManager companyManager;
	
	@Reference(version="1.0")
	private ICompanyPublishedManager companyPublishedManager;
	
	@Reference(version = "1.0")
	private IAreaManager areaManager;
	
	@Reference(version="1.0")
    private IFileManager fileManager;
	
	@Reference(version="1.0")
	private IDictionaryManager dictionaryManager;
	
	/**
	 * @Title: showCompanyBaseInfo 
	 * @Description: 显示企业信息
	 * @param request
	 * @param response
	 * @return
	 * @author 王文羚
	 */
	@RequestMapping(value="showCompanyBaseInfo")
	public String showCompanyBaseInfo(HttpServletRequest request,HttpServletResponse response){
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		try {
			List<Dictionary> dicCompanys = dictionaryManager.getDictionariesByDictionaryId(1303);
			List<Dictionary> dicStaffs = dictionaryManager.getDictionariesByDictionaryId(1301);
			Company company = companyManager.getByObjectId(user.getCompanyId());
			if(company!=null&&company.getVerifyStatus()==IBSConstants.VERIFY_STATUS_ING){
				if(company!=null&&company.getCompanyType()!=null){
					Dictionary dictionary = dictionaryManager.getDictionaryByDictionaryIdAndValue(1303, company.getCompanyType());
					request.setAttribute("companyTypeName", dictionary.getName());
				}
				if(company!=null&&company.getType()!=null){
					Dictionary dictionary = dictionaryManager.getDictionaryByDictionaryIdAndValue(1301, company.getType());
					request.setAttribute("typeName", dictionary.getName());
				}
				if(company.getAreaId()!=null){
					AreaInfo areaInfo = areaManager.getAreaInfoByDistObjectId(Long.valueOf(company.getAreaId()));
					if(areaInfo!=null){
						List<Area> dists = areaManager.getChildren(Long.valueOf(areaInfo.getCityCode()));
						List<Area> cities = areaManager.getChildren(Long.valueOf(areaInfo.getProvinceCode()));
						request.setAttribute("dists", dists);
						request.setAttribute("cities", cities);
						request.setAttribute("areaInfo", areaInfo);
					}
					request.setAttribute("areaInfo", areaInfo);
				}
				request.setAttribute("entity", company);
			}else{
				CompanyPublished companyPublish = companyPublishedManager.getByObjectId(user.getCompanyId());
				if(companyPublish!=null&&companyPublish.getCompanyType()!=null){
					Dictionary dictionary = dictionaryManager.getDictionaryByDictionaryIdAndValue(1303, companyPublish.getCompanyType());
					request.setAttribute("companyTypeName", dictionary.getName());
				}
				if(companyPublish!=null&&companyPublish.getType()!=null){
					Dictionary dictionary = dictionaryManager.getDictionaryByDictionaryIdAndValue(1301, company.getType());
					request.setAttribute("typeName", dictionary.getName());
				}
				if(companyPublish.getAreaId()!=null){
					AreaInfo areaInfo = areaManager.getAreaInfoByDistObjectId(Long.valueOf(companyPublish.getAreaId()));
					if(areaInfo!=null){
						List<Area> dists = areaManager.getChildren(Long.valueOf(areaInfo.getCityCode()));
						List<Area> cities = areaManager.getChildren(Long.valueOf(areaInfo.getProvinceCode()));
						request.setAttribute("dists", dists);
						request.setAttribute("cities", cities);
						request.setAttribute("areaInfo", areaInfo);
					}
					request.setAttribute("areaInfo", areaInfo);
				}
				request.setAttribute("entity", companyPublish);
			}
			String attachment = company.getAttachment();
			if(attachment!=null&&!"".equals(attachment)){
				String[] attachments = attachment.split(",");
				request.setAttribute("attachments", attachments);
			}
			List<Area> provinces = areaManager.getRoot();
			request.setAttribute("provinces", provinces);
			request.setAttribute("user", user);
			request.setAttribute("dicCompanys", dicCompanys);
			request.setAttribute("dicStaffs", dicStaffs);
			return "company/companyBaseInfo";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "company/companyBaseInfo";
	}
	
	/**
	 * @Title: updateCompanyInfo 
	 * @Description: 更新企业信息
	 * @param comany
	 * @param request
	 * @param response
	 * @return
	 * @author 王文羚
	 */					   
	@RequestMapping(value="updateCompanyInfo")
	public String updateCompanyInfo(Company comany,HttpServletRequest request,HttpServletResponse response){
		try {
			User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			comany.setUpdatedBy(user.getObjectId());
			comany.setUpdatedOn(new Date());
			comany.setTelephone(comany.getPhone());
			comany.setVerifyStatus(IBSConstants.VERIFY_STATUS_ING);
			companyManager.save(comany);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:showCompanyBaseInfo";
	}
	
	/**
	 * @Title: updateLogoImage 
	 * @Description: 更新logo
	 * @param request
	 * @param response
	 * @param map
	 * @return
	 * @author 王文羚
	 * @throws Exception 
	 */
	@RequestMapping(value="updateLogoImage")
	public String updateLogoImage(HttpServletRequest request, HttpServletResponse response,ModelMap map) throws Exception{
		String path = null;
		boolean op = true;
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		try {
			Company company = companyManager.getByObjectId(companyId);
			path = ImageUploadUtils.saveLogoImage(request, fileManager, "logoImage");
			company.setLogoId(path);
			company.setVerifyStatus(IBSConstants.VERIFY_STATUS_ING);
			companyManager.save(company);
			map.addAttribute("path", path);
		} catch (Exception e) {
			e.printStackTrace();
			op = false ;
		}
		map.addAttribute("result", op);
		//return "jsonView";
		AjaxUtils.doAjaxResponseOfMap(response, map);
        return null;
	}
	
	@RequestMapping(value="deleteAttachment")
	public String deleteAttachment(HttpServletRequest request, HttpServletResponse response,ModelMap map){
		String attachment = request.getParameter("attachment");
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		boolean result = false;
		try {
			Company company = companyManager.getByObjectId(companyId);
			if(company.getAttachment()!=null){
				String aaa = "";
				String[] attachments = company.getAttachment().split(",");
				for(String temp: attachments){
					if(!temp.equals(attachment)){
						aaa += temp + ",";
					}
				}
				company.setAttachment(aaa);
				companyManager.save(company);
				result = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		map.addAttribute("result", result);
		return "jsonView";
	}
	
	
	
	
	/**
	 * @Title: updateAttachment 
	 * @Description: 更新证件图
	 * @param request
	 * @param response
	 * @param map
	 * @return
	 * @author 王文羚
	 * @throws Exception 
	 */
	@RequestMapping(value="updateAttachment")
	public String updateAttachment(HttpServletRequest request, HttpServletResponse response,ModelMap map) throws Exception{
		String path = null;
		boolean op = true;
		try {
			Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
			path = ImageUploadUtils.saveAttachment(request, fileManager, "attachmentFile");
			Company company = companyManager.getByObjectId(companyId);
			if(company.getAttachment()!=null&&!"".equals(company.getAttachment())){
				company.setAttachment(path+","+company.getAttachment());
				company.setVerifyStatus(IBSConstants.VERIFY_STATUS_ING);
				companyManager.save(company);
			}else{
				company.setAttachment(path);
				company.setVerifyStatus(IBSConstants.VERIFY_STATUS_ING);
				companyManager.save(company);
			}
			map.addAttribute("attachments", company.getAttachment());
			//map.addAttribute("result", true);
			map.addAttribute("path", path);
			//return "jsonView";
		} catch (Exception e) {
			e.printStackTrace();
			op = false;
		}
		map.addAttribute("result", op);
		//return "jsonView";
		AjaxUtils.doAjaxResponseOfMap(response, map);
        return null;
	}
	
	
	@RequestMapping(value="getAreaName/{objectId}")
	public String getAreaName(@PathVariable(value="objectId")Long objectId,ModelMap map){
		List<Area> areas = this.areaManager.getChildren(objectId);
		map.addAttribute("areas", areas);
		return "jsonView";
	}
	
}
