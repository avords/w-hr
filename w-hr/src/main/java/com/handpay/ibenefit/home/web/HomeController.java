package com.handpay.ibenefit.home.web;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.gson.Gson;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.excitationActivity.entity.ExcitationActivity;
import com.handpay.ibenefit.excitationActivity.service.IExcitationActivityManager;
import com.handpay.ibenefit.framework.cache.ICacheManager;
import com.handpay.ibenefit.framework.config.GlobalConfig;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.service.Manager;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.web.PageController;
import com.handpay.ibenefit.grantWelfare.service.IGrantWelfareManager;
import com.handpay.ibenefit.home.entity.HomeWelfareItemView;
import com.handpay.ibenefit.home.service.IValidateCodeManager;
import com.handpay.ibenefit.info.service.IInfomationManager;
import com.handpay.ibenefit.member.entity.CompanyPublished;
import com.handpay.ibenefit.member.service.ICompanyFunctionManager;
import com.handpay.ibenefit.member.service.ICompanyPublishedManager;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.news.entity.Advert;
import com.handpay.ibenefit.news.entity.NewsNotify;
import com.handpay.ibenefit.news.service.IAdvertManager;
import com.handpay.ibenefit.news.service.INewsNotifyManager;
import com.handpay.ibenefit.order.entity.SubOrder;
import com.handpay.ibenefit.order.service.ISubOrderManager;
import com.handpay.ibenefit.point.entity.PointDistrubute;
import com.handpay.ibenefit.point.service.IPointDistrubuteManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.SecurityUtils;
import com.handpay.ibenefit.security.entity.LoginLog;
import com.handpay.ibenefit.security.entity.Menu;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.entity.UserMenus;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.system.entity.UpdateMobileForm;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.service.IWelfareManager;

/**
 * @ClassName: HomeController
 * @Description: TODO
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午7:02:15
 *
 */
@Controller
@RequestMapping("")
public class HomeController extends PageController<SubOrder> {

	private static final Logger LOGGER = Logger.getLogger(HomeController.class);
	@Reference(version="1.0")
	private IWelfareManager welfareManager;

	@Reference(version="1.0")
	private IInfomationManager infomationManager;

	@Reference(version="1.0")
	private IUserManager userManager;

	@Reference(version="1.0")
	private ICacheManager cacheManager;
	@Reference(version="1.0")
    private IDictionaryManager dictionaryManager;
	@Reference(version = "1.0")
	private IGrantWelfareManager grantWelfareManager;

	@Reference(version="1.0")
	private IPointDistrubuteManager pointDistrubuteManager;

	@Reference(version = "1.0")
    private IExcitationActivityManager exActivityManager;

	@Reference(version="1.0")
	private IValidateCodeManager validateCodeManager;

	@Reference(version="1.0")
    private IAdvertManager advertManager;

	@Reference(version="1.0")
	private ISubOrderManager subOrderManager;

	@Reference(version = "1.0")
	private INewsNotifyManager newsNotifyManager;

	@Reference(version="1.0")
	private ICompanyPublishedManager companyPublishedManager;

	@Reference(version="1.0")
	private ICompanyFunctionManager companyFunctionManager;

	@Reference(version="1.0")
	private IStaffManager iStaffManager;

	/**
	  * guide
	  *
	  * @Title: guide
	  * @Description: TODO (引导)
	  * @param @param request
	  * @param @param response
	  * @param @return    设定文件
	  * @return String    返回类型
	  * @throws
	 */
	@RequestMapping("guide")
	public String guide(HttpServletRequest request,HttpServletResponse response){
		Long userId = (Long) request.getSession().getAttribute(SecurityConstants.USER_ID);
		User user = userManager.getByObjectId(userId);
		if(user!=null&&user.getLastLoginTime()==null){
			String mobile = user.getMobilePhone();
			String email = user.getEmail();
			if(mobile!=null&&!"".equals(mobile)&&mobile.length()>7){
				mobile = mobile.replace(mobile.substring(3,7),"****");
				request.setAttribute("viewUserMobile", mobile);
				request.setAttribute("userMobile", user.getMobilePhone());
				request.setAttribute("mobileShow", true);
			}
			if(email!=null&&!"".equals(email)){
				request.setAttribute("userEmail", email);
				request.setAttribute("emailShow", true);
			}
			return "home/bind";
		}else if(user!=null&&user.getLastLoginTime()==null){
			return "home/initPassword";
		}else if(user!=null&&user.getPayPassword()==null){
			return "home/setPayPassword";
		}else{
			return "redirect:index";
		}
	}

	/**
	  * initPassword
	  *
	  * @Title: initPassword
	  * @Description: TODO (初始化密码)
	  * @param @param request
	  * @param @param response
	  * @param @return    设定文件
	  * @return String    返回类型
	  * @throws
	 */
	@RequestMapping("initPassword")
	public String initPassword(HttpServletRequest request,HttpServletResponse response){
		Long userId = (Long)request.getSession().getAttribute(SecurityConstants.USER_ID);
		User sessionUser = userManager.getByObjectId(userId);
		String newPassword = request.getParameter("newPassword");
		String confirmPassword = request.getParameter("confirmPassword");
		if(newPassword!=null&&confirmPassword!=null&&newPassword.equals(confirmPassword)){
			User user = new User();
			user.setPassword(SecurityUtils.generatePassword(newPassword));
			user.setObjectId(sessionUser.getObjectId());
			user.setLastLoginTime(new Date());
			userManager.updateUserByObjectId(user);
			sessionUser.setPassword(SecurityUtils.generatePassword(newPassword));
			sessionUser.setLastLoginTime(user.getLastLoginTime());
			request.getSession().setAttribute(SecurityConstants.SESSION_USER, sessionUser);
		}else{
			request.setAttribute("message", "两次密码设置不一样");
		}
		if(sessionUser!=null&&(sessionUser.getMobilePhone()==null&&sessionUser.getEmail()==null)){
			return "home/bind";
		}else if(sessionUser!=null&&sessionUser.getLastLoginTime()==null){
			return "home/initPassword";
		}else if(sessionUser!=null&&sessionUser.getPayPassword()==null){
			return "home/setPayPassword";
		}else{
			return "home/finish";
		}
	}

	/**
	  * bind
	  *
	  * @Title: bind
	  * @Description: TODO (绑定手机号)
	  * @param @param request
	  * @param @param response
	  * @param @return    设定文件
	  * @return String    返回类型
	  * @throws
	 */
	@RequestMapping("bind")
	public String bind(HttpServletRequest request,HttpServletResponse response,UpdateMobileForm t){
		Long userId = (Long)request.getSession().getAttribute(SecurityConstants.USER_ID);
		User sessionUser = userManager.getByObjectId(userId);
		if("mail".equals(t.getType())){
			if(t.getNewEmail()!=null&&!"".equals(t.getNewEmail())&&t.getCode()!=null&&!"".equals(t.getCode())){
				boolean bool = validateCodeManager.validateCode(t.getCode(),IBSConstants.VALIDATE_CODE_TYPE_UPDATE_EMAIL,t.getNewEmail());
				if(bool){
					//解绑用户邮箱
					userManager.unBindUserEmail(t.getNewEmail(),IBSConstants.USER_TYPE_COMPANY_ADMIN);
					userManager.unBindUserEmail(t.getNewEmail(),IBSConstants.USER_TYPE_COMPANY_HR);
					User user = new User();
					user.setEmail(t.getNewEmail());
					user.setObjectId(sessionUser.getObjectId());
					userManager.updateUserByObjectId(user);
					sessionUser.setEmail(t.getNewEmail());
					request.getSession().setAttribute(SecurityConstants.SESSION_USER, sessionUser);
					request.setAttribute("message", "邮箱绑定成功");
				}else{
					request.setAttribute("message", "验证码错误");
					String mobile = sessionUser.getMobilePhone();
					String email = sessionUser.getEmail();
					if(mobile!=null&&!"".equals(mobile)&&mobile.length()>7){
						mobile = mobile.replace(mobile.trim().substring(3,7),"****");
						request.setAttribute("viewUserMobile", mobile);
						request.setAttribute("mobileShow", true);
					}
					if(email!=null&&!"".equals(email)){
						request.setAttribute("userEmail", email);
						request.setAttribute("emailShow", true);
					}
					request.setAttribute("userMobile", sessionUser.getMobilePhone());
					return "home/bind";
				}
			}
		}else if("mobi".equals(t.getType())){
			if(t.getNewMobile()!=null&&!"".equals(t.getNewMobile())&&t.getCode()!=null&&!"".equals(t.getCode())){
				boolean bool = validateCodeManager.validateCode(t.getCode(),IBSConstants.VALIDATE_CODE_TYPE_BIND_MOBILE,t.getNewMobile());
				if(bool){
					//解绑用户手机
					userManager.unBindUserMobilePhone(t.getNewMobile(),IBSConstants.USER_TYPE_COMPANY_ADMIN);
					userManager.unBindUserMobilePhone(t.getNewMobile(),IBSConstants.USER_TYPE_COMPANY_HR);
					User user = new User();
					user.setMobilePhone(t.getNewMobile());
					user.setObjectId(sessionUser.getObjectId());
					userManager.updateUserByObjectId(user);
					sessionUser.setMobilePhone(t.getNewMobile());
					request.getSession().setAttribute(SecurityConstants.SESSION_USER, sessionUser);
					request.setAttribute("message", "手机号码更新成功");
				}else{
					request.setAttribute("message", "验证码错误");
					String mobile = sessionUser.getMobilePhone();
					String email = sessionUser.getEmail();
					if(mobile!=null&&!"".equals(mobile)&&mobile.length()>7){
						mobile = mobile.replace(mobile.trim().substring(3,7),"****");
						request.setAttribute("viewUserMobile", mobile);
						request.setAttribute("mobileShow", true);
					}
					if(email!=null&&!"".equals(email)){
						request.setAttribute("userEmail", email);
						request.setAttribute("emailShow", true);
					}
					request.setAttribute("userMobile", sessionUser.getMobilePhone());
					request.setAttribute("type", "mobi");
					return "home/bind";
				}
			}
		}
		if(sessionUser!=null&&(sessionUser.getMobilePhone()==null&&sessionUser.getEmail()==null)){
			return "home/bind";
		}else if(sessionUser!=null&&sessionUser.getLastLoginTime()==null){
			return "home/initPassword";
		}else if(sessionUser!=null&&sessionUser.getPayPassword()==null){
			return "home/setPayPassword";
		}else{
			return "home/finish";
		}
	}

	/**
	  * setPayPassword
	  *
	  * @Title: setPayPassword
	  * @Description: TODO (设置支付密码)
	  * @param @param request
	  * @param @param response
	  * @param @return    设定文件
	  * @return String    返回类型
	  * @throws
	 */
	@RequestMapping("setPayPassword")
	public String setPayPassword(HttpServletRequest request,HttpServletResponse response){
		Long userId = (Long)request.getSession().getAttribute(SecurityConstants.USER_ID);
		User sessionUser = userManager.getByObjectId(userId);
		String newPassword = request.getParameter("payPassword");
		String confirmPassword = request.getParameter("confirmPayPassword");
		if(newPassword!=null&&confirmPassword!=null&&newPassword.equals(confirmPassword)){
			User user = new User();
			user.setPayPassword(SecurityUtils.generatePassword(newPassword));
			user.setObjectId(sessionUser.getObjectId());
			user.setLastLoginTime(new Date());
			userManager.updateUserByObjectId(user);
			sessionUser.setPayPassword(user.getPayPassword());
			sessionUser.setLastLoginTime(new Date());
			request.getSession().setAttribute(SecurityConstants.SESSION_USER, sessionUser);
		}else{
			request.setAttribute("message", "两次支付密码设置不一样");
		}
		if(sessionUser!=null&&(sessionUser.getMobilePhone()==null&&sessionUser.getEmail()==null)){
			return "home/bind";
		}else if(sessionUser!=null&&sessionUser.getLastLoginTime()==null){
			return "home/initPassword";
		}else if(sessionUser!=null&&sessionUser.getPayPassword()==null){
			return "home/setPayPassword";
		}else{
			return "home/finish";
		}
	}

	@RequestMapping("index")
	public String index(HttpServletRequest request,HttpServletResponse response){
		User sessionUser = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		User user = userManager.getByObjectId(sessionUser.getObjectId());
		if(user!=null&&(user.getLastLoginTime()==null)){
			return "redirect:guide";
		}

		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		CompanyPublished companyPublished = companyPublishedManager.getByObjectId(companyId);
		String areaCode = (companyPublished.getAreaId().trim()).substring(0,4);
		request.getSession().setAttribute("areaCode", areaCode);
	    //专题菜单
        List<Dictionary> themes = dictionaryManager.getDictionariesByDictionaryId(1121);
        request.getSession().setAttribute("themes", themes);
        List<Dictionary> hotwords = dictionaryManager.getDictionariesByDictionaryId(1126);
        request.getSession().setAttribute("hotwords", hotwords);
        request.setAttribute("subject", "index");
        List<Advert> specialRecommend = advertManager.getAdvertByParams("04_02",areaCode, 3);
        request.setAttribute("specialRecommend", specialRecommend);


		Map<String, Object> param = new HashMap<String, Object>();
		param.put("itemGrade", IBSConstants.WELFARE_PACKAGE_PARENT_ITEM);

		try {
			List<WelfareItem> welfareItems = welfareManager.getItemByParam(param);
			List<HomeWelfareItemView> items = new ArrayList<HomeWelfareItemView>();
			int count = 0;
			for(WelfareItem welfareItem : welfareItems){
				if(count>2){
					break;
				}
				HomeWelfareItemView item = new HomeWelfareItemView();
				item.setItemId(welfareItem.getObjectId());
				item.setItemName(welfareItem.getItemName());
				Map<String, Object> subParam = new HashMap<String, Object>();
				subParam.put("parentItemId", welfareItem.getObjectId());
				subParam.put("status", IBSConstants.STATUS_YES);
				List<WelfareItem> subItems = welfareManager.getItemByParam(subParam);
				item.setItems(subItems);
				items.add(item);
				count++;
			}
			request.setAttribute("items", items);
			CompanyPublished company = companyPublishedManager.getByObjectId(companyId);
			List<NewsNotify> infos = new ArrayList<NewsNotify>();
			if(company!=null){
				if(company.getAreaId().length()==6){
					infos = newsNotifyManager.getNewsNotifyByArea(company.getAreaId().substring(0,4));
				}else if(company.getAreaId().length()==4){
					infos = newsNotifyManager.getNewsNotifyByArea(company.getAreaId());
				}
			}
			request.setAttribute("infos", infos);

			//代办事项---待发放福利卷
			Long userId = 0l;
			if (request.getSession().getAttribute(SecurityConstants.USER_ID) != null) {
				userId = (Long) request.getSession().getAttribute(
						SecurityConstants.USER_ID);
			}

			PageSearch page  = preparePage(request);
			Map map = new HashMap();
			if(IBSConstants.USER_TYPE_COMPANY_ADMIN!=sessionUser.getType()){//非企业管理员
				map.put("userId", sessionUser.getObjectId());
//				PropertyFilter userIdFilter = new PropertyFilter();
//				userIdFilter.setPropertyName("userId");
//				userIdFilter.setPropertyValue(sessionUser.getObjectId());
//				page.getFilters().add(userIdFilter);
			}else{//企业管理员
				String hresStr = String.valueOf(user.getObjectId());//企业管理员 下所有的HR（多个用,分割）
				User sample = new User();
				sample.setCompanyId(user.getCompanyId());
				sample.setType(IBSConstants.USER_TYPE_COMPANY_HR);
				List<User> hres = userManager.getBySample(sample);
				
				for (User userTemp : hres) {
					hresStr+=","+String.valueOf(userTemp.getObjectId());
				}
				
//				PropertyFilter userIdFilter = new PropertyFilter();
//				userIdFilter.setPropertyName("userId");
//				userIdFilter.setPropertyValue(hresStr);
//				page.getFilters().add(userIdFilter);
				
				map.put("userId", hresStr);
				
			}

			//子订单删除标识，未删除
			map.put("deleted", IBSConstants.NOT_DELETE);
//			PropertyFilter deleteFilter = new PropertyFilter();
//			deleteFilter.setPropertyName("deleted");
//			deleteFilter.setPropertyValue(IBSConstants.NOT_DELETE);
//			page.getFilters().add(deleteFilter);

			//子订单状态 : 待发放
			map.put("subOrderState", IBSConstants.ORDER_TO_BE_ISSUED_VIRTUAL);
//			PropertyFilter subOrderStateFilter = new PropertyFilter();
//			subOrderStateFilter.setPropertyName("subOrderState");
//			subOrderStateFilter.setPropertyValue(IBSConstants.ORDER_TO_BE_ISSUED_VIRTUAL);
//			page.getFilters().add(subOrderStateFilter);

			List<String> orderProdTypeList = new ArrayList<String>() ;
			orderProdTypeList.add(String.valueOf(IBSConstants.ORDER_PRODUCT_TYPE_WELFARE)) ;//福利套餐1
			orderProdTypeList.add(String.valueOf(IBSConstants.ORDER_PRODUCT_TYPE_PHYSICAL)) ;//体检套餐2

			//订单商品类型：福利套餐1，体检套餐2
			map.put("orderProdTypeList", orderProdTypeList);
//			PropertyFilter orderProdTypeFilter = new PropertyFilter();
//			orderProdTypeFilter.setPropertyName("orderProdTypeList");
//			orderProdTypeFilter.setPropertyValue(orderProdTypeList);
//			page.getFilters().add(orderProdTypeFilter);

			//子订单类型：虚拟
			map.put("subOrderType", IBSConstants.SUB_ORDER_TYPE_ELECTRON);//虚拟2
//			PropertyFilter subOrderTypeFilter = new PropertyFilter();
//			subOrderTypeFilter.setPropertyName("subOrderType");
//			subOrderTypeFilter.setPropertyValue(IBSConstants.SUB_ORDER_TYPE_ELECTRON);//虚拟2
//			page.getFilters().add(subOrderTypeFilter);

			Long totalCount = subOrderManager.findWelfareListCount(map) ;
			totalCount = (totalCount !=null ? totalCount : 0 );
			request.setAttribute("num", totalCount);

			Double accountBalance =  userManager.getUserBalance(userId);
			request.setAttribute("accountBalance", accountBalance);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error(e);
		}
		//设置用户菜单权限
		setUserMenus(request);
		return "home/index";
	}

	/**
	  * setUserMenus
	  *
	  * @Title: setUserMenus
	  * @Description: TODO (设置用户菜单权限)
	  * @param @param request    设定文件
	  * @return void    返回类型
	  * @throws
	 */
	private void setUserMenus(HttpServletRequest request) {
		boolean COMPANY_INDEX_INFOMATION = false;
		boolean COMPANY_INDEX_QUESTIONNAIRE = false;
		boolean COMPANY_INDEX_DEPARTMENT = false;
		boolean COMPANY_INDEX_STAFF = false;
		boolean COMPANY_INDEX_PERMISSION = false;
		boolean COMPANY_INDEX_BUY_POINTS = false;
		boolean COMPANY_INDEX_DISTRIBUTE = false;
		

		boolean COMPANY_INDEX_EXCITATION = false;
		boolean COMPANY_INDEX_WELFARE = false;
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		Integer platform = (Integer) request.getSession().getAttribute(SecurityConstants.USER_PLATFORM);
		String welfarePointName = (String) request.getSession().getAttribute(SecurityConstants.COMPANY_WELFARE_POINT_NAME);
		List<Menu> allPermissionMenus = (List<Menu>) request.getSession().getAttribute(SecurityConstants.MENU_PERMISSION);
		Gson gson = new Gson();
		List<UserMenus> userMenus = new ArrayList<UserMenus>();
		for(Menu menu : allPermissionMenus){
			if(menu.getParentId() == Long.parseLong(String.valueOf(platform))){
				UserMenus userMenu = new UserMenus();
				userMenu.setFolderName(menu.getName());
				userMenu.setFolderId(menu.getObjectId());
				List<Menu> subMenus = new ArrayList<Menu>();
				for(Menu subMenu : allPermissionMenus){
					subMenu.setName(subMenu.getName().replaceAll("积分", welfarePointName));
					if(subMenu.getParentId().equals(menu.getObjectId())){
						if(subMenu.getObjectId()==IBSConstants.COMPANY_INDEX_INFOMATION){
							COMPANY_INDEX_INFOMATION = true;
						}else if(subMenu.getObjectId()==IBSConstants.COMPANY_INDEX_DEPARTMENT){
							COMPANY_INDEX_DEPARTMENT = true;
						}else if(subMenu.getObjectId()==IBSConstants.COMPANY_INDEX_PERMISSION){
							COMPANY_INDEX_PERMISSION = true;
						}else if(subMenu.getObjectId()==IBSConstants.COMPANY_INDEX_QUESTIONNAIRE){
							COMPANY_INDEX_QUESTIONNAIRE = true;
						}else if(subMenu.getObjectId()==IBSConstants.COMPANY_INDEX_STAFF){
							COMPANY_INDEX_STAFF = true;
						}else if(subMenu.getObjectId()==IBSConstants.COMPANY_INDEX_BUY_POINTS){
							COMPANY_INDEX_BUY_POINTS = true;
						}else if(subMenu.getObjectId()==IBSConstants.COMPANY_INDEX_DISTRIBUTE){
							COMPANY_INDEX_DISTRIBUTE = true;
						}
						//是否显示激励
						if(subMenu.getParentId() == IBSConstants.COMPANY_FUNCTION_SHOW_EXCITATION){
							COMPANY_INDEX_EXCITATION = true;
							boolean bool = (Boolean) request.getSession().getAttribute(SecurityConstants.COMPANY_FUNCTION_SHOW_EXCITATION);
							if(!bool){
								continue;
							}
						}else if(subMenu.getParentId() == IBSConstants.COMPANY_FUNCTION_SHOW_WELFARE){
							COMPANY_INDEX_WELFARE=true;
							boolean bool = (Boolean) request.getSession().getAttribute(SecurityConstants.COMPANY_FUNCTION_SHOW_WELFARE);
							if(!bool){
								continue;
							}
						}else if(subMenu.getParentId() == IBSConstants.COMPANY_FUNCTION_SHOW_REPORT){
							boolean bool = (Boolean) request.getSession().getAttribute(SecurityConstants.COMPANY_FUNCTION_SHOW_REPORT);
							if(!bool){
								continue;
							}
						}
						subMenus.add(subMenu);
					}
				}
				if(subMenus.size()>0){
					userMenu.setMenus(subMenus);
					userMenus.add(userMenu);
				}
			}
		}
		request.getSession().setAttribute("COMPANY_INDEX_INFOMATION", COMPANY_INDEX_INFOMATION);
		request.getSession().setAttribute("COMPANY_INDEX_DEPARTMENT", COMPANY_INDEX_DEPARTMENT);
		request.getSession().setAttribute("COMPANY_INDEX_PERMISSION", COMPANY_INDEX_PERMISSION);
		request.getSession().setAttribute("COMPANY_INDEX_QUESTIONNAIRE", COMPANY_INDEX_QUESTIONNAIRE);
		request.getSession().setAttribute("COMPANY_INDEX_STAFF", COMPANY_INDEX_STAFF);
		request.getSession().setAttribute("COMPANY_INDEX_BUY_POINTS", COMPANY_INDEX_BUY_POINTS);
		request.getSession().setAttribute("COMPANY_INDEX_DISTRIBUTE", COMPANY_INDEX_DISTRIBUTE);
		request.getSession().setAttribute("COMPANY_INDEX_EXCITATION", COMPANY_INDEX_EXCITATION);
		request.getSession().setAttribute("COMPANY_INDEX_WELFARE", COMPANY_INDEX_WELFARE);
		
		request.getSession().setAttribute("userMenus", userMenus);
	}

	@RequestMapping(value = "out")
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
        for (Cookie cookie : request.getCookies()) {
            Cookie deleted = new Cookie(cookie.getName(), null);
            deleted.setMaxAge(0);
            deleted.setDomain(GlobalConfig.getCookieDomain());
            deleted.setPath(GlobalConfig.getCookiePath());
            response.addCookie(deleted);
        }
        cacheManager.delete(request.getSession().getId());
		request.getSession().invalidate();
		return "redirect:/index";
	}

	@RequestMapping("500")
	public String error500(HttpServletRequest request,HttpServletResponse response){
		return "common/500";
	}
	
	@RequestMapping("pageNotFound")
	public String pageNotFound(HttpServletRequest request,HttpServletResponse response){
		return "common/404";
	}
	
	@RequestMapping("noPermission")
	public String noPermission(HttpServletRequest request,HttpServletResponse response){
		return "common/403";
	}

	@RequestMapping(value = "/queryActivity", method = RequestMethod.POST)
	public String queryUnderway(HttpServletRequest request, ModelMap modelMap) throws Exception {
		User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		if(user.getType()==IBSConstants.USER_TYPE_COMPANY_HR){
			List<ExcitationActivity> underways = exActivityManager.queryActivityByUserId(user.getObjectId(),IBSConstants.INFOMATION_PUBLISH);
			modelMap.put("underway", underways.size());
			List<ExcitationActivity> finisheds = exActivityManager.queryActivityByUserId(user.getObjectId(),IBSConstants.INFOMATION_DELAY);
			modelMap.put("finished", finisheds.size());
		}else if(user.getType()==IBSConstants.USER_TYPE_COMPANY_ADMIN){
			List<ExcitationActivity> underways = exActivityManager.queryActivityByCompanyId(user.getCompanyId(),IBSConstants.INFOMATION_PUBLISH);
			modelMap.put("underway", underways.size());
			List<ExcitationActivity> finisheds = exActivityManager.queryActivityByCompanyId(user.getCompanyId(),IBSConstants.INFOMATION_DELAY);
			modelMap.put("finished", finisheds.size());
		}
		
		return "jsonView";
	}

	@RequestMapping(value = "/queryCurrentYearGrant", method = RequestMethod.POST)
	public String queryCurrentYearGrant(HttpServletRequest request, ModelMap modelMap) throws Exception {
		User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		if(user.getType()==IBSConstants.USER_TYPE_COMPANY_HR){
			PointDistrubute items = pointDistrubuteManager.queryCurrentYearGrantByUserId(user.getObjectId(),PointDistrubute.STATUS_FINISHED);
			modelMap.put("grantCount", items.getHeadCount());
			modelMap.put("grantTotal", items.getTotalPoint());
		}else if(user.getType()==IBSConstants.USER_TYPE_COMPANY_ADMIN){
			PointDistrubute items = pointDistrubuteManager.queryCurrentYearGrant(user.getCompanyId(),PointDistrubute.STATUS_FINISHED);
			modelMap.put("grantCount", items.getHeadCount());
			modelMap.put("grantTotal", items.getTotalPoint());
		}
		return "jsonView";
	}

	@RequestMapping(value = "/queryAccountBalance", method = RequestMethod.POST)
	public String queryAccountBalance(HttpServletRequest request, ModelMap modelMap) throws Exception {
		Long userId = (Long) request.getSession().getAttribute(SecurityConstants.USER_ID);
		Double accountBalance =  userManager.getUserBalance(userId);
		if(accountBalance==null){
			modelMap.put("accountBalance", 0.00);
		}else{
			DecimalFormat decimalFormat=new DecimalFormat("#.##");
			modelMap.put("accountBalance", decimalFormat.format(accountBalance));
		}
		return "jsonView";
	}

	@Override
	public Manager<SubOrder> getEntityManager() {
		return null;
	}

	@Override
	public String getFileBasePath() {
		return "home/";
	}

	public static void main(String[] args) {
		String mobile = "18616029816";
		System.out.println(mobile.replace(mobile.substring(3, 7),"****"));
	}
}
