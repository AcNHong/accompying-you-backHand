package com.jun.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求
 *
 * @author beihong
 */
@Data
public class UserLoginRequest implements Serializable {
    private static final long SerialVersionUID = 1L;

    private String userAccount;

    private String userPassword;

}
