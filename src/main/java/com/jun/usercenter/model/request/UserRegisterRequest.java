package com.jun.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {
    public static final long SerialVersionUID = 2L;

    private String userAccount;
    private String userPassword;
    private String checkPassword;

}
