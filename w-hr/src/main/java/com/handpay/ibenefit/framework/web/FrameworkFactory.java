package com.handpay.ibenefit.framework.web;

import com.handpay.ibenefit.framework.cache.ICacheManager;
import com.handpay.ibenefit.security.service.IAuthorizationManager;

/**
 * Framework factory
 * @author pubx
 *
 */
public class FrameworkFactory {
	
	private static IAuthorizationManager authorizationManager;
	
	private static ICacheManager cacheManager;
	
	public static IAuthorizationManager getAuthorizationManager() {
		return authorizationManager;
	}
	
	public static ICacheManager getCacheManager() {
		return cacheManager;
	}
	public void setAuthorizationManager(
			IAuthorizationManager authorizationManager) {
		FrameworkFactory.authorizationManager = authorizationManager;
	}
	public void setCacheManager(ICacheManager cacheManager) {
		FrameworkFactory.cacheManager = cacheManager;
	}
	
	
}
