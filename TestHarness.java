package com.smg.joe;

import java.util.Random;

public class TestHarness 
{
	static Joe joe;
	static int[] score;
	static Board brd;

	static Random rnd; 
	public static void main(String[] args) 
	{
		playGame();
	}
	
	static void playGame()
	{
		int col = Board.WHITE;
		joe = new Joe();
		brd = new Board();
		brd.newGame();
		joe.newGame();
		long totalNodes=0;
		rnd = new Random(System.currentTimeMillis());
		for (int i=0;i<30;i++)
		{
			System.out.println(i);
			Move m = joe.findBestMove(brd, col, 5000);
			brd.makeMove(m);
			System.out.println(joe.getNumTransHits()+"/"+joe.getABCalls());
			System.out.println(brd.getFENString());
			System.out.println(brd.dumpEval());
			if (m == null)
			{
				System.out.println("null");
				break;
			}
			totalNodes+=joe.getMainNodes();
			col=1-col;
		}
		brd.display();
		System.out.println("Total nodes: "+totalNodes);
	}
}
