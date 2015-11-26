/**
 * @Title: StimulateController.java
 * @Package com.handpay.ibenefit.stimulate.web
 * @Description: TODO
 * Copyright: Copyright (c) 2011 
 * 
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:53:14
 * @version V1.0
 */

package com.handpay.ibenefit.stimulate.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @ClassName: StimulateController
 * @Description: TODO
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:53:14
 *
 */
@Controller
@RequestMapping("stimulate")
public class StimulateController {
	
	@RequestMapping("index")
	public String index(HttpServletRequest request,HttpServletResponse response){
		return "stimulate/index";
	}
}
