package com.handpay.ibenefit.buywelfare.web;

import java.util.ArrayList;
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
import com.handpay.ibenefit.ProductConstants;
import com.handpay.ibenefit.base.area.entity.Area;
import com.handpay.ibenefit.base.area.service.IAreaManager;
import com.handpay.ibenefit.framework.config.GlobalConfig;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.order.entity.GoodsReceiptAddr;
import com.handpay.ibenefit.order.entity.Order;
import com.handpay.ibenefit.order.entity.RequestBookingOrder;
import com.handpay.ibenefit.order.entity.SubOrder;
import com.handpay.ibenefit.order.service.IGoodsReceiptAddrManager;
import com.handpay.ibenefit.order.service.IOrderManager;
import com.handpay.ibenefit.order.service.ISubOrderManager;
import com.handpay.ibenefit.product.entity.ProductPublish;
import com.handpay.ibenefit.product.entity.SkuPublish;
import com.handpay.ibenefit.product.service.IProductPublishManager;
import com.handpay.ibenefit.product.service.ISkuPublishManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.welfare.entity.WelfarePackage;
import com.handpay.ibenefit.welfare.service.IWelfarePackageManager;


/**
 * 购买福利
 * @author zhliu
 * @date 2015年6月19日
 * @parm
 */
@Controller
@RequestMapping("buyWelfare")
public class BuyWelfareController {
	private static final String HOME_DIR = "buywelfare";
	Logger logger = Logger.getLogger(BuyWelfareController.class);

	@Reference(version="1.0")
	private IGoodsReceiptAddrManager goodsReceiptAddrManager;
	@Reference(version="1.0")
	private IWelfarePackageManager welfarePackageManager;
	@Reference(version="1.0")
	private IOrderManager orderManager;
	@Reference(version="1.0")
	private ISubOrderManager subOrderManager;
	@Reference(version="1.0")
	private IStaffManager staffManager;
	@Reference(version="1.0")
	private ISkuPublishManager skuPublishManager;
	@Reference(version="1.0")
	private IProductPublishManager publishManager;
	@Reference(version="1.0")
	private IDictionaryManager dictionaryManager;
	@Reference(version="1.0")
	private IAreaManager areaManager;
	@Reference(version="1.0")
	private IUserManager userManager;





	/**
	 * 购买福利-填写收货信息
	 * @author zhliu
	 * @date 2015年6月19日
	 * @parm
	 * @param request
	 * @param response
	 * @param packageId : 套餐ID
	 * @param count
	 * @return
	 */

    @RequestMapping("buyWelfareIndex")
    public String buyWelfareIndex(HttpServletRequest request,HttpServletResponse response,Long pakageId,Long count){


    	int stockType = 1;//实体兑换券 需要填写 收货地址(1：实体兑换券2：电子兑换券)

    	try {
    		WelfarePackage welfarePackage = new WelfarePackage();
    		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);

    		List<GoodsReceiptAddr> receiptAddrList = goodsReceiptAddrManager.getAddrByUserId(user.getObjectId());
    		welfarePackage =welfarePackageManager.getPackagePrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), pakageId);

    		if(welfarePackage.getStockType() == IBSConstants.WELFARE_PACKAGE_WP_STOCK_TYPE_ELECTRON){
    			stockType = 2 ;
    		}else{
    			stockType = 1 ;
    		}

    		//初始化支付方式
    		List<Dictionary> payWays = dictionaryManager.getDictionariesByDictionaryId(1403);
            request.setAttribute("payWays", payWays);

            //初始化省份信息
            List<Area> provinces = areaManager.getRoot();
            request.setAttribute("provinces", provinces);

    		request.setAttribute("receiptAddrList", receiptAddrList);
    		request.setAttribute("welfarePackage", welfarePackage);
    		request.setAttribute("count", count);
    		request.setAttribute("stockType", stockType);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
    	return HOME_DIR+"/buyWelfareIndex";
    }



    /**
	 * 购买福利-生成订单
	 * @author zhliu
	 * @date 2015年6月19日
	 * @parm
	 * @param request
	 * @param response
	 * @param packageId : 套餐ID
	 * @param count
	 * @return
	 */

    @RequestMapping("/buyWelfareOrder")
    public String buyWelfareOrder(HttpServletRequest request,RequestBookingOrder bookingOrder){


    	Order order = new Order();

    	try {
    		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
    		bookingOrder.setUserId(user.getObjectId());
    		bookingOrder.setOrderType(IBSConstants.ORDER_TYPE_POINT_BUY);//积分购买
    		bookingOrder.setOrderSource(IBSConstants.ORDER_SOURCE_HR);//企业HR端
//    		bookingOrder.setOrderProductType(IBSConstants.ORDER_PRODUCT_TYPE_WELFARE);//订单商品类型  1福利套餐，2体检套餐，3SKU商品，4生活服务
    		bookingOrder.setUserId(user.getObjectId());

    		logger.info("HR端购买福利 --购买福利---开始");
    		order = orderManager.createImmediateOrder(bookingOrder);
    		logger.info("HR端购买福利 --购买福利---结束");
		} catch (Exception e) {
			logger.error("HR端购买福利 出错了，抛异常信息...",e);
			if(e.getMessage().equals("商品库存不足")){
				request.setAttribute("msg", e.getMessage());
			}else{
				request.setAttribute("msg", "预定失败");
			}
			return HOME_DIR+"/bookOrderFail";
		}


    	return "redirect:"+"buyWelfareOrderSuc/"+order.getObjectId();
    }







	/**
	 * 购买商品-填写收货信息
	 * @author zhliu
	 * @date 2015年6月19日
	 * @parm
	 * @param request
	 * @param response
	 * @param prodId : 商品ID
	 * @param count
	 * @return
	 */

    @RequestMapping("/buyProdIndex")
    public String buyProdIndex(HttpServletRequest request,HttpServletResponse response,Long skuId,Long count){

    	List<GoodsReceiptAddr> receiptAddrList = new ArrayList<GoodsReceiptAddr>();//收货地址
    	User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
    	SkuPublish skuPublish = new SkuPublish();
    	ProductPublish productPublish = new ProductPublish();
    	int stockType = 1;//实体兑换券 需要填写 收货地址(1：实体兑换券2：电子兑换券)
    	try {
    		receiptAddrList = goodsReceiptAddrManager.getAddrByUserId(user.getObjectId());

    		//查询商品详细
    		skuPublish = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), skuId);
    		productPublish =  publishManager.getByObjectId(skuPublish.getProductId());

    		if(skuPublish.getType() == ProductConstants.PRODUCT_TYPE_MATERIAL_OBJECT||skuPublish.getType()==ProductConstants.PRODUCT_TYPE_MATERIAL_CARD){
				stockType = 1;
			}else{
				stockType = 2;
			}
    		//初始化支付方式
    		List<Dictionary> payWays = dictionaryManager.getDictionariesByDictionaryId(1403);
            request.setAttribute("payWays", payWays);
            //初始化省份信息
            List<Area> provinces = areaManager.getRoot();
            request.setAttribute("provinces", provinces);


            request.setAttribute("receiptAddrList", receiptAddrList);
    		request.setAttribute("count", count);
    		request.setAttribute("skuPublish", skuPublish);
    		request.setAttribute("productPublish", productPublish);
        	request.setAttribute("stockType", stockType);

		} catch (Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}



    	return HOME_DIR+"/buyProdIndex";
    }



    /**
  	 * 购买商品-生成订单
  	 * @author zhliu
  	 * @date 2015年6月19日
  	 * @parm
  	 * @param request
  	 * @param response
  	 * @param packageId : 套餐ID
  	 * @param count
  	 * @return
  	 */

      @RequestMapping("/buyProdOrder")
      public String buyProdOrder(HttpServletRequest request,RequestBookingOrder bookingOrder){


      	Order order = new Order();

      	try {
      		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);


      		bookingOrder.setUserId(user.getObjectId());
      		bookingOrder.setOrderType(IBSConstants.ORDER_TYPE_POINT_BUY);//年度福利
      		bookingOrder.setOrderSource(IBSConstants.ORDER_SOURCE_HR);//企业HR端
      		bookingOrder.setOrderProductType(IBSConstants.ORDER_PRODUCT_TYPE_WELFARE);//订单商品类型  1福利套餐，2体检套餐，3SKU商品，4生活服务
      		bookingOrder.setUserId(user.getObjectId());


      		logger.info("HR端购买福利 --购买商品---开始");
      		order = orderManager.createImmediateOrder(bookingOrder);
      		logger.info("HR端购买福利 --购买商品---结束");
  		} catch (Exception e) {
  			e.printStackTrace();
  			logger.error("HR端购买福利 --购买商品---失败");
  			logger.error(e.getMessage());
  		}


      	return "redirect:"+"buyWelfareOrderSuc/"+order.getObjectId();
      }

    /**
     * 购买福利-生成订单
     * @author zhliu
     * @date 2015年7月3日
     * @parm
     * @param request
     * @param response
     * @param objectId : 总订单ID
     * @return
     */

    @RequestMapping("/buyWelfareOrderSuc/{objectId}")
    public String buyWelfareOrderSuc(HttpServletRequest request,@PathVariable Long objectId){
    	try {
    		List<SubOrder> subOrderList = null;
    		SubOrder subOrder = new SubOrder();
    		subOrder.setGeneralOrderId(objectId);
    		subOrderList =  subOrderManager.getBySample(subOrder);
    		subOrder = subOrderList.get(0);

    		return "redirect:"+GlobalConfig.getPayDomain()+"/onlinePay/orderPayCenter?subOrderIds="+subOrder.getObjectId();

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}

    	return HOME_DIR+"/buyWelfareOrder";
    }

    /**
     * 购买福利-生成订单 -- 根据子订单 ID 进行支付
     * @author zhliu
     * @date 2015年7月3日
     * @parm
     * @param request
     * @param response
     * @param objectId : 子订单ID
     * @return
     */

    @RequestMapping("/buyWelfareSubOrderSuc/{objectId}")
    public String buyWelfareSubOrderSuc(HttpServletRequest request,@PathVariable Long objectId){

    	User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
    	SubOrder subOrder = new SubOrder();
		double totalAmount = 0.0;//应付总额
		Double sumPoint = 0.0;//员工 积分
		Order order = new Order();//总订单
    	try {
    		//查询剩余积分
    		sumPoint = userManager.getUserBalance(user.getObjectId());
    		subOrder =  subOrderManager.getByObjectId(objectId);
    		order = orderManager.getByObjectId(subOrder.getGeneralOrderId());
    		totalAmount = subOrder.getPayableAmount();


    		request.setAttribute("sumPoint", sumPoint);
			request.setAttribute("subOrder", subOrder);
			request.setAttribute("totalAmount", totalAmount);


			if(order.getPaymentWay() !=null && order.getPaymentWay() == 2){
				return HOME_DIR+"/buyWelfareOrder";
			}else{
				return HOME_DIR+"/buyWelfareOrderPayOffline";
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}

    	return HOME_DIR+"/buyWelfareOrder";
    }

    /**
     * 支付宝支付
     * @author zhliu
     * @date 2015年6月30日
     * @param request
     * @param subOrderId : 子订单ID
     * @param subOrderNo:子订单号
     * @param payWay :线上支付渠道（1：积分支付；2支付宝支付；3：混合支付）
     * @return
     */
    @RequestMapping("onlinePaySetPayment")
    public String onlinePaySetPayment(HttpServletRequest request,String subOrderId,String subOrderNo,String payWay){

    	try {

    		//更新 订单 实付金额
			SubOrder subOrder = new SubOrder();
			subOrder.setSubOrderNo(subOrderNo);
			subOrder = subOrderManager.getBySample(subOrder).get(0);

    		if(payWay!=null &&  payWay.equals(String.valueOf(IBSConstants.PAY_WAY_ON_LINE_INTEGRAL))){//积分支付
    			subOrder.setActuallyAmount(0.0);
    			subOrder.setActuallyIntegral(subOrder.getPayableAmount());
    		}else if(payWay!=null &&  payWay.equals(String.valueOf(IBSConstants.PAY_WAY_ON_LINE_ALIPAY))){//支付宝支付
    			subOrder.setActuallyAmount(subOrder.getPayableAmount());
    			subOrder.setActuallyIntegral(0.0);
    		}else{
    			return null;
    		}

    		subOrderManager.save(subOrder);

    		return "redirect:/onlinePay/aliPay/"+subOrderId;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
    	return null;
    }

    /**
     * 积分支付
     * @author zhliu
     * @date 2015年6月30日
     * @param request
     * @param response
     * @param subOrderNo:子订单号
     * @param payWay :线上支付渠道（1：积分支付；2支付宝支付；3：混合支付）
     * @param password :支付密码
     * @return
     */
    @RequestMapping("integralPaySetPayment")
    public String integralPaySetPayment(HttpServletRequest request,String subOrderId,String subOrderNo,String payWay,String password,Long orderId){

    	try {
    		User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);


    		//更新 订单 实付金额
			SubOrder subOrder = new SubOrder();
			subOrder.setSubOrderNo(subOrderNo);
			subOrder = subOrderManager.getBySample(subOrder).get(0);

    		if(payWay!=null &&  payWay.equals(String.valueOf(IBSConstants.PAY_WAY_ON_LINE_INTEGRAL))){//积分支付
    			subOrder.setActuallyAmount(0.0);
    			subOrder.setActuallyIntegral(subOrder.getPayableAmount());
    		}else if(payWay!=null &&  payWay.equals(String.valueOf(IBSConstants.PAY_WAY_ON_LINE_ALIPAY))){//支付宝支付
    			subOrder.setActuallyAmount(subOrder.getPayableAmount());
    			subOrder.setActuallyIntegral(0.0);
    		}else{
    			return null;
    		}
    		subOrderManager.save(subOrder);

    		//积分支付
    		Map resultMap = orderManager.integralPay(subOrderId, user.getObjectId(), password);


    		request.setAttribute("message", resultMap.get("message"));//支付信息
    		/*if(orderId == null){//子订单ID
    			return "redirect:"+"buyWelfareSubOrderSuc/"+subOrderId;
    		}else{//总订单ID
    			return "redirect:"+"buyWelfareOrderSuc/"+orderId;
    		}*/

    		return "redirect:"+"buyWelfareSubOrderSuc/"+subOrderId;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}

    	return  null;
    }
}
