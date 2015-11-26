/**
 *
 */
package com.handpay.ibenefit.mall.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.ProductConstants;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.physical.entity.PhysicalItem;
import com.handpay.ibenefit.physical.service.IPhysicalItemManager;
import com.handpay.ibenefit.product.entity.ProductPublish;
import com.handpay.ibenefit.product.entity.SkuPublish;
import com.handpay.ibenefit.product.service.IProductManager;
import com.handpay.ibenefit.product.service.IProductPublishManager;
import com.handpay.ibenefit.product.service.ISkuPublishManager;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.entity.WelfarePackage;
import com.handpay.ibenefit.welfare.entity.WelfarePackageCategory;
import com.handpay.ibenefit.welfare.entity.WpRelation;
import com.handpay.ibenefit.welfare.service.IWelfareManager;
import com.handpay.ibenefit.welfare.service.IWelfarePackageCategoryManager;
import com.handpay.ibenefit.welfare.service.IWelfarePackageManager;
import com.handpay.ibenefit.welfare.service.IWpRelationManager;

/**
 * @author liran
 *
 */
@Controller
@RequestMapping("/welfarePackage")
public class WelfarePackageController {
    @Reference(version = "1.0")
    private IWpRelationManager wpRelationManager;
    @Reference(version = "1.0")
    private ISkuPublishManager skuPublishManager;
    @Reference(version = "1.0")
    private IWelfarePackageManager welfarePackageManager;
    @Reference(version = "1.0")
    private IWelfarePackageCategoryManager welfarePackageCategoryManager;
    @Reference(version = "1.0")
    private IDictionaryManager dictionaryManager;
    @Reference(version = "1.0")
    private IProductPublishManager productPublishManager;
    @Reference(version = "1.0")
    private IProductManager productManager;
    @Reference(version = "1.0")
    private IWelfareManager welfareManager;
    @Reference(version = "1.0")
    private IPhysicalItemManager physicalItemManager;

    @RequestMapping("/detail/{welfareId}")
    public String detail(HttpServletRequest request, HttpServletResponse response, @PathVariable Long welfareId) {
        boolean isPermission = welfarePackageManager.isHavePermission(FrameworkContextUtils.getCurrentUser().getCompanyId(), welfareId);
        request.setAttribute("isPermission", isPermission);
        WelfarePackage welPac = welfarePackageManager.getPackagePrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), welfareId);
        Integer welfareType = null;
        if (welPac != null) {
            welfareType = welPac.getWelfareType();
        }
        if (welfareType != null && welfareType.equals(IBSConstants.WELFARE_PACKAGE_TYPE_PHYSICAL)) {
            List<PhysicalItem> physicalItems = new ArrayList<PhysicalItem>();
            physicalItems = physicalItemManager.getPhysicalItems(welfareId);
            request.setAttribute("physicalItems", physicalItems);
            request.getSession().setAttribute("physicalPackage",welPac);
            return "mall/physicalDetail";
        } else if (welfareType != null && welfareType.equals(IBSConstants.WELFARE_PACKAGE_TYPE_WELFARE)) {
            WpRelation wpRelation = new WpRelation();
            wpRelation.setPackageId(welfareId);
            wpRelation.setProductType(1);
            Map<String,Object> wmap = new HashMap<String,Object>();
        	wmap.put("packageId", welfareId);
        	wmap.put("type", 1);
            //List<WpRelation> wpRelationList = wpRelationManager.getBySample(wpRelation);
        	List<WpRelation> wpRelationList =wpRelationManager.queryWpRelation(wmap); 
            List<SkuPublish> products = new ArrayList<SkuPublish>();
            WelfarePackage welfarePackage = null;
            WelfarePackageCategory wpc = null;
            if (wpRelationList.size() > 0) {
                welfarePackage = welfarePackageManager.getPackagePrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), wpRelationList.get(0).getPackageId());
                wpc = welfarePackageCategoryManager.getByObjectId(welfarePackage.getWpCategoryId());
            }
            for (int i = 0; i < wpRelationList.size(); i++) {
                SkuPublish skuPublish = new SkuPublish();
                skuPublish = skuPublishManager.getByObjectId(wpRelationList.get(i).getProductId());
                if (skuPublish == null) {
                    continue;
                }
                ProductPublish product = productPublishManager.getByObjectId(skuPublish.getProductId());
                if (product != null) {
                    String mainPicture = productPublishManager.getByObjectId(skuPublish.getProductId()).getMainPicture();
                    skuPublish.setMainPicture(mainPicture);
                }

                /**
                 * ##################查询套餐主题##########################
                 */
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("productId", skuPublish.getProductId());
                map.put("status", ProductConstants.PRODUCT_YES_PUBLISH);
                List<Long> selectedWelfare = productManager.getWelfare(map);
                String welfares = "";
                for (Long id : selectedWelfare) {
                    WelfareItem w = welfareManager.getByObjectId(id);
                    if(w!=null){
                        welfares = welfares + w.getItemName() + ",";
                    }
                }
                if (selectedWelfare.size() > 0) {
                    welfares = welfares.substring(0, welfares.length() - 1);
                }

                skuPublish.setWelfareTag(welfares);
                products.add(skuPublish);
            }
            if (wpc != null) {
                int totalCount = wpc.getFirstParameter();
                int count = wpc.getSecondParameter();

//                Dictionary dictionary = new Dictionary();
//                dictionary.setParentId((long) (55203));// 预算等级的字典objectId为55203，通过设置此parentId查询出对应value的预算
//                dictionary.setValue(wpc.getPackageBudget());
//                dictionary = dictionaryManager.getBySample(dictionary).get(0);
                double money = welfarePackage.getPackagePrice();

                request.setAttribute("count", count);
                request.setAttribute("totalCount", totalCount);
                request.setAttribute("money", money);
                String wpType = dictionaryManager.getDictionaryByDictionaryId(1602).getName();
                request.setAttribute("wpType", wpType);
            }
            request.setAttribute("products", products);
            request.setAttribute("welfarePackage", welfarePackage);
            return "mall/welfareDetail";
        }
        return null;
    }
}
