package com.smg.joe.common;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

public class AMQClient 
{
	public static final int TIMEOUT = 1000;
	Session session;
	MessageProducer producer;
	Connection connection;
	
	String queueName;
	
	public boolean connectToSendQ(String aQueueName)
	{
		queueName = aQueueName;
		System.out.println("AMQClient: Connecting to: "+queueName);
	    try 
	    {
	        // Create a ConnectionFactory
	        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL);
	
	        // Create a Connection
	        connection = connectionFactory.createConnection();
	        connection.start();
			System.out.println("AMQClient: Started connection successfully");
	
	        // Create a Session
	        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			System.out.println("AMQClient: Created session successfully");
	
	        // Create the destination (Topic or Queue)
	        Destination destination = session.createQueue(queueName);
			System.out.println("AMQClient: Created queue successfully");
	
	        // Create a MessageProducer from the Session to the Topic or Queue
	        producer = session.createProducer(destination);
	        producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);     
			System.out.println("AMQClient: Created producer successfully");
	    }
	    catch(Exception e)
	    {
			System.out.println("AMQClient: Failure connecting to: "+queueName);
	        e.printStackTrace();
	        return false;
	    }
	    return true;
	}
	
	public void closeConnection()
	{
	    try 
	    {
	        session.close();
	        connection.close();
	    }
	    catch(Exception e)
	    {
	        e.printStackTrace();
	    }	
	}
	
	public void sendMessage(String msg)
	{
	    try 
	    {
	    	//System.out.println("AMQClient: Sending ["+msg+"] to Engine on queue: "+queueName+" using session "+session);
	    	TextMessage message = session.createTextMessage(msg);
	    	producer.send(message);
	    }
	    catch (Exception e) 
	    {
	        e.printStackTrace();
	    }
	}
	
    public static void thread(Runnable runnable, boolean daemon) 
    {
        Thread brokerThread = new Thread(runnable);
        brokerThread.setDaemon(daemon);
        brokerThread.start();
    }
}
