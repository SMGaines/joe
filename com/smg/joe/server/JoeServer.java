package com.smg.joe.server;

import com.smg.joe.mq.MQCallback;
import com.smg.joe.mq.RabbitMQHandler;

public class JoeServer 
{
	public static void main(String[] args) 
	{
		Joe joe = new Joe();
		RabbitMQHandler mq = new RabbitMQHandler(joe,MQCallback.mqFromEngineQueueName,MQCallback.mqToEngineQueueName);
		joe.setQueue(mq);
		mq.run();
	}
}