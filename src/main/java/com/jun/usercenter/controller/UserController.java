package com.jun.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jun.usercenter.common.ErrorCodeEnum;
import com.jun.usercenter.excption.BusinessException;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.model.domain.request.UserLoginRequest;
import com.jun.usercenter.model.domain.request.UserRegisterRequest;
import com.jun.usercenter.model.domain.result.Result;
import com.jun.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.jun.usercenter.constant.UserConstant.DEFALUT;
import static com.jun.usercenter.constant.UserConstant.LOGIN_STATUS_SIGNATURE;

/**
 * 用户请求接口
 *
 * @author beihong
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController{
    @Resource
    private UserService userService;

    /**
     * 注册请求接口
     *
     * @param registerRequest 实体类对象封装用户名、账户、密码
     * @return id
     */
    @CrossOrigin(origins = "http://localhost:8080") // 允许来自 localhost:8080 的跨域请求
    @PostMapping("/register")
    public Result userRegister(@RequestBody UserRegisterRequest registerRequest){
        if (registerRequest == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }

        String userAccount = registerRequest.getUserAccount();
        String userPassword = registerRequest.getUserPassword();
        String checkPassword = registerRequest.getCheckPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }

        long id = userService.userRegister(userAccount, userPassword, checkPassword);
        return Result.success(id);
    }

    /**
     * 登录请求接口
     *
     * @param loginRequest 封装登录信息账户和密码
     * @param request HTTP请求对象，用于获取当前用户的登录态信息
     * @return User
     */
    @PostMapping("/login")
    public Result userLogin(@RequestBody UserLoginRequest loginRequest, HttpServletRequest request) {

        if (loginRequest == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }

        String userAccount = loginRequest.getUserAccount();
        String userPassword = loginRequest.getUserPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }

        User user = userService.userLogin(userAccount, userPassword, request);
        return Result.success(user);
    }

    /**
     * 获取当前用户登录态
     * @param request HTTP请求对象，用于获取当前用户的登录态信息
     * @return User
     */
    @GetMapping("/currentUser")
    public Result getCurrentUser(HttpServletRequest request){
        User currentUser = (User)request.getSession().getAttribute(LOGIN_STATUS_SIGNATURE);
        Long id = currentUser.getId();
        User latestUser = userService.getById(id);

        User safetyUser = userService.getSafetyUser(latestUser);
        return Result.success(safetyUser);

    }

    /**
     * 用户注销请求接口
     * @return int
     */
    @PostMapping("/logOut")
    public Result userLogin(HttpServletRequest request) {

        if (request == null) {
            return Result.error("错误请求");
        }

        userService.userLogOut(request);

        return Result.success();
    }

    /**
     * 批量查询请求接口
     *
     * @param username 用户的昵称
     * @param request HTTP请求对象，用于获取当前用户的登录态信息
     * @return List<User>
     */
    @GetMapping("/search")
    public Result searchUsers(String username, HttpServletRequest request) {
        //判断权限
        if (isNotAdmin(request)) {
            return Result.error("不是管理");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(username)) {
            //like默认模糊匹配 *column*
            queryWrapper.like("username", username);
        }

        List<User> users = userService.list(queryWrapper);
        List<User> safetyUsers = users.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return Result.success(safetyUsers);
    }

    /**
     * 删除用户请求接口
     *
     * @param id 查询id，用户的唯一标识
     * @param request HTTP请求对象，用于获取当前用户的登录态信息
     * @return User
     */
    @PostMapping("/delete")
    public Result deleteUser(Long id, HttpServletRequest request) {
        if (isNotAdmin(request)) {
            return Result.error("不是管理");
        }

        //判断id是否有效
        if (id <= 0) {
            return Result.error("id无效");
        }

        return Result.success(userService.removeById(id));

    }


    /**
     * 验证是否为管理员
     * @param request HTTP请求对象，用于获取当前用户的登录态信息
     * @return boolean
     */
    private boolean isNotAdmin(HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(LOGIN_STATUS_SIGNATURE);

        //非空权限判断
        return loginUser == null || loginUser.getRole() == DEFALUT;
    }


}


