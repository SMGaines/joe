package com.smg.joe.server;

public class TranspositionEntry 
{
	static final int transMaxMoves = 100;
	long hash;
	int score;
	int depth;
	
	TranspositionEntry()
	{
		hash=0;
		depth = Integer.MIN_VALUE;
		score=Integer.MIN_VALUE;
	}
	
	void set(int aDepth, long aHash,int aScore)
	{
		depth=aDepth;
		hash = aHash;
		score=aScore;
	}
	
	void reset()
	{
		hash=0;
		depth = Integer.MIN_VALUE;
		score=Integer.MIN_VALUE;
	}
	
	int getScore()
	{
		return score;
	}
	
	int getDepth()
	{
		return depth;
	}
	
	long getBoardHash()
	{
		return hash;
	}
}
