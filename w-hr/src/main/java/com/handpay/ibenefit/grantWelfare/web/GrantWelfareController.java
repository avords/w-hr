package com.handpay.ibenefit.grantWelfare.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.google.gson.Gson;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.UserUtils;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.service.Manager;
import com.handpay.ibenefit.framework.util.DateUtils;
import com.handpay.ibenefit.framework.util.ExcelUtil;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageSearchInit;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.framework.util.UploadFile;
import com.handpay.ibenefit.framework.web.PageController;
import com.handpay.ibenefit.grantWelfare.service.IGrantWelfareItemManager;
import com.handpay.ibenefit.grantWelfare.service.IGrantWelfareManager;
import com.handpay.ibenefit.order.entity.SubOrder;
import com.handpay.ibenefit.order.service.ISubOrderManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IPointOperateManager;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.entity.WelfarePackage;
import com.handpay.ibenefit.welfare.entity.WelfarePackageCategory;
import com.handpay.ibenefit.welfare.service.IWelfareManager;
import com.handpay.ibenefit.welfare.service.IWelfarePackageCategoryManager;
import com.handpay.ibenefit.welfare.service.IWelfarePackageManager;

/** 
 * @desc  发放福利券
 * @author ylan  
 * @date 创建时间：2015年6月23日
 */
@Controller
@RequestMapping("/grantWelfare")
public class GrantWelfareController extends PageController<SubOrder>{

	private static final String BASE_DIR = "grantWelfare/";
	
	Logger logger = Logger.getLogger(GrantWelfareController.class);
	
	@Reference(version = "1.0")
	private IGrantWelfareManager grantWelfareManager;
	@Reference(version = "1.0")
	private IGrantWelfareItemManager grantWelfareItemManager;
	@Reference(version="1.0")
	private IWelfareManager welfareManager;
	@Reference(version = "1.0")
	private IDictionaryManager dictionaryManager;
	@Reference(version="1.0")
	private IUserManager userManager;
	@Reference(version="1.0")
	private ISubOrderManager subOrderManager; 
	@Reference(version="1.0")
	private IPointOperateManager pointOperateManager;
	@Reference(version="1.0")
	private IWelfarePackageManager welfarePackageManager;
	@Reference(version="1.0")
	private IWelfarePackageCategoryManager welfarePackageCategoryManager;
	
	/**
	 * 福利管理-发放福利券
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/queryWelfareList")
	public String queryWelfareList(HttpServletRequest request, Integer backPage){
		PageSearch page  = preparePage(request);
		PageSearchInit.initcurrentPage(page, request);
		
		try{
			//当前登录人，订单商品类型为福利套餐1，体检套餐2，状态为待发放列表
			User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
			if(!UserUtils.isCompanyAdmin()){//非企业管理员
				PropertyFilter userIdFilter = new PropertyFilter();
				userIdFilter.setPropertyName("userId");
				userIdFilter.setPropertyValue(user.getObjectId());
				page.getFilters().add(userIdFilter);
			}else{//企业管理员
				PropertyFilter companyIdFilter = new PropertyFilter();
				companyIdFilter.setPropertyName("companyId");
				companyIdFilter.setPropertyValue(user.getCompanyId());
				page.getFilters().add(companyIdFilter);
				
				
				String hresStr = String.valueOf(user.getObjectId());//企业管理员 下所有的HR（多个用,分割）
				User sample = new User();
				sample.setCompanyId(user.getCompanyId());
				sample.setType(IBSConstants.USER_TYPE_COMPANY_HR);
				List<User> hres = userManager.getBySample(sample);
				
				for (User userTemp : hres) {
					hresStr+=","+String.valueOf(userTemp.getObjectId());
				}
				
				logger.info(hresStr);
				page.getFilters().add(new PropertyFilter("SubOrder", "INS_userId", hresStr));
				
				
				
			}
			
			//子订单删除标识，未删除
			PropertyFilter deleteFilter = new PropertyFilter();
			deleteFilter.setPropertyName("deleted");
			deleteFilter.setPropertyValue(IBSConstants.NOT_DELETE);
			page.getFilters().add(deleteFilter);
			
			//子订单状态 : 待发放
			PropertyFilter subOrderStateFilter = new PropertyFilter();
			subOrderStateFilter.setPropertyName("subOrderState");
			subOrderStateFilter.setPropertyValue(IBSConstants.ORDER_TO_BE_ISSUED_VIRTUAL);
			page.getFilters().add(subOrderStateFilter);
		
			List<String> orderProdTypeList = new ArrayList<String>() ;
			orderProdTypeList.add(String.valueOf(IBSConstants.ORDER_PRODUCT_TYPE_WELFARE)) ;//福利套餐1
			orderProdTypeList.add(String.valueOf(IBSConstants.ORDER_PRODUCT_TYPE_PHYSICAL)) ;//体检套餐2
			//订单商品类型：福利套餐1，体检套餐2
			PropertyFilter orderProdTypeFilter = new PropertyFilter();
			orderProdTypeFilter.setPropertyName("orderProdTypeList");
			orderProdTypeFilter.setPropertyValue(orderProdTypeList);
			page.getFilters().add(orderProdTypeFilter);
			
			//子订单类型：虚拟
			PropertyFilter subOrderTypeFilter = new PropertyFilter();
			subOrderTypeFilter.setPropertyName("subOrderType");
			subOrderTypeFilter.setPropertyValue(IBSConstants.SUB_ORDER_TYPE_ELECTRON);//虚拟2
			page.getFilters().add(subOrderTypeFilter);
			
			page.setSortProperty("bookingDate");
			page.setSortOrder("desc");
			
			page =  subOrderManager.findWelfareList(page);			
			request.setAttribute("pageActivity", page);
		}catch(Exception e){
			logger.info("加载福利券列表出错！");
			e.printStackTrace();
		} 
		return BASE_DIR+"/listGrantWelfare";
	}
	
	
	/**
	 * 跳转新增发放福利券界面
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/editWelfare/{objectId}")
	public String editWelfare(HttpServletRequest request,HttpServletResponse response,@PathVariable Long objectId,SubOrder subOrder){
		//查询发放名目 
		WelfareItem welfareItem = new WelfareItem();
		welfareItem.setItemType(IBSConstants.WELFAREITEM_TYPE_WELFARE);
		welfareItem.setItemGrade(IBSConstants.WELFAREITEM_GRADE_CLASSIFY);
		welfareItem.setStatus(IBSConstants.EFFECTIVE);//有效
		List<WelfareItem> welfareItems = welfareManager.getBySample(welfareItem);
		if(welfareItems!=null && welfareItems.size()>0){
			WelfareItem temp = (WelfareItem)welfareItems.get(0) ;
			request.setAttribute("welfareItems", welfareItems);
			request.setAttribute("temp", temp);
		}
		
		//根据福利订单ID查询套餐详情
		subOrder.setObjectId(objectId);
		SubOrder subOrderTemp = grantWelfareManager.viewWelfareInfo(subOrder) ;
		request.setAttribute("subOrderTemp", subOrderTemp);
		
		//调用公用方法
		getCommonViewInfo(subOrderTemp, request) ;
		
		return BASE_DIR+"/editWelfare";
	}
	
	
	/**
	 * 发放福利券-查看详情界面
	 * @param request
	 * @param response
	 * @param objectId
	 * @return
	 */
	@RequestMapping("/viewWelfareInfo/{objectId}")
	public String viewWelfareInfo(HttpServletRequest request,HttpServletResponse response,SubOrder subOrder, @PathVariable Long objectId){
		subOrder.setObjectId(objectId);
		subOrder = grantWelfareManager.viewWelfareInfo(subOrder) ;
		request.setAttribute("subOrderTemp", subOrder);
		String towardsPage = "" ;
		if(subOrder!=null && subOrder.getOrderProdType()!=null){
			if(subOrder.getOrderProdType()==IBSConstants.ORDER_PRODUCT_TYPE_WELFARE){//福利套餐1 
				
				//调用公用方法
				getCommonViewInfo(subOrder, request) ;
				
				//跳转
				towardsPage = BASE_DIR+"/viewWelfareInfo" ;			
			}else if(subOrder.getOrderProdType()==IBSConstants.ORDER_PRODUCT_TYPE_PHYSICAL){//体检套餐2
				//调用公用方法
				getCommonViewInfo(subOrder, request) ;
				
				//跳转
				towardsPage = BASE_DIR+"/viewPhysicalInfo" ;
			}
		}
		return towardsPage ;
	}
	

	/**
	 * 福利订单-查看详情界面
	 * @param request
	 * @param response
	 * @param objectId
	 * @return
	 */
	@RequestMapping("/viewOrderInfo/{objectId}")
	public String viewOrderInfo(HttpServletRequest request,HttpServletResponse response,SubOrder subOrder, @PathVariable Long objectId){
		subOrder.setObjectId(objectId);
		subOrder = grantWelfareManager.viewWelfareInfo(subOrder) ;
		request.setAttribute("subOrderTemp", subOrder);
		String towardsPage = "" ;
		if(subOrder!=null && subOrder.getOrderProdType()!=null){
			if(subOrder.getOrderProdType()==IBSConstants.ORDER_PRODUCT_TYPE_WELFARE){//福利套餐1 
				
				//调用公用方法
				getCommonViewInfo(subOrder, request) ;
				
				//跳转
				towardsPage = BASE_DIR+"/viewOrderWelfare" ;
				
			}else if(subOrder.getOrderProdType()==IBSConstants.ORDER_PRODUCT_TYPE_SKU){//SKU商品
				
				towardsPage = BASE_DIR+"/viewOrderSkuInfo" ;
				
			}else if(subOrder.getOrderProdType()==IBSConstants.ORDER_PRODUCT_TYPE_PHYSICAL){//体检套餐2
				//调用公用方法
				getCommonViewInfo(subOrder, request) ;
				
				//跳转
				towardsPage = BASE_DIR+"/viewOrderPhysical" ;

			}
		}
		return towardsPage ;
	}
	
	
	
	/**
	 * 公用方法
	 * @param subOrder
	 * @param request
	 */
	public void getCommonViewInfo(SubOrder subOrder, HttpServletRequest request){
		//计算子订单卡密生成器的总份数
		Long totalCount = grantWelfareManager.getSubOrderTotalCount(subOrder.getObjectId(),null) ;

		Long expiretotalCount = grantWelfareManager.getSubOrderTotalCount(subOrder.getObjectId(),IBSConstants.CARD_VOID) ;

		//计算已发放份数
		Long grantedCount = grantWelfareManager.getSubOrderByGranted(subOrder.getObjectId()) ;

		//计算未发放份数
		Long notGrantCount =  totalCount - grantedCount - expiretotalCount ;
		
		//根据子订单查询订单发放类型
		SubOrder temp = subOrderManager.getByObjectId(subOrder.getObjectId()) ;
		if(temp.getOrderGrantType()!=null && temp.getOrderGrantType() == IBSConstants.ORDER_GRANT_OFFLINE){//发放类型不为空，且为线下发放
			//计算已发放份数
			grantedCount = totalCount ;
			//计算未发放份数
			notGrantCount = 0l ;
		}

		request.setAttribute("expiretotalCount", expiretotalCount);
		request.setAttribute("notGrantCount", notGrantCount);
		request.setAttribute("grantedCount", grantedCount);
		
		//查询套餐详情
		WelfarePackage welfarePackage = welfarePackageManager.getByObjectId(Long.valueOf(subOrder.getPackageId()));
		request.setAttribute("welfarePackage", welfarePackage);
		
		//查询套餐几选几
		WelfarePackageCategory category =  welfarePackageCategoryManager.getByObjectId(welfarePackage.getWpCategoryId());
		request.setAttribute("category", category);
	}
	
	
	
	/**
	 * 发放福利券-线下发放导出卡号，卡密
	 * @param request
	 * @param response
	 * @param objectId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("exportCardInfo")
	public String exportCardInfo(HttpServletRequest request, HttpServletResponse response,Long subObjectId, String password) throws Exception {
		if(StringUtils.isNotBlank(password)){
			//验证支付密码
			if(pointOperateManager.validPayPassword(FrameworkContextUtils.getCurrentUserId(), password)){
				String[] titles={"序号","卡号","密码","状态"};
				//查询卡号密码状态根据字典
				List<Dictionary> dictionaries = dictionaryManager.getDictionariesByDictionaryId(1604);
				
				Long userId = 0l;
				if(request.getSession().getAttribute(SecurityConstants.USER_ID)!=null){
					userId = (Long)request.getSession().getAttribute(SecurityConstants.USER_ID);
				}
				
				Long companyId = 0l ;
				if(request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID)!=null){
					companyId = (Long)request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
				}
				
				//查询导出excel
				List<Object[]> datas = grantWelfareManager.getCardInfoBySubOrderOffline(dictionaries, subObjectId, userId, companyId) ;
				
				String exportName = "cardInfo.xls";
				ExcelUtil excelUtil = new ExcelUtil();
				excelUtil.exportExcel(response, datas, titles, exportName);
				
			}
		}

	
		return  null;
	}
	
	
	/**
	 * 导出已经发放的卡密信息
	 * @param request
	 * @param response
	 * @param subObjectId
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("exportGrantedCardInfo/{subObjectId}")
	public String exportGrantedCardInfo(HttpServletRequest request, HttpServletResponse response,@PathVariable Long subObjectId)  throws Exception {
		//查询导出excel
		List<Object[]> datas = grantWelfareManager.exportGrantedCardInfo(subObjectId) ;
		String exportName = "grantList.xls";
		String[] titles={"序号","部门","工号","姓名","发放时间"};
		ExcelUtil excelUtil = new ExcelUtil();
		excelUtil.exportExcel(response, datas, titles, exportName);
		
		return  null;
	}
	
	
	
	
	/**
	 * 保存   发放福利券
	 * @param request
	 * @param modelMap
	 * @param pointDistribute
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("saveDistribute")
	public ResponseEntity<String> saveDistribute(HttpServletRequest request,ModelMap modelMap, GrantWelfare grantWelfare, String realnameFlag, String sendFlag) throws Exception {
		Long organizationId = FrameworkContextUtils.getCurrentUser().getOrganizationId();
		Long companyId = FrameworkContextUtils.getCurrentUser().getCompanyId();
		grantWelfare.setCompanyId(companyId);

		grantWelfare.setOrganizationId(organizationId);
		byte[] fileData = new byte[0];
		List<Long> distrubuteItems = new ArrayList<Long>();
		if(grantWelfare.getDistributeBy() == GrantWelfare.DISTRIBUTE_BY_BATCH_IMPORT){
			UploadFile uploadFile = FileUpDownUtils.getUploadFile(request);
			fileData = FileUpDownUtils.getFileContent(uploadFile.getFile());
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
		grantWelfare.setCreateDate(new Date());
		grantWelfare.setUserId(FrameworkContextUtils.getCurrentUserId());
		grantWelfare.setHeadCount(0l);
		grantWelfare.setGrantCount(0l);
		
		if(grantWelfare.getGrantFlag()==0){//不立即发放
			String grantDateStr = grantWelfare.getGrantDateStr() ;//界面选择定时发放日期
			grantWelfare.setGrantDate(DateUtils.convertStringDate(grantDateStr, DateUtils.FORMAT_YYYY_MM_DD));
		}else{//立即发放，发放日期为系统日期
			String grantDateNow = DateUtils.getCurrentDate() ;
			grantWelfare.setGrantDate(DateUtils.convertStringDate(grantDateNow, DateUtils.FORMAT_YYYY_MM_DD));
		}
		
		grantWelfare.setRealnameFlag(realnameFlag!=null ? Integer.parseInt(realnameFlag) : 0);//是否使用实名
		if(grantWelfare.getOrderProdType()==2){//体检套餐
			grantWelfare.setSendFlag(sendFlag!=null ? Integer.parseInt(sendFlag) : 0);//是否统一寄送公司
		}

		//HR计算未发放数量
		
		
		Long grantedCount = grantWelfareManager.getSubOrderByGranted(grantWelfare.getSubOrderId()) ;//计算已发放份数	
		Long totalCount = grantWelfareManager.getSubOrderTotalCount(grantWelfare.getSubOrderId(),null) ;//计算子订单卡密生成器的总份数
		Long expiretotalCount = grantWelfareManager.getSubOrderTotalCount(grantWelfare.getSubOrderId(),IBSConstants.CARD_VOID) ;
		Long notGrantCount =  totalCount - grantedCount -expiretotalCount;
		SubOrder temp = subOrderManager.getByObjectId(grantWelfare.getSubOrderId()) ;
		if(temp.getOrderGrantType()!=null && temp.getOrderGrantType() == IBSConstants.ORDER_GRANT_OFFLINE){
			//发放类型不为空，且为线下发放
			notGrantCount = 0l ;
		}
		request.setAttribute("expiretotalCount", expiretotalCount);
		grantWelfare.setNotGrantCount(notGrantCount);
		
		//保存 发放主表，发放员工表，发放对象表
		grantWelfare = grantWelfareManager.saveDistribute(grantWelfare, distrubuteItems, fileData);

		//HR计算本次需发放数量
		Long grantCount = grantWelfareManager.getCountByGranted(grantWelfare.getObjectId()) ;
		grantWelfare.setGrantCount(grantCount);

		Gson g = new Gson();
		Map<String,Object> map = new HashMap<String,Object>();
		HttpHeaders headers = new HttpHeaders();  
	    headers.setContentType(MediaType.TEXT_PLAIN);  
		map.put("itemNames", grantWelfareItemManager.getPointDistrubuteItemNames(grantWelfare));
		map.put("accountBalance",FrameworkContextUtils.getCurrentUser().getAccountBalance());	
		map.put("pointDistribute",grantWelfare);	
		return new ResponseEntity<String>(g.toJson(map), headers, HttpStatus.OK);
	}
	
	
	/**
	 * 在线支付 确认支付密码
	 * @param modelMap
	 * @param distributeId
	 * @param password
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("confirmPayOnline")
	public String confirmPayOnline(ModelMap modelMap,Long distributeId, String password) throws Exception {
		int result = 0;
		if(distributeId!=null && StringUtils.isNotBlank(password)){
			//验证密码
			if(pointOperateManager.validPayPassword(FrameworkContextUtils.getCurrentUserId(), password)){
				//验证剩余份数
				grantWelfareManager.comfirmGrantWelfare(distributeId);
				result = 1;
			}
		}
		modelMap.addAttribute("result", result);
		return "jsonView";
	}
	
	
	/**
	 * 线下支付  确认支付密码
	 * @param modelMap
	 * @param distributeId
	 * @param password
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("confirmPayOffline")
	public String confirmPayOffline(ModelMap modelMap, Long subOrderId, String password) throws Exception {
		int result = 0;
		if(StringUtils.isNotBlank(password)){
			//验证密码
			if(pointOperateManager.validPayPassword(FrameworkContextUtils.getCurrentUserId(), password)){
				result = 1;
			}
		}
		modelMap.addAttribute("result", result);
		return "jsonView";
	}

	
	/**
	 * 校验当前订单的交易状态；sub_order_state=4表示待发放，sub_order_state=5表示已发放完成，则刷新当前界面
	 * @param modelMap
	 * @param subOrderId
	 * @return
	 */
	@RequestMapping("checkSubOrderState")
	public String checkSubOrderState(ModelMap modelMap, Long subOrderId){
		SubOrder subOrder = subOrderManager.getByObjectId(subOrderId) ;
		if(subOrder!=null){
			modelMap.addAttribute("subOrderState", subOrder.getSubOrderState());//订单交易状态
			modelMap.addAttribute("grantType", subOrder.getOrderGrantType());//发放类型
		}
		return "jsonView";
	}
	
	

	@Override
	public Manager<SubOrder> getEntityManager() {
		return subOrderManager;
	}


	@Override
	public String getFileBasePath() {
		return BASE_DIR;
	}


}
