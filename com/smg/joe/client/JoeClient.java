package com.smg.joe.client;

public class JoeClient 
{
	public static void main(String[] args) 
	{
		UCI uci = new UCI();
		uci.initialise("amqp://ncdvmadf:qdJCC6U4ARyPKG4UBzL7m10IfI4xMLwf@gopher.rmq.cloudamqp.com/ncdvmadf");
	}
}
