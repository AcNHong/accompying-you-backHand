package com.jun.usercenter.excption;

import com.jun.usercenter.common.ErrorCodeEnum;

/**
 * 自定义异常捕获处理
 */
public class BusinessException extends RuntimeException{
    private int code;

    private String description;


    public BusinessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BusinessException(ErrorCodeEnum errorCode,String description) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.description = description;
    }


    public BusinessException(ErrorCodeEnum errorCode) {
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
