/**
 * @Title: SmsController.java
 * @Package com.handpay.ibenefit.home.web
 * @Description: TODO
 * Copyright: Copyright (c) 2011
 *
 * @author Mac.Yoon
 * @date 2015-6-22 上午10:03:50
 * @version V1.0
 */

package com.handpay.ibenefit.portal.web;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.common.service.ISendEmailService;
import com.handpay.ibenefit.common.service.ISendSmsService;
import com.handpay.ibenefit.common.vo.IBSEmail;
import com.handpay.ibenefit.common.vo.IBSReceiver;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.DateUtils;
import com.handpay.ibenefit.framework.util.FreemarkerUtils;
import com.handpay.ibenefit.framework.util.RandomStringUtil;
import com.handpay.ibenefit.home.entity.ValidateCode;
import com.handpay.ibenefit.home.service.IValidateCodeManager;
import com.handpay.ibenefit.other.entity.MessageTemplate;
import com.handpay.ibenefit.other.service.IMessageTemplateManager;
import com.handpay.ibenefit.security.SecurityConstants;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;
import com.handpay.ibenefit.system.web.FreemarkerTemplateUtils;

/**
 * @ClassName: SmsController
 * @Description: TODO
 * @author Mac.Yoon
 * @date 2015-6-22 上午10:03:50
 *
 */
@Controller
@RequestMapping("sms")
public class SmsController {

	@Reference(version = "1.0", check = false,async=true)
	private ISendSmsService sendSmsService;

	@Reference(version="1.0",check=false)
	private IValidateCodeManager validateCodeManager;

	@Reference(version = "1.0", check = false)
	private IMessageTemplateManager messageTemplateManager;
	
	@Reference(version = "1.0", check = false)
	private IUserManager userManager;
	
	@Reference(version = "1.0", check = false,async=true)
	private ISendEmailService sendEmailService;
	

	@RequestMapping(value = "getBindValidateCode/{mobile}")
	public String getBindValidateCode(HttpServletRequest request, ModelMap modelMap,@PathVariable String mobile)
			throws Exception {
		validateCodeManager.delete(mobile,IBSConstants.VALIDATE_CODE_TYPE_BIND_MOBILE);
//		if(history_validateCode==null){
			MessageTemplate messageTemplate = messageTemplateManager.getTemplateByCode("MYG_005");
			if (messageTemplate != null) {
				try {
					String code = RandomStringUtil.createRandom(true, 6);
					Map<String, Object> model = new HashMap<String, Object>();
					model.put("code", code);
					model.put("mobile", mobile);
					String cotent = FreemarkerUtils.parseTemplate(messageTemplate.getMessageContent(),model);
					sendSmsService.send(mobile,cotent,code);
					ValidateCode validateCode = new ValidateCode();
					validateCode.setCode(code);
					validateCode.setMobile(mobile);
					validateCode.setType(IBSConstants.VALIDATE_CODE_TYPE_BIND_MOBILE);
					validateCode.setValidateTime(DateUtils.getPreviousOrNextSecondsOfDate(new Date(),60*10));
					validateCodeManager.save(validateCode);
					modelMap.put("message", "短信验证码已发送");
					modelMap.put("result", true);
				} catch (Exception e) {
					modelMap.put("message", "获取验证码失败");
					modelMap.put("result", false);
				}
			} else {
				modelMap.put("message", "短信模版配置异常");
				modelMap.put("result", false);
			}
//		}else{
//			modelMap.put("message", "短信验证码已发送");
//			modelMap.put("result", true);
//		}
		return "jsonView";
	}

	@RequestMapping(value = "getUpdateValidateCode/{mobile}")
	public String getUpdateValidateCode(HttpServletRequest request, ModelMap modelMap,@PathVariable String mobile)
			throws Exception {
		validateCodeManager.delete(mobile,IBSConstants.VALIDATE_CODE_TYPE_UPDATE_MOBILE);
//		ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(mobile,IBSConstants.VALIDATE_CODE_TYPE_UPDATE_MOBILE);
//		if(history_validateCode==null){
			MessageTemplate messageTemplate = messageTemplateManager.getTemplateByCode("MYG_005");
			if (messageTemplate != null) {
				try {
					String code = RandomStringUtil.createRandom(true, 6);
					Map<String, Object> model = new HashMap<String, Object>();
					model.put("code", code);
					model.put("mobile", mobile);
					String cotent = FreemarkerUtils.parseTemplate(messageTemplate.getMessageContent(),model);
					sendSmsService.send(mobile,cotent,code);
					ValidateCode validateCode = new ValidateCode();
					validateCode.setCode(code);
					validateCode.setMobile(mobile);
					validateCode.setType(IBSConstants.VALIDATE_CODE_TYPE_UPDATE_MOBILE);
					validateCode.setValidateTime(DateUtils.getPreviousOrNextSecondsOfDate(new Date(),60*10));
					validateCodeManager.save(validateCode);
					modelMap.put("message", "短信验证码已发送");
					modelMap.put("result", true);
				} catch (Exception e) {
					modelMap.put("message", "获取验证码失败");
					modelMap.put("result", false);
				}
			} else {
				modelMap.put("message", "短信模版配置异常");
				modelMap.put("result", false);
			}
//		}else{
//			modelMap.put("message", "短信验证码已发送");
//			modelMap.put("result", true);
//		}
		return "jsonView";
	}

	@RequestMapping(value = "getPayCodeValidateCode", method = RequestMethod.POST)
	public String getPayCodeValidateCode(HttpServletRequest request, ModelMap modelMap)
			throws Exception {
		User sessionUser = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
		User localUser = userManager.getByObjectId(sessionUser.getObjectId());
		if(localUser.getMobilePhone()!=null){
			validateCodeManager.delete(localUser.getMobilePhone(),IBSConstants.VALIDATE_CODE_TYPE_UPDATE_PAY_PASSWORD);
			MessageTemplate messageTemplate = messageTemplateManager.getTemplateByCode("MYG_014");
			if (messageTemplate != null) {
				try {
					String code = RandomStringUtil.createRandom(true, 6);
					Map<String, Object> model = new HashMap<String, Object>();
					model.put("code", code);
					model.put("mobile", localUser.getMobilePhone());
					String cotent = FreemarkerUtils.parseTemplate(messageTemplate.getMessageContent(),model);
					sendSmsService.send(localUser.getMobilePhone(),cotent,code);
					ValidateCode validateCode = new ValidateCode();
					validateCode.setCode(code);
					validateCode.setMobile(localUser.getMobilePhone());
					validateCode.setType(IBSConstants.VALIDATE_CODE_TYPE_UPDATE_PAY_PASSWORD);
					validateCode.setValidateTime(DateUtils.getPreviousOrNextSecondsOfDate(new Date(),60*10));
					validateCodeManager.save(validateCode);
					modelMap.put("message", "短信验证码已发送");
					modelMap.put("result", true);
				} catch (Exception e) {
					modelMap.put("message", "获取验证码失败");
					modelMap.put("result", false);
				}
			} else {
				modelMap.put("message", "短信模版配置异常");
				modelMap.put("result", false);
			}
		}else{
			modelMap.put("message", "请绑定手机");
			modelMap.put("result", false);
		}
		return "jsonView";
	}

	   @RequestMapping(value = "getRetrieveLoginPwdCode/{mobile}")
	    public String getRetrieveLoginPwdCode(HttpServletRequest request, ModelMap modelMap,@PathVariable String mobile)
	            throws Exception {
		   
	        String userId = request.getParameter("userId");
	        if(StringUtils.isNotBlank(userId)){
	        	validateCodeManager.delete(mobile,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
//	            ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(mobile,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
//	            if(history_validateCode==null){
	                MessageTemplate messageTemplate = messageTemplateManager.getTemplateByCode(IBSConstants.SMS_MOBILE_RETRIEVE_LOGIN_PASSWORD);
	                if (messageTemplate != null) {
	                    try {
	                        String code = RandomStringUtil.createRandom(true, 6);
	                         Map<String,Object> map = new HashMap<String,Object>();
	                         map.put("code", code);
	                         String content = FreemarkerTemplateUtils.getContent(messageTemplate.getMessageContent(), map);
	                         if(StringUtils.isNotBlank(content)){
	                             sendSmsService.send(mobile, content);
	                         }
	                        ValidateCode validateCode = new ValidateCode();
	                        validateCode.setCode(code);
	                        validateCode.setMobile(mobile);
	                        validateCode.setType(IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
	                        validateCode.setValidateTime(DateUtils.getPreviousOrNextSecondsOfDate(new Date(),60*10));
	                        validateCode.setUserId(Long.parseLong(userId));
	                        validateCodeManager.save(validateCode);
	                        modelMap.put("message", "短信验证码已发送");
	                        modelMap.put("result", true);
	                    } catch (Exception e) {
	                        modelMap.put("message", "获取验证码失败");
	                        modelMap.put("result", false);
	                    }
	                } else {
	                    modelMap.put("message", "短信模版配置异常");
	                    modelMap.put("result", false);
	                }
//	            }else{
//	                modelMap.put("message", "短信验证码已发送");
//	                modelMap.put("result", true);
//	            }
	        }else{
	            modelMap.put("message", "验证码发送失败");
                modelMap.put("result", false);
	        }
	        return "jsonView";
	    }

	   @RequestMapping(value = "getRetrievePaymentPwdCode/{mobile}")
       public String getRetrievePaymentPwdCode(HttpServletRequest request, HttpServletResponse response, ModelMap modelMap,@PathVariable String mobile)
               throws Exception {
           String userId = request.getParameter("userId");
           if(StringUtils.isNotBlank(userId)){
			validateCodeManager.delete(mobile, IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
			MessageTemplate messageTemplate = messageTemplateManager.getTemplateByCode(IBSConstants.SMS_MOBILE_RETRIEVE_PAYMENT_PASSWORD);
			if (messageTemplate != null) {
				try {
					String code = RandomStringUtil.createRandom(true, 6);
					Map<String, Object> map = new HashMap<String, Object>();
					map.put("code", code);
					String content = FreemarkerTemplateUtils.getContent(messageTemplate.getMessageContent(), map);
					if (StringUtils.isNotBlank(content)) {
						sendSmsService.send(mobile, content);
					}
					ValidateCode validateCode = new ValidateCode();
					validateCode.setCode(code);
					validateCode.setMobile(mobile);
					validateCode.setType(IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
					validateCode.setValidateTime(DateUtils.getPreviousOrNextSecondsOfDate(new Date(), 60 * 10));
					validateCode.setUserId(Long.parseLong(userId));
					validateCodeManager.save(validateCode);
					modelMap.put("message", "短信验证码已发送");
					modelMap.put("result", true);
				} catch (Exception e) {
					modelMap.put("message", "获取验证码失败");
					modelMap.put("result", false);
				}
			} else {
				modelMap.put("message", "短信模版配置异常");
				modelMap.put("result", false);
			}
           }else{
               modelMap.put("message", "验证码发送失败");
               modelMap.put("result", false);
           }
           AjaxUtils.doJsonpResponseOfMap(response, request.getParameter("callback"), modelMap);
           return null;
       }
	   
	   @RequestMapping(value = "getBindEmailValidateCode/{email}")
		public String getBindEmailValidateCode(HttpServletRequest request, ModelMap modelMap,
				@PathVariable String email)
				throws Exception {
		   email = request.getParameter("email");
			validateCodeManager.delete(email,IBSConstants.VALIDATE_CODE_TYPE_UPDATE_EMAIL);
			try {
				User user = (User) request.getSession().getAttribute(SecurityConstants.SESSION_USER);
				ValidateCode validateCode = new ValidateCode();
				String code = RandomStringUtil.createRandom(true, 6);
				validateCode.setCode(code);
				validateCode.setValidateTime(DateUtils.getPreviousOrNextSecondsOfDate(new Date(), 60 * 60 * 3));
				validateCode.setUserId(user.getObjectId());
				validateCode.setType(IBSConstants.VALIDATE_CODE_TYPE_UPDATE_EMAIL);
				validateCode.setMobile(email);
				validateCodeManager.save(validateCode);
				IBSReceiver ibsReceiver = new IBSReceiver();
				ibsReceiver.setEmail(email);
				MessageTemplate messageTemplate = messageTemplateManager.getTemplateByCode("EYG_011");
				Map<String, Object> model = new HashMap<String, Object>();
				model.put("code", code);
				model.put("userName", user.getUserName());
				String cotent = FreemarkerUtils.parseTemplate(messageTemplate.getMessageContent(), model);
				IBSEmail ibsEmail = new IBSEmail();
				ibsEmail.setBody(cotent);
				ibsEmail.setBodyIsHtml(true);
				sendEmailService.sendOne(ibsEmail, messageTemplate.getMessageTitle(), ibsReceiver);
				modelMap.put("result", true);
			} catch (Exception e) {
				e.printStackTrace();
				modelMap.put("message", "系统内部异常");
			}
			return "jsonView";
		}
	   
}
