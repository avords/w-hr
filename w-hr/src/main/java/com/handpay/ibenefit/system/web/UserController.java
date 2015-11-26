/**
 * @Title: RoleController.java
 * @Package com.handpay.ibenefit.system.web
 * @Description: TODO
 * Copyright: Copyright (c) 2011 
 * 
 * @author Mac.Yoon
 * @date 2015-6-24 下午2:57:28
 * @version V1.0
 */

package com.handpay.ibenefit.system.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.entity.CompanyPublished;
import com.handpay.ibenefit.member.service.ICompanyDepartmentManager;
import com.handpay.ibenefit.member.service.ICompanyPublishedManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.Role;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.entity.UserRole;
import com.handpay.ibenefit.security.entity.UserRoleView;
import com.handpay.ibenefit.security.service.IMenuManager;
import com.handpay.ibenefit.security.service.IRoleManager;
import com.handpay.ibenefit.security.service.IRoleMenuManager;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.security.service.IUserRoleManager;

/**
 * @ClassName: RoleController
 * @Description: TODO
 * @author Mac.Yoon
 * @date 2015-6-24 下午2:57:28
 * 
 */
@Controller
@RequestMapping("user")
public class UserController extends BaseController {
	
	@Reference(version="1.0")
	private IUserManager userManager;
	@Reference(version="1.0")
	private IRoleManager roleManager;
	@Reference(version="1.0")
	private IMenuManager menuManager;
	
	@Reference(version="1.0")
	private IRoleMenuManager roleMenuManager;
	@Reference(version="1.0")
	private IUserRoleManager userRoleManager;
	@Reference(version="1.0")
	private ICompanyDepartmentManager companyDepartmentManager;
	@Reference(version="1.0")
	private ICompanyPublishedManager companyPublishedManager;
	
	
	@RequestMapping("listUser")
	public String listUser(HttpServletRequest request, HttpServletResponse response) {
		return toList(request);
	}
	
	private String toList(HttpServletRequest request) {
		PageSearch page = PageUtils.preparePage(request, UserRoleView.class, true);
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		page.getFilters().add( new PropertyFilter(UserRoleView.class.getName(), "EQL_companyId", String.valueOf(companyId)));
		String cp = request.getParameter("c_p");
		Integer currPage = 1;
		if(cp!=null&&!("").equals(cp)){
			currPage = Integer.parseInt(cp);
		}
		page.setSortOrder("asc");
		page.setSortProperty("createdOn");
		page.setCurrentPage(currPage);
		handleFind(page);
		List<UserRoleView> items = page.getList();
		request.setAttribute("items", items);
		request.setAttribute("pageData", page);
		return "system/user";
	}

	protected void handleFind(PageSearch page) {
		PageSearch result = userManager.findHRUserlist(page);
		page.setTotalCount(result.getTotalCount());
		page.setList(result.getList());
	}

	@RequestMapping("addUser")
	public String addUser(HttpServletRequest request,
			HttpServletResponse response) {
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		List<Role> roles = roleManager.getRolesByCompanyId(companyId);
		request.setAttribute("roles", roles);
		CompanyDepartment sample = new CompanyDepartment();
		sample.setParentId(-1L);
		sample.setCompanyId(companyId);
		List<CompanyDepartment> companyDepartments = companyDepartmentManager.getBySample(sample);
		request.setAttribute("companyDepartments", companyDepartments);
		return "system/addUser";
	}
	
	
	@RequestMapping("saveUser")
	public String saveUser(HttpServletRequest request,
			HttpServletResponse response,User t) {
		try {
			Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
			Integer platform = (Integer) request.getSession().getAttribute(SecurityConstants.USER_PLATFORM);
			t.setCompanyId(companyId);
			t.setPlatform(platform);
			t.setType(IBSConstants.USER_TYPE_COMPANY_HR);
			t.setUserResources(IBSConstants.COMPANY_HR_CREATED);
			CompanyPublished company = companyPublishedManager.getByObjectId(companyId);
			t.setCompanyName(company.getCompanyName());
			t = userManager.save(t);
			userRoleManager.deleteUserRoleByUserId(t.getObjectId());
			if(t.getRoleId()!=null){
				UserRole entity = new UserRole();
				entity.setRoleId(t.getRoleId());
				entity.setUserId(t.getObjectId());
				userRoleManager.save(entity);
			}
			request.setAttribute("message", "提交成功");
		} catch (Exception e) {
			request.setAttribute("message", "提交失败");
			e.printStackTrace();
		}
		return toList(request);
	}
	
	@RequestMapping(value = "/delete/{userId}")
	public String delete(HttpServletRequest request,
			HttpServletResponse response,@PathVariable Long userId) throws Exception {
		User user = userManager.getByObjectId(userId);
		if(user!=null&&user.getLastLoginTime()!=null){
			request.setAttribute("message", "登录过的用户不能删除");
		}else{
			userManager.updateStatus(userId,IBSConstants.USER_STATUS_DELETED);
			request.setAttribute("message", "删除成功");
		}
		return toList(request);
	}
	
	@RequestMapping(value = "/edit/{userId}")
	public String edit(HttpServletRequest request,
			HttpServletResponse response,@PathVariable Long userId) throws Exception {
		User user = userManager.getByObjectId(userId);
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		List<Role> userRoles = roleManager.getRolesByUserId(userId);
		if(userRoles.size()!=0){
			request.setAttribute("role", userRoles.get(0));
		}
		CompanyDepartment companyDepartment = companyDepartmentManager.getByObjectId(user.getOrganizationId());
		request.setAttribute("companyDepartment", companyDepartment);
		CompanyDepartment sample = new CompanyDepartment();
		sample.setParentId(-1L);
		sample.setCompanyId(companyId);
		List<CompanyDepartment> companyDepartments = companyDepartmentManager.getBySample(sample);
		request.setAttribute("companyDepartments", companyDepartments);
		List<Role> roles = roleManager.getRolesByCompanyId(companyId);
		request.setAttribute("roles", roles);
		request.setAttribute("entity", user);
		return "system/addUser";
	}
	
	@RequestMapping(value = "/lockUser/{userId}")
	public String unlock(HttpServletRequest request,
			HttpServletResponse response,@PathVariable Long userId) throws Exception {
		userManager.updateStatus(userId,IBSConstants.USER_STATUS_LOCKED);
		return toList(request);
	}
	
	@RequestMapping(value = "/unLockUser/{userId}")
	public String unLockUser(HttpServletRequest request,
			HttpServletResponse response,@PathVariable Long userId) throws Exception {
		userManager.updateStatus(userId,IBSConstants.USER_STATUS_ENABLED);
		return toList(request);
	}
	
	@RequestMapping("isUnique")
	@ResponseBody
	public boolean isUnique(User t){
		if(t!=null){
			return userManager.isUnique(t);
		}else{
			return false;
		}
	}
	
	@RequestMapping(value = "checkUserMobilePhone/{mobile}", method = RequestMethod.POST)
	public String checkUserMobilePhone(HttpServletRequest request, ModelMap modelMap,@PathVariable String mobile)
		throws Exception {
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		if (mobile!=null&&!"".equals(mobile)) {
			User sample = new User();
			sample.setMobilePhone(mobile);
			sample.setType(IBSConstants.USER_TYPE_COMPANY_ADMIN);
			sample.setStatus(IBSConstants.USER_STATUS_ENABLED);
			List<User> users = userManager.getBySample(sample);
			for(User usertmp:users){
				if(usertmp.getObjectId().equals(user.getObjectId())){
					users.remove(usertmp);
					break;
				}
			}
			if(users!=null&&users.size()>0){
				modelMap.put("message", "该手机号已绑定其他账户，如继续操作，会自动解绑");
				modelMap.put("result", false);
			}else{
				User sampleHr = new User();
				sampleHr.setMobilePhone(mobile);
				sampleHr.setType(IBSConstants.USER_TYPE_COMPANY_HR);
				sampleHr.setStatus(IBSConstants.USER_STATUS_ENABLED);
				List<User> hrUsers = userManager.getBySample(sampleHr);
				for(User usertmp:hrUsers){
					if(usertmp.getObjectId().equals(user.getObjectId())){
						hrUsers.remove(usertmp);
						break;
					}
				}
				if(hrUsers!=null&&hrUsers.size()>0){
					modelMap.put("message", "该手机号已绑定其他账户，如继续操作，会自动解绑");
					modelMap.put("result", false);
				}else{
					modelMap.put("result", true);
				}
			}
		} else {
			modelMap.put("message", "手机号不正确");
			modelMap.put("result", false);
		}
		return "jsonView";
	}
	
	@RequestMapping(value = "checkUserEmail/{email}", method = RequestMethod.POST)
	public String checkUserEmail(HttpServletRequest request, ModelMap modelMap,@PathVariable String email)
		throws Exception {
		email = request.getParameter("email");
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		if (email!=null&&!"".equals(email)) {
			User sample = new User();
			sample.setEmail(email);
			sample.setType(IBSConstants.USER_TYPE_COMPANY_ADMIN);
			sample.setStatus(IBSConstants.USER_STATUS_ENABLED);
			List<User> users = userManager.getBySample(sample);
			for(User usertmp:users){
				if(usertmp.getObjectId().equals(user.getObjectId())){
					users.remove(usertmp);
					break;
				}
			}
			if(users!=null&&users.size()>0){
				modelMap.put("message", "该邮箱已绑定其他账户，如继续操作，会自动解绑");
				modelMap.put("result", false);
			}else{
				User sampleHr = new User();
				sampleHr.setEmail(email);
				sampleHr.setType(IBSConstants.USER_TYPE_COMPANY_HR);
				sampleHr.setStatus(IBSConstants.USER_STATUS_ENABLED);
				List<User> hrUsers = userManager.getBySample(sampleHr);
				for(User usertmp:hrUsers){
					if(usertmp.getObjectId().equals(user.getObjectId())){
						hrUsers.remove(usertmp);
						break;
					}
				}
				if(hrUsers!=null&&hrUsers.size()>0){
					modelMap.put("message", "该邮箱已绑定其他账户，如继续操作，会自动解绑");
					modelMap.put("result", false);
				}else{
					modelMap.put("result", true);
				}
			}
		} else {
			modelMap.put("message", "邮箱不正确");
			modelMap.put("result", false);
		}
		return "jsonView";
	}
}
