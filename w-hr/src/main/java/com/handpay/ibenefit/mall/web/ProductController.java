package com.handpay.ibenefit.mall.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.ProductConstants;
import com.handpay.ibenefit.base.area.entity.Area;
import com.handpay.ibenefit.base.area.service.IAreaManager;
import com.handpay.ibenefit.category.entity.ProductMallCategory;
import com.handpay.ibenefit.category.service.IProductMallCategoryManager;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.insure.service.IInsureManager;
import com.handpay.ibenefit.order.entity.GoodsReceiptAddr;
import com.handpay.ibenefit.order.entity.Order;
import com.handpay.ibenefit.order.entity.OrderSku;
import com.handpay.ibenefit.order.entity.RequestBookingOrder;
import com.handpay.ibenefit.order.entity.SubOrder;
import com.handpay.ibenefit.order.service.IGoodsReceiptAddrManager;
import com.handpay.ibenefit.order.service.IOrderManager;
import com.handpay.ibenefit.order.service.IOrderSkuManager;
import com.handpay.ibenefit.order.service.ISubOrderManager;
import com.handpay.ibenefit.product.entity.Attribute;
import com.handpay.ibenefit.product.entity.AttributeValue;
import com.handpay.ibenefit.product.entity.Brand;
import com.handpay.ibenefit.product.entity.ProductPublish;
import com.handpay.ibenefit.product.entity.SkuPublish;
import com.handpay.ibenefit.product.service.IAttributeManager;
import com.handpay.ibenefit.product.service.IBrandManager;
import com.handpay.ibenefit.product.service.IProductManager;
import com.handpay.ibenefit.product.service.IProductPublishManager;
import com.handpay.ibenefit.product.service.ISkuManager;
import com.handpay.ibenefit.product.service.ISkuPublishManager;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.service.IWelfareManager;

@Controller
@RequestMapping("/product")
public class ProductController {

    private static final Logger LOGGER = Logger.getLogger(ProductController.class);
    private static final String PORTAL_DIR = "mall/";

    @Reference(version="1.0")
    private ISkuManager skuManager;
    @Reference(version="1.0")
    private ISkuPublishManager skuPublishManager;
    @Reference(version="1.0")
    private IProductPublishManager productPublishManager;
    @Reference(version="1.0")
    private IProductManager productManager;
    @Reference(version="1.0")
    private IAttributeManager attributeManager;
    @Reference(version="1.0")
    private IWelfareManager welfareManager;
    @Reference(version="1.0")
    private IGoodsReceiptAddrManager goodsReceiptAddrManager;
    @Reference(version = "1.0")
    private IAreaManager areaManager;
    @Reference(version="1.0")
    private IOrderManager orderManager;
    @Reference(version="1.0")
    private ISubOrderManager subOrderManager;
    @Reference(version="1.0")
    private IOrderSkuManager orderSkuManager;
    @Reference(version="1.0")
    private IUserManager userManager;
    @Reference(version="1.0")
    private IBrandManager brandManager;
    @Reference(version="1.0")
    private IProductMallCategoryManager productMallCategoryManager;
    @Reference(version="1.0")
    private IInsureManager insureManager;


    @RequestMapping("/detail/{skuId}")
    public String detail(HttpServletRequest request,HttpServletResponse response,@PathVariable Long skuId){
        //判断此skuId是否在权限表里面
        boolean isPermission = skuPublishManager.isHavePermission(FrameworkContextUtils.getCurrentUser().getCompanyId(), skuId);
        request.setAttribute("isPermission", isPermission);
        SkuPublish sk = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), skuId);
        if(sk==null){
            return PORTAL_DIR+"errorDetail";
        }
        request.setAttribute("mainProduct", sk);
        //为了确认是否聚合，将attributeId1和attributeId2传回前端
        request.setAttribute("attributeId1", sk.getAttributeId1());
        request.setAttribute("attributeValue1", sk.getAttributeValue1());
        request.setAttribute("attributeId2", sk.getAttributeId2());
        request.setAttribute("attributeValue2", sk.getAttributeValue2());

        ProductPublish product = productPublishManager.getByObjectId(sk.getProductId());
        Long productId = product.getObjectId();
        //品牌logo
        String brandLogo = "";
        Brand brand = brandManager.getByObjectId(product.getBrandId());
        if(brand!=null){
            brandLogo = brand.getLogo();
        }
        request.setAttribute("brandLogo", brandLogo);

        Map<String,Object> map = new HashMap<String,Object>();
        map.put("productId", productId);
        map.put("status", ProductConstants.PRODUCT_YES_PUBLISH);

        //得到商品福利标签
        Map<String,Object> map1 = new HashMap<String,Object>();
        map1.put("productId", productId);
        map1.put("status", ProductConstants.PRODUCT_YES_PUBLISH);
        List<Long> selectedWelfare = productManager.getWelfare(map1);
        String welfares = "";
        for(Long id:selectedWelfare){
            WelfareItem w =welfareManager.getByObjectId(id);
            if(w!=null){
                welfares = welfares+w.getItemName()+",";
            }
        }
        if(selectedWelfare.size()>0){
            welfares = welfares.substring(0,welfares.length()-1);
        }
        request.setAttribute("welfares", welfares);
        //得到子图
        List<String> subPics = productManager.getProductPicture(map);
        //得到所有的属性1和属性2
        List<AttributeValue> att1 = skuManager.getAllAttribute1(productId);
        List<AttributeValue> att2 = skuManager.getAllAttribute2(productId);
        List<Attribute> attrs = new ArrayList<Attribute>();
        if(att1.size()>0&&att1.get(0)!=null){
            Long attributeId = att1.get(0).getAttributeId();
            Attribute att = attributeManager.getByObjectId(attributeId);
            att.setAttributeValues(att1);
            attrs.add(att);
        }

        if(att2.size()>0&&att2.get(0)!=null){
            Long attributeId = att2.get(0).getAttributeId();
            Attribute att = attributeManager.getByObjectId(attributeId);
            att.setAttributeValues(att2);
            attrs.add(att);
        }
       //判断属性1和2是否聚合展示
        for(Attribute attr:attrs){
          //得到聚合展示
            Map<String,Object> param = new HashMap<String,Object>();
            param.put("productId", productId);
            param.put("attributeId", attr.getObjectId());
            param.put("status", ProductConstants.PRODUCT_YES_PUBLISH);
            List<Integer> selectedProductTogetherShow = productManager.getProductTogetherShow(param);
            if(selectedProductTogetherShow.size()>0){
                attr.setIsTogeter(selectedProductTogetherShow.get(0));
            }
        }
        request.setAttribute("product", product);
        request.setAttribute("attrs", attrs);
        request.setAttribute("subPics", subPics);
        //面包屑
        Map<String,Object> param = new HashMap<String,Object>();
        param.put("productThirdId", product.getCategoryId());
        param.put("platform", IBSConstants.PLATEFORM_HR);
        List<ProductMallCategory> thirdMalls =  productMallCategoryManager.getAllThirdCategoryByProductThirdId(param);
        if(thirdMalls.size()>0){
            ProductMallCategory thirdMall = thirdMalls.get(0);
            request.setAttribute("thirdMall", thirdMall);
        }
        //04_32商品推荐
        Integer pageSize = 4;
        String pageSizeStr = request.getParameter("pageSize");
        if(StringUtils.isNotBlank(pageSizeStr)){
            pageSize = Integer.parseInt(pageSizeStr);
        }
        PageSearch rightProduct = new PageSearch();
        rightProduct.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        //当前页
        Integer currentPage = 1;
        String currentPageStr = request.getParameter("currentPage");
        if(StringUtils.isNotBlank(currentPageStr)){
            currentPage = Integer.parseInt(currentPageStr);
        }
        rightProduct.setPageSize(pageSize);
        rightProduct.setCurrentPage(currentPage);
        rightProduct.getFilters().add(new PropertyFilter(null,"EQS_positionCode","04_32"));
        //得到右侧商品推荐
        PageSearch result = skuPublishManager.getRecommendProductSkuByParam(rightProduct);
        rightProduct.setList(result.getList());
        rightProduct.setTotalCount(result.getTotalCount());
        request.setAttribute("rightProduct", rightProduct);
        if(sk.getType()==ProductConstants.PRODUCT_TYPE_GROUP_INSURE){
            return insureDetail(request, response,productId,skuId);
        }
        //获取价格最低的sku
        List<AttributeValue> attVal1 = new ArrayList<AttributeValue>();
        List<AttributeValue> attVal2 = new ArrayList<AttributeValue>();
        if(attrs.get(0).getIsTogeter()==IBSConstants.STATUS_YES){
            attVal1 =  attrs.get(0).getAttributeValues();
        }else{
            AttributeValue attrVal = new AttributeValue();
            attrVal.setObjectId(sk.getAttributeId1());
            attVal1.add(attrVal);
        }
        if(attrs.size()>1){
            if(attrs.get(1).getIsTogeter()==IBSConstants.STATUS_YES){
              attVal2 =  attrs.get(1).getAttributeValues();
            }else{
                AttributeValue attrVal = new AttributeValue();
                attrVal.setObjectId(sk.getAttributeId2());
                attVal2.add(attrVal);
            }
        }
        List<SkuPublish> allShowSku = new ArrayList<SkuPublish>();
        for(AttributeValue av1:attVal1){
            if(attVal2.size()>0){
                for(AttributeValue av2:attVal2){
                    SkuPublish skuPublish = new SkuPublish();
                    skuPublish.setAttributeId1(av1.getObjectId());
                    skuPublish.setAttributeId2(av2.getObjectId());
                    skuPublish.setCheckStatus(ProductConstants.PRODUCT_STATUS_IN_SALE);
                    List<SkuPublish> skus = skuPublishManager.getBySample(skuPublish);
                    if(skus.size()>0){
                        allShowSku.add(skus.get(0));
                    }
                }
            }else{
                SkuPublish skuPublish = new SkuPublish();
                skuPublish.setAttributeId1(av1.getObjectId());
                skuPublish.setCheckStatus(ProductConstants.PRODUCT_STATUS_IN_SALE);
                List<SkuPublish> skus = skuPublishManager.getBySample(skuPublish);
                if(skus.size()>0){
                    allShowSku.add(skus.get(0));
                }
            }
        }
        SkuPublish minPriceSku = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), allShowSku.get(0).getObjectId());
        for(int i=1;i<allShowSku.size();i++){
            SkuPublish skuTemp = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), allShowSku.get(0).getObjectId());
            if(skuTemp.getSellPrice()<minPriceSku.getSellPrice()){
                minPriceSku = skuTemp;
            }
        }
        request.setAttribute("mainProduct", minPriceSku);
        //为了确认是否聚合，将attributeId1和attributeId2传回前端
        request.setAttribute("attributeId1", minPriceSku.getAttributeId1());
        request.setAttribute("attributeValue1", minPriceSku.getAttributeValue1());
        request.setAttribute("attributeId2", minPriceSku.getAttributeId2());
        request.setAttribute("attributeValue2", minPriceSku.getAttributeValue2());
        return PORTAL_DIR+"detail";
    }

    @RequestMapping("/skuDetail/{skuId}")
    public String skuDetail(HttpServletRequest request,HttpServletResponse response,@PathVariable Long skuId){
      //判断此skuId是否在权限表里面
        boolean isPermission = skuPublishManager.isHavePermission(FrameworkContextUtils.getCurrentUser().getCompanyId(), skuId);
        request.setAttribute("isPermission", isPermission);
        SkuPublish sk = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), skuId);
        request.setAttribute("mainProduct", sk);
        //为了确认是否聚合，将attributeId1和attributeId2传回前端
        request.setAttribute("attributeId1", sk.getAttributeId1());
        request.setAttribute("attributeValue1", sk.getAttributeValue1());
        request.setAttribute("attributeId2", sk.getAttributeId2());
        request.setAttribute("attributeValue2", sk.getAttributeValue2());

        ProductPublish product = productPublishManager.getByObjectId(sk.getProductId());
        Long productId = product.getObjectId();
        //品牌logo
        String brandLogo = "";
        Brand brand = brandManager.getByObjectId(product.getBrandId());
        if(brand!=null){
            brandLogo = brand.getLogo();
        }
        request.setAttribute("brandLogo", brandLogo);

        Map<String,Object> map = new HashMap<String,Object>();
        map.put("productId", productId);
        map.put("status", ProductConstants.PRODUCT_YES_PUBLISH);

        //得到商品福利标签
        Map<String,Object> map1 = new HashMap<String,Object>();
        map1.put("productId", productId);
        map1.put("status", ProductConstants.PRODUCT_YES_PUBLISH);
        List<Long> selectedWelfare = productManager.getWelfare(map1);
        String welfares = "";
        for(Long id:selectedWelfare){
            WelfareItem w =welfareManager.getByObjectId(id);
            if(w!=null){
                welfares = welfares+w.getItemName()+",";
            }
        }
        if(selectedWelfare.size()>1){
            welfares = welfares.substring(0,welfares.length()-1);
        }
        request.setAttribute("welfares", welfares);
        //得到子图
        List<String> subPics = productManager.getProductPicture(map);
        //得到所有的属性1和属性2
        List<AttributeValue> att1 = skuManager.getAllAttribute1(productId);
        List<AttributeValue> att2 = skuManager.getAllAttribute2(productId);
        List<Attribute> attrs = new ArrayList<Attribute>();
        if(att1.size()>0&&att1.get(0)!=null){
            Long attributeId = att1.get(0).getAttributeId();
            Attribute att = attributeManager.getByObjectId(attributeId);
            att.setAttributeValues(att1);
            attrs.add(att);
        }

        if(att2.size()>0&&att2.get(0)!=null){
            Long attributeId = att2.get(0).getAttributeId();
            Attribute att = attributeManager.getByObjectId(attributeId);
            att.setAttributeValues(att2);
            attrs.add(att);
        }
       //判断属性1和2是否聚合展示
        for(Attribute attr:attrs){
            attr.setIsTogeter(0);
        }
        request.setAttribute("product", product);
        request.setAttribute("attrs", attrs);
        request.setAttribute("subPics", subPics);
        //04_32商品推荐
        Integer pageSize = 4;
        String pageSizeStr = request.getParameter("pageSize");
        if(StringUtils.isNotBlank(pageSizeStr)){
            pageSize = Integer.parseInt(pageSizeStr);
        }
        PageSearch rightProduct = new PageSearch();
        rightProduct.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        //当前页
        Integer currentPage = 1;
        String currentPageStr = request.getParameter("currentPage");
        if(StringUtils.isNotBlank(currentPageStr)){
            currentPage = Integer.parseInt(currentPageStr);
        }
        rightProduct.setPageSize(pageSize);
        rightProduct.setCurrentPage(currentPage);
        rightProduct.getFilters().add(new PropertyFilter(null,"EQS_positionCode","04_32"));
        //得到右側商品推薦
        PageSearch result = skuPublishManager.getRecommendProductSkuByParam(rightProduct);
        rightProduct.setList(result.getList());
        rightProduct.setTotalCount(result.getTotalCount());
        request.setAttribute("rightProduct", rightProduct);
        return PORTAL_DIR+"detail";
    }

    @RequestMapping("/productOrder/{skuId}")
    public String productOrder(HttpServletRequest request, HttpServletResponse response, ModelMap map,@PathVariable Long skuId) {
        try {
            List<GoodsReceiptAddr> receiptAddrList = new ArrayList<GoodsReceiptAddr>();// 收货地址
            User user = (User) request.getSession().getAttribute("s_user");
            receiptAddrList = goodsReceiptAddrManager.getAllByUserId(user.getObjectId());
            request.setAttribute("receiptAddrList", receiptAddrList);
            Double totalMoney = 0.0;
            SkuPublish sku = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), skuId);
            String count = request.getParameter("count");
            totalMoney = totalMoney+Integer.parseInt(count)*sku.getSellPrice();
            List<Area> provinces = areaManager.getRoot();
            String path = "product/productOrder/"+skuId+"?count="+count;
            request.setAttribute("path", path);
            request.setAttribute("provinces", provinces);
            request.setAttribute("totalMoney", totalMoney);
            request.setAttribute("count", count);
            request.setAttribute("sku", sku);
        } catch (Exception e) {
            LOGGER.error("product write a order error!");
        }
        return "mall/productOrder";
    }

    /**
     * generateOrder(员工端商品立即购买生成订单进行支付页面)
     * TODO(这里描述这个方法适用条件 – 可选)
     * @param   name
     * @param  @return    设定文件
     * @return String    DOM对象
     * @Exception 异常对象
     * @since  CodingExample　Ver(编码范例查看) 1.1
     */
    @RequestMapping("/generateOrder")
    public String generateOrder(HttpServletRequest request, HttpServletResponse response, RequestBookingOrder bookingOrder) {
        try {
            User user = (User) request.getSession().getAttribute("s_user");
            bookingOrder.setUserId(user.getObjectId());
            bookingOrder.setOrderType(IBSConstants.ORDER_TYPE_POINT_BUY);//年度福利
            bookingOrder.setOrderSource(IBSConstants.ORDER_SOURCE_STAFF);//员工端
            Order order = orderManager.createImmediateOrder(bookingOrder);
            SubOrder subOrder = new SubOrder();
            subOrder.setGeneralOrderId(order.getObjectId());
            subOrder = subOrderManager.getBySample(subOrder).get(0);
            //查询员工剩余积分
            double surplusScore = userManager.getUserBalance(user.getObjectId());
            String skuName = request.getParameter("skuName");
            request.setAttribute("skuName",skuName);
            request.setAttribute("subOrder", subOrder);
            request.setAttribute("surplusScore", surplusScore);
        } catch (Exception e) {
            LOGGER.error("generate cart order have a error");
        }
        return "mall/orderPayProduct";
    }

    /**
     * subOrderPay(员工端订单列表进入立即支付页面)
     * TODO(这里描述这个方法适用条件 – 可选)
     * @param   name
     * @param  @return    设定文件
     * @return String    DOM对象
     * @Exception 异常对象
     * @since  CodingExample　Ver(编码范例查看) 1.1
     */
    @RequestMapping("/subOrderPay/{subOrderId}")
    public String subOrderPay(HttpServletRequest request, HttpServletResponse response, @PathVariable Long subOrderId) {
        try {
            User user = (User) request.getSession().getAttribute("s_user");
            SubOrder subOrder = subOrderManager.getByObjectId(subOrderId);
            OrderSku orderSku = new OrderSku();
            orderSku.setSubOrderId(subOrderId);
            orderSku = orderSkuManager.getBySample(orderSku).get(0);
            //查询员工剩余积分
            double surplusScore = userManager.getUserBalance(user.getObjectId());
            request.setAttribute("skuName",orderSku.getName());
            request.setAttribute("subOrder", subOrder);
            request.setAttribute("surplusScore", surplusScore);
        } catch (Exception e) {
            LOGGER.error("generate cart order have a error");
        }
        return "mall/orderPayProduct";
    }

    private String insureDetail(HttpServletRequest request,HttpServletResponse response,Long productId,Long skuId){
        List<Attribute> attrs = (List<Attribute>) request.getAttribute("attrs");
        for(Attribute attribute:attrs){
            for(AttributeValue attributeValue:attribute.getAttributeValues()){
                String specPicUrl = productManager.getSpecPic(productId, attributeValue.getObjectId());
                attributeValue.setSpecPicUrl(specPicUrl);
            }
        }
        String jsonData = insureManager.getOrganizations(FrameworkContextUtils.getCurrentUser().getCompanyId());
        request.setAttribute("jsonData", jsonData);
        return PORTAL_DIR+"insureDetail";
    }
}
