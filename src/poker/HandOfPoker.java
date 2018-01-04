package poker;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
//import com.sun.org.apache.bcel.internal.generic.ACONST_NULL;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


public class HandOfPoker {

	final private static int OPENING_HAND = HandOfCards.ONE_PAIR_DEFAULT;
	public int highBet = 0;
	//public static final int TIMEOUT = Integer.MAX_VALUE;
	public static final int TIMEOUT = 1000000;

	private ArrayList<ActorRef> players;
	int ante;
	//public static ThreadLocal<Integer> pot = new ThreadLocal<Integer>();
	public int pot;



	OutputTerminal UI;
	ActorRef deck;
	ActorRef human;

	PrintWriter writer;
	final static boolean PRINT_TEST_FILE = false;
	final static boolean PRINT_BANKS_TO_UI_MORE = false;
	HashMap<ActorRef, Integer> betRecordz = new HashMap<ActorRef, Integer>();

	ActorRef dealer;

	
	public HandOfPoker(ArrayList<ActorRef> players, int ante, ActorRef deck, OutputTerminal UI, ActorRef player, ActorRef dealer) throws IOException{
		this.players = new ArrayList<ActorRef>();
		this.dealer = dealer;
		this.players.addAll(players);
		for (int i=0; i< this.players.size(); i++){
			//this.players.get(i).passHandOfPokerRef(this);
			this.players.get(i).tell(this,dealer);

		}
		this.ante = ante;
		this.deck = deck;
		this.UI = new OutputTerminal(dealer,player);
		this.human = (ActorRef) players.get(0);
		//pot.set(0);

		try {
			gameLoop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Runs the events of the game from a very high level
	 * @return
	 * @throws InterruptedException 
	 * @throws IOException
	 */
	void gameLoop() throws InterruptedException, IOException {

		setUpFilePrint();

		System.out.println("getting into gameLoop");
		dealHandsUntilOpen();

		human.tell("tweet initial cards", dealer);



		pot += collectAntes();
		displayPot();
		revisedTakeBets();
		testPrint(players.toString());

		System.out.println("number of players "+players.size());
		players.clear();
		for (ActorRef name: betRecordz.keySet()){
			players.add(name);
		} 
/*

		UI.printout("players.size() is "+players.size());
		UI.printout("betRecordz.size() is "+betRecordz.size());
*/


		if (betRecordz.size() > 1){
			discardCards();
			highBet = 0;
			displayPot();
			revisedTakeBets();
			displayPot();
			fillPlayers();
			if (betRecordz.size() >1){
				showCards();
			}
		}
		System.out.println("game loop 4");


		if(betRecordz.size() > 0){
			awardWinner(calculateWinners());
		}
		else{
			UI.printout("Everybody Folded!");
			for (ActorRef name: betRecordz.keySet()){

				String key =name.toString();
				String value = betRecordz.get(name).toString();  
				UI.printout("KV" +key + " " + value);  

			} 
		}


		human.tell("reply for next round", dealer);


		if (PRINT_TEST_FILE){
			writer.close();
		}
	}

	/**
	 * For truncating the bets
	 * @return the current lowest player pot in the round
	 */
	public void setLowestPotBounds() {
		//Get pot and name of lowest player
		int lowestPot = Integer.MAX_VALUE;
		String lowestPotName = null;
		for (int i=0; i<players.size(); i++){
			System.out.println(i);
			Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			Future<Object> future = Patterns.ask(players.get(i), "get pot", timeout);
			int pot = 0;
			try {
				pot = (Integer) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (pot < lowestPot){
				//lowestPot = players.get(i).playerPot;
				//lowestPotName = players.get(i).playerName;
				lowestPot = pot;
				lowestPotName = players.get(i).path().name();
			}
		}

		for (int i=0; i<players.size(); i++){
			//players.get(i).lowestPotBetLimit = lowestPot;
			//players.get(i).lowestPotPlayerName = lowestPotName;
			players.get(i).tell("set lowest pot:"+lowestPot, dealer);
			players.get(i).tell("lowest pot player name:"+lowestPotName,dealer);
		}
	}

	public void printTruncatedBetPrompt(String name, int bet){
		UI.printout("Bet truncated to " + bet + " chips, " + name + " only has " +bet + "chips.");
	}

	private void setUpFilePrint() {
		if (PRINT_TEST_FILE){
			String timeStamp = new SimpleDateFormat("HHmmss").format(Calendar.getInstance().getTime());
			try{
				writer = new PrintWriter( timeStamp + ".txt", "UTF-8");
				writer.println("The first line");
				writer.println("The second line");
			} catch (IOException e) {
				// do something
			}
		}
	}

	private void testPrint(String output) {
		if (PRINT_TEST_FILE){
			writer.println(output);
			writer.flush();
		}
	}

	private void testPrint(ArrayList<PokerPlayer> players, ArrayList<PokerPlayer> playersNotFolded, ArrayList<Integer> bettingRecord, String positionInfo) {
		testPrint("\n\n>>>>>>>>>>>>>>>");
		testPrint(positionInfo);
		testPrint("Players array:\n" + players);
		testPrint("Players not Folded: " + playersNotFolded);
		testPrint("Betting record: " + bettingRecord);
		testPrint("\n\n");
	}

	private void testShowBanks(){
		if (PRINT_BANKS_TO_UI_MORE){
			UI.printout("\n\nzzzzzzz");
			showBanks();
			UI.printout("\n");
		}
	}


	/**
	 * Deals hands until an opening hand is achieved from at least one player,
	 * shuffles and resets deck each time
	 * @throws InterruptedException 
	 */
	private void dealHandsUntilOpen() throws InterruptedException {
		do {
			System.out.println("got into dealhandsuntilopen");
			System.out.println("checking if null");
			System.out.println(deck==null);
			deck.tell("Shuffle", dealer);
			System.out.println("deck was shuffled");
			deck.tell("Reset", dealer);
			System.out.println("deck was reset");
            UI.printout("Dealing cards.....");
			for (int i=0; i<players.size(); i++){

				ActorRef currentRef = players.get(i);
				System.out.println("current ref "+currentRef);
				currentRef.tell("deal new hand", dealer);
				//players.get(i).dealNewHand();
			}
		} while (checkOpen() == false);
	}
	/**
	 * Returns true if any of the players has an opening hand.
	 * Prints prompts when a player opens and when no player can open.
	 */
	private boolean checkOpen() {
		boolean openingHand = false;
		for (int i=0; i<players.size(); i++){
			System.out.println(i+" out of "+players.size());
			ActorRef currentRef = players.get(i);
			System.out.println("current ref "+currentRef);


			Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			Future<Object> future = Patterns.ask(currentRef, "get hand", timeout);
			HandOfCards currentHand = null;
			try {
				currentHand = (HandOfCards) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (currentHand.getGameValue() >= OPENING_HAND  ){
				openingHand = true;
				UI.printout("Player "+ players.get(i).path().name() + " says I can open!\n");
				break;
			}
		}
		if (!openingHand){
			UI.printout("Nobody can open this round.");
		}
		return openingHand;
	}

	/**
	 * TODO: Implement antes properly when betting is implemented
	 * @return number of chips to be added to the pot
	 */
	public int collectAntes() {
		int antesTotal =0;
		for (int i=0; i<players.size(); i++){
			antesTotal += ante; 

			players.get(i).tell("subtract chips:"+ante, dealer);





		}
		UI.printout("All players have paid the ante of  " + ante + " chips for deal.");

		return antesTotal;
	}

	/**
	 * Takes bets from players. Players can fold their hands here.
	 * @return Number of chips to be added to the pot
	 * TODO: Should be a nested loop for going around the table until all bets are seen or folded
	 */


	private void revisedTakeBets() throws  InterruptedException {

		UI.printout("## Place your bets!\n");
		showBanks();
		highBet = 0;
		HashMap<ActorRef, Boolean> foldStatus = new HashMap<>();
		for(int i=0;i<players.size();i++){
			foldStatus.put(players.get(i), false);
		}

		int firstRaiserIndex = -1;
		ActorRef lastRaiser = null;


		ArrayList<ActorRef> playersNotFolded = new ArrayList<>();



		//testPrint(players, playersNotFolded, betRecord, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\nBefore first betting loop.");

		// 1
		// First bet loop goes until a player raises
		setLowestPotBounds();
		testPrint("First Betting Loop: until someone raises");
		for (int i=0; i< players.size() && firstRaiserIndex == -1; i++){

			testPrint("player "  + i);
			int bet =0;

			Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			Future<Object> future = Patterns.ask(players.get(i), "are you human?", timeout);
			boolean isHuman = false;
			try {
				isHuman = (Boolean) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}

			System.out.println(players.get(i).path().name() +" human? => "+isHuman);

			// Take opening bets 
			if (isHuman){
				timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
				future = Patterns.ask(human, "get opening bet", timeout);

				try {
					bet = (Integer) Await.result(future, timeout.duration());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			else {
				 timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
				 future = Patterns.ask(players.get(i), "get bet", timeout);

				try {
					bet = (Integer) Await.result(future, timeout.duration());
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			testPrint("bet =" + bet);

			if (bet > 0){
				testPrint("bet > 0. break 1st loop");
				highBet = bet;
				firstRaiserIndex = i;
				lastRaiser = players.get(i);
				testPrint("firstRaiserIndex = " + firstRaiserIndex);

				UI.printout(players.get(i).path().name() + " makes the first bet of " + bet + " chips.");
			}
			else {
				testPrint("bet !> 0. continue 1st loop");

				UI.printout(players.get(i).path().name() + " checks.");
			}
			playersNotFolded.add(players.get(i));
			foldStatus.put(players.get(i), false);
			betRecordz.put(players.get(i), bet);
			//testPrint(players, playersNotFolded, betRecord, "player " + i +  " finishes first loop");
			//testPrint(players, playersNotFolded, betRecord, "player " + i +  " finishes first loop");


		}


		pot += highBet;

		testShowBanks();

		//testPrint(players, playersNotFolded, betRecord, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\nBefore second betting loop.");


		// 2
		// Go back around again and see who raises and give the first raiser a second chance to raise
		for (int i =(firstRaiserIndex + 1)%players.size(); i != firstRaiserIndex; i = (i+1)%players.size()) {

			testPrint("player " + i);

			Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			Future<Object> future = Patterns.ask(players.get(i), "get bet", timeout);
			int bet =0;

			try {
				bet = (Integer) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}

			//int bet = players.get(i).getBet();

			testPrint("bet = " + bet);
			boolean raiseMessage = false;

			if (bet > highBet){
				testPrint ("bet > highbet: " + highBet);
				highBet = bet;
				lastRaiser = players.get(i);
				raiseMessage = true;

			}
			else if (bet == highBet){
				testPrint ("bet == highbet: " + highBet);
			}

			if (bet != 0){
				testPrint("bet != 0, lastRaiser = " + lastRaiser);
				if ( i > firstRaiserIndex){
					testPrint("i > firstRaiser index");
					playersNotFolded.add(players.get(i));
					foldStatus.put(players.get(i), false);
					//betRecord.add(bet);
					betRecordz.put(players.get(i), bet);
					//players.get(i).subtractChips(bet);
					pot += bet;
					if (raiseMessage) {
						//UI.printout(players.get(i).playerName + " raises the bet to " + highBet + " chips.");
						UI.printout(players.get(i).path().name() + " raises the bet to " + highBet + " chips.");
					}
					else {
						//UI.printout(players.get(i).playerName + " sees the bet of " + highBet + " chips.");
						UI.printout(players.get(i).path().name() + " sees the bet of " + highBet + " chips.");
					}
				}
				else {
					testPrint("i <= firstRaiser index");
					//pot += bet - betRecord.get(i);
					pot+= bet - betRecordz.get(players.get(i));
					//players.get(i).subtractChips(bet - betRecord.get(i));


				//	UI.printout(players.get(i).playerName + " sees the bet of " + highBet
				//			+ " and throws in the additional " + (bet - betRecordz.get(players.get(i))/*betRecord.get(i)*/) + " chips.\n");
					UI.printout(players.get(i).path().name() + " sees the bet of " + highBet
							+ " and throws in the additional " + (bet - betRecordz.get(players.get(i))/*betRecord.get(i)*/) + " chips.\n");



					//betRecord.set(i, bet);
					betRecordz.replace(players.get(i), bet);

				}
			}
			else {
				testPrint("bet == 0 ie folds");
				UI.printout("fold");
				if (i <= firstRaiserIndex){
					UI.printout("\n\n\n\nFOLDED\n\n\n\n");
					testPrint("i <= firstRaiser index " + firstRaiserIndex + " removing them.");
					//betRecord.remove(i);
					betRecordz.remove(players.get(i));
					foldStatus.put(players.get(i), true);
					playersNotFolded.remove(i);
				}

				//UI.printout(players.get(i).playerName + " folds.");
				UI.printout(players.get(i).path().name() + " folds.");



				playersNotFolded.remove(players.get(i));
				betRecordz.remove(players.get(i));
				foldStatus.put(players.get(i), true);
			}


			//	testPrint(players, playersNotFolded, betRecord, "player " + i +  " finishes second loop");
		}



		testShowBanks();

		//testPrint(players, playersNotFolded, betRecord, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\nBefore third loop swap.");


		// Call the bets back around to the last raiser if there are still enough players not folded
		//players.clear();
		//players.addAll(playersNotFolded);
		ArrayList<ActorRef> proceed = new ArrayList<>();
		for(int i =0;i<players.size();i++){
			if(foldStatus.get(players.get(i))==false){
				proceed.add((players.get(i)));
			}
		}
		players.clear();
		players.addAll(proceed);

		//testPrint(players, playersNotFolded, betRecord, "After third loop swap.");
		setLowestPotBounds();
		int lastRaiserIndex = -1;
		for (int i =0; i< players.size(); i++){
			if (players.get(i) == lastRaiser){
				lastRaiserIndex = i;
			}
		}
		testPrint("lastRaiserIndex = " + lastRaiserIndex);
		ArrayList<PokerPlayer> temp = new ArrayList<PokerPlayer>();
		testPrint("palyers size = " + players.size());
		if (players.size() > 1){
			testPrint("size > 1");
			testPrint("lastRaiserIndex  = " + lastRaiserIndex);
			foldStatus.clear();
			for (int i = (lastRaiserIndex+1)%players.size(); i != lastRaiserIndex; i = (i+1)%players.size()){
				//UI.printout("Player " + i);
				Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
				Future<Object> future = Patterns.ask(players.get(i), "has matched high bet", timeout);
				boolean hasMatchedHighBet = false;
				try {
					hasMatchedHighBet = (Boolean) Await.result(future, timeout.duration());
				} catch (Exception e) {
					e.printStackTrace();
				}


				//if (!players.get(i).hasMatchedHighBet()){
				if (!hasMatchedHighBet){
					timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
					System.out.println("getting call from "+players.get(i).path().name());
					future = Patterns.ask(players.get(i), "get call", timeout);
					int bet = 0;
					try {
						bet = (Integer) Await.result(future, timeout.duration());
					} catch (Exception e) {
						e.printStackTrace();
					}


				//	int bet = players.get(i).getCall();

					//UI.printout(players.get(i).playerName + " bets " + bet + " & high bet = " + this.highBet);

					testPrint("bet = " + bet);
					if (bet != 0){
						testPrint("bet != 0");



						playersNotFolded.add(players.get(i));
						foldStatus.put(players.get(i), false);
						pot += highBet - betRecordz.get(players.get(i));


						UI.printout(players.get(i).path().name() + " sees the bet of " + highBet
								+ " and throws in the additional " + (bet - betRecordz.get(players.get(i))/*betRecord.get(i)*/) + " chips.\n");
					}
					else {
						testPrint("bet = 0 so folds");

						UI.printout(players.get(i).path().name() + " folds.");


						betRecordz.remove(players.get(i));
						if(foldStatus.containsKey(players.get(i))){
							foldStatus.remove(players.get(i));
						}

					}
				}
				else {
					testPrint("player " + i + "already matched bet");
				}
				for (ActorRef name: betRecordz.keySet()){

					String key =name.toString();
					String value = betRecordz.get(name).toString();  

				} 

			}
		}
		else {

			UI.printout("Everyone has folded but " + players.get(0).path().name() + "!");
		}


		ArrayList<ActorRef> tempo = new ArrayList<>();
		for(int i = 0;i<players.size();i++){
			if(foldStatus.containsKey(players.get(i))){
				if(foldStatus.get(players.get(i))==false){
					tempo.add(players.get(i));
				}
			}
		}

		players.clear();
		players.addAll(tempo);


		testShowBanks();

		human.tell("reset current bet", dealer);


		for (int i=0; i<players.size(); i++){
			players.get(i).tell("round overall bet",dealer);
		}
	}

	private void showBanks() {
		for (int i=0; i< players.size(); i++){

			Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			Future<Object> future = Patterns.ask(players.get(i), "get pot", timeout);
			int pot = 0;
			try {
				pot = (int) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}

			UI.printout(players.get(i).path().name() + " has " + pot + " chips");



		}
	}

	/**
	 * All players discard up to three cards from their hand and re-deal themselves 
	 * and are re-dealt three from the deck
	 * @throws InterruptedException 
	 * @throws IOException
	 */
	private void discardCards() throws InterruptedException, IOException {
		//human.discard();
		//players.set(0, human);
		for (int i=0; i<players.size(); i++){
			System.out.println(i);

			Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			Future<Object> future = Patterns.ask(players.get(i), "get hand", timeout);
			HandOfCards hand = null;
			try {
				hand = (HandOfCards) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}

			hand.passPlayerType(players.get(i));

			timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			future = Patterns.ask(players.get(i), "discard", timeout);
			int discardedCount = 0;
			try {
				discardedCount = (Integer) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}

			UI.printout(players.get(i).path().name() + " discards " + discardedCount + "cards");
		}
		UI.printout("\n\n## Players are redealt their cards.");
	}

	/**
	 * Shows all hands remaining in the game
	 */
	private void showCards() {
		fillPlayers();
		ActorRef handWinner = getHandWinner();

		for (int i=0; i<players.size(); i++){
			//UI.printout(players.get(i).path().name() + " says ");



			players.get(i).tell("show cards:"+handWinner.path().toString(), dealer);
		}
	}

	/**
	 * Determines the winner of this hand of poker.
	 * @return
	 */
	private ActorRef getHandWinner(){
		fillPlayers();
		ActorRef winningPlayer = players.get(0);
		Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
		Future<Object> future = Patterns.ask(winningPlayer, "get hand", timeout);
		HandOfCards winnersHand = null;
		try {
			winnersHand = (HandOfCards) Await.result(future, timeout.duration());
		} catch (Exception e) {
			e.printStackTrace();
		}

		for(int i=1; i<players.size(); i++){
			System.out.println(i);

			future = Patterns.ask(players.get(i), "get hand", timeout);
			HandOfCards currentHand = null;
			try {
				currentHand = (HandOfCards) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}

			if(currentHand.getGameValue()>winnersHand.getGameValue()){
				winningPlayer = players.get(i);
			}
		}
		return winningPlayer;
	}

	/**
	 * Calculates who has the highest scoring hand in the group
	 * @return an arrayList containing the winner or tied winners in a very rare case
	 */
	private ArrayList<ActorRef> calculateWinners() {
		ArrayList<ActorRef> winnersCircle = new ArrayList<>();


		HashMap.Entry<ActorRef,Integer> entry=betRecordz.entrySet().iterator().next();
		ActorRef key= entry.getKey();
		Integer value=entry.getValue();
		ActorRef winner = key;


		Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));


		Future<Object> future = Patterns.ask(winner, "get hand", timeout);
		HandOfCards winnersHand = null;
		try {
			winnersHand = (HandOfCards) Await.result(future, timeout.duration());
		} catch (Exception e) {
			e.printStackTrace();
		}

		for (ActorRef name: betRecordz.keySet()){
			timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			System.out.println(name);

			future = Patterns.ask(name, "get hand", timeout);
			HandOfCards currentHand = null;
			try {
				currentHand = (HandOfCards) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(currentHand.getGameValue()>winnersHand.getGameValue()){

				winner = name;
			}

		} 

		// Store winner
		winnersCircle.add(winner);


		UI.printout("winner is "+winner.path().name());


		players.remove(winner);

		// Check for very rare occurrence of a draw for a split pot
		for (int i=0; i<players.size(); i++){
			System.out.println();
			timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			future = Patterns.ask(players.get(i), "get hand", timeout);
			HandOfCards currentHand = null;
			try {
				currentHand = (HandOfCards) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (currentHand.getGameValue() == winnersHand.getGameValue()){
				winnersCircle.add(players.get(i));
			}
		}

		return winnersCircle;
	}

	/**
	 * Awards winner the pot and declares the amount.
	 * TODO Implement when split pot betting occurs
	 * @param winners
	 */
	private void awardWinner(ArrayList<ActorRef> winners)  {

		if (winners.size() == 1){

			UI.printout("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			//UI.printout(winners.get(0).playerName + " wins with a " + winners.get(0).getHandType());
		//	UI.printout("## " + winners.get(0).playerName + " gets " + pot/winners.size() + " chips. ##\n");

			Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
			Future<Object> future = Patterns.ask(winners.get(0), "get hand type", timeout);
			String result = "null";
			try {
				result = (String) Await.result(future, timeout.duration());
			} catch (Exception e) {
				e.printStackTrace();
			}
			UI.printout(winners.get(0).path().name() + " wins with a " + result);
			UI.printout("## " + winners.get(0).path().name() + " gets " + pot/winners.size() + " chips. ##\n");

			UI.printout("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");


			//winners.get(0).awardChips(pot);
			winners.get(0).tell("award chips:"+pot, dealer);


			pot = 0;
		}

		else {

			for (int i=0; i<winners.size(); i++){
				Timeout timeout = new Timeout(Duration.create(TIMEOUT, "seconds"));
				Future<Object> future = Patterns.ask(winners.get(0), "get hand type", timeout);
				String result = "null";
				try {
					result = (String) Await.result(future, timeout.duration());
				} catch (Exception e) {
					e.printStackTrace();
				}

				UI.printout(winners.get(0).path().name() + " ties with a " + result);

			}

			for (int i=0; i< winners.size(); i++){
				winners.get(0).tell("award chips:"+pot/winners.size(), dealer);

				UI.printout("## " + winners.get(0).path().name() + " gets " + pot/winners.size() + " chips. ##\n");


			}
		}
	}


	/**
	 * Shows the pot to the interface
	 */
	private void displayPot(){
		UI.printout("\nThe pot has " + pot + " chips to play for.\n");
	}


	public int getPot(){
		return pot;
	}
	public void setPot(int pots){
		pot = pots;
	}

	public void fillPlayers(){
		ArrayList<ActorRef> temp = new ArrayList<ActorRef>();
		for (ActorRef name: betRecordz.keySet()){
			temp.add(name);
		} 
		players.clear();
		players.addAll(temp);

	}
}
