package com.lwj.mylock;

import cn.hutool.core.util.IdUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

//@Component 工厂直取
public class RedisDistributedLock implements Lock {

    private StringRedisTemplate stringRedisTemplate;

    private String lockName;//KEYS[1]
    private String uuidValue;//ARGV[1]
    private long   expireTime;//ARGV[2]

    public RedisDistributedLock(StringRedisTemplate stringRedisTemplate, String lockName, String uuidValue)
    {
        this.stringRedisTemplate = stringRedisTemplate;
        this.lockName = lockName;
        //this.uuidValue = IdUtil.simpleUUID()+":"+Thread.currentThread().getId();//UUID:ThreadID
        this.uuidValue = uuidValue+":"+Thread.currentThread().getId();//UUID:ThreadID
        this.expireTime = 30L;

    }

    @Override
    public void lock() {
        tryLock();

    }

    @Override
    public boolean tryLock() {
        try {
            tryLock(-1L,TimeUnit.SECONDS);} catch (InterruptedException e) {throw new RuntimeException(e);}
        return false;
    }

    /**
     * 真正干活的，实现加锁功能
     * @param time
     * @param unit
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        if(time != -1L){
            this.expireTime = unit.toSeconds(time);
        }
        String script =
                "if redis.call('exists',KEYS[1]) == 0 or redis.call('hexists',KEYS[1],ARGV[1]) == 1 then " +
                        "redis.call('hincrby',KEYS[1],ARGV[1],1) " +
                        "redis.call('expire',KEYS[1],ARGV[2]) " +
                        "return 1 " +
                        "else " +
                        "return 0 " +
                        "end";

        System.out.println("script: "+script);
        System.out.println("unlockName: "+lockName+",\t"+"uuidValue: "+uuidValue+",\t"+"expireTime: "+expireTime);

        while (!stringRedisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class),
                Arrays.asList(lockName),
                uuidValue,
                String.valueOf(expireTime))){
            //自旋重试，等待50ms
            TimeUnit.MILLISECONDS.sleep(50);
        }
        resetExpire();
        return true;
    }

    private void resetExpire() {
        String script =
                "if redis.call('HEXISTS',KEYS[1],ARGV[1]) == 1 then " +
                        "return redis.call('expire',KEYS[1],ARGV[2]) " +
                        "else " +
                        "return 0 " +
                        "end";
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (stringRedisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName),uuidValue,String.valueOf(expireTime))) {
                    resetExpire();
                }
            }
        },expireTime/3*1000);
    }

    /**
     *实现解锁功能
     */
    @Override
    public void unlock() {
        //注意lua脚本空格
        String script =
                "if redis.call('HEXISTS',KEYS[1],ARGV[1]) == 0 then " +
                        "return nil " +
                        "elseif redis.call('HINCRBY',KEYS[1],ARGV[1],-1) == 0 then " +
                        "return redis.call('del',KEYS[1]) " +
                        "else " +
                        "return 0 " +
                        "end";
        System.out.println("unlockName: "+lockName+",\t"+"uuidValue: "+uuidValue+",\t"+"expireTime: "+expireTime);
        // nil = false = 0; true = 1
        Long flag = stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuidValue, String.valueOf(expireTime));
        if(flag == null)
        {
            throw new RuntimeException("没有这个锁，HEXISTS查询无");
        }
    }

    // 忽略下面两个方法
    @Override
    public Condition newCondition() {
        return null;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }
}
