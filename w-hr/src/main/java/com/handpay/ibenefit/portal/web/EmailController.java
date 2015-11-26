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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.common.service.ISendEmailService;
import com.handpay.ibenefit.common.vo.IBSEmail;
import com.handpay.ibenefit.common.vo.IBSReceiver;
import com.handpay.ibenefit.framework.config.GlobalConfig;
import com.handpay.ibenefit.framework.util.DateUtils;
import com.handpay.ibenefit.framework.util.RandomStringUtil;
import com.handpay.ibenefit.home.entity.ValidateCode;
import com.handpay.ibenefit.home.service.IValidateCodeManager;
import com.handpay.ibenefit.other.entity.MessageTemplate;
import com.handpay.ibenefit.other.service.IMessageTemplateManager;
import com.handpay.ibenefit.security.SecurityUtils;
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
@RequestMapping("/email")
public class EmailController {

	@Reference(version = "1.0", check = false,async=true)
	private ISendEmailService sendEmailService;

	@Reference(version="1.0",check=false)
	private IValidateCodeManager validateCodeManager;

	@Reference(version = "1.0", check = false)
	private IMessageTemplateManager messageTemplateManager;

	@Reference(version="1.0",check=false)
    private IUserManager userManager;

	   @RequestMapping(value = "getRetrieveLoginPwdCode", method = RequestMethod.POST)
	    public String getRetrieveLoginPwdCode(HttpServletRequest request, ModelMap modelMap)
	            throws Exception {
	       String userId = request.getParameter("userId");
           String email = request.getParameter("email");
           if(StringUtils.isNotBlank(userId)&&StringUtils.isNotBlank(email)){
//	            ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(email,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
//	            if(history_validateCode==null){
	            validateCodeManager.delete(email,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
	                MessageTemplate messageTemplate = messageTemplateManager.getTemplateByCode(IBSConstants.EMAIL_RETRIEVE_LOGIN_PASSWORD);
	                if (messageTemplate != null) {
	                    try {
	                        String code = RandomStringUtil.createRandom(true, 6);
	                         Map<String,Object> map = new HashMap<String,Object>();
	                         User user = userManager.getByObjectId(Long.parseLong(userId));
	                         map.put("userName", user.getUserName());
                             SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                             map.put("date", sdf.format(new Date()));
	                         map.put("code", code);
	                         String content = FreemarkerTemplateUtils.getContent(messageTemplate.getMessageContent(), map);
	                         if(StringUtils.isNotBlank(content)){
	                             IBSEmail ie = new IBSEmail();
	                             ie.setBody(content);
	                             ie.setBodyIsHtml(true);
	                             ie.setSubject("");
	                             IBSReceiver ir = new IBSReceiver();
	                             ir.setEmail(email);
	                             sendEmailService.sendOne(ie, messageTemplate.getMessageTitle(), ir);
	                         }
	                        ValidateCode validateCode = new ValidateCode();
	                        validateCode.setCode(code);
	                        validateCode.setMobile(email);
	                        validateCode.setType(IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
	                        validateCode.setValidateTime(DateUtils.getPreviousOrNextSecondsOfDate(new Date(),60*60*24));
	                        validateCode.setUserId(Long.parseLong(userId));
	                        validateCodeManager.save(validateCode);
                            modelMap.put("result", true);
                            modelMap.put("message", "邮件已发送");
	                    } catch (Exception e) {
	                        modelMap.put("message", "获取验证码失败");
	                        modelMap.put("result", false);
	                    }
	                } else {
	                    modelMap.put("message", "邮件模版配置异常");
	                    modelMap.put("result", false);
	                }
//	            }else{
//	                modelMap.put("message", "邮件已发送");
//	                modelMap.put("result", true);
//	            }
	        }else{
	            modelMap.put("message", "邮件发送失败");
                modelMap.put("result", false);
	        }
	        return "jsonView";
	    }

	   @RequestMapping(value = "getRetrievePaymentPwdCode", method = RequestMethod.POST)
       public String getRetrievePaymentPwdCode(HttpServletRequest request, ModelMap modelMap)
               throws Exception {
	       String userId = request.getParameter("userId");
           String email = request.getParameter("email");
           if(StringUtils.isNotBlank(userId)&&StringUtils.isNotBlank(email)){
//               ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(email,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
//               if(history_validateCode==null){
                 validateCodeManager.delete(email,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
                   MessageTemplate messageTemplate = messageTemplateManager.getTemplateByCode(IBSConstants.EMAIL_RETRIEVE_PAYMENT_PASSWORD);
                   if (messageTemplate != null) {
                       try {
                           String code = RandomStringUtil.createRandom(true, 6);
                           code = SecurityUtils.generatePassword(code);
                           Map<String,Object> map = new HashMap<String,Object>();
                           String url = GlobalConfig.getSecureDomain()+"/findPaymentPwd/resetPasswordByEmail?code="+code;
                           User user = userManager.getByObjectId(Long.parseLong(userId));
                           map.put("userName", user.getUserName());
                           SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
                           map.put("date", sdf.format(new Date()));
                           map.put("url", url);
                           String content = FreemarkerTemplateUtils.getContent(messageTemplate.getMessageContent(), map);
                           String result = "";
                           if(StringUtils.isNotBlank(content)){
                               IBSEmail ie = new IBSEmail();
                               ie.setBody(content);
                               ie.setBodyIsHtml(true);
                               ie.setSubject("");
                               IBSReceiver ir = new IBSReceiver();
                               ir.setEmail(email);
                               result = sendEmailService.sendOne(ie, messageTemplate.getMessageTitle(), ir);
                           }
                           ValidateCode validateCode = new ValidateCode();
                           validateCode.setCode(code);
                           validateCode.setMobile(email);
                           validateCode.setType(IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
                           validateCode.setValidateTime(DateUtils.getPreviousOrNextSecondsOfDate(new Date(),60*60*24));
                           validateCode.setUserId(Long.parseLong(userId));
                           validateCodeManager.save(validateCode);
                           if("success".equals(result)){
                               modelMap.put("result", true);
                               modelMap.put("message", "邮件已发送");
                           }else{
                               modelMap.put("result", false);
                               modelMap.put("message", "邮件发送失败");
                           }
                       } catch (Exception e) {
                           modelMap.put("message", "获取验证码失败");
                           modelMap.put("result", false);
                       }
                   } else {
                       modelMap.put("message", "邮件模版配置异常");
                       modelMap.put("result", false);
                   }
//               }else{
//                   modelMap.put("message", "邮件已发送");
//                   modelMap.put("result", true);
//               }
           }else{
               modelMap.put("message", "邮件发送失败");
               modelMap.put("result", false);
           }
           return "jsonView";
       }
}
