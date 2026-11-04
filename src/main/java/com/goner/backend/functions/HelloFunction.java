package com.goner.backend.functions;

import com.google.cloud.functions.HttpFunction;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * 세팅 확인용 함수. 배포/로컬 실행이 되는지 확인한 뒤 지워도 된다.
 * 로컬: ./gradlew runFunction  →  http://localhost:8080
 */
public class HelloFunction implements HttpFunction {
    @Override
    public void service(HttpRequest request, HttpResponse response) throws IOException {
        response.setContentType("application/json; charset=utf-8");
        BufferedWriter writer = response.getWriter();
        writer.write("{\"service\":\"goner-backend\",\"status\":\"ok\"}");
    }
}
