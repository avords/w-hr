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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.gson.Gson;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.Menu;
import com.handpay.ibenefit.security.entity.Role;
import com.handpay.ibenefit.security.entity.RoleMenu;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.entity.UserMenus;
import com.handpay.ibenefit.security.entity.UserRole;
import com.handpay.ibenefit.security.entity.UserRoleForm;
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
@RequestMapping("role")
public class RoleController extends BaseController {

	@Reference(version = "1.0")
	private IUserManager userManager;
	@Reference(version = "1.0")
	private IRoleManager roleManager;
	@Reference(version = "1.0")
	private IMenuManager menuManager;

	@Reference(version = "1.0")
	private IRoleMenuManager roleMenuManager;
	@Reference(version = "1.0")
	private IUserRoleManager userRoleManager;

	@RequestMapping("listRole")
	public String listRole(HttpServletRequest request,
			HttpServletResponse response) {
		return toList(request);
	}

	private String toList(HttpServletRequest request) {
		PageSearch page = PageUtils.preparePage(request, UserRoleView.class,
				true);
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		page.getFilters().add(new PropertyFilter(UserRoleView.class.getName(),"EQL_companyId", String.valueOf(companyId)));
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
		return "system/role";
	}

	protected void handleFind(PageSearch page) {
		PageSearch result = roleManager.findHRRoleList(page);
		page.setTotalCount(result.getTotalCount());
		page.setList(result.getList());
	}

	@RequestMapping("addRole")
	public String addRole(HttpServletRequest request,
			HttpServletResponse response) {
		Long companyId = (Long) request.getSession().getAttribute(
				SecurityConstants.USER_COMPANY_ID);
		List<User> hrUsers = userManager.getHrUsersByCompanyId(companyId);
		Map<String,String> hrs = new HashMap<String,String>();
		for(User user: hrUsers){
			hrs.put(user.getObjectId().toString(), user.getLoginName());
		}
		Gson gson = new Gson();
		request.setAttribute("hrUsers",gson.toJson(hrs));
		setRoleMenu(request);
		return "system/addRole";
	}

	private void setRoleMenu(HttpServletRequest request) {
		Integer platform = (Integer) request.getSession().getAttribute(SecurityConstants.USER_PLATFORM);
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		List<Menu> allPermissionMenus = (List<Menu>) request.getSession()
				.getAttribute(SecurityConstants.MENU_PERMISSION);
		List<UserMenus> allMenus = new ArrayList<UserMenus>();
		for (Menu menu : allPermissionMenus) {
			if (menu.getParentId() == Long.parseLong(String.valueOf(platform))) {
				if(IBSConstants.MENU_SHOW_WELFARE.equals(menu.getObjectId())&&!user.isShowWelfare()){
					continue;
				}else if(IBSConstants.MENU_SHOW_EXCITATION.equals(menu.getObjectId())&&!user.isShowExcitation()){
					continue;
				}else if(IBSConstants.COMPANY_FUNCTION_SHOW_REPORT==menu.getObjectId()&&!user.isShowReport()){
					continue;
				}
				UserMenus userMenu = new UserMenus();
				userMenu.setFolderName(menu.getName());
				userMenu.setFolderId(menu.getObjectId());
				List<Menu> subMenus = new ArrayList<Menu>();
				for (Menu subMenu : allPermissionMenus) {
					if (subMenu.getParentId().equals(menu.getObjectId())
							&&!IBSConstants.MENU_ADMIN_ROLE_PERMISSION.equals(subMenu.getObjectId())
							&&!IBSConstants.MENU_ADMIN_USER_PERMISSION.equals(subMenu.getObjectId())
							&&!IBSConstants.MENU_ADMIN_PASSWORD_PERMISSION.equals(subMenu.getObjectId())) {
						subMenus.add(subMenu);
					}
				}
				if (subMenus.size() > 0) {
					userMenu.setMenus(subMenus);
					allMenus.add(userMenu);
				}
			}
		}
		request.setAttribute("allMenus", allMenus);
	}

	@RequestMapping("saveRole")
	public String saveRole(HttpServletRequest request,
			HttpServletResponse response, UserRoleForm t) {
		try {
			Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
			Integer platform = (Integer) request.getSession().getAttribute(SecurityConstants.USER_PLATFORM);
			Role role = new Role();
			role.setObjectId(t.getObjectId());
			role.setCompanyId(companyId);
			role.setPlatform(platform);
			role.setName(t.getRoleName());
			role.setRoleCode(t.getRoleCode());
			role = roleManager.save(role);
			if(t.getObjectId()!=null){
				userRoleManager.deleteUserRoleByRoleId(role.getObjectId());
				roleMenuManager.deleteRoleMenuByRoleId(role.getObjectId());
			}
			if(t.getUserIds()!=null){
				// 保存用户对应的角色 1对1
				for (String userId : t.getUserIds().split(",")) {
					UserRole userRole = new UserRole();
					userRole.setRoleId(role.getObjectId());
					userRole.setUserId(Long.parseLong(userId));
					userRoleManager.save(userRole);
				}
			}
			RoleMenu roleMenuBack = new RoleMenu();
			roleMenuBack.setMenuId(IBSConstants.MENU_ADMIN_PASSWORD_PERMISSION);
			roleMenuBack.setRoleId(role.getObjectId());
			roleMenuManager.save(roleMenuBack);
			if(t.getMenuIds()!=null){
				// 保存角色菜单
				for (String menuId : t.getMenuIds().split(",")) {
					if(IBSConstants.MENU_SHOW_WELFARE == Long.parseLong(menuId)
							||IBSConstants.MENU_SHOW_EXCITATION == Long.parseLong(menuId)){
						List<Menu> menus = menuManager.getMenusByParentId(Long.parseLong(menuId));
						for(Menu menu : menus){
							RoleMenu roleMenu = new RoleMenu();
							roleMenu.setMenuId(menu.getObjectId());
							roleMenu.setRoleId(role.getObjectId());
							roleMenuManager.save(roleMenu);
						}
					}
					RoleMenu roleMenu = new RoleMenu();
					roleMenu.setMenuId(Long.parseLong(menuId));
					roleMenu.setRoleId(role.getObjectId());
					roleMenuManager.save(roleMenu);
				}
			}
		} catch (Exception e) {
			request.setAttribute("message", "系统异常");
			e.printStackTrace();
		}
		return toList(request);
	}

	@RequestMapping(value = "/edit/{roleId}")
	public String edit(HttpServletRequest request,
			HttpServletResponse response, @PathVariable Long roleId)
			throws Exception {
		Long companyId = (Long) request.getSession().getAttribute(
				SecurityConstants.USER_COMPANY_ID);
		List<User> hrUsers = userManager.getHrUsersByCompanyId(companyId);
		Map<String,String> hrs = new HashMap<String,String>();
		for(User user: hrUsers){
			if(user.getStatus()==IBSConstants.USER_STATUS_ENABLED){
				hrs.put(user.getObjectId().toString(), user.getLoginName());
			}
		}
		Gson gson = new Gson();
		request.setAttribute("hrUsers",gson.toJson(hrs));
		setRoleMenu(request);
		Role role = roleManager.getByObjectId(roleId);
		List<Menu> haveMenus = menuManager.getMenusByRoleId(roleId);
		List<User> haveUsers = userManager.getUsersByRoleId(roleId);
		request.setAttribute("haveMenus", haveMenus);
		request.setAttribute("haveUsers", haveUsers);
		request.setAttribute("entity", role);
		return "system/addRole";
	}

	@RequestMapping(value = "/delete/{roleId}")
	public String delete(HttpServletRequest request,
			HttpServletResponse response, @PathVariable Long roleId)
			throws Exception {
		Integer count = roleManager.checkRoleUsers(roleId);
		if (count > 0) {
			request.setAttribute("message", "该角色下有有效用户，不允许删除");
		} else {
			userRoleManager.deleteUserRoleByRoleId(roleId);
			roleManager.delete(roleId);
			request.setAttribute("message", "删除成功");
		}
		return toList(request);
	}
	
	@RequestMapping("isUnique")
	@ResponseBody
	public boolean isUnique(HttpServletRequest request,Role t){
		if(t!=null){
			Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
			Integer platform = (Integer) request.getSession().getAttribute(SecurityConstants.USER_PLATFORM);
			t.setCompanyId(companyId);
			t.setPlatform(platform);
			return roleManager.isUnique(t);
		}else{
			return false;
		}
	}
}
