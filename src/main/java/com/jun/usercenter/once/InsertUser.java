package com.jun.usercenter.once;
import java.util.Date;


import com.jun.usercenter.model.domain.User;
import org.springframework.stereotype.Component;

@Component
public class InsertUser {
    public void doUser(){
        User fakeUser = new User();
        fakeUser.setUsername("fakeUser");
        fakeUser.setUserAccount("");
        fakeUser.setAvatarUrl("");
        fakeUser.setGender(0);
        fakeUser.setUserPassword("");
        fakeUser.setPhone("");
        fakeUser.setEmail("");
        fakeUser.setUserStatus(0);
        fakeUser.setPlanetCode(0);
        fakeUser.setTags("[]");


    }
}
