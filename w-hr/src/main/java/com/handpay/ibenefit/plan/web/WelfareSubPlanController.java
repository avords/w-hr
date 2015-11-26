package com.handpay.ibenefit.plan.web;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.CRUDUtils;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.service.ICompanyDepartmentManager;
import com.handpay.ibenefit.order.entity.OrderSku;
import com.handpay.ibenefit.order.entity.SubOrder;
import com.handpay.ibenefit.order.service.IOrderSkuManager;
import com.handpay.ibenefit.order.service.ISubOrderManager;
import com.handpay.ibenefit.plan.entity.WelfarePlan;
import com.handpay.ibenefit.plan.entity.WelfareSubPlan;
import com.handpay.ibenefit.plan.entity.WelfareSubPlanItem;
import com.handpay.ibenefit.plan.entity.WelfareSubPlanStaffItem;
import com.handpay.ibenefit.plan.service.IWelfarePlanManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanItemManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanStaffItemManager;
import com.handpay.ibenefit.product.entity.SkuPublish;
import com.handpay.ibenefit.product.service.ISkuPublishManager;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.entity.WelfarePackage;
import com.handpay.ibenefit.welfare.service.IWelfareItemManager;
import com.handpay.ibenefit.welfare.service.IWelfareManager;
import com.handpay.ibenefit.welfare.service.IWelfarePackageManager;

@Controller
@RequestMapping("welfareSubPlan")
public class WelfareSubPlanController extends BaseController {

    @Reference(version ="1.0")
    private IWelfarePlanManager welfarePlanManager;

	@Reference(version ="1.0")
	private IWelfareSubPlanManager welfareSubPlanManager;

	@Reference(version ="1.0")
    private IWelfareSubPlanItemManager welfareSubPlanItemManager;

    @Reference(version = "1.0")
    private IWelfareManager welfareManager;

    @Reference(version = "1.0")
    private IWelfarePackageManager welfarePackageManager;

    @Reference(version = "1.0")
    private ISkuPublishManager skuPublishManager;

    @Reference(version = "1.0",check=false)
    private IOrderSkuManager orderSkuManager;

    @Reference(version = "1.0",check=false)
    private ISubOrderManager subOrderManager;

    @Reference(version = "1.0",check=false)
    private IWelfareSubPlanStaffItemManager welfareSubPlanStaffItemManager;

    @Reference(version = "1.0",check=false)
    private ICompanyDepartmentManager companyDepartmentManager;

    @Reference(version ="1.0")
    private IWelfareItemManager welfareItemManager;

	@RequestMapping("getSubPlans/{planId}")
	public String getSubPlans(ModelMap modelMap,@PathVariable Long planId) throws Exception {
		return "jsonView";
	}

	@RequestMapping("/saveSubPlan")
	public String saveSubPlan(HttpServletRequest request, ModelMap modelMap, WelfareSubPlan welfareSubPlan) throws Exception {
	    boolean result = false;
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(welfareSubPlan.getPlanId());
	    short status = welfarePlan.getStatus();
	    if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
	        modelMap.addAttribute("result", result);
	        return "jsonView";
        }
	    CRUDUtils.prepareSave(welfareSubPlan);
	    String ids = request.getParameter("ids");
	    Integer type = Integer.parseInt(request.getParameter("type"));
	    String itemExplain = request.getParameter("itemExplain");
	    String itemNames = request.getParameter("itemName");
	    String itemPrices = request.getParameter("itemPrice");
	    welfareSubPlan = welfareSubPlanManager.save(welfareSubPlan, type,ids,itemExplain,itemNames,itemPrices);
	    //监控必选和默认的同意，有默认就有必选，没默认就没必选
	    WelfareSubPlanItem item = new WelfareSubPlanItem();
	    item.setSubPlanId(welfareSubPlan.getObjectId());
	    item.setIsDefault(true);
	    List<WelfareSubPlanItem> items = welfareSubPlanItemManager.getBySample(item);
	    welfareSubPlan = welfareSubPlanManager.getByObjectId(welfareSubPlan.getObjectId());
	    if(items.size()>0&&!welfareSubPlan.getRequired()){
	        welfareSubPlan.setRequired(true);
	        welfareSubPlanManager.save(welfareSubPlan);
	    }else if(items.size()<=0&&welfareSubPlan.getRequired()){
	        welfareSubPlan.setRequired(false);
	        welfareSubPlanManager.save(welfareSubPlan);
	    }
	    result = true;
	    modelMap.addAttribute("result", result);
		return "jsonView";
	}

	@RequestMapping("/getGoods")
    public String getGoods(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap) {
	    boolean result = false;
	    String type = request.getParameter("type");
	    String welfareId = request.getParameter("welfareId");
	    String name = request.getParameter("name");
	    PageSearch page = new PageSearch();
        page.getFilters().add(
                new PropertyFilter(null, "EQL_companyId", FrameworkContextUtils.getCurrentUser().getCompanyId()
                        .toString()));
        page.setPageSize(Integer.MAX_VALUE);
        page.setCurrentPage(1);
        page.getFilters().add(new PropertyFilter(null, "EQL_companyId", FrameworkContextUtils.getCurrentUser().getCompanyId()+""));
        page.getFilters().add(new PropertyFilter(null, "EQL_welfareId", welfareId));
        page.getFilters().add(new PropertyFilter(null, "LIKES_name", name));
        if(!"0".equals(type)){
            page.getFilters().add(new PropertyFilter(null, "EQI_type", type));
        }
        page.getFilters().add(new PropertyFilter(null, "EQI_isWelfarePlan", IBSConstants.STATUS_YES+""));
	    PageSearch resultList = welfareSubPlanItemManager.getWelfareProducts(page);
	    page.setList(resultList.getList());
	    page.setTotalCount(resultList.getTotalCount());
//	    List<WelfareSubPlanItem> list = page.getList();
//	    for(WelfareSubPlanItem w:list){
//	        if(w.getType()==(short)IBSConstants.ORDER_PRODUCT_TYPE_GRANT_POINTS){
//	            w.setGoodsName(w.getName());
//	        }
//	    }
	    result = true;
	    modelMap.addAttribute("result", result);
	    modelMap.addAttribute("pageSearch", page);
        return "jsonView";
    }
	@RequestMapping("edit/{planId}")
    public String edit(HttpServletRequest request, ModelMap modelMap, @PathVariable Long planId ) throws Exception {
	    WelfarePlan entity = welfarePlanManager.getByObjectId(planId);
	    String type = request.getParameter("type");
        short status = entity.getStatus();
        if("detail".equals(type)||status==WelfarePlan.STATUS_DRAFT||status==WelfarePlan.STATUS_WAIT_PUBLISH){
            request.setAttribute("entity", entity);
            List<WelfareSubPlan> welfareSubPlans = welfareSubPlanManager.getWelfareSubPlans(planId);
            for(WelfareSubPlan w:welfareSubPlans){
                if(w.getWelfareItemId()!=null){
                    WelfareItem welfareItem = welfareItemManager.getByObjectId(w.getWelfareItemId());
                    if(welfareItem!=null){
                        w.setWelfareItemName(welfareItem.getItemName());
                    }
                }
                List<WelfareSubPlanItem> wsis = welfareSubPlanItemManager.getBySubPlanId(w.getObjectId());
                for(WelfareSubPlanItem ws:wsis){
                    if(ws.getType().equals((short)1)){
//                        WelfarePackage wp = welfarePackageManager.getPackagePrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), ws.getGoodsId());
//                        ws.setGoodsName(wp.getPackageName());
//                        ws.setPrice(wp.getPackagePrice());

                        WelfarePackage wp = welfarePackageManager.getByObjectId(ws.getGoodsId());
                        ws.setGoodsName(wp.getPackageName());
                    }else if(ws.getType().equals((short)3)){
//                        SkuPublish sku = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), ws.getGoodsId());
//                        ws.setGoodsName(sku.getName()+"("+sku.getAttributeValue1()+","+sku.getAttributeValue2()+")");
//                        ws.setPrice(sku.getSellPrice());

                        SkuPublish sku = skuPublishManager.getByObjectId(ws.getGoodsId());
                        ws.setGoodsName(sku.getName()+"("+(sku.getAttributeValue1()==null?"":sku.getAttributeValue1())+(sku.getAttributeValue2()==null?"":(","+sku.getAttributeValue2()))+")");
                    }else if(ws.getType().equals((short)5)){
                        ws.setGoodsName(ws.getName());
                    }
                }
                w.setWelfareSubPlanItems(wsis);
            }
            request.setAttribute("welfareSubPlans", welfareSubPlans);

            Map<String,Object> param = new HashMap<String,Object>();
            param.put("itemType", 1);
            param.put("itemGrade", 2);
            param.put("status", 1);
            param.put("isWelfarePlan", 1);
            List<WelfareItem> welfareItems = welfareManager.getItemByParam(param);
            request.setAttribute("welfareItems", welfareItems);
            request.setAttribute("planId", planId);

            Date endDate = entity.getEndSelectDate();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String endSelectDate = sdf.format(endDate);
            request.setAttribute("endSelectDate", endSelectDate);
            if("detail".equals(type)){
                return "plan/detailSubWelfarePlan";
            }
            return "plan/editSubWelfarePlan";
        }else{
            return "redirect:/welfarePlan/page";
        }

    }
	@RequestMapping("/setRequired/{subPlanId}")
    public String setRequired(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap,@PathVariable Long subPlanId) {
        boolean result = false;
        WelfareSubPlan welfareSubPlan = welfareSubPlanManager.getByObjectId(subPlanId);
        WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(welfareSubPlan.getPlanId());
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            modelMap.addAttribute("result", result);
            return "jsonView";
        }
        welfareSubPlan.setRequired(true);
        welfareSubPlanManager.save(welfareSubPlan);
        result = true;
        //默认金额最小的选项
        List<WelfareSubPlanItem> wsis = welfareSubPlanItemManager.getBySubPlanId(subPlanId);
        if(wsis.size()>0){
            WelfareSubPlanItem wsi = wsis.get(0);
            welfareSubPlanItemManager.setDefault(welfareSubPlan.getPlanId(), subPlanId, wsi.getGoodsId(), wsi.getType());
        }
        modelMap.addAttribute("result", result);
        return "jsonView";
    }
	@RequestMapping("/cancelRequired/{subPlanId}")
    public String cancelRequired(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap,@PathVariable Long subPlanId) {
        boolean result = false;
        WelfareSubPlan welfareSubPlan = welfareSubPlanManager.getByObjectId(subPlanId);
        WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(welfareSubPlan.getPlanId());
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            modelMap.addAttribute("result", result);
            return "jsonView";
        }
        welfareSubPlan.setRequired(false);
        welfareSubPlanManager.save(welfareSubPlan);
        //取消必选下面的默认
        welfareSubPlanItemManager.cleanDefault(subPlanId);
        welfarePlanManager.updateMinGrantQuota(welfareSubPlan.getPlanId());
        result = true;
        modelMap.addAttribute("result", result);
        return "jsonView";
    }
	@RequestMapping("/deleteSubPlan/{subPlanId}")
    public String deleteSubPlan(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap,@PathVariable Long subPlanId) {
	    boolean result = false;
	    WelfareSubPlan welfareSubPlan = welfareSubPlanManager.getByObjectId(subPlanId);
        WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(welfareSubPlan.getPlanId());
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            modelMap.addAttribute("result", result);
            return "jsonView";
        }
        //删除子计划项目
        WelfareSubPlanItem item = new WelfareSubPlanItem();
        item.setSubPlanId(subPlanId);
        welfareSubPlanItemManager.deleteBySample(item);
        welfareSubPlan.setDeleted(IBSConstants.STATUS_YES);
        welfareSubPlanManager.delete(welfareSubPlan);
        result = true;
        modelMap.addAttribute("result", result);
        return "jsonView";
    }
	@RequestMapping("/getSubPlan/{subPlanId}")
    public String getSubPlan(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap,@PathVariable Long subPlanId) {
        boolean result = false;
        WelfareSubPlan welfareSubPlan = welfareSubPlanManager.getByObjectId(subPlanId);
        WelfareSubPlanItem wsi = new WelfareSubPlanItem();
        wsi.setSubPlanId(subPlanId);
        List<WelfareSubPlanItem> wsis = welfareSubPlanItemManager.getBySample(wsi);
        for(WelfareSubPlanItem ws:wsis){
            if(ws.getType().equals((short)1)){
                WelfarePackage wp = welfarePackageManager.getPackagePrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), ws.getGoodsId());
                ws.setGoodsName(wp.getPackageName());
                ws.setPrice(wp.getPackagePrice());
            }else if(ws.getType().equals((short)3)){
                SkuPublish sku = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), ws.getGoodsId());
                ws.setGoodsName(sku.getName()+"("+(sku.getAttributeValue1()==null?"":sku.getAttributeValue1())+(sku.getAttributeValue2()==null?"":(","+sku.getAttributeValue2()))+")");
                ws.setPrice(sku.getSellPrice());
            }else if(ws.getType().equals((short)5)){
                ws.setGoodsName(ws.getName());
            }
        }
        welfareSubPlan.setWelfareSubPlanItems(wsis);
        result = true;
        modelMap.addAttribute("subPlan", welfareSubPlan);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String publishDate = "";
        if(welfareSubPlan.getPublishDate()!=null){
            publishDate = sdf.format(welfareSubPlan.getPublishDate());
        }
        modelMap.addAttribute("publishDate", publishDate);
        modelMap.addAttribute("result", result);
        return "jsonView";
    }

	@RequestMapping("/grant/{subPlanId}")
    public String grant(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap,@PathVariable Long subPlanId) throws Exception {
        boolean result = false;
        String type = request.getParameter("type");
        String publishDate = request.getParameter("publishDate");
        if("1".equals(type)){//选择时间发布
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(publishDate);
            welfareSubPlanManager.updatePublishDate(subPlanId, date);
        }else if("2".equals(type)){//立即发布
            welfareSubPlanManager.updatePublishDate(subPlanId, new Date());
        }
        result = true;
        modelMap.addAttribute("result", result);
        return "jsonView";
    }

	@RequestMapping("/getStaffDetail/{subPlanId}")
    public String getStaffDetail(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap,@PathVariable Long subPlanId) throws Exception {
        boolean result = false;
        Long orderSkuId = Long.parseLong(request.getParameter("orderSkuId"));
        OrderSku os = orderSkuManager.getByObjectId(orderSkuId);
        SubOrder subOrder = subOrderManager.getByObjectId(os.getSubOrderId());
        Long goodsId = os.getProductId();
        Short type = subOrder.getOrderProdType().shortValue();
        if(type==(short)IBSConstants.ORDER_PRODUCT_TYPE_PHYSICAL){
            type = IBSConstants.ORDER_PRODUCT_TYPE_WELFARE;
        }
        PageSearch pageSearch = PageUtils.preparePage(request,WelfareSubPlanStaffItem.class,false);
        pageSearch.getFilters().add(new PropertyFilter(WelfareSubPlanStaffItem.class.getName(), "EQL_subPlanId", subPlanId.toString()));
        pageSearch.getFilters().add(new PropertyFilter(WelfareSubPlanStaffItem.class.getName(), "EQL_goodsId", goodsId.toString()));
        pageSearch.getFilters().add(new PropertyFilter(WelfareSubPlanStaffItem.class.getName(), "EQI_type", type.toString()));
//        pageSearch.getFilters().add(new PropertyFilter(WelfareSubPlanStaffItem.class.getName(), "EQI_status", WelfareSubPlanStaffItem.STATUS_SELECTED+""));
//        pageSearch.getFilters().add(new PropertyFilter(WelfareSubPlanStaffItem.class.getName(), "EQI_staffStatus", WelfarePlanStaff.STATUS_HR_CONFIRMED+""));
        PageSearch resultPage = welfareSubPlanStaffItemManager.getWelfarePlanStaffItem(pageSearch);
        pageSearch.setList(resultPage.getList());
        pageSearch.setTotalCount(resultPage.getTotalCount());
        List<WelfareSubPlanStaffItem> wssis = pageSearch.getList();
        for(WelfareSubPlanStaffItem s:wssis){
            //查询员工的信息
            Long departmentId = s.getStaff().getDepartmentId();
            if(departmentId!=null){
                CompanyDepartment department = companyDepartmentManager.getByObjectId(departmentId);
                if(department!=null){
                    s.setDepartmentName(department.getName());
                }
            }
        }
        modelMap.addAttribute("pageSearch", pageSearch);
        //得到子计划
        WelfareSubPlan subPlan = welfareSubPlanManager.getByObjectId(subPlanId);
        //得到总计划
        WelfarePlan plan = welfarePlanManager.getByObjectId(subPlan.getPlanId());
        //得到orderSku信息
        OrderSku orderSku = orderSkuManager.getByObjectId(orderSkuId);
        modelMap.addAttribute("subPlan", subPlan);
        modelMap.addAttribute("plan", plan);
        modelMap.addAttribute("orderSku",orderSku);
        result = true;
        modelMap.addAttribute("result", result);
        return "jsonView";
    }
}
