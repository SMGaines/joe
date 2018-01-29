package com.smg.joe.client;
import java.util.Scanner;

public class UCIInput implements Runnable
{	
	UCI uci;
	
	UCIInput(UCI aUCI)
	{
		uci = aUCI;
	}
	
    public void run() 
    {
    	Scanner input;
        try 
        {
    		input = new Scanner(System.in);
			while(true)
			{
				String inputCmd = input.nextLine();
				uci.processUCI(inputCmd);			
			}
        }
        catch (Exception e)
        {
        	e.printStackTrace();
        	System.exit(0);
        }
	}
}
