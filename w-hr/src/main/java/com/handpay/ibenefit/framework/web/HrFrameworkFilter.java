package com.handpay.ibenefit.framework.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.web.util.WebUtils;

import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.FrameworkConstants;
import com.handpay.ibenefit.framework.ProjectConfig;
import com.handpay.ibenefit.framework.config.GlobalConfig;
import com.handpay.ibenefit.framework.context.FrameworkContextImpl;
import com.handpay.ibenefit.framework.context.ThreadLocalContextHolder;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.IpUtils;
import com.handpay.ibenefit.framework.util.LocaleUtils;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.SecurityUtils;
import com.handpay.ibenefit.security.entity.Menu;
import com.handpay.ibenefit.security.entity.MenuLink;
import com.handpay.ibenefit.security.entity.User;

/**
 * Framework filter,including function ad follows:
 * 1、Security check
 * 2、Initialize framework utility
 * 3、Exception handle
 * 4、Log4j parameters set
 * 5、I18n config
 * @author pubx
 *
 */
public class HrFrameworkFilter implements Filter {
	private static final int AUTHENTICATE_SUCCESS = 1;
	private static final Logger LOGGER = Logger.getLogger(HrFrameworkFilter.class);
	private static final int ONE_SECOND = 1000;
	private static final int ONE_MINUTE_IN_MILLIS = 60 * 1000;
	private static final String PERMISSION_FILTERED_MENU = "SecurityFilter.filterMenu";
	private static final String PERMISSION_MY_URL = "SecurityFilter.myUrl";

	private static final String REQUEST_AJAX = "ajax";
	private static final String AJAX_TYPE_COMMON = "1";

	private static final String[] MDC_OBJECTS = new String[] {
			SecurityConstants.FULL_NAME, SecurityConstants.USER_ID,
			SecurityConstants.LOGIN_NAME, SecurityConstants.SECURITY_TOKEN,
			SecurityConstants.IP };

	private List<String> notNeedLoginUrls = null;
	private volatile List<String> allPermissionUrls = new ArrayList<String>();
	private volatile boolean hasPermissionUrl = false;
	private static int refreshPermissionTime;
	private static boolean securityFilter = true;
	private static boolean accessAll = true;


	@Override
    public void destroy() {
	}

	@Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
	        throws IOException, ServletException {
		if ((servletRequest instanceof HttpServletRequest) && (servletResponse instanceof HttpServletResponse)) {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			HttpSession session = request.getSession();
			localeChangeFilter(request, response);
			if(securityFilter){
				/**
				 * 1.Need authenticate
				 * 2.Is login URL
				 * 3.Is authenticate success
				 * 4.Has permission
				 */
			    if (isNeedAuthentication(request.getRequestURI())) {
			    	int authenticateResult = authenticateRequest(request);
			    	String servletPath = request.getServletPath();
			    	if (AUTHENTICATE_SUCCESS == authenticateResult) {
			    		if (isNeedValidate(servletPath, request)) {
			    			boolean ok = validatePermissionMenu(session, servletPath);
			    			if (!ok) {
			    				LOGGER.info(session.getAttribute(SecurityConstants.FULL_NAME) + " illegal access " + servletPath);
			    				echoIllegalAccess(response);
			    				return;
			    			}
			    		}
			    	} else {
			    		if (!GlobalConfig.getLoginUrl().endsWith(request.getRequestURI())) {
			    			response.sendRedirect( GlobalConfig.getLoginUrl());
			    			return;
			    		}
			    	}
			    }
			}
			try {
				FrameworkContextImpl frameworkContext = new FrameworkContextImpl();
				User user = (User) session.getAttribute(SecurityConstants.SESSION_USER);
				frameworkContext.setCurrentUser(user);
				ThreadLocalContextHolder.setContext(frameworkContext);
				putObjectToLog4jMdc(request, session);
				filterChain.doFilter(servletRequest, servletResponse);
				removeObjectFromLog4jMdc();
			} catch (Exception e) {
				doError(request, response, e);
			}
		}
	}

	private void localeChangeFilter(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = WebUtils.getCookie(request, LocaleUtils.DEFAULT_LOCALE_PARAM_NAME);
		String localeName = null;
		if(cookie==null){
			localeName = request.getParameter(LocaleUtils.DEFAULT_LOCALE_PARAM_NAME);
			if(localeName==null){
				localeName = request.getLocale().toString();
			}
		}else{
			localeName = cookie.getValue();
		}
	    if(localeName != null){
	    	Locale locale = LocaleUtils.getLocale(localeName);
	    	LocaleUtils.setSpringLocale(request.getSession(), locale);
	    	LocaleUtils.setExtremetableLocale(request.getSession(), localeName);
	    }
    }

	private void removeObjectFromLog4jMdc() {
	    for (int i = 0; i < MDC_OBJECTS.length; i++) {
	    	MDC.remove(MDC_OBJECTS[i]);
	    }
    }

	private void putObjectToLog4jMdc(HttpServletRequest request, HttpSession session) {
	    session.setAttribute(SecurityConstants.IP, IpUtils.getRealIp(request));
	    for (int i = 0; i < MDC_OBJECTS.length; i++) {
	    	Object obj = session.getAttribute(MDC_OBJECTS[i]);
	    	if (obj != null) {
	    		MDC.put(MDC_OBJECTS[i], obj);
	    	}
	    }
    }

	private void echoIllegalAccess(HttpServletResponse response) throws IOException {
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	}

	protected boolean isNeedAuthentication(final String requestUrl) {
		if (notNeedLoginUrls == null) {
			return true;
		} else {
			for (String url : notNeedLoginUrls) {
				if (requestUrl.indexOf(url)!=-1) {
					return false;
				}
			}
		}
		return true;
	}

	public static int authenticateRequest(HttpServletRequest request) {
		int result = 0;
		if (request.getSession().getAttribute(SecurityConstants.SESSION_USER) !=null) {
			result = AUTHENTICATE_SUCCESS;
		}
		return result;
	}

	protected boolean isNeedValidate(final String requestUrl, HttpServletRequest request) {
		boolean result = false;
		if(!accessAll){
			result = true;
		}else if (hasPermissionUrl) {
			for(String url : allPermissionUrls){
				if (requestUrl.matches(url)) {
					result = true;
				}
			}
		}
		return result;
	}

	private boolean validatePermissionMenu(HttpSession session, String requestURI) {
		List<String> myPermiddionUrl = (List<String>) session.getAttribute(PERMISSION_MY_URL);
		if(null!=myPermiddionUrl){
			for (String menu : myPermiddionUrl) {
				if (requestURI.matches(menu)) {
					return true;
				}
			}
		}
		return false;
	}

	public static void setUserToSession(HttpServletRequest request, HttpServletResponse response,String loginName) throws IOException {
		setUserToSessionInternal(request, response, FrameworkConstants.DEFAULT_SKIN, loginName);
	}

	private static void setUserToSessionInternal(HttpServletRequest request, HttpServletResponse response, String skin, String loginName) throws IOException {
		User user = FrameworkFactory.getAuthorizationManager().getUserPermissionByLoginName(loginName, ProjectConfig.PROJECT_NAME);
		if (user != null&&IBSConstants.PLATEFORM_HR == user.getPlatform()) {
			HttpSession session = request.getSession();
			session.setAttribute(SecurityConstants.SECURITY_TOKEN, SecurityUtils.generateSecurityToken(loginName));
			session.setAttribute(SecurityConstants.LOGIN_NAME, loginName);
			session.setAttribute(SecurityConstants.FULL_NAME, user.getUserName());
			session.setAttribute(SecurityConstants.USER_ID, user.getObjectId());
			session.setAttribute(SecurityConstants.SESSION_USER, user);
			session.setAttribute(SecurityConstants.MENU_PERMISSION, user.getMenus());
			session.setAttribute(SecurityConstants.OPERATION_PERMISSION, user.getOperations());
			session.setAttribute(SecurityConstants.USER_PLATFORM, user.getPlatform());
			session.setAttribute(SecurityConstants.USER_COMPANY_ID, user.getCompanyId());
			session.setAttribute(SecurityConstants.COMPANY_NAME, user.getCompanyName());
			session.setAttribute(SecurityConstants.COMPANY_LOGO, user.getCompanyLogo());
			session.setAttribute(SecurityConstants.COMPANY_EMAIL, user.getCompanyEemail());
			session.setAttribute(SecurityConstants.COMPANY_VERIFYSTATUS, user.getCompanyVerifyStatus());
			session.setAttribute(SecurityConstants.COMPANY_WELFARE_POINT_NAME, user.getWelfarePointName());
			
			session.setAttribute(SecurityConstants.COMPANY_FUNCTION_INNER_COMPANY, user.isInnerCompany());
			session.setAttribute(SecurityConstants.COMPANY_FUNCTION_SHOW_EXCITATION, user.isShowExcitation());
			session.setAttribute(SecurityConstants.COMPANY_FUNCTION_SHOW_LOGO, user.isShowLogo());
			session.setAttribute(SecurityConstants.COMPANY_FUNCTION_SHOW_REPORT, user.isShowReport());
			session.setAttribute(SecurityConstants.COMPANY_FUNCTION_SHOW_STAFF_LEVEL, user.isShowStaffLevel());
			session.setAttribute(SecurityConstants.COMPANY_FUNCTION_SHOW_WELFARE, user.isShowWelfare());
			
			List<Menu> allMenu = user.getMenus();
			List<Menu> filterMenu = new ArrayList<Menu>(allMenu.size());
			List<String> myPermissionUrls = new ArrayList<String>();
			for(Menu menu : allMenu) {
				if(Menu.TYPE_MENU == menu.getType()) {
					filterMenu.add(menu);
					addPermissionUrl(menu,myPermissionUrls);
				}
			}
			session.setAttribute(PERMISSION_FILTERED_MENU, filterMenu);
			session.setAttribute(PERMISSION_MY_URL, myPermissionUrls);
			if(StringUtils.isBlank(skin)){
				skin = FrameworkConstants.DEFAULT_SKIN;
			}
			session.setAttribute(FrameworkConstants.SKIN, skin);
		}
	}
	
	private Timer timer1 = new Timer();
	@Override
    public void init(FilterConfig filterConfig) throws ServletException {
		ServletContext context = filterConfig.getServletContext();
		context.setAttribute("loginUrl",GlobalConfig.getLoginUrl());
		context.setAttribute("dynamicDomain",GlobalConfig.getDynamicDomain());
		context.setAttribute("cookiePath", GlobalConfig.getCookiePath());
		context.setAttribute("cookieDomain", GlobalConfig.getCookieDomain());
		context.setAttribute("sessionName", GlobalConfig.getSessionName());
		context.setAttribute("tokenName", SecurityConstants.SECURITY_TOKEN);
		context.setAttribute("adminStaticDomain", GlobalConfig.getAdminStaticDomain());
		context.setAttribute("staticDomain", GlobalConfig.getStaticDomain());
		context.setAttribute("payDomain", GlobalConfig.getPayDomain());
		context.setAttribute("secureDomain", GlobalConfig.getSecureDomain());

		accessAll = GlobalConfig.isAccessAll();
		String security = filterConfig.getInitParameter("securityFilter");
		if(null!=security){
			securityFilter = Boolean.valueOf(security);
		}
		if(securityFilter){
			String notNeedLoginUrl = filterConfig.getInitParameter(SecurityConstants.NOT_NEED_LOGIN_URLS);
			notNeedLoginUrls = new ArrayList<String>(8);
			if (notNeedLoginUrl != null && notNeedLoginUrl.length() > 0) {
				if (notNeedLoginUrl.indexOf(",") != -1) {
					notNeedLoginUrls.addAll(Arrays.asList(notNeedLoginUrl.split(",")));
				} else {
					notNeedLoginUrls.addAll(Arrays.asList(notNeedLoginUrl.split(" ")));
				}
			}
			//default login in, login out
			notNeedLoginUrls.add(GlobalConfig.getLoginUrl());
			notNeedLoginUrls.add(GlobalConfig.getLoginUrl() + "/out");
			
			//延迟30秒获取全部权限
			timer1.schedule(new RefreshAllPermissionMenuTask(), ONE_SECOND * 30);
		}
	}

	public static void main(String[] args) {
		System.out.println("/ddss/edit/1".matches(".*/edit/[0-9]+$"));
	}
	
	final class RefreshAllPermissionMenuTask extends TimerTask {
		@Override
		public void run() {
			if(FrameworkFactory.getAuthorizationManager()!=null){
				try {
					List<Menu> allMenus = FrameworkFactory.getAuthorizationManager().getAllMenuByContext(ProjectConfig.PROJECT_NAME);
					if (allMenus == null) {
						hasPermissionUrl = false;
						allPermissionUrls.clear();
					} else {
						hasPermissionUrl = true;
						for (Menu menu:allMenus) {
							addPermissionUrl(menu,allPermissionUrls);
						}
					}
					LOGGER.debug("Update application " + ProjectConfig.PROJECT_NAME + "'s permission finished");
				} catch (RemoteAccessException e) {
					LOGGER.error("Update application " + ProjectConfig.PROJECT_NAME + "'s permission failed,retry after 1 minutes:" + e.getMessage());
					try {
						Thread.sleep(ONE_MINUTE_IN_MILLIS);
						this.run();
					} catch (InterruptedException e2) {
						LOGGER.error("run",e2);
					}
				}
			}else{
				//延迟30秒获取全部权限
				timer1.schedule(new RefreshAllPermissionMenuTask(), ONE_SECOND * 30);
			}
		}
	}

	private static void addPermissionUrl(Menu menu, List<String> list) {
		if(menu.isMenu()&&StringUtils.isNotBlank(menu.getUrl())){
			String pageUrl = menu.getUrl();
			list.add(pageUrl);
			if(pageUrl.indexOf("/")!=-1){
				String action = pageUrl.substring(pageUrl.lastIndexOf("/"));
				if(menu.getMenuLinks().size()>0){
					for(MenuLink menuLink : menu.getMenuLinks()){
						if(menuLink.getMenuLink()!=null){
							if(!menuLink.getMenuLink().startsWith("/")){
								list.add(pageUrl.replace(action, "/" + menuLink.getMenuLink()));
							}else{
								list.add(pageUrl.replace(menu.getUrl(), menuLink.getMenuLink()));
							}
						}
					}
				}
			}
		}
	}

	private void doError(HttpServletRequest request, HttpServletResponse response, Exception exception) {
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			String message = null;
			if(exception!=null){
				message = exception.getMessage();
			}else{
				message = "Unknow Problem";
			}
			String requestType = request.getParameter(REQUEST_AJAX);
			if (StringUtils.isNotBlank(requestType) && AJAX_TYPE_COMMON.equals(requestType)) {
				AjaxUtils.doAjaxResponse(response,"{\"status\":500,\"message\":\"" + message + "\"}");
			} else {
				response.sendRedirect( request.getContextPath() + "/500");
			}
		} catch (Exception e) {
			LOGGER.error("doError", e);
		} finally {
			LOGGER.error(request.getRequestURL(), exception);
			if (writer != null) {
				writer.flush();
				writer.close();
			}
		}
	}
}
