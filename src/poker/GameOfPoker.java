package poker;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;


public class GameOfPoker implements Runnable{
	public  static PrintStream defaultPrintStream = System.out;
	public String playerName = "";
	public ActorRef humanPlayer, deck;
	ArrayList<ActorRef> players = new ArrayList<ActorRef>(6);
	boolean playerWin = false;
	boolean playerLose = false;
	boolean continueGame = true;

	ActorRef player;
	ActorRef dealer;
	OutputTerminal a;
	ActorSystem system;

	public static final int PLAYER_POT_DEFAULT = 20;
	public static final int ROUND_NUMBER = 0;
	int ante = 1;
	ActorSystem gameSystem;

	public GameOfPoker(ActorRef dealer, ActorRef player, ActorRef deck, ActorSystem gameSystem) throws InterruptedException {
		system = gameSystem;
		this.player = player;
		this.dealer = dealer;
		playerName = player.path().name();
		this.deck = deck;
		a = new OutputTerminal(dealer, player);
		humanPlayer = gameSystem.actorOf(Props.create(HumanPokerPlayer.class, deck, dealer, player, a), "Human");
		players.add(humanPlayer);
		for(int i=0;i<2;i++){
			ActorRef computerPlayer = gameSystem.actorOf(Props.create(AutomatedPokerPlayer.class, deck, a), "Comp"+(i+1));
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
				continueGame = handOfPoker.gameLoop();

				System.out.println("continuegame was "+continueGame);
				if(continueGame == false){
					a.printout("Goodbye, thanks for playing!");
					player.tell(PoisonPill.getInstance(),dealer);
					dealer.tell(PoisonPill.getInstance(), ActorRef.noSender());
					system.shutdown();
					break;
				}

				ArrayList<ActorRef> nextRoundPlayers = new ArrayList<>();
				
				for(int i=0;i<players.size();i++){

					Timeout timeout = new Timeout(Duration.create(HandOfPoker.TIMEOUT, "seconds"));
					Future<Object> future = Patterns.ask(players.get(i), "player pot", timeout);
					int result = 0;
					try {
						result = (Integer) Await.result(future, timeout.duration());
					} catch (Exception e) {
						e.printStackTrace();
					}

					if(!(result<=0)){
						nextRoundPlayers.add(players.get(i));
					}
					else{
						player.tell("---Player "+players.get(i).path().name()+" is out of chips, and out of the game.---",dealer);

						future = Patterns.ask(players.get(i), "are you human?", timeout);
						boolean isHuman = false;
						try {
							isHuman = (Boolean) Await.result(future, timeout.duration());
						} catch (Exception e) {
							e.printStackTrace();
						}

						if(isHuman){
							playerLose = true;
							player.tell("Sorry, you are out of the game. Goodbye and thanks for playing!",dealer);
							player.tell("To play again, Tweet with #FOAKDeal",dealer);
							break;
						}
					}
				}
				players = nextRoundPlayers;
				
				if(players.size()==1 && !playerLose){
					Timeout timeout = new Timeout(Duration.create(HandOfPoker.TIMEOUT, "seconds"));
					Future<Object>future = Patterns.ask(players.get(0), "are you human", timeout);
					boolean isHuman = false;
					try {
						isHuman = (Boolean) Await.result(future, timeout.duration());
					} catch (Exception e) {
						e.printStackTrace();
					}
					if(isHuman){
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

			}

		}  catch (Exception e) {
			e.printStackTrace();
		}
	}

}
