package com.ariescat.seckill.mapper;

import com.ariescat.seckill.bean.SeckillGoods;
import com.ariescat.seckill.vo.GoodsVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface GoodsMapper {

    /**
     * 查出商品信息（包含该商品的秒杀信息）
     * 利用左外连接(LEFT JOIN...ON...)的方式查
     */
    @Select("select g.*, sg.stock_count, sg.start_date, sg.end_date, sg.seckill_price, sg.version from sk_goods_seckill sg left join sk_goods g on sg.goods_id = g.id")
    List<GoodsVo> listGoodsVo();

    /**
     * 通过商品的id查出商品的所有信息（包含该商品的秒杀信息）
     */
    @Select("select g.*, sg.stock_count, sg.start_date, sg.end_date, sg.seckill_price, sg.version  from sk_goods_seckill sg left join sk_goods g  on sg.goods_id = g.id where g.id = #{goodsId}")
    GoodsVo getGoodsVoByGoodsId(@Param("goodsId") long goodsId);

    // stock_count > 0 和 版本号实现乐观锁 防止超卖
    @Update("update sk_goods_seckill set stock_count = stock_count - 1, version= version + 1 where goods_id = #{goodsId} and stock_count > 0 and version = #{version}")
    int reduceStockByVersion(SeckillGoods seckillGoods);

    // 获取最新版本号
    @Select("select version from sk_goods_seckill  where goods_id = #{goodsId}")
    int getVersionByGoodsId(@Param("goodsId") long goodsId);


}
