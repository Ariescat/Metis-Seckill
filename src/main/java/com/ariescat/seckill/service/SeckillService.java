package com.ariescat.seckill.service;

import com.ariescat.seckill.bean.OrderInfo;
import com.ariescat.seckill.bean.SeckillOrder;
import com.ariescat.seckill.bean.User;
import com.ariescat.seckill.redis.RedisService;
import com.ariescat.seckill.redis.key.SeckillKeyPrefix;
import com.ariescat.seckill.util.MD5Util;
import com.ariescat.seckill.util.UUIDUtil;
import com.ariescat.seckill.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

@Service
public class SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    RedisService redisService;

    // 用于生成验证码中的运算符
    private char[] ops = new char[]{'+', '-', '*'};

    //保证这三个操作，减库存 下订单 写入秒杀订单是一个事务
    @Transactional
    public OrderInfo seckill(User user, GoodsVo goods) {
        // 减库存
        boolean success = goodsService.reduceStock(goods);
        if (success) {
            // 下订单 写入秒杀订单
            return orderService.createOrder(user, goods);
        } else {
            setGoodsOver(goods.getId());
            return null;
        }
    }

    public long getSeckillResult(long userId, long goodsId) {
        SeckillOrder order = orderService.getOrderByUserIdGoodsId(userId, goodsId);
        if (order != null) {
            return order.getOrderId();
        } else {
            boolean isOver = getGoodsOver(goodsId);
            if (isOver) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    private void setGoodsOver(Long goodsId) {
        redisService.set(SeckillKeyPrefix.isGoodsOver, "" + goodsId, true);
    }

    private boolean getGoodsOver(long goodsId) {
        return redisService.exists(SeckillKeyPrefix.isGoodsOver, "" + goodsId);
    }

    /**
     * 创建验证码
     */
    public BufferedImage createVerifyCode(User user, long goodsId) {
        if (user == null || goodsId <= 0) {
            return null;
        }

        // 验证码的宽高
        int width = 80;
        int height = 32;

        // create the image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        // set the background color
        g.setColor(new Color(0xDCDCDC));
        g.fillRect(0, 0, width, height);
        // draw the border
        g.setColor(Color.black);
        g.drawRect(0, 0, width - 1, height - 1);
        // create a random instance to generate the codes
        Random rdm = new Random();
        // make some confusion
        for (int i = 0; i < 50; i++) {
            int x = rdm.nextInt(width);
            int y = rdm.nextInt(height);
            g.drawOval(x, y, 0, 0);
        }
        // generate a random code
        String verifyCode = generateVerifyCode(rdm);
        g.setColor(new Color(0, 100, 0));
        g.setFont(new Font("Candara", Font.BOLD, 24));
        g.drawString(verifyCode, 8, 24);
        g.dispose();

        // 计算表达式值，并把把验证码值存到redis中
        int expResult = calc(verifyCode);
        redisService.set(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId, expResult);
        // 输出图片
        return image;
    }

    /**
     * 生成验证码，只含有+/-/*
     * <p>
     * 随机生成三个数字，然后生成表达式
     *
     * @return 验证码中的数学表达式
     */
    private String generateVerifyCode(Random rdm) {
        int num1 = rdm.nextInt(10);
        int num2 = rdm.nextInt(10);
        int num3 = rdm.nextInt(10);
        char op1 = ops[rdm.nextInt(3)];
        char op2 = ops[rdm.nextInt(3)];
        String exp = "" + num1 + op1 + num2 + op2 + num3;
        return exp;
    }

    /**
     * 使用ScriptEngine计算验证码中的数学表达式的值
     */
    private int calc(String exp) {
        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            return (Integer) engine.eval(exp);// 表达式计算
        } catch (Exception e) {
            log.error("", e);
            return 0;
        }
    }

    /**
     * 检验检验码的计算结果
     */
    public boolean checkVerifyCode(User user, long goodsId, int verifyCode) {
        if (user == null || goodsId <= 0) {
            return false;
        }

        // 从redis中获取验证码计算结果
        Integer oldCode = redisService.get(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId, Integer.class);
        if (oldCode == null || oldCode - verifyCode != 0) {// !!!!!!
            return false;
        }

        // 如果校验不成功，则说明校验码过期
        redisService.delete(SeckillKeyPrefix.seckillVerifyCode, user.getId() + "," + goodsId);
        return true;
    }

    /**
     * 创建秒杀地址, 并将其存储在redis中
     */
    public String createSeckillPath(User user, long goodsId) {
        if (user == null || goodsId <= 0) {
            return null;
        }

        // 随机生成秒杀地址
        String path = MD5Util.md5(UUIDUtil.uuid() + "123456");
        // 将随机生成的秒杀地址存储在redis中（保证不同的用户和不同商品的秒杀地址是不一样的）
        redisService.set(SeckillKeyPrefix.seckillPath, "" + user.getId() + "_" + goodsId, path);
        return path;
    }

    /**
     * 验证路径是否正确
     */
    public boolean checkPath(User user, long goodsId, String path) {
        if (user == null || path == null)
            return false;
        // 从redis中读取出秒杀的path变量是否为本次秒杀操作执行前写入redis中的path
        String oldPath = redisService.get(SeckillKeyPrefix.seckillPath, "" + user.getId() + "_" + goodsId, String.class);
        return path.equals(oldPath);
    }
}
