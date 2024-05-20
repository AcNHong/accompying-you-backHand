package com.jun.usercenter;

import com.jun.usercenter.common.TeamStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TeamStatusEnumTest {
    @Test
    void testEnumByValue(){
        System.out.println(TeamStatusEnum.TEAM_STATUS_ENCRYPTION.getStatusValue());
    }
}
