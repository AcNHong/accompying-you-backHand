package com.jun.usercenter;
import com.alibaba.excel.EasyExcel;
import com.jun.usercenter.model.domain.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * 读的常见写法
 *
 * @author Jiaju Zhuang
 */

@Slf4j
public class ReadTest {

    /**
     * 最简单的读
     * 直接读即可
     */
    @Test
    public void simpleRead() {
        // 写法1：JDK8+ ,不用额外写一个PageListener
        // since: 3.0.0-beta1
        String fileName = "D:\\develop\\Spring-project-workspace\\user-center\\src\\main\\resources\\test.xlsx";
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 文件流会自动关闭
        // 这里默认每次会读取100条数据 然后返回过来 直接调用使用数据就行
        // 具体需要返回多少行可以在`PageReadListener`的构造函数设置
        EasyExcel.read(fileName, User.class, new TableListener()).sheet().doRead();


    }

    @SneakyThrows
    @Test
    void testDateFun(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String date ="2022-12-12";
        Date conStumParse = simpleDateFormat.parse(date);
        System.out.println(new Date().toString());
        System.out.println(new Date().after(conStumParse));

    }

}
