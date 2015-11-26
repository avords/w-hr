package com.handpay.ibenefit.portal.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.code.kaptcha.Constants;

@Controller
@RequestMapping("/")
public class PortalController {

	private static final Logger LOG =Logger.getLogger(PortalController.class);
	private static final String PORTAL_DIR = "portal/";

	private boolean validateAuthCode(HttpServletRequest request) {
        HttpSession session = request.getSession();
        String authcode = (String)session.getAttribute(Constants.KAPTCHA_SESSION_KEY);
        String submitAuthCode = request.getParameter("authcode");
        return submitAuthCode.equals(authcode);
    }

    @ResponseBody
    @RequestMapping("/validateAuthCode")
    private boolean validateAuthCode(HttpServletRequest request,HttpServletResponse response) {
        return validateAuthCode(request);
    }
}
