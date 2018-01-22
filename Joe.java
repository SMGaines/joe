package com.smg.joe;

//
// Joe Chess (C) SMG 2017-2018
//

import java.util.Random;

public class Joe
{
	int historyIncrement = 1;
	static final String versionStr = "v0.93";
	
	static boolean NOTIME;
	
	static final int TIME_OUT = 99999;
	static final int LOWER_BOUND = -30000;
	static final int UPPER_BOUND = 30000;
	static final int MATE = -10000;
	static final int DRAW = 0;

	// Search Parameters
	static final int firstPly = 0;
	static final int maxSearchDepth = 32;
	static int maxDepthSearched;
	static int fullSearchDepth;
	static int mNodes;
	static int qNodes;
	static Move bestMove;
	static Move bestMoveThisIteration;
	static int numCutoffs;
	static int startingAspirationWindow = 20; // Tenth of a pawn
	int[] pvLength;
	long startTime,endTime,moveTime,availableTime;
	static String pvString;
	static int bestScore;
	static boolean threeFoldRepetition;
	
	static boolean uciDebug;

	static final int transTableSize = 4999999; // Prime number
	TranspositionEntry transTable[];
	int[][] history;
	boolean[][] historyMoveUsed;
	Move[][] pv;
	Move[] currentPV;
	int currentPVLength;
	MoveGenerator mg;	
	static final boolean capturesOnly = true;

	static Random rnd;
	static Board brd;
	
	int numTransHits;
	int numABCalls;
	
	boolean usingPV;
	
	Joe()
	{
		initHistoryTable();
		initPV();
		initTranspositionTable();
		mg = new MoveGenerator();
		uciDebug=false;
		bestMove = new Move();
		bestMoveThisIteration = new Move();
		rnd = new Random(System.currentTimeMillis());
	}
	
	void newGame()
	{
		clearHistoryTable();
		clearTranspositionTable();
	}

	void initHistoryTable()
	{
		history = new int[Board.boardSize][Board.boardSize];
		historyMoveUsed = new boolean[maxSearchDepth+1][MoveGenerator.maxMoves];		
	}
	
	void initPV()
	{
		pvLength=new int[maxSearchDepth+1];
		pv=new Move[maxSearchDepth+1][MoveGenerator.maxMoves];
		for (int i=0;i<maxSearchDepth;i++)
		{
			for (int j=0;j<MoveGenerator.maxMoves;j++)
			{
				pv[i][j]=new Move();
			}
		}
		currentPV=new Move[MoveGenerator.maxMoves];
		for (int j=0;j<MoveGenerator.maxMoves;j++)
		{
			currentPV[j]=new Move();
		}		
	}
	
	void initTranspositionTable()
	{
		transTable = new TranspositionEntry[(int)transTableSize];
		for (int i=0;i<transTableSize;i++)
			transTable[i]=new TranspositionEntry();
	}

	void clearTranspositionTable()
	{
		for (int i=0;i<transTableSize;i++)
			transTable[i].reset();
	}
	
	int getTTPercentFull()
	{
		int numTTEntries=0;
		for (int i=0;i<transTableSize;i++)
			if (transTable[i].getBoardHash() !=0)
				numTTEntries++;
		return ((100*numTTEntries)/transTableSize);
	}
	
	void clearHistoryTable()
	{
		for (int i=0;i<Board.boardSize;i++)
			for (int j=0;j<Board.boardSize;j++)
				history[i][j]=0;		
	}
	
	void setDebug(boolean state)
	{
		uciDebug = state;
		System.out.println("Debug set to: "+state);
	}
	
	static String getVersion()
	{
		return versionStr;
	}

	// Find the best move for 'colour' within a max of time4Move seconds
	// Returns the best move found or null if no moves available
	
	Move findBestMove(Board aBoard,int colour, long time4Move)
	{
		startTime=System.currentTimeMillis();
			
		brd = aBoard;
		
		int alpha = LOWER_BOUND;
		int beta = UPPER_BOUND;
		
		maxDepthSearched = 0;
		mNodes=0;
		qNodes=0;
		NOTIME=false;
		availableTime = time4Move-100;
		bestMove = new Move();
		bestMoveThisIteration = new Move();
		numCutoffs = 0;
		
		for (int i=0;i<maxSearchDepth;i++)
			pvLength[i]=0;
		
		// Clear History and Transposition tables
		// every 10 half-moves
		if (brd.getMoveNumber()%10 == 0)
		{
			clearHistoryTable();
			clearTranspositionTable();
		}
		
		// We can reset the hash table if the last move was a capture or a pawn move or castling
		// since these are irreversible and therefore previous positions can never
		// be repeated. We don't need to, it just makes things run slightly faster
		// since the hash table is smaller.
		
		Move lastMove = brd.getLastMove();
		if (lastMove != null)
		{
			if (lastMove.getCaptureValue() > 0 || lastMove.getPieceType() == Board.WP || lastMove.isCastlingMove())
			{
				brd.resetHashTable();	
			}
		}
		
		System.out.println("Transposition table "+ getTTPercentFull()+"% full");

	    mg.generateMoveList(brd,firstPly,colour,!capturesOnly);
	    
	    int numMoves = mg.getNumMoves(firstPly);
	    if (numMoves==0)
	    	return null;
	    
	    if (numMoves==1)
	    	return mg.getMove(firstPly,0);
	    
	    int score;
	    
	    bestScore = Integer.MIN_VALUE;
	    
	    numTransHits = 0;
	    numABCalls=0;
	    int aspirationWindow = startingAspirationWindow;
	    currentPVLength=0;
	    
		// Iterative deepening until we run out of time
	    fullSearchDepth = 1;
	    
	    while(true)
	    {
	    	usingPV=true;
		    score = alphaBeta(colour,alpha,beta,firstPly); 
		    if (Math.abs(score) == TIME_OUT)
		    	break;
		    if (score<=alpha || score >=beta)
		    {
		    	System.out.println("Depth:"+fullSearchDepth+" - Score("+score+") outside window ("+alpha+"-"+beta+")");
		    	aspirationWindow*=2;
		    	if (score <=alpha)
		    		alpha=score-aspirationWindow;
		    	if (score>=beta)
		    		beta=score+aspirationWindow;
		    }
		    else
		    {
			    Move.copy(bestMove,bestMoveThisIteration);
			    bestScore = score;
			    // If there's already mate, no point searching further
				if (scoreIsMate(score))
				{
					int movesToMate = calcMovesToMate(score);
					System.out.println("info pv "+pvStr()+" score mate "+(colour==Board.WHITE?movesToMate:-movesToMate));
					break;
				}
		    	aspirationWindow = startingAspirationWindow;
			    alpha=score-aspirationWindow;
			    beta=score+aspirationWindow;
			    System.out.println("Depth:"+fullSearchDepth);
			    System.out.println("info pv "+pvStr()+" score cp "+(colour==Board.WHITE?score:-score));
		    	fullSearchDepth++;
		    	// We use a copy of the pv[][] variable to guide Move Ordering
		    	// We don't use the pv[][] variable itself because that is 
		    	// being updated during the search
		    	for (int i=0;i<pvLength[0];i++)
		    		Move.copy(currentPV[i],pv[0][i]);
		    	currentPVLength = pvLength[0];
		    	System.out.println("PV Length="+currentPVLength);
		    }
	    }
	   endTime = System.currentTimeMillis();
	   moveTime=(endTime-startTime);
	   return bestMove;
	}
	
	boolean scoreIsMate(int score)
	{
		return Math.abs(Math.abs(score)-Math.abs(MATE)) < 100;
	}
	
	int calcMovesToMate(int score)
	{
		return (score>0?1:-1)*(1+(Math.abs(MATE)- Math.abs(score))/2);
	}

	boolean outOfTime()
	{
		return (System.currentTimeMillis() - startTime) >= availableTime;
	}
	
	TranspositionEntry getTransTableEntry(long aHash)
	{
	    return transTable[(int)(Math.abs(aHash)%transTableSize)];
	}
		
	int alphaBeta(int colour, int alpha, int beta, int depth) 
	{
		numABCalls++;
		if ((mNodes%1000) == 0)
			NOTIME=outOfTime();
		
	    if (NOTIME)
			return TIME_OUT;
	    
	    if (depth > 0 && brd.isThreefoldRepetition())
	    	return DRAW;
	   
		long brdHash = brd.getBoardHash();
		TranspositionEntry te = getTransTableEntry(brdHash);
		if (depth > firstPly && te.getBoardHash() == brdHash && (fullSearchDepth-depth)<=te.getDepth())
		{
			numTransHits++;
		    pvLength[depth] = depth-1;
			return te.getScore();
		}
	    pvLength[depth] = depth;
	    
		if (depth == fullSearchDepth) 
	    	return quiesce(colour,alpha,beta,depth);

	    mg.generateMoveList(brd,depth,colour,!capturesOnly);
	    int numMoves = mg.getNumMoves(depth);
	    if (numMoves==0)
	    {
		   if (brd.inCheck(colour))
			   return MATE+depth; // Later is better for loser!
		   else
			   // If Stalemate, return a Draw
			   return DRAW;
	    }
	    		
		for (int j=0;j<numMoves;j++)
			historyMoveUsed[depth][j]=false;
		
		for (int i=0;i<numMoves;i++)  
	    {
			  mNodes++;
			  Move m = chooseBestMove(depth);
			  
			  brd.makeMove(m);
			  int score = -alphaBeta(Board.flipSides(colour), -beta, -alpha, depth+1 );
			  brd.takeBackMove(m);
			  
			  usingPV=false;
			  
			  if (Math.abs(score) == TIME_OUT)
				  return TIME_OUT;
			  
			  if (score > alpha)
			  {
				  alpha=score;
			      if (alpha >=beta)
			      {
			    	  history[m.from()][m.to()]+=historyIncrement; 
				      numCutoffs++;
			    	  return beta;
			      }
			      copyPV(m,depth);
			      if (depth==0)
			      {
			    	  Move.copy(bestMoveThisIteration,m);
			      }
			  }
	    }
		if ((fullSearchDepth-depth) > te.getDepth())
			te.set(fullSearchDepth-depth, brdHash, alpha);
	    return alpha;
	}
		 	
	int quiesce(int colour, int alpha, int beta,int depth ) 
	{
		if ((qNodes%1000) == 0)
			NOTIME=outOfTime();
		
	    if (NOTIME)
			return TIME_OUT;
	    
		if (depth > maxDepthSearched)
			maxDepthSearched = depth;
		pvLength[depth] = depth;

	    if (depth >= maxSearchDepth-1)
	    	return brd.evaluate(colour);
	    
	    int standPat = brd.evaluate(colour);

 	    if (standPat >= beta)
	    	  return beta;
	    if (standPat > alpha)
	    	alpha=standPat;
	    
	    mg.generateMoveList(brd,depth,colour,capturesOnly);
	    int numMoves = mg.getNumMoves(depth);
	    
	 	for (int i=0;i<numMoves;i++)  
		{
	 		getMVVLVA(depth,i);
	 		
	 		qNodes++;
	 		Move m = mg.getMove(depth,i);
	 		brd.makeMove(m);
	        int score = -quiesce(Board.flipSides(colour), -beta, -alpha,depth+1);
	        brd.takeBackMove(m);
	        
			if (Math.abs(score) == TIME_OUT)
				return TIME_OUT;
	        
	        if( score > alpha)
	        {
          	   if( score >= beta )
  		       {
          		   history[m.from()][m.to()]+=historyIncrement;
          		   numCutoffs++;
          		   return beta;
  		       }
		       alpha = score;
	           copyPV(m,depth);
	        }
	    }
	    return alpha;
	 }
	 
	// Find the best MVV-LVA (Most Valuable Victim - Least Valuable Aggressor) score
	// Then swap position 'startingIndex' with this move such that the best MVV-LVA moves 
	// are searched first
	
	int getMVVLVA(int depth,int startingIndex)
	{
		int bestScore=Integer.MIN_VALUE;
		int bestIndex=Integer.MIN_VALUE;
		for (int i=startingIndex;i<mg.getNumMoves(depth);i++)
		{
			int from = mg.getMove(depth,i).from();
			int to = mg.getMove(depth,i).to();
			int mvvlvaScore = brd.pieceValueAt(to)-brd.pieceValueAt(from); //+history[from][to];
			if (mvvlvaScore>bestScore)
			{
				bestScore = mvvlvaScore;
				bestIndex=i;
			}
		}
		mg.swap(depth, startingIndex, bestIndex);
		return bestScore;
	}
		
	Move chooseBestMove(int depth)
	{		
		int bestScore=Integer.MIN_VALUE;
		int bestIndex=Move.NO_MOVE;
		for (int i=0;i<mg.getNumMoves(depth);i++)
		{
			Move m = mg.getMove(depth,i);
			int mScore = history[m.from()][m.to()];
			if (usingPV && currentPVLength <= depth && m.isSameMoveAs(currentPV[depth]))
					mScore+=UPPER_BOUND;
			if (mScore>bestScore && !historyMoveUsed[depth][i])
			{
				bestScore = mScore;
				bestIndex=i;
			}
		}
		historyMoveUsed[depth][bestIndex]=true;
		return mg.getMove(depth,bestIndex);
	}
	
	void copyPV(Move m, int depth)
	{
       Move.copy(pv[depth][depth],m);
       for (int j = depth + 1; j < pvLength[depth + 1]; ++j)
    	   Move.copy(pv[depth][j],pv[depth + 1][j]);
       pvLength[depth] = pvLength[depth + 1];
	}

	//
	// Various helper routines
	//		
	 
	int getTotalNodes()
	{
		return mNodes+qNodes;
	}
	int getMainNodes()
	{
		return mNodes;
	}
	
	int getQuiesceNodes()
	{
		return qNodes;
	}
	
	int getNumCutoffs()
	{
		return numCutoffs;
	}
	int getABCalls()
	{
		return numABCalls;
	}
	int getNumTransHits()
	{
		return numTransHits;
	}
	
	// Nodes per second. Multiply by 1000 because moveTime is in ms
	int getNPS()
	{
		return 1000*(int)((mNodes+qNodes)/(moveTime>0?moveTime:1));
	}
	
	int getMaxDepthSearched()
	{
		return maxDepthSearched;
	}
	
	int getBestMoveScore()
	{
		return bestScore;
	}
	
	long getMoveTime()
	{
		return moveTime;
	}
	 
	String pvStr()
	{
		StringBuffer sb = new StringBuffer();
  	  	for (int j=0;j<=pvLength[0];j++)
		  sb.append(pv[0][j].getString()+" ");
		return sb.toString();
	}
}