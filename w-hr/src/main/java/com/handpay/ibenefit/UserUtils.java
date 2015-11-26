package com.handpay.ibenefit;

import com.handpay.ibenefit.framework.util.FrameworkContextUtils;

/**
 * 用户类型判断工具类
 * @author bob.pu
 *
 */
public final class UserUtils {
	private UserUtils(){
	}
	
	public static boolean isCompanyHR(){
		return FrameworkContextUtils.getCurrentUser().getType()==IBSConstants.USER_TYPE_COMPANY_HR;
	}
	
	public static boolean isCompanyAdmin(){
		return FrameworkContextUtils.getCurrentUser().getType()==IBSConstants.USER_TYPE_COMPANY_ADMIN;
	}
}
