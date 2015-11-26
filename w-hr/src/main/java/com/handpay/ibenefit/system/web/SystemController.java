/**
 * @Title: SystemController.java
 * @Package com.handpay.ibenefit.system.web
 * @Description: TODO
 * Copyright: Copyright (c) 2011 
 * 
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:57:14
 * @version V1.0
 */

package com.handpay.ibenefit.system.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.base.area.entity.Area;
import com.handpay.ibenefit.base.area.service.IAreaManager;
import com.handpay.ibenefit.common.service.ISendEmailService;
import com.handpay.ibenefit.common.vo.IBSEmail;
import com.handpay.ibenefit.common.vo.IBSReceiver;
import com.handpay.ibenefit.framework.config.GlobalConfig;
import com.handpay.ibenefit.framework.entity.ForeverEntity;
import com.handpay.ibenefit.framework.util.AppUtils;
import com.handpay.ibenefit.framework.util.DateUtils;
import com.handpay.ibenefit.framework.util.FreemarkerUtils;
import com.handpay.ibenefit.framework.util.UuidUtil;
import com.handpay.ibenefit.home.entity.ValidateCode;
import com.handpay.ibenefit.home.service.IValidateCodeManager;
import com.handpay.ibenefit.member.entity.CompanyPublished;
import com.handpay.ibenefit.member.service.ICompanyPublishedManager;
import com.handpay.ibenefit.order.entity.GoodsReceiptAddr;
import com.handpay.ibenefit.order.service.IGoodsReceiptAddrManager;
import com.handpay.ibenefit.other.entity.MessageTemplate;
import com.handpay.ibenefit.other.service.IMessageTemplateManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.SecurityUtils;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.system.entity.UpdateMobileForm;
import com.handpay.ibenefit.system.entity.UpdatePasswordForm;
import com.handpay.ibenefit.system.entity.UpdatePayPasswordForm;

/**
 * @ClassName: SystemController
 * @Description: TODO
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:57:14
 * 
 */
@Controller
@RequestMapping("system")
public class SystemController {

	@Reference(version = "1.0")
	private IUserManager userManager;

	@Reference(version = "1.0")
	private IValidateCodeManager validateCodeManager;

	@Reference(version = "1.0")
	private IGoodsReceiptAddrManager goodsReceiptAddrManager; // 收货地址Manager

	@Reference(version = "1.0")
	private IAreaManager areaManager; // 省份区域manager

	@Reference(version = "1.0", check = false,sent=false)
	private ISendEmailService sendEmailService;

	@Reference(version = "1.0")
	private IMessageTemplateManager messageTemplateManager;

	@Reference(version = "1.0")
	private ICompanyPublishedManager companyPublishedManager;

	private static final String JSON_VIEW = "jsonView";

	@RequestMapping("index")
	public String index(HttpServletRequest request, HttpServletResponse response) {
		return "system/index";
	}

	@RequestMapping("employee")
	public String employee(HttpServletRequest request, HttpServletResponse response) {
		return "system/employee";
	}

	@RequestMapping("project")
	public String project(HttpServletRequest request, HttpServletResponse response) {
		return "system/project";
	}

	@RequestMapping("company")
	public String company(HttpServletRequest request, HttpServletResponse response) {
		return "system/company";
	}

	@RequestMapping("permission")
	public String permission(HttpServletRequest request, HttpServletResponse response) {
		return "system/permission";
	}

	@RequestMapping("account")
	public String account(HttpServletRequest request, HttpServletResponse response) {
		Long userId = (Long) request.getSession().getAttribute(SecurityConstants.USER_ID);
		User user = userManager.getByObjectId(userId);
		request.setAttribute("email", user.getEmail());
		String mobile = user.getMobilePhone();
		if (mobile != null && !"".equals(mobile)) {
			mobile = mobile.replace(mobile.substring(3, 7), "****");
			request.setAttribute("mobile", mobile);
		}
		return "system/account";
	}

	@RequestMapping("payPassword")
	public String payPassword(HttpServletRequest request, ModelMap modelMap, UpdatePayPasswordForm t) {
		Long userId = (Long) request.getSession().getAttribute(SecurityConstants.USER_ID);
		User user = userManager.getByObjectId(userId);
		String mobile = user.getMobilePhone();
		if (mobile != null && !"".equals(mobile)) {
			mobile = mobile.replace(mobile.substring(3, 7), "****");
			request.setAttribute("mobile", mobile);
		}
		return "system/updatePayPassword";
	}

	@RequestMapping("password")
	public String password(HttpServletRequest request, ModelMap modelMap, UpdatePayPasswordForm t) {
		return "system/updatePassword";
	}

	@RequestMapping("address")
	public String address(HttpServletRequest request, ModelMap modelMap, UpdatePayPasswordForm t) {
		List<Area> provinces = areaManager.getRoot();
		request.setAttribute("provinces", provinces);
		Long userId = Long.parseLong((request.getSession().getAttribute(SecurityConstants.USER_ID).toString()));
		List<GoodsReceiptAddr> addrs = new ArrayList<GoodsReceiptAddr>();
		if (userId != null) {
			addrs = goodsReceiptAddrManager.getAddrByUserId(userId);
		}
		request.setAttribute("addrs", addrs);
		return "system/address";
	}

	/**
	 * updatePassword
	 * 
	 * @Title: updatePassword
	 * @Description: TODO (首次登录修改密码)
	 * @param @param request
	 * @param @param response
	 * @param @param t
	 * @param @return 设定文件
	 * @return String 返回类型
	 * @throws
	 */
	@RequestMapping("updatePassword")
	public String updatePassword(HttpServletRequest request, HttpServletResponse response, UpdatePasswordForm t) {
		User sessionUser = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		if (t == null || StringUtils.isBlank(t.getNewPassword())) {
			request.setAttribute("message", "请确认密码输入无误");
			return "system/updatePassword";
		} else if (t.getNewPassword().equals(t.getOldPassword())) {
			request.setAttribute("message", "新密码与当前密码需不一致");
			return "system/updatePassword";
		} else if (!sessionUser.getPassword().equals(SecurityUtils.generatePassword(t.getOldPassword()))) {
			request.setAttribute("message", "当前密码不正确");
			return "system/updatePassword";
		} else if (!t.getNewPassword().equals(t.getConfirmPassword())) {
			request.setAttribute("message", "确认密码不一致");
			return "system/updatePassword";
		}
		User user = new User();
		user.setPassword(SecurityUtils.generatePassword(t.getNewPassword()));
		user.setObjectId(sessionUser.getObjectId());
		userManager.updateUserByObjectId(user);
		sessionUser.setPassword(SecurityUtils.generatePassword(t.getNewPassword()));
		request.setAttribute("message", "密码修改成功");
		return "system/updatePassword";
	}

	@RequestMapping("updateMobile")
	public String updateMobile(HttpServletRequest request, UpdateMobileForm t) {
		User sessionUser = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		try {
			if (t.getNewMobile() != null && !"".equals(t.getNewMobile()) && t.getCode() != null && !"".equals(t.getCode())) {
				boolean bool = validateCodeManager.validateCode(t.getCode(), IBSConstants.VALIDATE_CODE_TYPE_UPDATE_MOBILE, t.getNewMobile());
				if (bool) {
					userManager.unBindUserMobilePhone(t.getNewMobile(), IBSConstants.USER_TYPE_COMPANY_HR);
					userManager.unBindUserMobilePhone(t.getNewMobile(), IBSConstants.USER_TYPE_COMPANY_ADMIN);
					User user = new User();
					user.setMobilePhone(t.getNewMobile());
					user.setObjectId(sessionUser.getObjectId());
					userManager.updateUserByObjectId(user);
					sessionUser.setMobilePhone(t.getNewMobile());
					request.getSession().setAttribute(SecurityConstants.SESSION_USER, sessionUser);
					request.setAttribute("message", "手机号码更新成功");
				} else {
					request.setAttribute("message", "验证码错误");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			request.setAttribute("message", "系统内部异常");
		}
		return "redirect:/system/account";
	}

	@RequestMapping("updateEmail")
	public String updateEmail(HttpServletRequest request, ModelMap modelMap) {
		try {
			String email = request.getParameter("email");
			validateCodeManager.delete(email,IBSConstants.VALIDATE_CODE_TYPE_UPDATE_EMAIL);
			User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			CompanyPublished companyPublished = companyPublishedManager.getByObjectId(user.getCompanyId());
			ValidateCode validateCode = new ValidateCode();
			validateCode.setCode(UuidUtil.get32UUID());
			validateCode.setValidateTime(DateUtils.getPreviousOrNextSecondsOfDate(new Date(), 60 * 60 * 3));
			validateCode.setUserId(user.getObjectId());
			validateCode.setType(IBSConstants.VALIDATE_CODE_TYPE_UPDATE_EMAIL);
			validateCode.setMobile(email);
			validateCodeManager.save(validateCode);
			IBSReceiver ibsReceiver = new IBSReceiver();
			ibsReceiver.setEmail(email);
			MessageTemplate messageTemplate = messageTemplateManager.getTemplateByCode("EYG_005");
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("email", email);
			model.put("userName", user.getUserName());
			model.put("companyName", companyPublished.getCompanyName());
			model.put("activeUrl",
					"<a href='" + GlobalConfig.getSecureDomain() + "/system/active/" + validateCode.getCode() + "'>" + GlobalConfig.getSecureDomain()
							+ "/system/active/" + validateCode.getCode() + "</a>");
			String cotent = FreemarkerUtils.parseTemplate(messageTemplate.getMessageContent(), model);
			IBSEmail ibsEmail = new IBSEmail();
			ibsEmail.setBody(cotent);
			ibsEmail.setBodyIsHtml(true);
			sendEmailService.sendOne(ibsEmail, messageTemplate.getMessageTitle(), ibsReceiver);
			modelMap.put("message", "发送成功");
			modelMap.put("result", true);
		} catch (Exception e) {
			e.printStackTrace();
			modelMap.put("message", "系统内部异常");
		}
		return "jsonView";
	}

	@RequestMapping("active/{key}")
	public String active(HttpServletRequest request, HttpServletResponse response, @PathVariable String key) {
		try {
			ValidateCode validateCode = validateCodeManager.validateEmailCode(key, IBSConstants.VALIDATE_CODE_TYPE_UPDATE_EMAIL);
			if (validateCode != null) {
				userManager.unBindUserEmail(validateCode.getMobile(), IBSConstants.USER_TYPE_COMPANY_HR);
				userManager.unBindUserEmail(validateCode.getMobile(), IBSConstants.USER_TYPE_COMPANY_ADMIN);
				User user = new User();
				user.setEmail(validateCode.getMobile());
				user.setObjectId(validateCode.getUserId());
				userManager.updateUserByObjectId(user);
				// request.getSession().setAttribute(SecurityConstants.SESSION_USER,user);
				return "redirect:" + AppUtils.getHrDynamicDomain(null) + "/system/success";
			} else {
				return "redirect:" + AppUtils.getHrDynamicDomain(null) + "/system/fail";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "redirect:" + AppUtils.getHrDynamicDomain(null) + "/system/fail";
	}

	@RequestMapping("success")
	public String success(HttpServletRequest request) {
		return "system/success";
	}

	@RequestMapping("fail")
	public String fail(HttpServletRequest request) {
		return "system/fail";
	}

	/**
	 * updateCurrentPassword
	 * 
	 * @Title: updateCurrentPassword
	 * @Description: TODO (账户设置修改密码)
	 * @param @param request
	 * @param @param response
	 * @param @param t
	 * @param @return 设定文件
	 * @return String 返回类型
	 * @throws
	 */
	/*
	 * @RequestMapping("updateCurrentPassword") public String
	 * updateCurrentPassword(HttpServletRequest request,ModelMap
	 * modelMap,UpdatePasswordForm t ){ User sessionUser = (User)
	 * request.getSession().getAttribute(SecurityConstants.SESSION_USER); try {
	 * if(!sessionUser.getPassword().equals(SecurityUtils.generatePassword(t.
	 * getOldPassword()))){ modelMap.put("message", "当前密码不正确"); return
	 * "jsonView"; }else
	 * if(t==null||"".equals(t.getNewPassword())||t.getNewPassword()==null){
	 * modelMap.put("message", "请确认密码输入无误"); return "jsonView"; }else
	 * if(!t.getNewPassword().equals(t.getConfirmPassword())){
	 * modelMap.put("message", "确认密码不一致"); return "jsonView"; } User user = new
	 * User();
	 * user.setPassword(SecurityUtils.generatePassword(t.getNewPassword()));
	 * user.setObjectId(sessionUser.getObjectId());
	 * userManager.updatePassword(user); modelMap.put("message", "密码修改成功"); }
	 * catch (Exception e) { e.printStackTrace(); modelMap.put("message",
	 * "密码修改失败"); } return "jsonView"; }
	 */

	@RequestMapping("updatePayPassword")
	public String updatePayPassword(HttpServletRequest request, ModelMap modelMap, UpdatePayPasswordForm t) {
		User sessionUser = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		User localUser = userManager.getByObjectId(sessionUser.getObjectId());
		if(localUser!=null&&localUser.getMobilePhone()!=null){
			/**
			 * 1.验证验证码； 2.当前密码是否正确； 3.验证确认密码是否正确；
			 * 
			 */
			try {
				boolean bool = validateCodeManager.validateCode(t.getPayCode(), IBSConstants.VALIDATE_CODE_TYPE_UPDATE_PAY_PASSWORD, localUser.getMobilePhone());
				if (bool) {
					if (sessionUser.getPayPassword().equals(SecurityUtils.generatePassword(t.getCurrentPayPassword()))) {
						if (t.getNewPayPassword() != null && t.getConfirmPassword() != null && t.getNewPayPassword().equals(t.getConfirmPassword())) {
							User user = new User();
							user.setPayPassword(SecurityUtils.generatePassword(t.getNewPayPassword()));
							user.setObjectId(sessionUser.getObjectId());
							userManager.updateUserByObjectId(user);
							sessionUser.setPassword(SecurityUtils.generatePassword(t.getNewPayPassword()));
							request.setAttribute("message", "修改成功");
						} else {
							request.setAttribute("message", "两次支付密码设置不一样");
						}
					} else {
						request.setAttribute("message", "当前支付密码不正确");
					}
				} else {
					request.setAttribute("message", "验证码不正确");
				}
				Long userId = (Long) request.getSession().getAttribute(SecurityConstants.USER_ID);
				User user = userManager.getByObjectId(userId);
				String mobile = user.getMobilePhone();
				if (mobile != null && !"".equals(mobile)) {
					mobile = mobile.replace(mobile.substring(3, 7), "****");
					request.setAttribute("mobile", mobile);
				}
			} catch (Exception e) {
				e.printStackTrace();
				request.setAttribute("message", "支付密码修改失败");
			}
		}else{
			request.setAttribute("message", "用户信息已发生变更");
		}
		return "system/updatePayPassword";
	}

	/**
	 * 
	 * @Title: list
	 * @Description: 列表方法
	 * @param @param request
	 * @param @param response
	 * @param @return
	 * @return String
	 * @throws
	 * @author 陈传洞
	 */
	@RequestMapping("/loadaddrs")
	public String loadaddrs(HttpServletRequest request, HttpServletResponse response, ModelMap map) {
		Long userId = Long.parseLong((request.getSession().getAttribute(SecurityConstants.USER_ID).toString()));
		List<GoodsReceiptAddr> addrs = new ArrayList<GoodsReceiptAddr>();
		if (userId != null) {
			addrs = goodsReceiptAddrManager.getAddrByUserId(userId);
		}
		map.addAttribute("result", true);
		map.addAttribute("addrs", addrs);
		return JSON_VIEW;
	}

	/**
	 * 
	 * @Title: showGoodsReceiptAddr
	 * @Description: TODO 显示收货地址
	 * @param @param request
	 * @param @param addrId
	 * @param @param map
	 * @param @return
	 * @param @throws Exception
	 * @return String
	 * @throws
	 * @author 陈传洞
	 */
	@RequestMapping(value = "/showGoodsReceiptAddr/{addrId}")
	public String showGoodsReceiptAddr(HttpServletRequest request, @PathVariable Long addrId, ModelMap map) throws Exception {
		GoodsReceiptAddr receiptAddr = goodsReceiptAddrManager.getByObjectId(addrId);
		map.addAttribute("result", true);
		map.addAttribute("receiptAddr", receiptAddr);
		return JSON_VIEW;
	}

	/**
	 * 
	 * @param request
	 * @param modelMap
	 * @param code
	 * @return
	 */
	@RequestMapping(value = "/getAreaChildren/{code}")
	public String getChildren(HttpServletRequest request, ModelMap modelMap, @PathVariable String code) {
		List<Area> areas = areaManager.getChildren(Long.valueOf(code));
		modelMap.addAttribute("areas", areas);
		return "jsonView";
	}

	/**
	 * 
	 * @param request
	 * @param modelMap
	 * @param addr
	 * @return
	 */
	@RequestMapping(value = "/saveAddr")
	public String saveAddr(HttpServletRequest request, ModelMap modelMap, GoodsReceiptAddr addr) {
		Long userId = Long.parseLong((request.getSession().getAttribute(SecurityConstants.USER_ID).toString()));
		addr.setUserId(userId);
		addr.setDeleted(ForeverEntity.DELETED_NO);

		if (null == addr.getReceiptZipcode()) {
			addr.setReceiptZipcode(0L);
		}

		if (addr.getObjectId() == null) {
			addr.setCreateDate(new Date());
		} else {
			addr.setUpdateDate(new Date());
		}
		boolean flag = goodsReceiptAddrManager.saveAddr(addr, userId);
		modelMap.addAttribute("result", flag);
		return "jsonView";
	}

	/**
	 * 
	 * @Title: delAddr
	 * @Description: 删除收货地址
	 * @param @param request
	 * @param @param addrId
	 * @param @param map
	 * @param @return
	 * @param @throws Exception
	 * @return String
	 * @throws
	 * @author 陈传洞
	 */
	@RequestMapping(value = "/delAddr/{addrId}")
	public String delAddr(HttpServletRequest request, @PathVariable Long addrId, ModelMap map) throws Exception {
		Map<String, Object> resMap = goodsReceiptAddrManager.delAddr(addrId);
		String msg = resMap.get("msg").toString();
		Boolean flag = (Boolean) resMap.get("flag");
		if (!flag && msg.trim().equals("")) {
			msg = "删除收货地址失败！";
		}
		map.addAttribute("msg", msg);
		map.addAttribute("flag", flag);
		return JSON_VIEW;
	}

	/**
	 * 
	 * @Title: setDefault
	 * @Description: 设置默认收货地址
	 * @param @param request
	 * @param @param addrId
	 * @param @param map
	 * @param @return
	 * @param @throws Exception
	 * @return String
	 * @throws
	 * @author 陈传洞
	 */
	@RequestMapping(value = "/setDefault/{addrId}")
	public String setDefault(HttpServletRequest request, @PathVariable Long addrId, ModelMap map) throws Exception {
		Long userId = Long.parseLong((request.getSession().getAttribute(SecurityConstants.USER_ID).toString()));
		boolean res = goodsReceiptAddrManager.setDefault(addrId, userId);
		map.addAttribute("result", res);
		return JSON_VIEW;
	}

}
