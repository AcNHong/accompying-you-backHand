package com.jun.usercenter.crawl;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.service.UserService;
import com.jun.usercenter.service.impl.UserServiceImpl;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

// 有个很重要的点 TableListener 不能被spring管理，要每次读取excel都要new,然后里面用到spring可以构造方法传进去
@Slf4j
public class TableListener implements ReadListener<User> {



    List<User> userList = new ArrayList<>();
    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data    one row valu
     * @param context
     */
    @Override
    public void invoke(User data, AnalysisContext context) {
        userList.add(data);
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
    }

    private  void saveData(){
        UserService service = new UserServiceImpl();
        service.saveBatch(userList);
    }


}