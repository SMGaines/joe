package com.smg.joe.common;

public class MoveGenerator 
{
	public static final int maxMoves = 400;
	public static final int maxSearchDepth = 32;

	Move[][] moveList;
	int[] numMoves;
	Move tempMove,lastMove;
	Board board;
	int mobMoves[];
	int numKingDefenders;
	int numKingAttackers;
	int piecePlacement;
	
	static boolean capturesOnly;

	static final int numBDirs = 4;
	static final int[] bDir = {11,9,-11,-9};
	static final int numRDirs = 4;
	static final int[] rDir = {10,1,-10,-1};
	static final int numNDirs = 8;
	static final int[] nDir = {12,21,19,8,-12,-21,-19,-8};

	public MoveGenerator()
	{
		moveList = new Move[maxSearchDepth][maxMoves];
		numMoves = new int[maxSearchDepth];
		tempMove = new Move();
		
		for (int i=0;i<maxSearchDepth;i++)
		{
			for (int j=0;j<maxMoves;j++)
			{
				moveList[i][j] = new Move();
			}
		}
		mobMoves = new int[2];
	}

	public void generateMoveList(Board b,int depth,int colour, boolean aCapturesOnly)
	{
		capturesOnly = aCapturesOnly;
		board=b;
		int direction=colour==Board.WHITE?-Board.boardWidth:Board.boardWidth;
		int sq;
		numMoves[depth]=0;
		mobMoves[colour]=0;
		numKingDefenders=0;
		numKingAttackers=0;
		piecePlacement = 0;
		
		for (int i=Board.firstSquare;i<=Board.lastSquare;i++)
		{
			int piece = board.pieceAt(i);
			if (!board.mySide(colour, piece))
				continue;

			switch(Math.abs(piece))
			{
				case Board.WP:
					int epSquare=0;
					lastMove=board.getLastMove();
					if (lastMove != null && lastMove.isDoublePawnMove())
						epSquare=lastMove.to()+direction;
					if (board.oppositeSides(i,i+direction-1) || i+direction-1 == epSquare)
						addMove(depth,i,i+direction-1);	
					if (board.oppositeSides(i,i+direction+1) || i+direction+1 == epSquare)
						addMove(depth,i,i+direction+1);
					
					if(board.pawnOnStartingSquare(i,colour) && board.isEmpty(i+direction) && board.isEmpty(i+direction*2))
						addMove(depth,i,i+direction*2);
					if (board.isEmpty(i+direction))
						addMove(depth,i,i+direction);
					break;
					
				case Board.WN:
					piecePlacement+=centreBonus(i);
					for (int j=0;j<numNDirs;j++)
					{
						if (board.isEmpty(i+nDir[j]) || board.oppositeSides(i,i+nDir[j]))
							addMove(depth,i,i+nDir[j]);
					}
					break;
					
				case Board.WB:
					piecePlacement+=centreBonus(i);
					for (int j=0;j<numBDirs;j++)
					{
						sq=i+bDir[j];
						while(board.isEmpty(sq))
						{
							addMove(depth,i,sq);
							sq+=bDir[j];
						}
						if(board.oppositeSides(i,sq))
							addMove(depth,i,sq);
					}
					break;
				case Board.WR:
					if (board.getPawnsInColumn(colour,i) == 0)
					{
						piecePlacement+=5;
						if (board.getPawnsInColumn(1-colour,i) == 0)
							piecePlacement+=5;
					}
					for (int j=0;j<numRDirs;j++)
					{
						sq=i+rDir[j];
						while(board.isEmpty(sq))
						{
							addMove(depth,i,sq);
							sq+=rDir[j];
						}
						if(board.oppositeSides(i,sq))
							addMove(depth,i,sq);
					}
					break;
				case Board.WQ:
					for (int j=0;j<numBDirs;j++)
					{
						sq=i+bDir[j];
						while(board.isEmpty(sq))
						{
							addMove(depth,i,sq);
							sq+=bDir[j];
						}
						if (board.oppositeSides(i,sq))
							addMove(depth,i,sq);
					}
					for (int j=0;j<numRDirs;j++)
					{
						sq=i+rDir[j];
						while(board.isEmpty(sq))
						{
							addMove(depth,i,sq);
							sq+=rDir[j];
						}
						if (board.oppositeSides(i,sq))
							addMove(depth,i,sq);
					}
					break;
				case Board.WK:
					for (int j=0;j<numRDirs;j++)
					{
						if (board.isEmpty(i+rDir[j]) || board.oppositeSides(i,i+rDir[j]))
							addMove(depth,i,i+rDir[j]);
					}
					for (int j=0;j<numBDirs;j++)
					{
						if (board.isEmpty(i+bDir[j]) || board.oppositeSides(i,i+bDir[j]))
							addMove(depth,i,i+bDir[j]);
					}
					
					// King's Side Castle
					if (board.canKSCastle(colour))
						addMove(depth,i,i+2);
					// Queen's Side Castle
					if (board.canQSCastle(colour))
						addMove(depth,i,i-2);
					break;
			}
		}
		board.setMobility(colour,mobMoves[colour]);
		board.setNumKingAttackers(colour,numKingAttackers);
		board.setNumKingDefenders(colour,numKingDefenders);
		board.setPiecePlacement(colour,piecePlacement);
	}

	// Is sq adjacent to the king of colour
	boolean sqNearKing(int sq,int colour)
	{
		return Math.abs(sq%Board.boardWidth - board.getKingPos(colour)%Board.boardWidth) < 2 &&
			   Math.abs(sq/Board.boardWidth - board.getKingPos(colour)/Board.boardWidth) < 2;
	}
	
	int centreBonus(int sq)
	{
		return 10 - (Math.abs((sq%Board.boardWidth)-5)+Math.abs((sq/Board.boardWidth)-5));
	}

	void addMove(int depth,int from,int to)
	{
		int colour = board.getColour(from);
		int piece = board.pieceValueAt(from);
		if (piece < Board.WQ)
			mobMoves[colour]++;
		if (sqNearKing(to,colour))
			numKingDefenders++;
		if (sqNearKing(to,1-colour))
			numKingAttackers++;
		
		if (capturesOnly && board.isEmpty(to))
			return;
		
		moveList[depth][numMoves[depth]].set(board.pieceAt(from),from,to,board.pieceAt(to)); 
		boolean inCheck = false;
		board.makeMove(moveList[depth][numMoves[depth]]);
	    inCheck = board.inCheck(colour);
		board.takeBackMove(moveList[depth][numMoves[depth]]);
		if (inCheck)
			return;
		numMoves[depth]++;
	}

	public int getNumMoves(int depth)
	{
		return numMoves[depth];
	}
	 
	public void setScore(int depth,int index,int aScore)
	{
		moveList[depth][index].setScore(aScore);
	}
	 
	public Move getMove(int depth,int index)
	{
		return moveList[depth][index];
	}
	 
	public void swap(int depth,int m1,int m2)
	{
		Move.copy(tempMove,moveList[depth][m1]);
		Move.copy(moveList[depth][m1],moveList[depth][m2]);
		Move.copy(moveList[depth][m2],tempMove);
	}
}