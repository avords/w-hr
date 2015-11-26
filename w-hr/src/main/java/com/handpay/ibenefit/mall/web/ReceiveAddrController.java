package com.handpay.ibenefit.mall.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.base.area.entity.Area;
import com.handpay.ibenefit.base.area.service.IAreaManager;
import com.handpay.ibenefit.framework.entity.ForeverEntity;
import com.handpay.ibenefit.order.entity.GoodsReceiptAddr;
import com.handpay.ibenefit.order.service.IGoodsReceiptAddrManager;
import com.handpay.ibenefit.security.SecurityConstants;


@Controller
@RequestMapping("/receiveAddr")
public class ReceiveAddrController {

	@Reference(version = "1.0")
	private IGoodsReceiptAddrManager goodsReceiptAddrManager;	//收货地址Manager
	@Reference(version = "1.0")
	private IAreaManager areaManager;				//省份区域manager
	private static final String JSON_VIEW = "jsonView";


	/**
	 *
	 * @Title: list
	 * @Description: 列表方法
	 * @param @param request
	 * @param @param response
	 * @param @return
	 * @return String
	 * @throws
	 * @author 闫冬全
	 */
	@RequestMapping("/loadaddrs")
	 public String loadaddrs(HttpServletRequest request,HttpServletResponse response,ModelMap map){
		Long userId = Long.parseLong((request.getSession().getAttribute(SecurityConstants.USER_ID).toString()));
		List<GoodsReceiptAddr> addrs =  new ArrayList<GoodsReceiptAddr>();
		if(userId != null){
			addrs = goodsReceiptAddrManager.getAddrByUserId(userId);
		}
		map.addAttribute("result", true);
		map.addAttribute("addrs",addrs);
		return JSON_VIEW;
	}

	/**
	 *
	 * @Title: showGoodsReceiptAddr
	 * @Description: TODO 显示收货地址
	 * @param @param request
	 * @param @param addrId
	 * @param @param map
	 * @param @return
	 * @param @throws Exception
	 * @return String
	 * @throws
	 * @author 闫冬全
	 */
	@RequestMapping(value = "/showGoodsReceiptAddr/{addrId}")
	public String showGoodsReceiptAddr(HttpServletRequest request,  @PathVariable Long addrId ,ModelMap map) throws Exception {
		GoodsReceiptAddr receiptAddr = goodsReceiptAddrManager.getByObjectId(addrId);
		map.addAttribute("result", true);
		map.addAttribute("receiptAddr",receiptAddr);
		return JSON_VIEW;
	}

	@RequestMapping(value="/getAreaChildren/{code}")
	public String getChildren(HttpServletRequest request, ModelMap modelMap,@PathVariable String code){
		List<Area> areas =areaManager.getChildren(Long.valueOf(code));
		modelMap.addAttribute("areas",areas);
		return "jsonView";
	}


	@RequestMapping(value="/saveAddr")
	public String saveAddr(HttpServletRequest request, ModelMap modelMap,GoodsReceiptAddr addr){
		Long userId = Long.parseLong((request.getSession().getAttribute(SecurityConstants.USER_ID).toString()));
		addr.setUserId(userId);
		addr.setDeleted(ForeverEntity.DELETED_NO);
		if(addr.getObjectId() == null){
			addr.setCreateDate(new Date());
		}else{
			addr.setUpdateDate(new Date());
		}
		boolean flag  = goodsReceiptAddrManager.saveAddr(addr,userId);
		modelMap.addAttribute("result", flag);
		return "jsonView";
	}

	/**
	 *
	 * @Title: delAddr
	 * @Description: 删除收货地址
	 * @param @param request
	 * @param @param addrId
	 * @param @param map
	 * @param @return
	 * @param @throws Exception
	 * @return String
	 * @throws
	 * @author 闫冬全
	 */
	@RequestMapping(value = "/delAddr/{addrId}")
	public String delAddr(HttpServletRequest request,  @PathVariable Long addrId ,ModelMap map) throws Exception {
		Map<String,Object> resMap = goodsReceiptAddrManager.delAddr(addrId);
		String msg = resMap.get("msg").toString();
		Boolean flag = (Boolean) resMap.get("flag");
		if(!flag && msg.trim().equals("")){
			msg = "删除收货地址失败！";
		}
		map.addAttribute("msg", msg);
		map.addAttribute("flag", flag);
		return JSON_VIEW;
	}


	/**
	 *
	 * @Title: setDefault
	 * @Description: 设置默认收货地址
	 * @param @param request
	 * @param @param addrId
	 * @param @param map
	 * @param @return
	 * @param @throws Exception
	 * @return String
	 * @throws
	 * @author 闫冬全
	 */
	@RequestMapping(value = "/setDefault/{addrId}")
	public String setDefault(HttpServletRequest request,  @PathVariable Long addrId ,ModelMap map) throws Exception {
		Long userId = Long.parseLong((request.getSession().getAttribute(SecurityConstants.USER_ID).toString()));
		boolean res = goodsReceiptAddrManager.setDefault(addrId, userId);
		map.addAttribute("result", res);
		return JSON_VIEW;
	}

}
