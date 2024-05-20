package com.jun.usercenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jun.usercenter.common.ErrorCodeEnum;
import com.jun.usercenter.common.TeamStatusEnum;
import com.jun.usercenter.excption.BusinessException;
import com.jun.usercenter.mapper.TeamMapper;
import com.jun.usercenter.mapper.UserTeamMapper;
import com.jun.usercenter.model.domain.Team;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.model.domain.UserTeam;
import com.jun.usercenter.model.dto.TeamQuery;
import com.jun.usercenter.model.request.TeamDeleteRequest;
import com.jun.usercenter.model.request.TeamJoinRequest;
import com.jun.usercenter.model.request.TeamQuitRequest;
import com.jun.usercenter.model.request.TeamUpdateRequest;
import com.jun.usercenter.model.vo.TeamUserVO;
import com.jun.usercenter.model.vo.UserVO;
import com.jun.usercenter.service.TeamService;
import com.jun.usercenter.service.UserService;
import com.jun.usercenter.service.UserTeamService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
* @author 14136
* @description 针对表【Team(队伍表)】的数据库操作Service实现
* @createDate 2024-04-16 17:43:08
*/
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {

    @Resource
    private TeamMapper teamMapper;
    @Resource
    private UserTeamMapper userTeamMapper;
    @Resource
    private UserService userService;
    @Resource
    private UserTeamService userTeamService;
    @Resource
    private RedissonClient redissonClient;
    /**
     *
     * @param team
     * @param loginUser
     * @return teamId 队伍id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        //1. 请求参数是否为空？
        if(team == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        //2. 是否登录，未登录不允许创建
        if(loginUser == null){
            throw new BusinessException(ErrorCodeEnum.NOT_LOGIN);
        }
        final long userId = loginUser.getId();
        //3. 校验信息
        //   1. 队伍人数 > 1 且 <= 20
        int teamNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if(teamNum < 1 || teamNum >20){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"队伍人数不满足要求");
        }
        //   2. 队伍标题 <= 20
        String title = team.getName();
        if(StringUtils.isBlank(title) || title.length() > 20){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"队伍名称不满足要求");
        }
        //   3. 描述 <= 512
        String desc = team.getDescription();
        if(StringUtils.isNotBlank(desc) && desc.length() > 512) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "描述太长");
        }

        //   4. status 是否公开（int）不传默认为 0（公开）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        //   5. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if(teamStatusEnum == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"队伍状态不满足要求");
        }
        if(TeamStatusEnum.TEAM_STATUS_ENCRYPTION.equals(teamStatusEnum)){
            String teamPassword = team.getPassword();
            if(StringUtils.isBlank(teamPassword) || teamPassword.length() > 32){
                throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"密码不满足要求");
            }
        }
        //   6. 超时时间 > 当前时间
        long time = team.getExpireTime().getTime();
        if(System.currentTimeMillis() >= time){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"超时时间异常");
        }
        //   7. 校验用户最多创建 5 个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        Long teamCount = teamMapper.selectCount(queryWrapper);
        if(teamCount >= 5){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"单个用户最多创建5个用户");
        }
        team.setId(null);
        team.setUserId(loginUser.getId());
        //4. 插入队伍信息到队伍表
        int inserted = teamMapper.insert(team);
        Long teamId = team.getId();
        if(inserted < 0 || teamId == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"创建队伍失败");
        }
        //5. 插入用户  => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(team.getUserId());
        userTeam.setTeamId(team.getId());
        userTeam.setJoinTime(new Date());
        inserted = userTeamMapper.insert(userTeam);
        if(inserted < 0){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"创建队伍失败");
        }
        return teamId;
    }

    /**
     * 查询队伍列表
     * @param teamQuery
     * @param isAdmin
     * @return
     */

    public List<TeamUserVO> listTeams(TeamQuery teamQuery, Boolean isAdmin) {
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        //判断查询条件
        if(teamQuery != null){
            //id
            Long id = teamQuery.getId();
            if(id != null && id > 0){
                teamQueryWrapper.eq("id",id);
            }
            //idList
            List<Long> idList = teamQuery.getIdList();
            if(!CollectionUtils.isEmpty(idList)){
                teamQueryWrapper.in("id",idList);
            }

            //名称
            String teamName = teamQuery.getName();
            if(StringUtils.isNotBlank(teamName)){
                teamQueryWrapper.like("name",teamName);
            }
            //描述
            String description = teamQuery.getDescription();
            if(StringUtils.isNotBlank(description)){
                teamQueryWrapper.like("description",description);
            }
            //关键词搜索
            String searchText = teamQuery.getSearchText();
            if(StringUtils.isNotBlank(searchText)){
                teamQueryWrapper.and(qw -> qw.like("name",searchText).or().like("description",searchText));
            }
            //最大人数
            Integer maxNum = teamQuery.getMaxNum();
            if(maxNum != null && maxNum > 0){
                teamQueryWrapper.eq("maxNum",maxNum);
            }
            //用户id
            Long userId = teamQuery.getUserId();
            if(userId != null && userId > 0){
                teamQueryWrapper.eq("userId",userId);
            }
            // 状态 如果是管理员，所有状态的队伍都可查看
            Integer status = teamQuery.getStatus();
            TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
            //默认设置为公共访问状态 todo查看创建队伍，可能其它除公开状态的队伍看不到
            if(enumByValue == null){
               enumByValue = TeamStatusEnum.TEAM_STATUS_PUBLIC;
            }
            //不是管理员 同时 队伍状态私密
            if(!isAdmin && TeamStatusEnum.TEAM_STATUS_PRIVATE.equals(enumByValue)) {
                throw new BusinessException(ErrorCodeEnum.NOT_AUTH);
            }
            //为什么不直接传status而是通过TeamStatusEnum对象获取，主要是怕status为null
            teamQueryWrapper.eq("status",enumByValue.getStatusValue());
        }
        //不显示过期的队伍
        teamQueryWrapper.and(qw -> qw.gt("expireTime",new Date()).or().isNull("expireTime"));

        List<Team> teamList = teamMapper.selectList(teamQueryWrapper);
        if(CollectionUtils.isEmpty(teamList)){
            return new ArrayList<>();
        }
        //关联查询创建人的信息
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long userId = team.getUserId();
            if(userId == null){
                continue;
            }
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team,teamUserVO);
            User user = userService.getById(userId);
            if(user != null){
                UserVO userVO = new UserVO();
                BeanUtils.copyProperties(user,userVO);
                teamUserVO.setCreateUser(userVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    public boolean updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        //查询当前队伍的用户id
        Long id = teamUpdateRequest.getId();
        if(id == null || id <= 0){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Team oldTeam = teamMapper.selectById(id);
        if(oldTeam == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"队伍不存在");
        }
        //获取用户id
        Long userId = oldTeam.getUserId();
        if(!userService.isAdmin(loginUser) && userId != loginUser.getId()){
            throw new BusinessException(ErrorCodeEnum.NOT_AUTH);
        }
        //新值和老值不一致 不需要修改 todo

        //如果更新状态 需要判断密码
        Integer status = teamUpdateRequest.getStatus();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        if(TeamStatusEnum.TEAM_STATUS_ENCRYPTION.equals(enumByValue) && StringUtils.isBlank(teamUpdateRequest.getPassword())){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"加密房间必须设置密码");
        }
        //更新
        Team updateTeam  = new Team();
        BeanUtils.copyProperties(teamUpdateRequest,updateTeam);
        int i = teamMapper.updateById(updateTeam);
        return i > 0;
    }

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if(teamJoinRequest == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team willJoinTeam = teamMapper.selectById(teamId);
        if(willJoinTeam == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"查询队伍不存在");
        }
        //过期时间
        Date expireTime = willJoinTeam.getExpireTime();
        if(expireTime != null && new Date().after(expireTime)){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"队伍已过期");
        }
        //加入队伍是否私密
        Integer status = willJoinTeam.getStatus();
        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
        if(TeamStatusEnum.TEAM_STATUS_PRIVATE.equals(enumByValue)){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"私有队伍不能加入");
        }
        //加密队伍必须提供正确密码
        if(TeamStatusEnum.TEAM_STATUS_ENCRYPTION.equals(enumByValue) && (StringUtils.isBlank(willJoinTeam.getPassword()) || (!willJoinTeam.getPassword().equals(teamJoinRequest.getPassword())))){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"需要密码");
        }

        /**
         * 需要同步锁
         * 步骤：1、尝试获取锁 2、等待 or 获取锁 3、执行完进入finally解锁 等待进程获取资源
         */
        RLock lock = redissonClient.getLock("accompanying:joinTeam");
        //尝试获取锁 等待or获取 debug模式下线程会直接判定为宕机，无法认定为等待，所以不会续期
        try {
            /**
             * @param1 long l等待时间
             * @param2 long l1持有锁时间
             * @param3 TimeUnit timeUnit时间单位
             */
            Long userId = loginUser.getId();
            //优化todo 可能会有更好的解决方法
            while (true) {
                //未获取到锁的一直等待
                if (lock.tryLock(0,-1,TimeUnit.MICROSECONDS)) {
                    //查加入队伍用户数 查队伍_用户关系表
                    QueryWrapper queryWrapper = new QueryWrapper();
                    queryWrapper.eq("userId", userId);
                    long count = userTeamService.count(queryWrapper);
                    if (count >= 5) {
                        throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "已加入过多队伍");
                    }
                    //查看是否重复加入
                    QueryWrapper reQueryWrapper = new QueryWrapper();
                    reQueryWrapper.eq("userId", userId);
                    reQueryWrapper.eq("teamId", teamId);
                    long hasJoinTeamNum = userTeamService.count(reQueryWrapper);
                    if (hasJoinTeamNum > 0) {
                        throw new BusinessException(ErrorCodeEnum.PARAM_ERROR, "已加入该队伍");
                    }

                    //查询该队伍是否已经加满人数
                    QueryWrapper userTeamQueryWrapper = new QueryWrapper();
                    userTeamQueryWrapper.eq("teamId",teamId);
                    long hasTeamJoinMaxNum = userTeamService.count(userTeamQueryWrapper);
                    if(hasTeamJoinMaxNum >= willJoinTeam.getMaxNum()){
                        throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"该队伍已加满人数");
                    }
                    //加入成功 更新队伍_用户表
                    UserTeam userTeam = new UserTeam();
                    userTeam.setTeamId(teamId);
                    userTeam.setUserId(userId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            //出现异常，加入失败
            System.err.println("join error"+e);
            return false;
        } finally {
            //只能释放自己的锁
            if(lock.isHeldByCurrentThread()) {
                System.out.println(Thread.currentThread().getName() + "：unlock");
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTeam(TeamDeleteRequest teamDeleteRequest,User loginUser) {
        if(teamDeleteRequest == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }

        //查询队伍是否存在
        long teamId = teamDeleteRequest.getTeamId();
        Team willDeleteTeam = teamMapper.selectById(teamId);
        if(willDeleteTeam == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"队伍不存在");
        }
        //查询操作用户是不是队长
        long userId = loginUser.getId();
        if(willDeleteTeam.getUserId() != userId){
            throw new BusinessException(ErrorCodeEnum.NOT_AUTH,"该用户无权限");
        }
        //删除队伍并接触队伍用户关系表
        QueryWrapper userTeamQueryWrapper = new QueryWrapper();
        userTeamQueryWrapper.eq("teamId",teamId);
        boolean remove = userTeamService.remove(userTeamQueryWrapper);
        int deleteResult = teamMapper.deleteById(teamId);
        if(deleteResult <= 0 || remove == false){
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR,"删除失败");
        }
        return deleteResult > 0;
    }

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
            if(teamQuitRequest == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        long userId = loginUser.getId();
        //检查队伍是否存在
        long teamId = teamQuitRequest.getTeamId();
        Team willQuitTeam = teamMapper.selectById(teamId);
        if(willQuitTeam == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"队伍不存在");
        }
        //该用户是否已经加入队伍
        QueryWrapper userTeamQueryWrapper = new QueryWrapper();
        userTeamQueryWrapper.eq("userId",userId);
        userTeamQueryWrapper.eq("teamId",teamId);
        UserTeam hasJoinTeam = userTeamService.getOne(userTeamQueryWrapper);
        if(hasJoinTeam == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL,"未加入队伍");
        }

        //查看队伍有多少用户 只有一个用户就解散队伍
        QueryWrapper userTeamCountUserQueryWrapper = new QueryWrapper();
        userTeamCountUserQueryWrapper.eq("teamId",teamId);
        long hasUserNum = userTeamService.count(userTeamCountUserQueryWrapper);
        if(hasUserNum == 1){
            //解散用户_队伍关系表
            boolean removeUserTeam = userTeamService.remove(userTeamCountUserQueryWrapper);
            //解散队伍表
            int removeTeam = teamMapper.deleteById(teamId);
            if(removeUserTeam == false || removeTeam <= 0){
                throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR,"删除队伍失败");
            }
        }else{
            //这里直接分支
            //队长退出 涉及队长位置传递
            //获取加入时间，将队长传递给最早加入的用户 这里只用查前两条数据
            //检验是否是队长
            if(willQuitTeam.getUserId() == userId){
                //是队长
                //limit过滤前两条数据
                userTeamCountUserQueryWrapper.last("order by id asc limit 1,1");
                UserTeam userTeam = userTeamMapper.selectOne(userTeamCountUserQueryWrapper);
                //修改队伍表
                if(userTeam == null){
                    throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR,"队长传递有误");
                }
                willQuitTeam.setUserId(userTeam.getUserId());
                int updateCaptain = teamMapper.updateById(willQuitTeam);
                if(updateCaptain <= 0){
                    throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR,"更新队长失败");
                }
            }
        }

        //退出队伍
        return userTeamService.remove(userTeamQueryWrapper);
    }


}




