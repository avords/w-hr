package com.handpay.ibenefit.portal.web;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.FrameworkConstants;
import com.handpay.ibenefit.framework.cache.ICacheManager;
import com.handpay.ibenefit.framework.config.GlobalConfig;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.AppUtils;
import com.handpay.ibenefit.framework.util.CookieUtils;
import com.handpay.ibenefit.framework.util.IpUtils;
import com.handpay.ibenefit.framework.web.HrFrameworkFilter;
import com.handpay.ibenefit.framework.web.MessageUtils;
import com.handpay.ibenefit.member.entity.CompanyFunction;
import com.handpay.ibenefit.member.entity.CompanyPublished;
import com.handpay.ibenefit.member.service.ICompanyFunctionManager;
import com.handpay.ibenefit.member.service.ICompanyPublishedManager;
import com.handpay.ibenefit.news.entity.Advert;
import com.handpay.ibenefit.news.service.IAdvertManager;
import com.handpay.ibenefit.portal.service.ILoginErrorManager;
import com.handpay.ibenefit.portal.service.ILoginManager;
import com.handpay.ibenefit.portal.service.IThemeManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.AuthenticationResult;
import com.handpay.ibenefit.security.entity.LoginLog;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.ILoginLogManager;
import com.handpay.ibenefit.security.service.IUserManager;

@Controller
@RequestMapping("/login")
public class HrLoginController {

	private static final Logger LOGGER = Logger.getLogger(HrLoginController.class);
	@Reference(version="1.0")
	private ILoginManager loginManager;
	@Reference(version="1.0")
	private IThemeManager themeManager;
	@Reference(version="1.0")
	private ILoginErrorManager loginErrorManager;
	@Reference(version="1.0")
	private ILoginLogManager loginLogManager;
	@Reference(version="1.0")
	private ICacheManager cacheManager;
	@Reference(version="1.0")
	private IUserManager userManager;
	@Reference(version = "1.0")
	private ICompanyPublishedManager companyPublishedManager;
	@Reference(version = "1.0")
	private IAdvertManager advertManager;
	@Reference(version = "1.0")
	private ICompanyFunctionManager companyFunctionManager;
	
	//华尔街域名简写
    private final String HEJ_DOMAIN_SHORTNAME="wse";
    private final String HEJ_LOGIN_URL = "wsehr.ibenefit.com.cn"; 
	public static void main(String[] args) {
		String host = "aighr.ibenefit.com.cn";
		String shortName = host.substring(0, host.indexOf(".")-2);
		System.out.println(shortName);

	}

	//查询二级域名所属企业
	private CompanyPublished getCompany(HttpServletRequest request){
		String serverName = request.getServerName();
		//企业二级域名
		String shortName = null;
		try{
			//https://aighr.ibenefit.com.cn/hr/login/in
			shortName = serverName.substring(0, serverName.indexOf(".")-2);
		}catch(Exception e){
		}
		CompanyPublished company = null;
		if(StringUtils.isNotBlank(shortName)){
			company = companyPublishedManager.getCompanyByShortName(shortName);
		}
		boolean showLogo = true;
		if(company!=null){
			CompanyFunction function = companyFunctionManager.getByCompanyIdAndFunction(company.getObjectId(), IBSConstants.COMPANY_FUNCTION_SHOW_LOGO);
			if(function==null){
				showLogo = false;
			}
		}
		request.setAttribute(SecurityConstants.COMPANY_FUNCTION_SHOW_LOGO, showLogo);
		return company;
	}

	private Advert getAdvert(){
		List<Advert> adverts = advertManager.getAdvertByParams("04_01", null, 1);
		if(adverts.size()>0){
			return adverts.get(0);
		}
		return null;
	}

	private String prepareInto(HttpServletRequest request, CompanyPublished company, User user){
		String dynamicDomain = "";
		if(company==null){
			company = companyPublishedManager.getByObjectId(user.getCompanyId());
		}
		if(company!=null){
			dynamicDomain = company.getShortName();
		}
		dynamicDomain = AppUtils.getHrDynamicDomain(dynamicDomain);
		String indexPage = dynamicDomain + "/index";
		request.getSession().setAttribute("dynamicDomain", dynamicDomain);
		return indexPage;
	}

	@RequestMapping(value = "in")
	public String index(HttpServletRequest request, HttpServletResponse response, User user) throws Exception{
		
		String userId=request.getParameter("userId");
		User redirectUser=null;
		if(StringUtils.isNotBlank(userId)){
			redirectUser=userManager.getByObjectId(Long.valueOf(userId));
		}
		
		CompanyPublished company = getCompany(request);
		request.getSession().setAttribute(FrameworkConstants.SKIN, FrameworkConstants.DEFAULT_SKIN);
		
		/**
		 * 添加华尔街登录定制
		 */
		String refferUrl = request.getHeader("Referer");
		String serverName = request.getServerName();
		if(StringUtils.isNotBlank(serverName)&& serverName.equals(HEJ_LOGIN_URL)){
			request.setAttribute("wallstreetenglish", IBSConstants.STATUS_YES);
		}else{
			request.setAttribute("wallstreetenglish", IBSConstants.STATUS_NO);
		}
		
		String loginUrl = "";//登录路径
		if(StringUtils.isBlank(request.getParameter("loginUrl"))){
			loginUrl = "https://"+serverName+request.getContextPath()+"/login/in";
		}else{
			loginUrl = request.getParameter("loginUrl");
		}
		
		Advert advert = getAdvert();
		String message = request.getParameter("message");
		User sessionUser = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		if (null != sessionUser) {
			return handleSuccess(request, response, company, sessionUser);
		}
		
		if (null != user && null != user.getLoginName()) {
			user.setPlatform(IBSConstants.PLATEFORM_HR);
			//管理员和HR共用
			user.setType(IBSConstants.USER_TYPE_COMPANY_ADMIN);
			if(company!=null){
				user.setCompanyId(company.getObjectId());
			}
			AuthenticationResult result = loginManager.authenticateUser(user, IpUtils.getRealIp(request), request.getSession().getId());
			// login success
			if (result.isSuccess()) {
				user.setLoginName(result.getUser().getLoginName());
				loginSuccess(request, response, user);
				return handleSuccess(request, response, company, result.getUser());
			}else{
				message = result.getMessage();
			}
		}
		if(message==null || message=="null"){
			message="";
		}
		request.setAttribute("message", message);
		request.setAttribute("company", company);
		request.setAttribute("advert", advert);
		request.setAttribute("loginUrl", loginUrl);
		//官网的登录
		if(request.getParameter("ajax")!=null && request.getParameter("ajax").equals("1")){
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("result",message);
			AjaxUtils.doJsonpResponseOfMap(response, request.getParameter("callback"), map);
			return null;
		}
		String requestUrl=request.getRequestURL().toString();
		String gcSecureUrl= GlobalConfig.getSecureDomain() + "/login/in";
		int requestUrlStart=requestUrl.indexOf("//");
		if(requestUrlStart!=-1){
			requestUrl=requestUrl.substring(requestUrlStart+2);
		}
		int gcUrlStart=gcSecureUrl.indexOf("//");
		if(gcUrlStart!=-1){
			gcSecureUrl=gcSecureUrl.substring(gcUrlStart+2);
		}
		if(requestUrl.equals(gcSecureUrl)){
			String reffer = AppUtils.getHrDynamicDomain(null) + "/login/in";
			if(redirectUser!=null){
				company = companyPublishedManager.getByObjectId(redirectUser.getCompanyId());
				if(company!=null){
					reffer = AppUtils.getHrDynamicDomain(company.getShortName()) + "/login/in";
				}
			}else{
				if(StringUtils.isNotBlank(refferUrl) && !"".equals(refferUrl)){
					if(refferUrl.indexOf(HEJ_DOMAIN_SHORTNAME)!=-1){
						reffer = AppUtils.getHrDynamicDomain(HEJ_DOMAIN_SHORTNAME) + "/login/in";
					}
				}	
			}
			int query = reffer.indexOf("?");
			if(query!=-1){
				reffer = reffer.substring(0, query);
			}
			
			if(StringUtils.isBlank(message)){
				return "redirect:" + reffer;	
			}else{
				return "redirect:" + reffer + MessageUtils.getMessage(message);	
			}
		}
		return "portal/login";
	}
	
	private String handleSuccess(HttpServletRequest request, HttpServletResponse response, CompanyPublished company, User user) throws Exception {
		if(request.getParameter("ajax") !=null && request.getParameter("ajax").equals("1")){
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("result", "ok");
			AjaxUtils.doJsonpResponseOfMap(response, request.getParameter("callback"), map);
			return null;
		}
		String indexPage = prepareInto(request, company, user);
		return "redirect:" + indexPage;
	}

	private void loginSuccess(HttpServletRequest request, HttpServletResponse response, User user) throws IOException {
		LoginLog loginLog = new LoginLog();
		loginLog.setLoginName(user.getLoginName());
		loginLog.setBeginDate(new Date());
		loginLog.setResult(LoginLog.LOGIN_RESULT_SUCCESS);
		User realUser = userManager.getUserByLoginName(user.getLoginName());
		user.setObjectId(realUser.getObjectId());
		loginLog.setUserId(realUser.getObjectId());
		loginErrorManager.deleteLoginLog(loginLog.getIp(),realUser.getLoginName());
		HrFrameworkFilter.setUserToSession(request, response, realUser.getLoginName());
		createLog(loginLog);
	}

	private void createLog(LoginLog loginLog) {
		loginLog.setEndDate(new Date());
		loginLog.setStatus(LoginLog.STATUS_ONLINE);
		loginLog.setSpendTime((int) (loginLog.getEndDate().getTime() - loginLog.getBeginDate().getTime()));
		String message = loginLog.getMessage();
		if(message!=null&&message.length()>255){
			loginLog.setMessage(message.substring(0,255));
		}
		loginLogManager.save(loginLog);
	}

	@RequestMapping(value = "in/out")
	public String loginOut(HttpServletRequest request,HttpServletResponse response) throws Exception {
		Integer logoutFrom = null;
		if(request.getParameter("logoutFrom")!=null){
			try{
				logoutFrom = Integer.parseInt(request.getParameter("logoutFrom"));
			}catch ( Exception e){
			}
		}
		if(logoutFrom==null){
			logoutFrom = LoginLog.LOGOUT_BY_MANUAL;
		}
        CookieUtils.deleteCookie(response, GlobalConfig.getSessionName());
        cacheManager.delete(request.getSession().getId());
		request.getSession().invalidate();
		return "redirect:../in";
	}
}
