package com.jun.usercenter;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jun.usercenter.mapper.TeamMapper;
import com.jun.usercenter.model.domain.Team;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class MPUpdateByIdTest {
    @Resource
    private TeamMapper teamMapper;
    @Test
    void testUpdateReType(){
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("id",100);
        Team team = new Team();
        team.setName("hhh");
        int result = teamMapper.update(team, queryWrapper);
        System.out.println(result);
    }
}
