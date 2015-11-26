package com.handpay.ibenefit.system.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.CRUDUtils;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.entity.ProjectTeam;
import com.handpay.ibenefit.member.entity.Staff;
import com.handpay.ibenefit.member.service.ICompanyDepartmentManager;
import com.handpay.ibenefit.member.service.ICompanyManager;
import com.handpay.ibenefit.member.service.IProjectTeamManager;
import com.handpay.ibenefit.member.service.IProjectTeamMemberManager;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;

@Controller
@RequestMapping("projectTeam")
public class ProjectTeamController {
	
	@Reference(version="1.0")
	private IProjectTeamManager projectTeamManager;
	
	@Reference(version="1.0")
	private IUserManager userManager;
	
	@Reference(version = "1.0")
	private IStaffManager staffManager;
	
	@Reference(version="1.0")
	private ICompanyManager companyManager;
	
	@Reference(version="1.0")
	private ICompanyDepartmentManager companyDepartmentManager;
	
	@Reference(version ="1.0")
	private IProjectTeamMemberManager projectTeamMemberManager;
	
	@RequestMapping("index")
	public String index(HttpServletRequest request,HttpServletResponse response){
		return "team/index";
	}
	
	@RequestMapping("listTeam")
	public String listTeam(HttpServletRequest request,HttpServletResponse response,ProjectTeam projectTeam){
		PageSearch page  = PageUtils.preparePage(request,ProjectTeam.class,true);
		page.setSortOrder("desc");
		page.setSortProperty("createdOn");
		User user = FrameworkContextUtils.getCurrentUser();
		User sample = new User();
		sample.setType(IBSConstants.USER_TYPE_COMPANY_HR);
		sample.setCompanyId(user.getCompanyId());
		List<User> hrs = userManager.getBySample(sample);
		request.setAttribute("hrs", hrs);
		if(UserUtils.isCompanyHR()){
			page.getFilters().add(new PropertyFilter(ProjectTeam.class.getName(),"EQI_organizationId",String.valueOf(user.getOrganizationId())));
		}
		page.getFilters().add(new PropertyFilter(ProjectTeam.class.getName(),"EQI_companyId",String.valueOf(user.getCompanyId())));
		handleFind(page);
		request.setAttribute("pageSearch", page);
		return "system/listTeam";
	}
	
	protected void handleFind(PageSearch page) {
		page.getFilters().add(new PropertyFilter(ProjectTeam.class.getName(),"EQI_deleted",IBSConstants.STATUS_NO + ""));
		PageSearch result = projectTeamManager.findList(page);
		page.setTotalCount(result.getTotalCount());
		page.setList(result.getList());
	}
	
	@RequestMapping("saveTeam")
	public String saveTeam(HttpServletRequest request,ModelMap modelMap,ProjectTeam t){
		User user = FrameworkContextUtils.getCurrentUser();
		if(t.getObjectId()==null){
			//管理员创建的项目组的机构ID为企业 ID
			if(UserUtils.isCompanyAdmin()){
				t.setOrganizationId(user.getCompanyId());
			}else{
				t.setOrganizationId(user.getOrganizationId());
			}
			t.setCompanyId(user.getCompanyId());
			t.setHeadCount(0L);
		}
		t.setLeaderName(request.getParameter("userIds"));
		CRUDUtils.prepareSave(t);
		ProjectTeam sample = new ProjectTeam();
		sample.setCompanyId(user.getCompanyId());
		sample.setOrganizationId(t.getOrganizationId());
		sample.setName(t.getName());
		sample.setDeleted(IBSConstants.STATUS_NO);
		List<ProjectTeam> list = projectTeamManager.getBySample(sample);
		boolean isNotDupicate = false;
		if(list.size()==0){
			isNotDupicate = true;
		}else if(list.size()==1){
			if(t.getObjectId()!=null){
				if(list.get(0).getObjectId().equals(t.getObjectId())){
					isNotDupicate = true;
				}
			}
		}
		//同一个机构下项目组名称不能重复
		if(isNotDupicate){
			projectTeamManager.save(t);
		}else{
			modelMap.addAttribute("message", "创建项目组失败:名称重复");
		}
		modelMap.addAttribute("result", isNotDupicate);
		return "jsonView";
	}
	
	@RequestMapping("saveTeamMemeber")
	public String saveTeamMemeber(ModelMap modelMap,Long projectTeamId, String userIds){
		if(projectTeamId!=null){
			List<Long> userId = new ArrayList<Long>();
			if(StringUtils.isNotBlank(userIds)){
				String[] arr = userIds.split(",");
				for(String temp : arr){
					userId.add(Long.parseLong(temp));
				}
			}
			projectTeamMemberManager.saveMembers(userId, projectTeamId);
		}
		modelMap.addAttribute("result", true);
		return "jsonView";
	}
	
	@RequestMapping("getDepartmentProjectTeams")
	public String getDepartmentProjectTeams(HttpServletRequest request,ModelMap modelMap){
		User user = FrameworkContextUtils.getCurrentUser();
		long companyId = user.getCompanyId();
		List<CompanyDepartment> departments = null;
		List<ProjectTeam> projectTeams = null;
		//分权限取数据
		CompanyDepartment root = null;
		if(UserUtils.isCompanyAdmin()){
			departments = companyDepartmentManager.getTopCompanyDepartmentByCompanyId(companyId);
			root = new CompanyDepartment();
			root.setName(user.getCompanyName());
			root.setObjectId(companyId);
			projectTeams = projectTeamManager.getProjectTeamsByCompanyId(companyId);
			List<ProjectTeam> teams = new ArrayList<ProjectTeam>();
			for(ProjectTeam projectTeam : projectTeams){
				if(companyId == projectTeam.getOrganizationId()){
					teams.add(projectTeam);
				}
			}
			root.setProjectTeams(projectTeams);
		}else if(UserUtils.isCompanyHR()){
			departments = new ArrayList<CompanyDepartment>(1);
			Long organizationId = user.getOrganizationId();
			departments.add(companyDepartmentManager.getByObjectId(organizationId));
			projectTeams = projectTeamManager.getProjectTeamsByOrganizationId(organizationId);
		}
		//组装树形结构
		for(CompanyDepartment companyDepartment : departments){
			List<ProjectTeam> teams = new ArrayList<ProjectTeam>();
			long organizationId = companyDepartment.getObjectId();
			for(ProjectTeam projectTeam : projectTeams){
				if(organizationId == projectTeam.getOrganizationId()){
					teams.add(projectTeam);
				}
			}
			companyDepartment.setProjectTeams(teams);
		}
		modelMap.addAttribute("treeNode", CompanyDepartmentUtils.generateProjectTeamTree(root,departments));
		modelMap.addAttribute("departments", departments);
		return "jsonView";
	}
	
	@RequestMapping("delete/{teamId}")
	public String delete(ModelMap modelMap,Long projectTeamId, @PathVariable Long teamId){
		boolean result = false;
		if(teamId!=null){
			ProjectTeam team = projectTeamManager.getByObjectId(teamId);
			if(team.getHeadCount()==0){
				projectTeamManager.delete(team);
				result = true;
			}
		}
		modelMap.addAttribute("result", result);
		return "jsonView";
	}
	
	@RequestMapping("get/{teamId}")
	public String get(ModelMap modelMap, @PathVariable Long teamId){
		if(teamId!=null){
			ProjectTeam team = projectTeamManager.getByObjectId(teamId);
			modelMap.addAttribute("team", team);
		}
		return "jsonView";
	}
	
	@RequestMapping("getMembers/{teamId}")
	public String getMembers(ModelMap modelMap, @PathVariable Long teamId){
		if(teamId!=null){
			List<Staff> members = staffManager.getProjectTeamMembers(teamId);
			modelMap.addAttribute("members", members);
		}
		return "jsonView";
	}
}
