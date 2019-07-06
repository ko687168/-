package com.pinyougou.page.service.impl;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.activemq.filter.function.inListFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Component
public class PageDeleteListener implements MessageListener {
	
	@Autowired
	private ItemPageServiceImpl itemPageServiceImpl;
	@Override
	public void onMessage(Message message) {
		// TODO Auto-generated method stub
		ObjectMessage  objectMessage = (ObjectMessage)message;
		try {
			Long[] goodsIds = (Long[])objectMessage.getObject();
			System.out.println("接收到监听消息"+goodsIds);
			boolean b = itemPageServiceImpl.deleteItemHtml(goodsIds);
			System.out.println("网页删除结果"+b);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}
