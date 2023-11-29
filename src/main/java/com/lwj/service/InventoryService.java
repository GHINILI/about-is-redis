package com.lwj.service;


import ch.qos.logback.core.util.TimeUtil;
import cn.hutool.core.util.IdUtil;
import com.lwj.mylock.DistributedLockFactory;
import com.lwj.mylock.RedisDistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;

@Service
@Slf4j
public class InventoryService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Value("${server.port}")
    private String port;

    @Autowired
    private DistributedLockFactory distributedLockFactory;

    /* V2.0 单机版线程锁，在分布式中失效
    private Lock lock = new ReentrantLock();

    public String sale() {

        String retMessage = "";

        lock.lock();

        //1.查询库存
        String result = stringRedisTemplate.opsForValue().get("inventory001");

        //2.判断库存
        Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);

        if(inventoryNum > 0){
            stringRedisTemplate.opsForValue().set("inventory001", String.valueOf(--inventoryNum));
            retMessage = "成功卖出一件商品，剩余" + inventoryNum;
            System.out.println(retMessage);
        }else {
            retMessage = "暂无库存";
        }
        lock.unlock();
        return retMessage+"\t"+"服务端口号："+port;
    }
     */

    /* V3.1 问题1，递归重试容易导致stackoverflowerror，
            问题2，高并发下if存在虚假唤醒问题

     public String sale() {

        String retMessage = "";
        //定义锁门名
        String key = "RedisLock";
        //定义锁value
        String value = IdUtil.fastSimpleUUID() + ":" + Thread.currentThread().getId();
        //获得锁 setnx
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,value);
        //获得锁失败
        if(!flag) {
            //等待20ms，递归再次尝试获得锁
            try {TimeUnit.MILLISECONDS.sleep(20);} catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            sale();
        }else {
            //1.查询库存
            String result = stringRedisTemplate.opsForValue().get("inventory001");

            //2.判断库存
            Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);

            if(inventoryNum > 0){
                stringRedisTemplate.opsForValue().set("inventory001", String.valueOf(--inventoryNum));
                retMessage = "成功卖出一件商品，剩余" + inventoryNum;
                System.out.println(retMessage);
            }else {
                retMessage = "暂无库存";
            }
            //删除锁
            stringRedisTemplate.delete(key);
        }
        return retMessage+"\t"+"服务端口号："+port;
    }
     */

    /*
    V4.1 问题：存在加锁解锁不一致
    public String sale() {

        String retMessage = "";
        //定义锁门名
        String key = "RedisLock";
        //定义锁value
        String value = IdUtil.fastSimpleUUID() + ":" + Thread.currentThread().getId();
        //获得锁 setnx
        //while替换if，自旋替换递归
        while (!(stringRedisTemplate.opsForValue().setIfAbsent(key,value,10L,TimeUnit.SECONDS))) {
            //等待20ms，递归再次尝试获得锁
            try {TimeUnit.MILLISECONDS.sleep(20);} catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        //1.查询库存
        String result = stringRedisTemplate.opsForValue().get("inventory001");

        //2.判断库存
        Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);

        if(inventoryNum > 0){
            stringRedisTemplate.opsForValue().set("inventory001", String.valueOf(--inventoryNum));
            retMessage = "成功卖出一件商品，剩余" + inventoryNum;
            System.out.println(retMessage);
        }else {
            retMessage = "暂无库存";
        }
        //删除锁
        stringRedisTemplate.delete(key);
        return retMessage+"\t"+"服务端口号："+port;
    }
     */

    /*
    V5.1 问题：最后判断锁和删除锁非原子性
    public String sale() {

        String retMessage = "";
        //定义锁门名
        String key = "RedisLock";
        //定义锁value
        String value = IdUtil.fastSimpleUUID() + ":" + Thread.currentThread().getId();
        //获得锁 setnx
        //while替换if，自旋替换递归
        while (!(stringRedisTemplate.opsForValue().setIfAbsent(key,value,10L,TimeUnit.SECONDS))) {
            //等待20ms，递归再次尝试获得锁
            try {TimeUnit.MILLISECONDS.sleep(20);} catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        //1.查询库存
        String result = stringRedisTemplate.opsForValue().get("inventory001");

        //2.判断库存
        Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);

        if(inventoryNum > 0){
            stringRedisTemplate.opsForValue().set("inventory001", String.valueOf(--inventoryNum));
            retMessage = "成功卖出一件商品，剩余" + inventoryNum;
            System.out.println(retMessage);
        }else {
            retMessage = "暂无库存";
        }
        //删除锁，修复加锁解锁不一致问题
        if(stringRedisTemplate.opsForValue().get(key).equalsIgnoreCase(value)){
            stringRedisTemplate.delete(key);
        }
        return retMessage+"\t"+"服务端口号："+port;
    }
     */

    /*
    V6.1 问题：不满足可重入性原则
    public String sale() {

        String retMessage = "";
        //定义锁门名
        String key = "RedisLock";
        //定义锁value
        String value = IdUtil.fastSimpleUUID() + ":" + Thread.currentThread().getId();
        //获得锁 setnx
        //while替换if，自旋替换递归
        while (!(stringRedisTemplate.opsForValue().setIfAbsent(key,value,10L,TimeUnit.SECONDS))) {
            //等待20ms，递归再次尝试获得锁
            try {TimeUnit.MILLISECONDS.sleep(20);} catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        //1.查询库存
        String result = stringRedisTemplate.opsForValue().get("inventory001");

        //2.判断库存
        Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);

        if(inventoryNum > 0){
            stringRedisTemplate.opsForValue().set("inventory001", String.valueOf(--inventoryNum));
            retMessage = "成功卖出一件商品，剩余" + inventoryNum;
            System.out.println(retMessage);
        }else {
            retMessage = "暂无库存";
        }
        //更新用lua脚本实现原子性
        String luaScript =
                "if (redis.call('get',KEYS[1]) == ARGV[1]) then " +
                        "return redis.call('del',KEYS[1]) " +
                        "else " +
                        "return 0 " +
                        "end";
        stringRedisTemplate.execute(new DefaultRedisScript<>(luaScript,Boolean.class), Arrays.asList(key),value);

        return retMessage+"\t"+"服务端口号："+port;
    }
     */

    /*
    v7.2，存在锁过期时间可能少于业务时间的问题

    //private Lock lock = new ReentrantLock();
    //考虑扩展性，引入工厂模式
    //private Lock mylock = new RedisDistributedLock(stringRedisTemplate,"redisLock");

    public String sale() {

        String retMessage = "";
        //工厂在方法里面
        Lock redisLock = distributedLockFactory.getDistributedLock("redis");
        //lock.lock();
        redisLock.lock();

        //1.查询库存
        String result = stringRedisTemplate.opsForValue().get("inventory001");

        //2.判断库存
        Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);

        if(inventoryNum > 0){
            stringRedisTemplate.opsForValue().set("inventory001", String.valueOf(--inventoryNum));
            retMessage = "成功卖出一件商品，剩余" + inventoryNum;
            System.out.println(retMessage);
            this.testReEnter();
        }else {
            retMessage = "暂无库存";
        }
        //lock.unlock();
        redisLock.unlock();
        return retMessage+"\t"+"服务端口号："+port;
    }

    private void testReEnter()
    {
        Lock redisLock = distributedLockFactory.getDistributedLock("redis");
        redisLock.lock();
        try
        {
            System.out.println("################测试可重入锁####################################");
        }finally {
            redisLock.unlock();
        }
    }
     */

    //V8.0
    public String sale() {

        String retMessage = "";
        //工厂在方法里面
        Lock redisLock = distributedLockFactory.getDistributedLock("redis");
        //lock.lock();
        redisLock.lock();

        //1.查询库存
        String result = stringRedisTemplate.opsForValue().get("inventory001");

        //2.判断库存
        Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);

        if(inventoryNum > 0){
            stringRedisTemplate.opsForValue().set("inventory001", String.valueOf(--inventoryNum));
            retMessage = "成功卖出一件商品，剩余" + inventoryNum;
            System.out.println(retMessage);

        }else {
            retMessage = "暂无库存";
        }
        //lock.unlock();
        redisLock.unlock();
        return retMessage+"\t"+"服务端口号："+port;
    }


    // V9.0 引入redisson的redlock实现类
    // V9.1 修复解锁异常问题，增加解锁校验
    @Autowired
    private Redisson redisson;
    public String saleByRedisson() {
        String retMessage = "";

        RLock redissonLock = redisson.getLock("RedisLock");
        redissonLock.lock();

        try{
            //1.查询库存
            String result = stringRedisTemplate.opsForValue().get("inventory001");

            //2.判断库存
            Integer inventoryNum = result == null ? 0 : Integer.parseInt(result);

            if(inventoryNum > 0){
                stringRedisTemplate.opsForValue().set("inventory001", String.valueOf(--inventoryNum));
                retMessage = "成功卖出一件商品，剩余" + inventoryNum;
                System.out.println(retMessage);

            }else {
                retMessage = "暂无库存";
            }
        }finally {
            //lock.unlock();
            //redissonLock.unlock();
            //解锁校验，锁必须是锁定的状态且是自己的锁
            if(redissonLock.isLocked() && redissonLock.isHeldByCurrentThread())
            {
                redissonLock.unlock();
            }
        }
        return retMessage+"\t"+"服务端口号："+port;
    }
}
