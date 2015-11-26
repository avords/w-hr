package com.handpay.ibenefit.plan.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.framework.util.UploadFile;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.entity.Staff;
import com.handpay.ibenefit.member.service.ICompanyDepartmentManager;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.plan.entity.WelfarePlan;
import com.handpay.ibenefit.plan.entity.WelfarePlanQuota;
import com.handpay.ibenefit.plan.entity.WelfarePlanStaff;
import com.handpay.ibenefit.plan.entity.WelfareSubPlanItem;
import com.handpay.ibenefit.plan.entity.WelfareSubPlanStaffItem;
import com.handpay.ibenefit.plan.service.IWelfarePlanManager;
import com.handpay.ibenefit.plan.service.IWelfarePlanQuotaManager;
import com.handpay.ibenefit.plan.service.IWelfarePlanStaffManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanItemManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanStaffItemManager;
import com.handpay.ibenefit.product.service.ISkuPublishManager;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.system.web.CompanyDepartmentUtils;
import com.handpay.ibenefit.welfare.service.IWelfarePackageManager;

@Controller
@RequestMapping("welfarePlanStaff")
public class WelfarePlanStaffController {

	@Reference(version = "1.0")
	private IWelfarePlanStaffManager welfarePlanStaffManager;

	@Reference(version = "1.0")
	private IStaffManager staffManager;

	@Reference(version = "1.0")
	private IUserManager userManager;

	@Reference(version = "1.0")
	private IWelfarePlanManager welfarePlanManager;

	@Reference(version = "1.0")
	private ICompanyDepartmentManager companyDepartmentManager;

	@Reference(version = "1.0")
    private IWelfareSubPlanStaffItemManager welfareSubPlanStaffItemManager;

	@Reference(version = "1.0")
    private IWelfarePackageManager welfarePackageManager;

	@Reference(version = "1.0")
    private ISkuPublishManager skuPublishManager;

	@Reference(version = "1.0")
    private IWelfareSubPlanItemManager welfareSubPlanItemManager;
	@Reference(version = "1.0")
	private IWelfarePlanQuotaManager welfarePlanQuotaManager;

	@RequestMapping("get/{planId}/{userId}")
	public String getWelfareSubPlanStaff(@PathVariable Long planId, @PathVariable Long userId, ModelMap modelMap) throws Exception {
		WelfarePlanStaff entity = welfarePlanStaffManager.getWelfarePlanStaff(planId, userId);
		Staff staff = staffManager.getByObjectId(userId);
		User user = userManager.getByObjectId(userId);
		modelMap.addAttribute("entity", entity);
		modelMap.addAttribute("staff", staff);
		modelMap.addAttribute("user", user);
		return "jsonView";
	}

	@RequestMapping("page/{planId}")
	public String page(HttpServletRequest request, HttpServletResponse response, @PathVariable Long planId) throws Exception {
		if(planId!=null){
			WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
			short status = welfarePlan.getStatus();
	        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
	            return "redirect:/welfarePlan/page";
	        }
			request.setAttribute("welfarePlan", welfarePlan);
			PageSearch pageSearch = PageUtils.preparePage(request, WelfarePlanStaff.class, true);
			pageSearch.getFilters().add(new PropertyFilter("","EQL_planId",planId + ""));
			pageSearch = welfarePlanStaffManager.findWelfarePlanStaff(pageSearch);
			request.setAttribute("pageSearch", pageSearch);
			User user = FrameworkContextUtils.getCurrentUser();
			List<CompanyDepartment> departments = companyDepartmentManager.getCompanyDepartmentsByCompanyId(user.getCompanyId());
			if(UserUtils.isCompanyHR()){
				departments = CompanyDepartmentUtils.getSubCompanyDepartment(departments, user.getOrganizationId());
			}
			CompanyDepartmentUtils.fillALlFullName(departments);
			request.setAttribute("departments", departments);
			return "plan/listWelfarePlanStaff";
		}
		return null;
	}


	@RequestMapping("update")
	public String update(ModelMap modelMap, WelfarePlanStaff welfareSubPlanStaff) throws Exception {
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(welfareSubPlanStaff.getPlanId());
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            modelMap.addAttribute("result", false);
            return "jsonView";
        }
	    welfarePlanStaffManager.updateByPlanIdAndUserId(welfareSubPlanStaff);
		modelMap.addAttribute("result", true);
		return "jsonView";
	}

	@RequestMapping("delete")
	public String delete(ModelMap modelMap, Long planId, Long userId) throws Exception {
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            return "redirect:/welfarePlan/page";
        }
		if(planId!=null && userId !=null){
			welfarePlanStaffManager.deleteWelfarePlanStaff(planId,userId);
		}
		return "redirect:page/" + planId;
	}

	@RequestMapping("saveBatch")
	public String saveBatch(ModelMap modelMap,WelfarePlanStaff welfareSubPlanStaff, String userIds) throws Exception {
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(welfareSubPlanStaff.getPlanId());
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            modelMap.addAttribute("result", false);
            return "jsonView";
        }
	    boolean result = false;
		if(StringUtils.isNotBlank(userIds)){
			Set<Long> userId = new HashSet<Long>();
			String[] ids = userIds.split(",");
			for(String id : ids){
				userId.add(Long.parseLong(id));
			}
			welfarePlanStaffManager.saveBatch(welfareSubPlanStaff, new ArrayList<Long>(userId));
			result = true;
		}
		modelMap.addAttribute("result", result);
		return "jsonView";
	}

	@RequestMapping("importQuota/{planId}")
	public String importQuota(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap, @PathVariable Long planId) throws Exception {
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            modelMap.addAttribute("result", false);
            modelMap.addAttribute("message", "处于发布中的计划不能导入");
            AjaxUtils.doAjaxResponseOfMap(response, modelMap);
            return null;
        }
	    UploadFile uploadFile = FileUpDownUtils.getUploadFile(request);
		boolean result = false;
		String message = "导入失败";
		if(uploadFile!=null && planId !=null ){
			byte[] fileData = FileUpDownUtils.getFileContent(uploadFile.getFile());
			Map<String,String> map = welfarePlanStaffManager.importQuota(fileData, planId);
		    if(map.get("result").equals("true")){
		        result = true;
		    }else if(map.get("result").equals("false")){
		        result = false;
		    }
		    message = map.get("message");
		}
		modelMap.addAttribute("result", result);
		modelMap.addAttribute("message", message);
		AjaxUtils.doAjaxResponseOfMap(response, modelMap);
		return null;
	}

	@RequestMapping("importQuotaBySalary/{planId}")
    public String importQuotaBySalary(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap, @PathVariable Long planId) throws Exception {
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
        short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            modelMap.addAttribute("result", false);
            modelMap.addAttribute("message", "处于发布中的计划不能导入");
            AjaxUtils.doAjaxResponseOfMap(response, modelMap);
            return null;
        }
	    boolean result = false;
        String message = "导入失败";
	    String percentStr = request.getParameter("percent");
	    UploadFile uploadFile = FileUpDownUtils.getUploadFile(request);
        if(StringUtils.isNotBlank(percentStr)){
            double percent = Double.parseDouble(percentStr);
            if(uploadFile!=null && planId !=null ){
                byte[] fileData = FileUpDownUtils.getFileContent(uploadFile.getFile());
                Map<String,String> map = welfarePlanStaffManager.importQuotaBySalary(fileData,percent, planId);
                if(map.get("result").equals("true")){
                    result = true;
                }else if(map.get("result").equals("false")){
                    result = false;
                }
                message = map.get("message");
            }
        }
        modelMap.addAttribute("result", result);
        modelMap.addAttribute("message", message);
        AjaxUtils.doAjaxResponseOfMap(response, modelMap);
        return null;
    }
	@RequestMapping("confirmSelect/{planId}")
    public String confirmSelect(HttpServletRequest request, HttpServletResponse response, @PathVariable Long planId) throws Exception {
	    WelfarePlan entity = welfarePlanManager.getByObjectId(planId);
	    request.setAttribute("entity", entity);
	    boolean isNextStep = new Date().getTime()>=entity.getEndSelectDate().getTime();
        request.setAttribute("isNextStep",isNextStep);
        if(!isNextStep||!entity.getStatus().equals(WelfarePlan.STATUS_WAIT_CONFIRMED)){
            return "redirect:/welfarePlan/page";
        }
        //自动对没有确认的员工做必选项的确认
        WelfarePlanStaff sample = new WelfarePlanStaff();
        sample.setPlanId(planId);
        sample.setStatus(WelfarePlanStaff.STATUS_WAIT_CONFIRME);
        List<WelfarePlanStaff> unConfirmed = welfarePlanStaffManager.getBySample(sample);
        if(unConfirmed.size()>0){
            List<WelfareSubPlanItem> subPlanItems = welfareSubPlanItemManager.getRequiredSubPlanItems(planId);
            if(subPlanItems.size()>0){
                for(WelfarePlanStaff staff : unConfirmed){
                    if(staff.getQuota() >= (entity.getMinGrantQuota()==null?0.0:entity.getMinGrantQuota())){
                        staff.setCommitDate(entity.getEndSelectDate());
                        staff.setStatus(WelfarePlanStaff.STATUS_CONFIRMED);

                        //先删除，根据planId和userId
//                        welfareplanstaffmanager.deletewelfareplanstaff(planid, staff.getuserid());
//                        welfareplanstaffmanager.save(staff);
                        welfarePlanStaffManager.updateByPlanIdAndUserId(staff);
                        for(WelfareSubPlanItem item : subPlanItems){
                            WelfareSubPlanStaffItem userItem = new WelfareSubPlanStaffItem();
                            userItem.setPlanId(planId);
                            userItem.setSubPlanId(item.getSubPlanId());
                            userItem.setGoodsId(item.getGoodsId());
                            userItem.setUserId(staff.getUserId());
                            userItem.setType(item.getType());
                            userItem.setStatus(WelfareSubPlanStaffItem.STATUS_SELECTED);
                            welfareSubPlanStaffItemManager.deleteWelfarePlanStaffItem(staff.getUserId(), item.getSubPlanId(), item.getGoodsId(), item.getType());
                            welfareSubPlanStaffItemManager.save(userItem);
                        }
                        //更新已经使用的额度
                        Map<String,Object> map = new HashMap<String,Object>();
                        map.put("planId", planId);
                        map.put("userId", staff.getUserId());
                        map.put("status", WelfareSubPlanStaffItem.STATUS_SELECTED);
                        map.put("planStaffStatus", WelfarePlanStaff.STATUS_CONFIRMED);
                        double used = welfareSubPlanStaffItemManager.getPayAmountByParam(map);
                        //剩余额度的计算
                        double minGrantQuota = entity.getMinGrantQuota()==null?0.0:entity.getMinGrantQuota();

                         //使用的额度记录
                         WelfarePlanQuota wpq = new WelfarePlanQuota();
                         wpq.setOperateDate(new Date());
                         wpq.setPlanId(planId);
                         wpq.setType(WelfarePlanQuota.OPERATION_TYPE_USE);
                         wpq.setUserId(staff.getUserId());
                        if(used>minGrantQuota){
                            staff.setUsed(used);
                            wpq.setQuota(used);
                        }else{
                            staff.setUsed(minGrantQuota);
                            wpq.setQuota(minGrantQuota);
                        }
                        welfarePlanQuotaManager.save(wpq);
                        staff.setCommitDate(entity.getEndSelectDate());
                        staff.setStatus(WelfarePlanStaff.STATUS_CONFIRMED);
                        if(entity.getOverplusStrategy() != WelfarePlan.OVERPLUSSTRATEGY_CANCEL){
                            staff.setOverplusQuota(staff.getQuota() - staff.getUsed());
                        }else{
                            //作废的剩余额度记录
                            WelfarePlanQuota wp = new WelfarePlanQuota();
                            wp.setOperateDate(new Date());
                            wp.setPlanId(planId);
                            wp.setType(WelfarePlanQuota.OPERATION_TYPE_INVALID);
                            wp.setUserId(staff.getUserId());
                            wp.setQuota(staff.getOverplusQuota());
                            welfarePlanQuotaManager.save(wp);
                            staff.setOverplusQuota(0D);
                        }
                        staff.setHistoryQuota(welfarePlanStaffManager.findOverplusQuota(staff.getUserId()));
                        //先删除，根据planId和userId
//                        welfarePlanStaffManager.deleteWelfarePlanStaff(planId, staff.getUserId());
//                        welfarePlanStaffManager.save(staff);
                        welfarePlanStaffManager.updateByPlanIdAndUserId(staff);
                    }
                }
            }
        }
        welfarePlanManager.updateHeadCountAndTotalAmount(planId);
        //离职员工自动取消
        welfarePlanStaffManager.updateStatus(WelfarePlanStaff.STATUS_CANCELLED,planId,IBSConstants.STAFF_STATUS_OFF);
        //在职员工自动同意
        welfarePlanStaffManager.updateStatus(WelfarePlanStaff.STATUS_HR_CONFIRMED,planId,IBSConstants.STAFF_STATUS_ON);
	    PageSearch page= PageUtils.preparePage(request, WelfarePlanStaff.class, false);
	    page.getFilters().add(
                new PropertyFilter("WelfarePlanStaff", "EQL_planId", planId+""));
	    PageSearch result = welfarePlanStaffManager.getPlanStaff(page);
	    page.setList(result.getList());
	    page.setTotalCount(result.getTotalCount());
	    List<WelfarePlanStaff> list = page.getList();
	    Double amount=0.0;
	    for(WelfarePlanStaff w : list){
	        Map<String,Object> param = new HashMap<String,Object>();
            param.put("planId", planId);
            param.put("status", WelfareSubPlanStaffItem.STATUS_SELECTED);
            param.put("userId", w.getUserId());
	        List<WelfareSubPlanStaffItem> wsis = welfareSubPlanStaffItemManager.getByParam(param);
	        w.setStaffItems(wsis);
	        Double totalAmount = 0.0;
	        for(WelfareSubPlanStaffItem temp:wsis){
	            totalAmount = totalAmount+temp.getPrice();
	        }
	        w.setSubTotalAmount(totalAmount);
	        w.setStaffItems(wsis);
	        if(wsis.size()==0){
	            w.setCommitDate(entity.getEndSelectDate());
	            WelfareSubPlanStaffItem wssi = new WelfareSubPlanStaffItem();
	            wssi.setGoodsName("员工未选择");
	            wsis.add(wssi);
	            w.setStaffItems(wsis);
	        }
	    }
	  //得到需要结算的金额
        Map<String,Object> param = new HashMap<String,Object>();
        param.put("planId", planId);
        param.put("status", WelfareSubPlanStaffItem.STATUS_SELECTED);
        param.put("planStaffStatus", WelfarePlanStaff.STATUS_HR_CONFIRMED);
        amount = welfareSubPlanStaffItemManager.getPayAmountByParam(param);

	    request.setAttribute("pageSearch", page);
	    request.setAttribute("planId", planId);
	    request.setAttribute("amount", amount);
	    return "plan/confirmSelect";
    }

	@RequestMapping("cancel")
    public String cancel(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,WelfarePlanStaff welfarePlanStaff) throws Exception {
	    welfarePlanStaffManager.updateStatusToCancel(welfarePlanStaff.getPlanId(),welfarePlanStaff.getUserId(),welfarePlanStaff.getCancelReason());
	    modelMap.addAttribute("result", true);
        return "jsonView";
    }

	@RequestMapping("agree")
    public String agree(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,WelfarePlanStaff welfarePlanStaff) throws Exception {
	    welfarePlanStaffManager.updateStatusToAgree(welfarePlanStaff.getPlanId(),welfarePlanStaff.getUserId());
        modelMap.addAttribute("result", true);
        return "jsonView";
    }

	@RequestMapping("/isHaveSelected/{planId}")
    public String isHaveSelected(HttpServletRequest request, HttpServletResponse response, @PathVariable Long planId,ModelMap modelMap) throws Exception {
        boolean result = false;
	    WelfarePlanStaff sp = new WelfarePlanStaff();
        sp.setPlanId(planId);
        sp.setStatus(WelfarePlanStaff.STATUS_CONFIRMED);
        Long count = welfarePlanStaffManager.getObjectCount(sp);
        if(count>0){
            result = true;
        }
        modelMap.addAttribute("result", result);
        return "jsonView";
    }
}
