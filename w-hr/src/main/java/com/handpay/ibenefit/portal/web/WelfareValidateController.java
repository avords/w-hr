package com.handpay.ibenefit.portal.web;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.stereotype.Controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.product.entity.ProdSellArea;
import com.handpay.ibenefit.product.entity.SkuPublish;
import com.handpay.ibenefit.product.service.IProdSellAreaManager;
import com.handpay.ibenefit.product.service.ISkuPublishManager;
import com.handpay.ibenefit.welfare.entity.WelfarePackage;
import com.handpay.ibenefit.welfare.entity.WpAreaRelation;
import com.handpay.ibenefit.welfare.service.IWelfarePackageManager;
import com.handpay.ibenefit.welfare.service.IWpAreaRelationManager;


/**
 * 购买福利校验
 * @author zhliu
 * @date 2015年7月10日
 * @parm
 */
@Controller
@RequestMapping("welfareValidate")
public class WelfareValidateController {

	
	
	@Reference(version="1.0")
	private IWpAreaRelationManager wpAreaRelationManager;
	@Reference(version="1.0")
	private IProdSellAreaManager prodSellAreaManager;
	@Reference(version="1.0")
	private ISkuPublishManager skuPublishManager;
	@Reference(version="1.0")
	private IWelfarePackageManager welfarePackageManager;
	
	
	
	
	
	/**
	 * 商品销售区域判断
	 * @author zhliu
	 * @date 2015年6月30日
	 * @parm
	 * @param modelMap
	 * @return
	 */
	@RequestMapping("validateProdAddr")
	public String validateAddr(HttpServletRequest request,ModelMap modelMap){
		
		List<ProdSellArea> prodSellAreas = new ArrayList<ProdSellArea>();
		String areaCode = request.getParameter("cityId");
		String productId = request.getParameter("productId");
		String inSalesRange = "1";//0:在销售范围内；1：不在销售范围内
		try {
			String []productIds = productId.split(",");
			if(productIds.length>0){
				productId = productIds[0];
			}
			
			if(StringUtils.isNotBlank(productId) && StringUtils.isNotBlank(areaCode)){
				ProdSellArea prodSellArea = new ProdSellArea();
				prodSellArea.setProductId(Long.valueOf(productId));
				prodSellAreas = prodSellAreaManager.getBySample(prodSellArea);
				
				//若商品没有选择销售区域，默认为全国
				if(prodSellAreas!=null && prodSellAreas.size()<1){
					inSalesRange = "0";
					modelMap.addAttribute("inSalesRange",inSalesRange);
					return "jsonView";
				}else{
					prodSellArea.setAreaCode(areaCode);
					prodSellAreas = prodSellAreaManager.getBySample(prodSellArea);
					if(prodSellAreas!=null && prodSellAreas.size()>0){
						inSalesRange = "0";
						modelMap.addAttribute("inSalesRange",inSalesRange);
						return "jsonView";
					}
				}
				
				
			}
			modelMap.addAttribute("inSalesRange",inSalesRange);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "jsonView";
	}
	
	
	
	
	
	/**
	 * 套餐销售区域判断
	 * @author zhliu
	 * @date 2015年6月30日
	 * @parm
	 * @param modelMap
	 * @return
	 */
	@RequestMapping("validateWelfareAddr")
	public String validateWelfareAddr(HttpServletRequest request,ModelMap modelMap){
		
		List<WpAreaRelation> wpAreaRelations = new ArrayList<WpAreaRelation>();
		String areaCode = request.getParameter("cityId");
		String packageId = request.getParameter("packageId");
		String inSalesRange = "1";//0:在销售范围内；1：不在销售范围内
		
		try {
			
			if(packageId!=null && !packageId.trim().equals("") && areaCode!=null&& !areaCode.trim().equals("") ){
				WpAreaRelation wpAreaRelation = new WpAreaRelation();
				wpAreaRelation.setPackageId(Long.valueOf(packageId));
				wpAreaRelations = wpAreaRelationManager.getBySample(wpAreaRelation);
				
				if(wpAreaRelations!=null && wpAreaRelations.size()<1){
					inSalesRange = "0";
					modelMap.addAttribute("inSalesRange",inSalesRange);
					return "jsonView";
				}else{
					wpAreaRelation.setAreaId(Long.valueOf(areaCode));
					wpAreaRelations = wpAreaRelationManager.getBySample(wpAreaRelation);
					if(wpAreaRelations!=null && wpAreaRelations.size()>0){
						inSalesRange = "0";
						modelMap.addAttribute("inSalesRange",inSalesRange);
						return "jsonView";
					}
				}
				
				
				
				
			}
			modelMap.addAttribute("inSalesRange",inSalesRange);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "jsonView";
	}
	
	
	
	
	
	
	
	
	
	/**
	 * 校验福利套餐 库存
	 * @author zhliu
	 * @date 2015年7月10日
	 * @parm
	 * @param request
	 * @param modelMap
	 * @return
	 */
	@RequestMapping("validateWelfareStock")
	public String validateWelfareStock(HttpServletRequest request,ModelMap modelMap){
		
		String welfarePackageId = request.getParameter("welfarePackageId");
		WelfarePackage welfarePackage = new WelfarePackage();
		if(StringUtils.isNotBlank(welfarePackageId)){
			welfarePackage = welfarePackageManager.getByObjectId(Long.valueOf(welfarePackageId));
		}
		modelMap.addAttribute("welfarePackage", welfarePackage);
		return "jsonView";
	}
	
	
	
	
	
	
	/**
	 * 校验福利商品 库存
	 * @author zhliu
	 * @date 2015年7月10日
	 * @parm
	 * @param request
	 * @param modelMap
	 * @return
	 */
	@RequestMapping("validateProdStock")
	public String validateProdStock(HttpServletRequest request,ModelMap modelMap){
		
		SkuPublish skuPublish = new SkuPublish();
		
		String skuId = request.getParameter("skuId");
		if(StringUtils.isNotBlank(skuId)){
			skuPublish = skuPublishManager.getByObjectId(Long.valueOf(skuId));
		}
		
		modelMap.addAttribute("skuPublish", skuPublish);
		
		
		
		return "jsonView";
	}
	
	
	
	
	
	
	
	
	
	
}
