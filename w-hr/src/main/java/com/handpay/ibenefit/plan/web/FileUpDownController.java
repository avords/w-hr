package com.handpay.ibenefit.plan.web;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.base.file.service.IFileManager;
@Controller
@RequestMapping("/fileUpDown")
public class FileUpDownController {
    @Reference(version = "1.0")
    private IFileManager fileManager;
    @RequestMapping("/downloadFile")
    public String downloadFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String filePath = request.getParameter("filePath");
        String fileName = request.getParameter("fileName");
        if(StringUtils.isBlank(fileName)){
            fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        }
        response.setContentType("APPLICATION/OCTET-STREAM");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        byte[] fileData = fileManager.getFile(filePath);
        OutputStream out = response.getOutputStream();
        out.write(fileData);
        out.flush();
        out.close();
        return null;
    }
}
