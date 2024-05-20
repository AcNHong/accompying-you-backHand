package com.jun.usercenter.model.request;

import lombok.Data;
import java.io.Serializable;

/**
 * 删除队伍请求体
 * @author bh
 */
@Data
public class TeamDeleteRequest implements Serializable {
    private Long teamId;
}
