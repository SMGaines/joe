package com.smg.joe.common;

import java.util.ArrayList;
import java.util.Random;

public class Board 
{
	public static final int WHITE=0;
	public static final int BLACK=1;
	public static final int boardSize = 120;
	public static final int boardWidth = 10;
	static final int firstBlackPiece = 21;
	static final int lastWhitePiece = 98;
	static final int firstWhitePawn = 81;
	static final int lastBlackPawn = 38;
	
	public static final int EM = 0;
	public static final int WP = 1;
	public static final int WN = 2;
	public static final int WB = 3;
	public static final int WR = 4;
	public static final int WQ = 5;
	public static final int WK = 6;
	public static final int BP = -1;
	public static final int BN = -2;
	public static final int BB = -3;
	public static final int BR = -4;
	public static final int BQ = -5;
	public static final int BK = -6;
	public static final int OB = -99;
	public static final int firstSquare = 21;
	public static final int lastSquare = 98;
	
	public static final int startingBoard[] = {OB,OB,OB,OB,OB,OB,OB,OB,OB,OB,
									   OB,OB,OB,OB,OB,OB,OB,OB,OB,OB,
									   OB,BR,BN,BB,BQ,BK,BB,BN,BR,OB,
									   OB,BP,BP,BP,BP,BP,BP,BP,BP,OB,
									   OB,EM,EM,EM,EM,EM,EM,EM,EM,OB,
									   OB,EM,EM,EM,EM,EM,EM,EM,EM,OB,
									   OB,EM,EM,EM,EM,EM,EM,EM,EM,OB,
									   OB,EM,EM,EM,EM,EM,EM,EM,EM,OB,
									   OB,WP,WP,WP,WP,WP,WP,WP,WP,OB,
									   OB,WR,WN,WB,WQ,WK,WB,WN,WR,OB,
									   OB,OB,OB,OB,OB,OB,OB,OB,OB,OB,
									   OB,OB,OB,OB,OB,OB,OB,OB,OB,OB};

	static final String sp2 = "  ";
	static final String sp4 = "    ";
	static final String fenFieldSeparator = " ";
	
	int[] board;

	int[] kingMoves;
	int[] qsrMoves;
	boolean[] hasCastled;
	int ksrMoves[];
	int kingPos[];
	
	long hashEntries[];
	int numHashEntries;
	long[][][] zPieces;
	long zHash;
	long zSide;
	long zCastle[][];
	long oldHash[];
	
	int halfMoveClock;
	int oldHalfMoveClock;
	
	int[] kingDirections = {1,11,10,9,-1,-11,-10,-9};
	// Evaluation terms
	int[][] pawnColumn;
	int[] mobility;
	int material[];
	int[] numKingDefenders;
	int[] numKingAttackers;
	int[] piecePlacement;
	
	static Random rnd;

	static int currentMoveNum;
	static Move[] gameMoves;

	public Board()
	{
		gameMoves = new Move[MoveGenerator.maxMoves];
		for (int i=0;i<MoveGenerator.maxMoves;i++)
		{
			gameMoves[i]=new Move();
		}
		hasCastled = new boolean[2];
		board=new int[boardSize];
		kingMoves = new int[2];
		qsrMoves=new int[2];
		ksrMoves=new int[2];
		kingPos = new int[2];
		hashEntries = new long[MoveGenerator.maxMoves];
		material = new int[2];
		pawnColumn = new int [2][boardWidth];
		mobility = new int[2];
		numKingDefenders = new int[2];
		numKingAttackers = new int[2];
		piecePlacement = new int[2];
		
		currentMoveNum=0;
		rnd = new Random(System.currentTimeMillis());
		zPieces = new long[6][2][boardSize];
	}
			 	 
	void initZHash()
	{
		// For each piece type, colour and board square
		// initialise with a random number
		for (int i=0;i<6;i++)
		{
			for (int j=0;j<2;j++)
			{
				for (int k=0;k<boardSize;k++)
				{
					zPieces[i][j][k] = rnd.nextLong();
				}
			}
		}
		zSide = rnd.nextLong();
		// Now set up the Zobrist Hash, based on the board
		for(int i = 0; i < boardSize; i++)
		{
			if (isPiece(i))
				zHash ^= zPieces[pieceTypeAt(i)-1][getColour(i)][i];
		}
	}
	
	public Move getLastMove()
	{
		if (currentMoveNum==0)
			return null;
		else
			return gameMoves[currentMoveNum-1];
	}
	
	public int getMoveNumber()
	{
		return currentMoveNum;
	}
	
	public void makeMove(Move m)
	{
		oldHalfMoveClock = halfMoveClock;
		halfMoveClock++;
		Move.copy(gameMoves[currentMoveNum++],m);
		int from=m.from();
		int to=m.to();
		int fromValue = Math.abs(board[from]);
		int colour = (board[from]>0?WHITE:BLACK);
		material[colour]+=m.getCaptureValue();
		 
		board[to]=board[from];
		board[from]=EM;
		// Remove 'from' piece from Hash
		zHash^= zPieces[fromValue-1][colour][from];
		// Remove any captured piece from Hash
		if (m.getCaptureValue() > 0)
		{
			halfMoveClock = 0;
			zHash^= zPieces[m.getCapturePieceType()-1][flipSides(colour)][to];
		}
		// Add 'to' piece to Hash
		zHash^= zPieces[fromValue-1][colour][to];
		// Same board position but who is moving is critical to identifying as the same board
		if (colour==WHITE)
			zHash ^= zSide;
		 
		 int toColumn = to%boardWidth;
		 
		 if (m.getCapturePieceType()==WP)
			 pawnColumn[flipSides(colour)][toColumn]--;
		 
		 switch(fromValue)
		 {
		 	case WP:
				 halfMoveClock = 0;
				 pawnColumn[colour][from%boardWidth]--;
				 pawnColumn[colour][to%boardWidth]++;
				 if (Math.abs(from-to)%boardWidth != 0 && m.getCapture() == EM) //i.e. is EnPassant pawn capture
				 {
					board[(colour==WHITE?to+boardWidth:to-boardWidth)] = EM;
					// Remove the enpassant pawn from the hash
					zHash^= zPieces[WP-1][flipSides(colour)][(colour==WHITE?to+boardWidth:to-boardWidth)];
					material[colour]+=Move.getPieceValue(WP);
					pawnColumn[flipSides(colour)][toColumn]--;
				 } 
		
				 if (m.isPawnPromotion())
				 {
					 board[to]=(colour==WHITE?m.getQueeningPiece():-m.getQueeningPiece());
					 pawnColumn[colour][toColumn]--;
					 material[colour]+=(Move.getPieceValue(WQ)-Move.getPieceValue(WP));
					 // Remove a pawn from the Hash
					 zHash^= zPieces[WP-1][colour][to];
					 // Add a queen to the Hash
					 zHash^= zPieces[m.getQueeningPiece()-1][colour][to];
				 }                                                         
				 break;
		 	case WR:
				 if (from==91)
					 qsrMoves[WHITE]++;
				 if (from==21)
					 qsrMoves[BLACK]++;
				 if (from==98)
					 ksrMoves[WHITE]++;
				 if (from==28)
					 ksrMoves[BLACK]++;
				 break;
		 
		 	case WK:
				kingPos[colour]=to;
				kingMoves[colour]++;
		
				 if ((from-to)==2) //Queen Side
					 {
						 hasCastled[colour]=true;
						 qsrMoves[colour]++;
						 board[to-2]=EM;
						 board[to+1]=(colour==WHITE?WR:BR);
						 // Remove a rook from the Hash
						 zHash^= zPieces[WR-1][colour][to-2];
						 // Add a rook to the Hash
						 zHash^= zPieces[WR-1][colour][to+1];
					 }
					 else if ((to-from)==2)
					 {
						 hasCastled[colour]=true;
						 ksrMoves[colour]++;
						 board[to+1]=EM;
						 board[to-1]=(colour==WHITE?WR:BR);
						 // Remove a rook from the Hash
						 zHash^= zPieces[WR-1][colour][to+2];
						 // Add a rook to the Hash
						 zHash^= zPieces[WR-1][colour][to-1];
					 }
			break;
		 }
		 hashEntries[numHashEntries++]=zHash;
	 }
	
	public void takeBackMove(Move m)
	 {
		 halfMoveClock = oldHalfMoveClock;
		 int from=m.from();
		 int to=m.to();
		 board[from]=board[to];
		 board[to]=m.getCapture();
	
		 int colour = (board[from]>0?WHITE:BLACK);
		 material[colour]-=m.getCaptureValue();
		 if (m.isPawnPromotion())
		 {
			 pawnColumn[colour][to%boardWidth]++;
			 board[from]=(colour==WHITE?WP:BP);
			 material[colour]-=(Move.getPieceValue(WQ)-Move.getPieceValue(WP));
			 // Remove a queen from the Hash
			 zHash^= zPieces[m.getQueeningPiece()-1][colour][to];
			 // Add a pawn to the Hash
			 zHash^= zPieces[WP-1][colour][to];
		 }
		 
		 int fromValue = Math.abs(board[from]);
		 int toColumn = to%boardWidth;
		 int fromColumn = from%boardWidth;
		 // Remove 'to' piece from Hash
		 zHash^= zPieces[fromValue-1][colour][to];
		 // Remove any captured piece from Hash
		 if (m.getCapturePieceType() > 0)
			 zHash^= zPieces[m.getCapturePieceType()-1][flipSides(colour)][to];
		 // Add 'from' piece to Hash
		 zHash^= zPieces[fromValue-1][colour][from];
		
		 if (colour==WHITE)
			zHash ^= zSide;
		 
		 switch(fromValue)
		 {
		 	case WP:
		 		pawnColumn[colour][fromColumn]++;
		 		pawnColumn[colour][toColumn]--;
				if (Math.abs(from-to)%boardWidth!=0 && m.getCapture() == EM) //i.e. is EnPassant pawn capture
				{
					board[from+toColumn-fromColumn] = (colour==WHITE?BP:WP);
					material[colour]-=Move.getPieceValue(WP);
					pawnColumn[flipSides(colour)][toColumn]++;
					// Add the enpassant pawn back to the hash
					zHash^= zPieces[WP-1][flipSides(colour)][(colour==WHITE?to+boardWidth:to-boardWidth)];
				} 
		 		break;
		 	case WR:
		 		if (from==91)
		 			qsrMoves[WHITE]--;
		 		if (from==21)
		 			qsrMoves[BLACK]--;
		 		if (from==98)
		 			ksrMoves[WHITE]--;
		 		if (from==28)
		 			ksrMoves[BLACK]--;
		 		break;
			 
		 	case WK:
		 		kingPos[colour]=from;
		 		kingMoves[colour]--;
		 		if ((from-to)==2) //Queen Side
		 		{
					hasCastled[colour]=false;
					qsrMoves[colour]--;
		 			board[to-2]=(colour==WHITE?WR:BR);
		 			board[to+1]=EM;
					 // Add a rook back to the Hash
					 zHash^= zPieces[WR-1][colour][to-2];
					 // Remove a rook from the Hash
					 zHash^= zPieces[WR-1][colour][to+1];
		 		}
		 		else if ((to-from)==2)
		 		{
					hasCastled[colour]=false;
					ksrMoves[colour]--;
		 			board[to+1]=(colour==WHITE?WR:BR);
		 			board[to-1]=EM;
					// Add a rook back to the Hash
					zHash^= zPieces[WR-1][colour][to+2];
					// Remove a rook from the Hash
					zHash^= zPieces[WR-1][colour][to-1];
		 		}
	 		break;
		 }		 
		 // If I've captured a pawn, they've lost one from this column, so put it back
		 if (m.getCapturePieceType() == WP)
			 pawnColumn[flipSides(colour)][to%boardWidth]++;
		 currentMoveNum--;
		 if (numHashEntries > 0)
			 numHashEntries --;
	 }
	 
	 boolean inEndGame()
	 {
		 return currentMoveNum > 30;
	 }

	public void newGame()
	{
		System.arraycopy(startingBoard, 0, board, 0, boardSize);
		kingPos[WHITE]=95;
		kingPos[BLACK]=25;
		
		kingMoves[WHITE]=0;
		kingMoves[BLACK]=0;
		ksrMoves[WHITE]=0;
		ksrMoves[BLACK]=0;
		qsrMoves[WHITE]=0;
		qsrMoves[BLACK]=0;
		hasCastled[WHITE]=false;
		hasCastled[BLACK]=false;
		currentMoveNum=0;
		numHashEntries = 0;
		
		material[WHITE]=0;
		material[BLACK]=0;
		mobility[WHITE]=0;
		mobility[BLACK]=0;
		for (int i=1;i<boardWidth-1;i++)
		{
			pawnColumn[WHITE][i]=1;
			pawnColumn[BLACK][i]=1;
		}
		
		initZHash();
		
		halfMoveClock=0;
	}
	
	public boolean inCheck(int aCol)
	{
		return attacked(kingPos[aCol],flipSides(aCol));
	}

	// Is square 'sq' attacked by colour'?
	boolean attacked(int sq,int colour)
	{
		int direction=colour==WHITE?1:-1;
		int oppositeColourSign = colour==WHITE?1:-1;
		if (board[sq+direction*(boardWidth-1)]== oppositeColourSign*WP || board[sq+direction*(boardWidth+1)]==oppositeColourSign*WP)
			return true;
		for (int i=0;i<MoveGenerator.numNDirs;i++)
		{
			if (board[sq+MoveGenerator.nDir[i]]==oppositeColourSign*WN)
				return true;
		}
		for (int i=0;i<MoveGenerator.numBDirs;i++)
		{
			int aSq=sq+MoveGenerator.bDir[i];
			if (board[aSq]==oppositeColourSign*WK)
				return true;
			while(board[aSq]==EM)
				aSq+=MoveGenerator.bDir[i];
			if (board[aSq]==oppositeColourSign*WB || board[aSq]==oppositeColourSign*WQ)
				return true;
		}
		for (int i=0;i<MoveGenerator.numRDirs;i++)
		{
			int aSq=sq+MoveGenerator.rDir[i];
			if (board[aSq]==oppositeColourSign*WK)
				return true;
			while(board[aSq]==EM)
				aSq+=MoveGenerator.rDir[i];
			if (board[aSq]==oppositeColourSign*WR || board[aSq]==oppositeColourSign*WQ)
				return true;
		}
		return false;
	}

	boolean canKSCastle(int aCol)
	{
		if (kingMoves[aCol]>0 || ksrMoves[aCol]>0)
			return false;
		
		int oppositeColour = flipSides(aCol);
		int kp = (aCol==WHITE?95:25);
		return (board[kp+1]==EM &&
				board[kp+2]==EM &&
				board[kp+3]==(aCol==WHITE?WR:BR) &&
				!attacked(kp,oppositeColour) && 
				!attacked(kp+1,oppositeColour) && 
				!attacked(kp+2,oppositeColour));
	}

	boolean canQSCastle(int aCol)
	{
		if (kingMoves[aCol]>0 || qsrMoves[aCol]>0)
			return false;
		
		int oppositeColour = flipSides(aCol);
		int kp = (aCol==WHITE?95:25);
		
		return (board[kp-1]==EM &&
				board[kp-2]==EM &&
				board[kp-3]==EM &&
				board[kp-4]==(aCol==WHITE?WR:BR) &&
				!attacked(kp,oppositeColour) && 
				!attacked(kp-1,oppositeColour) && 
				!attacked(kp-2,oppositeColour));
	}
	
	public void setMobility(int colour,int score)
	{
		mobility[colour]=score;
	}
	
	public void setNumKingDefenders(int colour,int count)
	{
		numKingDefenders[colour]=count;
	}
	
	public void setNumKingAttackers(int colour,int count)
	{
		numKingAttackers[colour]=count;
	}
	
	public void setPiecePlacement(int colour,int count)
	{
		piecePlacement[colour]=count;
	}
	
	public void resetHashTable()
	{
		numHashEntries = 0;
	}
	
	public long getBoardHash()
	{
		return zHash;
	}
	
	public boolean isThreefoldRepetition()
	{
		int count = 0;
		long hash = getBoardHash();
		for (int i=0;i<numHashEntries;i++)
		{
			if (hash == hashEntries[i])
				count++;
		}
		return count==3;
	}
	
	// Evaluation routines
	public int evaluate(int colour)
	{	 
		int oppColour = Board.flipSides(colour);
		return  rnd.nextInt(3)+
				material(colour) - material(oppColour) +
			    3*(mobility(colour)-mobility(oppColour))+
			    pawnStructure(colour)-pawnStructure(oppColour)+
			    4*(kingAttack(colour)-kingAttack(oppColour))+ 
		   	    2*(kingSafety(colour)-kingSafety(oppColour))+
		   	    piecePlacement(colour) - piecePlacement(oppColour);
	}

	public String dumpEval()
	{
		String e ="Score: "+evaluate(WHITE)+" [MW:"+material(WHITE)+"/MB:"+material(BLACK)+"/MOBW:"+mobility(WHITE)+"/MOBB:"+mobility(BLACK)+
				  "/PSW:"+ pawnStructure(WHITE)+"/PSB:"+pawnStructure(BLACK)+
				  "/KAW:"+kingAttack(WHITE)+"/KAB:"+kingAttack(BLACK)+
				  "/KSW:"+kingSafety(WHITE)+"/KSB:"+kingSafety(BLACK)+
		          "/PPW:"+piecePlacement(WHITE)+"/PPB:"+piecePlacement(BLACK)+"]";
		
		e+="\n[WPC:";
		for (int j=0;j<boardWidth;j++)
			e+=pawnColumn[WHITE][j];
		e+="] [BPC:";
		for (int j=0;j<boardWidth;j++)
			e+=pawnColumn[BLACK][j];
		e+="]";
		return e;
	}
	
	int material(int colour)
	{
		return material[colour];
	}
	
	int piecePlacement(int colour)
	{
		return piecePlacement[colour];
	}
	
	int kingSafety(int colour)
	{
		int score = numKingDefenders[colour];
		int pawnProtection=colour==WHITE?-boardWidth:+boardWidth;
		int myPawn = (colour==WHITE?WP:BP);
		if (hasCastled[colour])
		{
			if (board[kingPos[colour]+pawnProtection]==myPawn)
				score+=5;
			if (board[kingPos[colour]+pawnProtection+1]==myPawn)
				score+=5;
			if (board[kingPos[colour]+pawnProtection-1]==myPawn)
				score+=5;
		}
		if (!hasCastled[colour])
			score-=20;
		return score;
	}

	int kingAttack(int colour)
	{
		return numKingAttackers[colour];
	}
	
	int mobility(int colour)
	{
		return mobility[colour];
	}
	
	int pawnStructure(int colour)
	{
		// Doubled, Passed & Isolated pawns
		int score = 0;
		for (int i=1;i<boardWidth-1;i++)
		{
			int myPawns=pawnColumn[colour][i];
			
			// Doubled pawn penalty
			if (myPawns > 1)
				score-=5*(myPawns-1);
			
			// Check if we have passed pawns
			if (myPawns > 0 && pawnColumn[flipSides(colour)][i-1]==0 && pawnColumn[flipSides(colour)][i+1]==0)
			{
				boolean foundPawn = false;
				int passedPawnPos = 0;
				int pos = boardWidth+i;
				int increment = boardWidth;
				int myPawn = BP;
				if (colour == WHITE)
				{
					pos = boardSize-boardWidth+i;
					increment = -boardWidth;
					myPawn = WP;
				}
				// Find our pawn in this column
				while (pos > 0 && pos <boardSize && board[pos] != myPawn)
					pos+=increment;
				// passedPawnPos is the potential location of a passed pawn
				passedPawnPos=pos;
				if (pawnColumn[flipSides(colour)][i]!=0)
				{			
					// Now search further down the board and see if we find an enemy pawn
					pos+=increment;
					while(pos > 0 && pos <boardSize)
					{
						if (board[pos]==-myPawn)
						{
							foundPawn=true;
							break;
						}
						pos+=increment;
					}
				}
				// Passed pawn
				if (!foundPawn)
				{
					int row = passedPawnPos/boardWidth;
					// Bonus increased the nearer they are to the last rank
					if (colour == WHITE)
						score+=15*(10-row);
					else
						score+=15*row;
				}

			}
			// Isolated pawn penalty
			if (myPawns > 0 && pawnColumn[colour][i-1]==0 && pawnColumn[colour][i+1]==0)
				score-=5;
		}
		return score;
	}
	
	// Board Access Utilities
	public int pieceAt(int sq)
	{
		return board[sq];
	}
	
	public int pieceTypeAt(int sq)
	{
		return Math.abs(board[sq]);
	}
	
	public boolean isPiece(int sq)
	{
		return board[sq] != OB && board[sq] != 0;
	}
	
	public boolean isWhitePiece(int sq)
	{
		return board[sq] > 0 && board[sq] != Board.OB;
	}
	
	public boolean isBlackPiece(int sq)
	{
		return board[sq] < 0 && board[sq] != Board.OB;
	}
	
	public boolean isEmpty(int sq)
	{
		return board[sq] == EM;
	}
	
	public boolean isOffBoard(int sq)
	{
		return board[sq] == OB;
	}
	
	public static int flipSides(int aCol)
	{
		return aCol==Board.WHITE?Board.BLACK:Board.WHITE;
	}

	public boolean oppositeSides(int sq1,int sq2)
	{
		return board[sq1] != OB && board[sq2] != OB &&  
			   ((board[sq1]>0 && board[sq2]<0) || (board[sq1]<0 && board[sq2]>0));
	}
	
	public boolean mySide(int colour,int piece)
	{
		return colour==WHITE?(piece>0):(piece<0 && piece !=OB);
	}

	public boolean pawnOnStartingSquare(int sq, int colour)
	{
		return Math.abs(board[sq]) == WP && (colour == WHITE?sq >= firstWhitePawn:sq <=lastBlackPawn);
	}
	
	public int getColour(int sq)
	{
		return board[sq] >0?WHITE:BLACK;
	}
	
	int getKingPos(int colour)
	{
		return kingPos[colour];
	}
	 
	// How many pawns for colour in the column where sq is?
	int getPawnsInColumn(int colour,int sq)
	{
		return pawnColumn[colour][sq%boardWidth];
	}
	
	public int pieceValueAt(int sq)
	{
		return Move.pieceValue[Math.abs(board[sq])];
	}

	public static String getColumnString(int sq)
	{
		if (sq >= 21 && sq <=98)
			return " abcdefgh".substring(sq%Board.boardWidth,sq%Board.boardWidth+1);
		else
			return "";
	}

	public static String getRowString(int sq)
	{
		if (sq >=21 && sq <=98)
			return ""+(10-sq/Board.boardWidth);
		else 
			return "";
	}
		
	public static int alg2Square(String sq)
	{
		int column = 1+"abcdefgh".indexOf(sq.substring(0,1));
		int row = 2+"87654321".indexOf(sq.substring(1,2));
		return row*Board.boardWidth+column;
	}
	
	public String getFENString()
	{
		String fenStr = "";
		Move lastMove = getLastMove();
		System.out.println(lastMove.getString());
		int lastMoveSide = getColour(lastMove.to());
		int numConsecutiveEmpty = 0;
		
		// Field 1: Board
		for (int i=0;i<boardSize;i++)
		{
			if (i>=firstBlackPiece && i<=lastWhitePiece)
			{
				if (isEmpty(i))
					numConsecutiveEmpty++;
				else
				{
					if (numConsecutiveEmpty > 0)
						fenStr+=""+numConsecutiveEmpty;
					numConsecutiveEmpty = 0;
					if (isOffBoard(i) && isOffBoard(i+1))
						fenStr+="/";
					if (isWhitePiece(i))
						fenStr+=Move.whitePieceStr[pieceTypeAt(i)];
					else if (isBlackPiece(i))
						fenStr+=Move.blackPieceStr[pieceTypeAt(i)];
				}
				
			}
		}
		
		fenStr+=fenFieldSeparator;	
		
		// Field 2: Next to move
		fenStr+=lastMoveSide == WHITE?"b":"w";

		fenStr+=fenFieldSeparator;	
		
		// Field 3: Castling rights
		String castleStr="";
		castleStr+=(!hasCastled[WHITE] && kingMoves[WHITE] ==0 && ksrMoves[WHITE]==0)?"K":"";
		castleStr+=(!hasCastled[WHITE] && kingMoves[WHITE] ==0 && qsrMoves[WHITE]==0)?"Q":"";
		castleStr+=(!hasCastled[BLACK] && kingMoves[BLACK] ==0 && ksrMoves[BLACK]==0)?"k":"";
		castleStr+=(!hasCastled[BLACK] && kingMoves[BLACK] ==0 && qsrMoves[BLACK]==0)?"q":"";
		if (castleStr.length() == 0)
			fenStr+="-";
		else
			fenStr+=castleStr;
		
		fenStr+=fenFieldSeparator;	

		// Field 4: En Passant Square
		if (lastMove.isDoublePawnMove())
		{
			int epSquare = lastMove.to()+(lastMoveSide==WHITE?boardWidth:-boardWidth);
			fenStr+=getColumnString(epSquare)+getRowString(epSquare);
		}
		else
			fenStr+="-";

		fenStr+=fenFieldSeparator;	

		// Field 5: Half-move clock (number of half-moves since last capture or pawn move
		fenStr+=""+halfMoveClock;
		
		fenStr+=fenFieldSeparator;	

		// Field 6: Move Number
		fenStr+=""+(1+currentMoveNum/2);
		
		return fenStr;
	}
	
	public void setupBoardFromFenStr(String fenStr)
	{
		newGame();
		ArrayList<String> fenFields = createFENFields(fenStr);
		
		String field1=fenFields.get(0);
		int currentSq=firstBlackPiece;
		for (int i=0;i<field1.length();i++)
		{
			String sqStr=field1.substring(i, i+1);
			if (sqStr.equals("/"))
			{
				currentSq+=2;
				continue;
			}
			int numEmpties="12345678".indexOf(sqStr);
			if (numEmpties !=-1)
				for (int j=0;j<=numEmpties;j++)
					board[currentSq++]=EM;
			else
			{
				int piece = Move.pieceFromStr(sqStr);
				if (piece != Move.NOT_A_PIECE)
					board[currentSq++] = piece;
			}
		}
		// To be Implemented
		String field2=fenFields.get(1);
		
		String field3=fenFields.get(2);
		if (field3.indexOf("K") == -1 || field3.indexOf("Q") == -1) hasCastled[WHITE] = true;
		if (field3.indexOf("k") == -1 || field3.indexOf("q") == -1) hasCastled[BLACK] = true;

		String field4=fenFields.get(3);
		if (!field4.equals("-"))
		{
			// Set the last move to be a double-pawn move to the En Passant Square
			int sq = alg2Square(field4);
			int sqRow=sq/boardWidth;
			int from,to,piece;
			if (sqRow==5) // Must be White
			{
				from=sq+boardWidth;
				to=sq-boardWidth;
				piece=WP;
			}
			else
			{
				from=sq-boardWidth;
				to=sq+boardWidth;
				piece=BP;
			}
			gameMoves[currentMoveNum++]= new Move(piece,from,to);
		}
	}
	
	static ArrayList<String> createFENFields(String fenStr)
	{
		ArrayList<String> fenFields = new ArrayList<String>();
		
		while(true)
		{
			int index = fenStr.indexOf(fenFieldSeparator);
			
			if (index == -1)
				break;
			String fenField = fenStr.substring(0, index);
			fenFields.add(fenField);
			fenStr=fenStr.substring(index+1);
		}
		return fenFields;
	}
	
	public void display()
	{
		System.out.println("  "+sp2+"A"+sp4+"B"+sp4+"C"+sp4+"D"+sp4+"E"+sp4+"F"+sp4+"G"+sp4+"H");
		for (int i=2;i<10;i++)
		{
			String row = ""+(10-i)+" ";
			for (int j=0;j<boardWidth;j++)
			{
				int sq =i*boardWidth+j;
				if (isWhitePiece(sq))
					row+=sp2+Move.whitePieceStr[pieceTypeAt(sq)]+sp2;
				else if (isBlackPiece(sq))
					row+=sp2+Move.blackPieceStr[pieceTypeAt(sq)]+sp2;
				else if (isEmpty(sq))
					row+=sp2+"."+sp2;
			}
			System.out.println(row+(10-i)+"\n");
		}
		System.out.println("  "+sp2+"A"+sp4+"B"+sp4+"C"+sp4+"D"+sp4+"E"+sp4+"F"+sp4+"G"+sp4+"H");
	}
	
	public int setPosition(String moveStr)
	{
		// We currently assume there is always a startpos present in a position command
		newGame();
		currentMoveNum = 0;
		int colToMove=WHITE;
		ArrayList<Move> moves = Move.string2Moves(moveStr);
		for (Move m: moves)
		{
			m.setCapture(pieceAt(m.to()));
			m.setPiece(pieceAt(m.from()));
			makeMove(m);
			currentMoveNum++;
			colToMove = flipSides(colToMove);
		}
		return colToMove;
	}
}