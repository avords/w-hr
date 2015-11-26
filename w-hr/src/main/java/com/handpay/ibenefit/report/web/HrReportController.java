package com.handpay.ibenefit.report.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.BaseController;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.base.file.service.IFileManager;
import com.handpay.ibenefit.framework.util.FileUpDownUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.report.entity.Report;
import com.handpay.ibenefit.report.entity.ReportData;
import com.handpay.ibenefit.report.service.IReportDataManager;
import com.handpay.ibenefit.report.service.IReportManager;

@Controller
@RequestMapping("hrreport")
public class HrReportController extends BaseController{
	
	@Reference(version="1.0")
	private IReportManager reportManager;
	
	@Reference(version = "1.0")
	private IReportDataManager reportDataManager;
	
	@Reference(version = "1.0")
	private IFileManager fileManager;
	
	@RequestMapping("downloadReport/{dataId}")
	public String downloadReport(HttpServletRequest request, HttpServletResponse response, @PathVariable Long dataId) throws Exception {
		ReportData reportData = reportDataManager.getByObjectId(dataId);
		if(reportData!=null){
			if(reportData.getPlatform() == IBSConstants.PLATEFORM_HR 
					&& reportData.getCompanyId().equals(FrameworkContextUtils.getCurrentUser().getCompanyId())){
				String fileName = FileUpDownUtils.encodeDownloadFileName(request, reportData.getReportName());
				FileUpDownUtils.setDownloadResponseHeaders(response, fileName);
				byte[] fileData = fileManager.getFile(reportData.getFilePath());
				response.getOutputStream().write(fileData,0,fileData.length);
				response.getOutputStream().flush();
				response.getOutputStream().close();
			}
		}
		return null;
	}
	
	@RequestMapping("queryReport/{reportId}")
	public String queryReport(HttpServletRequest request, HttpServletResponse response, @PathVariable Long reportId) throws Exception {
		if(reportId!=null){
			Report report = reportManager.getByObjectId(reportId);
			if(report!=null && report.getPlatform() == IBSConstants.PLATEFORM_HR){
				PageSearch pageSearch = PageUtils.preparePage(request, ReportData.class, true);
				pageSearch.getFilters().add(new PropertyFilter("ReportData","EQL_reportId", reportId + ""));
				pageSearch.getFilters().add(new PropertyFilter("ReportData","EQL_companyId", FrameworkContextUtils.getCurrentUser().getCompanyId() + ""));
				pageSearch.getFilters().add(new PropertyFilter("ReportData","EQL_platform", IBSConstants.PLATEFORM_HR + ""));
				pageSearch.setSortOrder("desc");
				pageSearch.setSortProperty("reportDate");
				pageSearch = reportDataManager.find(pageSearch);
				request.setAttribute("pageSearch", pageSearch);
			}else{
				report = new Report();
				report.setObjectId(reportId);
				report.setReportName("报表未定义");
			}
			request.setAttribute("report", report);
		}
		return "report/queryReport";
	}
}
