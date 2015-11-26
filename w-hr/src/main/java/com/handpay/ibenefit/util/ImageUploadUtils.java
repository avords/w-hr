package com.handpay.ibenefit.util;

import java.awt.image.BufferedImage;

import javax.servlet.http.HttpServletRequest;

import com.handpay.ibenefit.base.file.service.IFileManager;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.ImageUtils;
import com.handpay.ibenefit.framework.util.UploadFile;

public  class  ImageUploadUtils {
	
	//保存Logo图片
	public static String saveLogoImage(HttpServletRequest request,IFileManager fileManager,String parameterName) throws Exception{
		String filePath = null;
		UploadFile uploadFile = FileUpDownUtils.getUploadFile(request,parameterName);
		BufferedImage image = ImageUtils.readImage(uploadFile.getFile().getAbsolutePath());
		if(isImage(image)){
			byte[] fileData = FileUpDownUtils.getFileContent(uploadFile.getFile());
            filePath = fileManager.saveCompanyLogo(fileData, uploadFile.getFileName());
		}
		return filePath;
	}
	//保存证件图片
	public static String saveAttachment(HttpServletRequest request,IFileManager fileManager,String parameterName) throws Exception{
		String filePath = null;
		UploadFile uploadFile = FileUpDownUtils.getUploadFile(request,parameterName);
		BufferedImage image = ImageUtils.readImage(uploadFile.getFile().getAbsolutePath());
		if(isImage(image)){
			byte[] fileData = FileUpDownUtils.getFileContent(uploadFile.getFile());
            filePath = fileManager.saveCompanyLogo(fileData, uploadFile.getFileName());
		}
		return filePath;
	}
	public static Boolean isImage(BufferedImage image){
		int height = image.getHeight();
		int width = image.getWidth();
		if(height > 0 && width >0){
			return true;
		}
		return false;
	}
}
