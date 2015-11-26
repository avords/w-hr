/**
 * 
 */
package com.handpay.ibenefit.questionnaire.web;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.framework.service.Manager;
import com.handpay.ibenefit.framework.util.DateUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.framework.util.PageSearch;
import com.handpay.ibenefit.framework.util.PageUtils;
import com.handpay.ibenefit.framework.util.PropertyFilter;
import com.handpay.ibenefit.framework.web.PageController;
import com.handpay.ibenefit.info.entity.Infomation;
import com.handpay.ibenefit.questionnaire.entity.Questionnaire;
import com.handpay.ibenefit.questionnaire.entity.Questions;
import com.handpay.ibenefit.questionnaire.service.IQuestionnaireManager;
import com.handpay.ibenefit.questionnaire.service.IQuestionsManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;

/**
 * @ClassName: QuestionnaireController
 * @Description: 问卷调查
 * @author wangshengri
 * @date 2015-6-12 下午4:20:46
 * 
 */
@Controller
@RequestMapping("questionnaire")
public class QuestionnaireController extends PageController<Questionnaire> {
	private static final String BASE_DIR = "questionnaire/";

	@Reference(version = "1.0")
	private IQuestionnaireManager questionnaireManager;
	@Reference(version = "1.0")
	private IQuestionsManager questionsManager;

	@Override
	public Manager<Questionnaire> getEntityManager() {
		return questionnaireManager;
	}

	@Override
	public String getFileBasePath() {
		return BASE_DIR;
	}

	/**
	 * 
	 */
	protected String handlePage(HttpServletRequest request, PageSearch page) {
		User user = FrameworkContextUtils.getCurrentUser();
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		String cp = request.getParameter("c_p");
		Integer currPage = 1;
		if (cp != null && !("").equals(cp)) {
			currPage = Integer.parseInt(cp);
		}
		page = preparePage(request);
		page.setSortOrder("asc");
		page.setSortProperty("createdOn");
		// 查询条件
		page.getFilters().add(new PropertyFilter("Questionnaire", "EQL_deleted", IBSConstants.NOT_DELETE + ""));
		// 部门编号为空的是管理员
		if (IBSConstants.USER_TYPE_COMPANY_HR == user.getType()) {
			page.getFilters().add(new PropertyFilter(Questionnaire.class.getName(), "EQL_createdBy", user.getObjectId().toString()));
		}
		if (companyId != null) {
			page.getFilters().add(new PropertyFilter(Infomation.class.getName(), "EQL_companyId", companyId.toString()));
		}
		page.setCurrentPage(currPage);
		handleFind(page);
		List<Questionnaire> list = page.getList();
		if (list != null && list.size() > 0) {
			for (Questionnaire questionnaire : list) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("questionnaireId", questionnaire.getObjectId());
				Integer countNum = questionnaireManager.getCountNum(map);
				if (countNum == null) {
					countNum = 0;
				}
				questionnaire.setCountNum(countNum.longValue());
				String questionInfo = "";
				List<Questions> questions = questionsManager.getByQuestionnaireId(questionnaire.getObjectId());
				if (questions != null && questions.size() > 0) {
					for (Questions question : questions) {
						String temp = this.accuracy(question.getVoteNum(), countNum, 0);
						questionInfo = questionInfo + question.getQuestionContent() + "||" + temp + ",";
					}
					questionInfo = questionInfo.substring(0, questionInfo.length() - 1);
					questionnaire.setQuestionInfo(questionInfo);
				}
				// questionnaire.setQuestionList(questions);
			}
		}
		afterPage(request, page, PageUtils.IS_NOT_BACK);
		request.setAttribute("action", "page");
		request.setAttribute("pageQust", page);
		return getFileBasePath() + "list" + getActualArgumentType().getSimpleName();
	}

	/**
	 * 
	 * @param request
	 * @param modelMap
	 * @param t
	 * @return
	 * @throws Exception
	 */
	protected String handleSaveToPage(HttpServletRequest request, ModelMap modelMap, Questionnaire t) throws Exception {
		Long companyId = (Long) request.getSession().getAttribute(SecurityConstants.USER_COMPANY_ID);
		User user = FrameworkContextUtils.getCurrentUser();
		String[] prejectContents = request.getParameterValues("prejectContent");
		Date date = new Date();
		if (t.getObjectId() == null) {
			t.setCreatedOn(date);
			t.setCreatedBy(user.getObjectId());
			t.setReleaseDate(t.getStartDate());
			t.setCompanyId(companyId);
			t.setDeleted(t.DELETED_NO);
			t.setUpdatedOn(date);
			t.setUpdatedBy(user.getObjectId());
			if (user.getOrganizationId() != null) {
				t.setDeptId(user.getOrganizationId());
			}
			Date fmtStartDate = DateUtils.getBeginOfTheDate(t.getStartDate());
			Date fmtEndDate = DateUtils.getEndOfTheDate(t.getEndDate());
			t.setStartDate(fmtStartDate);
			t.setEndDate(fmtEndDate);
			if(new Date().after(t.getStartDate())&&(new Date().before(t.getEndDate()))){
				t.setStatus(IBSConstants.QUESTION_VOTE);
			}else{
				t.setStatus(IBSConstants.QUESTION_DRAFT);
			}
			t = save(t);
			if (prejectContents != null) {
				for (String string : prejectContents) {
					Questions question = new Questions();
					question.setQuestionContent(string);
					question.setQuestionnaireId(t.getObjectId());
					question.setVoteNum(0L);
					question.setDeleted(question.DELETED_NO);
					questionsManager.save(question);
				}
			}
		} else {
			Questionnaire quest = questionnaireManager.getByObjectId(t.getObjectId());
			if (quest.getStatus() == IBSConstants.QUESTION_DRAFT) {
				quest.setQuestionTitle(t.getQuestionTitle());
				quest.setSelectSet(t.getSelectSet());
			} else if (quest.getStatus() == IBSConstants.QUESTION_END) {
				quest.setStatus(IBSConstants.QUESTION_VOTE);
			}
			Date fmtStartDate = DateUtils.getBeginOfTheDate(t.getStartDate());
			Date fmtEndDate = DateUtils.getEndOfTheDate(t.getEndDate());
			quest.setStartDate(fmtStartDate);
			quest.setEndDate(fmtEndDate);
//			if(new Date().after(t.getStartDate())&&(new Date().before(t.getEndDate()))){
//				t.setStatus(IBSConstants.QUESTION_VOTE);
//			}else{
//				t.setStatus(IBSConstants.QUESTION_DRAFT);
//			}
			questionnaireManager.save(quest);
			if (quest.getStatus() == IBSConstants.QUESTION_DRAFT) {
				if (prejectContents != null) {
					List<Questions> questions = questionsManager.getByQuestionnaireId(t.getObjectId());
					if (questions != null && questions.size() > 0) {
						for (Questions question : questions) {
							questionsManager.delete(question.getObjectId());
						}
					}
					for (String string : prejectContents) {
						Questions question = new Questions();
						question.setQuestionContent(string);
						question.setQuestionnaireId(t.getObjectId());
						question.setVoteNum(0L);
						question.setDeleted(question.DELETED_NO);
						questionsManager.save(question);
					}
				}
			}
		}

		return "redirect:page" + getMessage("common.base.success", request);
	}

	protected String handleEdit(HttpServletRequest request, HttpServletResponse response, Long objectId) throws Exception {
		if (null != objectId) {
			Object entity = getManager().getByObjectId(objectId);
			List<Questions> questions = questionsManager.getByQuestionnaireId(objectId);
			request.setAttribute("entity", entity);
			request.setAttribute("questions", questions);
		}
		return getFileBasePath() + "edit" + getActualArgumentType().getSimpleName();
	}

	@RequestMapping("/doSetStatus/{objectId}/{status}")
	protected String doSetStatus(HttpServletRequest request, HttpServletResponse response, @PathVariable Long objectId, @PathVariable Long status) {
		boolean ret = questionnaireManager.setStatus(status, objectId);
		if (ret) {
			return "redirect:../../page" + getMessage("common.base.success", request);
		} else {
			return "redirect:../../page" + getMessage("设置状态失败", request);
		}
	}

	protected String handleDelete(HttpServletRequest request, HttpServletResponse response, Long objectId) throws Exception {
		// 不是真正删除
		Questionnaire questionnaire = questionnaireManager.getByObjectId(objectId);
//		questionnaire.setDeleted(questionnaire.DELETED_YES);
//		save(questionnaire);
		questionnaireManager.delete(questionnaire);
		List<Questions> questions = questionsManager.getByQuestionnaireId(objectId);
		if (questions != null && questions.size() > 0) {
			for (Questions question : questions) {
				question.setDeleted(question.DELETED_YES);
				questionsManager.save(question);
			}
		}
		return "redirect:../page" + getMessage("common.base.deleted", request);
	}

	/**
	 * 批量删除数据
	 * 
	 * @param request
	 * @param response
	 * @param objectIds
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/doBatchdelete/{objectIds}")
	protected String doBatchDelete(HttpServletRequest request, HttpServletResponse response, @PathVariable String objectIds) throws Exception {
		String[] arr = objectIds.split(",");
		if (arr != null && arr.length > 0) {
			for (int i = 0; i < arr.length; i++) {
				long objectId = Long.parseLong(arr[i]);
				// 不是真正删除
				Questionnaire questionnaire = questionnaireManager.getByObjectId(objectId);
//				questionnaire.setDeleted(questionnaire.DELETED_YES);
				questionnaireManager.delete(questionnaire);
				List<Questions> questions = questionsManager.getByQuestionnaireId(objectId);
				if (questions != null && questions.size() > 0) {
					for (Questions question : questions) {
						question.setDeleted(question.DELETED_YES);
						questionsManager.save(question);
					}
				}
			}
		}
		return "redirect:../page" + getMessage("common.base.deleted", request);
	}

	/**
	 * 批量删除数据
	 * 
	 * @param request
	 * @param response
	 * @param objectIds
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/doBatchLock/{status}/{objectIds}")
	protected String doBatchLock(HttpServletRequest request, HttpServletResponse response, @PathVariable Long status, @PathVariable String objectIds)
			throws Exception {
		String[] arr = objectIds.split(",");
		if (arr != null && arr.length > 0) {
			for (int i = 0; i < arr.length; i++) {
				long objectId = Long.parseLong(arr[i]);
				questionnaireManager.setStatus(status, objectId);
			}
		}
		return "redirect:../../page" + getMessage("common.base.success", request);
	}

	public static String accuracy(double num, double total, int scale) {
		DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();
		// 可以设置精确几位小数
		df.setMaximumFractionDigits(scale);
		// 模式 例如四舍五入
		df.setRoundingMode(RoundingMode.HALF_UP);
		if (total == 0) {
			return "0%";
		} else {
			double accuracy_num = num / total * 100;
			return df.format(accuracy_num) + "%";
		}

	}
}
