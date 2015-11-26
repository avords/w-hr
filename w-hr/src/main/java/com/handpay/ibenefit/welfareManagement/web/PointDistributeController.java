package com.handpay.ibenefit.welfareManagement.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.UploadFile;
import com.handpay.ibenefit.point.entity.PointDistrubute;
import com.handpay.ibenefit.point.service.IPointDistrubuteItemManager;
import com.handpay.ibenefit.point.service.IPointDistrubuteManager;
import com.handpay.ibenefit.security.service.IPointOperateManager;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.service.IWelfareManager;

@Controller
@RequestMapping("pointDistribute")
public class PointDistributeController extends BaseController {
	
	@Reference(version="1.0")
	private IPointDistrubuteManager pointDistrubuteManager;
	
	@Reference(version = "1.0")
	private IPointDistrubuteItemManager pointDistrubuteItemManager;
	
	@Reference(version="1.0")
	private IWelfareManager welfareManager;
	
	@Reference(version="1.0")
	private IUserManager userManager;
	
	@Reference(version="1.0")
	private IPointOperateManager pointOperateManager;
	
	@RequestMapping("")
	public String index(HttpServletRequest request, HttpServletResponse response) throws Exception {
		WelfareItem welfareItem = new WelfareItem();
		welfareItem.setItemType(IBSConstants.WELFAREITEM_TYPE_WELFARE);
		welfareItem.setItemGrade(IBSConstants.WELFAREITEM_GRADE_CLASSIFY);
		welfareItem.setStatus(IBSConstants.EFFECTIVE);//有效
		request.setAttribute("items", welfareManager.getBySample(welfareItem));
		request.setAttribute("accountBalance", userManager.getUserBalance(FrameworkContextUtils.getCurrentUserId()));
		return "welfareManagement/grantPoints";
	}
	
	
	@RequestMapping("saveDistribute")
	public String saveDistribute(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap, PointDistrubute pointDistribute) throws Exception {
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
		if(pointDistribute.getDistributeDate()==null){
			pointDistribute.setDistributeDate(new Date());
		}
		pointDistribute.setUserId(FrameworkContextUtils.getCurrentUserId());
		pointDistribute.setDistributeType((short)IBSConstants.WELFAREITEM_TYPE_WELFARE);
		pointDistribute.setTotalPoint(0.0);
		pointDistribute.setHeadCount(0L);
		pointDistribute.setItemType(IBSConstants.ACTIVITY_ITEM_TYPE_RECOMMEND);
		pointDistribute = pointDistrubuteManager.saveDistribute(pointDistribute, distrubuteItems, fileData);
		modelMap.addAttribute("itemNames", pointDistrubuteItemManager.getPointDistrubuteItemNames(pointDistribute));
		modelMap.addAttribute("accountBalance", userManager.getUserBalance(FrameworkContextUtils.getCurrentUserId()));
		modelMap.addAttribute("pointDistribute", pointDistribute);
		AjaxUtils.doAjaxResponseOfMap(response, modelMap);
		return null;
	}
	
	@RequestMapping("toConfirm")
	public String toConfirm(ModelMap modelMap,Long distributeId) throws Exception {
		PointDistrubute pointDistribute = pointDistrubuteManager.getByObjectId(distributeId);
		modelMap.addAttribute("itemNames", pointDistrubuteItemManager.getPointDistrubuteItemNames(pointDistribute));
		modelMap.addAttribute("accountBalance", userManager.getUserBalance(FrameworkContextUtils.getCurrentUserId()));
		modelMap.addAttribute("pointDistribute", pointDistribute);
		return "jsonView";
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
	
	@RequestMapping("cancel")
	public String cancel(HttpServletRequest request, ModelMap modelMap,Long distributeId) throws Exception {
		boolean result = false;
		if(distributeId!=null){
			PointDistrubute pointDistrubute = pointDistrubuteManager.getByObjectId(distributeId);
			if(pointDistrubute!=null && pointDistrubute.getStatus() < 3){
				result = pointDistrubuteManager.cancelDistribute(distributeId);
			}
		}
		modelMap.addAttribute("result", result);
		return "jsonView";
	}

	
}
