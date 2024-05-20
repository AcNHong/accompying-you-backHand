package com.jun.usercenter.job;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jun.usercenter.model.domain.User;
import com.jun.usercenter.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class PreCacheJob {
    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private RedissonClient redisson;

    @Scheduled(cron = "0 03 12 * * *")
    public void doCacheRecommend(){
        //拟重要用户id
        int id = 1;
        RLock lock = redisson.getLock("accompanying:precachejob:docache:lock");

        try {
            //尝试获取锁 等待or获取 debug模式下线程会直接判定为宕机，无法认定为等待，所以不会续期
            if (lock.tryLock(0,-1,TimeUnit.MICROSECONDS)) {
                log.info("currentThreadId:{}",Thread.currentThread().getId());
                //构造键
                String keyString = String.format("accompanying:user:recommend:%s", id);
                ValueOperations valueOperations = redisTemplate.opsForValue();
                //没找到信息 查库
                IPage<User> page = new Page<>(1,20);
                List<User> usersList = userService.list(page);

                log.info("cache successful---------------------------------------");

                //存入缓存
                try {
                    valueOperations.set(keyString,usersList,30000, TimeUnit.MICROSECONDS);
                } catch (Exception e) {
                    log.error("redis set key error:{}",e);
                }
            }
        } catch (InterruptedException e) {
            log.error("cache set error:{}",e.getMessage());
        } finally {
            //只能释放自己的锁
            if(lock.isHeldByCurrentThread()){
                log.info("currentThreadId:{}",Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

}
