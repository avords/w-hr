/**
 * @Title: WelfareController.java
 * @Package com.handpay.ibenefit.welfare.web
 * @Description: TODO
 * Copyright: Copyright (c) 2011 
 * 
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:52:46
 * @version V1.0
 */

package com.handpay.ibenefit.welfare.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @ClassName: WelfareController
 * @Description: TODO
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:52:46
 *
 */
@Controller
@RequestMapping("welfare")
public class WelfareController {

	@RequestMapping("index")
	public String index(HttpServletRequest request,HttpServletResponse response){
		return "welfare/index";
	}
}
