package com.handpay.ibenefit.insure.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.dubbo.config.annotation.Reference;
import com.handpay.ibenefit.framework.entity.Dictionary;
import com.handpay.ibenefit.framework.service.IDictionaryManager;
import com.handpay.ibenefit.framework.util.AjaxUtils;
import com.handpay.ibenefit.insure.entity.InsureRange;
import com.handpay.ibenefit.insure.service.IInsureRangeManager;

@Controller
@RequestMapping("/insureRange")
public class InsureRangeController {

    @Reference(version = "1.0", timeout= 60000)
    private IInsureRangeManager insureRangeManager;
    @Reference(version = "1.0", timeout= 60000)
    private IDictionaryManager dictionaryManager;

    @RequestMapping("/getTypes")
    public String getTypes(HttpServletRequest request,HttpServletResponse response,ModelMap modelMap,Long attributeValueId) throws Exception{
        List<Integer> types = insureRangeManager.getTypesByAttributeValueId1(attributeValueId);
      //年龄阶段字典
        List<Dictionary> ageDictionary = dictionaryManager.getDictionariesByDictionaryId(3001);
      //职业类型字典
        List<Dictionary> jobDictionary = dictionaryManager.getDictionariesByDictionaryId(3002);
      //社保类型字典
        List<Dictionary> socialDictionary = dictionaryManager.getDictionariesByDictionaryId(3003);
        StringBuilder jsonB = new StringBuilder();
        for(int j=0;j<types.size();j++){
            Integer type = types.get(j);
            if(type==InsureRange.AGE){
                jsonB.append("'age':{'name':'投保人年龄','opt':{");
                for(int i=0;i<ageDictionary.size();i++){
                    Dictionary d = ageDictionary.get(i);
                    jsonB.append("'").append(d.getValue()).append("':'").append(d.getName()).append("'");
                    if(i!=ageDictionary.size()-1){
                        jsonB.append(",");
                    }else{
                        jsonB.append("}}");
                    }
                }
            }else if(type==InsureRange.JOB){
                jsonB.append("'job':{'name':'职业类型','opt':{");
                for(int i=0;i<jobDictionary.size();i++){
                    Dictionary d = jobDictionary.get(i);
                    jsonB.append("'").append(d.getValue()).append("':'").append(d.getName()).append("'");
                    if(i!=jobDictionary.size()-1){
                        jsonB.append(",");
                    }else{
                        jsonB.append("}}");
                    }
                }
            }else if(type==InsureRange.SOCIAL){
                jsonB.append("'social':{'name':'社保类型','opt':{'");
                for(int i=0;i<socialDictionary.size();i++){
                    Dictionary d = socialDictionary.get(i);
                    jsonB.append("'").append(d.getValue()).append("':'").append(d.getName()).append("'");
                    if(i!=socialDictionary.size()-1){
                        jsonB.append(",");
                    }else{
                        jsonB.append("}}");
                    }
                }
            }
            if(j!=types.size()-1){
                jsonB.append(",");
            }
        }
        String backFunc = request.getParameter("backFunc");
        String result = "{" + jsonB.toString() + "}";
        AjaxUtils.doAjaxResponse(response, backFunc + "(" + result + ")");
        return null;
    }
}
