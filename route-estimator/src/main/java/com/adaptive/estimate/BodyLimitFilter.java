package com.adaptive.estimate;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Bounds JSON bodies even when Content-Length is absent (chunked transfer). */
@Component
public class BodyLimitFilter extends OncePerRequestFilter {
    private static final int MAX=1048576;
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        if (!"POST".equals(request.getMethod())) {chain.doFilter(request,response);return;}
        if(request.getContentLengthLong()>MAX){reject(response);return;}
        byte[] bytes=request.getInputStream().readNBytes(MAX+1);
        if(bytes.length>MAX){reject(response);return;}
        chain.doFilter(new HttpServletRequestWrapper(request){
            @Override public ServletInputStream getInputStream(){
                var input=new ByteArrayInputStream(bytes);
                return new ServletInputStream(){
                    public int read(){return input.read();}
                    public int read(byte[] b,int off,int len){return input.read(b,off,len);}
                    public boolean isFinished(){return input.available()==0;}
                    public boolean isReady(){return true;}
                    public void setReadListener(ReadListener listener){throw new UnsupportedOperationException("Synchronous JSON endpoint");}
                };
            }
            @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
        },response);
    }
    private void reject(HttpServletResponse response)throws IOException{
        response.setStatus(413);response.setContentType("application/json");
        response.getWriter().write("{\"requestId\":null,\"code\":\"PAYLOAD_TOO_LARGE\",\"message\":\"Maximum JSON body is 1048576 bytes\",\"details\":{},\"timestamp\":\""+java.time.Instant.now()+"\"}");
    }
}
