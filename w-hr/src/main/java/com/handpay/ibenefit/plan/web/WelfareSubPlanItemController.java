package com.handpay.ibenefit.plan.web;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.plan.entity.WelfarePlan;
import com.handpay.ibenefit.plan.entity.WelfareSubPlanItem;
import com.handpay.ibenefit.plan.service.IWelfarePlanManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanItemManager;

@Controller
@RequestMapping("/welfareSubPlanItem")
public class WelfareSubPlanItemController {

    @Reference(version = "1.0")
    private IWelfareSubPlanItemManager welfareSubPlanItemManager;
    @Reference(version = "1.0")
    private IWelfarePlanManager welfarePlanManager;

    @RequestMapping("/setDefault/{planId}/{subPlanId}/{goodsId}")
    public String setDefault(HttpServletRequest request, ModelMap modelMap, @PathVariable Long planId, @PathVariable Long subPlanId, @PathVariable Long goodsId) {
        boolean result = false;
        WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            modelMap.addAttribute("result", result);
            return "jsonView";
        }
        Short type = Short.parseShort(request.getParameter("type"));
        welfareSubPlanItemManager.setDefault(planId, subPlanId, goodsId, type);
        result = true;
        modelMap.addAttribute("result", result);
        return "jsonView";
    }

    @RequestMapping("/deleteSubPlanItem/{planId}/{subPlanId}/{goodsId}")
    public String deleteSubPlanItem(HttpServletRequest request, ModelMap modelMap, @PathVariable Long planId, @PathVariable Long subPlanId, @PathVariable Long goodsId) {
        boolean result = false;
        WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            modelMap.addAttribute("result", result);
            return "jsonView";
        }
        Short type = Short.parseShort(request.getParameter("type"));
        WelfareSubPlanItem ws = new WelfareSubPlanItem();
        ws.setSubPlanId(subPlanId);
        ws.setPlanId(planId);
        ws.setGoodsId(goodsId);
        ws.setType(type);
        welfareSubPlanItemManager.deleteBySample(ws);
        result = true;
        modelMap.addAttribute("result", result);
        return "jsonView";
    }
}
