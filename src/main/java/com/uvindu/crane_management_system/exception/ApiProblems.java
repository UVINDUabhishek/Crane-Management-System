package com.uvindu.crane_management_system.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;

public final class ApiProblems {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private ApiProblems() { }

    public static ProblemDetail create(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:chms:problem:" + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setProperty("code", code);
        return problem;
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status)
            throws IOException {
        String code = status == HttpStatus.UNAUTHORIZED ? "UNAUTHORIZED" : "FORBIDDEN";
        String detail = status == HttpStatus.UNAUTHORIZED
                ? "Authentication is required or credentials are invalid" : "Access is denied";
        ProblemDetail problem = create(status, code, detail);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        JSON.writeValue(response.getOutputStream(), Map.of("type", problem.getType().toString(), "title", status.getReasonPhrase(),
                "status", status.value(), "detail", detail, "instance", request.getRequestURI(), "code", code));
    }
}
