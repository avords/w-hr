/**
 * @Title: StatisticsController.java
 * @Package com.handpay.ibenefit.statistics.web
 * @Description: TODO
 * Copyright: Copyright (c) 2011 
 * 
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:56:47
 * @version V1.0
 */

package com.handpay.ibenefit.statistics.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @ClassName: StatisticsController
 * @Description: TODO
 * @author Comsys-Mac.Yoon
 * @date 2015-6-8 下午8:56:47
 *
 */
@Controller
@RequestMapping("statistics")
public class StatisticsController {
	
	@RequestMapping("index")
	public String index(HttpServletRequest request,HttpServletResponse response){
		return "statistics/index";
	}
}
