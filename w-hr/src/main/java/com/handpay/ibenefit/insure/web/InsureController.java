package com.handpay.ibenefit.insure.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.ExcelUtil;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.ListUtils;
import com.handpay.ibenefit.framework.util.UploadFile;
import com.handpay.ibenefit.insure.entity.InsureNotComplete;
import com.handpay.ibenefit.insure.service.IInsureManager;
import com.handpay.ibenefit.insure.service.IInsureNotCompleteManager;
import com.handpay.ibenefit.insure.service.IInsureOrderManager;
import com.handpay.ibenefit.insure.service.IInsureRangeManager;
import com.handpay.ibenefit.member.entity.Staff;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.security.entity.User;

/**
  * @ClassName: InsureController
  * @Description: TODO 保险模块
  * @author Comsys-Mac.Yoon
  * @date 2015-11-5 下午5:19:25
  *
 */
@Controller
@RequestMapping("insure")
public class InsureController {

    private static final Logger LOGGER = Logger.getLogger(InsureController.class);

	@Reference(version = "1.0", timeout= 60000)
    private IInsureNotCompleteManager insureNotCompleteManager;
	@Reference(version = "1.0", timeout= 60000)
    private IInsureOrderManager insureOrderManager;
	@Reference(version = "1.0", timeout= 60000)
    private IStaffManager staffManager;
	@Reference(version = "1.0", timeout= 60000)
    private IInsureManager insureManager;
	@Reference(version = "1.0", timeout= 60000)
    private IInsureRangeManager insureRangeManager;

	/**
	 * 投保
	 * @param request
	 * @param response
	 * @param modelMap
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/placeOrder")
	public String placeOrder(HttpServletRequest request,HttpServletResponse response,ModelMap modelMap) throws Exception{
	    boolean result = false;
	    Long productId = Long.parseLong(request.getParameter("productId"));
        Long attributeValueId = Long.parseLong(request.getParameter("attributeValueId"));
        Long hrId = FrameworkContextUtils.getCurrentUserId();
        User user = FrameworkContextUtils.getCurrentUser();
        String optionType = request.getParameter("optionType");
        String value = request.getParameter("value");
        String[] values = value.split(",");
        List<Long> staffIds = new ArrayList<Long>();
        if("member".equals(optionType)){
            String memberType = request.getParameter("memberType");
            List<Long> hasStaffIds = new ArrayList<Long>();
            for(String idStr:values){
                hasStaffIds.add(Long.parseLong(idStr));
            }
            if("1".equals(memberType)){//特定人员
                staffIds = hasStaffIds;
            }else if("0".equals(memberType)){//特定人员不发放
                //得到所有的员工，让后取差积
                Staff staff = new Staff();
                staff.setStatus(IBSConstants.STAFF_STATUS_ON);
                staff.setOrganizationId(user.getOrganizationId());
                staff.setCompanyId(user.getCompanyId());
                List<Staff> staffs = staffManager.getBySample(staff);
                List<Long> allStaffIds = new ArrayList<Long>();
                for(Staff sta:staffs){
                    allStaffIds.add(sta.getObjectId());
                }
                //取差集
                staffIds = ListUtils.filter(allStaffIds, hasStaffIds);
            }

        }else if("all".equals(optionType)){
            Staff staff = new Staff();
            staff.setStatus(IBSConstants.STAFF_STATUS_ON);
            staff.setOrganizationId(user.getOrganizationId());
            staff.setCompanyId(user.getCompanyId());
            List<Staff> staffs = staffManager.getBySample(staff);
            for(Staff sta:staffs){
                staffIds.add(sta.getObjectId());
            }
        }else if("part".equals(optionType)){
            for(String idStr:values){
                Long partId = Long.parseLong(idStr);
                List<Staff> partUsers = staffManager.getAllStaffs(user.getCompanyId(),partId);
                for(Staff sta:partUsers){
                    staffIds.add(sta.getObjectId());
                }
            }
        }else if("team".equals(optionType)){
            for(String idStr:values){
                Long teamId = Long.parseLong(idStr);
                List<Staff> teamUsers = staffManager.getProjectTeamMembers(teamId);
                for(Staff sta:teamUsers){
                    staffIds.add(sta.getObjectId());
                }
            }
        }
        return createInsureOrder(productId, attributeValueId, hrId, staffIds, response, modelMap, result);
	}

	@RequestMapping("/placeOrderByUpload")
    public String placeOrderByUpload(HttpServletRequest request,HttpServletResponse response,ModelMap modelMap) throws Exception{
        boolean result = false;
        Long productId = Long.parseLong(request.getParameter("productId"));
        Long attributeValueId = Long.parseLong(request.getParameter("attributeValueId"));
        Long hrId = FrameworkContextUtils.getCurrentUserId();
        User user = FrameworkContextUtils.getCurrentUser();
        String optionType = request.getParameter("optionType");
        String value = request.getParameter("value");
        String[] values = value.split(",");
        List<Long> staffIds = new ArrayList<Long>();
        UploadFile uploadFile = FileUpDownUtils.getUploadFile(request);
        if(uploadFile!=null ){
            byte[] fileData = FileUpDownUtils.getFileContent(uploadFile.getFile());
            Map<String,Object> map = insureManager.getStaffIdsByFile(hrId,fileData);
            String importResult = (String) map.get("result");
            String message = (String) map.get("message");
            if("true".equals(importResult)){
                staffIds = (List<Long>) map.get("staffIds");
            }else{
                modelMap.addAttribute("result", result);
                modelMap.addAttribute("message", message);
                AjaxUtils.doAjaxResponseOfMap(response, modelMap);
                return null;
            }
        }
        return createInsureOrder(productId, attributeValueId, hrId, staffIds, response, modelMap, result);
    }

	private String createInsureOrder(Long productId,Long attributeValueId, Long hrId, List<Long> staffIds,HttpServletResponse response,ModelMap modelMap,boolean result) throws Exception{
	  //去重
        Set<Long> set = new HashSet<Long>();
        boolean flag = set.addAll(staffIds);
        if(flag){
            staffIds = new ArrayList<Long>(set);
        }

        if(staffIds==null||staffIds.size()==0){
            modelMap.addAttribute("result", result);
            modelMap.addAttribute("message", "没有找到员工");
            AjaxUtils.doAjaxResponseOfMap(response, modelMap);
            return null;
        }
        long count = insureNotCompleteManager.insertNotCompleteStaffInfo(productId, attributeValueId, hrId, staffIds);
        if(count>0){
            modelMap.addAttribute("result", result);
            modelMap.addAttribute("count",count);
            modelMap.addAttribute("downloadUrl", "/insure/exportNotCompleteInfo?productId="+productId+"&attributeValueId="+attributeValueId);
            modelMap.addAttribute("message", "有"+count+"位员工的投保信息不全，请您提醒他们尽快完善投保信息后再进行投保!");
            AjaxUtils.doAjaxResponseOfMap(response, modelMap);
            return null;
        }
        //插入核保单表
        insureOrderManager.insertInsureOrder(productId, attributeValueId, hrId, staffIds);
        result = true;
        modelMap.addAttribute("result", result);
        modelMap.addAttribute("message", "请等待保单核价！");
        AjaxUtils.doAjaxResponseOfMap(response, modelMap);
        return null;
	}
	/**
	 * 导出信息不完整的员工
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/exportNotCompleteInfo")
    public String exportNotCompleteInfo(HttpServletRequest request, HttpServletResponse response) {
        Long productId = Long.parseLong(request.getParameter("productId"));
        Long attributeValueId = Long.parseLong(request.getParameter("attributeValueId"));
        Long hrId = FrameworkContextUtils.getCurrentUserId();
        String[] titles = { "姓名", "工号", "部门" };
        List<Object[]> datas = new ArrayList<Object[]>();
        InsureNotComplete inc = new InsureNotComplete();
        inc.setProductId(productId);
        inc.setAttributeValueId(attributeValueId);
        inc.setHrId(hrId);
        List<InsureNotComplete> insureNotCompletes = insureNotCompleteManager.getBySample(inc);
        for(InsureNotComplete in:insureNotCompletes){
            Object[] arr = new Object[3];
            arr[0]=in.getStaffName();
            arr[1]=in.getStaffWorkNo();
            arr[2]=in.getStaffDepartment();
            datas.add(arr);
        }
        String exportName = "";
        exportName = FileUpDownUtils.encodeDownloadFileName(request, "信息不完善员工的名单_" + new Date().getTime() + ".xls");
        ExcelUtil excelUtil = new ExcelUtil();
        try {
            excelUtil.exportExcel(response, datas, titles, exportName);
        } catch (Exception e) {
            LOGGER.error("导出excel异常\n"+e);
        }
        return null;
    }

	@RequestMapping("searchDepartmentUsers")
    public String searchDepartmentUsers(HttpServletRequest request,HttpServletResponse response, String keywords) throws Exception {
        String backFunc = request.getParameter("backFunc");
	    if(StringUtils.isNotBlank(keywords)){
            User user = FrameworkContextUtils.getCurrentUser();
            Long companyId = user.getCompanyId();
            Long organizationId = user.getOrganizationId();
            List<Staff> all = staffManager.searchOrganizationStaffs(companyId, organizationId, keywords);
            String result = "{" + getStaffJson(all) + "}";
            AjaxUtils.doAjaxResponse(response, backFunc + "(" + result + ")");
        }
        return null;
    }

	private String getStaffJson(List<Staff> staffs) {
        StringBuilder result = new StringBuilder();
        for(int i=0; i< staffs.size();i++){
            Staff staff = staffs.get(i);
            result.append("'").append(staff.getObjectId()).append("':").append("'").append(staff.getWorkNo()).append("|").append(staff.getUserName()).append("'");
            if(i!=staffs.size()-1){
                result.append(",");
            }
        }
        return result.toString();
    }

	@RequestMapping("/queryPrice")
    public String queryPrice(HttpServletRequest request,HttpServletResponse response, ModelMap modelMap){
	    boolean result = false;
	    Long productId = Long.parseLong(request.getParameter("productId"));
        Long attributeValueId = Long.parseLong(request.getParameter("attributeValueId"));
        String[] countStr = request.getParameterValues("count");
        Long companyId = FrameworkContextUtils.getCurrentUser().getCompanyId();
        int totalCount = 0;
        double totalPrice = 0;
        if(countStr==null||countStr.length==0){
            modelMap.addAttribute("result",result);
            modelMap.addAttribute("message","人数不能为空");
            return "jsonView";
        }else{
            String[] ageStr = request.getParameterValues("age");
            String[] jobStr = request.getParameterValues("job");
            String[] socialStr = request.getParameterValues("social");
            for(int i=0;i<countStr.length;i++){
                int count = Integer.parseInt(countStr[i]);
                totalCount = totalCount+count;
                Integer age = null;
                Integer job = null;
                Integer social = null;
                if(ageStr!=null&&ageStr.length!=0){
                    age = Integer.parseInt(ageStr[i]);
                }
                if(jobStr!=null&&jobStr.length!=0){
                    job = Integer.parseInt(jobStr[i]);
                }
                if(socialStr!=null&&socialStr.length!=0){
                    social = Integer.parseInt(socialStr[i]);
                }
                Double price = insureRangeManager.queryInsurePrice(companyId, productId, attributeValueId, age, job, social);
                if(price==null){
                    modelMap.addAttribute("result",result);
                    modelMap.addAttribute("message","不能查询到具体的保险商品，请联系运营人员");
                    return "jsonView";
                }
                totalPrice = totalPrice + price*count;
            }
        }
        modelMap.addAttribute("result",true);
        modelMap.addAttribute("totalCount",totalCount);
        modelMap.addAttribute("totalPrice",totalPrice);
        modelMap.addAttribute("message","操作成功");
        return "jsonView";
    }
}
