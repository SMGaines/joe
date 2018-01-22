package com.smg.joe;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log 
{
	static final String dir = "./logs/";
	
    static FileWriter logFile;
    static BufferedWriter logger;

	Log()
	{
		
	}
	
	void openLog()
	{
	    try
	    {
	    	DateFormat dateFormat = new SimpleDateFormat("ddMMyy-HHmmss");
	        Date date = new Date();
	        logFile = new FileWriter(dir+"log"+dateFormat.format(date)+".log");
	        logger = new BufferedWriter(logFile);
	     }
	    catch (Exception e)
	    {
	          System.err.println("Error: " + e.getMessage());
	    }
	}
	
	void closeLog()
	{
	    try
	    {
	        logger.close();
	     }
	    catch (Exception e)
	    {
	          System.err.println("Error: " + e.getMessage());
	    }
	}
	
	void log(String s)
	{
		try
		{
			logger.write(s+"\n");
			logger.flush();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}