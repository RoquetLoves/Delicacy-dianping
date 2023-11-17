package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryShopList() {
        String key = "shop-type";
        //1.从redis里查询商品类型数据
        String List = stringRedisTemplate.opsForValue().get(key);


        //json转list
        List<ShopType> shopTypes = JSONUtil.toList(List, ShopType.class);


        // 存在直接返回
        if (!CollectionUtils.isEmpty(shopTypes)) {
            return Result.ok(shopTypes);
        }


        // 不存在，从数据库中查询商铺类型，并根据sort排序
        List<ShopType> shopType = query().orderByAsc("sort").list();

        //不存在返回404
        if (shopType == null) {
            return Result.fail("数据错误！404");
        }


        // 存在，将商铺类型存储到redis中
        stringRedisTemplate.opsForValue().set(key , JSONUtil.toJsonStr(shopType));

        return Result.ok(shopType);
    }
}
