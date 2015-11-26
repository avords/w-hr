package com.handpay.ibenefit.welfareManagement.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.common.service.alipay.IAlipayService;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.member.entity.SupplierTypes;
import com.handpay.ibenefit.member.service.ICompanyManager;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.member.service.ISupplierTypesManager;
import com.handpay.ibenefit.order.entity.Order;
import com.handpay.ibenefit.order.entity.SubOrder;
import com.handpay.ibenefit.order.service.IOrderManager;
import com.handpay.ibenefit.order.service.ISubOrderManager;
import com.handpay.ibenefit.order.vo.LifeServiceBookingOrder;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;

/**
 * hr端-福利管理-购买积分
 * @author zhliu
 * @date 2015年6月12日
 * @parm
 */
@Controller
@RequestMapping("points")
public class PointsController {
	private static final String HOME_DIR = "points";
	
	Logger logger = Logger.getLogger(PointsController.class);
	
	@Reference(version="1.0")
	private IStaffManager staffManager;
	@Reference(version="1.0")
	private IAlipayService alipayService;
	@Reference(version="1.0")
	private IOrderManager orderManager;
	@Reference(version="1.0")
	private ISubOrderManager subOrderManager;
	@Reference(version="1.0")
	private ICompanyManager companyManager;
	@Reference(version="1.0")
	private IUserManager userManager;
	@Reference(version="1.0")
	private ISupplierTypesManager supplierTypesManager;
	@Reference(version="1.0")
	private IDictionaryManager dictionaryManager;
	
	/**
	 * 购买积分
	 * @author zhliu
	 * @date 2015年6月12日
	 * @parm
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("")
	public String index(HttpServletRequest request,HttpServletResponse response){
		try {
			Double sumPoint = 0.0;//员工 积分
			User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			
			sumPoint = userManager.getUserBalance(user.getObjectId());
			SubOrder subOrder = new SubOrder();
			//查询 订单信息 -- 发票信息
			if(user.getObjectId()!=null){
				Map<String,Object> paramMap = new HashMap<String,Object>();
				paramMap.put("userId", user.getObjectId());
				List<SubOrder> subOrders = subOrderManager.selectInvoiceInfo(paramMap);
				if(subOrders.size()>0){
					subOrder = subOrders.get(0);
				}
			}
			
			
			request.setAttribute("sumPoint", sumPoint);
			request.setAttribute("user", user);
			request.setAttribute("subOrderTemp", subOrder);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("points");
			logger.error(e.getMessage());
		}
		return HOME_DIR+"/pointsIndex";
	}
	
	
	/**
	 * 确认积分购买信息
	 * @author zhliu
	 * @date 2015年6月12日
	 * @parm
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("confirmPointsMsg")
	public String confirmPointsMsg(HttpServletRequest request,LifeServiceBookingOrder lifeServiceBookingOrder){
		try {
			request.setAttribute("bookingOrder", lifeServiceBookingOrder);
			
    		//初始化支付方式
    		List<Dictionary> payWays = dictionaryManager.getDictionariesByDictionaryId(1403);
            request.setAttribute("payWays", payWays);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return HOME_DIR+"/pointsOrder";
	}
	
	
	
	/**
	 * 生成积分购买 -- 订单
	 * @author zhliu
	 * @date 2015年6月12日
	 * @parm
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("createPointsOrder")
	public String createPointsOrder(HttpServletRequest request,HttpServletResponse response,LifeServiceBookingOrder lifeServiceBookingOrder){
		
		Order order = new Order();
		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		
		//查询生活服务类 供应商
		SupplierTypes supplierTypes = new SupplierTypes();
		supplierTypes.setTypeId(IBSConstants.SUPPLIER_TYPES_LIFE_SERVICE);
		
		
		try {
			
			//查询生活服务类 供应商
			List<SupplierTypes> supplierTypeses = supplierTypesManager.getBySample(supplierTypes);
			
			
			lifeServiceBookingOrder.setUserId(user.getObjectId());
			lifeServiceBookingOrder.setName("积分");
			lifeServiceBookingOrder.setProductType(IBSConstants.PRODUCT_TYPE_LIFE_SERVICE);
			lifeServiceBookingOrder.setProductPrice(1.0);
			lifeServiceBookingOrder.setOrderSource(IBSConstants.ORDER_SOURCE_HR);
			lifeServiceBookingOrder.setOrderType(IBSConstants.ORDER_TYPE_BUY_POINT);
			lifeServiceBookingOrder.setIfInvoice(1);		//是否开发票
			
			
			lifeServiceBookingOrder.setInvoiceAmount(Double.valueOf(String.valueOf(lifeServiceBookingOrder.getCount())));//付费金额
			if(supplierTypeses != null && supplierTypeses.size()>0){
				lifeServiceBookingOrder.setSupplierId(supplierTypeses.get(0).getSupplierId());
			}
			
			
			order = orderManager.createLifeServiceOrder(lifeServiceBookingOrder);//总订单
			
			return "redirect:"+"createPointsOrderSuc/"+order.getObjectId();
			
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		
		return null;
	}
	
	
	
	
	
	
	/**
	 * 生成订单信息
	 * @author zhliu
	 * @date 2015年6月29日
	 * @parm
	 * @param request
	 * @param objectId
	 * @return
	 */
	@RequestMapping("createPointsOrderSuc/{objectId}")
	public String createPointsOrderSuc(HttpServletRequest request,@PathVariable Long objectId){
		
		List<SubOrder> subOrderList = new ArrayList<SubOrder>();
		Order order = new Order();
		Double payCount = 0.0;//支付金额
		try {
			
			User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			
			
			order = orderManager.getByObjectId(objectId);
			
			//查询子订单信息
			SubOrder subOrder = new SubOrder();
			subOrder.setGeneralOrderId(order.getObjectId());
			subOrderList = subOrderManager.getBySample(subOrder);
			
			for (SubOrder subOrderTemp : subOrderList) {
				payCount += subOrderTemp.getPayableAmount();
			}
			
			
			
			request.setAttribute("orderModel", order);
			request.setAttribute("subOrder", subOrderList.get(0));
			request.setAttribute("payCount", payCount);
			request.setAttribute("user", user);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		
		return HOME_DIR+"/createPointOrderSuc";
	}
	
	
	
	
	
	/**
	 * 根据子订单号 支付(单个子订单支付)
	 * @author zhliu
	 * @date 2015年6月30日
	 * @parm
	 * @param response
	 * @param request
	 * @param subOrderId:子订单Id
	 * @return
	 */
	@RequestMapping("/onlinePaySetPayment")
	public String onlinePaySetPayment(HttpServletResponse response,HttpServletRequest request,Long subOrderId){
		
		
		try {
			//设置 订单 实付金额
			SubOrder subOrder = new SubOrder();
			subOrder = subOrderManager.getByObjectId(subOrderId);
			subOrder.setActuallyAmount(subOrder.getPayableAmount());
			subOrder.setOnlinePayChannels(IBSConstants.ONLINE_PAY_ALIPAY);
			
			subOrderManager.save(subOrder);
			
			
			return "redirect:/onlinePay/aliPay/"+subOrderId;
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		return null;
	}
	
	

	
	
	
	
}
