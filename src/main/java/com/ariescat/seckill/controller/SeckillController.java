package com.ariescat.seckill.controller;

import com.ariescat.seckill.access.AccessLimit;
import com.ariescat.seckill.bean.SeckillOrder;
import com.ariescat.seckill.bean.User;
import com.ariescat.seckill.rabbitmq.MQSender;
import com.ariescat.seckill.rabbitmq.SeckillMessage;
import com.ariescat.seckill.redis.RedisService;
import com.ariescat.seckill.redis.key.GoodsKeyPrefix;
import com.ariescat.seckill.result.CodeMsg;
import com.ariescat.seckill.result.Result;
import com.ariescat.seckill.service.GoodsService;
import com.ariescat.seckill.service.OrderService;
import com.ariescat.seckill.service.SeckillService;
import com.ariescat.seckill.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/seckill")
public class SeckillController implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(SeckillController.class);

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    SeckillService seckillService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender sender;

    //基于令牌桶算法的限流实现类
//    RateLimiter rateLimiter = RateLimiter.create(10);

    //做标记，标记库存是否为空，从而减少对redis的访问
    private final HashMap<Long, Boolean> localOverMap = new HashMap<>();

    /**
     * 系统初始化,将商品信息加载到redis和本地内存
     */
    @Override
    public void afterPropertiesSet() {
        List<GoodsVo> goodsVoList = goodsService.listGoodsVo();
        if (goodsVoList == null) {
            return;
        }
        for (GoodsVo goods : goodsVoList) {
            redisService.set(GoodsKeyPrefix.getGoodsStock, "" + goods.getId(), goods.getStockCount());
            //初始化商品都是没有处理过的
            localOverMap.put(goods.getId(), false);
        }
    }

    /**
     * GET POST
     * 1、GET幂等,服务端获取数据，无论调用多少次结果都一样
     * 2、POST，向服务端提交数据，不是幂等
     * <p>
     * 将同步下单改为异步下单
     */
    @AccessLimit(seconds = 5, maxAccessCount = 5, needLogin = true)
    // {path}为客户端回传的path，最初也是有服务端产生的
    @RequestMapping(value = "/{path}/do_seckill", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> doSeckill(User user,
                                     @RequestParam("goodsId") long goodsId,
                                     @PathVariable("path") String path) {

        // 1. 在执行下面的逻辑之前，会相对path请求进行拦截处理（@AccessLimit， AccessInterceptor），防止访问次数过于频繁，对服务器造成过大的压力

//        2. 或用令牌桶
//        if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
//            return Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
//        }
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        // 验证path是否正确
        boolean check = seckillService.checkPath(user, goodsId, path);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL); // 请求非法
        }

        // 内存标记，减少redis访问
        boolean over = localOverMap.get(goodsId);
        if (over) {
            return Result.error(CodeMsg.SECKILL_OVER);
        }
        // 判断重复秒杀
        SeckillOrder order = orderService.getOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            return Result.error(CodeMsg.REPEATE_SECKILL);
        }
        // 预减库存
        long stock = redisService.decr(GoodsKeyPrefix.getGoodsStock, "" + goodsId);//10
        if (stock < 0) {
            localOverMap.put(goodsId, true);
            return Result.error(CodeMsg.SECKILL_OVER);
        }
        // 入队
        SeckillMessage message = new SeckillMessage();
        message.setUser(user);
        message.setGoodsId(goodsId);
        sender.sendSeckillMessage(message);
        return Result.success(0); // 排队中
    }

    /**
     * orderId：成功
     *
     * @return -1: 秒杀失败; 0: 排队中
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> getSeckillResult(User user, @RequestParam("goodsId") long goodsId) {
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long orderId = seckillService.getSeckillResult(user.getId(), goodsId);
        return Result.success(orderId);
    }

    /**
     * 获取秒杀接口地址
     * 每一次点击秒杀，都会生成一个随机的秒杀地址返回给客户端
     * 对秒杀的次数做限制（通过自定义拦截器注解完成）
     *
     * @param goodsId    秒杀的商品id
     * @param verifyCode 验证码
     * @return 被隐藏的秒杀接口路径
     */
    @AccessLimit(seconds = 5, maxAccessCount = 5, needLogin = true)
    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getSeckillPath(User user,
                                         @RequestParam("goodsId") long goodsId,
                                         @RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode) {

        // 在执行下面的逻辑之前，会相对path请求进行拦截处理（@AccessLimit， AccessInterceptor），防止访问次数过于频繁，对服务器造成过大的压力

        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        // 校验验证码
        boolean check = seckillService.checkVerifyCode(user, goodsId, verifyCode);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);// 检验不通过，请求非法
        }

        // 检验通过，获取秒杀路径
        String path = seckillService.createSeckillPath(user, goodsId);
        // 向客户端回传随机生成的秒杀地址
        return Result.success(path);
    }


    /**
     * 验证码
     * <p>
     * 使用HttpServletResponse的输出流返回客户端异步获取的验证码（异步获取的代码如下所示）
     * goods_detail.htm: $("#verifyCodeImg").attr("src", "/seckill/verifyCode?goodsId=" + $("#goodsId").val());
     */
    @RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getVerifyCode(HttpServletResponse response, User user, @RequestParam("goodsId") long goodsId) {
        if (user == null)
            return Result.error(CodeMsg.SESSION_ERROR);

        // 创建验证码
        try {
            BufferedImage image = seckillService.createVerifyCode(user, goodsId);
            ServletOutputStream out = response.getOutputStream();
            // 将图片写入到resp对象中
            ImageIO.write(image, "JPEG", out);
            out.close();
            out.flush();
            return null;
        } catch (Exception e) {
            log.error("", e);
            return Result.error(CodeMsg.SECKILL_FAIL);
        }
    }
}
