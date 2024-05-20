package com.jun.usercenter;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;



import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {
    /**
     * 测试redis缓存框架
     */
    @Resource
    RedisTemplate redisTemplate;

    @Test
    void cache(){
        ValueOperations valueOps = redisTemplate.opsForValue();
        valueOps.set("id9",1);
        Assert.assertEquals(valueOps.get("id9"),1);
    }
}
