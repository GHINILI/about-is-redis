package com.lwj.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
@Slf4j
public class BloomFilterInit {
    @Resource
    private RedisTemplate redisTemplate;

    @PostConstruct//初始化白名单数据
    public void init() {
        for (int i = 0; i < 100; i++) {
            //预加载白名单
            String uid = "customer:" + i;
            //1.计算hashcode，取绝对值排除负数影响
            int hashValue = Math.abs(uid.hashCode());
            //2.取hashValue的2的32次方的余数，获得坑位
            long index = (long) (hashValue % Math.pow(2, 32));
            //2.1后台打印坑位
            log.info(uid + "对应坑位index：{}",index);
            //3.设置redis的bitmap对应坑位，置为1
           redisTemplate.opsForValue().setBit("whitelistCustomer",index,true);
        }
    }
}
