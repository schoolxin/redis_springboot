package com.atguigu.redis.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/redisTest")
public class RedisTestController {

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping
    public String testRedis()
    {
        System.out.println("hhha");
        redisTemplate.opsForValue().set("names","lucy");

        String name = (String)redisTemplate.opsForValue().get("names");

        return name;
    }

    @GetMapping("doSecKill")
    //秒杀过程
    public boolean doSecKill() throws IOException {
        //1 uid和prodid非空判断
        String uid = new Random().nextInt(50000) +"" ;
        String prodid = "10016";
        if(uid == null || prodid == null) {
            return false;
        }



        //3 拼接key
        // 3.1 库存key
        String kcKey = "sk:"+prodid+":qt";
        // 3.2 秒杀成功用户key
        String userKey = "sk:"+prodid+":user";

        //监视库存
        redisTemplate.watch(kcKey);

        //4 获取库存，如果库存null，秒杀还没有开始
        Integer kc = (Integer)redisTemplate.opsForValue().get(kcKey);



        if(kc == null) {
            System.out.println("秒杀还没有开始，请等待");
            return false;
        }

        // 5 判断用户是否重复秒杀操作
        if(redisTemplate.opsForSet().isMember(userKey, uid)) {
            System.out.println("已经秒杀成功了，不能重复秒杀");
            return false;
        }

        //6 判断如果商品数量，库存数量小于1，秒杀结束
        if(kc<=0) {
            System.out.println("秒杀已经结束了");
            return false;
        }
        //7 秒杀过程
        //使用事务

        redisTemplate.multi();
        //7.1 库存-1
        redisTemplate.opsForValue().decrement(kcKey);
        //7.2 把秒杀成功用户添加清单里面

        redisTemplate.opsForSet().add(userKey,uid);

        //执行exec
        List results = redisTemplate.exec();

        if(results==null||results.size()==0)
        {
            System.out.println("秒杀失败了..");
            return  false;
        }

        System.out.println("秒杀成功了..");
        return true;
    }
}
