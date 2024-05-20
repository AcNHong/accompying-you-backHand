package com.jun.usercenter.model.result;

import com.jun.usercenter.common.ErrorCodeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result {
    /**
     * 响应码
     */
    private Integer code;
    /**
     * 提示信息
     */
    private String msg;
    /**
     * 描述信息
     */
    private String description;
    /**
     * 响应数据
     */
    private Object data;

    /**
     * 返回成功的响应对象
     *
     * @return Result
     */
    public static Result success() {
        return new Result(200, "success", "", null);
    }

    /**
     * 返回成功的响应对象
     *
     * @return Result
     */
    public static Result success(Object data) {
        return new Result(200, "success", "", data);
    }

    /**
     * 返回成功的响应对象
     * @param errorCode 枚举类
     * @return Result
     */
    public static Result success(ErrorCodeEnum errorCode,Object data) {
        return new Result(errorCode.getCode(), errorCode.getMsg(), errorCode.getDescription(), data);
    }

    /**
     * 返回失败的响应对象
     *
     * @return Result
     */
    public static Result error(String msg) {
        return new Result(500, msg, "", null);
    }

    /**
     * 返回失败的响应对象
     *
     * @return Result
     */
    public static Result error(String msg, String description) {
        return new Result(500, msg, description, null);
    }

    /**
     * 返回失败的响应对象
     *
     * @return Result
     */
    public static Result error(int code,String msg, String description) {
        return new Result(code, msg, description, null);
    }

    /**
     * 返回失败的响应对象
     *
     * @return Result
     */
    public static Result error(ErrorCodeEnum errorCode) {
        return new Result(errorCode.getCode(), errorCode.getMsg(), errorCode.getDescription(), null);
    }


    public static Result error(ErrorCodeEnum errorCodeEnum, String message, String description) {
        return new Result(errorCodeEnum.getCode(),message,description,null);
    }
}
