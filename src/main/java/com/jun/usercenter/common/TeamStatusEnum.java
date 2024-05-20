package com.jun.usercenter.common;

import com.jun.usercenter.excption.BusinessException;

/**
 * 队伍状态枚举
 * @author bh
 */
public enum TeamStatusEnum {
    
    TEAM_STATUS_PUBLIC(0,"队伍公开"),
    TEAM_STATUS_PRIVATE(1,"队伍私密"),
    TEAM_STATUS_ENCRYPTION(2,"队伍加密");
    public static TeamStatusEnum getEnumByValue(Integer value){
        if(value == null){
            return null;
        }
        TeamStatusEnum[] values = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : values) {
            if(teamStatusEnum.getStatusValue() == value){
                return teamStatusEnum;
            }
        }
        return null;
    }
    
    private int statusValue;
    private String statusText;

    TeamStatusEnum(int statusValue, String statusText) {
        this.statusValue = statusValue;
        this.statusText = statusText;
    }

    public int getStatusValue() {
        return statusValue;
    }


    public String getStatusText() {
        return statusText;
    }
}
