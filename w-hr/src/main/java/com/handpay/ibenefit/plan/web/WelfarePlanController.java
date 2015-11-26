package com.handpay.ibenefit.plan.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.CRUDUtils;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.framework.config.GlobalConfig;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.service.ICompanyDepartmentManager;
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
import com.handpay.ibenefit.plan.entity.WelfareSubPlanItem;
import com.handpay.ibenefit.plan.service.IWelfarePlanManager;
import com.handpay.ibenefit.plan.service.IWelfarePlanOrderManager;
import com.handpay.ibenefit.plan.service.IWelfarePlanStaffManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanItemManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanManager;
import com.handpay.ibenefit.plan.service.IWelfareSubPlanStaffItemManager;
import com.handpay.ibenefit.product.entity.SkuPublish;
import com.handpay.ibenefit.product.service.ISkuPublishManager;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.welfare.entity.WelfarePackage;
import com.handpay.ibenefit.welfare.service.IWelfareItemManager;
import com.handpay.ibenefit.welfare.service.IWelfarePackageManager;

@Controller
@RequestMapping("welfarePlan")
public class WelfarePlanController extends BaseController{

	@Reference(version ="1.0")
	private IWelfarePlanManager welfarePlanManager;

	@Reference(version = "1.0")
	private IWelfareSubPlanManager welfareSubPlanManager;

	@Reference(version = "1.0")
	private IWelfareSubPlanItemManager welfareSubPlanItemManager;

	@Reference(version = "1.0")
	private IWelfarePlanStaffManager welfarePlanStaffManager;

	@Reference(version = "1.0")
	private IWelfareSubPlanStaffItemManager welfareSubPlanStaffItemManager;

	@Reference(version = "1.0")
	private IUserManager userManager;

	@Reference(version = "1.0")
    private IOrderManager orderManager;

	@Reference(version ="1.0")
    private IWelfarePlanOrderManager welfarePlanOrderManager;

	@Reference(version ="1.0")
    private ISubOrderManager subOrderManager;

	@Reference(version ="1.0")
    private IOrderSkuManager orderSkuManager;

	@Reference(version ="1.0")
    private IWelfareItemManager welfareItemManager;

	@Reference(version ="1.0")
    private ICompanyDepartmentManager companyDepartmentManager;

	@Reference(version ="1.0")
    private IWelfarePackageManager welfarePackageManager;

	@Reference(version ="1.0")
    private ISkuPublishManager skuPublishManager;

	@RequestMapping("page")
	public String page(HttpServletRequest request, HttpServletResponse response) throws Exception {
		PageSearch pageSearch = PageUtils.preparePage(request, WelfarePlan.class, true);
		pageSearch.getFilters().add(new PropertyFilter("WelfarePlan","EQI_deleted",IBSConstants.STATUS_NO + ""));
		User user=FrameworkContextUtils.getCurrentUser();
        pageSearch.getFilters().add(new PropertyFilter(WelfarePlan.class.getName(),"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        if(UserUtils.isCompanyHR()){
            //if (user.getOrganizationId()!=null) {
                pageSearch.getFilters().add(new PropertyFilter(WelfarePlan.class.getName(),"EQL_createdBy",user.getObjectId() + ""));
            //}
        }
		pageSearch = welfarePlanManager.find(pageSearch);
		List<WelfarePlan> list = pageSearch.getList();
		for(WelfarePlan w:list){
		    w.setRejectHeadCount(w.getHeadCount()-w.getConfirmHeadCount());
		}
		request.setAttribute("pageSearch", pageSearch);
		request.setAttribute("search_EQI_year", request.getParameter("search_EQI_year"));
		request.setAttribute("allYear", welfarePlanManager.getAllYear(user.getCompanyId()));
		return "plan/listWelfarePlan";
	}

	@RequestMapping("create")
	public String create(HttpServletRequest request, ModelMap modelMap) throws Exception {

		return "plan/editWelfarePlan";
	}


	@RequestMapping("edit/{planId}")
	public String edit(HttpServletRequest request, ModelMap modelMap, @PathVariable Long planId ) throws Exception {
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
	    request.setAttribute("entity", welfarePlan);

	    short status = welfarePlan.getStatus();
        if(status!=WelfarePlan.STATUS_DRAFT&&status!=WelfarePlan.STATUS_WAIT_PUBLISH){
            return "redirect:/welfarePlan/page";
        }
	    User user = FrameworkContextUtils.getCurrentUser();
	    if(!user.getObjectId().equals(welfarePlan.getCreatedBy())){
	        return "redirect:/welfarePlan/page";
	    }
        return "plan/editWelfarePlan";
	}


	@RequestMapping("save")
	public String save(HttpServletRequest request, ModelMap modelMap, WelfarePlan welfarePlan) throws Exception {
		if(welfarePlan.getObjectId()==null){
			User user = FrameworkContextUtils.getCurrentUser();
			welfarePlan.setCompanyId(user.getCompanyId());
			if(UserUtils.isCompanyHR()){
				welfarePlan.setOrganizationId(user.getOrganizationId());
			}
			welfarePlan.setStatus(WelfarePlan.STATUS_DRAFT);
			welfarePlan.setHeadCount(0);
			welfarePlan.setTotalAmount(0D);
			welfarePlan.setRejectHeadCount(0);
			welfarePlan.setConfirmHeadCount(0);
		}
		CRUDUtils.prepareSave(welfarePlan);
		welfarePlan = welfarePlanManager.save(welfarePlan);
		return "redirect:/welfareSubPlan/edit/" + welfarePlan.getObjectId();
	}

	@RequestMapping("delete/{planId}")
	public String delete(HttpServletRequest request, ModelMap modelMap,  @PathVariable Long planId) throws Exception {
		boolean result = false;
		if(planId!=null){
			WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
			if(welfarePlan.getStatus()==WelfarePlan.STATUS_DRAFT){
				welfarePlanManager.delete(welfarePlan);
				result = true;
			}
		}
		modelMap.addAttribute("result", result);
		return "jsonView";
	}

	@RequestMapping("/getPlans/{year}")
	public String getPlans(HttpServletRequest request, ModelMap modelMap,@PathVariable Short year) throws Exception {
	    WelfarePlan wp = new WelfarePlan();
	    wp.setYear(year);
	    wp.setDeleted(IBSConstants.STATUS_NO);
	    wp.setCompanyId(FrameworkContextUtils.getCurrentUser().getCompanyId());
        if(UserUtils.isCompanyHR()){
            wp.setCreatedBy(FrameworkContextUtils.getCurrentUser().getObjectId());
        }

	    List<WelfarePlan> plans = welfarePlanManager.getBySample(wp);
	    Collections.sort(plans, new Comparator<WelfarePlan>() {
            @Override
            public int compare(WelfarePlan o1, WelfarePlan o2) {
                return (int) -(o1.getCreatedOn().getTime()-o2.getCreatedOn().getTime());
            }
        });
	    modelMap.addAttribute("plans", plans);
	    modelMap.addAttribute("result", true);
		return "jsonView";
	}

	@RequestMapping("/copy/{planId}")
	public String copy(HttpServletRequest request, ModelMap modelMap, @PathVariable Long planId) throws Exception {
	    //复制总计划
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
	    welfarePlan.setObjectId(null);
	    welfarePlan.setBeginSelectDate(null);
	    welfarePlan.setEndSelectDate(null);
	    welfarePlan.setTotalAmount(0.0);
	    welfarePlan.setHeadCount(0);
	    welfarePlan.setConfirmHeadCount(0);
	    welfarePlan.setRejectHeadCount(0);
	    welfarePlan.setStatus(WelfarePlan.STATUS_DRAFT);
	    CRUDUtils.prepareSave(welfarePlan);
        welfarePlan = welfarePlanManager.save(welfarePlan);
        //复制子计划
        //1.得到所有的子计划
        WelfareSubPlan wsp = new WelfareSubPlan();
        wsp.setPlanId(planId);
        wsp.setDeleted(IBSConstants.STATUS_NO);
        List<WelfareSubPlan> subPlans = welfareSubPlanManager.getBySample(wsp);
        Long newPlanId = welfarePlan.getObjectId();
        for(WelfareSubPlan subPlan:subPlans){
            //复制子计划
            Long oldSubPlanId = subPlan.getObjectId();
            subPlan.setObjectId(null);
            subPlan.setPublishDate(null);
            subPlan.setOrderId(null);
            subPlan.setPlanId(newPlanId);
            CRUDUtils.prepareSave(subPlan);
            subPlan = welfareSubPlanManager.save(subPlan);
            Long newSubPlanId = subPlan.getObjectId();
            //复制子计划项目
            //1.查询出所有的子计划项目
            WelfareSubPlanItem wspi = new WelfareSubPlanItem();
            wspi.setSubPlanId(oldSubPlanId);
            List<WelfareSubPlanItem> items = welfareSubPlanItemManager.getBySample(wspi);
            for(WelfareSubPlanItem item:items){
                item.setSubPlanId(newSubPlanId);
                item.setPlanId(newPlanId);
                item.setCount((short)0);
                item.setSubOrderId(null);
                welfareSubPlanItemManager.save(item);
            }
        }
        modelMap.addAttribute("result", true);
        modelMap.addAttribute("newPlanId", newPlanId);
		return "jsonView";
	}


	@RequestMapping("/publish/{planId}")
	public String publish(HttpServletRequest request,ModelMap modelMap, @PathVariable Long planId) throws Exception {
		WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
		short result = 0;
		if(welfarePlan.getStatus() == WelfarePlan.STATUS_DRAFT){
			//员工额度是否设置
			WelfarePlanStaff sample = new WelfarePlanStaff();
			sample.setPlanId(planId);
			long total = welfarePlanStaffManager.getObjectCount(sample);
			if(total == 0){
				result = 4;//没选择员工
			}
			//是否大于最低额度
			int count = welfarePlanStaffManager.getInsufficientStaff(planId);
			if( count>0 ){
				modelMap.addAttribute("count", count);
				result = 2;//员工额度小于最低发放额度
			}else if(result!=4&&result!=2){
			    //查询总计划下面是否有子计划
			    WelfareSubPlan subPlan = new WelfareSubPlan();
			    subPlan.setPlanId(planId);
			    subPlan.setDeleted((int)IBSConstants.NOT_DELETE);
			    List<WelfareSubPlan> subPlans = welfareSubPlanManager.getBySample(subPlan);
			    if(subPlans.size()<=0){
			        result = 5;
			    }else{
			        //查询子计划下面是否有子计划选项
			        for(WelfareSubPlan sub:subPlans){
			            WelfareSubPlanItem item = new WelfareSubPlanItem();
			            item.setSubPlanId(sub.getObjectId());
			            Long itemCount = welfareSubPlanItemManager.getObjectCount(item);
			            if(itemCount==null||itemCount==0){
			                result = 6;
			                break;
			            }
			        }
			        if(result!=6){
    	                welfarePlan.setStatus(WelfarePlan.STATUS_WAIT_PUBLISH);
    	                welfarePlanManager.save(welfarePlan);
    	                result = 1;//发布成功
			        }
			    }
			}
		}
		modelMap.addAttribute("result", result);
		return "jsonView";
	}

	@RequestMapping("staffSelect/{planId}")
	public String staffSelected(HttpServletRequest request,ModelMap modelMap, @PathVariable Long planId) throws Exception {
		WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
		request.setAttribute("welfarePlan", welfarePlan);
		boolean canBack = true;
		if(welfarePlan.getBeginSelectDate()!=null){
			canBack = welfarePlan.getBeginSelectDate().compareTo(new Date())>=0;
		}
		request.setAttribute("canBack", canBack);
		return "plan/staffSelect";
	}

	@RequestMapping("exportQuota/{planId}")
	public String exportQuota(HttpServletRequest request,HttpServletResponse response, @PathVariable Long planId) throws Exception {
		WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
		User user = FrameworkContextUtils.getCurrentUser();
		if(welfarePlan.getCompanyId().equals(user.getCompanyId())){
			if(UserUtils.isCompanyAdmin() || user.getOrganizationId().equals(welfarePlan.getOrganizationId())){
				PageSearch pageSearch = PageUtils.getPageSearch(request);
				pageSearch.getFilters().add(new PropertyFilter("","EQL_planId",planId + ""));
				pageSearch.setPageSize(Integer.MAX_VALUE);
				pageSearch = welfarePlanStaffManager.findWelfarePlanStaff(pageSearch);
				String exportName = welfarePlan.getName() + "员工额度.xls";
				Double point = userManager.getUserBalance(FrameworkContextUtils.getCurrentUserId());

				HSSFWorkbook wb=new HSSFWorkbook();//excel文件对象
		        HSSFSheet sheet=wb.createSheet("员工导入模板");
		        CellStyle style = getStyle(wb);
		        CellRangeAddress range= new CellRangeAddress(0, 2, 0, 3);
		        sheet.addMergedRegion(range);
		        Row row0 = sheet.createRow(0);
		        Cell cell00 = row0.createCell(0, Cell.CELL_TYPE_STRING);
		        cell00.setCellStyle(style);
		        cell00.setCellValue("说明：当前可用"+request.getSession().getAttribute("s_welfarePointName")+"：" + point + ",导入的时候列的顺序不能修改。");

		        Row row1 = sheet.createRow(3);
		        Cell cell0 = row1.createCell(0, Cell.CELL_TYPE_STRING);
		        cell0.setCellStyle(style);
		        cell0.setCellValue("部门");
		        sheet.autoSizeColumn(0);

		        Cell cell1 = row1.createCell(1, Cell.CELL_TYPE_STRING);
		        cell1.setCellStyle(style);
		        cell1.setCellValue("员工工号");
		        sheet.autoSizeColumn(1);

		        Cell cell2 = row1.createCell(2, Cell.CELL_TYPE_STRING);
		        cell2.setCellStyle(style);
		        cell2.setCellValue("姓名");
		        sheet.autoSizeColumn(2);

		        Cell cell3 = row1.createCell(3, Cell.CELL_TYPE_STRING);
		        cell3.setCellStyle(style);
		        cell3.setCellValue("额度");
		        sheet.autoSizeColumn(3);
		        short row = 4;
		        for(WelfarePlanStaff staff : (List<WelfarePlanStaff>)pageSearch.getList()){
		        	row1 = sheet.createRow(row++);
		            cell0 = row1.createCell(0, Cell.CELL_TYPE_STRING);
		            cell0.setCellStyle(style);
		            cell0.setCellValue(staff.getDepartmentName());
		            sheet.autoSizeColumn(0);

		            cell1 = row1.createCell(1, Cell.CELL_TYPE_STRING);
		            cell1.setCellStyle(style);
		            cell1.setCellValue(staff.getWorkNo());
		            sheet.autoSizeColumn(1);

		            cell2 = row1.createCell(2, Cell.CELL_TYPE_STRING);
		            cell2.setCellStyle(style);
		            cell2.setCellValue(staff.getUserName());
		            sheet.autoSizeColumn(2);

		            cell3 = row1.createCell(3, Cell.CELL_TYPE_STRING);
		            cell3.setCellStyle(style);
		            cell3.setCellValue(staff.getQuota());
		            sheet.autoSizeColumn(3);
		        }
		        FileUpDownUtils.setDownloadResponseHeaders(response, FileUpDownUtils.encodeDownloadFileName(request, exportName));
		        wb.write(response.getOutputStream());
			}
		}
		return null;
	}

	private CellStyle getStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		// 设置单元格字体
		Font headerFont = workbook.createFont(); // 字体
		headerFont.setFontHeightInPoints((short) 14);
		headerFont.setColor(HSSFColor.BLACK.index);
		headerFont.setFontName("宋体");
		style.setFont(headerFont);
		style.setWrapText(true);

		// 设置单元格边框及颜色
		style.setBorderBottom((short) 1);
		style.setBorderLeft((short) 1);
		style.setBorderRight((short) 1);
		style.setBorderTop((short) 1);
		style.setWrapText(true);
		return style;
	}

	@RequestMapping("exportUserSelect/{planId}")
	public String exportUserSelect(HttpServletRequest request, HttpServletResponse response, @PathVariable Long planId) throws Exception {
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
        User user = FrameworkContextUtils.getCurrentUser();
        if(welfarePlan.getCompanyId().equals(user.getCompanyId())){
            if(UserUtils.isCompanyAdmin() || user.getOrganizationId().equals(welfarePlan.getOrganizationId())){
                PageSearch pageSearch = PageUtils.getPageSearch(request);
                pageSearch.getFilters().add(new PropertyFilter("","EQL_planId",planId + ""));
                pageSearch.setPageSize(Integer.MAX_VALUE);
                pageSearch = welfareSubPlanStaffItemManager.exportWelfarePlanStaffSelect(pageSearch);
                String exportName = welfarePlan.getName() + "员工选择.xls";
                Double point = userManager.getUserBalance(FrameworkContextUtils.getCurrentUserId());

                HSSFWorkbook wb=new HSSFWorkbook();//excel文件对象
                HSSFSheet sheet=wb.createSheet("员工导入模板");
                CellStyle style = getStyle(wb);
                CellRangeAddress range= new CellRangeAddress(0, 2, 0, 3);
                sheet.addMergedRegion(range);
                Row row0 = sheet.createRow(0);
                Cell cell00 = row0.createCell(0, Cell.CELL_TYPE_STRING);
                cell00.setCellStyle(style);
                cell00.setCellValue("说明：当前可用"+request.getSession().getAttribute("s_welfarePointName")+"：" + point);

                Row row1 = sheet.createRow(3);
                Cell cell0 = row1.createCell(0, Cell.CELL_TYPE_STRING);
                cell0.setCellStyle(style);
                cell0.setCellValue("年份");
                sheet.autoSizeColumn(0);

                Cell cell1 = row1.createCell(1, Cell.CELL_TYPE_STRING);
                cell1.setCellStyle(style);
                cell1.setCellValue("员工工号");
                sheet.autoSizeColumn(1);

                Cell cell2 = row1.createCell(2, Cell.CELL_TYPE_STRING);
                cell2.setCellStyle(style);
                cell2.setCellValue("子计划名称");
                sheet.autoSizeColumn(2);

                Cell cell3 = row1.createCell(3, Cell.CELL_TYPE_STRING);
                cell3.setCellStyle(style);
                cell3.setCellValue("选择项");
                sheet.autoSizeColumn(3);

                Cell cell4 = row1.createCell(4, Cell.CELL_TYPE_STRING);
                cell4.setCellStyle(style);
                cell4.setCellValue("姓名");
                sheet.autoSizeColumn(4);

                Cell cell5 = row1.createCell(5, Cell.CELL_TYPE_STRING);
                cell5.setCellStyle(style);
                cell5.setCellValue("部门");
                sheet.autoSizeColumn(5);

                Cell cell6 = row1.createCell(6, Cell.CELL_TYPE_STRING);
                cell6.setCellStyle(style);
                cell6.setCellValue("额度");
                sheet.autoSizeColumn(6);

                short row = 4;
                for(Map map : (List<Map>)pageSearch.getList()){
                    row1 = sheet.createRow(row++);
                    //年份
                    cell0 = row1.createCell(0, Cell.CELL_TYPE_STRING);
                    cell0.setCellStyle(style);
                    cell0.setCellValue(welfarePlan.getYear().toString());
                    sheet.autoSizeColumn(0);
                    //员工工号
                    cell1 = row1.createCell(1, Cell.CELL_TYPE_STRING);
                    cell1.setCellStyle(style);
                    cell1.setCellValue(map.get("WORK_NO").toString());
                    sheet.autoSizeColumn(1);
                    //子计划名称
                    cell2 = row1.createCell(2, Cell.CELL_TYPE_STRING);
                    cell2.setCellStyle(style);
                    cell2.setCellValue(map.get("SUB_PLAN_NAME").toString());
                    sheet.autoSizeColumn(2);
                    //选择项名称
                    cell3 = row1.createCell(3, Cell.CELL_TYPE_STRING);
                    cell3.setCellStyle(style);
                    Long goodsId = Long.parseLong(map.get("GOODS_ID").toString());
                    Short type = Short.parseShort(map.get("TYPE").toString());
                    if(type.equals((short)1)){
                        WelfarePackage wp = welfarePackageManager.getPackagePrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), goodsId);
                        cell3.setCellValue(wp.getPackageName());
                    }else if(type.equals((short)3)){
                        SkuPublish sku = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), goodsId);
                        cell3.setCellValue(sku.getName()+"("+(sku.getAttributeValue1()==null?"":sku.getAttributeValue1())+(sku.getAttributeValue2()==null?"":(","+sku.getAttributeValue2()))+")");
                    }else if(type.equals((short)5)){
                        cell3.setCellValue(map.get("GOODS_NAME").toString());
                    }
                    sheet.autoSizeColumn(3);
                    //姓名
                    cell4 = row1.createCell(4, Cell.CELL_TYPE_STRING);
                    cell4.setCellStyle(style);
                    cell4.setCellValue(map.get("USER_NAME").toString());
                    sheet.autoSizeColumn(4);
                    //部门
                    cell5 = row1.createCell(5, Cell.CELL_TYPE_STRING);
                    cell5.setCellStyle(style);
                    Long departmentId = Long.parseLong(map.get("DEPARTMENT_ID").toString());
                    CompanyDepartment department = null;
                    if(departmentId!=null){
                        department = companyDepartmentManager.getByObjectId(departmentId);
                    }
                    if(department!=null){
                        cell5.setCellValue(department.getName());
                    }else{
                        cell5.setCellValue("");
                    }
                    sheet.autoSizeColumn(5);
                    //额度
                    cell6 = row1.createCell(6, Cell.CELL_TYPE_STRING);
                    cell6.setCellStyle(style);
                    cell6.setCellValue(map.get("QUOTA").toString());
                    sheet.autoSizeColumn(6);
                }
                FileUpDownUtils.setDownloadResponseHeaders(response, FileUpDownUtils.encodeDownloadFileName(request, exportName));
                wb.write(response.getOutputStream());
            }
        }
        return null;
	}

	@RequestMapping("/generateOrder/{planId}")
    public String generateOrder(HttpServletRequest request, HttpServletResponse response, @PathVariable Long planId) throws Exception {
	    WelfarePlan welfarePlan = welfarePlanManager.getByObjectId(planId);
	    short status = welfarePlan.getStatus();
	    if(status==WelfarePlan.STATUS_WAIT_PAY){
	        return "redirect:"+GlobalConfig.getPayDomain()+"/onlinePay/welfarePlanOrderPayCenter?planId="+planId;
	    }else if(status==WelfarePlan.STATUS_WAIT_CONFIRMED){
	        boolean result = welfarePlanManager.confirmOrder(planId, FrameworkContextUtils.getCurrentUserId());
	        if(result){
	             return "redirect:"+GlobalConfig.getPayDomain()+"/onlinePay/welfarePlanOrderPayCenter?planId="+planId;
	        }else{
	             return "redirect:/welfarePlan/page";
	        }
	    }else{
	        return "redirect:/welfarePlan/page";
	    }
    }

	@RequestMapping("/payDetail/{planId}")
    public String payDetail(HttpServletRequest request, HttpServletResponse response, @PathVariable Long planId) throws Exception {
        WelfarePlan plan = welfarePlanManager.getByObjectId(planId);
        request.setAttribute("welfarePlan", plan);
        List<WelfareSubPlan> wsps = welfareSubPlanManager.getHaveOrderSubPlan(planId);

        if(plan.getOverplusStrategy()==WelfarePlan.OVERPLUSSTRATEGY_TRANSFER_INTEGRAL){
            WelfarePlanOrder wp = new WelfarePlanOrder();
            wp.setPlanId(planId);
            List<WelfarePlanOrder> wpos = welfarePlanOrderManager.getBySample(wp);
            for(WelfarePlanOrder w:wpos){
                WelfareSubPlan wsp = new WelfareSubPlan();
                wsp.setName("剩余额度转"+(request.getSession().getAttribute("s_welfarePointName")));
                wsp.setStatus(w.getStatus());
                wsp.setOrderId(w.getOrderId());
                WelfarePlanStaff wps = new WelfarePlanStaff();
                wps.setPlanId(planId);
                wps.setStatus(WelfarePlanStaff.STATUS_HR_CONFIRMED);
                Long headCount = welfarePlanStaffManager.getObjectCount(wps);
                wsp.setHeadCount(headCount);
                wsps.add(wsp);
            }
        }
        double totalAmount = 0.0;
        double paid = 0.0;
        boolean isCanPay = false;
        for(WelfareSubPlan w:wsps){
            Long orderId = w.getOrderId();
            List<SubOrder> subOrders = subOrderManager.getSubOrderNoByGeneralOrderID(orderId);
            List<OrderSku> orderSs = new ArrayList<OrderSku>();
            for(SubOrder s:subOrders){
                OrderSku os = new OrderSku();
                os.setSubOrderId(s.getObjectId());
                List<OrderSku> orderSkus = orderSkuManager.getBySample(os);
                OrderSku orderSku = orderSkus.get(0);
                orderSs.add(orderSku);
                double subTotal = orderSku.getProductPrice()*orderSku.getProductCount();
                totalAmount = totalAmount+ subTotal;
                if(w.getStatus()>=WelfareSubPlan.STATUS_PAID){
                    paid = paid+subTotal;
                    isCanPay = isCanPay||false;
                }else{
                    isCanPay = isCanPay||true;
                }
            }
            w.setOrderSkus(orderSs);
            Long welfareItemId = w.getWelfareItemId();
            if(welfareItemId!=null){
                w.setWelfareItemName(welfareItemManager.getByObjectId(welfareItemId).getItemName());
            }
            //取总订单的信息
            Order order = orderManager.getByObjectId(w.getOrderId());
            w.setOrder(order);
            //判断是否已经发放了
            if(w.getPublishDate()!=null&&new Date().getTime()-w.getPublishDate().getTime()>0){
                //已经发放
                w.setIsPublish(true);
            }else{
                w.setIsPublish(false);
            }
        }
        request.setAttribute("welfareSubPlans", wsps);
        request.setAttribute("totalAmount", totalAmount);
        request.setAttribute("paid", paid);
        request.setAttribute("unpaid", totalAmount-paid);
        request.setAttribute("isCanPay", isCanPay);
        return "plan/payDetail";
    }
}
