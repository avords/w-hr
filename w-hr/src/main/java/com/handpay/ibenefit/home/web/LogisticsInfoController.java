package com.handpay.ibenefit.home.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.apache.commons.lang3.StringUtils;
import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.order.entity.OrderLogisticsInfo;
import com.handpay.ibenefit.order.service.IOrderLogisticsInfoManager;



/**
 * 物流信息查询
 * @author zhliu
 * @date 2015年7月10日
 * @parm
 */
@Controller
@RequestMapping("logisticsInfo")
public class LogisticsInfoController {
	
	
	@Reference(version="1.0")
	private IOrderLogisticsInfoManager orderLogisticsInfoManager;
	
	
	/**
	 * 物流信息查询
	 * @author zhliu
	 * @date 2015年7月10日
	 * @parm
	 * @param model
	 * @param companyCode ：物流公司编号
	 * @param logisticsNo ： 物流单号
	 * @return
	 */
	@RequestMapping("getLogisticsInfo")
	public String getLogisticsInfo(Model model,String companyCode,String logisticsNo){
		List<OrderLogisticsInfo>  OrderLogisticsInfoList = new ArrayList<OrderLogisticsInfo>();
		
		Map<String,Object> parmMap = new HashMap<String,Object>();
		parmMap.put("logisticsNo", logisticsNo);
		parmMap.put("companyCode", companyCode);
		try {
			if(StringUtils.isNotBlank(logisticsNo) && StringUtils.isNotBlank(companyCode)){
				OrderLogisticsInfoList = orderLogisticsInfoManager.queryOrderLogisticsInfo(parmMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		model.addAttribute("LogisticsInfoes", OrderLogisticsInfoList);
		return "jsonView";
	}
	
	
	
	
	

}
