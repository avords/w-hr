package com.handpay.ibenefit.system.web;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.log4j.Logger;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;

public class FreemarkerTemplateUtils {
    private static final Logger LOGGER = Logger.getLogger(FreemarkerTemplateUtils.class);

    public static String getContent(String content,Map<String,Object> map){
        if(map == null || map.size()==0){
            return content;
        }
        StringTemplateLoader stringLoader = new StringTemplateLoader();
        stringLoader.putTemplate("template",content);
        Configuration freemarkerConfiguration = new Configuration();
        freemarkerConfiguration.setTemplateLoader(stringLoader);
        freemarkerConfiguration.setDefaultEncoding("UTF-8");
        Template template = null;
        Writer writer = new StringWriter();
        String htmlText = null;
        try {
            template = freemarkerConfiguration.getTemplate("template");
            template.process(map, writer);
            htmlText = writer.toString();
            writer.close();
        }catch (Exception e) {
            LOGGER.error("getContent",e);
            return "";
        }
        return htmlText;
    }
}
