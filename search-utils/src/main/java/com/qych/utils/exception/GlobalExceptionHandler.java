package com.qych.utils.exception;

import com.qych.utils.pojo.BaseResponse;
import com.qych.utils.utils.RetCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 默认给个 dev，防止没配环境变量时报错
    @Value("${evnname:dev}")
    String evnName;

    // TODO: 如果你以后把公司的异常日志表也搬过来了，再把这个解开
    // @Autowired
    // ExceptionLogService exceptionLogService;

    @ExceptionHandler(ClientAbortException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    private void handleClientAbortException(Exception e, HttpServletRequest resq){
        log.warn("接口["+resq.getRequestURI() + "]请求过程中客户端断开连接...");
    }

    @ExceptionHandler(BaseException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    // @DisableSecretResponse (TODO: 公司自定义的免密注解，如果不需要拦截器加密可以暂时注掉)
    private BaseResponse handleBaseException(HttpServletRequest request, HttpServletResponse response, BaseException e) throws UnsupportedEncodingException {
        log.info("*************exception*************", e);
        // 注意：如果你之前 BaseResponse 里没写三个参数的 error 方法，这里会标红。
        // 请在 BaseResponse 里补上: public static <T> BaseResponse<T> error(int code, String error, T data)
        return BaseResponse.error(e.getErrorCode(), e.getMessage(), getStack(request, e));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    private BaseResponse handleException(HttpServletRequest request, HandlerMethod handler, Exception e) throws IOException {
        log.info("*************exception*************", e);

        if(e.getCause() instanceof BaseException){
            return BaseResponse.error(RetCode.RET_EXCEPTION, e.getCause().getMessage(), getStack(request, e));
        }

        // TODO: 如果搬运了 Sentry 监控和异常日志服务，再解开下面两行
        // Sentry.captureException(e);
        // exceptionLogService.saveExceptionRecord(request, e, handler);

        return BaseResponse.error(RetCode.RET_EXCEPTION, "未知异常：" + e.getMessage(), getStack(request, e));
    }

    // 处理 @Valid 注解校验参数异常 (非常完美的设计！)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    private BaseResponse handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e){
        BindingResult bindingResult = e.getBindingResult();
        List<ObjectError> errors = bindingResult.getAllErrors();

        StringBuilder errorMessage = new StringBuilder();
        for (ObjectError error : errors) {
            errorMessage.append(error.getDefaultMessage()).append("; ");
        }
        if (errorMessage.length() > 0) {
            errorMessage.setLength(errorMessage.length() - 2);
        }

        log.error("{} *************MethodArgumentNotValidException*************", request.getRequestURL(), e);
        return BaseResponse.error(RetCode.RET_EXCEPTION, errorMessage.toString());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private BaseResponse handleMissingServletRequestParameterException(HttpServletRequest request, MissingServletRequestParameterException e) {
        log.error("{} *************MissingServletRequestParameterException*************", request.getRequestURL(), e);
        return BaseResponse.error(RetCode.RET_EXCEPTION, "缺少必须的请求参数");
    }

    private String getStack(HttpServletRequest request, Exception e) throws UnsupportedEncodingException {
        boolean isDebug = false;
        Cookie[] cookies = request.getCookies();
        if(cookies != null){
            for(Cookie cookie : cookies){
                // 你们公司的祖传后门密码
                if(cookie != null && "debug".equals(cookie.getName()) && "qdeicc".equals(cookie.getValue())){
                    isDebug = true;
                    break;
                }
            }
        }

        if (isDebug || "dev".equals(evnName) || "test".equals(evnName) || "local".equals(evnName)) {
            StringBuffer sb = new StringBuffer();
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            sb.append(stringWriter);
            return sb.toString();
        }
        return null;
    }
}