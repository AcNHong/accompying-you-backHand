package com.jun.usercenter;

import com.google.gson.Gson;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;
import java.util.*;


@SpringBootTest
public class UserCenterApplicationTest {

    @Resource
    private UserService userService;
    @Test
    void testContent(){
        List<String> list = new ArrayList<>();
        Collections.addAll(list,"JAVA","PYTHON");

        List<User> users = userService.searchUserByTags(list);
        System.out.println(users);
    }

    @Test
    void testJson(){
        List<String> list = Arrays.asList("C#","C++","SHELL","PHP","PYTHON","GO","JAVA");
        Gson gson = new Gson();
        System.out.println(gson.toJson(list));


    }
    @Test
    void testDateGetTime(){
        long time  = System.currentTimeMillis();
        Date date = new Date();
        date.setTime(time);
        System.out.println(date);
        System.out.println(date.getTime());
    }


}
