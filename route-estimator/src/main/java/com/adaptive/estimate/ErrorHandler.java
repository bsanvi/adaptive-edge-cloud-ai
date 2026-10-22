package com.adaptive.estimate;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler({IllegalArgumentException.class,MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid(Exception e,HttpServletRequest request) {
        if(e instanceof MethodArgumentNotValidException validation && validation.getBindingResult().getTarget() instanceof EstimateRequest body)
            request.setAttribute("requestId",body.requestId());
        Map<String,Object> body=new LinkedHashMap<>(); body.put("requestId",request.getAttribute("requestId"));
        body.put("code","VALIDATION_ERROR");body.put("message","Malformed or invalid estimate context");
        body.put("details",Map.of());body.put("timestamp",Instant.now().toString());
        return ResponseEntity.badRequest().body(body);
    }
}
