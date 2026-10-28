package com.adaptive.cloud;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler(ApiFailure.class) public ResponseEntity<?> failure(ApiFailure e,HttpServletRequest request) {
        return envelope(e.status,e.code,e.getMessage(),request);
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalid(Exception e,HttpServletRequest request) {
        if (e instanceof MethodArgumentNotValidException validation && validation.getBindingResult().getTarget() != null) {
            Object target=validation.getBindingResult().getTarget();
            if (target instanceof CloudController.RawRequest raw) request.setAttribute("requestId",raw.requestId());
            if (target instanceof CloudController.HybridRequest hybrid) request.setAttribute("requestId",hybrid.requestId());
        }
        return envelope(400,"VALIDATION_ERROR","Malformed or invalid request",request);
    }
    private ResponseEntity<?> envelope(int status,String code,String message,HttpServletRequest request) {
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("requestId",request.getAttribute("requestId")); body.put("code",code); body.put("message",message);
        body.put("details",Map.of()); body.put("timestamp",Instant.now().toString());
        return ResponseEntity.status(status).body(body);
    }
}
