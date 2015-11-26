package com.handpay.ibenefit;

import java.util.Date;

import com.handpay.ibenefit.framework.entity.AuditEntity;
import com.handpay.ibenefit.framework.entity.ForeverEntity;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;

public final class CRUDUtils {
	
	private CRUDUtils(){
	}
	
	/**
	 * 保存对象准备处理，做一些基本赋值
	 * @param baseEntity
	 */
	public static void prepareSave(AuditEntity baseEntity){
		if(baseEntity.getObjectId()==null){
			baseEntity.setCreatedBy(FrameworkContextUtils.getCurrentUserId());
			baseEntity.setCreatedOn(new Date());
		}else{
			baseEntity.setUpdatedBy(FrameworkContextUtils.getCurrentUserId());
			baseEntity.setUpdatedOn(new Date());
		}
		if(baseEntity instanceof ForeverEntity){
			((ForeverEntity)baseEntity).setDeleted(ForeverEntity.DELETED_NO);
		}
	}
	
	/**
	 * 删除对象预处理
	 * 
	 * @param objectId
	 */
	public static void prepareDelete(ForeverEntity foreverEntity) {
		foreverEntity.setDeleted(ForeverEntity.DELETED_YES);
		foreverEntity.setUpdatedBy(FrameworkContextUtils.getCurrentUserId());
		foreverEntity.setUpdatedOn(new Date());
	}
}
