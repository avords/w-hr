package com.handpay.ibenefit.excitationActivity.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
import com.handpay.ibenefit.excitationActivity.entity.ActivityItem;
import com.handpay.ibenefit.excitationActivity.entity.ActivityRewardInfo;
import com.handpay.ibenefit.excitationActivity.entity.ExcitationActivity;
import com.handpay.ibenefit.excitationActivity.service.IActivityItemManager;
import com.handpay.ibenefit.excitationActivity.service.IActivityRewardInfoManager;
import com.handpay.ibenefit.excitationActivity.service.IExcitationActivityManager;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.DateUtils;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.UploadFile;
import com.handpay.ibenefit.member.service.ICompanyPublishedManager;
import com.handpay.ibenefit.point.entity.PointDistrubute;
import com.handpay.ibenefit.point.service.IPointDistrubuteItemManager;
import com.handpay.ibenefit.point.service.IPointDistrubuteManager;
import com.handpay.ibenefit.point.service.IPointDistrubuteStaffManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IPointOperateManager;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.service.IWelfareManager;

/**
 * @desc   HR激励管理---发放奖励
 * @author mwu
 * @date   2015年6月19日上午9:18:55
 */
@Controller
@RequestMapping("grantExcitationReward")
public class GrantExcitationRewardController{
	private static final Logger logger = Logger.getLogger(GrantExcitationRewardController.class);
	
	private static final String HOME_DIR = "excitationActivity";
	@Reference(version = "1.0")
    private IActivityItemManager activityItemManager;
	@Reference(version="1.0")
	private IWelfareManager welfareManager;
	@Reference(version = "1.0")
    private IExcitationActivityManager exActivityManager;
	@Reference(version = "1.0")
	private IPointDistrubuteManager poManager;
	@Reference(version = "1.0")
	private IPointDistrubuteStaffManager poStaffManager;
	@Reference(version = "1.0")
	private ICompanyPublishedManager cPublishedManager;
	@Reference(version="1.0")
	private IPointDistrubuteManager pointDistrubuteManager;
	@Reference(version = "1.0")
	private IPointDistrubuteItemManager pointDistrubuteItemManager;
	@Reference(version="1.0")
	private IUserManager userManager;
	@Reference(version="1.0")
	private IActivityRewardInfoManager acRewardInfoManager;
	@Reference(version="1.0")
	private IPointOperateManager pointOperateManager;
	/**
	 * 加载下拉框数据
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/grantReward")
	public String grantReward(HttpServletRequest request,HttpServletResponse response){
		try{
			logger.info("激励管理--->发放奖励!");
			//1、获取当前账户信息
			User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			request.setAttribute("user", user);
			//2、加载活动分类数据、企业活动分类表
			List<ActivityItem> acItemList = exActivityManager.queryActivityItemList(user.getCompanyId(),null);
			request.setAttribute("acItemList",acItemList);
			Long activityCategoriesId = null;
			List<WelfareItem> welfareItemList = exActivityManager.queryWelfareItemList(activityCategoriesId);
			request.setAttribute("welfareItemList",welfareItemList);
			if(welfareItemList !=null && welfareItemList.size()>0){
				WelfareItem weItem = welfareItemList.get(0);
				Long weItemId = weItem.getObjectId();
				ExcitationActivity exActivity = new ExcitationActivity();
				exActivity.setActivityCategoriesId(weItemId);
				exActivity.setDeleted(0); //正常未被删除的数据
				exActivity.setCompanyId(user.getCompanyId());
				List<ExcitationActivity> exList = exActivityManager.getBySample(exActivity);
				request.setAttribute("exList", exList);
				request.setAttribute("weItem", weItem);
			}
			//3、当前账户余额
			request.setAttribute("accountBalance", userManager.getUserBalance(FrameworkContextUtils.getCurrentUserId()));
		}catch(Exception e){
			logger.error("grantReward",e);
		}
		return HOME_DIR+"/grantExcitationReward";
	}
	
	/**
	 * 根据选中的活动类目，获取关联的活动标题
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/queryTitle")
	public String queryTitle(ModelMap modelMap,HttpServletRequest request){
		try{
			User user = (User)request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			String welfareItemId = request.getParameter("welfareItemId");
			logger.info("根据选中的活动类目，获取关联的活动标题,welfareItemId=="+welfareItemId);
			if(StringUtils.isNotBlank(welfareItemId)){
				List<ExcitationActivity> exActivitieList = new ArrayList<ExcitationActivity>();
				if(welfareItemId!=null && !welfareItemId.equals("")){
					ExcitationActivity exActivity = new ExcitationActivity();
					exActivity.setCompanyId(user.getCompanyId());
					exActivity.setActivityCategoriesId(Long.valueOf(welfareItemId));
					exActivity.setDeleted(0);
					exActivitieList = exActivityManager.getBySample(exActivity);
				}
				modelMap.addAttribute("exActivitieList", exActivitieList);
				
			}
		}catch(Exception e){
			logger.error("queryTitle",e);
		}
		return "jsonView";
	}
	/**
	 * 发放奖励
	 * @param request
	 * @param modelMap
	 * @param pointDistribute
	 * @return
	 */
	@RequestMapping("saveDistribute")
	public String saveDistribute(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap, PointDistrubute pointDistribute){
		try{
			Long organizationId = FrameworkContextUtils.getCurrentUser().getOrganizationId();
			Long companyId = FrameworkContextUtils.getCurrentUser().getCompanyId();
			pointDistribute.setCompanyId(companyId);
			pointDistribute.setOrganizationId(organizationId);
			byte[] fileData = new byte[0];
			List<Long> distrubuteItems = new ArrayList<Long>();
			if(pointDistribute.getDistributeBy() == PointDistrubute.DISTRIBUTE_BY_BATCH_IMPORT){
				UploadFile uploadFile = FileUpDownUtils.getUploadFile(request);
				fileData = FileUpDownUtils.getFileContent(uploadFile.getFile());
				//批量发放为0
				pointDistribute.setPoint(0L);
			}else{
				if(request.getParameter("itemIds")!=null){
					String[] itemIds = request.getParameter("itemIds").split(",");
					if(itemIds!=null){
						for(String itemId : itemIds){
							if(StringUtils.isNotBlank(itemId)){
								distrubuteItems.add(Long.parseLong(itemId));
							}
						}
					}
				}
			}
			pointDistribute.setCreateDate(new Date());
			String distributeTime = request.getParameter("distributeTime");
			if(distributeTime != null && !"".equals(distributeTime)){
				pointDistribute.setDistributeDate(DateUtils.convertStringDate(distributeTime, DateUtils.FORMAT_YYYY_MM_DD));
			}
			if(pointDistribute.getDistributeDate()==null){
				pointDistribute.setDistributeDate(new Date());
			}
			pointDistribute.setUserId(FrameworkContextUtils.getCurrentUserId());
			pointDistribute.setDistributeType((short)IBSConstants.WELFAREITEM_TYPE_EXCITATION);
			pointDistribute.setTotalPoint(0.0);
			pointDistribute.setHeadCount(0L);
			
			String[] rewardObjectStr = request.getParameterValues("rewardObjectId");
			String[] rewardUserIdStr = request.getParameterValues("rewardUserId");
			if(rewardUserIdStr!=null&&!"".equals(rewardUserIdStr)){
				List list = Arrays.asList(rewardUserIdStr);
				Set set = new HashSet(list);
				String [] rid=(String [])set.toArray(new String[0]);
				if(rewardUserIdStr.length!=rid.length){
					logger.error("saveDistribute:激励发放人重复");
					modelMap.addAttribute("result", false);
					AjaxUtils.doAjaxResponseOfMap(response, modelMap);
					return null;
				}
			}
			String[] rewardStr = request.getParameterValues("reward");
			List<String> distrubuteUserIds = new ArrayList<String>();
			if(rewardObjectStr !=null && rewardObjectStr.length>0){
				for(int i = 0;i<rewardObjectStr.length;i++){
					String rewardObjectId = rewardObjectStr[i];
					String reward = rewardStr[i];
					logger.info("rewardObjectId=="+rewardObjectId);
					if(rewardUserIdStr !=null && rewardUserIdStr.length>0){
						if(rewardUserIdStr[i] != null && rewardUserIdStr[i].length()>0){
							String[] rewardUser = rewardUserIdStr[i].split(",");
							if(rewardUser !=null && rewardUser.length>0){
								for(int j=0;j<rewardUser.length;j++){
									String userId = rewardUser[j];
									logger.info("userId=="+userId);
									distrubuteUserIds.add(userId+","+rewardObjectId+","+reward);
								}
							}
						}
					}
				}
				String name=request.getParameter("titleName");
				pointDistribute.setName(name);
				pointDistribute.setPoint(0L);
				pointDistribute = pointDistrubuteManager.saveDistributeByReward(pointDistribute, distrubuteUserIds);
			}else{
				String rewardOne = request.getParameter("rewardOne");  //单项奖励设置活动标题名称
				if(rewardOne!=null && !"".equals(rewardOne)){
					String name=request.getParameter("titleName");
					pointDistribute.setName(name);
				}else{
					String name=request.getParameter("name");
					pointDistribute.setName(name);
				}
				pointDistribute = pointDistrubuteManager.saveDistribute(pointDistribute, distrubuteItems, fileData);
			}
			modelMap.addAttribute("itemNames", pointDistrubuteItemManager.getPointDistrubuteItemNames(pointDistribute));
			modelMap.addAttribute("accountBalance", userManager.getUserBalance(FrameworkContextUtils.getCurrentUserId()));
			modelMap.addAttribute("pointDistribute", pointDistribute);
			modelMap.addAttribute("result", true);
			AjaxUtils.doAjaxResponseOfMap(response, modelMap);
		}catch(Exception e){
			modelMap.addAttribute("result", false);
			try {
				AjaxUtils.doAjaxResponseOfMap(response, modelMap);
			} catch (Exception e1) {
				logger.error("saveDistribute",e1);
			}
			logger.error("saveDistribute",e);
		}
		return null;
	}
	
	@RequestMapping("confirm")
	public String confirm(ModelMap modelMap,Long distributeId, String password) throws Exception {
		int result = 0;
		if(distributeId!=null && StringUtils.isNotBlank(password)){
			//验证密码
			if(pointOperateManager.validPayPassword(FrameworkContextUtils.getCurrentUserId(), password)){
				//验证余额
				PointDistrubute pointDistrubute = pointDistrubuteManager.getByObjectId(distributeId);
				if(pointDistrubute!=null){
					if(pointOperateManager.validAccountBalance(FrameworkContextUtils.getCurrentUserId(), pointDistrubute.getTotalPoint().doubleValue())){
						pointDistrubuteManager.comfirmDistribute(distributeId);
						result = 1;
					}else{
						result = 2;
					}
				}
			}
		}
		modelMap.addAttribute("result", result);
		return "jsonView";
	}
	
	/**
	 * 根据选中的活动标题，获取对应的活动规则明细
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/queryRewardByTitleId")
	public String queryRewardByTitleId(ModelMap modelMap,HttpServletRequest request){
		try{
			String titleId = request.getParameter("titleId");
			logger.info("根据选中的活动标题，获取对应的活动规则明细,titleId=="+titleId);
			if(StringUtils.isNotBlank(titleId)){
				List<ActivityRewardInfo> acRewardInfoList = new ArrayList<ActivityRewardInfo>();
				if(titleId!=null && !titleId.equals("")){
					ActivityRewardInfo acRewardInfo = new ActivityRewardInfo();
					acRewardInfo.setTitleId(Long.valueOf(titleId));
					acRewardInfoList = acRewardInfoManager.getBySample(acRewardInfo);
				}
				modelMap.addAttribute("acRewardInfoList", acRewardInfoList);
				return "jsonView";
			}
		}catch(Exception e){
			logger.error("queryRewardByTitleId",e);
		}
		return null;
	}
}
