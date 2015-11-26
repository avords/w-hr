package com.handpay.ibenefit.mall.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.base.area.service.IAreaManager;
import com.handpay.ibenefit.category.entity.ProductMallCategory;
import com.handpay.ibenefit.category.entity.ProductRecommend;
import com.handpay.ibenefit.category.service.IProductMallCategoryManager;
import com.handpay.ibenefit.category.service.IProductRecommendManager;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.news.entity.Advert;
import com.handpay.ibenefit.news.entity.AdvertCategory;
import com.handpay.ibenefit.news.service.IAdvertCategoryManager;
import com.handpay.ibenefit.news.service.IAdvertManager;
import com.handpay.ibenefit.product.entity.Attribute;
import com.handpay.ibenefit.product.entity.AttributeValue;
import com.handpay.ibenefit.product.entity.Brand;
import com.handpay.ibenefit.product.entity.Cart;
import com.handpay.ibenefit.product.entity.CartView;
import com.handpay.ibenefit.product.entity.Product;
import com.handpay.ibenefit.product.entity.ProductPublish;
import com.handpay.ibenefit.product.entity.SkuPublish;
import com.handpay.ibenefit.product.service.IAttributeManager;
import com.handpay.ibenefit.product.service.IAttributeValueManager;
import com.handpay.ibenefit.product.service.IBrandManager;
import com.handpay.ibenefit.product.service.ICartManager;
import com.handpay.ibenefit.product.service.IProductPublishManager;
import com.handpay.ibenefit.product.service.ISkuPublishManager;
import com.handpay.ibenefit.search.SearchParams;
import com.handpay.ibenefit.search.SearchResult;
import com.handpay.ibenefit.search.service.ISearchManager;
import com.handpay.ibenefit.welfare.entity.PackageView;
import com.handpay.ibenefit.welfare.entity.WelfareItem;
import com.handpay.ibenefit.welfare.entity.WelfarePackage;
import com.handpay.ibenefit.welfare.entity.WpRelation;
import com.handpay.ibenefit.welfare.service.IWelfareManager;
import com.handpay.ibenefit.welfare.service.IWelfarePackageManager;
import com.handpay.ibenefit.welfare.service.IWpRelationManager;

@Controller
@RequestMapping("/mall")
public class MallController {

    private static final String BASE_DIR = "mall/";

    @Reference(version = "1.0")
    private IProductMallCategoryManager productMallCategoryManager;
    @Reference(version="1.0")
    private ISkuPublishManager skuPublishManager;
    @Reference(version="1.0")
    private IAreaManager areaManager;
    @Reference(version="1.0")
    private IAdvertCategoryManager advertCategoryManager;
    @Reference(version="1.0")
    private IWelfarePackageManager welfarePackageManager;
    @Reference(version="1.0")
    private IWelfareManager welfareManager;
    @Reference(version="1.0")
    private ICartManager cartManager;
    @Reference(version="1.0")
    private IAdvertManager advertManager;
    @Reference(version="1.0")
    private IProductRecommendManager productRecommendManager;
    @Reference(version="1.0")
    private ISearchManager searchManager;
    @Reference(version="1.0")
    private IBrandManager brandManager;
    @Reference(version="1.0")
    private IWpRelationManager wpRelationManager;
    @Reference(version="1.0")
    private IProductPublishManager productPublishManager;
    @Reference(version="1.0")
    private IAttributeManager attributeManager;
    @Reference(version="1.0")
    private IAttributeValueManager attributeValueManager;
    @Reference(version="1.0")
    private IDictionaryManager dictionaryManager;

    private void setRecommendPackage(Integer pageSize,int number,String positionCode,HttpServletRequest request,HttpServletResponse response){
         PageSearch page = new PageSearch();
         page.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
         page.setPageSize(pageSize);
         page.setCurrentPage(1);

         page.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQS_positionCode",positionCode));
         PageSearch result = welfarePackageManager.getRecommendPackage(page);
         page.setList(result.getList());
         page.setTotalCount(result.getTotalCount());
         //得到推荐位的大标题和小标题
         ProductRecommend recommend = productRecommendManager.getBasicInfoByPositionCode(positionCode);
         if("04_05".equals(positionCode)){
             List<PackageView> list = page.getList();
             for(PackageView p:list){
                 Long packageId = p.getPackageId();
                 WpRelation wpRelation = new WpRelation();
                 wpRelation.setPackageId(packageId);
                 wpRelation.setProductType(1);
                 List<WpRelation> wpRelationList = wpRelationManager.getBySample(wpRelation);
                 for (int i = 0; i < wpRelationList.size(); i++) {
                     if(i==4){
                         break;
                     }
                     SkuPublish skuPublish = new SkuPublish();
                     skuPublish = skuPublishManager.getByObjectId(wpRelationList.get(i).getProductId());
                     if(skuPublish==null){
                         continue;
                     }
                     ProductPublish product = productPublishManager.getByObjectId(skuPublish.getProductId());
                     if(product!=null){
                         String mainPicture = productPublishManager.getByObjectId(skuPublish.getProductId()).getMainPicture();
                         List<String> pictures = p.getProductPictures();
                         if(pictures==null){
                             pictures = new ArrayList<String>();
                             p.setProductPictures(pictures);
                         }
                         pictures.add(mainPicture);
                     }
                 }
             }
         }
         request.setAttribute("recommend_"+number, recommend);
         request.setAttribute("page_"+number, page);
    }

    private void setRecommendProduct(Integer pageSize,int number,String positionCode,HttpServletRequest request,HttpServletResponse response){
        PageSearch page = new PageSearch();
        page.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        page.setPageSize(pageSize);
        page.setCurrentPage(1);

        page.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQS_positionCode",positionCode));
        PageSearch result = skuPublishManager.getRecommendProductSkuByParam(page);
        page.setList(result.getList());
        page.setTotalCount(result.getTotalCount());
        //得到推荐位的大标题和小标题
        ProductRecommend recommend = productRecommendManager.getBasicInfoByPositionCode(positionCode);
        request.setAttribute("recommend_"+number, recommend);
        request.setAttribute("page_"+number, page);
   }

    @RequestMapping("/menuIndex")
    public String menuIndex(HttpServletRequest request,HttpServletResponse response){
    	return index(request,response);
    }
    @RequestMapping("/index")
    public String index(HttpServletRequest request,HttpServletResponse response){
    	String areaCode  = (String)request.getSession().getAttribute("areaCode");
        //得到商城所有的一级菜单
        List<ProductMallCategory> firstCates = productMallCategoryManager.getAllFirstCategory(IBSConstants.PLATEFORM_HR,8);
        request.setAttribute("firstCates", firstCates);

        List<Advert> banner = advertManager.getAdvertByParams("04_03", areaCode, 3);
        request.setAttribute("banner", banner);
        int bannerNum  = banner.size();
        request.setAttribute("bannerNum", bannerNum);
        //04_04 套餐(更多跳当季特供)
        setRecommendPackage(4, 1, "04_04", request, response);
        //04_05 套餐(更多跳当季特供)
        setRecommendPackage(4, 2, "04_05", request, response);
        //04_06 套餐(更多跳搜索结果)
        setRecommendPackage(3, 3, "04_06", request, response);
        //04_07 套餐
        setRecommendPackage(3, 4, "04_07", request, response);
        //04_08 套餐
        setRecommendPackage(3, 5, "04_08", request, response);
        //04_09 套餐
        setRecommendPackage(3, 6, "04_09", request, response);
        //04_10 套餐
        setRecommendPackage(3, 7, "04_10", request, response);
        //04_11 套餐
        setRecommendPackage(3, 8, "04_11", request, response);
        //商品
        //实物主图推荐1和实物主图推荐2
        List<Advert> productAdvert1 = advertManager.getAdvertByParams("04_12", areaCode, 1);
        List<Advert> productAdvert2 = advertManager.getAdvertByParams("04_18", areaCode, 1);
        request.setAttribute("productAdvert1", productAdvert1.size()>0?productAdvert1.get(0):new Advert());
        request.setAttribute("productAdvert2", productAdvert2.size()>0?productAdvert2.get(0):new Advert());
        //商品位 1-1 1-2 1-3 1-4 1-5
        setRecommendProduct(4, 11, "04_13", request, response);
        setRecommendProduct(4, 12, "04_14", request, response);
        setRecommendProduct(4, 13, "04_15", request, response);
        setRecommendProduct(4, 14, "04_16", request, response);
        setRecommendProduct(4, 15, "04_17", request, response);
        //商品位 2-1 2-2 2-3 2-4 2-5
        setRecommendProduct(4, 21, "04_19", request, response);
        setRecommendProduct(4, 22, "04_20", request, response);
        setRecommendProduct(4, 23, "04_21", request, response);
        setRecommendProduct(4, 24, "04_22", request, response);
        setRecommendProduct(4, 25, "04_23", request, response);
        request.setAttribute("subject", "index");
        return BASE_DIR+"index";
    }

    @RequestMapping("/season")
    public String season(HttpServletRequest request,HttpServletResponse response){
      //推荐套餐和商品
      //positionCode 04-24
      PageSearch pageProduct = new PageSearch();
      PageSearch pagePackage = new PageSearch();
      pageProduct.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
      pagePackage.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
      Integer currentPageProduct = 1;
      Integer currentPagePackage = 1;
      String currentPageProductStr = request.getParameter("currentPageProduct");
      String currentPagePackageStr = request.getParameter("currentPagePackage");
      if(StringUtils.isNotBlank(currentPageProductStr)){
          currentPageProduct = Integer.parseInt(currentPageProductStr);
      }
      if(StringUtils.isNotBlank(currentPagePackageStr)){
          currentPagePackage = Integer.parseInt(currentPagePackageStr);
      }
      pageProduct.setPageSize(20);//5 x 4行
      pageProduct.setCurrentPage(currentPageProduct);

      pagePackage.setPageSize(24);// 3 x 8行
      pagePackage.setCurrentPage(currentPagePackage);
      //=============================================================================查询条件
      String welfareIdStr = request.getParameter("welfareItemId");
      if(StringUtils.isNotBlank(welfareIdStr)){
          if(!"all".equalsIgnoreCase(welfareIdStr)){
              pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQL_welfareItemId",welfareIdStr));
              pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQL_welfareItemId",welfareIdStr));
              request.setAttribute("welfareItemId", welfareIdStr);
          }else{
              request.setAttribute("welfareItemId", "all");
          }
      }else{
          request.setAttribute("welfareItemId", "all");
      }
    //价格区域
    String priceZone = request.getParameter("priceZone");
      if(StringUtils.isNotBlank(priceZone)){
          if(!"all".equalsIgnoreCase(priceZone)){
              String[] priceZones = priceZone.split("-");
              if(priceZones.length>1){
                  pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
                  pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQN_priceZoneBefore",priceZones[1]));
                  pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
                  pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQN_priceZoneBefore",priceZones[1]));
              }else if(!priceZone.startsWith("all")){
                  pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
                  pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
              }
              request.setAttribute("priceZone", priceZone);
          }else{
              request.setAttribute("priceZone", "all");
          }
    }else{
        request.setAttribute("priceZone", "all");
    }
      //套餐类型
      String wpTypeStr = request.getParameter("wpType");
      if(StringUtils.isNotBlank(wpTypeStr)){
          if(!"all".equalsIgnoreCase(wpTypeStr)){
              pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQI_wpType",wpTypeStr));
              request.setAttribute("wpType", wpTypeStr);
          }else{
              request.setAttribute("wpType", "all");
          }
      }else{
          request.setAttribute("wpType", "all");
      }
      //品牌
      String brandIdStr = request.getParameter("brandId");
      if(StringUtils.isNotBlank(brandIdStr)){
          if(!"all".equalsIgnoreCase(brandIdStr)){
              pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQL_brandId",brandIdStr));
              pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQL_brandId",brandIdStr));
              request.setAttribute("brandId", brandIdStr);
          }else{
              request.setAttribute("brandId", "all");
          }
      }else{
          request.setAttribute("brandId", "all");
      }
      //区域
      String areaIdStr = request.getParameter("areaId");
      if(StringUtils.isNotBlank(areaIdStr)){
          if(!"all".equalsIgnoreCase(areaIdStr)){
              pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQL_areaId",areaIdStr));
              pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQL_areaId",areaIdStr));
              request.setAttribute("areaId", areaIdStr);
          }else{
              request.setAttribute("areaId", "all");
          }
      }else{
          request.setAttribute("areaId", "all");
      }
      //================================================================================
        //推荐商品
        pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQS_positionCode","04_25"));
        PageSearch result = skuPublishManager.getRecommendProductSkuByParam(pageProduct);
        pageProduct.setList(result.getList());
        pageProduct.setTotalCount(result.getTotalCount());
        request.setAttribute("pageProduct", pageProduct);
        //推荐套餐
        pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQS_positionCode","04_24"));
        PageSearch resultPack = welfarePackageManager.getRecommendPackage(pagePackage);
        pagePackage.setList(resultPack.getList());
        pagePackage.setTotalCount(resultPack.getTotalCount());
        request.setAttribute("pagePackage", pagePackage);
        request.setAttribute("subject", "season");

        //福利标签条件查询
        List<WelfareItem> welfareItems = getThirdWelfare(12);
        pageProduct.getFilters().add(new PropertyFilter(null,"EQL_brandCount","12"));
        pageProduct.setPageSize(Integer.MAX_VALUE);
        List<Brand> brands = brandManager.getRecommendBrands(pageProduct);
        request.setAttribute("welfareItems", welfareItems);
        request.setAttribute("brands", brands);
        pageProduct.setPageSize(20);//5 x 4行
        return BASE_DIR+"season";
    }

    @RequestMapping("/welfare")
    public String welfare(HttpServletRequest request,HttpServletResponse response){
        //推荐套餐和商品
        PageSearch pagePackage = new PageSearch();
        pagePackage.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        Integer currentPagePackage = 1;
        String currentPagePackageStr = request.getParameter("currentPagePackage");
        if(StringUtils.isNotBlank(currentPagePackageStr)){
            currentPagePackage = Integer.parseInt(currentPagePackageStr);
        }

        pagePackage.setPageSize(24);// 3 x 8行
        pagePackage.setCurrentPage(currentPagePackage);
        //=============================================================================查询条件
        //分类查询
        String welfareIdStr = request.getParameter("welfareItemId");
        if(StringUtils.isNotBlank(welfareIdStr)){
            if(!"all".equalsIgnoreCase(welfareIdStr)){
                pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQL_welfareItemId",welfareIdStr));
                request.setAttribute("welfareItemId", welfareIdStr);
            }else{
                request.setAttribute("welfareItemId", "all");
            }
        }else{
            request.setAttribute("welfareItemId", "all");
        }
      //价格区域
      String priceZone = request.getParameter("priceZone");
        if(StringUtils.isNotBlank(priceZone)){
            if(!"all".equalsIgnoreCase(priceZone)){
                String[] priceZones = priceZone.split("-");
                if(priceZones.length>1){
                    pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
                    pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQN_priceZoneBefore",priceZones[1]));
                }else if(!priceZone.startsWith("all")){
                    pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
                }
                request.setAttribute("priceZone", priceZone);
            }else{
                request.setAttribute("priceZone", "all");
            }
      }else{
          request.setAttribute("priceZone", "all");
      }
        //套餐类型
        String wpTypeStr = request.getParameter("wpType");
        if(StringUtils.isNotBlank(wpTypeStr)){
            if(!"all".equalsIgnoreCase(wpTypeStr)){
                pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQI_wpType",wpTypeStr));
                request.setAttribute("wpType", wpTypeStr);
            }else{
                request.setAttribute("wpType", "all");
            }
        }else{
            request.setAttribute("wpType", "all");
        }
        //区域
        String areaIdStr = request.getParameter("areaId");
        if(StringUtils.isNotBlank(areaIdStr)){
            if(!"all".equalsIgnoreCase(areaIdStr)){
                pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQL_areaId",areaIdStr));
                request.setAttribute("areaId", areaIdStr);
            }else{
                request.setAttribute("areaId", "all");
            }
        }else{
            request.setAttribute("areaId", "all");
        }
        //================================================================================
          //套餐
          pagePackage.getFilters().add(new PropertyFilter(null,"EQI_welfareType","0"));
          PageSearch resultPack = welfarePackageManager.getPublishPackage(pagePackage);
          pagePackage.setList(resultPack.getList());
          pagePackage.setTotalCount(resultPack.getTotalCount());

          request.setAttribute("pagePackage", pagePackage);
          //福利标签条件查询
          List<WelfareItem> welfareItems = getThirdWelfare(12);
          request.setAttribute("welfareItems", welfareItems);
          request.setAttribute("subject", "welfare");
        return BASE_DIR+"welfare";
    }

    @RequestMapping("/search")
    public String search(HttpServletRequest request,HttpServletResponse response){
        String queryKeywords = request.getParameter("queryKeywords");
        request.setAttribute("queryKeywords", queryKeywords);
        SearchParams sParam = new SearchParams();
        sParam.setQueryWords(queryKeywords);
        //推荐套餐和商品
        //positionCode 04-24
        PageSearch pageProduct = new PageSearch();
        PageSearch pagePackage = new PageSearch();
        PageSearch pageProductOther = new PageSearch();
        pageProduct.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        pagePackage.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        pageProductOther.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        Integer currentPageProduct = 1;
        Integer currentPagePackage = 1;
        Integer currentPageProductOther = 1;
        String currentPageProductStr = request.getParameter("currentPageProduct");
        String currentPagePackageStr = request.getParameter("currentPagePackage");
        String currentPageProductOtherStr = request.getParameter("currentPageProductOther");
        if(StringUtils.isNotBlank(currentPageProductStr)){
            currentPageProduct = Integer.parseInt(currentPageProductStr);
        }
        if(StringUtils.isNotBlank(currentPagePackageStr)){
            currentPagePackage = Integer.parseInt(currentPagePackageStr);
        }
        if(StringUtils.isNotBlank(currentPageProductOtherStr)){
            currentPageProductOther = Integer.parseInt(currentPageProductOtherStr);
        }
        pageProduct.setPageSize(20); // 5 x 4行
        pageProduct.setCurrentPage(currentPageProduct);

        pagePackage.setPageSize(24); // 3 x 8行
        pagePackage.setCurrentPage(currentPagePackage);

        pageProductOther.setPageSize(5);
        pageProductOther.setCurrentPage(currentPageProductOther);
        //=============================================================================查询条件
        //分类查询
        //一级分类
        String firstIdStr = request.getParameter("firstId");
        if(StringUtils.isNotBlank(firstIdStr)){
            if(!"all".equalsIgnoreCase(firstIdStr)){
                pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQL_firstId",firstIdStr));
                request.setAttribute("firstId", firstIdStr);
                //得到一级商城分类
                ProductMallCategory firstMall = new ProductMallCategory();
                firstMall.setFirstId(Long.parseLong(firstIdStr));
                firstMall.setLayer(1);
                firstMall.setPlatform(IBSConstants.PLATEFORM_HR);
                firstMall = productMallCategoryManager.getBySample(firstMall).get(0);
                request.setAttribute("firstMall", firstMall);
                pagePackage.setPageSize(3);
            }else{
                request.setAttribute("firstId", "all");
            }
        }else{
            request.setAttribute("firstId", "all");
        }
        String secondIdStr = request.getParameter("secondId");
        if(StringUtils.isNotBlank(secondIdStr)){
            if(!"all".equalsIgnoreCase(secondIdStr)){
                pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQL_secondId",secondIdStr));
                request.setAttribute("secondId", secondIdStr);
                //得到二级商城分类
                ProductMallCategory secondMall = new ProductMallCategory();
                secondMall.setSecondId(Long.parseLong(secondIdStr));
                secondMall.setLayer(2);
                secondMall.setPlatform(IBSConstants.PLATEFORM_HR);
                secondMall = productMallCategoryManager.getBySample(secondMall).get(0);
                //一级分类
                ProductMallCategory firstMall = new ProductMallCategory();
                firstMall.setFirstId(secondMall.getFirstId());
                firstMall.setLayer(1);
                firstMall.setPlatform(IBSConstants.PLATEFORM_HR);
                firstMall = productMallCategoryManager.getBySample(firstMall).get(0);
                secondMall.setFirstName(firstMall.getName());
                request.setAttribute("secondMall", secondMall);
                pagePackage.setPageSize(3);
            }else{
                request.setAttribute("secondId", "all");
            }
        }else{
            request.setAttribute("secondId", "all");
        }
        String categoryIdStr = request.getParameter("categoryId");
        if(StringUtils.isNotBlank(categoryIdStr)){
            if(!"all".equalsIgnoreCase(categoryIdStr)){
                pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQL_categoryId",categoryIdStr));
                request.setAttribute("categoryId", categoryIdStr);
                //得到三级商城分类
                Map<String,Object> map = new HashMap<String,Object>();
                map.put("platform", IBSConstants.PLATEFORM_HR);
                map.put("categoryId", Long.parseLong(categoryIdStr));
                ProductMallCategory thirdMall = productMallCategoryManager.getThirdCategoryByParam(map).get(0);
                request.setAttribute("thirdMall", thirdMall);
                pagePackage.setPageSize(3);
            }else{
                request.setAttribute("categoryId", "all");
            }
        }else{
            request.setAttribute("categoryId", "all");
        }

        String welfareIdStr = request.getParameter("welfareItemId");
        if(StringUtils.isNotBlank(welfareIdStr)){
            if(!"all".equalsIgnoreCase(welfareIdStr)){
                pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQL_welfareItemId",welfareIdStr));
                pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQL_welfareItemId",welfareIdStr));
                sParam.setWelfareId(Long.parseLong(welfareIdStr));
                request.setAttribute("welfareItemId", welfareIdStr);
            }else{
                request.setAttribute("welfareItemId", "all");
            }
        }else{
            request.setAttribute("welfareItemId", "all");
        }
      //价格区域
      String priceZone = request.getParameter("priceZone");
        if(StringUtils.isNotBlank(priceZone)){
            if(!"all".equalsIgnoreCase(priceZone)){
                String[] priceZones = priceZone.split("-");
                if(priceZones.length>1){
                    pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
                    pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQN_priceZoneBefore",priceZones[1]));
                    pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
                    pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQN_priceZoneBefore",priceZones[1]));
                }else if(!priceZone.startsWith("all")){
                    pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
                    pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQN_priceZoneAfter",priceZones[0]));
                }
                request.setAttribute("priceZone", priceZone);
            }else{
                request.setAttribute("priceZone", "all");
            }
      }else{
          request.setAttribute("priceZone", "all");
      }
        //套餐类型
        String wpTypeStr = request.getParameter("wpType");
        if(StringUtils.isNotBlank(wpTypeStr)){
            if(!"all".equalsIgnoreCase(wpTypeStr)){
                pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQI_wpType",wpTypeStr));
                sParam.setPackageType(Long.parseLong(wpTypeStr));
                request.setAttribute("wpType", wpTypeStr);
            }else{
                request.setAttribute("wpType", "all");
            }
        }else{
            request.setAttribute("wpType", "all");
        }
        //品牌
        String brandIdStr = request.getParameter("brandId");
        if(StringUtils.isNotBlank(brandIdStr)){
            if(!"all".equalsIgnoreCase(brandIdStr)){
                pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQL_brandId",brandIdStr));
                pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQL_brandId",brandIdStr));
                sParam.setBandId(Long.parseLong(brandIdStr));
                request.setAttribute("brandId", brandIdStr);
            }else{
                request.setAttribute("brandId", "all");
            }
        }else{
            request.setAttribute("brandId", "all");
        }
        //区域
        String areaIdStr = request.getParameter("areaId");
        if(StringUtils.isNotBlank(areaIdStr)){
            if(!"all".equalsIgnoreCase(areaIdStr)){
                pageProduct.getFilters().add(new PropertyFilter(Product.class.getName(),"EQL_areaId",areaIdStr));
                pagePackage.getFilters().add(new PropertyFilter(WelfarePackage.class.getName(),"EQL_areaId",areaIdStr));
                request.setAttribute("areaId", areaIdStr);
            }else{
                request.setAttribute("areaId", "all");
            }
        }else{
            request.setAttribute("areaId", "all");
        }
        //================================================================================
          List<Long> productIds = new ArrayList<Long>();
          List<Long> packageIds = new ArrayList<Long>();
          if(StringUtils.isNotBlank(queryKeywords)){
              SearchResult searchResult = searchManager.process(sParam);
              productIds = searchResult.getGoodsIdList();
              packageIds = searchResult.getPackageIdList();
          }
          StringBuilder searchProductIdsBuilder = new StringBuilder();
          StringBuilder searchPackageIdsBuilder = new StringBuilder();
          String searchProductIds = "";
          String searchPackageIds = "";
          for(Long id:productIds){
              searchProductIdsBuilder.append(id).append(",");
          }
          for(Long id:packageIds){
              searchPackageIdsBuilder.append(id).append(",");
          }
          if(searchProductIdsBuilder.length()>0){
              searchProductIds = searchProductIdsBuilder.substring(0, searchProductIdsBuilder.length()-1);
          }
          if(searchPackageIdsBuilder.length()>0){
              searchPackageIds = searchPackageIdsBuilder.substring(0,searchPackageIdsBuilder.length()-1);
          }
         //商品
          if(StringUtils.isNotBlank(queryKeywords)&&searchProductIds.length()>0){
              pageProduct.getFilters().add(new PropertyFilter(null,"EQS_searchProductIds",searchProductIds.toString()));
          }else if(StringUtils.isNotBlank(queryKeywords)&&searchProductIds.length()==0){
              pageProduct.getFilters().add(new PropertyFilter(null,"EQS_searchProductIds","-99999"));
          }
          if(StringUtils.isNotBlank(queryKeywords)&&searchProductIds.equals("")){
              pageProduct.setList(new ArrayList());
              pageProduct.setTotalCount(0);
          }else{
              PageSearch result = skuPublishManager.getPublishProductSkuByParam(pageProduct);
              pageProduct.setList(result.getList());
              pageProduct.setTotalCount(result.getTotalCount());
          }
          request.setAttribute("pageProduct", pageProduct);
          //套餐
          if(StringUtils.isNotBlank(queryKeywords)&&searchPackageIds.length()>0){
              pagePackage.getFilters().add(new PropertyFilter(null,"EQS_searchPackageIds",searchPackageIds.toString()));
          }else if(StringUtils.isNotBlank(queryKeywords)&&searchPackageIds.length()==0){
              pagePackage.getFilters().add(new PropertyFilter(null,"EQS_searchPackageIds","-99999"));
          }
          if(StringUtils.isNotBlank(queryKeywords)&&searchPackageIds.equals("")){
              pagePackage.setList(new ArrayList());
              pagePackage.setTotalCount(0);
          }else{
              PageSearch resultPack = welfarePackageManager.getPublishPackage(pagePackage);
              pagePackage.setList(resultPack.getList());
              pagePackage.setTotalCount(resultPack.getTotalCount());
          }

          request.setAttribute("pagePackage", pagePackage);
          //其他热卖商品
          pageProductOther.getFilters().add(new PropertyFilter(Product.class.getName(),"EQS_positionCode","04_31"));
          PageSearch resultOther = skuPublishManager.getRecommendProductSkuByParam(pageProductOther);
          pageProductOther.setList(resultOther.getList());
          pageProductOther.setTotalCount(resultOther.getTotalCount());
          request.setAttribute("pageProductOther", pageProductOther);

          //福利标签条件查询
          List<WelfareItem> welfareItems = getThirdWelfare(12);
          pageProduct.getFilters().add(new PropertyFilter(null,"EQL_brandCount","12"));
          pageProduct.setPageSize(Integer.MAX_VALUE);
          List<Brand> brands = brandManager.getPublishBrands(pageProduct);
          request.setAttribute("welfareItems", welfareItems);
          request.setAttribute("brands", brands);
          pageProduct.setPageSize(20);
        return BASE_DIR+"search";
    }
    @RequestMapping("/subject/{positionId}")
    public String subject(HttpServletRequest request,HttpServletResponse response,@PathVariable Long positionId){
    	String areaCode  = (String)request.getSession().getAttribute("areaCode");
    	AdvertCategory ac = advertCategoryManager.getByObjectId(positionId);
        String positionCode = ac.getPositionCode();
        //banner
        List<Advert> banner = advertManager.getAdvertByParams(positionCode, areaCode, 3);
        request.setAttribute("banner", banner);
        int bannerNum  = banner.size();
        request.setAttribute("bannerNum", bannerNum);
        PageSearch pageSearch = new PageSearch();
        //得到与类型有关的套餐
        Dictionary dictionary = dictionaryManager.getDictionaryByDictionaryIdAndValue(1121, positionId.intValue());
        Integer welfareType = Integer.parseInt(dictionary.getRemark());
        if("27".equals(positionId.toString())){//保险
            return insure(request, response, positionId);
        }
        pageSearch.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        pageSearch.getFilters().add(new PropertyFilter(null,"EQI_welfareType",welfareType.toString()));
        //分页大小
        Integer pageSize = Integer.MAX_VALUE;
        String pageSizeStr = request.getParameter("pageSize");
        if(StringUtils.isNotBlank(pageSizeStr)){
            pageSize = Integer.parseInt(pageSizeStr);
        }
        //当前页
        Integer currentPage = 1;
        String currentPageStr = request.getParameter("c_p");
        if(StringUtils.isNotBlank(currentPageStr)){
            currentPage = Integer.parseInt(currentPageStr);
        }
        pageSearch.setPageSize(pageSize);
        pageSearch.setCurrentPage(currentPage);

        PageSearch resultPack = welfarePackageManager.getPublishPackage(pageSearch);
        pageSearch.setList(resultPack.getList());
        pageSearch.setTotalCount(resultPack.getTotalCount());

        request.setAttribute("pagePackage", pageSearch);
        request.setAttribute("subject", positionId);
        return BASE_DIR+"subject";
    }
    @RequestMapping("/cart")
    public String cart(HttpServletRequest request,HttpServletResponse response){
        //分页大小
        Integer pageSize = 10;
        String pageSizeStr = request.getParameter("pageSize");
        if(StringUtils.isNotBlank(pageSizeStr)){
            pageSize = Integer.parseInt(pageSizeStr);
        }
        //当前页
        Integer currentPage = 1;
        String currentPageStr = request.getParameter("currentPage");
        if(StringUtils.isNotBlank(currentPageStr)){
            currentPage = Integer.parseInt(currentPageStr);
        }

        PageSearch page = new PageSearch();
        page.setCurrentPage(currentPage);
        page.setPageSize(pageSize);
        page.setEntityClass(Cart.class);
        page.getFilters().add(new PropertyFilter(Cart.class.getName(),"EQL_userId",FrameworkContextUtils.getCurrentUserId().toString()));
        page.getFilters().add(new PropertyFilter(Cart.class.getName(),"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        PageSearch result = cartManager.getCarts(page);
        List<Cart> list = result.getList();
        for(Cart cart:list){
            Long productId = cart.getProductId();
            CartView cv = new CartView();
            Map<String,Object> param = new HashMap<String,Object>();
            param.put("objectId", productId);
            SkuPublish sku = skuPublishManager.getSkuPublishPrice(FrameworkContextUtils.getCurrentUser().getCompanyId(), productId);
            ProductPublish p = sku.getProduct();
            if(p!=null){
                cv.setMainPicture(p.getMainPicture());
            }
            cv.setName(sku.getName());
            cv.setSellPrice(sku.getSellPrice());
            cv.setCheckStatus(sku.getCheckStatus());
            Attribute attribute1 = null;
            Attribute attribute2 = null;
            if(sku.getAttributeId1()!=null){
                AttributeValue value1 = attributeValueManager.getByObjectId(sku.getAttributeId1());
                if(value1!=null){
                    attribute1 = attributeManager.getByObjectId(value1.getAttributeId());
                    attribute1.setAttributeValue(sku.getAttributeValue1());
                }
            }
            if(sku.getAttributeId2()!=null){
                AttributeValue value2 = attributeValueManager.getByObjectId(sku.getAttributeId2());
                if(value2!=null){
                    attribute2 = attributeManager.getByObjectId(value2.getAttributeId());
                    attribute2.setAttributeValue(sku.getAttributeValue2());
                }
            }
            cv.setAttribute1(attribute1);
            cv.setAttribute2(attribute2);
            cart.setCartView(cv);
        }
        page.setList(list);
        page.setTotalCount(result.getTotalCount());
        request.setAttribute("pageCart", page);
        return BASE_DIR+"cart";
    }
    @RequestMapping("/unfinishedWelfare")
    public String unfinishedWelfare(HttpServletRequest request,HttpServletResponse response){
        return BASE_DIR+"unfinishedWelfare";
    }

    private List<WelfareItem> getThirdWelfare(int count){
      //三级福利
        //得到二级福利
        Map<String,Object> param = new HashMap<String,Object>();
        param.put("itemType", 1);
        param.put("itemGrade", 2);
        param.put("status", 1);
        param.put("count", 10);
        List<WelfareItem> welfareItems = welfareManager.getItemByParam(param);
        return welfareItems;
    }

    private String insure(HttpServletRequest request,HttpServletResponse response, Long positionId){
        PageSearch pageSearch = new PageSearch();
        //得到与类型有关的套餐
        pageSearch.getFilters().add(new PropertyFilter(null,"EQL_companyId",FrameworkContextUtils.getCurrentUser().getCompanyId().toString()));
        pageSearch.getFilters().add(new PropertyFilter(null,"EQL_isInsure","1"));
        //分页大小
        Integer pageSize = 10;
        String pageSizeStr = request.getParameter("pageSize");
        if(StringUtils.isNotBlank(pageSizeStr)){
            pageSize = Integer.parseInt(pageSizeStr);
        }
        //当前页
        Integer currentPage = 1;
        String currentPageStr = request.getParameter("ec_p");
        if(StringUtils.isNotBlank(currentPageStr)){
            currentPage = Integer.parseInt(currentPageStr);
        }
        pageSearch.setPageSize(pageSize);
        pageSearch.setCurrentPage(currentPage);

        PageSearch result = skuPublishManager.getPublishProductSkuByParam(pageSearch);
        pageSearch.setList(result.getList());
        pageSearch.setTotalCount(result.getTotalCount());

        request.setAttribute("pageSearch", pageSearch);
        request.setAttribute("subject", positionId);
        return BASE_DIR+"insure";
    }
}
