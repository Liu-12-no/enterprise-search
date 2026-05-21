package com.qych.utils.exception;

import com.qych.utils.pojo.BaseResponse;
import com.qych.utils.utils.RetCode;

public class BaseException extends RuntimeException{
    private int errorCode;

    public BaseException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BaseException(String message) {
        super(message);
        this.errorCode = RetCode.RET_EXCEPTION;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public BaseResponse getResponse(){
        return new BaseResponse().error(getErrorCode(),getMessage());
    }

    public static <T> BaseResponse<T> error(int code, String error, T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.code = code;
        response.message = error;
        response.data = data;
        return response;
    }
}
