package com.jun.usercenter.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jun.usercenter.common.ErrorCodeEnum;
import com.jun.usercenter.excption.BusinessException;
import com.jun.usercenter.model.domain.Team;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.model.domain.UserTeam;
import com.jun.usercenter.model.dto.TeamQuery;
import com.jun.usercenter.model.request.*;
import com.jun.usercenter.model.result.Result;
import com.jun.usercenter.model.vo.TeamUserVO;
import com.jun.usercenter.service.TeamService;
import com.jun.usercenter.service.UserService;
import com.jun.usercenter.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.jws.soap.SOAPBinding;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 队伍请求接口
 * @author beihong
 */
@RestController
@RequestMapping("/team")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000/"})
public class TeamController {
    @Resource
    private UserService userService;
    @Resource
    private TeamService teamService;
    @Resource
    private UserTeamService userTeamService;
    /**
     * 新增接口
     * @param teamAddRequest
     * @param request
     * @return 
     */
    @PostMapping("/add")
    public Result addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        if(teamAddRequest == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);
        User userLogin = userService.getUserLogin(request);
        long result = teamService.addTeam(team, userLogin);
        return Result.success(result);

    }

    @PostMapping("/delete")
    public Result deleteTeam(@RequestBody TeamDeleteRequest teamDeleteRequest, HttpServletRequest request){
        if(teamDeleteRequest == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        User loginUser = userService.getUserLogin(request);
        boolean delete = teamService.deleteTeam(teamDeleteRequest,loginUser);
        return Result.success(delete);
    }
    @PostMapping("/update")
    public Result updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,HttpServletRequest request){
        if(teamUpdateRequest == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        User loginUser = userService.getUserLogin(request);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if(!result){
            throw new BusinessException(ErrorCodeEnum.SYSTEM_ERROR);
        }
        return Result.success(result);
    }

    @PostMapping("/quit")
    public Result quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request){
        if(teamQuitRequest == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        User loginUser = userService.getUserLogin(request);
        boolean quit = teamService.quitTeam(teamQuitRequest,loginUser);
        return Result.success(quit);
    }
    @PostMapping("/join")
    public Result joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request){
        if(teamJoinRequest == null){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        User loginUser = userService.getUserLogin(request);

        boolean result = teamService.joinTeam(teamJoinRequest,loginUser);
        return Result.success();
    }

    @GetMapping("/get")
    public Result getTeamById(int id){
        if (id <= 0) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_NULL);
        }
        return Result.success(team);
    };

    @GetMapping("/list")
    public Result listTeams(TeamQuery teamQuery,HttpServletRequest request) {
        if(teamQuery == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        User loginUser = userService.getUserLogin(request);
        List<TeamUserVO> teamUserVOList = teamService.listTeams(teamQuery, isAdmin);
       //直接查询的list 通过比对userid筛选我加入的队伍。
        List<Long> teamIdList = teamUserVOList.stream().map(teamUserVO -> teamUserVO.getId()).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(teamIdList)){
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR,"空");
        }
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",loginUser.getId());
        userTeamQueryWrapper.in("teamId",teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        //遍历teamTeamList 获取teamId集合
        Set<Long> userTeamIdSet = userTeamList.stream().map(userTeam -> userTeam.getTeamId()).collect(Collectors.toSet());
        teamUserVOList.forEach(
                teamUserVO ->{
                    boolean hasJoinTeam = userTeamIdSet.contains(teamUserVO.getId());
                    teamUserVO.setHasJoin(hasJoinTeam);
                }
        );
        //获取该队伍的用户人数
        QueryWrapper<UserTeam> hasJoinNumUserTeamQueryWrapper = new QueryWrapper<>();
        hasJoinNumUserTeamQueryWrapper.in("teamId", teamIdList);
        //对teamUser列表分组，相同组别代表同样的队伍id下的不同用户列表 这样每个分组下就是不同的队伍人数
        Map<Long, List<UserTeam>> TeamIdUserTeamMap = userTeamService.list(hasJoinNumUserTeamQueryWrapper).stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamUserVOList.forEach(teamUserVO -> teamUserVO.setHasJoinNum(TeamIdUserTeamMap.getOrDefault(teamUserVO.getId(),new ArrayList<>()).size()));
        return Result.success(teamUserVOList);
    }

    /**
     * 我创建的队伍
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/myTeam")
    public Result listMyTeam(TeamQuery teamQuery,HttpServletRequest request){
        if(teamQuery == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        User loginUser = userService.getUserLogin(request);
        teamQuery.setUserId(loginUser.getId());
        //查看我创建的队伍 这里直接给管理员权限
        List<TeamUserVO> teamUserVOList = teamService.listTeams(teamQuery, true);
        return Result.success(teamUserVOList);
    }

    /**
     * 我加入的队伍
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/myJoin")
    public Result listMyJoinTeam(TeamQuery teamQuery,HttpServletRequest request){
        if(teamQuery == null) {
            throw new BusinessException(ErrorCodeEnum.PARAM_ERROR);
        }
        User loginUser = userService.getUserLogin(request);
        long userId = loginUser.getId();
        //查询我加入的队伍列表
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("userId",userId);
        List<UserTeam> list = userTeamService.list(userTeamQueryWrapper);
        Set<Long> idSet = list.stream().collect(Collectors.groupingBy(UserTeam::getTeamId)).keySet();
        ArrayList<Long> idList = new ArrayList<>(idSet);
        teamQuery.setIdList(idList);

        return Result.success(teamService.listTeams(teamQuery,true));
    }
    @GetMapping("/list/page")
    public Result listTeamsByPage(TeamQuery teamQuery){
        return null;
    }

}
