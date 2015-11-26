/**
 * @Title: DistributeScoreController.java
 * @Package com.handpay.ibenefit.system.web
 * @Description: TODO
 * Copyright: Copyright (c) 2011 
 * 
 * @author Mac.Yoon
 * @date 2015-7-2 上午11:54:29
 * @version V1.0
 */

package com.handpay.ibenefit.system.web;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.gson.Gson;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.HRUserDistributeScore;
import com.handpay.ibenefit.security.entity.Role;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.entity.UserRoleView;
import com.handpay.ibenefit.security.service.IPointOperateManager;
import com.handpay.ibenefit.security.service.IRoleManager;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.system.service.IDistributeScoreManager;

/**
 * @ClassName: DistributeScoreController
 * @Description: TODO
 * @author Mac.Yoon
 * @date 2015-7-2 上午11:54:29
 *
 */
@Controller
@RequestMapping("distributeScore")
public class DistributeScoreController extends BaseController{
	

	@Reference(version="1.0")
	private IDistributeScoreManager distributeScoreManager;
	@Reference(version="1.0")
	private IUserManager userManager;
	@Reference(version="1.0")
	private IRoleManager roleManager;
	@Reference(version="1.0")
	private IPointOperateManager pointOperateManager;
	
	@RequestMapping("list")
	public String list(HttpServletRequest request, HttpServletResponse response) {
		PageSearch page = PageUtils.preparePage(request, UserRoleView.class, true);
		User sessionUser = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		page.getFilters().add( new PropertyFilter(HRUserDistributeScore.class.getName(), "EQL_operationUserId", String.valueOf(sessionUser.getObjectId())));
		String cp = request.getParameter("c_p");
		Integer currPage = 1;
		if(cp!=null&&!("").equals(cp)){
			currPage = Integer.parseInt(cp);
		}
		page.setSortOrder("desc");
		page.setSortProperty("objectId");
		page.setCurrentPage(currPage);
		PageSearch result = distributeScoreManager.findHRUserScore(page);
		page.setTotalCount(result.getTotalCount());
		page.setList(result.getList());
		List<HRUserDistributeScore> items = page.getList();
		request.setAttribute("items", items);
		List<Role> roles = roleManager.getRolesByCompanyId(companyId);
		Map<Object,Map<Object,String>> roleUsers = new HashMap<Object, Map<Object,String>>();
		for(Role role : roles){
			HashMap<Object,String> map = new HashMap<Object, String>();
			List<User> users = userManager.getUsersByRoleId(role.getObjectId());
			
			if(users!=null&&users.size()==0){
				continue;
			}
			for(User user : users){
				if(IBSConstants.USER_STATUS_ENABLED == user.getStatus()&&!user.getObjectId().equals(sessionUser.getObjectId())){
					map.put(user.getObjectId(), user.getUserName());
				}
			}
			roleUsers.put(role.getObjectId(), map);
		}
		HashMap<Object,String> map = new HashMap<Object, String>();
		User isAgencyUser = userManager.getUserByCompanyAdmin(companyId,IBSConstants.STATUS_YES);
		if(isAgencyUser!=null&&!isAgencyUser.getObjectId().equals(sessionUser.getObjectId())){
			map.put(isAgencyUser.getObjectId(), isAgencyUser.getUserName());
			//增加代理管理员
			roleUsers.put(0, map);
			Role role = new Role();
			role.setObjectId(0L);
			role.setName("代理管理员");
			roles.add(role);
		}
		Double accountBalance =  userManager.getUserBalance(sessionUser.getObjectId());
		if(accountBalance==null){
			request.setAttribute("accountBalance", 0.00);
		}else{
			DecimalFormat decimalFormat=new DecimalFormat("#.##");   
			request.setAttribute("accountBalance", decimalFormat.format(accountBalance));
		}
		Gson gson = new Gson();
		request.setAttribute("roleUsers", gson.toJson(roleUsers));
		request.setAttribute("roles", roles);
		request.setAttribute("pageData", page);
		return "system/distributeScore";
	}
	
	@RequestMapping(value = "/queryRoleUser/{roleId}", method = RequestMethod.POST)
	public String queryRoleUser(HttpServletRequest request, ModelMap modelMap,@PathVariable Long roleId) throws Exception {
		List<User> users = userManager.getUsersByRoleId(roleId);
		modelMap.put("users", users);
		return "jsonView";
	}
	
	@RequestMapping("saveDistributeScore")
	public String saveDistributeScore(HttpServletRequest request,
			HttpServletResponse response,HRUserDistributeScore t) {
		User currentUser = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		String roleId = request.getParameter("roleId");
		String userId = request.getParameter("userId");
		if(roleId!=null&&!"".equals(roleId)&&userId!=null&&!"".equals(userId)){
			boolean result = distributeScoreManager.saveDistributeScore(currentUser, t, roleId, userId);
			if(result){
				request.setAttribute("message", "发放成功");
			}else{
				request.setAttribute("message", "发放失败");
			}
		}
		return "redirect:list";
	}
}
