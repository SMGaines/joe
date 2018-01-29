package com.smg.joe.server;

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

public class RcvFromUCI implements Runnable
{
	public static final int TIMEOUT = 100000;
	
	Session session;
	MessageConsumer consumer;
	Connection connection;
	Joe joeEngine;
	String queueName;
	
	public RcvFromUCI(Joe aJoeEngine,String aQueueName)
	{
		joeEngine=aJoeEngine;
		queueName = aQueueName;
	    try 
	    {
	        // Create a ConnectionFactory
	        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL);
	
	        // Create a Connection
	        connection = connectionFactory.createConnection();
	        connection.start();
	
	        // Create a Session
	        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	
	        // Create the destination (Topic or Queue)
	        Destination destination = session.createQueue(queueName);
	
            // Create a MessageConsumer from the Session to the Topic or Queue
            consumer = session.createConsumer(destination);
            System.out.println("RcvFromUCI: Initialised for "+queueName);
	    }
	    catch(Exception e)
	    {
	        e.printStackTrace();
	    }
	}
	
	void closeConnection()
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
		
    public void run() 
    {
        System.out.println("RcvFromUCI: Listening on "+queueName);
        while(true)
    	{
		    try 
		    {
		        Message message = consumer.receive(TIMEOUT);
	            System.out.println("RcvFromUCI: Received from "+queueName);
		        
		        if (message instanceof TextMessage) 
		            joeEngine.processMessage(((TextMessage) message).getText());
		    }
		    catch(Exception e)
		    {
		        e.printStackTrace();
		        System.exit(0);
		    }
    	}
	}
}
