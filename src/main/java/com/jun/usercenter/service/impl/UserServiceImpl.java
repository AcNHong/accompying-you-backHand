package com.jun.usercenter.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jun.usercenter.common.ErrorCodeEnum;
import com.jun.usercenter.excption.BusinessException;
import com.jun.usercenter.mapper.UserMapper;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.service.UserService;

import com.jun.usercenter.utils.StrProcess;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jun.usercenter.constant.UserConstant.LOGIN_STATUS_SIGNATURE;

/**
* @author BH
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2024-03-03 11:27:40
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {


    @Resource
    private UserMapper userMapper;
    public long userRegister(String userAccount,String userPassword,String checkPassword) {
        //非空校验
        if(userAccount == null || userPassword == null || checkPassword == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }

        //字符串准确性校验
        if(userAccount.length() < 4 || userPassword.length() < 8){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        //是否重复验证
        Long count = userMapper.selectCount(queryWrapper);
        if(count > 0){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"账户重复");
        }

        //特殊字符
        String validPattern = "[\\W_]\n";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"包含特殊字符");
        }

        //两次密码相同
        if(!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"两次密码不一致");
        }

        //加密
        //保存数据
        User user = new User();
        try {
            String md5Password = StrProcess.getMD5Hash(userPassword);

            user.setUserAccount(userAccount);
            user.setUserPassword(md5Password);
            this.save(user);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }



        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //非空校验
        if(userAccount == null || userPassword == null){
           throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }

        //字符串准确性校验
        if(userAccount.length() < 4 || userPassword.length() < 8){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        //特殊字符
        String validPattern = "[\\W_]\n";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if(matcher.find()){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"包含特殊字符");
        }


        String userPasswordMd5 = null;
        //md5转换
        try {
            userPasswordMd5 = StrProcess.getMD5Hash(userPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",userPasswordMd5);
        //查询mapper
        User user = userMapper.selectOne(queryWrapper);
        User safetyUser = getSafetyUser(user);
        //登陆状态LOGIN_STATUS_SIGNATURE
        request.getSession().setAttribute(LOGIN_STATUS_SIGNATURE,safetyUser);

        return safetyUser;

    }

    /**
     * 用户脱敏
     * @param user
     * @return User
     */
    public User getSafetyUser(User user){
        if(user == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"账户或密码错误");
        }
        User safetyDateUser = new User();
        safetyDateUser.setId(user.getId());
        safetyDateUser.setUsername(user.getUsername());
        safetyDateUser.setUserAccount(user.getUserAccount());
        safetyDateUser.setAvatarUrl(user.getAvatarUrl());
        safetyDateUser.setRole(user.getRole());
        safetyDateUser.setGender(user.getGender());
        safetyDateUser.setPhone(user.getPhone());
        safetyDateUser.setEmail(user.getEmail());
        safetyDateUser.setUserStatus(user.getUserStatus());

        return safetyDateUser;
    }

    @Override
    public void userLogOut(HttpServletRequest request) {
        //修改登录态
        request.getSession().removeAttribute(LOGIN_STATUS_SIGNATURE);

    }
}




