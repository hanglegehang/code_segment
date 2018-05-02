package com.jdai.task.utils

import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Form
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType

class HttpClientUtilTest {
    public static void main(String[] args) {
        HashMap hashMap = new HashMap();
        hashMap.put("param", "1111")
//        HttpClientUtil.post("http://localhost:8080/tagTimerMaster/auth/ssoLogin", "params=11111", true)
//        HttpClientUtil.post("http://localhost:8080/tagTimerMaster/auth/ssoLogin", hashMap)

        HttpResponse response = Request.Post("http://localhost:8080/tagTimerMaster/auth/ssoLogin").addHeader("Cookie", "JSESSIONID=5B6B3E96FEA6C89DB0C7498DCCDC3368; sso.jd.com=79253f5ada834adc9a5e7bb2c0ca26cd")
                .bodyForm(Form.form().add("username", "vip").add("password", "secret").build())
                .execute().returnResponse();
        println response.getStatusLine()
        Request.Post("http://localhost:8080/tagTimerMaster/auth/ssoLogin").addHeader("Cookie", "JSESSIONID=5B6B3E96FEA6C89DB0C7498DCCDC3368; sso.jd.com=79253f5ada834adc9a5e7bb2c0ca26cd")
                .bodyString("1111", ContentType.APPLICATION_JSON)
                .execute().returnContent().asString();
    }
}
