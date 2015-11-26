package com.handpay.ibenefit.system.web;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.lang3.StringUtils;

import com.handpay.ibenefit.framework.entity.BaseTree;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.entity.ProjectTeam;
import com.handpay.ibenefit.security.entity.Department;

public final class CompanyDepartmentUtils {
	private CompanyDepartmentUtils() {
	}

	public static CompanyDepartment getCompanyDepartmentByObjectId(List<CompanyDepartment> all, long objectId) {
		for (CompanyDepartment temp : all) {
			if (temp.getObjectId() == objectId) {
				return temp;
			}
		}
		return null;
	}
	
	public static List<CompanyDepartment> searchDepartemnts(List<CompanyDepartment> all, String departmentName) {
		if(StringUtils.isNotBlank(departmentName)){
			Set<CompanyDepartment> matched = new HashSet<CompanyDepartment>();
			for (CompanyDepartment temp : all) {
				if (temp.getName().indexOf(departmentName)!=-1) {
					matched.add(temp);
					addParent(all, temp.getParentId(), matched);
				}
			}
			return new ArrayList<CompanyDepartment>(matched);
		}
		return all;
	}
	
	public static void addParent(List<CompanyDepartment> all,long parentId, Set<CompanyDepartment> matched){
		if(parentId==BaseTree.ROOT){
			return ;
		}
		for(CompanyDepartment temp : all){
			if(temp.getObjectId() == parentId){
				if(!matched.contains(temp)){
					matched.add(temp);
					addParent(all, temp.getParentId(), matched);
				}else{
					return;
				}
			}
		}
	}
	
	public static List<CompanyDepartment> getSubCompanyDepartment(List<CompanyDepartment> all, long parentId) {
		List<CompanyDepartment> result = new ArrayList<CompanyDepartment>();
		//本级的
		for (int n =all.size() -1;n>=0 ;n--) {
			CompanyDepartment temp = all.get(n);
			if (temp.getParentId() == parentId) {
				result.add(temp);
				//避免死循环
				all.remove(n);
			}
		}
		//循环取下一级
		if(result.size()>0){
			List<CompanyDepartment> added = new ArrayList<CompanyDepartment>();
			for (int n =result.size() -1;n>=0 ;n--) {
				CompanyDepartment temp = result.get(n);
				added.addAll(getSubCompanyDepartment(all, temp.getObjectId()));
			}
			result.addAll(added);
		}
		return result;
	}

	public static DefaultMutableTreeNode getTree(CompanyDepartment root, List<CompanyDepartment> all) {
		if (root != null) {
			DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(root);
			List<CompanyDepartment> menus = new ArrayList<CompanyDepartment>();
			for (CompanyDepartment menu : all) {
				if (menu.getParentId().equals(root.getObjectId())) {
					menus.add(menu);
				}
			}
			if (menus.size() > 0) {
				for (CompanyDepartment menu : menus) {
					treeNode.add(getTree(menu, all));
				}
			}
			return treeNode;
		}
		return null;
	}

	public static CompanyDepartment getTop(List<CompanyDepartment> all) {
		for (CompanyDepartment baseTree : all) {
			if (baseTree.getParentId() == BaseTree.ROOT) {
				return baseTree;
			}
		}
		return null;
	}
	
	public static CompanyDepartment getTop(List<CompanyDepartment> all , long departmentId) {
		for (CompanyDepartment baseTree : all) {
			if( baseTree.getObjectId() == departmentId ){
				if(baseTree.getParentId() != BaseTree.ROOT){
					return getTop(all,baseTree.getParentId());
				}else{
					return baseTree;
				}
			}
		}
		return null;
	}

	public static DefaultMutableTreeNode getDefalutDepartmentTree(List<CompanyDepartment> all) {
		CompanyDepartment root = getTop(all);
		if (root != null) {
			return getTree(root, all);
		}
		return null;
	}

	public static void generateDeptTreeHtml(DefaultMutableTreeNode treeNode, StringBuilder treeHtml) {
		if (null != treeNode && null != treeNode.getUserObject()) {
			CompanyDepartment menu = (CompanyDepartment) treeNode.getUserObject();
			int has = treeNode.getChildCount()>0?1:0;
			if(has==1){
				treeHtml.append("<dt data-has='" + has +"' data-id='" + menu.getObjectId() + "' class='j-tree z-on'>" + menu.getName() + "</dt>");
			}else{
				treeHtml.append("<span data-has='" + has +"' data-id='" + menu.getObjectId() + "' class='j-tree z-on'>" + menu.getName() + "</span>");
			}
			if (treeNode.getChildCount() > 0) {
				treeHtml.append("<dd><dl>");
				Enumeration dd = treeNode.children();
				while (dd.hasMoreElements()) {
					generateDeptTreeHtml((DefaultMutableTreeNode) dd.nextElement(), treeHtml);
				}
				treeHtml.append("</dl></dd>");
			}
		}
	}
	
	private static void fillFull(CompanyDepartment entity, List<CompanyDepartment> allCompanyDepartments) {
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

	public static CompanyDepartment getParentDepartment(Long parentId, List<CompanyDepartment> allCompanyDepartments) {
		for(CompanyDepartment companyDepartment : allCompanyDepartments){
			if(companyDepartment.getObjectId().equals(parentId)){
				if(companyDepartment.getFullName()==null){
					fillFull(companyDepartment, allCompanyDepartments);
				}
				return companyDepartment;
			}
		}
		return null;
	}
	
	public static void fillALlFullName(List<CompanyDepartment> allCompanyDepartments){
		for(CompanyDepartment department : allCompanyDepartments){
			fillFull(department, allCompanyDepartments);
		}
	}
	
	public static void generatePointTreeHtml(DefaultMutableTreeNode treeNode, StringBuilder treeHtml) {
		if (null != treeNode && null != treeNode.getUserObject()) {
			CompanyDepartment menu = (CompanyDepartment) treeNode.getUserObject();
			boolean hasChildren = treeNode.getChildCount()>0?true:false;
			if(hasChildren){
				treeHtml.append("<h3><i class='f-ib'>-</i><label class='f-ib'>");
			}else{
				treeHtml.append("<h3><label>");
			}
			//根目录不能选择
			if(menu.getObjectId()==BaseTree.ROOT){
				treeHtml.append( menu.getName() + "</label>");
			}else{
				treeHtml.append("<input type='checkbox' value='" + menu.getObjectId() + "'> " + menu.getName() + "</label>");
			}
			
			if(hasChildren){
				treeHtml.append("</h3>");
			}else{
				treeHtml.append("</h3>");
			}
			if (treeNode.getChildCount() > 0) {
				treeHtml.append("<div>");
				Enumeration dd = treeNode.children();
				while (dd.hasMoreElements()) {
					generatePointTreeHtml((DefaultMutableTreeNode) dd.nextElement(), treeHtml);
				}
				treeHtml.append("</div>");
			}
		}
	}
	
	public static String generateProjectTeamTree(CompanyDepartment root, List<CompanyDepartment> tops){
		StringBuilder treeHtml = new StringBuilder();
		if(root!=null && root.getProjectTeams().size()>0){
			treeHtml.append("<h3 class='lst'><i class='f-ib'>-</i><label class='f-ib'>");
			treeHtml.append(root.getName() + "</label>");
			treeHtml.append("</h3><div>");
			for(ProjectTeam projectTeam : root.getProjectTeams()){
				treeHtml.append("<h3><label>");
				treeHtml.append("<input type='checkbox' value='" + projectTeam.getObjectId() + "'> " + projectTeam.getName() + "</label>");
				treeHtml.append("</h3>");
			}
			for(CompanyDepartment companyDepartment: tops){
				List<ProjectTeam> projectTeams = companyDepartment.getProjectTeams();
				if(projectTeams.size()>0){
					treeHtml.append("<h3><i class='f-ib'>-</i><label class='f-ib'>");
					treeHtml.append(companyDepartment.getName() + "</label>");
					treeHtml.append("</h3>");
					treeHtml.append("<div>");
					for(ProjectTeam projectTeam : projectTeams){
						treeHtml.append("<h3><label>");
						treeHtml.append("<input type='checkbox' value='" + projectTeam.getObjectId() + "'> " + projectTeam.getName() + "</label>");
						treeHtml.append("</h3>");
					}
					treeHtml.append("</div>");
				}
			}
			treeHtml.append("</div>");
		}else{
			for(CompanyDepartment companyDepartment: tops){
				List<ProjectTeam> projectTeams = companyDepartment.getProjectTeams();
				if(projectTeams.size()>0){
					treeHtml.append("<h3><i class='f-ib'>-</i><label class='f-ib'>");
					treeHtml.append(companyDepartment.getName() + "</label>");
					treeHtml.append("</h3>");
					treeHtml.append("<div>");
					for(ProjectTeam projectTeam : projectTeams){
						treeHtml.append("<h5><label>");
						treeHtml.append("<input type='checkbox' value='" + projectTeam.getObjectId() + "'> " + projectTeam.getName() + "</label>");
						treeHtml.append("</h5>");
					}
					treeHtml.append("</div>");
				}
			}
		}
		
		return treeHtml.toString();
	}
	
}
