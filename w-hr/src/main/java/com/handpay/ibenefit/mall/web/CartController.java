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
import com.handpay.ibenefit.base.area.entity.Area;
import com.handpay.ibenefit.base.area.service.IAreaManager;
import com.handpay.ibenefit.category.entity.ProductRecommend;
import com.handpay.ibenefit.category.service.IProductRecommendManager;
import com.handpay.ibenefit.common.service.alipay.IAlipayService;
import com.handpay.ibenefit.framework.config.GlobalConfig;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.order.entity.GoodsReceiptAddr;
import com.handpay.ibenefit.order.entity.Order;
import com.handpay.ibenefit.order.entity.SubOrder;
import com.handpay.ibenefit.order.service.IGoodsReceiptAddrManager;
import com.handpay.ibenefit.order.service.IOrderManager;
import com.handpay.ibenefit.order.service.ISubOrderManager;
import com.handpay.ibenefit.order.vo.SplitSingleBookingOrder;
import com.handpay.ibenefit.product.entity.Cart;
import com.handpay.ibenefit.product.entity.CartView;
import com.handpay.ibenefit.product.entity.SkuPublish;
import com.handpay.ibenefit.product.service.ICartManager;
import com.handpay.ibenefit.product.service.ISkuPublishManager;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.welfare.entity.WelfarePackage;

@Controller
@RequestMapping("/cart")
public class CartController {

    private static final Logger LOGGER = Logger.getLogger(CartController.class);
    @Reference(version = "1.0")
    private ICartManager cartManager;
    @Reference(version = "1.0")
    private IGoodsReceiptAddrManager goodsReceiptAddrManager;
    @Reference(version = "1.0")
    private ISkuPublishManager skuPublishManager;
    @Reference(version = "1.0")
    private IOrderManager orderManager;
    @Reference(version = "1.0")
    private ISubOrderManager subOrderManager;
    @Reference(version = "1.0")
    private IAreaManager areaManager;
    @Reference(version = "1.0")
    private IDictionaryManager dictionaryManager;
    @Reference(version = "1.0")
    private IProductRecommendManager productRecommendManager;
    @Reference(version = "1.0")
    private IUserManager userManager;
    @Reference(version = "1.0")
    private IAlipayService alipayService;

    @RequestMapping("/addCart/{productId}")
    public String addCart(HttpServletRequest request, HttpServletResponse response, ModelMap map,
            @PathVariable Long productId) {
        boolean result = false;
        String productCountStr = request.getParameter("productCount");
        Long productCount = 1L;
        if (StringUtils.isNotBlank(productCountStr)) {
            productCount = Long.parseLong(productCountStr);
        }
        // 根据商品id，商品类型 ，userId查看商品是否存在，如果存在就改变数量，否则创建新纪录
        Cart cart = new Cart();
        cart.setUserId(FrameworkContextUtils.getCurrentUserId());
        cart.setProductId(productId);
        List<Cart> carts = cartManager.getBySample(cart);
        if (carts.size() > 0) {
            // 更新数量
            Cart ca = carts.get(0);
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("objectId", ca.getObjectId());
            param.put("count", ca.getProductCount() + productCount);
            cartManager.updateCountByParam(param);
            result = true;
        } else {
            cart.setUserId(FrameworkContextUtils.getCurrentUserId());
            cart.setProductCount(productCount);
            cartManager.save(cart);
            result = true;
        }
        map.addAttribute("result", result);
        map.addAttribute("productId", productId);
        return "jsonView";
    }

    @RequestMapping("/updateCount")
    public String addCount(HttpServletRequest request, HttpServletResponse response, ModelMap map) {
        boolean result = false;
        String message = "操作成功";
        String objectIdStr = request.getParameter("objectId");
        Long objectId = Long.parseLong(objectIdStr);
        String productCountStr = request.getParameter("productCount");
        Long productCount = Long.parseLong(productCountStr);
        Cart cart = cartManager.getByObjectId(objectId);
        if (!cart.getUserId().equals(FrameworkContextUtils.getCurrentUserId())) {
            message = "你没有权限更改数量";
            map.addAttribute("result", result);
            map.addAttribute("message", message);
            return "jsonView";
        }
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("objectId", objectId);
        param.put("count", productCount);
        cartManager.updateCountByParam(param);
        result = true;
        map.addAttribute("result", result);
        map.addAttribute("message", message);
        return "jsonView";
    }

    @RequestMapping("/delete/{objectId}")
    public String delete(HttpServletRequest request, HttpServletResponse response, ModelMap map,
            @PathVariable Long objectId) {
        boolean result = false;
        String message = "删除成功";
        Cart cart = cartManager.getByObjectId(objectId);
        if (!cart.getUserId().equals(FrameworkContextUtils.getCurrentUserId())) {
            message = "你没有权限删除";
            map.addAttribute("result", result);
            map.addAttribute("message", message);
            return "jsonView";
        }
        cartManager.delete(objectId);
        result = true;
        map.addAttribute("result", result);
        map.addAttribute("message", message);
        return "jsonView";
    }

    @RequestMapping("/delete")
    public String deleteOption(HttpServletRequest request, HttpServletResponse response, ModelMap map) {
        boolean result = false;
        String message = "删除成功";
        String ids = request.getParameter("ids");
        String[] str = ids.split(",");
        for (String idStr : str) {
            Long objectId = Long.parseLong(idStr);
            Cart cart = cartManager.getByObjectId(objectId);
            if (!cart.getUserId().equals(FrameworkContextUtils.getCurrentUserId())) {
                message = "你没有权限删除";
                map.addAttribute("result", result);
                map.addAttribute("message", message);
                return "jsonView";
            }
            cartManager.delete(objectId);
        }
        result = true;
        map.addAttribute("result", result);
        map.addAttribute("message", message);
        return "jsonView";
    }

    @RequestMapping("/queryCount")
    public String queryCount(HttpServletRequest request, HttpServletResponse response, ModelMap map) throws Exception {
        boolean result = false;
        User user = FrameworkContextUtils.getCurrentUser();
        Long count = cartManager.queryCountByUserId(user.getObjectId(), user.getCompanyId());
        result = true;
        map.addAttribute("result", result);
        map.addAttribute("count", count);
        AjaxUtils.doJsonpResponseOfMap(response, request.getParameter("callback"), map);
        return null;
    }

    @RequestMapping("/order")
    public String order(HttpServletRequest request, HttpServletResponse response, ModelMap map, String cartIds) {
        try {
            List<GoodsReceiptAddr> receiptAddrList = new ArrayList<GoodsReceiptAddr>();// 收货地址
            User user = (User) request.getSession().getAttribute("s_user");

            receiptAddrList = goodsReceiptAddrManager.getAllByUserId(user.getObjectId());
            request.setAttribute("receiptAddrList", receiptAddrList);

            // 购物车商品清单
            List<Cart> carts = new ArrayList<Cart>();
            Double totalMoney = 0.0;
            long totalCount = 0;
            if (StringUtils.isNotBlank(cartIds)) {
                String[] cartIdsStr = cartIds.split(",");
                request.setAttribute("cartIds", cartIdsStr);
                for (String idStr : cartIdsStr) {
                    Long id = Long.parseLong(idStr);
                    Cart cart = cartManager.getByObjectId(id);
                    Long productId = cart.getProductId();
                    CartView cv = new CartView();
                    Map<String, Object> param = new HashMap<String, Object>();
                    param.put("objectId", productId);
                    SkuPublish sku = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser()
                            .getCompanyId(), productId);
                    cv.setMainPicture(sku.getProduct().getMainPicture());
                    cv.setName(sku.getName());
                    cv.setSellPrice(sku.getSellPrice());
                    cv.setCheckStatus(sku.getCheckStatus());
                    cart.setCartView(cv);
                    carts.add(cart);
                    totalMoney = totalMoney + cart.getProductCount() * cv.getSellPrice();
                    totalCount = totalCount + cart.getProductCount();
                }
            }
            request.setAttribute("totalCount", totalCount);
            request.setAttribute("totalMoney", totalMoney);
            request.setAttribute("carts", carts);
            List<Area> provinces = areaManager.getRoot();
            request.setAttribute("provinces", provinces);
            String path = "cart/order?cartIds=" + cartIds;
            request.setAttribute("path", path);

            List<Dictionary> payWays = dictionaryManager.getDictionariesByDictionaryId(1403);
            request.setAttribute("payWays", payWays);
        } catch (Exception e) {
            LOGGER.error("cart write a order error!");
        }
        return "mall/cartOrder";
    }

    @RequestMapping("/generateOrder")
    public String generateOrder(HttpServletRequest request, HttpServletResponse response,
            SplitSingleBookingOrder bookingOrder) {
        Order order = null;
        List<Long> objectIds = bookingOrder.getObjectIds();
        for(Long id:objectIds){
            Cart cart = cartManager.getByObjectId(id);
            if(cart==null){
                return "mall/errorCartOrder";
            }
        }
        try {
            User user = (User) request.getSession().getAttribute("s_user");

            bookingOrder.setUserId(user.getObjectId());
            bookingOrder.setOrderType(IBSConstants.ORDER_TYPE_POINT_BUY);// 积分购买
            bookingOrder.setOrderSource(IBSConstants.ORDER_SOURCE_HR);// hr端

            order = orderManager.createOrderByMultiplySubOrder(bookingOrder);
            if(order!=null){
                // 删除购物车
                List<Long> cartIds = bookingOrder.getObjectIds();
                for (Long cartId : cartIds) {
                    cartManager.delete(cartId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("generate cart order have a error");
            return "mall/orderDetail";
        }
        SubOrder subOrder = new SubOrder();
        subOrder.setGeneralOrderId(order.getObjectId());
        List<SubOrder> subs= subOrderManager.getBySample(subOrder);
        String subOrderIds = "";
        for(SubOrder o:subs){
            subOrderIds = subOrderIds+o.getObjectId()+",";
        }
        if(subs.size()>0){
            subOrderIds = subOrderIds.substring(0,subOrderIds.length()-1);
        }
        return "redirect:"+GlobalConfig.getPayDomain()+"/onlinePay/orderPayCenter?subOrderIds="+subOrderIds;
    }

    @RequestMapping("/addSuccess/{skuId}")
    public String addSuccess(HttpServletRequest request, HttpServletResponse response, @PathVariable Long skuId) {
        Cart cart = new Cart();
        cart.setUserId(FrameworkContextUtils.getCurrentUserId());
        cart.setProductId(skuId);
        List<Cart> carts = cartManager.getBySample(cart);
        cart = null;
        if (carts.size() > 0) {
            // 更新数量
            cart = carts.get(0);
            CartView cv = new CartView();
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("objectId", skuId);
            SkuPublish sku = skuPublishManager.getSkuPublishPrice(
                    FrameworkContextUtils.getCurrentUser().getCompanyId(), skuId);
            cv.setMainPicture(sku.getProduct().getMainPicture());
            cv.setName(sku.getName());
            cv.setSellPrice(sku.getSellPrice());
            cv.setCheckStatus(sku.getCheckStatus());
            cart.setCartView(cv);
        }
        Long count = cartManager.queryCountByUserId(FrameworkContextUtils.getCurrentUserId(), FrameworkContextUtils
                .getCurrentUser().getCompanyId());
        request.setAttribute("totalCount", count);
        request.setAttribute("cart", cart);
        // 其他热卖商品
        setRecommendProduct(5, 1, "04_31", request, response, 1);
        return "mall/addCartSuccess";
    }

    private PageSearch setRecommendProduct(Integer pageSize, int number, String positionCode,
            HttpServletRequest request, HttpServletResponse response, Integer currentPage) {
        PageSearch page = new PageSearch();
        page.getFilters().add(
                new PropertyFilter(null, "EQL_companyId", FrameworkContextUtils.getCurrentUser().getCompanyId()
                        .toString()));
        page.setPageSize(pageSize);
        page.setCurrentPage(currentPage);

        page.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(), "EQS_positionCode", positionCode));
        PageSearch result = skuPublishManager.getRecommendProductSkuByParam(page);
        page.setList(result.getList());
        page.setTotalCount(result.getTotalCount());
        // 得到推荐位的大标题和小标题
        ProductRecommend recommend = productRecommendManager.getBasicInfoByPositionCode(positionCode);
        request.setAttribute("recommend_" + number, recommend);
        request.setAttribute("page_" + number, page);
        return page;
    }

    @RequestMapping("/changeBatch")
    public String changeBatch(HttpServletRequest request, HttpServletResponse response, ModelMap map) {
        boolean result = false;
        Integer currentPage = Integer.parseInt(request.getParameter("currentPage"));
        PageSearch page = setRecommendProduct(5, 1, "04_31", request, response, currentPage);
        map.addAttribute("page", page);
        result = true;
        map.addAttribute("result", result);
        return "jsonView";
    }

}
