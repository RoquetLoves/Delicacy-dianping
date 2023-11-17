package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*
*   每次请求都会刷新用户
*
* */

//TODO 拦截所有

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //TODO 1.获取请求头中的token
        String token = request.getHeader("authorization");

        // 判断是否为空
        if (StrUtil.isBlank(token)) {
            // 不存在，拦截 返回401状态码
            return true;
        }

      String key = RedisConstants.LOGIN_USER_KEY + token;

        //TODO 2.基于token获取redis中的用户信息
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

        //3.判断用户是否存在
        if (userMap.isEmpty()) {
            // 不存在，拦截 返回401状态码
            return true;
        }

        //TODO 5.将查询到的Hash数据转为UserDTO对象              // 遇到问题抛异常
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);


        //TODO 6. 存在，保存用户信息到ThreadLocal线程
        UserHolder.saveUser(userDTO);

        //TODO 7. 刷新token有效期
        stringRedisTemplate.expire(key , RedisConstants.LOGIN_USER_TTL , TimeUnit.MINUTES);

        //TODO 8.放行
        return true;
    }


    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
