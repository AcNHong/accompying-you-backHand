package com.jun.usercenter.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jun.usercenter.model.domain.Team;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.model.dto.TeamQuery;
import com.jun.usercenter.model.request.TeamDeleteRequest;
import com.jun.usercenter.model.request.TeamJoinRequest;
import com.jun.usercenter.model.request.TeamQuitRequest;
import com.jun.usercenter.model.request.TeamUpdateRequest;
import com.jun.usercenter.model.vo.TeamUserVO;

import java.util.List;


/**
* @author 14136
* @description 针对表【Team(队伍表)】的数据库操作Service
* @createDate 2024-04-16 17:43:08
*/
public interface TeamService extends IService<Team> {
    /**
     * 新增队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 查询队伍列表
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, Boolean isAdmin);

    /**
     * 更新队伍
     *
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 解散队伍
     * @param teamDeleteRequest
     * @param loginUser
     * @return
     */
    boolean deleteTeam(TeamDeleteRequest teamDeleteRequest,User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);


}
