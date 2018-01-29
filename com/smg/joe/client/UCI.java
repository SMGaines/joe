package com.smg.joe.client;
import java.util.*;

import com.smg.joe.common.*;

public class UCI 
{
	static final int TOKEN_NOT_FOUND = -1;
	static final String versionStr = "v0.95";

	static Log sl;
	static int whiteTimeLeft;
	static int blackTimeLeft;
	
	static final int uciUnknown = -1;
	
	static final String[] UCICmdStr = {"uci","isready","ucinewgame","position","go","quit","debug"};
	
	static final int uciNumCommands = 7;
	static final int uciUCI = 0;
	static final int uciIsReady = 1;
	static final int uciNewGame = 2;
	static final int uciPosition = 3;
	static final int uciGo = 4;
	static final int uciQuit = 5;
	static final int uciDebug = 6;
	
	// Only these arguments currently supported
	static final String[] cmdTokens = {"startpos","moves","wtime","btime"};
	
	static String uciCommandStr,uciCommandArgs;
	
	static AMQClient qClient;

	String moveStr;
	
	public void initialise()
	{
		sl = new Log();
		sl.openLog();
		log("UCI: Starting log");
		initialiseQueues();
		moveStr="";
		startUCIListener();
	}
	
	void startUCIListener()
	{
		// Waits on command input
		thread(new UCIInput(this),true);		
	}
	
	void initialiseQueues()
	{
		log("UCI: Starting queues");		
		qClient = new AMQClient();
		boolean success = qClient.connectToSendQ(MQConstants.toEngineQueueName);
		if (!success)
		{
			log("UCI: Client queue setup failed");
		}
		thread(new RcvFromEngine(this,MQConstants.fromEngineQueueName),true);		
		log("UCI: Completed queue init");		
	}
	
	void shutdown()
	{
		sl.closeLog();		
	}
	
	int processUCICommand(String uciCmd)
	{
		int spaceIndex = uciCmd.indexOf(" ");
		if (spaceIndex == -1)
		{
			uciCommandStr=uciCmd;
			uciCommandArgs="";
		}
		else
		{
			uciCommandStr=uciCmd.substring(0,spaceIndex);
			uciCommandArgs = uciCmd.substring(spaceIndex);
		}
		for (int i=0;i<uciNumCommands;i++)
		{
			if (uciCommandStr.equals(UCICmdStr[i]))
				return i;
		}
		return uciUnknown;
	}
	
	public void processUCI(String uciCommandStr)
	{
		int cmd = processUCICommand(uciCommandStr);
		
		if (cmd == uciUnknown)
		{
			log("Received unknown UCI command: "+uciCommandStr);

			//qClient.sendMessage(MQConstants.sampleMessage);
			return;
		}

		log("UCI Command Received: "+uciCommandStr);

		switch(cmd)
		{
			case uciUCI:
				outputUCI();
				break;
			case uciIsReady:
				writeUCI("readyok");
				break;
			case uciNewGame:
				//newGame();
				break;
			case uciPosition:
				moveStr=getMovesString();
				break;
			case uciGo:
				processGo();
				break;
			case uciQuit:					
				shutdown();
				System.exit(0); 
				return;
			case uciDebug:
				
				break;
		}
	}
		
	// Is the given token in uciCommandArgs?
	boolean tokenExists(String token)
	{
		return uciCommandArgs.indexOf(token) != -1;		
	}
	
	// Find a 'token' in uciCommandArgs and return it's value
	// e.g.  getTokenValue(str,"wtime") with str ="wtime 300000 btime 300000 winc 0 binc 0"
	// returns 30000 as an int

	int getTokenNumberValue(String token)
	{
		if (!tokenExists(token))
			return TOKEN_NOT_FOUND;
		String valueStr;
		
		int searchIndex = uciCommandArgs.indexOf(token);
		
		int nextTokenStartLocation = TOKEN_NOT_FOUND;

		int tokenEndLocation=searchIndex+token.length();
		for (int i=tokenEndLocation;i<uciCommandArgs.length();i++)
		{
			String chr = uciCommandArgs.substring(i, i+1);
			if (isAlpha(chr))
			{
				nextTokenStartLocation=i;
				break;
			}
		}
		if (nextTokenStartLocation == TOKEN_NOT_FOUND)
			valueStr=uciCommandArgs.substring(tokenEndLocation).trim();
		else
			valueStr=uciCommandArgs.substring(tokenEndLocation, nextTokenStartLocation).trim();
		
		//Should now just have an int value in a string
		try
		{
			return new Integer(valueStr).intValue();
		}
		catch(Exception e)
		{
			return TOKEN_NOT_FOUND;
		}
	}

	String getMovesString()
	{
		String token = "moves";
		if (!tokenExists(token))
			return "";
		return uciCommandArgs.substring(uciCommandArgs.indexOf(token)+token.length()+1);
	}

	static boolean isAlpha(String name)
	{
	    return name.matches("[a-z]+");
	}
	
	void processGo()
	{
		log("ProcessGo");
		//goString e.g. wtime 300000 btime 300000 winc 0 binc 0
		
		// Set a default value of 10 seconds thinking time
		int time4Move = 10000;
		// Maybe we have a specific time to move given by 'movetime' e.g. movetime 60000
		if (tokenExists("movetime"))
			time4Move=getTokenNumberValue("movetime");
		
		think(time4Move);
	}

	void outputUCI()
	{
		writeUCI("id name Joe "+versionStr);
		writeUCI("id author SMG");
		writeUCI("uciok");
	}
	
	public void think(int time4Move)
	{
		qClient.sendMessage(time4Move+MQConstants.fieldSeparator+moveStr);
	}
	
	public void writeUCI(String msg)
	{
		System.out.println(msg);
	}
	
	public void log(String msg)
	{
		System.out.println(msg);
		sl.log(msg);
	}
	
    public void thread(Runnable runnable, boolean daemon) 
    {
        Thread brokerThread = new Thread(runnable);
        brokerThread.setDaemon(daemon);
        brokerThread.start();
    }
}