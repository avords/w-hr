package com.handpay.ibenefit.onlinepay.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.common.service.alipay.IAlipayService;
import com.handpay.ibenefit.common.vo.AlipayEntity;
import com.handpay.ibenefit.framework.util.AppUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.order.entity.Order;
import com.handpay.ibenefit.order.entity.OrderSku;
import com.handpay.ibenefit.order.entity.SubOrder;
import com.handpay.ibenefit.order.service.IOrderManager;
import com.handpay.ibenefit.order.service.IOrderSkuManager;
import com.handpay.ibenefit.order.service.ISubOrderManager;
import com.handpay.ibenefit.plan.entity.WelfarePlan;
import com.handpay.ibenefit.plan.entity.WelfarePlanOrder;
import com.handpay.ibenefit.plan.entity.WelfarePlanStaff;
import com.handpay.ibenefit.plan.entity.WelfareSubPlan;
import com.handpay.ibenefit.plan.service.IWelfarePlanManager;
import com.handpay.ibenefit.plan.service.IWelfarePlanOrderManager;
import com.handpay.ibenefit.plan.service.IWelfarePlanStaffManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanItemManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanManager;
import com.handpay.ibenefit.security.entity.SystemParameter;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IPointOperateManager;
import com.handpay.ibenefit.security.service.ISystemParameterManager;
import com.handpay.ibenefit.security.service.IUserManager;



/**
 * 在线支付
 * @author zhliu
 * @date 2015年6月29日
 * @parm
 */
@Controller
@RequestMapping("onlinePay")
public class OnlinePayController {
    private static final Logger LOGGER = Logger.getLogger(OnlinePayController.class);

	private static final String HOME_DIR = "onlinePay/";

	@Reference(version = "1.0")
	private IAlipayService alipayService;
	@Reference(version = "1.0")
	private ISubOrderManager subOrderManager;
	@Reference(version = "1.0")
    private IOrderManager orderManager;
	@Reference(version = "1.0")
    private IUserManager userManager;
	@Reference(version = "1.0")
	private IOrderSkuManager orderSkuManager;
	@Reference(version = "1.0")
    private IPointOperateManager pointOperateManager;
	@Reference(version = "1.0")
    private ISystemParameterManager systemParameterManager;
	@Reference(version = "1.0")
    private IWelfareSubPlanItemManager welfareSubPlanItemManager;
	@Reference(version = "1.0")
    private IWelfareSubPlanManager welfareSubPlanManager;
	@Reference(version = "1.0")
    private IWelfarePlanManager welfarePlanManager;
	@Reference(version = "1.0")
    private IWelfarePlanOrderManager welfarePlanOrderManager;
	@Reference(version = "1.0")
    private IWelfarePlanStaffManager welfarePlanStaffManager;

	@RequestMapping("/intoPay/{subOrderIds}")
    public String intoPay(HttpServletResponse response,HttpServletRequest request,@PathVariable String subOrderIds){
        return HOME_DIR+"payFail";
    }

	/**
	 * 根据子订单号 支付(单个子订单支付)
	 * @author zhliu
	 * @date 2015年6月30日
	 * @parm
	 * @param response
	 * @param request
	 * @param subOrderId:子订单ID
	 * @return
	 */
	@RequestMapping("aliPay/{subOrderId}")
	public String onlinePay(HttpServletResponse response,HttpServletRequest request,@PathVariable String subOrderId){
		try {
			User user = (User) request.getSession().getAttribute("s_user");
			String ip = (String) request.getSession().getAttribute("ip");
			String resultXml = "";

			AlipayEntity alipayEntity = new AlipayEntity();
			List<String> subOrderIdList = new ArrayList<String>(); // 内部产生的订单ID
			subOrderIdList.add(subOrderId);
			alipayEntity.setDomain(AppUtils.getHrDynamicDomain(""));
			alipayEntity.setPaySucPage("welfareOrder");
			alipayEntity.setPayFailPage("index");

			alipayEntity.setUserId(user.getObjectId());
			alipayEntity.setSubOrderIdList(subOrderIdList);
			alipayEntity.setExterInvokeIp(ip);
			alipayEntity.setSubject("购买积分");


			resultXml = alipayService.webPay(alipayEntity);
			LOGGER.info("===即时到账支付接口--表单提交===");
			LOGGER.info(resultXml);
			LOGGER.info("===即时到账支付接口结束===");

			response.resetBuffer();
			response.setContentType("text/html; charset=UTF-8");
			response.getWriter().print(resultXml);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
     * 支付宝支付
     * @author 闫冬全
     * @date 2015年7月7日
     * @param response
     * @param request
     * @param subOrderIds:用逗号分隔的子订单ID
     * @return
     */
	@RequestMapping("/alipay/{subOrderIds}")
    public String alipay(HttpServletResponse response,HttpServletRequest request,@PathVariable String subOrderIds){
	    String[] subOrders = subOrderIds.split(",");
	    List<String> subOrderIdList = new ArrayList<String>();
        for(String idStr:subOrders){
            subOrderIdList.add(idStr);
        }
        Long subOrderId = Long.parseLong(subOrders[0]);
        OrderSku orderSku = new OrderSku();
        orderSku.setSubOrderId(subOrderId);
        orderSku = orderSkuManager.getBySample(orderSku).get(0);

	    User user = (User) request.getSession().getAttribute("s_user");
        String ip = (String) request.getSession().getAttribute("ip");
        String resultXml = "";

        AlipayEntity alipayEntity = new AlipayEntity();
        alipayEntity.setUserId(user.getObjectId());
        alipayEntity.setSubOrderIdList(subOrderIdList);
        alipayEntity.setExterInvokeIp(ip);
        alipayEntity.setSubject(orderSku.getName());
        alipayEntity.setBody(orderSku.getName());
        alipayEntity.setDomain(AppUtils.getHrDynamicDomain(""));
        alipayEntity.setPaySucPage("onlinePay/success/"+subOrderIds);
        alipayEntity.setPayFailPage("onlinePay/fail/"+subOrderIds);

        resultXml = alipayService.webPay(alipayEntity);
        LOGGER.info("===即时到账支付接口--表单提交===");
        LOGGER.info(resultXml);
        LOGGER.info("===即时到账支付接口结束===");

        response.resetBuffer();
        response.setContentType("text/html; charset=UTF-8");
        try {
            response.getWriter().print(resultXml);
        } catch (IOException e) {
            LOGGER.info("alipay response write content to page have a error!\n"+e.getMessage());
        }
        return null;
    }
	/**
     * 积分支付
     * @author 闫冬全
     * @date 2015年7月7日
     * @param response
     * @param request parameter或者Attribute里面必须给定支付密码password
     * @param subOrderIds:用逗号分隔的子订单ID
     * @return
     */
	@RequestMapping("/integralpay/{subOrderIds}")
    public String integralpay(HttpServletResponse response,HttpServletRequest request,@PathVariable String subOrderIds){
	  //积分支付
        String password = request.getParameter("password");
        if(StringUtils.isBlank(password)){
            password = (String) request.getAttribute("password");
        }
        try{
            Map<String,String> map = orderManager.integralPay(subOrderIds, FrameworkContextUtils.getCurrentUserId(), password);
            if("true".equals(map.get("code"))){
                return "forward:/onlinePay/success/"+subOrderIds;
            }else if("false".equals(map.get("code"))){
            	String msg=map.get("message");
                return "forward:/onlinePay/fail/"+subOrderIds+"?msg="+msg;
            }
        }catch(Exception e){
            LOGGER.error("integralpay have a error\n"+e);
            return "forward:/onlinePay/fail/"+subOrderIds;
        }
        return HOME_DIR+"payFail";
    }

	  /**
     * 支付成功跳转的页面
     * @author 闫冬全
     * @date 2015年7月7日
     * @param response
     * @param request
     * @param subOrderIds:用逗号分隔的子订单ID
     * @return
     */
    @RequestMapping("/success/{subOrderIds}")
    public String success(HttpServletResponse response,HttpServletRequest request,@PathVariable String subOrderIds){
        String[] subOrders = subOrderIds.split(",");
        List<SubOrder> subOrderList = new ArrayList<SubOrder>();
        double totalAmount = 0;
        for(String idStr:subOrders){
            Long orderId = Long.parseLong(idStr);
            SubOrder subOrder = subOrderManager.getByObjectId(orderId);
            subOrderList.add(subOrder);
            totalAmount += subOrder.getPayableAmount();
        }
        request.setAttribute("subOrderList", subOrderList);
        request.setAttribute("totalAmount", totalAmount);
        return HOME_DIR+"paySuccess";
    }
    /**
     * 支付失败跳转的页面
     * @author 闫冬全
     * @date 2015年7月7日
     * @param response
     * @param request
     * @param subOrderIds:用逗号分隔的子订单ID
     * @return
     */
    @RequestMapping("/fail/{subOrderIds}")
    public String fail(HttpServletResponse response,HttpServletRequest request,@PathVariable String subOrderIds){
        String[] subOrders = subOrderIds.split(",");
        List<SubOrder> subOrderList = new ArrayList<SubOrder>();
        for(String idStr:subOrders){
            Long orderId = Long.parseLong(idStr);
            SubOrder subOrder = subOrderManager.getByObjectId(orderId);
            subOrderList.add(subOrder);
        }
        Order order = orderManager.getByObjectId(subOrderList.get(0).getGeneralOrderId());
        //计算订单过时时间
        Date orderDate = order.getBookingDate();
        SystemParameter sp = systemParameterManager.getHrOrderTimeOutLimit();
        Date endDate = DateUtils.addHours(orderDate, Integer.parseInt(sp.getValue()));
        Long surplusTime = endDate.getTime()-new Date().getTime();
        request.setAttribute("surplusTime", surplusTime);
        request.setAttribute("subOrderList", subOrderList);
        request.setAttribute("msg", request.getParameter("msg"));
        return HOME_DIR+"payFail";
    }

    /**
     * 进入订单支付中心(表单中放入参数subOrderIds：用逗号隔开的orderId)
     * @author 闫冬全
     * @date 2015年7月7日
     * @param response
     * @param request 表的
     * @return
     */
    @RequestMapping("/orderPayCenter")
    public String orderPay(HttpServletRequest request, HttpServletResponse response) {
            String subOrderIds = request.getParameter("subOrderIds");
            List<SubOrder> subOrderList = new ArrayList<SubOrder>();
            User user = (User) request.getSession().getAttribute("s_user");
            double totalAmount = 0.0;// 应付总额

            boolean orderType = true;
            String[] subIdStrs = subOrderIds.split(",");
            for(String idStr:subIdStrs){
                SubOrder subOrder = subOrderManager.getByObjectId(Long.parseLong(idStr));
                if(subOrder!=null){
                	if(subOrder.getOrderType()==IBSConstants.ORDER_TYPE_BUY_POINT){
                		orderType = false;
                	}
                    totalAmount += subOrder.getPayableAmount();
                    subOrderList.add(subOrder);
                }
            }

            // 查询员工剩余积分
            Double surplusScoreDou = userManager.getUserBalance(user.getObjectId());
            double surplusScore = surplusScoreDou==null?0:surplusScoreDou;
            request.setAttribute("orderType", orderType);
            request.setAttribute("subOrderList", subOrderList);
            request.setAttribute("totalAmount", totalAmount);
            request.setAttribute("surplusScore", surplusScore);
            Order order = orderManager.getByObjectId(subOrderList.get(0).getGeneralOrderId());
            //计算订单过时时间
            Date orderDate = order.getBookingDate();
            SystemParameter sp = systemParameterManager.getHrOrderTimeOutLimit();
            Date endDate = DateUtils.addHours(orderDate, Integer.parseInt(sp.getValue()));
            Long surplusTime = endDate.getTime()-new Date().getTime();
            request.setAttribute("surplusTime", surplusTime);
            if(order.getPaymentWay()!=null&&order.getPaymentWay()==1){
                return HOME_DIR+"payOffline";
            }else{
                return HOME_DIR+"orderPayCenter";
            }
    }

    /**
    *
    * @author 闫冬全
    * @date 2015年6月30日
    * @param request
    * @param response
    * @param payWay
    *  :线上支付渠道（1：积分支付；2支付宝支付；3：混合支付）
    * @return
    */
   @RequestMapping("onlinePaySetPayment")
   public String onlinePaySetPayment(HttpServletRequest request, HttpServletResponse response,String payWay) {
       try {
           String[] subOrderIds = request.getParameterValues("subOrderId");
           for (String subOrderId : subOrderIds) {
               // 更新 订单 实付金额
               if(StringUtils.isBlank(subOrderId)){
                   continue;
               }
               SubOrder subOrder =  subOrderManager.getByObjectId(Long.parseLong(subOrderId));

               //根据传过来的子订单号可以写自己的业务逻辑。
               if (payWay != null && payWay.equals(String.valueOf(IBSConstants.PAY_WAY_ON_LINE_INTEGRAL))) {// 积分支付
                   subOrder.setOnlinePayChannels(IBSConstants.ONLINE_PAY_POINT);
                   subOrder.setActuallyAmount(0.0);
                   subOrder.setActuallyIntegral(subOrder.getPayableAmount());
               } else if (payWay != null && payWay.equals(String.valueOf(IBSConstants.PAY_WAY_ON_LINE_ALIPAY))) {// 支付宝支付
                   subOrder.setOnlinePayChannels(IBSConstants.ONLINE_PAY_ALIPAY);
                   subOrder.setActuallyAmount(subOrder.getPayableAmount());
                   subOrder.setActuallyIntegral(0.0);
               } else {
                   return null;
               }
               subOrderManager.save(subOrder);
           }

           String ids = StringUtils.join(subOrderIds,",");
           if("1".equals(payWay)){
               //积分支付
               String password = request.getParameter("password");
               request.setAttribute("password", password);
               return "forward:/onlinePay/integralpay/"+ids;
           }else if("2".equals(payWay)){
               //支付宝支付
               return "forward:/onlinePay/alipay/"+ids;
           }
       } catch (Exception e) {
           e.printStackTrace();
       }
       return null;
   }
   @RequestMapping("validatePaymentPwd")
   public String onlinePaySetPayment(HttpServletRequest request, HttpServletResponse response,ModelMap map) {
       String password = request.getParameter("password");
       Long userId = FrameworkContextUtils.getCurrentUserId();
       boolean validateResult = pointOperateManager.validPayPassword(userId,password);
       map.put("result", validateResult);
       return "jsonView";
   }

   /**
    * 进入订单支付中心(表单中放入参数subOrderIds：用逗号隔开的orderId)
    * @author 闫冬全
    * @date 2015年7月7日
    * @param response
    * @param request 表的
    * @return
    */
   @RequestMapping("/welfarePlanOrderPayCenter")
   public String welfarePlanOrderPayCenter(HttpServletRequest request, HttpServletResponse response) {
             String planIdStr = request.getParameter("planId");
             Long planId = Long.parseLong(planIdStr);
             //得到有订单的子计划
             List<WelfareSubPlan> welfareSubPlans = welfareSubPlanManager.getHaveOrderSubPlan(planId);
             //判断是否 额度转为积分
             WelfarePlan plan = welfarePlanManager.getByObjectId(planId);
             if(plan.getOverplusStrategy()==WelfarePlan.OVERPLUSSTRATEGY_TRANSFER_INTEGRAL){
                 WelfarePlanOrder wpo = new WelfarePlanOrder();
                 wpo.setPlanId(planId);
                 List<WelfarePlanOrder> wpos = welfarePlanOrderManager.getBySample(wpo);
                 for(WelfarePlanOrder w:wpos){
                     WelfareSubPlan wsp = new WelfareSubPlan();
                     wsp.setName("剩余额度转"+(request.getSession().getAttribute("s_welfarePointName")));
                     wsp.setStatus(w.getStatus());
                     wsp.setOrderId(w.getOrderId());
                     //剩余额度转积分员工数
                     Long headCount = welfarePlanStaffManager.getQuotaToIntegralCount(planId,WelfarePlanStaff.STATUS_HR_CONFIRMED);
                     wsp.setHeadCount(headCount);
                     welfareSubPlans.add(wsp);
                 }
             }
           //查询所有的总订单编号
             List<String> orderNos = new ArrayList<String>();
             for(WelfareSubPlan w:welfareSubPlans){
                 Long orderId = w.getOrderId();
                 Order order = orderManager.getByObjectId(orderId);
                 orderNos.add(order.getGeneralOrderNo());
                 SubOrder subOrder = new SubOrder();
                 subOrder.setGeneralOrderId(orderId);
                 List<SubOrder> subOrders = subOrderManager.getBySample(subOrder);
                 double payableAmount = 0.0;
                 for(SubOrder so:subOrders){
                     payableAmount += so.getPayableAmount();
                 }
                 w.setPayableAmount(payableAmount);
                 w.setOrder(order);
             }
             request.setAttribute("welfareSubPlans", welfareSubPlans);
             request.setAttribute("orderNos", orderNos);
//           // 查询员工剩余积分
           Double surplusScoreDou = userManager.getUserBalance(FrameworkContextUtils.getCurrentUserId());
           double surplusScore = surplusScoreDou==null?0:surplusScoreDou;
           request.setAttribute("surplusScore", surplusScore);


//           Order order = orderManager.getByObjectId(subOrderList.get(0).getGeneralOrderId());
//           //计算订单过时时间
//           Date orderDate = order.getBookingDate();
//           SystemParameter sp = systemParameterManager.getHrOrderTimeOutLimit();
//           Date endDate = DateUtils.addHours(orderDate, Integer.parseInt(sp.getValue()));
//           Long surplusTime = endDate.getTime()-new Date().getTime();
//           request.setAttribute("surplusTime", surplusTime);
           return HOME_DIR+"welfarePlanOrderPayCenter";
   }

   /**
   *
   * @author 闫冬全
   * @date 2015年6月30日
   * @param request
   * @param response
   * @param payWay
   *  :线上支付渠道（1：积分支付；2支付宝支付；3：混合支付）
   * @return
   */
  @RequestMapping("welfarePaySetPayment")
  public String welfarePaySetPayment(HttpServletRequest request, HttpServletResponse response) {
      String orderIdsStr = request.getParameter("orderIds");
      String payWay = request.getParameter("payWay");
      String payPassword = request.getParameter("payPassword");
      List<Long> subOrderIds = new ArrayList<Long>();
      if(StringUtils.isNotBlank(orderIdsStr)){
          String[] orderIds = orderIdsStr.split(",");
          String[] payWays = payWay.split("_");
          for(String orderId:orderIds){
              Long id = Long.parseLong(orderId);
              List<SubOrder> sos = subOrderManager.getSubOrderObjectIdByGeneralOrderID(id);
              //总订单支付方式
              Order order = orderManager.getByObjectId(id);
              if("1".equals(payWays[0])){//线上支付
                  order.setPaymentWay(IBSConstants.PAY_WAY_ON_LINE);
              }else if("2".equals(payWays[0])){//线下支付
                  order.setPaymentWay(IBSConstants.PAY_WAY_OFF_LINE);
              }

              orderManager.save(order);
              for(SubOrder so:sos){
                  subOrderIds.add(so.getObjectId());
              }
          }
          if(payWays.length==1){//线下支付
              List<SubOrder> subOrderList = new ArrayList<SubOrder>();
              double totalAmount = 0;
              for(Long subOrderId:subOrderIds){
                  SubOrder subOrder = subOrderManager.getByObjectId(subOrderId);
                  subOrderList.add(subOrder);
                  totalAmount += subOrder.getPayableAmount();
              }
              request.setAttribute("subOrderList", subOrderList);
              request.setAttribute("totalAmount", totalAmount);
              //得到福利计划
              WelfareSubPlan wsp = new WelfareSubPlan();
              wsp.setOrderId(Long.parseLong(orderIds[0]));
              List<WelfareSubPlan> wsps = welfareSubPlanManager.getBySample(wsp);
              Long planId = null;
              if(wsps.size()>0){
                  planId = wsps.get(0).getPlanId();
              }else{
                  WelfarePlanOrder wpo = new WelfarePlanOrder();
                  wpo.setOrderId(Long.parseLong(orderIds[0]));
                  planId = welfarePlanOrderManager.getBySample(wpo).get(0).getPlanId();
              }
              WelfarePlan plan = welfarePlanManager.getByObjectId(planId);
              request.setAttribute("plan", plan);
              return HOME_DIR+"payOfflineForWelfarePlan";
          }else{//线上支付
              //payWays[0];线上支付
              //payWays[1];支付方式

              if (payWays[1] != null && payWays[1].equals(String.valueOf(IBSConstants.PAY_WAY_ON_LINE_INTEGRAL))) {// 积分支付
                  for(Long subOrderId:subOrderIds){
                      SubOrder subOrder =  subOrderManager.getByObjectId(subOrderId);
                      subOrder.setOnlinePayChannels(IBSConstants.ONLINE_PAY_POINT);
                      subOrder.setActuallyAmount(0.0);
                      subOrder.setActuallyIntegral(subOrder.getPayableAmount());
                      subOrderManager.save(subOrder);
                  }
              } else if (payWays[1] != null && payWays[1].equals(String.valueOf(IBSConstants.PAY_WAY_ON_LINE_ALIPAY))) {// 支付宝支付
                  for(Long subOrderId:subOrderIds){
                      SubOrder subOrder =  subOrderManager.getByObjectId(subOrderId);
                      subOrder.setOnlinePayChannels(IBSConstants.ONLINE_PAY_ALIPAY);
                      subOrder.setActuallyAmount(subOrder.getPayableAmount());
                      subOrder.setActuallyIntegral(0.0);
                      subOrderManager.save(subOrder);
                  }
              } else {
                  return null;
              }
              String ids = StringUtils.join(subOrderIds,",");
              if("1".equals(payWays[1])){
                  //积分支付
                  request.setAttribute("payPassword", payPassword);
                  return "forward:/onlinePay/integralpayForWelfarePlan/"+orderIdsStr;
              }else if("2".equals(payWays[1])){
                  //支付宝支付
                  return "forward:/onlinePay/alipayForWelfarePlan/"+orderIdsStr;
              }
          }
      }
      return null;
  }

  /**
   * 支付宝支付
   * @author 闫冬全
   * @date 2015年7月7日
   * @param response
   * @param request
   * @param subOrderIds:用逗号分隔的子订单ID
   * @return
   */
  @RequestMapping("/alipayForWelfarePlan/{orderIdsStr}")
  public String alipayForWelfarePlan(HttpServletResponse response,HttpServletRequest request,@PathVariable String orderIdsStr){
      String[] orderIds = orderIdsStr.split(",");
      List<String> subOrderIds = new ArrayList<String>();
      for(String orderId:orderIds){
          Long id = Long.parseLong(orderId);
          List<SubOrder> sos = subOrderManager.getSubOrderObjectIdByGeneralOrderID(id);
          for(SubOrder so:sos){
              subOrderIds.add(so.getObjectId().toString());
          }
      }

      Long orderId = Long.parseLong(orderIds[0]);
    //得到福利计划
      WelfareSubPlan wsp = new WelfareSubPlan();
      wsp.setOrderId(orderId);
      List<WelfareSubPlan> wsps = welfareSubPlanManager.getBySample(wsp);
      Long planId = null;
      if(wsps.size()>0){
          planId = wsps.get(0).getPlanId();
      }else{
          WelfarePlanOrder wpo = new WelfarePlanOrder();
          wpo.setOrderId(orderId);
          planId = welfarePlanOrderManager.getBySample(wpo).get(0).getPlanId();
      }
      WelfarePlan plan = welfarePlanManager.getByObjectId(planId);

      User user = (User) request.getSession().getAttribute("s_user");
      String ip = (String) request.getSession().getAttribute("ip");
      String resultXml = "";

      AlipayEntity alipayEntity = new AlipayEntity();
      alipayEntity.setUserId(user.getObjectId());
      alipayEntity.setSubOrderIdList(subOrderIds);
      alipayEntity.setExterInvokeIp(ip);
      alipayEntity.setSubject(plan.getName());
      alipayEntity.setBody(plan.getName());
      alipayEntity.setDomain(AppUtils.getHrDynamicDomain(""));
      alipayEntity.setPaySucPage("onlinePay/successForWelfarePlan/"+orderIdsStr);
      alipayEntity.setPayFailPage("onlinePay/failForWelfarePlan/"+orderIdsStr);

      resultXml = alipayService.webPay(alipayEntity);
      LOGGER.info("===即时到账支付接口--表单提交===");
      LOGGER.info(resultXml);
      LOGGER.info("===即时到账支付接口结束===");

      response.resetBuffer();
      response.setContentType("text/html; charset=UTF-8");
      try {
          response.getWriter().print(resultXml);
      } catch (IOException e) {
          LOGGER.info("alipay response write content to page have a error!\n"+e.getMessage());
      }
      return null;
  }
  /**
   * 积分支付
   * @author 闫冬全
   * @date 2015年7月7日
   * @param response
   * @param request parameter或者Attribute里面必须给定支付密码password
   * @param subOrderIds:用逗号分隔的子订单ID
   * @return
   */
  @RequestMapping("/integralpayForWelfarePlan/{orderIds}")
  public String integralpayForWelfarePlan(HttpServletResponse response,HttpServletRequest request,@PathVariable String orderIds){
    //积分支付
      String password = request.getParameter("payPassword");
      if(StringUtils.isBlank(password)){
          password = (String) request.getAttribute("payPassword");
      }
      try{
          boolean result = orderManager.yearWelFareIntegralPay(orderIds, FrameworkContextUtils.getCurrentUserId(), password);
          if(result){
              return "forward:/onlinePay/successForWelfarePlan/"+orderIds;
          }else{
              return "forward:/onlinePay/failForWelfarePlan/"+orderIds;
          }
      }catch(Exception e){
          LOGGER.error("integralpay have a error\n"+e);
          return "forward:/onlinePay/failForWelfarePlan/"+orderIds;
      }
  }

  /**
   * 支付成功跳转的页面
   * @author 闫冬全
   * @date 2015年7月7日
   * @param response
   * @param request
   * @param subOrderIds:用逗号分隔的子订单ID
   * @return
   */
  @RequestMapping("/successForWelfarePlan/{orderIds}")
  public String successForWelfarePlan(HttpServletResponse response,HttpServletRequest request,@PathVariable String orderIds){
      String[] orderIdsStr = orderIds.split(",");
      List<Long> subOrderIds = new ArrayList<Long>();
      for(String orderId:orderIdsStr){
          Long id = Long.parseLong(orderId);
          List<SubOrder> sos = subOrderManager.getSubOrderObjectIdByGeneralOrderID(id);
          for(SubOrder so:sos){
              subOrderIds.add(so.getObjectId());
          }
      }
      List<SubOrder> subOrderList = new ArrayList<SubOrder>();
      double totalAmount = 0;
      for(Long subOrderId:subOrderIds){
          SubOrder subOrder = subOrderManager.getByObjectId(subOrderId);
          subOrderList.add(subOrder);
          totalAmount += subOrder.getPayableAmount();
      }
      request.setAttribute("subOrderList", subOrderList);
      request.setAttribute("totalAmount", totalAmount);
      //得到福利计划
      WelfareSubPlan wsp = new WelfareSubPlan();
      wsp.setOrderId(Long.parseLong(orderIdsStr[0]));
      List<WelfareSubPlan> wsps = welfareSubPlanManager.getBySample(wsp);
      Long planId = null;
      if(wsps.size()>0){
          planId = wsps.get(0).getPlanId();
      }else{
          WelfarePlanOrder wpo = new WelfarePlanOrder();
          wpo.setOrderId(Long.parseLong(orderIdsStr[0]));
          planId = welfarePlanOrderManager.getBySample(wpo).get(0).getPlanId();
      }
      WelfarePlan plan = welfarePlanManager.getByObjectId(planId);
      request.setAttribute("plan", plan);
      return HOME_DIR+"paySuccessForWelfarePlan";
  }
  /**
   * 支付失败跳转的页面
   * @author 闫冬全
   * @date 2015年7月7日
   * @param response
   * @param request
   * @param subOrderIds:用逗号分隔的子订单ID
   * @return
   */
  @RequestMapping("/failForWelfarePlan/{orderIds}")
  public String failForWelfarePlan(HttpServletResponse response,HttpServletRequest request,@PathVariable String orderIds){
      String[] orderIdsStr = orderIds.split(",");
      List<Order> orderList = new ArrayList<Order>();
      for(String orderId:orderIdsStr){
          Long id = Long.parseLong(orderId);
          Order order = orderManager.getByObjectId(id);
          if(order!=null){
              orderList.add(order);
          }
      }
      Order order = orderList.get(0);
      //计算订单过时时间
      Date orderDate = order.getBookingDate();
      SystemParameter sp = systemParameterManager.getHrOrderTimeOutLimit();
      Date endDate = DateUtils.addHours(orderDate, Integer.parseInt(sp.getValue()));
      Long surplusTime = endDate.getTime()-new Date().getTime();
      request.setAttribute("surplusTime", surplusTime);
      request.setAttribute("orderList", orderList);
      return HOME_DIR+"payFailForWelfarePlan";
  }


}
