package com.handpay.ibenefit.system.web;

import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.common.service.ISendSmsService;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.framework.util.FrameworkContextUtils;
import com.handpay.ibenefit.home.entity.ValidateCode;
import com.handpay.ibenefit.home.service.IValidateCodeManager;
import com.handpay.ibenefit.security.SecurityUtils;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;

@Controller
@RequestMapping("/findPaymentPwd")
public class FindPaymentPwdController {
    private static final Logger LOGGER = Logger.getLogger(FindPaymentPwdController.class);

    private static final String BASE_DIR = "findPwd/";
    @Reference(version="1.0",async=true)
    private ISendSmsService sendSmsService;
    @Reference(version="1.0")
    private IUserManager userManager;
    @Reference(version="1.0")
    private IValidateCodeManager validateCodeManager;

    @RequestMapping("/validateCode")
    public String validateCode(HttpServletRequest request,HttpServletResponse response,ModelMap map){
        String code = request.getParameter("code");
        String mobilePhone = FrameworkContextUtils.getCurrentUser().getMobilePhone();
        ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(mobilePhone,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
        if(history_validateCode!=null&&history_validateCode.getCode().equals(code)){
            //验证通过
            request.getSession().setAttribute("payment_verify_userId", history_validateCode.getUserId());
            validateCodeManager.delete(mobilePhone,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
            map.put("result", true);
        }else{
            map.put("result", false);
            map.put("message", "验证码错误");
        }
        try {
            AjaxUtils.doJsonpResponseOfMap(response,request.getParameter("callback"),map);
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return null;
    }
    @RequestMapping("/getPaymentCode")
    public String getPaymentCode(HttpServletRequest request,HttpServletResponse response,ModelMap map){
        User user = FrameworkContextUtils.getCurrentUser();
        if(user!=null){
            return "forward:/sms/getRetrievePaymentPwdCode/"+user.getMobilePhone()+"?userId="+user.getObjectId();
        }else{
            map.addAttribute("result",false);
            map.addAttribute("message","发送短信异常");
            try {
                AjaxUtils.doJsonpResponseOfMap(response,request.getParameter("callback"), map);
            } catch (Exception e) {
                LOGGER.error(e);
            }
            return null;
        }
    }
    @RequestMapping("/resetPwd")
    public String resetPwd(HttpServletRequest request,HttpServletResponse response,ModelMap map){
        String password = request.getParameter("password");
        Long userId = (Long) request.getSession().getAttribute("payment_verify_userId");
        if(userId == null||StringUtils.isBlank(password)){
            map.addAttribute("result",false);
            map.addAttribute("message","密码重置失败");
        }else{
            User user = userManager.getByObjectId(userId);
            user.setPayPassword(SecurityUtils.generatePassword(password));
            userManager.save(user);
            map.addAttribute("result",true);
            map.addAttribute("message","密码重置成功");
        }
        try {
        	AjaxUtils.doJsonpResponseOfMap(response,request.getParameter("callback"),map);
        } catch (Exception e) {
            LOGGER.error(e);
        }
        return null;
    }

    private void setMessage(HttpServletRequest request){
        String message = request.getParameter("message");
        if(StringUtils.isNotBlank(message)){
            request.setAttribute("message", message);
        }
    }

    @RequestMapping("verifyIdentity")
    public String verifyIdentity(HttpServletRequest request,HttpServletResponse response){
        setMessage(request);
        User user = FrameworkContextUtils.getCurrentUser();
        request.setAttribute("email", user.getEmail());
        request.setAttribute("mobilePhone", user.getMobilePhone());
        request.getSession().setAttribute("payment_verify_email", user.getEmail());
        request.getSession().setAttribute("payment_verify_mobilePhone", user.getMobilePhone());
        request.setAttribute("loginName", user.getLoginName());
        return BASE_DIR+"paymentPwdVerifyIdentity";
    }
    @RequestMapping("resetPassword")
    public String resetPassword(HttpServletRequest request,HttpServletResponse response) throws Exception{
        String code = request.getParameter("code");
        String mobilePhone = (String) request.getSession().getAttribute("payment_verify_mobilePhone");
        ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(mobilePhone,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
        if(history_validateCode!=null&&history_validateCode.getCode().equals(code)){
            //验证通过
            request.getSession().setAttribute("payment_verify_userId", history_validateCode.getUserId());
            validateCodeManager.delete(mobilePhone,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
            return BASE_DIR+"paymentPwdResetPwd";
        }
        return "forward:/findPaymentPwd/verifyIdentity?message="+URLEncoder.encode("验证码不正确", "UTF-8");
    }
    @RequestMapping("resetPasswordByEmail")
    public String resetPasswordByEmail(HttpServletRequest request,HttpServletResponse response) throws Exception{
        String code = request.getParameter("code");
        String mobilePhone = (String) request.getSession().getAttribute("payment_verify_email");
        ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(mobilePhone,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
        if(history_validateCode!=null&&history_validateCode.getCode().equals(code)){
            //验证通过
            request.getSession().setAttribute("payment_verify_userId", history_validateCode.getUserId());
            validateCodeManager.delete(mobilePhone,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_PAYMENT_PASSWORD);
            return BASE_DIR+"paymentPwdResetPwd";
        }
        return "forward:/findPaymentPwd/verifyIdentity?message="+URLEncoder.encode("链接已失效", "UTF-8");
    }
    @RequestMapping("success")
    public String success(HttpServletRequest request,HttpServletResponse response){
        Long userId = (Long) request.getSession().getAttribute("payment_verify_userId");
        String password = request.getParameter("password");
        if(userId!=null){
            User user = userManager.getByObjectId(userId);
            user.setPayPassword(SecurityUtils.generatePassword(password));
            userManager.save(user);
            return BASE_DIR+"paymentPwdSuccess";
        }
        return null;
    }

    @RequestMapping("/sendEmail")
    public String sendEmail(HttpServletRequest request,HttpServletResponse response,ModelMap map){
        User user = FrameworkContextUtils.getCurrentUser();
        if(user!=null){
            return "forward:/email/getRetrievePaymentPwdCode?userId="+user.getObjectId()+"&email="+user.getEmail();
        }else{
            map.addAttribute("result",false);
            map.addAttribute("message","发送邮件异常");
            return "jsonView";
        }
    }
}
