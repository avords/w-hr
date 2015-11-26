package com.handpay.ibenefit.excitationActivity.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.excitationActivity.entity.ActivityItem;
import com.handpay.ibenefit.excitationActivity.entity.ActivityRewardInfo;
import com.handpay.ibenefit.excitationActivity.entity.ActivityUser;
import com.handpay.ibenefit.excitationActivity.entity.ExcitationActivity;
import com.handpay.ibenefit.excitationActivity.service.IActivityItemManager;
import com.handpay.ibenefit.excitationActivity.service.IActivityRewardInfoManager;
import com.handpay.ibenefit.excitationActivity.service.IActivityUserManager;
import com.handpay.ibenefit.excitationActivity.service.IExcitationActivityManager;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.service.Manager;
import com.handpay.ibenefit.framework.util.ExcelUtil;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageSearchInit;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.framework.util.UeditorUtils;
import com.handpay.ibenefit.framework.web.PageController;
import com.handpay.ibenefit.member.entity.CompanyDepartment;
import com.handpay.ibenefit.member.service.ICompanyDepartmentManager;
import com.handpay.ibenefit.member.service.IStaffManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.service.IWelfareManager;

/**
 * @desc   激励活动管理
 * @author mwu
 * @date   2015年6月15日下午3:16:57
 */
@Controller
@RequestMapping("/excitationActivity")
public class ExcitationActivityController extends PageController<ExcitationActivity>{
	
	private static final Logger logger = Logger.getLogger(ExcitationActivityController.class);
	
	private static final String HOME_DIR = "excitationActivity";
	
	@Reference(version = "1.0")
    private IExcitationActivityManager exActivityManager;
	@Reference(version="1.0")
	private IActivityRewardInfoManager acRewardInfoManager;
	@Reference(version="1.0")
	private IActivityUserManager  acUserManager;
	@Reference(version="1.0")
	private IWelfareManager welfareManager;
	@Reference(version="1.0")
	private IActivityItemManager acItemManager;
	@Reference(version="1.0")
	private IUserManager userManager;
	@Reference(version="1.0")
	private IStaffManager staffManager;
	@Reference(version="1.0")
	private ICompanyDepartmentManager cDepartmentManager;
	@Reference(version="1.0")
	private IDictionaryManager dictionaryManager;
	/**
	 * 加载激励活动数据
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/queryActivityList")
	public String queryActivityList(HttpServletRequest request,HttpServletResponse response,ExcitationActivity exActivity,Integer backPage){
		try{
			//1、设置当前页面
			PageSearch page  = preparePage(request);
			page = PageSearchInit.initcurrentPage(page, request);
			//2、查询激励活动数据
			User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			if(!UserUtils.isCompanyAdmin()){//非企业管理员 -- 只能查询当前用户发布的活动
				page.getFilters().add(new PropertyFilter("ExcitationActivity", "EQL_userId", String.valueOf(user.getObjectId())));
			}
			page.getFilters().add(new PropertyFilter("ExcitationActivity","EQL_companyId",user.getCompanyId()+""));
			page = exActivityManager.queryExcitationActivity(page);
			/*if(page.getTotalCount()==0){
				page.setTotalCount(1);
	        }*/
			request.setAttribute("pageActivity", page);
			//3、加载活动分类数据、企业活动分类表
			queryActivityItemList(request,null);
		}catch(Exception e){
			logger.error("queryActivityList",e);
		} 
		return HOME_DIR+"/listExcitationActivity";
	}
	/**
	 * 新增激励活动数据
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/createActivity")
	public String createActivity(HttpServletRequest request,HttpServletResponse response){
		try{
			request.setAttribute("ecActivity", new ExcitationActivity());
			queryActivityItemList(request,null);
		}catch(Exception e){
			logger.error("createActivity",e);
		}
		return HOME_DIR+"/editExcitationActivity";
	}
	/**
	 * 修改激励活动数据
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/editActivity")
	public String editActivity(HttpServletRequest request,HttpServletResponse response,String objectId){
		try{
			if(StringUtils.isNotBlank(objectId)){
				//1、获取激励活动
				ExcitationActivity ecActivity = exActivityManager.getByObjectId(Long.valueOf(objectId)); 
				request.setAttribute("ecActivity", ecActivity);
				
				//2、获取激励活动参与人员信息
				ActivityUser acUser = new ActivityUser();
				acUser.setTitleId(Long.valueOf(objectId));
				acUser.setDeleted(0);
			    List<ActivityUser> acUserList= acUserManager.getBySample(acUser);
			    request.setAttribute("acUserList", acUserList);
			    
			    //3、获取激励活动规则明细
			    queryActivityRewardInfoList(request,Long.valueOf(objectId));
			    
				//4、加载活动分类数据
				queryActivityItemList(request,null);
			}
		}catch(Exception e){
			logger.error("editActivity",e);
		}
		return HOME_DIR+"/editExcitationActivity";
	}
	/**
	 * 删除激励活动数据
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/deleteActivity")
	public String deleteActivity(HttpServletRequest request,HttpServletResponse response,String objectId){
		try{
			//激励活动数据--逻辑删除
			if(StringUtils.isNotBlank(objectId)){
			   delete(Long.valueOf(objectId));
			}
		}catch(Exception e){
			logger.error("deleteActivity",e);
		}
		return "redirect:/excitationActivity/queryActivityList";
	}
	/**
	 * 保存激励活动数据
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/saveActivity")
	public String createActivity(HttpServletRequest request,HttpServletResponse response,ExcitationActivity exActivity){
		try{
			String objectId = request.getParameter("objectId");    //用来区分是做新增 还是修改
			exActivity.setActivityContent(UeditorUtils.convertSpace(exActivity.getActivityContent()));
			User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			String applyBeginTime = request.getParameter("applyBeginTime");//报名开始时间
			String applyEndTime = request.getParameter("applyEndTime");//报名结束时间
			String activityBeginTime = request.getParameter("activityBeginTime");//活动开始时间
			String activityEndTime = request.getParameter("activityEndTime");//活动结束时间
			SimpleDateFormat simpleDateApplyFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");   //报名时间精确到时分秒
			SimpleDateFormat simpleDateActivityFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//活动时间精确到时分秒
			//报名开始日期--结束日期
			if (applyBeginTime != null && !"".equals(applyBeginTime)&& applyEndTime != null && !"".equals(applyEndTime)){
				exActivity.setApplyBeginDate(simpleDateApplyFormat.parse(applyBeginTime));
				exActivity.setApplyEndDate(simpleDateApplyFormat.parse(applyEndTime));
			}
			//活动开始日期--结束日期
			if(activityBeginTime != null && !"".equals(activityBeginTime) && activityEndTime != null && !"".equals(activityEndTime)){
				exActivity.setActivityBeginDate(simpleDateActivityFormat.parse(activityBeginTime));
				exActivity.setActivityEndDate(simpleDateActivityFormat.parse(activityEndTime));
			}
			if(objectId!=null&&!"".equals(objectId)){    //修改数据
				/**
				 * 修改激励活动信息
				 */
				exActivity.setUpdatedBy(user.getObjectId());
				exActivity.setUpdatedOn(new Date());
				exActivity.setCompanyId(user.getCompanyId());
				exActivity = exActivityManager.updateById(exActivity);
				/**
				 * 修改发布对象信息
				 */
				//先删除发布过的对象
				ActivityUser acUser = new ActivityUser();
				acUser.setTitleId(exActivity.getObjectId());
				List<ActivityUser> userList = acUserManager.getBySample(acUser);
				if(userList !=null && userList.size()>0){
					acUserManager.deleteBySample(acUser);
				}
				//再保存最新的发布对象
				saveActivityUser(request, exActivity.getObjectId());
				/**
				 * 修改激励活动奖励规则信息
				 */
				updateActivityReward(request, exActivity.getObjectId());
			}else{ 
				/**
				 * 新增数据
			     */
				//激励活动管理
				exActivity.setCreatedOn(new Date());                        //设置激励活动记录创建时间
				exActivity.setCreatedBy(user.getObjectId());
				exActivity.setCompanyId(user.getCompanyId());
				exActivity = exActivityManager.save(exActivity);            //保存激励管理活动列表
				Long titleId = exActivity.getObjectId();
				//激励活动规则明细
				saveActivityReward(request, titleId);
				//激励活动参与人员
				saveActivityUser(request, titleId);
			}
		}catch(Exception e){
			logger.error("saveActivity",e);
		}
		return "redirect:/excitationActivity/queryActivityList";
	}
	/**
	 * 查看激励活动详情
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/activityDetatil")
	public String activityDetatil(HttpServletRequest request,HttpServletResponse response,String objectId,String itemName){
		try{
			if(StringUtils.isNotBlank(objectId)){
				//保存激励活动对象
				ExcitationActivity exActivity = exActivityManager.getByObjectId(Long.valueOf(objectId));
				
				//保存活动类目名称
				exActivity.setItemName(itemName);
				
				//获取创建人
				Long createBy = exActivity.getCreatedBy();
				exActivity.setCreatedUser(queryUserName(createBy));
				
				//获取修改人
				Long updateBy = exActivity.getUpdatedBy();
				exActivity.setUpdatedUser(queryUserName(updateBy));
				
				request.setAttribute("exActivity", exActivity); 
				//保存激励活动规则明细list
				ActivityRewardInfo acRewardInfo = new ActivityRewardInfo();
				acRewardInfo.setTitleId(Long.valueOf(objectId));
				List<ActivityRewardInfo> acList = acRewardInfoManager.getBySample(acRewardInfo);
				logger.info(acList.size());
				request.setAttribute("acList", acList); 
				
				//获取激励活动参与人员信息
				ActivityUser acUser = new ActivityUser();
				acUser.setTitleId(Long.valueOf(objectId));
				acUser.setDeleted(0);
			    List<ActivityUser> acUserList= acUserManager.getBySample(acUser);
			    request.setAttribute("acUserList", acUserList);
			}
	    }catch(Exception e){
			logger.error("activityDetatil",e);
		}
		return HOME_DIR+"/excitationActivityDetatil";
	}
	/**
	 * 更新激励活动状态
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/updateActivityStatus")
	public String updateActivityStatus(HttpServletRequest request,HttpServletResponse response,String objectId,String status){
		try{
			if(StringUtils.isNotBlank(objectId) && StringUtils.isNotBlank(status)){
				ExcitationActivity exActivity = exActivityManager.getByObjectId(Long.valueOf(objectId));
				exActivity.setStatus(Integer.valueOf(status));
				exActivityManager.updateById(exActivity);
			}
			
		}catch(Exception e){
			logger.error("updateActivityStatus",e);
		}
		return "redirect:/excitationActivity/queryActivityList";
	}
	/**
	 * 查看激励活动报名人员列表
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/activityUserInfo/{objectId}")
	public String activityUserInfo(HttpServletRequest request,HttpServletResponse response,@PathVariable Long objectId){
		try{
			//1、加载激励活动数据
			ExcitationActivity exActivity = exActivityManager.getByObjectId(objectId);
			request.setAttribute("exActivity", exActivity);    //保存激励活动对象
			//2、加载部门信息
			User user =(User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			Long companyId = user.getCompanyId();
			if(companyId != null){
				List<CompanyDepartment> deptList = cDepartmentManager.getCompanyDepartmentsByCompanyId(companyId);
				request.setAttribute("deptList", deptList);
			}
			List<Dictionary> dictionaryList = dictionaryManager.getDictionariesByDictionaryId(IBSConstants.SEX_CONSTANT);
			request.setAttribute("dictionaryList", dictionaryList);
			//3、设置当前页码
			PageSearch page  = preparePage(request);
			page = PageSearchInit.initcurrentPage(page, request);
			//4、查询报名活动人员信息
			page.getFilters().add(new PropertyFilter("ExcitationActivity","EQL_objectId",objectId+""));
			page = exActivityManager.queryActivityUserInfo(page);
			/*if(page.getTotalCount()==0){
				page.setTotalCount(1);
	        }*/
			request.setAttribute("pageUserInfo", page);
	    }catch(Exception e){
			logger.error("activityUserInfo",e);
		}
		return HOME_DIR+"/listExcitationUser";
	}
	/**
	 * 批量删除激励活动数据
	 * @param request
	 * @param response
	 * @param exActivity
	 * @return
	 */
	@RequestMapping("/batchDetele")
	public String batchDetele(HttpServletRequest request,HttpServletResponse response,ExcitationActivity exActivity){
		try{
			String[] exinfoIdStr = request.getParameterValues("exinfoId");
			if(exinfoIdStr!=null&&exinfoIdStr.length>0){
				for(int i =0;i<exinfoIdStr.length;i++){
					if(exinfoIdStr[i]==null||"".equals(exinfoIdStr[i])){
						continue;
					}
					Long objectId = Long.valueOf(exinfoIdStr[i]);
					logger.info("objectId=="+objectId);
					//逻辑删除激励活动数据
					delete(objectId);
				}
			}
		}catch(Exception e){
			logger.error("batchDetele",e);
		}
		return "redirect:/excitationActivity/queryActivityList";
	}
	/**
	 * 置顶激励活动数据
	 * @param request
	 * @param response
	 * @param exActivity
	 * @return
	 */
	@RequestMapping("/activityTop")
	public String activityTop(HttpServletRequest request,HttpServletResponse response,String objectId){
		try{
			if(StringUtils.isNotBlank(objectId)){
				ExcitationActivity exActivity = exActivityManager.getByObjectId(Long.valueOf(objectId));
				logger.info("new Date().getTime()=="+new Date().getTime());
				exActivity.setStickFlag(new Date().getTime());
				exActivityManager.updateById(exActivity);
			}
		}catch(Exception e){
			logger.error("activityTop",e);
		}
		return "redirect:/excitationActivity/queryActivityList";
	}
	/**
	 * 新增活动分类
	 */
	@RequestMapping("/addTitle")
	public String addTitle(ModelMap modelMap,HttpServletRequest request,HttpServletResponse response){
		 try{
			 String newTitle = request.getParameter("newTitle");
			 if(StringUtils.isNotBlank(newTitle)){
				 User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
				 ActivityItem item = new ActivityItem();
				 item.setActivityName(newTitle);
				 item.setDeleted(0);
				 item.setCompanyId(user.getCompanyId());
				 List<ActivityItem> itemList = acItemManager.getBySample(item);
				 if(itemList != null && itemList.size() == 1){
					 ActivityItem itemNew = itemList.get(0);
					 modelMap.addAttribute("itemNew", itemNew);
				 }
				 else{
					 ActivityItem acItem = new ActivityItem();
					 acItem.setActivityName(newTitle);
					 acItem.setCreatedBy(user.getObjectId());
					 acItem.setCreatedOn(new Date());
					 acItem.setCompanyId(user.getCompanyId());
					 acItem = acItemManager.save(acItem);
					 modelMap.addAttribute("acItem", acItem);
				 }
				 return "jsonView"; 
			 }
		 }catch(Exception e){
			 logger.error("addTitle",e);
		 }
		 return null;
	}
	/**
	 * 删除活动分类
	 */
	@RequestMapping("/delTitle")
	public String delTitle(ModelMap modelMap,HttpServletRequest request,HttpServletResponse response){
		 try{
		    User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		    String delId = request.getParameter("delId");
			if (StringUtils.isNotBlank(delId)) {
				ExcitationActivity exActivity = new ExcitationActivity();
				exActivity.setDeleted(0);
				exActivity.setActivityCategoriesId(Long.valueOf(delId));
				exActivity.setCompanyId(user.getCompanyId());
				List<ExcitationActivity> exActivityList = exActivityManager.getBySample(exActivity);
				if (exActivityList != null && exActivityList.size() > 0) {      //判断当前活动分类是否在使用，存在在使用中的活动 则不能删除当前活动分类
					modelMap.addAttribute("delResult", "2");
				} else {                 //删除当前活动分类
					ActivityItem acItem = new ActivityItem();
					acItem.setUpdatedBy(user.getObjectId());
					acItem.setUpdatedOn(new Date());
					acItem.setDeleted(1);
					acItem.setObjectId(Long.valueOf(delId));
					acItemManager.delete(acItem);
					modelMap.addAttribute("delResult", "0");
				}
			}
		 }catch(Exception e){
			 logger.error("delTitle",e);
			 modelMap.addAttribute("delResult", "1");
		 }
		 return "jsonView";
	}
	/**
	 * 校验活动标题的唯一性
	 * @param request
	 * @param response
	 * @param objectId
	 * @return
	 */
	@RequestMapping(value = "/isUniqueTitle")
	public String isUniqueTitle(HttpServletRequest request, HttpServletResponse response, Long objectId,String title){
		boolean result = false;
		try {
			User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			ExcitationActivity exActivity = new ExcitationActivity();
			exActivity.setTitle(title);
			exActivity.setDeleted(0);
			exActivity.setCompanyId(user.getCompanyId());
			List<ExcitationActivity> exActivityList = exActivityManager.getBySample(exActivity);
			if(exActivityList != null && exActivityList.size() == 1){
				result =  true;
			}
			response.getWriter().print(result);
		} catch (Exception e) {
			logger.error("isUniqueTitle",e);
		}
		return null;
	}
	
	/**
	 * 删除活动奖励明细
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/deleteRewardInfo")
	public String deleteRewardInfo(ModelMap modelMap,HttpServletRequest request,HttpServletResponse response,Long objectId){
		try{
			//物理删除活动奖励明细规则数据
			acRewardInfoManager.delete(objectId);
			modelMap.addAttribute("delResult", "0");  //删除成功
		}catch(Exception e){
			logger.error("deleteRewardInfo",e);
			modelMap.addAttribute("delResult", "1");  //删除出错
		}
		return "jsonView";
	}
	
	/**
	 * 导出激励活动excel记录
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/exportExcelByActivity")
	public String exportExcelByActivity(HttpServletRequest request,HttpServletResponse response){
		try {
			try {
				SimpleDateFormat simpleDateActivityFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//活动时间精确到时分秒
				String[] titles = {"标题", "奖励(分)", "活动类目", "报名人数", "创建日期",
						"有效日期","状态"};
				PageSearch page = preparePage(request);
				User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
				if(!UserUtils.isCompanyAdmin()){//非企业管理员 -- 只能查询当前用户发布的活动
					page.getFilters().add(new PropertyFilter("ExcitationActivity", "EQL_userId", String.valueOf(user.getObjectId())));
				}
				page.setPageSize(10000);   //设置最大的导出条数
				page.getFilters().add(new PropertyFilter("ExcitationActivity","EQL_companyId",user.getCompanyId()+""));
				List<ExcitationActivity> exList = exActivityManager.queryExActivityExport(page);
				
				List<Object[]> datas = new ArrayList<Object[]>();
				for (ExcitationActivity exActivity : exList) {
					Object[] arr = new Object[7];
					arr[0] = exActivity.getTitle();
					List<ActivityRewardInfo> acList = exActivity.getAcList();
					String ac = "";
					String str ="";
					String welfarePointName = (String)request.getSession().getAttribute(SecurityConstants.COMPANY_WELFARE_POINT_NAME);
					if(acList != null && acList.size()>0){
						for(int i =0 ;i<acList.size();i++){
							if(acList.get(i).getExcitationRule() == null || "".equals(acList.get(i).getExcitationRule())){
								ac = "奖励"+welfarePointName+":"+acList.get(i).getReward()+"\n";
							}else{
								str = "奖励规则\n";
								ac+= acList.get(i).getExcitationRule()+"：奖励"+welfarePointName+acList.get(i).getReward()+"\n";
							}
						}
					}
					arr[1] = str+ac;
					arr[2] = exActivity.getItemName();
					arr[3] = exActivity.getCanApplyPeople();
					arr[4] = simpleDateActivityFormat.format(exActivity.getCreatedOn());
					
					if(exActivity.getActivityBeginDate() == null && exActivity.getActivityEndDate() == null){
						arr[5] = "全年有效";
					}else{
						arr[5] = simpleDateActivityFormat.format(exActivity.getActivityBeginDate())+"至"+simpleDateActivityFormat.format(exActivity.getActivityEndDate());
					}
					if(exActivity.getStatus() == IBSConstants.INFOMATION_DRAFT){
						arr[6] = "草稿";
					}else if(exActivity.getStatus() == IBSConstants.INFOMATION_PUBLISH){
						arr[6] = "发布中";
					}else if(exActivity.getStatus() == IBSConstants.INFOMATION_DELAY){
						arr[6] = "已过期";
					}else{
						arr[6] = "";
					}
					datas.add(arr);
				}
				ExcelUtil excelUtil = new ExcelUtil();
				excelUtil.exportExcel(response, datas, titles, "activity.xls");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		} catch (Exception e) {
			logger.error("exportExcelByActivity",e);
		}
		return null;
	}
	/**
	 * 导出报名人员excel记录
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/exportExcelByUser/{objectId}")
	public String exportExcelByUser(HttpServletRequest request,HttpServletResponse response,@PathVariable Long objectId){
		try {
			try {
				String[] titles = {"工号", "姓名", "性别", "手机号", "部门","邮箱"};
				//查询性别字典信息
				List<Dictionary> dictionaryList = dictionaryManager.getDictionariesByDictionaryId(IBSConstants.SEX_CONSTANT);
				PageSearch page = preparePage(request);
				page.setPageSize(10000);   //设置最大的导出条数
				page.getFilters().add(new PropertyFilter("ExcitationActivity","EQL_objectId",objectId+""));
				List<ExcitationActivity> exList = exActivityManager.queryActivityUserInfoExport(page);
				List<Object[]> datas = new ArrayList<Object[]>();
				for (ExcitationActivity exActivity : exList) {
					Object[] arr = new Object[6];
					arr[0] = exActivity.getWorkNo();
					arr[1] = exActivity.getStaffName();
					String sex = "";//性别
					for (Dictionary dict:dictionaryList) {
						if(exActivity.getSex().equals(dict.getValue())){
							sex = dict.getName();
							break;
						}
					}
					arr[2] = sex;
					arr[3] = exActivity.getTelephone();
					arr[4] = exActivity.getDeptName();
					arr[5] = exActivity.getEmail();
					
					datas.add(arr);
				}
				ExcelUtil excelUtil = new ExcelUtil();
				excelUtil.exportExcel(response, datas, titles, "user.xls");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		} catch (Exception e) {
			logger.error("exportExcelByUser",e);
		}
		return null;
	}
	/**
	 * 新增激励活动规则明细
	 * @param request
	 * @param titleId
	 */
	public void saveActivityReward(HttpServletRequest request,Long titleId){
		/**
		 * 激励活动规则明细
		 */
		String type =  request.getParameter("type"); //活动规则类型  1、单项奖励  2、多项奖励
		String score = request.getParameter("score");//单项奖励
		User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		if(type!=null&&!"".equals(type)){
			if(Integer.valueOf(type) == IBSConstants.SINGLE_REWARD_TYPE){   //1、单项奖励
				if(score!=null && !"".equals(score)){
					//单项奖励数据入库
					insertRewarInfoByOne(score, titleId, user);
				}
			}else if(Integer.valueOf(type) == IBSConstants.MULTITERM_REWARD_TYPE){ //2、多项奖励
				String excitationSize = request.getParameter("excitationSize"); //奖励规则长度
				if(excitationSize!=null&&!"".equals(excitationSize)){
					int exSize = Integer.valueOf(excitationSize);
					for(int i=0;i<=exSize;i++){
						String parames="excitation"+i;
						if(i==0){
							parames="excitation";
						}
						String[] excitationStr = request.getParameterValues(parames);
						if(excitationStr!=null&&excitationStr.length==3){
							 if(excitationStr[0]!=null && !"".equals(excitationStr[0])  
										&& excitationStr[1]!=null && !"".equals(excitationStr[1]) 
										&& (excitationStr[2]==null || "".equals(excitationStr[2]))){
								 //多项奖励数据入库
								 insertRewardInfoByMore(excitationStr[0],excitationStr[1],titleId,user);
							}
						}
					}
				}
			}
		}
	}
	/**
	 * 修改激励活动规则明细
	 * @param request
	 * @param titleId
	 */
	public void updateActivityReward(HttpServletRequest request,Long titleId){
		/**
		 * 激励活动规则明细
		 */
		try{
			String type =  request.getParameter("type"); //活动规则类型  1、单项奖励  2、多项奖励
			String score = request.getParameter("score");//单项奖励
			User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			if (type != null && !"".equals(type)) {
				if(Integer.valueOf(type) == IBSConstants.SINGLE_REWARD_TYPE){   //1、单项奖励
					if(score!=null && !"".equals(score)){
						List<ActivityRewardInfo> rewardInfoList = rewardInfoList(user, titleId);
						int k = 0;    //用来判断多条奖励规则被删除完的标识
 						if(rewardInfoList !=null && rewardInfoList.size()>0){
							for(int i=0;i<rewardInfoList.size();i++){
								ActivityRewardInfo rewardInfo = rewardInfoList.get(i);
								int rewardType = rewardInfo.getType();
								if(rewardType == IBSConstants.MULTITERM_REWARD_TYPE){      //活动奖励是多项奖励  则物理删除活动明细
									acRewardInfoManager.delete(rewardInfo.getObjectId());
									k++;
								}else if(rewardType == IBSConstants.SINGLE_REWARD_TYPE){ //活动奖励是单项奖励  则更新当前的单项奖励
									rewardInfo.setUpdatedBy(user.getObjectId());
									rewardInfo.setUpdatedOn(new Date());
									rewardInfo.setExcitationRule("");
									rewardInfo.setReward(score);
									acRewardInfoManager.updateById(rewardInfo);
								}
							}
							if(k==rewardInfoList.size()){   //多项奖励删除掉之后，新增单项奖励数据
								insertRewarInfoByOne(score, titleId, user);
							}
						}else{   //当前活动不存在奖励规则，则直接新增
							insertRewarInfoByOne(score, titleId, user);
						}
					}else{     //活动原本是单项奖励，将单项的奖励值去掉了 则直接删掉数据库里面的记录
						List<ActivityRewardInfo> rewardInfoList = rewardInfoList(user, titleId);
						if (rewardInfoList != null && rewardInfoList.size() > 0) {
							for (int i = 0; i < rewardInfoList.size(); i++) {
								ActivityRewardInfo rewardInfo = rewardInfoList.get(i);
								int rewardType = rewardInfo.getType();
								if (rewardType == IBSConstants.SINGLE_REWARD_TYPE) { //活动奖励是单项奖励 则物理删除活动明细
									acRewardInfoManager.delete(rewardInfo.getObjectId());
								}
							}
						}
					}
				}else if(Integer.valueOf(type) == IBSConstants.MULTITERM_REWARD_TYPE){ //2、多项奖励
					List<ActivityRewardInfo> rewardInfoList = rewardInfoList(user, titleId);
					if (rewardInfoList != null && rewardInfoList.size() > 0) {
						for (int i = 0; i < rewardInfoList.size(); i++) {
							ActivityRewardInfo rewardInfo = rewardInfoList.get(i);
							int rewardType = rewardInfo.getType();
							if (rewardType == IBSConstants.SINGLE_REWARD_TYPE) { //活动奖励是单项奖励 则物理删除活动明细
								acRewardInfoManager.delete(rewardInfo.getObjectId());
							}
						}
					}
					String excitationSize = request.getParameter("excitationSize"); //奖励规则长度
					if(excitationSize!=null&&!"".equals(excitationSize)){
						int exSize = Integer.valueOf(excitationSize);
						if(exSize > 0 ){
							for(int i=0;i<=exSize;i++){
								String parames="excitation"+i;
								if(i==0){
									parames="excitation";
								}
								String[] excitationStr = request.getParameterValues(parames);
								if(excitationStr!=null&&excitationStr.length==3){      //多项奖励规则，修改了规则名称或者奖励 则更新数据
									if(excitationStr[0]!=null && !"".equals(excitationStr[0])
											&& excitationStr[1]!=null && !"".equals(excitationStr[1]) 
											&& excitationStr[2]!=null && !"".equals(excitationStr[2])){
										Long objectId = Long.valueOf(excitationStr[2]);   //存放每条奖励规则对应的objectId
										ActivityRewardInfo aRewardInfo = acRewardInfoManager.getByObjectId(objectId);
										aRewardInfo.setExcitationRule(excitationStr[0]);
										aRewardInfo.setReward(excitationStr[1]);
										aRewardInfo.setUpdatedBy(user.getObjectId());
										aRewardInfo.setUpdatedOn(new Date());
										acRewardInfoManager.updateById(aRewardInfo); //保存激励活动规则明细列表
									}else if(excitationStr[0]!=null && !"".equals(excitationStr[0])   //如果存在objectId为空的数据，则新增
											&& excitationStr[1]!=null && !"".equals(excitationStr[1]) 
											&& (excitationStr[2]==null || "".equals(excitationStr[2]))){
										insertRewardInfoByMore(excitationStr[0], excitationStr[1], titleId, user);
									}
								}
							}
						}else{  //活动原本是多项奖励，将多项奖励在页面上都移除掉了，则删除数据库对应的记录
							List<ActivityRewardInfo> rewardInfoMore = rewardInfoList(user, titleId);
	 						if(rewardInfoMore !=null && rewardInfoMore.size()>0){
								for(int i=0;i<rewardInfoMore.size();i++){
									ActivityRewardInfo info = rewardInfoMore.get(i);
									int rewardType = info.getType();
									if(rewardType == IBSConstants.MULTITERM_REWARD_TYPE){      //活动奖励是多项奖励  则物理删除活动明细
										acRewardInfoManager.delete(info.getObjectId());
									}
								}
	 						}
						}
					}
				}
			}	
		}catch(Exception e){
			logger.error("updateActivityReward",e);
		}
	}
	/**
	 * 新增/修改激励活动参与人员
	 * @param request
	 * @param titleId
	 */
	public void saveActivityUser(HttpServletRequest request,Long titleId){
		/**
		 * 激励活动参与人员
		 */
	   try{
		    User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			String memberStr = request.getParameter("members");
			if (memberStr != null && !"".equals(memberStr)){
				String[] members = memberStr.split(",");
				for (String member : members) {
					String memberId = member.split("\\|")[0];
					ActivityUser acUser = new ActivityUser();
					acUser.setTitleId(titleId);
					acUser.setUserType("");
					acUser.setUserId(Long.valueOf(memberId));
					acUser.setCreatedOn(new Date()); // 设置激励活动参与人员创建时间
					acUser.setCreatedBy(user.getObjectId());
					acUser.setCompanyId(user.getCompanyId());
					if(user.getOrganizationId() !=null){
			    		acUser.setOrganizationId(user.getOrganizationId());
			    	}
					acUserManager.save(acUser); // 保存激励活动参与人员
				}
			}else{
				String userAll = request.getParameter("userAll"); // 选中全体员工的标识
				if (userAll != null && !"".equals(userAll)) {
					ActivityUser acUser = new ActivityUser();
					acUser.setTitleId(titleId);
					acUser.setUserType(userAll);
					acUser.setUserId(null);
					acUser.setCreatedOn(new Date()); // 设置激励活动参与人员创建时间
					acUser.setCreatedBy(user.getObjectId());
					int userType = user.getType();   //用户类型
				    if(userType == IBSConstants.USER_TYPE_COMPANY_HR){  //企业HR
				    	if(user.getOrganizationId() !=null){
				    		acUser.setOrganizationId(user.getOrganizationId());
				    	}
					}
					acUser.setCompanyId(user.getCompanyId());
					acUserManager.save(acUser); // 保存激励活动参与人员
				}
			}
	   }catch(Exception e){
		   logger.error("saveActivityUser",e);
	   }
	}
	/**
	 * 查询活动分类数据
	 * @param request
	 */
	public void queryActivityItemList(HttpServletRequest request,Long activityCategoriesId){
		List<WelfareItem> welfareItemList = exActivityManager.queryWelfareItemList(activityCategoriesId);
		if(welfareItemList !=null && welfareItemList.size()>0){
			WelfareItem weItem = welfareItemList.get(0);
			request.setAttribute("weItem", weItem);
		}
		request.setAttribute("welfareItemList",welfareItemList);
		
		User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		List<ActivityItem> acItemList = exActivityManager.queryActivityItemList(user.getCompanyId(),null);
		request.setAttribute("acItemList",acItemList);
	}
	/**
	 * 获取激励活动规则信息
	 */
	public void queryActivityRewardInfoList(HttpServletRequest request,Long objectId){
		//获取激励活动规则明细
	    ActivityRewardInfo acRewardInfo = new ActivityRewardInfo();
	    acRewardInfo.setTitleId(objectId);
	    List<ActivityRewardInfo> acRewardInfoList = acRewardInfoManager.getBySample(acRewardInfo);
	    if(acRewardInfoList !=null){
	    	if(acRewardInfoList.size() == 1){
	    		if(acRewardInfoList.get(0).getType() == 1){  //1、单项奖励  2、多项奖励
	    			ActivityRewardInfo acInfo = acRewardInfoList.get(0);
		    		request.setAttribute("acInfo", acInfo);
	    		}
	    		else{
	    			request.setAttribute("acRewardInfoList", acRewardInfoList);
	    		}
	    	}
	    	else if(acRewardInfoList.size() >1){
	    		request.setAttribute("acRewardInfoList", acRewardInfoList);
	    	}
	    }
	}
	/**
	 * 插入激励活动规则数据---单项奖励
	 * @param score
	 * @param titleId
	 * @param user
	 */
	public void insertRewarInfoByOne(String score,Long titleId,User user){
		ActivityRewardInfo aRewardInfo = new ActivityRewardInfo();
		aRewardInfo.setExcitationRule("");
		aRewardInfo.setReward(score);
		aRewardInfo.setType(1);                       //单项奖励类型
		aRewardInfo.setTitleId(titleId);
		aRewardInfo.setCreatedOn(new Date());         //设置激励活动规则明细创建时间
		aRewardInfo.setCreatedBy(user.getObjectId());
		aRewardInfo.setCompanyId(user.getCompanyId());
		acRewardInfoManager.save(aRewardInfo);        //保存激励活动规则明细列表
	}
	/**
	 * 插入激励活动规则数据---多项奖励
	 * @param rule
	 * @param reward
	 * @param titleId
	 * @param user
	 */
	public void insertRewardInfoByMore(String rule,String reward,Long titleId,User user){
		ActivityRewardInfo aRewardInfo = new ActivityRewardInfo();
		aRewardInfo.setExcitationRule(rule);
		aRewardInfo.setReward(reward);
		aRewardInfo.setType(2);
		aRewardInfo.setTitleId(titleId);
		aRewardInfo.setCreatedOn(new Date());  //设置激励活动规则明细创建时间
		aRewardInfo.setCreatedBy(user.getObjectId());
		aRewardInfo.setCompanyId(user.getCompanyId());
		acRewardInfoManager.save(aRewardInfo); //保存激励活动规则明细列表
	}
	/**
	 * 获取当前活动的奖励明细列表
	 * @param user
	 * @param titleId
	 * @return
	 */
	public List<ActivityRewardInfo> rewardInfoList(User user,Long titleId){
		ActivityRewardInfo aRewardInfoNew = new ActivityRewardInfo();   //查询当前企业 该活动对应的有效的奖励规则数据 (0：正常的未被删除的数据)
		aRewardInfoNew.setCompanyId(user.getCompanyId());
		aRewardInfoNew.setTitleId(titleId);
		aRewardInfoNew.setDeleted(0);
		List<ActivityRewardInfo> rewardInfoList = acRewardInfoManager.getBySample(aRewardInfoNew);
		return rewardInfoList;
	}
	/**
	 * 显示当前用户的名称
	 */
	public String queryUserName(Long createBy){
		String userName = "";
		if(createBy !=null){
			User userCreate = new User();
			userCreate.setObjectId(createBy);
		    userName = userManager.getBySample(userCreate).get(0).getUserName();
		}
		return userName;
	}
	@Override
	public Manager<ExcitationActivity> getEntityManager() {
		return exActivityManager;
	}
	@Override
	public String getFileBasePath() {
		return "excitationActivity";
	}
}
