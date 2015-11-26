/**
 * @Title: BaseController.java
 * @Package com.handpay.ibenefit
 * @Description: TODO
 * Copyright: Copyright (c) 2011 
 * 
 * @author Comsys-Mac.Yoon
 * @date 2015-6-15 下午4:02:15
 * @version V1.0
 */

package com.handpay.ibenefit;

import java.util.Date;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import com.handpay.ibenefit.framework.web.JdfCustomDateEditor;

/**
 * @ClassName: BaseController
 * @Description: Controller基类，提供一些工具方法
 * @author Comsys-Mac.Yoon
 * @date 2015-6-15 下午4:02:15
 *
 */

public class BaseController {
	
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		// CustomDateEditor dateEditor = new CustomDateEditor(new
		// SimpleDateFormat("yyyy-MM-dd"), true);
		// Date transformer
		binder.registerCustomEditor(Date.class, new JdfCustomDateEditor(true));
	}
	
}
