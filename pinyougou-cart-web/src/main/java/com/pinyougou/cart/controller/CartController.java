package com.pinyougou.cart.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.pojo.group.Cart;

import entity.Result;

@RestController
@RequestMapping("/cart")
public class CartController {
	@Reference(timeout=6000)
	private CartService cartService;
	@Autowired
	private HttpServletRequest request;
	@Autowired
	private HttpServletResponse response;
	/**
	 * 购物车列表
	 * @return
	 */
	@RequestMapping("/findCartList")
	public List<Cart> findCartList(){
		//得到登陆人账号,判断当前是否有人登陆
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		
		String cartList = util.CookieUtil.getCookieValue(request, "cartList","UTF-8");
			if(cartList==null || cartList.equals("")){
				cartList="[]";
			}
		List<Cart> cartList_cookie = JSON.parseArray(cartList,Cart.class);
		//如果是未登录那么从cookie中查
				if(username.equals("anonymousUser")){
			return cartList_cookie;
		}else {
			//如果登陆从redis中查
			List<Cart> cartList_redis = cartService.findCartListFromRedis(username);
			//如果本地存在购物车
			if(cartList_cookie.size()>0){
				//合并购物车
				System.out.println("合并购物车");
				cartList_redis=cartService.mergeCartList(cartList_redis, cartList_cookie);
				//清除本地cookie的数据
				util.CookieUtil.deleteCookie(request, response, "cartList");
				//将合并后的数据存入redis
				cartService.saveCartListToRedis(username, cartList_redis);
			}
			return cartList_redis;
		}
	}
	/**
	 * 添加商品到购物车
	 * @param itemId
	 * @param num
	 * @return
	 */
	@RequestMapping("/addGoodsToCartList")
	@CrossOrigin(origins="http://localhost:9105",allowCredentials="true")
	public Result addGoodsToCartList(Long itemId,Integer num){
		
		//response.setHeader("Access-Control-Allow-Origin", "http://localhost:9105");//可以访问的域(当此方法不需要操作cookie)
		//response.setHeader("Access-Control-Allow-Credentials", "true");//如果操作cookie，必须加上这句话
		//得到登陆人账号,判断当前是否有人登陆
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		try {
			//查询购物车列表
			List<Cart> cartList=findCartList();
			cartList=cartService.addGoodsToCartList(cartList, itemId, num);
			if(username.equals("anonymousUser")){
				util.CookieUtil.setCookie(request, response, "cartList", JSON.toJSONString(cartList),3600*24,"UTF-8");
				System.out.println("向cookie中存入数据");
			}else {
			//已登录,存入redis中
				cartService.saveCartListToRedis(username, cartList);
			}
			return new Result(true, "添加成功");
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return new Result(false, "添加失败");
		}
		
	}
	
}
