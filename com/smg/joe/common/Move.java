package com.smg.joe.common;

import java.util.ArrayList;

public class Move 
{
	public static final int NO_MOVE = -1;
	public static final int NOT_A_PIECE = -999;
	
	public static final String[] whitePieceStr = {"","P","N","B","R","Q","K"};
	public static final String[] blackPieceStr = {"","p","n","b","r","q","k"};
	
	public static int[] pieceValue = {0,100,300,320,500,900,10000};
	
	int from;
	int to;
	int piece;
	int capturedPiece;
	int queeningPiece;
	
	public Move()
	{
		from=0;
		to=0;
		piece = 0;
		capturedPiece=Board.EM;
		queeningPiece=Board.WQ;
	}
	
	public Move(int aPiece, int aFrom,int aTo,int aCapture)
	{
		piece = aPiece;
		from=aFrom;
		to=aTo;
		capturedPiece=aCapture;
		queeningPiece=Board.WQ;
	}
	
	public Move(int aPiece, int aFrom,int aTo)
	{
		piece = aPiece;
		from=aFrom;
		to=aTo;
		capturedPiece=Board.EM;
		queeningPiece=Board.WQ;
	}
	
	static int pieceFromStr(String pieceStr)
	{
		for (int i=0;i<whitePieceStr.length;i++)
		{
			if(whitePieceStr[i].equals(pieceStr))
				return i;
			if(blackPieceStr[i].equals(pieceStr))
				return -i;
		}
		return NOT_A_PIECE;
	}
	
	public static void copy(Move m1,Move m2)
	{
		m1.piece = m2.piece;
		m1.from=m2.from;
		m1.to=m2.to;
		m1.capturedPiece=m2.capturedPiece;
		m1.queeningPiece=m2.queeningPiece;
	}
	
	public int getQueeningPiece()
	{
		return queeningPiece;
	}
	
	public int getPiece()
	{
		return piece;
	}
	
	public int getCapturePieceType()
	{
		return Math.abs(capturedPiece);
	}
	
	public int getPieceType()
	{
		return Math.abs(piece);
	}
	
	public int getCaptureValue()
	{
		return pieceValue[Math.abs(capturedPiece)];
	}
	
	public boolean isSameMoveAs(Move aMove)
	{
		return piece==aMove.piece && from == aMove.from && to == aMove.to;
	}
	
	public void set(int aPiece, int aFrom,int aTo, int aCapture)
	{
		piece = aPiece;
		from=aFrom;
		to=aTo;
		capturedPiece = aCapture;
	}
	
	public void setPiece(int aPiece)
	{
		piece=aPiece;
	}
	
	public void setCapture(int aCapture)
	{
		capturedPiece = aCapture;
	}
	
	public static int getPieceValue(int aPiece)
	{
		return pieceValue[aPiece];
	}
	
	public int from()
	{
		return from;
	}
	
	public int to()
	{
		return to;
	}
	
	public int getCapture()
	{
		return capturedPiece;
	}
	
	public String getString()
	{
		return Board.getColumnString(from)+Board.getRowString(from)+
			   Board.getColumnString(to)+Board.getRowString(to)+(this.isPawnPromotion()?"Q":"");
	}

	public boolean isDoublePawnMove()
	{
		return Math.abs(piece) == Board.WP && Math.abs(from-to) > Board.boardWidth+1;
	}
	
	public boolean isCastlingMove()
	{
		return Math.abs(piece) == Board.WK && Math.abs(from-to) ==2;
	}
	
	public boolean isPawnCaptureMove()
	{
		return Math.abs(from-to)%Board.boardWidth !=0;
	}
	
	public boolean isPawnPromotion()
	{
		return (this.getPieceType() == Board.WP && (to < 30 || to > 90) );
	}

	//eg e2e4 to an int move
	public static Move alg2Move(String moveStr)
	{
		int from = Board.alg2Square(moveStr.substring(0,2));
		int to = Board.alg2Square(moveStr.substring(2,4));
		if (from == NO_MOVE || to == NO_MOVE)
			return null;
		return new Move(0,from,to);
	}
	
	public static ArrayList<Move> string2Moves(String moveStr)
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