package com.smg.joe;
import java.util.*;

public class UCI 
{
	static final int TOKEN_NOT_FOUND = -1;

	static Joe joe;
	static Board brd;
	static Log sl;
	static int colToMove;
	static int numGameMoves;
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
	
	static Scanner input;
	
	static String uciCommandStr,uciCommandArgs;
	
	public static void main(String[] args) 
	{
		initialise();
		processUCI();
		shutdown();
		System.exit(0); 
	}

	static void initialise()
	{
		input = new Scanner(System.in);
		sl = new Log();
		sl.openLog();
		log("UCI: Starting log");
		brd = new Board();
		joe=new Joe();		
		colToMove = Board.WHITE;
		numGameMoves = 0;
	}
	
	static void shutdown()
	{
		input.close();
		sl.closeLog();		
	}
	
	public static void processUCI()
	{
		while(true)
		{
			int cmd = processUCICommand();
			log("UCI Received: "+uciCommandStr);
			
			if (cmd == uciUnknown)
			{
				sl.log("Received unknown UCI command: "+uciCommandStr);
				continue;
			}
			
			switch(cmd)
			{
				case uciUCI:
					outputUCI();
					break;
				case uciIsReady:
					writeUCI("readyok");
					break;
				case uciNewGame:
					newGame();
					break;
				case uciPosition:
					setPosition();
					break;
				case uciGo:
					processGo();
					break;
				case uciQuit:
					return;
				case uciDebug:
					processDebug();
					break;
			}
		}
	}
	
	static void processDebug()
	{
		log("processDebug");
		if (tokenExists("on"))
			joe.setDebug(true);
		if (tokenExists("off"))
			joe.setDebug(false);
	}
	
	static int processUCICommand()
	{
		String inputCmd = input.nextLine();
		log("Received: "+inputCmd);
		int spaceIndex = inputCmd.indexOf(" ");
		if (spaceIndex == -1)
		{
			uciCommandStr=inputCmd;
			uciCommandArgs="";
		}
		else
		{
			uciCommandStr=inputCmd.substring(0,spaceIndex);
			uciCommandArgs = inputCmd.substring(spaceIndex);
		}
		log("uciCommandStr="+uciCommandStr+"/uciCommandArgs="+uciCommandArgs);
		for (int i=0;i<uciNumCommands;i++)
		{
			if (uciCommandStr.equals(UCICmdStr[i]))
				return i;
		}
		return uciUnknown;
	}
	
	// Is the given token in uciCommandArgs?
	static boolean tokenExists(String token)
	{
		return uciCommandArgs.indexOf(token) != -1;		
	}
	
	// Find a 'token' in uciCommandArgs and return it's value
	// e.g.  getTokenValue(str,"wtime") with str ="wtime 300000 btime 300000 winc 0 binc 0"
	// returns 30000 as an int

	static int getTokenNumberValue(String token)
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

	static String getMovesString()
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
	
	static void processGo()
	{
		log("ProcessGo");
		//goString e.g. wtime 300000 btime 300000 winc 0 binc 0
		
		// Set a default value of 10 seconds thinking time
		int time4Move = 10000;
		// Maybe we have a specific time to move given by 'movetime' e.g. movetime 60000
		if (tokenExists("movetime"))
			time4Move=getTokenNumberValue("movetime");
		
		if (time4Move == TOKEN_NOT_FOUND)
		{
			// Maybe we're given time left for White and Black e.g. wtime 300000 btime 300000 winc 0 binc 0
			// N.B. winc/binc not yet supported
			if (tokenExists("wtime"))
				whiteTimeLeft = getTokenNumberValue("wtime");
			if (tokenExists("btime"))
				blackTimeLeft = getTokenNumberValue("btime");
			
			int timeLeft = colToMove==Board.WHITE?whiteTimeLeft:blackTimeLeft;
			
			// For now, we'll use 3% of our available time for each move but with a minimum of 2 seconds
			time4Move = timeLeft/30;
			if (time4Move < 2000)
				time4Move = 2000;
		}
		outputBestMove(time4Move);
	}
	
	static void newGame()
	{
		brd.newGame();
		joe.newGame();
	}

	static void outputUCI()
	{
		writeUCI("id name Joe "+Joe.getVersion());
		writeUCI("id author SMG");
		writeUCI("uciok");
	}
	
	static void setPosition()
	{
		// We currently assume there is always a startpos present in a position command
		if (tokenExists("startpos"))
			brd.newGame();
		
		if (tokenExists("moves"))
		{
			ArrayList<Move> moves = Move.string2Moves(getMovesString());
			numGameMoves = 0;
			colToMove=Board.WHITE;
			for (Move m: moves)
			{
				m.setCapture(brd.pieceAt(m.to()));
				m.setPiece(brd.pieceAt(m.from()));
				brd.makeMove(m);
				colToMove=Board.flipSides(colToMove);
				numGameMoves++;
			}
		}
	}
	
	public static void outputBestMove(int time4Move)
	{
		log("outputBestMove: Thinking for "+(time4Move/1000)+" seconds");
		Move bestMove = joe.findBestMove(brd,colToMove, time4Move);
	    log(""+joe.getMoveTime());
		writeUCI("bestmove "+bestMove.getString());

		writeUCI("info nodes "+joe.getTotalNodes()+" depth "+joe.getMaxDepthSearched()+" nps "+joe.getNPS());
		System.out.println((colToMove==Board.WHITE?"WHITE":"BLACK")+" played "+bestMove.getString()+"["+brd.dumpEval()+"]");
	}
	
	public static void writeUCI(String msg)
	{
		System.out.println(msg);
	}
	
	public static void log(String msg)
	{
		System.out.println(msg);
		sl.log(msg);
	}
}