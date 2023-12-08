package com.lwj.canal.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author liwin
 * redis连接工具类
 */
public class RedisUtils {
    //redis连接地址
    public static final String  REDIS_IP_ADDR = "192.168.159.130";
    //redis连接密码
    public static final String  REDIS_pwd = "123456";
    //定义redis连接池
    public static JedisPool jedisPool;

    //静态代码块，连接池相关配置
    static {
        //new池配置对象
        JedisPoolConfig jedisPoolConfig=new JedisPoolConfig();
        //最大连接数
        jedisPoolConfig.setMaxTotal(20);
        //最大连接id长度
        jedisPoolConfig.setMaxIdle(10);
        //配置连接池参数：配置对象，地址，端口号，时间，密码
        jedisPool=new JedisPool(jedisPoolConfig,REDIS_IP_ADDR,6379,10000,REDIS_pwd);
    }

    //get连接方法
    public static Jedis getJedis() throws Exception {
        if(null!=jedisPool){
            return jedisPool.getResource();
        }
        throw new Exception("Jedispool is not ok");
    }
}
