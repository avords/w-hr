package com.handpay.ibenefit.plan.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.member.entity.Staff;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.plan.entity.WelfarePlanStaff;
import com.handpay.ibenefit.plan.service.IWelfarePlanStaffManager;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;

@Controller
@RequestMapping("welfareSubPlanStaff")
public class WelfareSubPlanStaffController {
	
	@Reference(version = "1.0")
	private IWelfarePlanStaffManager welfareSubPlanStaffManager;
	
	@Reference(version = "1.0")
	private IStaffManager staffManager;
	
	@Reference(version = "1.0")
	private IUserManager userManager;
	
	@RequestMapping("get/{planId}/{userId}")
	public String getWelfareSubPlanStaff(@PathVariable Long planId, @PathVariable Long userId, ModelMap modelMap) throws Exception {
		WelfarePlanStaff entity = welfareSubPlanStaffManager.getWelfarePlanStaff(planId, userId);
		Staff staff = staffManager.getByObjectId(userId);
		User user = userManager.getByObjectId(userId);
		modelMap.addAttribute("entity", entity);
		modelMap.addAttribute("staff", staff);
		modelMap.addAttribute("user", user);
		return "jsonView";
	}
	
	@RequestMapping("update")
	public String update(ModelMap modelMap, WelfarePlanStaff welfareSubPlanStaff) throws Exception {
		welfareSubPlanStaffManager.updateByPlanIdAndUserId(welfareSubPlanStaff);
		return "jsonView";
	}
	
}
