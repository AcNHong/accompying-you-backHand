package com.jun.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jun.usercenter.common.ErrorCodeEnum;
import com.jun.usercenter.excption.BusinessException;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.model.request.UserLoginRequest;
import com.jun.usercenter.model.request.UserRegisterRequest;
import com.jun.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.jun.usercenter.model.result.Result;
import static com.jun.usercenter.constant.UserConstant.LOGIN_STATUS_SIGNATURE;

/**
 * 用户请求接口
 *
 * @author beihong
 */
@RestController
@RequestMapping("/user")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000/"})
public class UserController{
    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 注册请求接口
     *
     * @param registerRequest 实体类对象封装用户名、账户、密码
     * @return id
     */

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
        if (userService.isAdmin(request)) {
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
        if (userService.isAdmin(request)) {
            return Result.error("不是管理");
        }

        //判断id是否有效
        if (id <= 0) {
            return Result.error("id无效");
        }

        return Result.success(userService.removeById(id));

    }


    /**
     * 修改用户信息
     * @param user
     * @param request
     * @return
     */
    @PostMapping("/update")
    public Result updateUser(@RequestBody User user, HttpServletRequest request){
        //非空判断
        if(user == null || request == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }
        //获取当前登录用户
        User loginUser = userService.getUserLogin(request);
        //更新用户接口
        int count = userService.updateUser(loginUser, user);

        return Result.success(count);
    }

    /**
     * 根据标签搜索用户
     * @param tagList
     * @return
     */
    @GetMapping("/search/tags")
    public Result searchUserByTags(@RequestParam(required = false) List<String> tagList){
        //非空判断
        if(CollectionUtils.isEmpty(tagList)){
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }

        List<User> users = userService.searchUserByTags(tagList);

        return Result.success(users);
    }

    /**
     * 获取推荐用户
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    public Result recommendUsers(HttpServletRequest request,Long currentPage,Long size){
        //获取登录用户id
        User userLogin = userService.getUserLogin(request);

        if(userLogin == null){
            throw new BusinessException(ErrorCodeEnum.NOT_LOGIN);
        }
            userLogin.getId();

            //构造键
            ValueOperations valueOperations = redisTemplate.opsForValue();
            String keyString = String.format("accompanying:user:recommend:%s", userLogin.getId());
            Object cacheUsers = valueOperations.get(keyString);
            if (cacheUsers != null) {
                return Result.success(cacheUsers);
            }

        //没找到信息 查库
        IPage<User> page = new Page<>(currentPage,size);
        List<User> usersList = userService.list(page);

        //存入缓存
        try {
            valueOperations.set(keyString,usersList,30000, TimeUnit.MICROSECONDS);
        } catch (Exception e) {
            log.error("redis set key error:{}",e);
        }
        return Result.success(usersList);
    }

    /**
     * 返回标签匹配用户
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public Result matchUsers(long num,HttpServletRequest request){
        //num限定20
        if(num <= 0 || num > 20){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        //获取登录用户
        User loginUser = userService.getUserLogin(request);
        return Result.success(userService.matchUsers(loginUser,num));
    }


}


