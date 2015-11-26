package com.handpay.ibenefit.system.web;

import java.net.URLEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.IBSConstants;
import com.handpay.ibenefit.common.service.ISendSmsService;
import com.handpay.ibenefit.home.entity.ValidateCode;
import com.handpay.ibenefit.home.service.IValidateCodeManager;
import com.handpay.ibenefit.security.SecurityUtils;
import com.handpay.ibenefit.security.entity.User;
import com.handpay.ibenefit.security.service.IUserManager;

@Controller
@RequestMapping("/findLoginPwd")
public class FindLoginPwdController {
    private static final String BASE_DIR = "findPwd/";
    @Reference(version="1.0",async=true)
    private ISendSmsService sendSmsService;
    @Reference(version="1.0")
    private IUserManager userManager;
    @Reference(version="1.0")
    private IValidateCodeManager validateCodeManager;

    private void setMessage(HttpServletRequest request){
        String message = request.getParameter("message");
        if(StringUtils.isNotBlank(message)){
            request.setAttribute("message", message);
        }
    }

    @RequestMapping("index")
    public String index(HttpServletRequest request,HttpServletResponse response){
        setMessage(request);
        return BASE_DIR+"loginPwdIndex";
    }
    @RequestMapping("verifyIdentity")
    public String verifyIdentity(HttpServletRequest request,HttpServletResponse response) throws Exception{
        String loginName = request.getParameter("loginName");
        setMessage(request);      
        User user = userManager.getUserByLoginNameAndPlatform(loginName, IBSConstants.PLATEFORM_HR);
        if(StringUtils.isBlank(loginName)||user==null){
          // return "redirect:"+GlobalConfig.getSecureDomain()+"/findLoginPwd/index?message="+URLEncoder.encode("账户不存在", "UTF-8");
           return "forward:/findLoginPwd/index?message="+URLEncoder.encode("账户不存在", "UTF-8");
        }else{
			String mobile = user.getMobilePhone();
			String email = user.getEmail();
			if(mobile!=null&&!"".equals(mobile)&&mobile.length()>7){
				mobile = mobile.replace(mobile.substring(3,7),"****");
				request.setAttribute("viewUserMobile", mobile);
				request.setAttribute("mobilePhone", user.getMobilePhone());
				request.setAttribute("mobileShow", true);
			}
			if(email!=null&&!"".equals(email)){
				request.setAttribute("email", email);
				request.setAttribute("emailShow", true);
			}
        }
        
        request.getSession().setAttribute("login_verify_email", user.getEmail());
        request.getSession().setAttribute("login_verify_mobilePhone", user.getMobilePhone());
        request.setAttribute("loginName", loginName);
        return BASE_DIR+"loginPwdVerifyIdentity";
    }
    @RequestMapping("resetPwd")
    public String resetPwd(HttpServletRequest request,HttpServletResponse response) throws Exception{
        String loginName = request.getParameter("loginName");
        String type = request.getParameter("type");
        User user = userManager.getUserByLoginNameAndPlatform(loginName, IBSConstants.PLATEFORM_HR);
        if("mobile".equals(type)){
        	String code = request.getParameter("code");
        	 ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(user.getMobilePhone(),IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
             if(history_validateCode!=null&&history_validateCode.getCode().equals(code)){
                 //验证通过
                 request.getSession().setAttribute("login_verify_userId", history_validateCode.getUserId());
                 validateCodeManager.delete(user.getMobilePhone(),IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
                 return BASE_DIR+"loginPwdResetPwd";
             }
        }else if("mail".equals(type)){
        	String code = request.getParameter("emailcode");
        	 ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(user.getEmail(),IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
             if(history_validateCode!=null&&history_validateCode.getCode().equals(code)){
                 //验证通过
                 request.getSession().setAttribute("login_verify_userId", history_validateCode.getUserId());
                 validateCodeManager.delete(user.getEmail(),IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
                 return BASE_DIR+"loginPwdResetPwd";
             }
        }
       
        return "forward:/findLoginPwd/verifyIdentity?loginName="+loginName+"&message="+URLEncoder.encode("验证码错误", "UTF-8");
    }
    @RequestMapping("resetPwdByEmail")
    public String resetPwdByEmail(HttpServletRequest request,HttpServletResponse response) throws Exception{
        String code = request.getParameter("code");
        String loginName = request.getParameter("loginName");
        User user = userManager.getUserByLoginNameAndPlatform(loginName, IBSConstants.PLATEFORM_HR);
        String email = user.getEmail();
        ValidateCode history_validateCode = validateCodeManager.getValidateCodeByEffective(email,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
        if(history_validateCode!=null&&history_validateCode.getCode().equals(code)){
            //验证通过
            request.getSession().setAttribute("login_verify_userId", history_validateCode.getUserId());
            validateCodeManager.delete(email,IBSConstants.VALIDATE_CODE_TYPE_RETRIEVE_LOGIN_PASSWORD);
            return BASE_DIR+"loginPwdResetPwd";
        }
        return "forward:/findLoginPwd/verifyIdentity?loginName="+loginName+"&message="+URLEncoder.encode("链接已失效", "UTF-8");
    }
    @RequestMapping("success")
    public String success(HttpServletRequest request,HttpServletResponse response){
        Long userId = (Long) request.getSession().getAttribute("login_verify_userId");
        String password = request.getParameter("password");
        if(userId!=null){
            User user = userManager.getByObjectId(userId);
            user.setPassword(SecurityUtils.generatePassword(password));
            userManager.save(user);
            request.setAttribute("userId", userId);
            return BASE_DIR+"loginPwdSuccess";
        }
        return null;
    }
    @RequestMapping("/getLoginCode")
    public String getLoginCode(HttpServletRequest request,HttpServletResponse response,ModelMap map){
        String loginName = request.getParameter("loginName");
        User user = userManager.getUserByLoginNameAndPlatform(loginName, IBSConstants.PLATEFORM_HR);
        if(user!=null){
            return "forward:/sms/getRetrieveLoginPwdCode/"+user.getMobilePhone()+"?userId="+user.getObjectId();
        }else{
            map.addAttribute("result",false);
            map.addAttribute("message","发送短信异常");
            return "jsonView";
        }
    }

    @RequestMapping("/sendEmail")
    public String sendEmail(HttpServletRequest request,HttpServletResponse response,ModelMap map){
        String loginName = request.getParameter("loginName");
        User user = userManager.getUserByLoginNameAndPlatform(loginName, IBSConstants.PLATEFORM_HR);
        if(user!=null){
            return "forward:/email/getRetrieveLoginPwdCode?userId="+user.getObjectId()+"&email="+user.getEmail();
        }else{
            map.addAttribute("result",false);
            map.addAttribute("message","发送邮件异常");
            return "jsonView";
        }
    }
}
