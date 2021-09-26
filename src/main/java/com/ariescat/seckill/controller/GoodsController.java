package com.ariescat.seckill.controller;

import com.alibaba.druid.util.StringUtils;
import com.ariescat.seckill.bean.User;
import com.ariescat.seckill.redis.RedisService;
import com.ariescat.seckill.redis.key.GoodsKeyPrefix;
import com.ariescat.seckill.result.Result;
import com.ariescat.seckill.service.GoodsService;
import com.ariescat.seckill.vo.GoodsDetailVo;
import com.ariescat.seckill.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.spring4.context.SpringWebContext;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;

    /**
     * 商品列表页面
     * <p>
     * Jmeter: 1000 * 10 -> QPS:433
     * <p>
     * 优化: 页面级缓存
     */
    @RequestMapping(value = "/to_list", produces = "text/html")
    @ResponseBody
    public String list(HttpServletRequest request, HttpServletResponse response, Model model, User user) {

        //取缓存
        String html = redisService.get(GoodsKeyPrefix.getGoodsList, "", String.class);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        model.addAttribute("user", user);
        model.addAttribute("goodsList", goodsList);

        //手动渲染
        SpringWebContext ctx = new SpringWebContext(request, response,
                request.getServletContext(), request.getLocale(), model.asMap(), applicationContext);
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list", ctx);

        if (!StringUtils.isEmpty(html)) {
            redisService.set(GoodsKeyPrefix.getGoodsList, "", html);
        }
        //结果输出
        return html;
    }


    /**
     * 商品详情页面（未做页面静态化处理）
     * <p>
     * 优化: URL级缓存, 实际上URL级缓存和页面级缓存是一样的, 只不过URL级缓存会根据url的参数从redis中取不同的数据
     */
    @RequestMapping(value = "/to_detail2/{goodsId}", produces = "text/html")
    @ResponseBody
    public String detail2(HttpServletRequest request, HttpServletResponse response, Model model, User user, @PathVariable("goodsId") long goodsId) {

        //取缓存
        String html = redisService.get(GoodsKeyPrefix.getGoodsDetail, "" + goodsId, String.class);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }

        //根据id查询商品详情
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("user", user);
        model.addAttribute("goods", goods);

        // 获取商品的秒杀开始与结束的时间
        long startTime = goods.getStartDate().getTime();
        long endTime = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        // 秒杀状态; 0: 秒杀未开始，1: 秒杀进行中，2: 秒杀已结束
        int seckillStatus = 0;
        // 秒杀剩余时间
        int remainSeconds = 0;

        if (now < startTime) { // 秒杀还没开始，倒计时
            seckillStatus = 0;
            remainSeconds = (int) ((startTime - now) / 1000);
        } else if (now > endTime) { // 秒杀已经结束
            seckillStatus = 2;
            remainSeconds = -1;
        } else { // 秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("seckillStatus", seckillStatus);
        model.addAttribute("remainSeconds", remainSeconds);

        // 手动渲染
        SpringWebContext ctx = new SpringWebContext(request, response,
                request.getServletContext(), request.getLocale(), model.asMap(), applicationContext);
        html = thymeleafViewResolver.getTemplateEngine().process("goods_detail", ctx);
        if (!StringUtils.isEmpty(html)) {
            redisService.set(GoodsKeyPrefix.getGoodsDetail, "" + goodsId, html);
        }
        return html;
    }

    /**
     * 商品详情页面（页面静态化处理, 直接将数据返回给客户端，交给客户端处理）
     */
    @RequestMapping(value = "/detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> detail(User user, @PathVariable("goodsId") long goodsId) {

        //根据id查询商品详情
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);

        // 获取商品的秒杀开始与结束的时间
        long startTime = goods.getStartDate().getTime();
        long endTime = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        // 秒杀状态; 0: 秒杀未开始，1: 秒杀进行中，2: 秒杀已结束
        int seckillStatus = 0;
        // 秒杀剩余时间
        int remainSeconds = 0;

        if (now < startTime) { // 秒杀还没开始，倒计时
            seckillStatus = 0;
            remainSeconds = (int) ((startTime - now) / 1000);
        } else if (now > endTime) { // 秒杀已经结束
            seckillStatus = 2;
            remainSeconds = -1;
        } else { // 秒杀进行中
            seckillStatus = 1;
            remainSeconds = 0;
        }

        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(user);
        vo.setRemainSeconds(remainSeconds);
        vo.setSeckillStatus(seckillStatus);
        return Result.success(vo);
    }
}
