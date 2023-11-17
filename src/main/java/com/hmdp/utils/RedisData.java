package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData<T>  {
    // 设置逻辑过期时间
    private LocalDateTime expireTime;
    private T data;
}
