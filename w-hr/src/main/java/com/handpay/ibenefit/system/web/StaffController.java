package com.handpay.ibenefit.system.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
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
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.entity.CompanyFunction;
import com.handpay.ibenefit.member.entity.CompanyPublished;
import com.handpay.ibenefit.member.entity.Staff;
import com.handpay.ibenefit.member.service.ICompanyDepartmentManager;
import com.handpay.ibenefit.member.service.ICompanyFunctionManager;
import com.handpay.ibenefit.member.service.ICompanyPublishedManager;
import com.handpay.ibenefit.member.service.IProjectTeamManager;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;

@Controller
@RequestMapping("staff")
public class StaffController {

	@Reference(version = "1.0", timeout= 60000)
	private IStaffManager staffManager;

	@Reference(version="1.0")
	private IUserManager userManager;

	@Reference(version="1.0")
	private ICompanyDepartmentManager companyDepartmentManager;

	@Reference(version = "1.0")
	private IProjectTeamManager projectTeamManager;

	@Reference(version = "1.0")
	private ICompanyPublishedManager companyPublishedManager;

	@Reference(version = "1.0")
	private ICompanyFunctionManager companyFunctionManager;

	@RequestMapping("getPoint")
	public String getPoint(HttpServletRequest request,ModelMap modelMap) throws Exception {
		User user = userManager.getByObjectId(FrameworkContextUtils.getCurrentUserId());
		if(user!=null){
			modelMap.addAttribute("point", user.getAccountBalance());
		}
		return "jsonView";
	}

	@RequestMapping("getHeadCount")
	public String getHeadCount(HttpServletRequest request,ModelMap modelMap) throws Exception {
		Long organizationId = FrameworkContextUtils.getCurrentUser().getOrganizationId();
		long result = 0;
		if(organizationId!=null){
			result = staffManager.getHeadCountByOrganizationId(organizationId);
		}
		modelMap.addAttribute("headCount", result);
		return "jsonView";
	}

	@RequestMapping("getDepartmentUsers")
	public String getDepartmentUsers(HttpServletRequest request,HttpServletResponse response) throws Exception {
		User user = FrameworkContextUtils.getCurrentUser();
		List<Staff> all = null;
		if(UserUtils.isCompanyHR()){
			all = staffManager.getAllStaffs(user.getCompanyId(),user.getOrganizationId());
		}else{
			all = staffManager.getAllStaffs(user.getCompanyId(),null);
		}
		Map<Long,List<Staff>> map = new HashMap<Long,List<Staff>>();
		for(Staff staff : all){
			List<Staff> staffs = map.get(staff.getDepartmentId());
			if(staffs==null){
				staffs = new ArrayList<Staff>();
				staffs.add(staff);
				map.put(staff.getDepartmentId(), staffs);
			}else{
				staffs.add(staff);
			}
		}
		 AjaxUtils.doAjaxResponse(response, getStaffJsonByMap(map));
		 return null;
	}

	@RequestMapping("searchDepartmentUsers")
	public String searchDepartmentUsers(HttpServletRequest request,HttpServletResponse response, String keywords) throws Exception {
		if(StringUtils.isNotBlank(keywords)){
			User user = FrameworkContextUtils.getCurrentUser();
			Long companyId = user.getCompanyId();
			Long organizationId = user.getOrganizationId();
			List<Staff> all = staffManager.searchOrganizationStaffs(companyId, organizationId, keywords);
			AjaxUtils.doAjaxResponse(response, "{" + getStaffJson(all) + "}");
		}
		return null;
	}


	@RequestMapping("getAccountBalance")
	public String getAccountBalance(ModelMap modelMap) throws Exception {
		User user = userManager.getByObjectId(FrameworkContextUtils.getCurrentUserId());
		modelMap.addAttribute("accountBalance", user.getAccountBalance());
		return "jsonView";
	}

	@RequestMapping("exportStaffTemplate")
	public String exportStaffTemplate(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap, String pointName) throws Exception {
		Long organizationId = FrameworkContextUtils.getCurrentUser().getOrganizationId();
		Long companyId = FrameworkContextUtils.getCurrentUser().getCompanyId();
		CompanyPublished companyPublished = companyPublishedManager.getByObjectId(companyId);
		List<CompanyFunction> functions = companyFunctionManager.getCompanyFunctionsByCompanyId(companyId);
		boolean hasLevel = false;
		for(CompanyFunction function : functions){
			if(function.getFunctionId()==4){
				hasLevel = true;
				break;
			}
		}
		List<CompanyDepartment> departments = companyDepartmentManager.getCompanyDepartmentsByCompanyId(companyId);;
		//分权限取数据
		if(UserUtils.isCompanyHR()){
			departments = CompanyDepartmentUtils.getSubCompanyDepartment(departments, organizationId);
			departments.add(companyDepartmentManager.getByObjectId(organizationId));
		}
		CompanyDepartmentUtils.fillALlFullName(departments);
		User user = userManager.getByObjectId(FrameworkContextUtils.getCurrentUserId());

		HSSFWorkbook wb=new HSSFWorkbook();//excel文件对象
        HSSFSheet sheet=wb.createSheet("员工导入模板");
        CellStyle style = getStyle(wb);
        CellRangeAddress range= new CellRangeAddress(0, 2, 0, 3);
        sheet.addMergedRegion(range);
        Row row0 = sheet.createRow(0);
        Cell cell00 = row0.createCell(0, Cell.CELL_TYPE_STRING);
        cell00.setCellStyle(style);
        cell00.setCellValue("说明：当前可用积分：" + user.getAccountBalance() + ",导入的时候列的顺序不能修改。");

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

        if(StringUtils.isBlank(pointName)){
        	pointName = "积分";
        }
        Cell cell3 = row1.createCell(3, Cell.CELL_TYPE_STRING);
        cell3.setCellStyle(style);
        cell3.setCellValue(pointName);
        sheet.autoSizeColumn(3);

        if(hasLevel){
	        Cell cell4 = row1.createCell(4, Cell.CELL_TYPE_STRING);
	        cell4.setCellStyle(style);
	        cell4.setCellValue("等级");
	        sheet.autoSizeColumn(4);
        }
        List<Staff> staffs = staffManager.getAllStaffs(companyId, organizationId);
        short row = 4;
        for(Staff staff : staffs){
        	row1 = sheet.createRow(row++);
            cell0 = row1.createCell(0, Cell.CELL_TYPE_STRING);
            cell0.setCellStyle(style);
            if(staff.getDepartmentId()!=null){
            	cell0.setCellValue(getDepartmentFullName(staff.getDepartmentId(), departments));
            }else{
            	cell0.setCellValue("");
            }
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
            cell3.setCellValue("");
            sheet.autoSizeColumn(3);
            if(hasLevel){
    	        Cell cell4 = row1.createCell(4, Cell.CELL_TYPE_STRING);
    	        cell4.setCellStyle(style);
    	        cell4.setCellValue(staff.getStaffLevel() + "");
    	        sheet.autoSizeColumn(4);
            }
        }
        String fileName = companyPublished.getCompanyName() + "员工导入模板" + ".xls";
        FileUpDownUtils.setDownloadResponseHeaders(response, FileUpDownUtils.encodeDownloadFileName(request, fileName));
        wb.write(response.getOutputStream());
		return null;
	}

	private String getStaffJsonByMap(Map<Long, List<Staff>> map) {
		StringBuilder result = new StringBuilder();
		for (Map.Entry<Long, List<Staff>> entry : map.entrySet()) {
			result.append("'z").append(entry.getKey()).append("':{");
			result.append(getStaffJson(entry.getValue()));
			result.append("},");
		}
		if(result.length()>0){
			return "{" + result.substring(0, result.length()-1) + "}";
		}else{
			return "{}";
		}
	}

	private String getStaffJson(List<Staff> staffs) {
		StringBuilder result = new StringBuilder();
		for(int i=0; i< staffs.size();i++){
			Staff staff = staffs.get(i);
			result.append("'").append(i).append("':").append("'").append(staff.getWorkNo()).append("|").append(staff.getUserName()).append("|").append(staff.getObjectId()).append("'");
			if(i!=staffs.size()-1){
				result.append(",");
			}
		}
		return result.toString();
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

	private String getDepartmentFullName(long departmentId, List<CompanyDepartment> all){
		for(CompanyDepartment companyDepartment : all){
			if(companyDepartment.getObjectId() == departmentId){
				return companyDepartment.getFullName();
			}
		}
		return "";
	}

	@RequestMapping("exportStaffTemplateForPlan")
	public String exportStaffTemplateForPlan(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws Exception {
	    User user = userManager.getByObjectId(FrameworkContextUtils.getCurrentUserId());
	    String content = "说明：\n1.当前可用积分：" + user.getAccountBalance() + ",导入的时候列的顺序不能修改\n"+
	                          "2、必填项：部门、员工工号、姓名和额度\n"+
                              "3、部门：填写的部门必须已经存在的组织机构\n"+
                              "4、员工工号、姓名: 必须是已经在职（未冻结）的员工工号和姓名\n"+
                              "5、额度：是发放给员工的选择额度，必须是大于零的整数";
	    return exportStaffTemplate(request, response,new ModelMap(),"额度",content);
	}
	@RequestMapping("exportStaffTemplateForPlanBySalary")
    public String exportStaffTemplateForPlanBySalary(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap) throws Exception {
	    User user = userManager.getByObjectId(FrameworkContextUtils.getCurrentUserId());
        String content = "说明：\n1.当前可用积分：" + user.getAccountBalance() + ",导入的时候列的顺序不能修改\n"+
                              "2、必填项：部门、员工工号、姓名和额度\n"+
                              "3、部门：填写的部门必须已经存在的组织机构\n"+
                              "4、员工工号、姓名: 必须是已经在职（未冻结）的员工工号和姓名\n"+
                              "5、年薪：请填写每位员工的实际年薪，系统会根据您设置的福利比例帮您计算出每位员工的额度，必须填写大于零的整数";
	    return exportStaffTemplate(request, response, new ModelMap(),"年薪(元)",content);
    }

	private String exportStaffTemplate(HttpServletRequest request, HttpServletResponse response,ModelMap modelMap, String pointName,String content) throws Exception {
        Long organizationId = FrameworkContextUtils.getCurrentUser().getOrganizationId();
        Long companyId = FrameworkContextUtils.getCurrentUser().getCompanyId();
        CompanyPublished companyPublished = companyPublishedManager.getByObjectId(companyId);
        List<CompanyFunction> functions = companyFunctionManager.getCompanyFunctionsByCompanyId(companyId);
        boolean hasLevel = false;
        for(CompanyFunction function : functions){
            if(function.getFunctionId()==4){
                hasLevel = true;
                break;
            }
        }
        List<CompanyDepartment> departments = companyDepartmentManager.getCompanyDepartmentsByCompanyId(companyId);;
        //分权限取数据
        if(UserUtils.isCompanyHR()){
            departments = CompanyDepartmentUtils.getSubCompanyDepartment(departments, organizationId);
            departments.add(companyDepartmentManager.getByObjectId(organizationId));
        }
        CompanyDepartmentUtils.fillALlFullName(departments);

        HSSFWorkbook wb=new HSSFWorkbook();//excel文件对象
        HSSFSheet sheet=wb.createSheet("员工导入模板");
        CellStyle style = getStyle(wb);
        CellRangeAddress range= new CellRangeAddress(0, 2, 0, 4);
        sheet.addMergedRegion(range);
        Row row0 = sheet.createRow(0);
        Cell cell00 = row0.createCell(0, Cell.CELL_TYPE_STRING);

        CellStyle styleTitle = getStyle(wb);
        styleTitle.setAlignment(CellStyle.ALIGN_LEFT);
        styleTitle.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        cell00.setCellStyle(styleTitle);
        cell00.setCellValue(content);
        row0.setHeightInPoints(8*20);//设置行高

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

        if(StringUtils.isBlank(pointName)){
            pointName = "积分";
        }
        Cell cell3 = row1.createCell(3, Cell.CELL_TYPE_STRING);
        cell3.setCellStyle(style);
        cell3.setCellValue(pointName);
        sheet.autoSizeColumn(3);

        if(hasLevel){
            Cell cell4 = row1.createCell(4, Cell.CELL_TYPE_STRING);
            cell4.setCellStyle(style);
            cell4.setCellValue("等级");
            sheet.autoSizeColumn(4);
        }
        List<Staff> staffs = staffManager.getAllStaffs(companyId, organizationId);
        short row = 4;
        for(Staff staff : staffs){
            row1 = sheet.createRow(row++);
            cell0 = row1.createCell(0, Cell.CELL_TYPE_STRING);
            cell0.setCellStyle(style);
            if(staff.getDepartmentId()!=null){
                cell0.setCellValue(getDepartmentFullName(staff.getDepartmentId(), departments));
            }else{
                cell0.setCellValue("");
            }
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
            cell3.setCellValue("");
            sheet.autoSizeColumn(3);
            if(hasLevel){
                Cell cell4 = row1.createCell(4, Cell.CELL_TYPE_STRING);
                cell4.setCellStyle(style);
                cell4.setCellValue(staff.getStaffLevel() + "");
                sheet.autoSizeColumn(4);
            }
        }
        String fileName = companyPublished.getCompanyName() + "员工导入模板" + ".xls";
        FileUpDownUtils.setDownloadResponseHeaders(response, FileUpDownUtils.encodeDownloadFileName(request, fileName));
        wb.write(response.getOutputStream());
        return null;
    }
}
