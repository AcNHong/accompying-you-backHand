package com.jun.usercenter.common;

/**
 * 枚举，定义错误响应常见信息变量
 */
public enum ErrorCodeEnum {

    /**
     * 构造，自定义响应信息
     */
    SUCCESS(2000,"OK",""),
    PARAM_ERROR(4000,"参数错误",""),
    PARAM_NULL(4001,"参数为空",""),
    NOT_AUTH(4010,"无权限",""),
    NOT_LOGIN(4011,"没有登陆",""),
    SYSTEM_ERROR(5000,"系统异常","");


    private final int code;

    private final String msg;

    private final String description;


    ErrorCodeEnum(int code, String msg, String description) {
        this.code = code;
        this.msg = msg;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public String getDescription() {
        return description;
    }
}
