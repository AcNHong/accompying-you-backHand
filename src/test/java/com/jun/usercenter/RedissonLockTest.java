package com.jun.usercenter;

import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class RedissonLockTest {
    @Resource
    private RedissonClient redisson;
    @Test
    void doTestLock(){
        RLock myLock = redisson.getLock("myLock");
        try {
            if(myLock.tryLock(0,-1, TimeUnit.MICROSECONDS)){
                //todo something
                long threadId = Thread.currentThread().getId();
                System.out.printf("getLock:%d",threadId);
            }
        } catch (InterruptedException e) {
            System.err.println("error lock");
        } finally {
            //释放锁
            if (myLock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                myLock.unlock();
            }
        }

    }

}
