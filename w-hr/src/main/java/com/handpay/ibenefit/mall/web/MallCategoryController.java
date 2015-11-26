package com.handpay.ibenefit.mall.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.category.entity.ProductMallCategory;
import com.handpay.ibenefit.category.service.IProductMallCategoryManager;

@Controller
@RequestMapping("/mallCategory")
public class MallCategoryController {

    @Reference(version = "1.0")
    private IProductMallCategoryManager productMallCategoryManager;
    @RequestMapping("/getSecond/{firstId}")
    public String getSecond(HttpServletRequest request,HttpServletResponse response,@PathVariable Long firstId,ModelMap map){
        Map<String,Object> param = new HashMap<String,Object>();
        param.put("firstId", firstId);
        param.put("platform", IBSConstants.PLATEFORM_HR);
        List<ProductMallCategory> secondCates = productMallCategoryManager.getSecondCategoryByParam(param);
        for(int i=0;i<secondCates.size();i++){
            ProductMallCategory pm = secondCates.get(i);
            Long secondId = pm.getSecondId();
            Map<String,Object> pa = new HashMap<String,Object>();
            pa.put("secondId", secondId);
            pa.put("platform", IBSConstants.PLATEFORM_HR);
            List<ProductMallCategory> thirdCates = productMallCategoryManager.getThirdCategoryByParam(pa);
            pm.setChildrens(thirdCates);
        }
        map.put("secondCates", secondCates);
        return "jsonView";
    }
}
