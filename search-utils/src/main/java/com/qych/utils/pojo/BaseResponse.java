package com.qych.utils.pojo;

import com.qych.utils.utils.RetCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {


    public int code;
    public String message;
    public T data;

    public BaseResponse() {}

    // ================= 静态工厂方法 =================
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.code = RetCode.RET_OK;
        response.message = "操作成功";
        response.data = data;
        return response;
    }
    /**
     * 仅返回操作成功状态，不带具体数据
     */
    public static <T> BaseResponse<T> success() {
        BaseResponse<T> response = new BaseResponse<>();
        response.code = RetCode.RET_OK;
        response.message = "操作成功";
        response.data = null;
        return response;
    }

    public static <T> BaseResponse<T> success(String message, T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.code = RetCode.RET_OK;
        response.message = message;
        response.data = data;
        return response;
    }


    // ================= 失败静态工厂 =================

    // 1. 接收 2 个参数：状态码 + 错误信息 (这个必须有，因为下面的默认方法依赖它)
    public static <T> BaseResponse<T> error(int code, String error) {
        BaseResponse<T> response = new BaseResponse<>();
        response.code = code;
        response.message = error;
        response.data = null;
        return response;
    }

    // 2. 接收 1 个参数：默认只传错误信息，状态码自动用 1 (RET_EXCEPTION)
    public static <T> BaseResponse<T> error(String error) {
        // 这里就不会报错了，因为它会去调用上面那个两参数的方法
        return error(RetCode.RET_EXCEPTION, error);
    }

    // 3. 接收 3 个参数：状态码 + 错误信息 + 数据 (专门为你们公司的 getStack 堆栈数据准备的！)
    public static <T> BaseResponse<T> error(int code, String error, T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.code = code;
        response.message = error;
        response.data = data;
        return response;
    }


}