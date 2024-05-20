package com.jun.usercenter.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jun.usercenter.common.ErrorCodeEnum;
import com.jun.usercenter.constant.UserConstant;
import com.jun.usercenter.excption.BusinessException;
import com.jun.usercenter.mapper.UserMapper;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.service.UserService;
import com.jun.usercenter.utils.AlgorithmUtils;
import com.jun.usercenter.utils.StrProcess;
import org.apache.commons.lang3.tuple.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static com.jun.usercenter.constant.UserConstant.LOGIN_STATUS_SIGNATURE;

/**
* @author BH
* &#064;description  针对表【user(用户表)】的数据库操作Service实现
* &#064;createDate  2024-03-03 11:27:40
 */
@Service
@Slf4j
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


        String userPasswordMd5;
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
     * @param user 用户
     * @return User 用户
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
        safetyDateUser.setPlanetCode(user.getPlanetCode());
        safetyDateUser.setUserStatus(user.getUserStatus());
        safetyDateUser.setTags(user.getTags());

        return safetyDateUser;
    }

    @Override
    public void userLogOut(HttpServletRequest request) {
        //修改登录态
        request.getSession().removeAttribute(LOGIN_STATUS_SIGNATURE);

    }

    /**
     * 根据标签查询用户
     * @param tagList 标签列表
     * @return List<User>
     */
    @Override
    public List<User> searchUserByTags(List<String> tagList) {
        if(tagList == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }
       /* QueryWrapper<User> queryWrapper = new QueryWrapper();

        */
        /**
         * 直接查询数据库
         */
        /*
        for (String tagName : tagList) {
            queryWrapper = queryWrapper.like("tags",tagName);
        }
        List<User> users = userMapper.selectList(queryWrapper);*/

        /**
         * 查询内存 todo
         */
        //查询所有
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);

        Gson gson = new Gson();
        //对list中的user挨个查询搜索
        /*for (User user : userList) {
            //取user字段 tags
            String tags = user.getTags();
            //json转set列表
            Set<String> tempTagNameSet = gson.fromJson(tags, Set.class);
            for (String tag : tempTagNameSet) {
                if(){

                }
            }

        }*/


      return userList.stream().filter( user -> {
                String tags = user.getTags();
               /* if(StringUtils.isEmpty(tags)){
                    return false;
                }*/
                log.info("tags{}",tags);
                Set<String> tempTagNameSet = gson.fromJson(tags,new TypeToken<Set<String>>(){}.getType());
                //如果集合为空，则对其赋一个随机默认值
                tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
                //遍历tagList，满足tagList的user将会被留下
                for (String tag : tagList) {
                    if(!tempTagNameSet.contains(tag)){
                        return false;
                    }
                }
                return true;
            }).map(this::getSafetyUser).collect(Collectors.toList());



    }

    /**
     * 获取当前登录用户
     * @param request
     * @return User
     */
    @Override
    public User getUserLogin(HttpServletRequest request) {
        if(request == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        //获取session中的登录信息
        Object objUser = request.getSession().getAttribute(LOGIN_STATUS_SIGNATURE);
        return (User)objUser;
    }

    /**
     * 更新用户接口
     * @param loginUser
     * @param currentUser
     */
    @Override
    public int updateUser(User loginUser,User currentUser) {
        if(currentUser.getId() < 0){
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
        if(!isAdmin(loginUser)&&(currentUser.getId()!=loginUser.getId())){
            throw new BusinessException(ErrorCodeEnum.NOT_AUTH);
        }
        User oldUser = userMapper.selectById(currentUser.getId());
        if(oldUser == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        return userMapper.updateById(currentUser);

    }

    /**
     * 判断用户权限
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser){
        return (loginUser != null) && (loginUser.getRole() == UserConstant.ADMIN);
    }

    /**
     * 标签匹配用户
     * @param loginUser
     * @param num
     * @return
     */
    @Override
    public List<User> matchUsers(User loginUser, long num) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id","tags");
        queryWrapper.isNotNull("tags");
        //获取登录用户的tags 并转为list
        String loginUserTags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> loginUserTagList = gson.fromJson(loginUserTags, new TypeToken<List<String>>() {
        }.getType());
        //用户相似度集合user-long
        ArrayList<Pair<User,Integer>> DistanceList = new ArrayList<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        //遍历userList
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(0);
            //tags非空判断 如果是本用户不进行匹配，跳过本次循环
            String userTags = user.getTags();
            if(StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()){
                continue;
            }
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //相似度匹配计算
            int distance = AlgorithmUtils.minDistance(loginUserTagList, userTagList);
            DistanceList.add(Pair.of(user,distance));
        }
        //根据相似度进行排序 同时限定容量
        List<Pair<User, Integer>> topUserPairList = DistanceList.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        //取出id 查库
        List<Long> idList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> idsQueryWrapper = new QueryWrapper<>();
        idsQueryWrapper.in("id",idList);
        //用户脱敏 并分组id
        Map<Long, List<User>> userIdUserListMap = userMapper.selectList(idsQueryWrapper).stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        ArrayList<User> finalUserList = new ArrayList<>();
        //保持idList一样顺序
        for (Long id : idList) {
            finalUserList.add(userIdUserListMap.get(id).get(0));
        }
        return finalUserList;
    }

    /**
     * 验证是否为管理员
     * @param request HTTP请求对象，用于获取当前用户的登录态信息
     * @return boolean
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(LOGIN_STATUS_SIGNATURE);

        //非空权限判断
        return (loginUser!=null) && (loginUser.getRole() == UserConstant.ADMIN);
    }

}




