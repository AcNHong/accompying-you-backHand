package com.jun.usercenter.common;

import lombok.Data;
import java.io.Serializable;

/**
 * 通用分页请求参数
 *
 * @author bh
 *
 */

@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = -5860707094194210841L;
    /**
     * 每页项目条数
     */
    protected int pageSize;

    /**
     * 当前第几页
     */
    protected int pageNum;
}
