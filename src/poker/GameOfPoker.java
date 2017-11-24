package poker;

import akka.actor.ActorRef;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;


public class GameOfPoker implements Runnable{
	public  static PrintStream defaultPrintStream = System.out;
	public String playerName = "";
	private DeckOfCards deck;
	public HumanPokerPlayer humanPlayer;
	//private TwitterInteraction twitter;
	ArrayList<PokerPlayer> players = new ArrayList<PokerPlayer>(6);
	boolean playerWin = false;
	boolean playerLose = false;
	boolean continueGame = true;

	ActorRef player;
	ActorRef dealer;
	OutputTerminal a;

	/*
	public GameOfPoker(String username, TwitterInteraction t, DeckOfCards d) throws InterruptedException{
		playerName = username;
		deck = d;
		humanPlayer = new HumanPokerPlayer(deck, t);
		twitter = t;
		players.add(humanPlayer);
		for(int i=0;i<5;i++){
			PokerPlayer computerPlayer = new AutomatedPokerPlayer(deck, twitter);
			players.add(computerPlayer);	
		}
	}*/
	/*
	public GameOfPoker(String username,  DeckOfCards d) throws InterruptedException{
		playerName = username;
		deck = d;
		humanPlayer = new HumanPokerPlayer(deck);
		players.add(humanPlayer);
		for(int i=0;i<5;i++){
			PokerPlayer computerPlayer = new AutomatedPokerPlayer(deck, a);
			players.add(computerPlayer);	
		}
	}*/
	
	

	public static final int PLAYER_POT_DEFAULT = 20;
	public static final int ROUND_NUMBER = 0;
	int ante = 1;

	public GameOfPoker(ActorRef dealer, ActorRef player, DeckOfCards deck) throws InterruptedException {
		this.player = player;
		this.dealer = dealer;
		playerName = player.path().name();
		this.deck = deck;
		a = new OutputTerminal(dealer, player);
		humanPlayer = new HumanPokerPlayer(deck,dealer,player,a);
		players.add(humanPlayer);
		for(int i=0;i<5;i++){
			PokerPlayer computerPlayer = new AutomatedPokerPlayer(deck, a);
			players.add(computerPlayer);
		}

	}

	//Runnable code segment
	@Override
	public void run() {
		System.out.println("getting into run");

		try {
			while(!playerWin && !playerLose && continueGame && !(Thread.currentThread().isInterrupted())){
				HandOfPoker handOfPoker = new HandOfPoker(players,ante,deck,a, player, dealer);

				ArrayList<PokerPlayer> nextRoundPlayers = new ArrayList<PokerPlayer>();
				
				for(int i=0;i<players.size();i++){
					if(!(players.get(i).playerPot<=0)){
						nextRoundPlayers.add(players.get(i));
					}
					else{
						player.tell("---Player "+players.get(i).playerName+" is out of chips, and out of the game.---",dealer);
						if(players.get(i).isHuman()){
							playerLose = true;
							player.tell("Sorry, you are out of the game. Goodbye and thanks for playing!",dealer);
							player.tell("To play again, Tweet with #FOAKDeal",dealer);
							break;
						}
					}
				}
				players = nextRoundPlayers;
				
				if(players.size()==1 && !playerLose){
					if(players.get(0).isHuman()){
						player.tell("You have beaten the bots and won the game! Congratulations!", dealer);
						player.tell("To play another game, Tweet with #FOAKDeal !",dealer);
						playerWin = true;
					}
					else{
						player.tell("Hard luck, you have lost the game.", dealer);
						player.tell("You can play again by Tweeting #FOAKDeal", dealer);
						playerLose = true;
					}
				}
				/*
				if(TwitterStreamer.gamesOfPoker.containsKey(playerName)){
					if(TwitterStreamer.userHasQuit(playerName) == true){
						break;
					}
				}
				*/
			}
			/*
			if(playerWin || playerLose){
				TwitterStreamer.usersPlayingGames.remove(playerName);
				TwitterStreamer.gamesOfPoker.remove(playerName);
			}*/
		}  catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		DeckOfCards deck = new DeckOfCards();
		GameOfPoker test = new GameOfPoker(null,null, deck);
		test.run();
	}

}
