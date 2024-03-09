package com.jun.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jun.usercenter.model.domain.User;

import javax.servlet.http.HttpServletRequest;


/**
* @author 14136
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2024-03-03 11:27:40
*/
public interface UserService extends IService<User> {
     static String PRE_MD5_PASSWORD = "beihong";

     /**
      *
      * @param userAccount 账户
      * @param userPassword 密码
      * @param checkPassword 校验密码
      * @return id
      */
     long userRegister(String userAccount,String userPassword,String checkPassword) ;

     /**
      *
      * @param userAccount 账户
      * @param userPassword 密码
      * @param request http请求对象
      * @return 用户对象
      */
     User userLogin(String userAccount, String userPassword, HttpServletRequest request);

     /**
      * 用户脱敏
      * @param user
      * @return User
      */
     User getSafetyUser(User user);

     /**
      * 用户注销
      */
     void userLogOut(HttpServletRequest request);

}
