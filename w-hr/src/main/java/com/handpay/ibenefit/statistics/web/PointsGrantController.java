package com.handpay.ibenefit.statistics.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.excitationActivity.entity.ActivityItem;
import com.handpay.ibenefit.excitationActivity.service.IActivityItemManager;
import com.handpay.ibenefit.excitationActivity.service.IExcitationActivityManager;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.service.Manager;
import com.handpay.ibenefit.framework.util.ExcelUtil;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageSearchInit;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.framework.web.PageController;
import com.handpay.ibenefit.point.entity.PointDistrubute;
import com.handpay.ibenefit.point.entity.PointDistrubuteStaff;
import com.handpay.ibenefit.point.service.IPointDistrubuteManager;
import com.handpay.ibenefit.point.service.IPointDistrubuteStaffManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.service.IWelfareManager;

/**
 * 积分发放记录
 * @author zhliu
 * @date 2015年6月15日
 * @parm
 */
@Controller
@RequestMapping("/pointsGrant")
public class PointsGrantController extends PageController<PointDistrubute> {


	private static final String HOME_DIR = "pointsGrant";
	Logger logger = Logger.getLogger(PointsGrantController.class);
	
	@Reference(version="1.0")
	private IPointDistrubuteManager pointDistrubuteManager;
	@Reference(version="1.0")
	private IWelfareManager welfareManager;
	@Reference(version="1.0")
	private IPointDistrubuteStaffManager pointDistrubuteStaffManager;
	@Reference(version="1.0")
	private IDictionaryManager dictionaryManager;
	@Reference(version="1.0")
	private IActivityItemManager activityItemManager;
	@Reference(version = "1.0")
    private IExcitationActivityManager exActivityManager;
	
	/**
	 * 积分发放记录
	 * @author zhliu
	 * @date 2015年6月15日
	 * @parm
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value="")
	public String index(HttpServletRequest request){
		try {
			User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			PageSearch page  = preparePage(request);
			PageSearchInit.initcurrentPage(page, request);
			
			/**  查询发放名目 */
			WelfareItem welfareItem = new WelfareItem();
			welfareItem.setItemType(IBSConstants.WELFAREITEM_TYPE_WELFARE);
			welfareItem.setItemGrade(IBSConstants.WELFAREITEM_GRADE_CLASSIFY);
			List<WelfareItem> welfareItems = welfareManager.getBySample(welfareItem);
			List<ActivityItem> acItemList = exActivityManager.queryActivityItemList(user.getCompanyId(),null);
			if(!UserUtils.isCompanyAdmin()){//非企业管理员
				page.getFilters().add(new PropertyFilter("PointDistrubute", "EQL_userId", String.valueOf(user.getObjectId())));
			}
			page.getFilters().add(new PropertyFilter("PointDistrubute", "EQL_companyId", String.valueOf(user.getCompanyId())));
			page.setSortProperty("createDate");
			page.setSortOrder("desc");
			
			page = pointDistrubuteManager.queryPointDistRelItem(page);
			//查询发放对象字典信息
			List<Dictionary> dictionaryList = dictionaryManager.getDictionariesByDictionaryId(IBSConstants.POINTS_GRANT_OBJECT_CONSTANT);
			
			//查询发放对象字典信息
			List<Dictionary> grantPointsStatusList = dictionaryManager.getDictionariesByDictionaryId(IBSConstants.GRANT_POINTS_STATUS);
			request.setAttribute("dictionaryList", dictionaryList);
			request.setAttribute("grantPointsStatusList", grantPointsStatusList);
			request.setAttribute("pageActivity", page);
			request.setAttribute("welfareItems", welfareItems);
			request.setAttribute("acItemList", acItemList);
			Date now = new Date();
			for(PointDistrubute distrubute : (List<PointDistrubute>)page.getList()){
				//借用字段
				distrubute.setUserId((long)distrubute.getDistributeDate().compareTo(now));
			}
		} catch (Exception e) {
			logger.error("index",e);
		}
		return HOME_DIR+"/listpointsGrant";
	}
	
	
	/**
	 * 查看发放详细
	 * @author zhliu
	 * @date 2015年6月16日
	 * @parm
	 * @param request
	 * @param distributeId :发放类目ID
	 * @return
	 */
	@RequestMapping("pointsGrantDetail")
	public String pointsGrantDetail(HttpServletRequest request,Long distributeId,String staffName){
		try {
			PageSearch page  = preparePage(request);
			PageSearchInit.initcurrentPage(page, request);
			PointDistrubute pointDistrubute =  pointDistrubuteManager.getByObjectId(distributeId);
			String itemName = "";//发放名目
			if(pointDistrubute!=null && pointDistrubute.getDistributeType() == IBSConstants.WELFAREITEM_TYPE_EXCITATION){
				if(pointDistrubute.getItemType() == IBSConstants.ACTIVITY_ITEM_TYPE_RECOMMEND){//运营端推荐名目类的
					itemName = welfareManager.getByObjectId(pointDistrubute.getWelfareItemId()).getItemName();
				}else{
					itemName = activityItemManager.getByObjectId(pointDistrubute.getWelfareItemId()).getActivityName();
				}
			}else{
				itemName = welfareManager.getByObjectId(pointDistrubute.getWelfareItemId()).getItemName();
			}
			page.getFilters().add(new PropertyFilter("PointDistrubute", "EQL_distributeId", String.valueOf(distributeId)));
			if(!org.apache.commons.lang3.StringUtils.isEmpty(staffName)){
				page.getFilters().add(new PropertyFilter("PointDistrubute", "EQS_staffName", String.valueOf(staffName)));
			}
			page = pointDistrubuteStaffManager.selectPointStaff(page);
			//查询性别字典信息
			List<Dictionary> dictionaryList = dictionaryManager.getDictionariesByDictionaryId(IBSConstants.SEX_CONSTANT);
			request.setAttribute("dictionaryList", dictionaryList);
			request.setAttribute("staffName", staffName);
			request.setAttribute("pointDistrubute", pointDistrubute);
			request.setAttribute("pageActivity", page);
			request.setAttribute("distributeId", distributeId);
			request.setAttribute("itemName", itemName);
		} catch (Exception e) {
			logger.error("pointsGrantDetail",e);
		}
		return HOME_DIR+"/detailpointsGrant";
	}
	
	
	
	
	
	/**
	 * 导出积分发放详细
	 * @author zhliu
	 * @date 2015年7月8日
	 * @parm
	 * @return
	 */
	@RequestMapping("exportPointsGrantDetail")
	public String exportPointsGrantDetail(HttpServletResponse response,HttpServletRequest request,Long distributeId,String staffName){
		List<Object[]> datas = new ArrayList<Object[]>();
		String[] titles={"工号","姓名","性别","手机号码","部门","邮箱","状态"};
		String excelName = "pointsGrantDetail.xls";
		try {
			//查询性别字典信息
			List<Dictionary> dictionaryList = dictionaryManager.getDictionariesByDictionaryId(IBSConstants.SEX_CONSTANT);
			PageSearch page  = preparePage(request);
			PageSearchInit.initcurrentPage(page, request);
			page.setPageSize(Integer.MAX_VALUE);
			
			page.getFilters().add(new PropertyFilter("PointDistrubute", "EQL_distributeId", String.valueOf(distributeId)));
			if(!org.apache.commons.lang3.StringUtils.isEmpty(staffName)){
				page.getFilters().add(new PropertyFilter("PointDistrubute", "EQS_staffName", String.valueOf(staffName)));
			}
			List<PointDistrubuteStaff> PointDistrubuteStaffList = pointDistrubuteStaffManager.selectPointStaffList(page);
			
			for (PointDistrubuteStaff pointDis:PointDistrubuteStaffList) {
				String sex = "";//性别
				String status = "";//状态
				for (Dictionary dict:dictionaryList) {
					if(pointDis.getSex()!=null&&pointDis.getSex().equals(dict.getValue())){
						sex = dict.getName();
						break;
					}
				}
				if(pointDis.getStatus() == 0){
					status = "未领取";
				}else if(pointDis.getStatus()==1){
					status = "已领取";
				}else{
					status = "待确认";
				}
				
				Object[] arr = new Object[titles.length];
				arr[0] = pointDis.getWorkNo();
				arr[1] = pointDis.getStaffName();
				arr[2] = sex;
				arr[3] = pointDis.getTelephone();
				arr[4] = pointDis.getDeptName();
				arr[5] = pointDis.getEmail();
				arr[6] = status;
				datas.add(arr);
			}
			ExcelUtil excelUtil = new ExcelUtil();
			excelUtil.exportExcel(response, datas, titles, excelName);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Manager<PointDistrubute> getEntityManager() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getFileBasePath() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
