package com.handpay.ibenefit.portal.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.common.service.IConfigService;
import com.handpay.ibenefit.common.util.DesUtil;
import com.handpay.ibenefit.framework.util.ExcelUtil;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.product.entity.ElectronicCard;
import com.handpay.ibenefit.product.service.IElectronicCardManager;
import com.handpay.ibenefit.security.service.IPointOperateManager;
import com.handpay.ibenefit.virtualCardInfo.VirtualCardInfo;
import com.handpay.ibenefit.virtualCardInfo.service.IVirtualCardInfoManager;


/**
 * 第三方虚拟卡券
 * @author zhliu
 * @date 2015年7月14日
 * @parm
 */
@Controller
@RequestMapping("virtualCardInfo")
public class VirtualCardInfoController {
	@Reference(version="1.0")
	private IPointOperateManager pointOperateManager;
	@Reference(version="1.0")
	private IVirtualCardInfoManager virtualCardInfoManager;
	@Reference(version="1.0")
	private IElectronicCardManager electronicCardManager;
	
	@Reference(version="1.0")
	private IConfigService configService;
	
	
	
	/**
	 * 下载第三方卡密信息
	 * @author zhliu
	 * @date 2015年7月9日
	 * @parm payCode ： 验证码
	 * @param subOrderId ：子订单ID
	 * @param mobile:手机号
	 * @return
	 */
	@RequestMapping("downVirtualCardInfo")
	public String downCardInfo(ModelMap modelMap,HttpServletResponse response,Long subOrderId, String password){
		
		List<Object[]> datas = new ArrayList<Object[]>();
		String[] titles={"卡号","密码"};
		String exportName = "cardInfo.xls";
		VirtualCardInfo virtualCardInfo = new VirtualCardInfo();
		List<ElectronicCard> prodCardList = new ArrayList<ElectronicCard>();
		String msg = "";
		try {
			
			//校验验证码信息
			boolean bool = pointOperateManager.validPayPassword(FrameworkContextUtils.getCurrentUserId(), password);
			if(bool){
				virtualCardInfo.setSubOrderId(subOrderId);
				List<VirtualCardInfo> virtualCardInfos = virtualCardInfoManager.getBySample(virtualCardInfo);
				
				for (VirtualCardInfo virTemp:virtualCardInfos) {
					Object[] arr = new Object[titles.length];
					arr[0] = DesUtil.decrypt(virTemp.getCardNo(), configService.getGameCardKey());
					arr[1] = DesUtil.decrypt(virTemp.getCardPassword(), configService.getGameCardKey());
					datas.add(arr);
				}
				
				
				//OTO卡密信息
				Map<String,Object> map = new HashMap<String,Object>();
				map.put("subOrderId", subOrderId);
				map.put("type", IBSConstants.PRODUCT_STOCK_SOURCE_THIRD_OTO);
				if(subOrderId != null){
					prodCardList = electronicCardManager.selectSubOrderCardInfo(map);
				}
				//OTO卡密信息
				for (ElectronicCard prodCard : prodCardList) {
					Object[] arr = new Object[titles.length];
					if(StringUtils.isNotBlank(prodCard.getCardNo())){
						arr[0] = prodCard.getCardNo();
					}
					if(StringUtils.isNotBlank(prodCard.getCardPassword())){
						arr[1] = prodCard.getCardPassword();
					}
					datas.add(arr);
				}
				
				
				
				ExcelUtil excelUtil = new ExcelUtil();
				excelUtil.exportExcel(response, datas, titles, exportName);
				msg="下载成功";
			}else{
				msg="验证码不正确";
				modelMap.addAttribute("msg",msg);
				return "jsonView";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	
	
	
}
