/**
 * @Title: MemberController.java
 * @Package com.handpay.ibenefit.member.web
 * @Description: TODO
 * Copyright: Copyright (c) 2015 
 * 
 * @author yazhou.li
 * @date 2015-6-23 下午12:37:42
 * @version V1.0
 */

package com.handpay.ibenefit.system.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.hssf.usermodel.DVConstraint;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFDataValidation;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.framework.util.UploadFile;
import com.handpay.ibenefit.member.entity.Company;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.entity.CompanyStaff;
import com.handpay.ibenefit.member.entity.Staff;
import com.handpay.ibenefit.member.service.ICompanyDepartmentManager;
import com.handpay.ibenefit.member.service.ICompanyManager;
import com.handpay.ibenefit.member.service.ICompanyStaffManager;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.Department;
import com.handpay.ibenefit.security.entity.StaffGradeScore;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IStaffGradeScoreManager;
import com.handpay.ibenefit.security.service.IUserManager;

/**
 * @ClassName: MemberController
 * @author yazhou.li
 * @date 2015-6-23 下午12:37:42
 *
 */
@Controller
@RequestMapping("member")
public class MemberController extends BaseController {
	
	@Reference(version="1.0",timeout=20000)
	private IStaffManager staffManager;
	
	@Reference(version="1.0")
	private IUserManager userManager;
	
	@Reference(version="1.0")
	private ICompanyManager companyManager;
	
	@Reference(version="1.0")
	private IDictionaryManager dictionaryManager;
	
	@Reference(version="1.0")
	private ICompanyDepartmentManager companyDepartmentManager;
	
	@Reference(version = "1.0")
	private ICompanyStaffManager companyStaffManager;
	
	@Reference(version = "1.0")
	private IStaffGradeScoreManager staffGradeScoreManager;
	
	@RequestMapping("index")
	public String index(HttpServletRequest request,HttpServletResponse response){
		return "member/index";
	}
	
	@RequestMapping("listStaff")
	public String listStaff(HttpServletRequest request,HttpServletResponse response){
		User user=FrameworkContextUtils.getCurrentUser();
		PageSearch page  = PageUtils.preparePage(request,Staff.class,true);
		page.setPageSize(15);
		page.getFilters().add(new PropertyFilter(Staff.class.getName(),"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
		if(UserUtils.isCompanyHR()){
			if (user.getOrganizationId()!=null) {
				page.getFilters().add(new PropertyFilter(Staff.class.getName(),"EQL_organizationId",user.getOrganizationId().toString()));
			}
		}
		String[] sortOrders = {"asc","asc"};
		String[] sortProperties = {"departmentId","userName"};
		page.setSortOrders(sortOrders);
		page.setSortProperties(sortProperties);
		handleFind(page);
		request.setAttribute("pageSearch", page);
		request.setAttribute("now", new Date());
		if (user!=null && user.getCompanyId()!=null) {
			CompanyDepartment cd=new CompanyDepartment();
			cd.setCompanyId(user.getCompanyId());
			cd.setDeleted(IBSConstants.STATUS_NO);
			List<CompanyDepartment> cds=companyDepartmentManager.getBySample(cd);
			request.setAttribute("departments", cds);
			List<StaffGradeScore> scores=staffGradeScoreManager.getAll();
			request.setAttribute("staffGS", scores);
		}
		return "member/listStaff";
	}
	
	protected void handleFind(PageSearch page) {
		PageSearch result = staffManager.findList(page);
		page.setTotalCount(result.getTotalCount());
		page.setList(result.getList());
	}
	
	
	@RequestMapping("viewStaff/{objectId}")
	public String viewStaff(HttpServletRequest request,HttpServletResponse response,@PathVariable Long objectId){
		if (objectId!=null) {
			Staff staff=staffManager.getByObjectId(objectId);
			if (staff!=null) {
				CompanyDepartment department = companyDepartmentManager.getByObjectId(staff.getDepartmentId());
				staff.setCompanyDepartment(department);
				request.setAttribute("staff", staff);
				request.setAttribute("user", userManager.getByObjectId(objectId));
			}
		}
		return "member/viewStaff";
	}
	
	
	@RequestMapping("addStaff")
	public String addStaff(HttpServletRequest request,HttpServletResponse response){
		Long userId = (Long) request.getSession().getAttribute(SecurityConstants.USER_ID);
		User user=userManager.getByObjectId(userId);
		if (user!=null) {
			if (user.getCompanyId()!=null) {
				Company company=companyManager.getByObjectId(user.getCompanyId());
				if (company!=null) {
					request.setAttribute("companyId", company.getObjectId());
				}
			}
		}
		request.setAttribute("staff", new Staff());
		return "member/addStaff";
	}
	
	@RequestMapping("saveStaff")
	public String saveStaff(HttpServletRequest request,HttpServletResponse response,Staff t, User user){
		User currentUser = FrameworkContextUtils.getCurrentUser();
		Long organizationId = currentUser.getOrganizationId();
		////企业管理员取顶级部门的部门ID作为组织ID
		if(UserUtils.isCompanyAdmin()){
			Long departmentId = t.getDepartmentId();
			if(departmentId!=null){
				List<CompanyDepartment> all = companyDepartmentManager.getCompanyDepartmentsByCompanyId(currentUser.getCompanyId());
				CompanyDepartment top = CompanyDepartmentUtils.getTop(all, departmentId);
				if(top!=null){
					organizationId = top.getObjectId();
				}
			}
		}
		t.setCompanyId(currentUser.getCompanyId());
		t.setOrganizationId(organizationId);
		staffManager.save(t,user);
		return "redirect:/member/listStaff";
	}
	
	@RequestMapping("editStaff/{objectId}")
	public String editStaff(HttpServletRequest request,HttpServletResponse response, @PathVariable("objectId") Long objectId){
		Staff staff = staffManager.getByObjectId(objectId);
		if(staff.getLeaveDate()==null){
			staff.setLeaveDate(new Date());
		}
		if(staff.getQuitDays()!=null && staff.getQuitType() == IBSConstants.STAFF_QUIT_TYPE_NORMAL){
			SimpleDateFormat sdf = new SimpleDateFormat(com.handpay.ibenefit.framework.util.DateUtils.FORMAT_YYYY_MM_DD); 
			String begin = sdf.format(staff.getQuitDays());
			String end = sdf.format(new Date());
			int days = Math.round(com.handpay.ibenefit.framework.util.DateUtils.getDiffDay(end,begin));
			if(days<0){
				days=0;
			}
			request.setAttribute("days", days);
		}
		CompanyDepartment department = companyDepartmentManager.getByObjectId(staff.getDepartmentId());
		request.setAttribute("department", department);
		request.setAttribute("staff", staff);
		request.setAttribute("user", userManager.getByObjectId(objectId));
		return "member/updateStaff";
	}
	
	
	/**
	 * 将员工设置为离职，并设置冻结时间
	 * @param objectIds
	 * @param day
	 * @return
	 */
	@RequestMapping(value="updateQuitType/{day}")
	public String updateQuitType(HttpServletRequest request,@PathVariable Integer day){
		Date quitDays = new Date();
		String staffIds=request.getParameter("ids");
		if(staffIds!=null){
			String[] array=staffIds.split(",");
			for (int i = 0; i < array.length; i++) {
				Staff staff = staffManager.getByObjectId(Long.parseLong(array[i]));
				staff.setStatus(IBSConstants.STAFF_STATUS_OFF);
				staff.setLeaveDate(new Date());
				if(day != null && day >0){
					staff.setQuitDays(DateUtils.addDays(quitDays, day));
				}
				User user = userManager.getByObjectId(staff.getObjectId());
				staff.setMobilePhone(user.getMobilePhone());
				staffManager.save(staff);
				userManager.unBindUserMobilePhone(user.getMobilePhone(), IBSConstants.USER_TYPE_COMPANY_EMPLOYEE);
				userManager.unBindUserEmail(user.getEmail(), IBSConstants.USER_TYPE_COMPANY_EMPLOYEE);
			}
		}
		return "redirect:/member/listStaff";
	}
	
	/**
	 * 更新员工账户为冻结
	 * @param objectIds
	 * @param map
	 * @return
	 */
	@RequestMapping(value="updateBlocked")
	public String updateBlocked(HttpServletRequest request,HttpServletResponse response){
		String staffIds=request.getParameter("ids");
		String[] array=staffIds.split(",");
		for (int i = 0; i < array.length; i++) {
			Staff staff = staffManager.getByObjectId(Long.parseLong(array[i]));
			if(staff.getQuitType()==null || staff.getQuitType()!=IBSConstants.STAFF_QUIT_TYPE_BLOCKED){
				staff.setQuitType(IBSConstants.STAFF_QUIT_TYPE_BLOCKED);
				staff.setQuitDays(new Date());
				staffManager.save(staff);
			}
			userManager.updateStatus(staff.getObjectId(), IBSConstants.USER_STATUS_LOCKED);
		}
		return "redirect:/member/listStaff";
	}
	
	@RequestMapping(value="updateBlocked/{objectId}")
	public String updateBlocked(HttpServletRequest request,HttpServletResponse response,@PathVariable Long objectId){
		Staff staff = staffManager.getByObjectId(objectId);
		if(staff.getQuitType()==null || staff.getQuitType()!=IBSConstants.STAFF_QUIT_TYPE_BLOCKED){
			staff.setQuitType(IBSConstants.STAFF_QUIT_TYPE_BLOCKED);
			staff.setQuitDays(new Date());
			staffManager.save(staff);
		}
		userManager.updateStatus(staff.getObjectId(), IBSConstants.USER_STATUS_LOCKED);
		return "redirect:/member/listStaff";
	}
	
	/**
	 * 更新员工账户为有效
	 * @param objectIds
	 * @param map
	 * @return
	 */
	@RequestMapping(value="updateNormal/{objectId}")
	public String updateNormal(HttpServletRequest request,@PathVariable Long objectId){
		Staff staff = staffManager.getByObjectId(objectId);
		staff.setQuitType(IBSConstants.STAFF_QUIT_TYPE_NORMAL);
		staff.setQuitDays(null);
		userManager.updateStatus(staff.getObjectId(), IBSConstants.USER_STATUS_ENABLED);
		staffManager.updateById(staff);
		return "redirect:/member/listStaff";
	}
	
	
	/**
	 * 更新员工信息
	 * @param staff
	 * @param request
	 * @return
	 */
	@RequestMapping(value="updateStaff")
	public String updateStaff(Staff staff,HttpServletRequest request){
		Integer quitType = staff.getQuitType();
		if(IBSConstants.STAFF_STATUS_ON == staff.getStatus()){
			staff.setQuitType(IBSConstants.STATUS_YES);
		}else{
			staff.setQuitType(quitType);
		}
		if(quitType == 1){
			staff.setQuitDays(null);
		}else if(quitType > 1){
			staff.setQuitType(IBSConstants.STAFF_QUIT_TYPE_BLOCKED);
			staff.setQuitDays(DateUtils.addDays(new Date(), quitType));
		}
		this.staffManager.save(staff);
		request.setAttribute("staff", staff);
		return "member/addStaffPart";
	}

	private List<Staff> getStaffs(Long ... objectIds ) {
		List<Staff> staffs = new ArrayList<Staff>(objectIds.length);
		for(int i = 0 ; i < objectIds.length ; i ++){
			Staff staff = new Staff();
			staff.setObjectId(objectIds[i]);
			staffs.add(staff);
		}
		return staffs;
	}
	
	/**
	 * 导入的模板
	 * @param request
	 * @param response
	 * @param companyId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/exportTemplate")
	public String exportTemplate(HttpServletRequest request, HttpServletResponse response) throws Exception {
		User user = FrameworkContextUtils.getCurrentUser();
		if (user!=null && user.getCompanyId()!=null) {
			Long companyId=user.getCompanyId();
			Company company = companyManager.getByObjectId(companyId);
			List<CompanyDepartment> companyDepartments = companyDepartmentManager.getCompanyDepartmentsByCompanyId(companyId);
			List<String> departmentNames = new ArrayList<String>(companyDepartments.size());
			for(CompanyDepartment department : companyDepartments){
				fillFull(department, companyDepartments);
				departmentNames.add(department.getFullName()+ "[" + department.getObjectId() + "]");
			}
			
			HSSFWorkbook wb=new HSSFWorkbook();//excel文件对象  
	        HSSFSheet sheet=wb.createSheet("员工导入模板");
	        CellStyle style = getStyle(wb);
	        style.setWrapText(true);
	        CellRangeAddress range= new CellRangeAddress(0, 2, 0, 9);
	        sheet.addMergedRegion(range);
	        Row row0 = sheet.createRow(0);
	        Cell cell00 = row0.createCell(0, Cell.CELL_TYPE_STRING);
	        cell00.setCellStyle(style);
	        cell00.setCellValue(new HSSFRichTextString("说明：1、加*为必填项，如‘员工账号’‘员工工号’‘所属部门’‘姓名’‘入职日期（2015/5/1）’‘手机号码’;" + "\n" + "2、‘手机号码’或‘邮箱‘两者必须填写一。"));
	        sheet.autoSizeColumn(0);
	        
	        Row row1 = sheet.createRow(3);
	        Cell cell0 = row1.createCell(0, Cell.CELL_TYPE_STRING);
	        cell0.setCellStyle(style);
	        cell0.setCellValue("员工帐号*");
	        sheet.autoSizeColumn(0);
	        
	        Cell cell1 = row1.createCell(1, Cell.CELL_TYPE_STRING);
	        cell1.setCellStyle(style);
	        cell1.setCellValue("员工工号*");
	        sheet.autoSizeColumn(1);
	        
	        Cell cell2 = row1.createCell(2, Cell.CELL_TYPE_STRING);
	        cell2.setCellStyle(style);
	        cell2.setCellValue("所属部门*");
	        sheet.autoSizeColumn(2);
	        
	        Cell cell3 = row1.createCell(3, Cell.CELL_TYPE_STRING);
	        cell3.setCellStyle(style);
	        cell3.setCellValue("员工姓名*");
	        sheet.autoSizeColumn(3);
	        
	        Cell cell4 = row1.createCell(4, Cell.CELL_TYPE_STRING);
	        cell4.setCellStyle(style);
	        cell4.setCellValue("入职日期*");
	        sheet.autoSizeColumn(4);
	        
	        
	        Cell cell5 = row1.createCell(5, Cell.CELL_TYPE_STRING);
	        cell5.setCellStyle(style);
	        cell5.setCellValue("手机号码*");
	        sheet.autoSizeColumn(5);
	        
	        
	        Cell cell6 = row1.createCell(6, Cell.CELL_TYPE_STRING);
	        cell6.setCellStyle(style);
	        cell6.setCellValue("邮箱*");
	        sheet.autoSizeColumn(6);
	        
	        Cell cell7 = row1.createCell(7, Cell.CELL_TYPE_STRING);
	        cell7.setCellStyle(style);
	        cell7.setCellValue("员工生日");
	        sheet.autoSizeColumn(7);
	        
	        
	        Cell cell8 = row1.createCell(8, Cell.CELL_TYPE_STRING);
	        cell8.setCellStyle(style);
	        cell8.setCellValue("员工证件类型");
	        sheet.autoSizeColumn(8);
	        
	        
	        Cell cell9 = row1.createCell(9, Cell.CELL_TYPE_STRING);
	        cell9.setCellStyle(style);
	        cell9.setCellValue("员工证件号码");
	        sheet.setColumnWidth(9, 5000);
	        
	        //部门有效性
	        if(departmentNames.size()>0){
	        	addDepartmentsValidation(departmentNames, wb, sheet);
	        }
	        //证件有效性
	        Dictionary dictionary=dictionaryManager.getDictionaryByDictionaryId(1306);
	        List<Dictionary> dictionaries=dictionaryManager.getChildrenByParentId(dictionary.getObjectId());
	        String[] IDTypes=new String[dictionaries.size()];
	        for (int i = 0; i < dictionaries.size(); i++) {
				IDTypes[i]=dictionaries.get(i).getName();
			}
	        HSSFDataValidation ddValidation = setDataValidationList((short)4, (short)32767, (short)8, IDTypes );
	        sheet.addValidationData(ddValidation);
	        String exportFileName = FileUpDownUtils.encodeDownloadFileName(request, company.getCompanyName() + "员工导入模板"+ ".xls");
	        FileUpDownUtils.setDownloadResponseHeaders(response, exportFileName);
	        wb.write(response.getOutputStream());
		}
		return null;
	}

	private void addDepartmentsValidation(List<String> departmentNames, HSSFWorkbook wb, HSSFSheet sheet) {
		HSSFSheet hidden = wb.createSheet("hidden");
		for (int i = 0, length = departmentNames.size(); i < length; i++) {
			String name = departmentNames.get(i);
			HSSFRow row = hidden.createRow(i);
			HSSFCell cell = row.createCell(0);
			cell.setCellValue(name);
		}
		Name namedCell = wb.createName();
		namedCell.setNameName("hidden");
		namedCell.setRefersToFormula("hidden!$A$1:$A$" + departmentNames.size());
		DVConstraint constraint = DVConstraint.createFormulaListConstraint("hidden");
		CellRangeAddressList addressList = new CellRangeAddressList((short) 4, (short) 32767, (short) 2, (short) 2);
		HSSFDataValidation validation = new HSSFDataValidation(addressList, constraint);
		wb.setSheetHidden(1, true);
		sheet.addValidationData(validation);
	}
	
	private CellStyle getStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		// 设置单元格字体
		Font headerFont = workbook.createFont(); // 字体
		headerFont.setFontHeightInPoints((short) 10);
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
	
	public static HSSFDataValidation setDataValidationList(short firstRow,short endRow, short col, String[] textList){  
        //加载下拉列表内容  
        DVConstraint constraint= DVConstraint.createExplicitListConstraint(textList);  
        //设置数据有效性加载在哪个单元格上。  
          
        //四个参数分别是：起始行、终止行、起始列、终止列  
        CellRangeAddressList regions=new CellRangeAddressList(firstRow,endRow,col,col);  
        //数据有效性对象  
        HSSFDataValidation dataValidation = new HSSFDataValidation(regions, constraint);  
        return dataValidation;  
    } 
	
	private void fillFull(CompanyDepartment entity, List<CompanyDepartment> allCompanyDepartments) {
		if (null != entity) {
			if (null != entity.getParentId() && !Department.ROOT.getObjectId().equals(entity.getParentId())) {
				CompanyDepartment parentMenu = getParentDepartment(entity.getParentId(),allCompanyDepartments);
				if (null != parentMenu) {
					entity.setFullName(parentMenu.getFullName() + Department.PATH_SEPARATOR + entity.getName());
					return;
				}
			}
			entity.setFullName(Department.PATH_SEPARATOR + entity.getName());
		}
	}

	public CompanyDepartment getParentDepartment(Long departmentId, List<CompanyDepartment> allCompanyDepartments) {
		for(CompanyDepartment companyDepartment : allCompanyDepartments){
			if(companyDepartment.getObjectId().equals(departmentId)){
				if(companyDepartment.getFullName()==null){
					fillFull(companyDepartment, allCompanyDepartments);
				}
				return companyDepartment;
			}
		}
		return null;
	}
	
	/**
	 * 导入员工
	 * @param request
	 * @param companyId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/uploadStaffs")
	public String uploadStaffs(HttpServletRequest request,HttpServletResponse response, ModelMap modelMap) throws Exception {
		User user = FrameworkContextUtils.getCurrentUser();
		String result ="";
		if (user!=null && user.getCompanyId()!=null) {
			Long companyId=user.getCompanyId();
			UploadFile uploadFile = FileUpDownUtils.getUploadFile(request,"uploadFile");
			byte[] fileData = FileUpDownUtils.getFileContent(uploadFile.getFile());
			try{
				result = staffManager.importStaffs(fileData, companyId, user.getObjectId(), true);
			}catch(Exception e){
				result = "导入失败";
			}
		}
		modelMap.addAttribute("iResult", result);
		AjaxUtils.doAjaxResponseOfMap(response, modelMap);
		return null;
	}
	
	/**
	 * 批量导出员工
	 * @param request
	 * @param response
	 * @param modelMap
	 * @return
	 */
	@RequestMapping(value="exportAll")
	public String exportAll(HttpServletRequest request,HttpServletResponse response,ModelMap modelMap){
		User user = FrameworkContextUtils.getCurrentUser();
		if (user!=null && user.getCompanyId()!=null) {
			Company company = companyManager.getByObjectId(user.getCompanyId());
			try {
		    	List<CompanyDepartment> companyDepartments = companyDepartmentManager.getCompanyDepartmentsByCompanyId(user.getCompanyId());
		 		List<String> departmentNames = new ArrayList<String>(companyDepartments.size());
		 		for(CompanyDepartment department : companyDepartments){
		 			fillFull(department, companyDepartments);
		 			departmentNames.add(department.getFullName()+ "[" + department.getObjectId() + "]");
		 		}
		 		
		 		 HSSFWorkbook wb=new HSSFWorkbook();//excel文件对象  
		         HSSFSheet sheet=wb.createSheet("员工导入模板");
		         CellStyle style = getStyle(wb);
		         style.setWrapText(true);
		         CellRangeAddress range= new CellRangeAddress(0, 2, 0, 9);
		         sheet.addMergedRegion(range);
		         Row row0 = sheet.createRow(0);
		         Cell cell00 = row0.createCell(0, Cell.CELL_TYPE_STRING);
		         cell00.setCellStyle(style);
		         cell00.setCellValue(new HSSFRichTextString("说明：1、加*为必填项，如‘员工账号’‘员工工号’‘所属部门’‘姓名’‘入职日期（2015/5/1）’‘手机号码’;" + "\n" + "2、‘手机号码’或‘邮箱‘两者必须填写一个。"));
		         sheet.autoSizeColumn(0);
		         
		         Row row1 = sheet.createRow(3);
		         Cell cell0 = row1.createCell(0, Cell.CELL_TYPE_STRING);
		         cell0.setCellStyle(style);
		         cell0.setCellValue("员工帐号*");
		         sheet.autoSizeColumn(0);
		         
		         Cell cell1 = row1.createCell(1, Cell.CELL_TYPE_STRING);
		         cell1.setCellStyle(style);
		         cell1.setCellValue("员工工号*");
		         sheet.autoSizeColumn(1);
		         
		         Cell cell2 = row1.createCell(2, Cell.CELL_TYPE_STRING);
		         cell2.setCellStyle(style);
		         cell2.setCellValue("所属部门*");
		         sheet.autoSizeColumn(2);
		         
		         Cell cell3 = row1.createCell(3, Cell.CELL_TYPE_STRING);
		         cell3.setCellStyle(style);
		         cell3.setCellValue("员工姓名*");
		         sheet.autoSizeColumn(3);
		         
		         Cell cell4 = row1.createCell(4, Cell.CELL_TYPE_STRING);
		         cell4.setCellStyle(style);
		         cell4.setCellValue("入职日期*");
		         sheet.autoSizeColumn(4);
		         
		         
		         Cell cell5 = row1.createCell(5, Cell.CELL_TYPE_STRING);
		         cell5.setCellStyle(style);
		         cell5.setCellValue("手机号码*");
		         sheet.autoSizeColumn(5);
		         
		         
		         Cell cell6 = row1.createCell(6, Cell.CELL_TYPE_STRING);
		         cell6.setCellStyle(style);
		         cell6.setCellValue("邮箱*");
		         sheet.autoSizeColumn(6);
		         
		         Cell cell7 = row1.createCell(7, Cell.CELL_TYPE_STRING);
		         cell7.setCellStyle(style);
		         cell7.setCellValue("员工生日");
		         sheet.autoSizeColumn(7);
		         
		         
		         Cell cell8 = row1.createCell(8, Cell.CELL_TYPE_STRING);
		         cell8.setCellStyle(style);
		         cell8.setCellValue("员工证件类型");
		         sheet.autoSizeColumn(8);
		         
		         
		         Cell cell9 = row1.createCell(9, Cell.CELL_TYPE_STRING);
		         cell9.setCellStyle(style);
		         cell9.setCellValue("员工证件号码");
		         sheet.setColumnWidth(9, 5000);
		         
		         CompanyStaff errorCompanyStaff=new CompanyStaff();
		         errorCompanyStaff.setCompanyId(user.getCompanyId());
		         errorCompanyStaff.setUserId(user.getObjectId());
		         errorCompanyStaff.setStatus(2);
		         List<CompanyStaff> errorStaffs=companyStaffManager.getBySample(errorCompanyStaff);
		         if (errorStaffs.size()!=0) {
		        	 for (int i = 0; i < errorStaffs.size(); i++) {
		        		 CompanyStaff companyStaff=errorStaffs.get(i);
					     Row row2 = sheet.createRow(i+4);
				         Cell cellA = row2.createCell(0, Cell.CELL_TYPE_STRING);
				         cellA.setCellStyle(style);
				         cellA.setCellValue(companyStaff.getLoginName());
				         sheet.autoSizeColumn(0);
				         
				         Cell cellB = row2.createCell(1, Cell.CELL_TYPE_STRING);
				         cellB.setCellStyle(style);
				         cellB.setCellValue(companyStaff.getWorkNo());
				         sheet.autoSizeColumn(1);
				         
				         Cell cellC = row2.createCell(2, Cell.CELL_TYPE_STRING);
				         cellC.setCellStyle(style);
				         cellC.setCellValue(companyStaff.getDepartmentId());
				         sheet.autoSizeColumn(2);
				         
				         Cell cellD = row2.createCell(3, Cell.CELL_TYPE_STRING);
				         cellD.setCellStyle(style);
				         cellD.setCellValue(companyStaff.getStaffName());
				         sheet.autoSizeColumn(3);
				         
				         Cell cellE = row2.createCell(4, Cell.CELL_TYPE_STRING);
				         cellE.setCellStyle(style);
				         cellE.setCellValue(companyStaff.getEntryDay());
				         sheet.autoSizeColumn(4);
				         
				         
				         Cell cellF = row2.createCell(5, Cell.CELL_TYPE_STRING);
				         cellF.setCellStyle(style);
				         cellF.setCellValue(companyStaff.getTelephone());
				         sheet.autoSizeColumn(5);
				         
				         
				         Cell cellG = row2.createCell(6, Cell.CELL_TYPE_STRING);
				         cellG.setCellStyle(style);
				         cellG.setCellValue(companyStaff.getEmail());
				         sheet.autoSizeColumn(6);
				         
				         Cell cellH = row2.createCell(7, Cell.CELL_TYPE_STRING);
				         cellH.setCellStyle(style);
				         cellH.setCellValue(companyStaff.getBirthday());
				         sheet.autoSizeColumn(7);
				         
				         
				         Cell cellI = row2.createCell(8, Cell.CELL_TYPE_STRING);
				         cellI.setCellStyle(style);
				         cellI.setCellValue(companyStaff.getIdentityType());
				         sheet.autoSizeColumn(8);
				         
				         
				         Cell cellJ = row2.createCell(9, Cell.CELL_TYPE_STRING);
				         cellJ.setCellStyle(style);
				         cellJ.setCellValue(companyStaff.getIdNo());
				         sheet.autoSizeColumn(9);
					}
				}
		         
		         //部门有效性
		         if(departmentNames.size()>0){
		        	addDepartmentsValidation(departmentNames, wb, sheet);
		         }
		         //证件
		         Dictionary dictionary=dictionaryManager.getDictionaryByDictionaryId(1306);
		         List<Dictionary> dictionaries=dictionaryManager.getChildrenByParentId(dictionary.getObjectId());
		         String[] IDTypes=new String[dictionaries.size()];
		         for (int i = 0; i < dictionaries.size(); i++) {
		 			IDTypes[i]=dictionaries.get(i).getName();
		 		 }
		         HSSFDataValidation ddValidation = setDataValidationList((short)4, (short)32767, (short)8, IDTypes );
		         sheet.addValidationData(ddValidation);
		         String exportFileName = FileUpDownUtils.encodeDownloadFileName(request, company.getCompanyName() + "员工导入异常数据"+ ".xls");
		         FileUpDownUtils.setDownloadResponseHeaders(response, exportFileName);
		         wb.write(response.getOutputStream());
			} catch (Exception e) {
			}
		}
		return null;
	}
	
	/**
	 * 工号校验
	 * @param request
	 * @param companyId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/validateNo/{workNo}")
	public String validateNo(HttpServletRequest request,@PathVariable String workNo, ModelMap modelMap) throws Exception {
		User user = FrameworkContextUtils.getCurrentUser();
		boolean result =false;
		if (user!=null && user.getCompanyId()!=null) {
			Long companyId=user.getCompanyId();
			Staff staff=new Staff();
			staff.setCompanyId(companyId);
			staff.setWorkNo(workNo);
			List<Staff> staffs=staffManager.getBySample(staff);
			if (staffs.size()!=0) {
				result=true;
			}
		}
		modelMap.addAttribute("iResult", result);
		return "jsonView";
	}
	
	/**
	 * 登录账户校验
	 * @param request
	 * @param companyId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/validateLoginName/{loginName}")
	public String validateLoginName(HttpServletRequest request,@PathVariable String loginName, ModelMap modelMap) throws Exception {
		boolean result =false;
		User user=new User();
		user.setLoginName(loginName);
		List<User> users=userManager.getBySample(user);
		if (users.size()!=0) {
			result=true;
		}
		modelMap.addAttribute("iResult", result);
		return "jsonView";
	}
	
	@RequestMapping("isUnique")
	@ResponseBody
	public boolean isUnique(HttpServletRequest request,Staff t){
		Long companyId = (Long)request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		t.setCompanyId(companyId);
		if(t!=null){
			return staffManager.isUnique(t);
		}else{
			return false;
		}
	}
}
