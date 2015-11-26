package com.handpay.ibenefit.system.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.CRUDUtils;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.framework.entity.BaseTree;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.entity.Staff;
import com.handpay.ibenefit.member.service.ICompanyDepartmentManager;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;

@Controller
@RequestMapping("companyDepartment")
public class CompanyDepartmentController {
	private static final Logger LOGGER = Logger.getLogger(CompanyDepartmentController.class);
	
	@Reference(version="1.0")
	private ICompanyDepartmentManager companyDepartmentManager;
	
	@Reference(version="1.0")
	private IStaffManager staffManager;

	@RequestMapping("getDepartments")
	public String getDepartments(HttpServletRequest request,ModelMap modelMap){
		User user = FrameworkContextUtils.getCurrentUser();
		Long companyId = user.getCompanyId();
		Long organizationId = user.getOrganizationId();
		List<CompanyDepartment> all = companyDepartmentManager.getCompanyDepartmentsByCompanyId(companyId);
		CompanyDepartment root = null;
		List<CompanyDepartment> my = null;
		if(UserUtils.isCompanyAdmin()){
			root = new CompanyDepartment();
			root.setName(user.getCompanyName());
			root.setObjectId(BaseTree.ROOT);
			my = all;
		}else{
			root = CompanyDepartmentUtils.getCompanyDepartmentByObjectId(all, organizationId);
			my = CompanyDepartmentUtils.getSubCompanyDepartment(all, organizationId);
			my.remove(root);
		}
		DefaultMutableTreeNode treeNode = CompanyDepartmentUtils.getTree(root,my);
		StringBuilder treeHtml = new StringBuilder();
		CompanyDepartmentUtils.generatePointTreeHtml(treeNode, treeHtml);
		modelMap.addAttribute("treeNode",  treeHtml.toString());
		modelMap.addAttribute("departments", my);
		return "jsonView";
	}
	
	@RequestMapping("department")
	public String department(HttpServletRequest request,HttpServletResponse response){
		User user = FrameworkContextUtils.getCurrentUser();
		Long companyId = user.getCompanyId();
		List<CompanyDepartment> all = companyDepartmentManager.getCompanyDepartmentsByCompanyId(companyId);
		List<CompanyDepartment> departments = new ArrayList<CompanyDepartment>(0);
		CompanyDepartment root = null;
		if(UserUtils.isCompanyHR()){
			Long organizationId = FrameworkContextUtils.getCurrentUser().getOrganizationId();
			if(organizationId!=null){
				root = CompanyDepartmentUtils.getCompanyDepartmentByObjectId(all, organizationId);
				if(root!=null){
					List<CompanyDepartment> list = CompanyDepartmentUtils.getSubCompanyDepartment(all, root.getObjectId());
					departments = list;
				}
			}
		}else{
			root = new CompanyDepartment();
			root.setName(user.getCompanyName());
			root.setObjectId(BaseTree.ROOT);
			departments = all;
		}
		CompanyDepartmentUtils.fillALlFullName(departments);
		request.setAttribute("departments", departments);
		List<CompanyDepartment> matched = CompanyDepartmentUtils.searchDepartemnts(departments, request.getParameter("departmentName"));
		request.setAttribute("departmentName", request.getParameter("departmentName"));
		DefaultMutableTreeNode tree = CompanyDepartmentUtils.getTree(root,matched);
		StringBuilder treeHtml = new StringBuilder();
		CompanyDepartmentUtils.generateDeptTreeHtml(tree, treeHtml);
		request.setAttribute("treeHtml", treeHtml.toString());
		request.setAttribute("isAdmin", UserUtils.isCompanyAdmin());
		return "system/department";
	}
	
	@RequestMapping("getDepartment/{departmentId}")
	public String getDepartment(HttpServletRequest request,@PathVariable Long departmentId,ModelMap modelMap){
		CompanyDepartment sample = new CompanyDepartment();
		sample.setCompanyId((Long)request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID));
		sample.setObjectId(departmentId);
		CompanyDepartment department = companyDepartmentManager.getByObjectId(departmentId);
		if(department!=null && department.getParentId()!=BaseTree.ROOT){
			CompanyDepartment parent = companyDepartmentManager.getByObjectId(department.getParentId());
			modelMap.addAttribute("parent", parent);
		}
		modelMap.addAttribute("department", department);
		//解决默认为0的问题
		modelMap.addAttribute("sortNo", department.getSortNo()==null?"":department.getSortNo());
		return "jsonView";
	}
	
	@RequestMapping("saveDepartment")
	public String saveDepartment(HttpServletRequest request,CompanyDepartment companyDepartment){
		try {
			Long companyId = (Long)request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
			companyDepartment.setCompanyId(companyId);
			if(companyDepartment.getHeadCount()==null){
				companyDepartment.setHeadCount(0L);
			}
			if(companyDepartment.getParentId()==null){
				User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
				if(UserUtils.isCompanyAdmin()){
					companyDepartment.setParentId(BaseTree.ROOT);
				}else if(UserUtils.isCompanyHR()){
					Staff staff = staffManager.getByObjectId(user.getObjectId());
					companyDepartment.setParentId(staff.getDepartmentId().longValue());
				}
			}
			//同级部门不能重名
			List<CompanyDepartment> all = companyDepartmentManager.getCompanyDepartmentsByCompanyId(companyId);
			List<CompanyDepartment> childrens = new ArrayList<CompanyDepartment>();
			for(CompanyDepartment dept : all){
				if(dept.getParentId().equals(companyDepartment.getParentId())){
					childrens.add(dept);
				}
			}
			if(this.isSameName(childrens,companyDepartment)){
				request.setAttribute("message", "保存部门失败:部门名称重复");
				return "forward:department"; 
			}
			boolean isCreate = companyDepartment.getObjectId()==null;
			//修改的时候，可能会发生循环嵌套的问题
			if(!isCreate){
				all.remove(companyDepartment);
				all.add(companyDepartment);
				try{
					CompanyDepartmentUtils.fillALlFullName(all);
				}catch(StackOverflowError e ){
					request.setAttribute("message", "部门定义错误，不能循环嵌套");
					return "forward:department"; 
				}
			}
			CRUDUtils.prepareSave(companyDepartment);
			companyDepartmentManager.save(companyDepartment);
			if(!isCreate){
				companyDepartmentManager.updateHeadCount(companyId);
			}
			return "redirect:department";
		} catch (Exception e) {
			LOGGER.error("saveDepartment",e);
		}
		request.setAttribute("message", "保存部门成功");
		return "forward:department"; 
	}
	

	@RequestMapping("deleteDepartment/{departmentId}")
	public String deleteDepartment(HttpServletRequest request,@PathVariable Long departmentId) throws Exception{
		CompanyDepartment department = new CompanyDepartment();
		department.setParentId(departmentId);
		List<CompanyDepartment> childrens = companyDepartmentManager.getBySample(department);
		if((childrens.size() > 0) || this.hasStaffs(departmentId)){
			request.setAttribute("message", "删除部门失败");
			return "forward:../department"; 
		}
		CompanyDepartment sample = new CompanyDepartment();
		sample.setCompanyId((Long)request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID));
		sample.setObjectId(departmentId);
		companyDepartmentManager.deleteBySample(sample);
		return "redirect:../department";
	}
	
	

	private boolean isSameName(List<CompanyDepartment> childrens, CompanyDepartment department) {
		for(CompanyDepartment depart : childrens){
			if(department.getName().equals(depart.getName())){
				if(department.getObjectId()==null){
					return true;
				}else if(!department.getObjectId().equals(depart.getObjectId())){
					return true;
				}
			}
		}
		return false;
	}
	
	
	private boolean hasStaffs(Long departmentId) {
		Staff sample = new Staff();
		sample.setDepartmentId(departmentId);
		sample.setStatus(IBSConstants.USER_STATUS_ENABLED);
		List<Staff> list = staffManager.getBySample(sample);
		if(list.size() > 0){
			return true;
		}
		return false;
	}
}
