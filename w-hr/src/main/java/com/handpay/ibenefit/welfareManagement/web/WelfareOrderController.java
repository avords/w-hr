package com.handpay.ibenefit.welfareManagement.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.framework.service.Manager;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageSearchInit;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.framework.web.PageController;
import com.handpay.ibenefit.order.entity.SubOrder;
import com.handpay.ibenefit.order.service.IOrderManager;
import com.handpay.ibenefit.order.service.ISubOrderManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;


/**
 * 福利订单列表
 * @author zhliu
 * @date 2015年6月17日
 * @parm
 */
@Controller
@RequestMapping("welfareOrder")
public class WelfareOrderController extends PageController<SubOrder> {

	
	private static final String HOME_DIR = "welfareorder";
	
	private static final Logger LOGGER = Logger.getLogger(WelfareOrderController.class);
	
	@Reference(version="1.0")
	private ISubOrderManager subOrderManager;
	@Reference(version="1.0")
	private IOrderManager orderManager;
	@Reference(version="1.0")
	private IUserManager userManager;
	
	
	/**
	 * 福利订单
	 * @author zhliu
	 * @date 2015年6月17日
	 * @parm
	 * @return
	 */
	@RequestMapping("")
	public String index(HttpServletRequest request,Integer backPage){
		LOGGER.info("welfareOrder index 开始");
		PageSearch page  = preparePage(request);
		PageSearchInit.initcurrentPage(page, request);
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		String isAll = "0";//1：是；0：否
		
		boolean isCompanyAdmin = UserUtils.isCompanyAdmin();
		try {
			
			if(!isCompanyAdmin){//非企业管理员
				page.getFilters().add(new PropertyFilter("SubOrder", "INS_userId", String.valueOf(user.getObjectId())));
			}else{
				
				
				String hresStr = String.valueOf(user.getObjectId());//企业管理员 下所有的HR（多个用,分割）
				User sample = new User();
				sample.setCompanyId(user.getCompanyId());
				sample.setType(IBSConstants.USER_TYPE_COMPANY_HR);
				List<User> hres = userManager.getBySample(sample);
				
				for (User userTemp : hres) {
					hresStr+=","+String.valueOf(userTemp.getObjectId());
				}
				LOGGER.info(hresStr);
				hres.add(user);
				request.setAttribute("allHres", hres);
				
				
				if(page.getFilterValue("INS_userId") !=null && !page.getFilterValue("INS_userId").toString().contains(",")){//选择一个hr用户进行查询
					isAll = "0";
					page.getFilters().add(new PropertyFilter("SubOrder", "INS_userId", page.getFilterValue("INS_userId").toString()));
					page.getFilters().add(new PropertyFilter("SubOrder", "EQL_companyId", String.valueOf(user.getCompanyId())));
				}else{
					isAll = "1";//查询所有的
					page.getFilters().add(new PropertyFilter("SubOrder", "INS_userId", hresStr));
					page.getFilters().add(new PropertyFilter("SubOrder", "EQL_companyId", String.valueOf(user.getCompanyId())));
				}
			}
			
			page.getFilters().add(new PropertyFilter("SubOrder", "EQI_deleted", String.valueOf(IBSConstants.NOT_DELETE)));
			page.setSortProperty("bookingDate");
			page.setSortOrder("desc");
			
			page = subOrderManager.findWelfareOrdder(page);
			
			
			request.setAttribute("isAll", isAll);
			request.setAttribute("pageActivity", page);
			request.setAttribute("isCompanyAdmin", isCompanyAdmin);
		} catch (Exception e) {
			LOGGER.error("index", e);
		}
		
		return HOME_DIR+"/welfareOrderIndex";
	}
	
	
	
	
	
	
	
	/**
	 * 取消订单
	 * @author zhliu
	 * @date 2015年6月17日
	 * @parm
	 * @return
	 */
	@RequestMapping(value="cancelSubOrder",method=RequestMethod.POST)
	public String cancelSubOrder(HttpServletRequest request, ModelMap modelMap){
		
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		String result = "failure";
		String subOrderId = request.getParameter("subOrderId");
		try {
			if(StringUtils.isNotBlank(subOrderId)){
				result = orderManager.updateOrderStatus(Long.valueOf(subOrderId),user.getObjectId(),IBSConstants.ORDER_CANCEL);
			}
		} catch (Exception e) {
			LOGGER.error("cancelSubOrder", e);
		}
		modelMap.addAttribute("result", result);
		return "jsonView";
	}

	
	/**
	 * 删除订单
	 * @author zhliu
	 * @date 2015年6月17日
	 * @parm
	 * @return
	 */
	@RequestMapping(value="deleteSubOrder",method=RequestMethod.POST)
	public String deleteSubOrder(HttpServletRequest request, ModelMap modelMap){
		
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		String code = "1";//0：成功；1：失败
		String subOrderId = request.getParameter("subOrderId");
		try {
			
			if(StringUtils.isNotBlank(subOrderId)){
				SubOrder subOrder = subOrderManager.getByObjectId(Long.valueOf(subOrderId));
				subOrder.setUserId(user.getObjectId());
				subOrder.setDeleted(Integer.valueOf(IBSConstants.DELETE));
				subOrderManager.save(subOrder);
				code = "0";
			}else{
				code = "1";
			}
			
			
		} catch (Exception e) {
			LOGGER.error("deleteSubOrder", e);
			code = "1";
		}
		modelMap.addAttribute("code", code);
		return "jsonView";
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	





	@Override
	public Manager<SubOrder> getEntityManager() {
		return null;
	}

	@Override
	public String getFileBasePath() {
		return null;
	}
	
	
}
