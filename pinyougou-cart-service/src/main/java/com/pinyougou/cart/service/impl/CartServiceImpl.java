package com.pinyougou.cart.service.impl;

import static org.hamcrest.CoreMatchers.nullValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.management.RuntimeErrorException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.druid.sql.dialect.odps.visitor.OdpsASTVisitor;
import com.alibaba.dubbo.config.annotation.Service;
import com.fasterxml.jackson.databind.deser.impl.CreatorCollector;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojo.group.Cart;
import com.sun.tools.classfile.Annotation.element_value;
/**
 * 购物车服务实现类
 * @author XH
 *
 */
@Service
public class CartServiceImpl implements CartService{
	@Autowired
	private TbItemMapper itemMapper;
	@Autowired
	private RedisTemplate redisTemplate;
	

	@Override
	public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {
		//根据sku id查询sku商品信息
		TbItem item = itemMapper.selectByPrimaryKey(itemId);
		if(item==null){
			throw new RuntimeException("商品不存在");
		}
		if(!item.getStatus().equals("1")){
			throw new RuntimeException("商品状态无效");
		}
		//获取商家id
		String sellerId = item.getSellerId();
		//根据商家id判断购物车是否存在该商家的购物车
		Cart cart = searchCartBySellerId(cartList, sellerId);
		//如果列表中不存在该商家的购物车
		if(cart==null){
			//4.1新建购物车对象
			cart=new Cart();
			cart.setSellerId(sellerId);
			cart.setSellerId(item.getSellerId());
			TbOrderItem orderItem = createOrderItem(item, num);
			List orderItemList = new ArrayList<>();
			orderItemList.add(orderItem);
			cart.setOrderItemList(orderItemList);
			//4.2将购物车对象添加到购物车列表
			cartList.add(cart);
		}else{
			//如果购物车存在该商家的购物车
			//判断购物车明细中是否有该商品
			TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(), itemId);
			if(orderItem==null){
			//5.1如果没有该商品 新增商品明细
			orderItem = createOrderItem(item, num);
			cart.getOrderItemList().add(orderItem);
			}else {
				//5.2如果有,在原购物车明细上添加数量，更改金额
				orderItem.setNum(orderItem.getNum()+num);
				orderItem.setTotalFee(new BigDecimal(orderItem.getNum()*orderItem.getPrice().doubleValue()));
				//如果数量操作后小于0 移除
				if(orderItem.getNum()<=0){
					cart.getOrderItemList().remove(orderItem);//移除购物车明细
				}
				//如果购物车明细移除后cart的明细数量=0；则将cart移除
				if(cart.getOrderItemList().size()==0){
					cartList.remove(cart);
				}
			}
		}
		
		return cartList;
	}
	/**
	 * 根据商家id查询购物车对象
	 * @param cartList
	 * @param sellerId
	 * @return
	 */
	public Cart searchCartBySellerId(List<Cart> cartList,String sellerId){
		for(Cart cart:cartList){
			if(cart.getSellerId().equals(sellerId)){
				return cart;
			}
		}	
		return null;
	}
	/**
	 * 根据商品明细查询
	 * @param orderItemList
	 * @param itemId
	 * @return
	 */
	private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList,Long itemId){
		for(TbOrderItem orderItem:orderItemList){
			if(orderItem.getItemId().longValue()==itemId.longValue()){
				return orderItem;
			}
		}
		return null;
	}
	/**
	 * 创建订单明细
	 * @param item
	 * @param num
	 * @return
	 */
    private TbOrderItem createOrderItem(TbItem item,Integer num){
    	if(num<0){
    		throw new RuntimeException("数量非法");
    	}
    	TbOrderItem orderItem = new TbOrderItem();
    	orderItem.setGoodsId(item.getGoodsId());
		orderItem.setItemId(item.getId());
		orderItem.setNum(num);
		orderItem.setPicPath(item.getImage());
		orderItem.setPrice(item.getPrice());
		orderItem.setSellerId(item.getSellerId());
		orderItem.setTitle(item.getTitle());
		orderItem.setTotalFee(new BigDecimal(item.getPrice().doubleValue()*num));
		return orderItem;
    }
    /**
     * 从redis中查询购物车列表
     */
	@Override
	public List<Cart> findCartListFromRedis(String username) {
		System.out.println("从redis中提取数据");
		List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get("username");
		if(cartList==null){
			cartList=new ArrayList();
		}
		return cartList;
	}
	/**
	 * 将购物车列表存入redis中
	 */
	@Override
	public void saveCartListToRedis(String username, List<Cart> cartList) {
		System.out.println("向redis中存入购物车信息");
		redisTemplate.boundHashOps("cartList").put(username, cartList);
		
	}
	/**
	 * 合并购物车
	 */
	@Override
	public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {
		System.out.println("合并购物车");
		for (Cart cart:cartList2) {
			for(TbOrderItem orderItem :cart.getOrderItemList()){
				cartList1=addGoodsToCartList(cartList1, orderItem.getItemId(), orderItem.getNum());
			}
		}
		return cartList1;
	}

}
