package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.events.Event;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    // 缓存穿透
    public void set(String key , Object value , Long time , TimeUnit unit) {
        // 写入redis
        stringRedisTemplate.opsForValue().set(key , JSONUtil.toJsonStr(value), time , unit);
    }

    // 逻辑过期
    public void setWithLogicalExpire(String key , Object value , Long time , TimeUnit unit) {
        // 设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // 写入redis
        stringRedisTemplate.opsForValue().set(key , JSONUtil.toJsonStr(redisData));

    }

    // 缓存穿透
    public <R , ID> R queryWithPassThrough(String keyPrefix, ID id , Class<R> type , Function<ID, R> dbFallback ,Long time , TimeUnit unit ) {
        String key = keyPrefix + id;

        // 1.从redis查询缓存
        String json = stringRedisTemplate.opsForValue().get(key);

        // 2.判断是否存在（店铺数据）
        if (StrUtil.isNotBlank(json)) {
            // 3.存在，直接返回信息
            // 字符串反序列为java对象
            return JSONUtil.toBean(json, type);
        }

        //判断命中的是否是空值 （空字符串）
        if (json != null) {
            // 返回错误信息
            return null;
        }

        // 两个都没有就去查数据库 null

        //4.不存在，根据id查询数据库
        // 函数式接口完成数据库查询
        R r = dbFallback.apply(id);

        //5.不存在，返回错误信息
        if (r == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key , "" , CACHE_NULL_TTL , TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }

        //6.存在，将数据写入redis,转成json字符串存入
        this.set(key , r , time , unit);

        // 7. 返回
        return r;
    }




    // 线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    // 缓存击穿（逻辑过期）
    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type,Function<ID,R> dbFallback, Long time, TimeUnit unit) {

        String key = keyPrefix + id;

        // 1.从redis查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 2.判断是否存在（店铺数据）
        if (StrUtil.isBlank(shopJson)) {
            // 不存在，返回null
            return null;
        }

        // 4.命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //5.1未过期，直接返回店铺信息
            return r;
        }

        //5.2过期

        //6.缓存重建
        //6.1获取互斥锁

        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        //6.2判断是否获取锁成功
        if (isLock) {
            //TODO 6.3成功，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //查询数据库
                    R r1 = dbFallback.apply(id);

                    // 写入redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }

        //6.4失败，返回过期的店铺信息
        return r;
    }

    // 上锁
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    // 释放锁
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
