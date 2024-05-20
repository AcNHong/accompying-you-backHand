package com.jun.usercenter.model.request;

import lombok.Data;

/**
 * 加入队伍请求封装类
 * @author bh
 */
@Data
public class TeamJoinRequest {
    private Long teamId;

    private String password;
}
