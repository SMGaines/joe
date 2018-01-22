package com.smg.joe;

import java.util.ArrayList;

public class Move 
{
	static final int NO_MOVE = -1;
	
	static final String[] whitePieceStr = {"","P","N","B","R","Q","K"};
	static final String[] blackPieceStr = {"","p","n","b","r","q","k"};
	
	static int[] pieceValue = {0,100,300,320,500,900,10000};
	
	int from;
	int to;
	int score;
	int piece;
	int capturedPiece;
	int queeningPiece;
	
	Move()
	{
		from=0;
		to=0;
		piece = 0;
		score=0;
		capturedPiece=Board.EM;
		queeningPiece=Board.WQ;
	}
	
	Move(int aPiece, int aFrom,int aTo,int aCapture)
	{
		piece = aPiece;
		from=aFrom;
		to=aTo;
		score = 0;
		capturedPiece=aCapture;
		queeningPiece=Board.WQ;
	}
	
	Move(int aPiece, int aFrom,int aTo)
	{
		piece = aPiece;
		from=aFrom;
		to=aTo;
		score = 0;
		capturedPiece=Board.EM;
		queeningPiece=Board.WQ;
	}
	
	static void copy(Move m1,Move m2)
	{
		m1.piece = m2.piece;
		m1.from=m2.from;
		m1.to=m2.to;
		m1.score = m2.score;
		m1.capturedPiece=m2.capturedPiece;
		m1.queeningPiece=m2.queeningPiece;
	}
	
	int getQueeningPiece()
	{
		return queeningPiece;
	}
	
	int getPiece()
	{
		return piece;
	}
	
	int getCapturePieceType()
	{
		return Math.abs(capturedPiece);
	}
	
	int getPieceType()
	{
		return Math.abs(piece);
	}
	
	int getCaptureValue()
	{
		return pieceValue[Math.abs(capturedPiece)];
	}
	
	boolean isSameMoveAs(Move aMove)
	{
		return piece==aMove.piece && from == aMove.from && to == aMove.to;
	}
	
	void set(int aPiece, int aFrom,int aTo, int aCapture)
	{
		piece = aPiece;
		from=aFrom;
		to=aTo;
		capturedPiece = aCapture;
	}
	
	void setPiece(int aPiece)
	{
		piece=aPiece;
	}
	
	void setScore(int aScore)
	{
		score=aScore;
	}
	
	void setCapture(int aCapture)
	{
		capturedPiece = aCapture;
	}
	
	static int getPieceValue(int aPiece)
	{
		return pieceValue[aPiece];
	}
	
	int from()
	{
		return from;
	}
	
	int to()
	{
		return to;
	}
	
	int getCapture()
	{
		return capturedPiece;
	}
	
	int getScore()
	{
		return score;
	}
	
	String getString()
	{
		return Board.getColumnString(from)+Board.getRowString(from)+
			   Board.getColumnString(to)+Board.getRowString(to)+(this.isPawnPromotion()?"Q":"");
	}

	boolean isDoublePawnMove()
	{
		return Math.abs(piece) == Board.WP && Math.abs(from-to) > Board.boardWidth+1;
	}
	
	boolean isCastlingMove()
	{
		return Math.abs(piece) == Board.WK && Math.abs(from-to) ==2;
	}
	
	boolean isPawnCaptureMove()
	{
		return Math.abs(from-to)%Board.boardWidth !=0;
	}
	
	boolean isPawnPromotion()
	{
		return (this.getPieceType() == Board.WP && (to < 30 || to > 90) );
	}

	//eg e2e4 to an int move
	static Move alg2Move(String moveStr)
	{
		return new Move(0,Board.alg2Square(moveStr.substring(0,2)),Board.alg2Square(moveStr.substring(2,4)));
	}
	
	static ArrayList<Move> string2Moves(String moveStr)
	{
		// Convert a UCI move string to an arraylist of moves
		ArrayList<Move> moves = new ArrayList<Move>();
		String mStr = moveStr;
		while (true)
		{
			mStr=mStr.trim();
			int pos = mStr.indexOf(" ");
			if (pos == -1)
			{
				moves.add(alg2Move(mStr.substring(0)));
				break;
			}
			moves.add(alg2Move(mStr.substring(0, pos)));
			mStr=mStr.substring(pos+1);
		}
		return moves;
	}

}
