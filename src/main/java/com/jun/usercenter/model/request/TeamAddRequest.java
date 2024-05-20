package com.jun.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 创建队伍请求体
 * @author bh
 */
@Data
public class TeamAddRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120792L;
    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 密码
     */
    private String password;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 创建时间
     */
    private Date expireTime;

    /**
     * 用户id
     */
    private Long userId;

    /**
     *  0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;
}
