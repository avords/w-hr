package com.handpay.ibenefit.mall.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.ProductConstants;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.product.entity.SkuPublish;
import com.handpay.ibenefit.product.service.ISkuPublishManager;

@Controller
@RequestMapping("/skuPublish")
public class SkuPublishController {

    @Reference(version="1.0")
    private ISkuPublishManager skuPublishManager;

    @RequestMapping("/searchStock")
    public String searchStock(HttpServletRequest request,HttpServletResponse response,ModelMap map){
        boolean result = false;
        Map<String,Object> param = new HashMap<String,Object>();
        SkuPublish skuPublish = new SkuPublish();
        String attributeValueId1 = request.getParameter("attributeValueId1");
        String attributeValueId2 = request.getParameter("attributeValueId2");
        Long productId = Long.parseLong(request.getParameter("productId"));

        param.put("productId", productId);
        skuPublish.setProductId(productId);
        if(StringUtils.isNotBlank(attributeValueId1)){
            Long attributeId1 = Long.parseLong(attributeValueId1);
            param.put("attributeId1", attributeId1);
            skuPublish.setAttributeId1(attributeId1);
        }
        if(StringUtils.isNotBlank(attributeValueId2)){
            Long attributeId2 = Long.parseLong(attributeValueId2);
            param.put("attributeId2", attributeId2);
            skuPublish.setAttributeId2(attributeId2);
        }

        //通过三个参数查询sku是否唯一，如果唯一则返回sku，否则返回库存
        skuPublish.setCheckStatus(ProductConstants.PRODUCT_STATUS_IN_SALE);
        List<SkuPublish> skus = skuPublishManager.getBySample(skuPublish);
        if(skus.size()>1){
            //获取总库存
            Long totalStock = skuPublishManager.getStock(param);
            map.addAttribute("totalStock", totalStock);
        }else{
            if(skus.size()==0){
                //没有查询到商品
                map.addAttribute("sku", "null");
            }else{
                //返回唯一的sku
                SkuPublish sku = skus.get(0);
                //返回sku实际价格
                sku = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), sku.getObjectId());
                map.addAttribute("sku", sku);
            }
        }
        result = true;
        map.addAttribute("result", result);
        return "jsonView";
    }

    @RequestMapping("/searchStock/{skuId}")
    public String searchStock(HttpServletRequest request,HttpServletResponse response,@PathVariable Long skuId,ModelMap map){
        boolean result = false;
        Long stock = 0L;
        SkuPublish skuPublish = skuPublishManager.getByObjectId(skuId);
        if(skuPublish!=null){
            stock = skuPublish.getStock();
        }
        result = true;
        map.addAttribute("result", result);
        map.addAttribute("stock", stock);
        return "jsonView";
    }
}
