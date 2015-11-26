/**
 * @Title: HomePackageView.java
 * @Package com.handpay.ibenefit.home.web
 * @Description: TODO
 * Copyright: Copyright (c) 2011 
 * 
 * @author Mac.Yoon
 * @date 2015-6-16 下午3:10:20
 * @version V1.0
 */

package com.handpay.ibenefit.home.web;

import java.util.List;

import com.handpay.ibenefit.welfare.entity.WelfareItem;

/**
 * @ClassName: HomePackageView
 * @Description: TODO
 * @author Mac.Yoon
 * @date 2015-6-16 下午3:10:20
 *
 */

public class HomePackageView {
	private String itemName;
	private Long itemId;
	private List<WelfareItem> items;
	public String getItemName() {
		return itemName;
	}
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	public Long getItemId() {
		return itemId;
	}
	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}
	public List<WelfareItem> getItems() {
		return items;
	}
	public void setItems(List<WelfareItem> items) {
		this.items = items;
	}
}
