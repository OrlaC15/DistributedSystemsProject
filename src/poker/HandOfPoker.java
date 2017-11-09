package poker;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


public class HandOfPoker {

	final private static int OPENING_HAND = HandOfCards.ONE_PAIR_DEFAULT;
	public int highBet = 0;

	private ArrayList<PokerPlayer> players;
	int ante;
	//public static ThreadLocal<Integer> pot = new ThreadLocal<Integer>();
	public int pot;



	OutputTerminal UI;
	DeckOfCards deck;
	HumanPokerPlayer human;

	PrintWriter writer;
	final static boolean PRINT_TEST_FILE = false;
	final static boolean PRINT_BANKS_TO_UI_MORE = false;
	HashMap<PokerPlayer, Integer> betRecordz = new HashMap<PokerPlayer, Integer>();

	
	public HandOfPoker(ArrayList<PokerPlayer> players, int ante, DeckOfCards deck, OutputTerminal UI) throws IOException{
		this.players = new ArrayList<PokerPlayer>();
		this.players.addAll(players);
		for (int i=0; i< this.players.size(); i++){
			this.players.get(i).passHandOfPokerRef(this);
		}
		this.ante = ante;
		this.deck = deck;
		this.UI = new OutputTerminal();
		this.human = (HumanPokerPlayer) players.get(0);
		//pot.set(0);

		try {
			gameLoop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	 
/*
	public HandOfPoker(ArrayList<PokerPlayer> players, int ante, DeckOfCards deck, TwitterInteraction t) throws TwitterException, IOException{
		this.players = new ArrayList<PokerPlayer>();
		this.players.addAll(players);
		for (int i=0; i< this.players.size(); i++){
			this.players.get(i).passHandOfPokerRef(this);
		}
		this.ante = ante;
		this.twitter = t;
		this.deck = deck;
		this.UI = new OutputTerminal();
		this.human = (HumanPokerPlayer) players.get(0);
		//pot.set(0);

		try {
			gameLoop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		

	}
	*/

	/**
	 * Runs the events of the game from a very high level
	 * @return
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws TwitterException 
	 */
	void gameLoop() throws InterruptedException, IOException {

		setUpFilePrint();

		System.out.println("getting into gameLoop");
		dealHandsUntilOpen();
	//	twitter.postCompoundTweet();
		human.tweetInitialCards();
		//twitter.postCompoundTweet();
		pot += collectAntes();
		displayPot();
		//twitter.postCompoundTweet();
		revisedTakeBets();
		testPrint(players.toString());
		//twitter.postCompoundTweet();

		//twitter.postCompoundTweet();
		System.out.println("number of players "+players.size());
		players.clear();
		for (PokerPlayer name: betRecordz.keySet()){
			players.add(name);
		} 

		UI.printout("players.size() is "+players.size());
		UI.printout("betRecordz.size() is "+betRecordz.size());


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
			for (PokerPlayer name: betRecordz.keySet()){

				String key =name.toString();
				String value = betRecordz.get(name).toString();  
				UI.printout("KV" +key + " " + value);  

			} 
		}

		//human.currentbet = 0;
		human.replyForNextRound();


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
			if (players.get(i).playerPot < lowestPot){
				lowestPot = players.get(i).playerPot;
				lowestPotName = players.get(i).playerName;
			}
		}

		for (int i=0; i<players.size(); i++){
			players.get(i).lowestPotBetLimit = lowestPot;
			players.get(i).lowestPotPlayerName = lowestPotName;
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
			deck.shuffle();
			System.out.println("deck was shuffled");
			deck.reset();
			System.out.println("deck was reset");
			//twitter.appendToCompoundTweet("Dealing hands...");
			UI.printout("Dealing hands... to "+players.size());
			UI.printout("Dealing"+players.size());

			for (int i=0; i<players.size(); i++){
				UI.printout("Dealing");
				players.get(i).dealNewHand();
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
			if (players.get(i).hand.getGameValue() >= OPENING_HAND){
				openingHand = true;
				//twitter.appendToCompoundTweet("Player "+ players.get(i).playerName + " says I can open!\n");
				UI.printout("Player "+ players.get(i).playerName + " says I can open!\n");
				break;
			}
		}
		if (!openingHand){
			//twitter.appendToCompoundTweet("Nobody can open this round.");
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
			players.get(i).subtractChips(ante);
			//twitter.appendToCompoundTweet(players.get(i).playerName + " paid " + ante + " chips for deal.");
			UI.printout(players.get(i).playerName + " paid " + ante + " chips for deal.");
		}
		return antesTotal;
	}

	/**
	 * Takes bets from players. Players can fold their hands here.
	 * @return Number of chips to be added to the pot
	 * TODO: Should be a nested loop for going around the table until all bets are seen or folded
	 * @throws TwitterException 
	 */


	private void revisedTakeBets() throws  InterruptedException {

		//twitter.appendToCompoundTweet("## Place your bets!\n");
		UI.printout("## Place your bets!\n");
		showBanks();
		highBet = 0;
		HashMap<PokerPlayer, Boolean> foldStatus = new HashMap<PokerPlayer, Boolean>();
		for(int i=0;i<players.size();i++){
			foldStatus.put(players.get(i), false);
		}

		int firstRaiserIndex = -1;
		PokerPlayer lastRaiser = null;

		//ArrayList<Integer> betRecord = new ArrayList<Integer>(); // list for keeping track of bets,bet record[i] will represent player[i]'s bet

		ArrayList<PokerPlayer> playersNotFolded = new ArrayList<PokerPlayer>();



		//testPrint(players, playersNotFolded, betRecord, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\nBefore first betting loop.");

		// 1
		// First bet loop goes until a player raises
		setLowestPotBounds();
		testPrint("First Betting Loop: until someone raises");
		for (int i=0; i< players.size() && firstRaiserIndex == -1; i++){

			testPrint("player "  + i);
			int bet =0;

			// Take opening bets 
			if (players.get(i).equals(human)){
				bet = human.openingBet();
			}
			else {
				bet = players.get(i).getBet();
			}

			testPrint("bet =" + bet);
			//players.get(i).subtractChips(bet);

			if (bet > 0){
				testPrint("bet > 0. break 1st loop");
				highBet = bet;
				firstRaiserIndex = i;
				lastRaiser = players.get(i);
				testPrint("firstRaiserIndex = " + firstRaiserIndex);
				//twitter.appendToCompoundTweet(players.get(i).playerName + " makes the first bet of " + bet + " chips.");
				//twitter.postCompoundTweet();
				UI.printout(players.get(i).playerName + " makes the first bet of " + bet + " chips.");
			}
			else {
				testPrint("bet !> 0. continue 1st loop");
				//twitter.appendToCompoundTweet(players.get(i).playerName + " checks.");
				//twitter.postCompoundTweet();
				UI.printout(players.get(i).playerName + " checks.");
			}
			playersNotFolded.add(players.get(i));
			foldStatus.put(players.get(i), false);
			//betRecord.add(bet);
			betRecordz.put(players.get(i), bet);
			//twitter.postCompoundTweet();
			//testPrint(players, playersNotFolded, betRecord, "player " + i +  " finishes first loop");
			//testPrint(players, playersNotFolded, betRecord, "player " + i +  " finishes first loop");


		}


		pot += highBet;
		//twitter.appendToCompoundTweet("Second Loop");

		testShowBanks();

		//testPrint(players, playersNotFolded, betRecord, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\nBefore second betting loop.");


		// 2
		// Go back around again and see who raises and give the first raiser a second chance to raise
		for (int i =(firstRaiserIndex + 1)%players.size(); i != firstRaiserIndex; i = (i+1)%players.size()) {

			testPrint("player " + i);

			int bet = players.get(i).getBet();
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
					//twitter.appendToCompoundTweet(players.get(i).playerName + " sees the bet of " + highBet + " chips.");
					//twitter.postCompoundTweet();
					if (raiseMessage) {
						UI.printout(players.get(i).playerName + " raises the bet to " + highBet + " chips.");
					}
					else {
						UI.printout(players.get(i).playerName + " sees the bet of " + highBet + " chips.");
					}
				}
				else {
					testPrint("i <= firstRaiser index");
					//pot += bet - betRecord.get(i);
					pot+= bet - betRecordz.get(players.get(i));
					//players.get(i).subtractChips(bet - betRecord.get(i));
					//twitter.appendToCompoundTweet(players.get(i).playerName + " sees the bet of " + highBet 
					//		+ " and throws in the additional " + (bet - betRecord.get(i)) + " chips.\n");
					UI.printout(players.get(i).playerName + " sees the bet of " + highBet 
							+ " and throws in the additional " + (bet - betRecordz.get(players.get(i))/*betRecord.get(i)*/) + " chips.\n");
					//betRecord.set(i, bet);
					betRecordz.replace(players.get(i), bet);
					//twitter.postCompoundTweet();
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
				//twitter.appendToCompoundTweet(players.get(i).playerName + " folds.");
				//twitter.postCompoundTweet();
				UI.printout(players.get(i).playerName + " folds.");
				playersNotFolded.remove(players.get(i));
				betRecordz.remove(players.get(i));
				foldStatus.put(players.get(i), true);
			}


			//	testPrint(players, playersNotFolded, betRecord, "player " + i +  " finishes second loop");
		}
		//twitter.postCompoundTweet();


		testShowBanks();

		//testPrint(players, playersNotFolded, betRecord, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\nBefore third loop swap.");


		//twitter.appendToCompoundTweet("Third Loop");

		// Call the bets back around to the last raiser if there are still enough players not folded
		//players.clear();
		//players.addAll(playersNotFolded);
		ArrayList<PokerPlayer> proceed = new ArrayList<PokerPlayer>();
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
				if (!players.get(i).hasMatchedHighBet()){
					int bet = players.get(i).getCall();
					//UI.printout(players.get(i).playerName + " bets " + bet + " & high bet = " + this.highBet);

					testPrint("bet = " + bet);
					if (bet != 0){
						testPrint("bet != 0");
						UI.printout("Adding "+players.get(i).playerName + " to the playersnotfolded");
						playersNotFolded.add(players.get(i));
						foldStatus.put(players.get(i), false);
						//pot += highBet - betRecord.get(i);
						pot += highBet - betRecordz.get(players.get(i));
						//players.get(i).subtractChips(highBet - betRecord.get(i));
						//twitter.appendToCompoundTweet(players.get(i).playerName + " sees the bet of " + highBet 
						//		+ " and throws in the additional " + (bet - betRecord.get(i)) + " chips.\n");
						//twitter.postCompoundTweet();
						UI.printout(players.get(i).playerName + " sees the bet of " + highBet 
								+ " and throws in the additional " + (bet - betRecordz.get(players.get(i))/*betRecord.get(i)*/) + " chips.\n");
					}
					else {
						testPrint("bet = 0 so folds");						
						UI.printout(players.get(i).playerName + " folds.");
						//betRecord.remove(i);
						betRecordz.remove(players.get(i));
						//players.remove(i);
						if(foldStatus.containsKey(players.get(i))){
							foldStatus.remove(players.get(i));
						}
						//twitter.appendToCompoundTweet(players.get(i).playerName + " folds.");
						//twitter.postCompoundTweet();

					}
				}
				else {
					testPrint("player " + i + "already matched bet");
				}
				for (PokerPlayer name: betRecordz.keySet()){

					String key =name.toString();
					String value = betRecordz.get(name).toString();  
					//UI.printout("KV in loop: " +key + " " + value);  

				} 

				//testPrint(players, playersNotFolded, betRecord, "player " + i +  " finishes third loop");
			}
		}
		else {
			//twitter.appendToCompoundTweet("Everyone has folded but " + players.get(0).playerName + "!");
			//twitter.postCompoundTweet();
			UI.printout("Everyone has folded but " + players.get(0).playerName + "!");
		}

		//players.clear();
		//players.addAll(playersNotFolded);

		ArrayList<PokerPlayer> tempo = new ArrayList<PokerPlayer>();
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
		human.currentBet =0;
		for (int i=0; i<players.size(); i++){
			players.get(i).roundOverallBet = 0;
		}
	}

	private void showBanks() {
		for (int i=0; i< players.size(); i++){
			//twitter.appendToCompoundTweet(players.get(i).playerName + " has " + players.get(i).playerPot + " chips");
			UI.printout(players.get(i).playerName + " has " + players.get(i).playerPot + " chips");
		}
		//twitter.postCompoundTweet();
	}

	/**
	 * All players discard up to three cards from their hand and re-deal themselves 
	 * and are re-dealt three from the deck
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws TwitterException 
	 */
	private void discardCards() throws InterruptedException, IOException {
		//human.discard();
		//players.set(0, human);
		for (int i=0; i<players.size(); i++){
			players.get(i).hand.passPlayerType(players.get(i));
			int discardedCount = players.get(i).discard();
			//twitter.appendToCompoundTweet(players.get(i).playerName + " discards " + discardedCount + "cards");
			UI.printout(players.get(i).playerName + " discards " + discardedCount + "cards");
		}
		//twitter.appendToCompoundTweet("\n\n## Players are redealt their cards.");
		UI.printout("\n\n## Players are redealt their cards.");
		//twitter.postCompoundTweet();
	}

	/**
	 * Shows all hands remaining in the game
	 */
	private void showCards() {
		fillPlayers();
		PokerPlayer handWinner = getHandWinner();

		for (int i=0; i<players.size(); i++){
			//twitter.appendToCompoundTweet(players.get(i).playerName + " says ");
			UI.printout(players.get(i).playerName + " says ");
			players.get(i).showCards(handWinner);
		}
	}

	/**
	 * Determines the winner of this hand of poker.
	 * @return
	 */
	private PokerPlayer getHandWinner(){
		fillPlayers();
		PokerPlayer winningPlayer = players.get(0);

		for(int i=1; i<players.size(); i++){
			if(players.get(i).hand.getGameValue()>winningPlayer.hand.getGameValue()){
				winningPlayer = players.get(i);
			}
		}
		return winningPlayer;
	}

	/**
	 * Calculates who has the highest scoring hand in the group
	 * @return an arrayList containing the winner or tied winners in a very rare case
	 */
	private ArrayList<PokerPlayer> calculateWinners() {
		ArrayList<PokerPlayer> winnersCircle = new ArrayList<PokerPlayer>();


		HashMap.Entry<PokerPlayer,Integer> entry=betRecordz.entrySet().iterator().next();
		PokerPlayer key= entry.getKey();
		Integer value=entry.getValue();
		PokerPlayer winner = key;

		// Look for highest scoring hand
		/*for (int i=1; i<players.size(); i++){
			if (players.get(i).hand.getGameValue() > winner.hand.getGameValue()){
				winner = players.get(i);
			}
		}*/

		for (PokerPlayer name: betRecordz.keySet()){

			if(name.hand.getGameValue()>winner.hand.getGameValue()){
				winner = name;
			}

		} 

		// Store winner
		winnersCircle.add(winner);
		UI.printout("winner is "+winner.playerName);

		players.remove(winner);

		// Check for very rare occurrence of a draw for a split pot
		for (int i=0; i<players.size(); i++){
			if (players.get(i).hand.getGameValue() == winner.hand.getGameValue()){
				winnersCircle.add(players.get(i));
			}
		}

		return winnersCircle;
	}

	/**
	 * Awards winner the pot and declares the amount.
	 * TODO Implement when split pot betting occurs
	 * @param winners
	 * @throws TwitterException 
	 */
	private void awardWinner(ArrayList<PokerPlayer> winners)  { 

		if (winners.size() == 1){
			//twitter.postCompoundTweet(); //Make sure compound tweet is clear
			//twitter.appendToCompoundTweet("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			//twitter.appendToCompoundTweet(winners.get(0).playerName + " wins with a " + winners.get(0).getHandType());
			//twitter.appendToCompoundTweet("## " + winners.get(0).playerName + " gets " + pot/winners.size() + " chips. ##\n");
			//twitter.appendToCompoundTweet("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			//twitter.postCompoundTweet();

			UI.printout("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			UI.printout(winners.get(0).playerName + " wins with a " + winners.get(0).getHandType());
			UI.printout("## " + winners.get(0).playerName + " gets " + pot/winners.size() + " chips. ##\n");
			UI.printout("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");


			winners.get(0).awardChips(pot);
			pot = 0;
		}

		else {

			for (int i=0; i<winners.size(); i++){
				//twitter.appendToCompoundTweet(winners.get(0).playerName + " ties with a " + winners.get(0).getHandType());
				UI.printout(winners.get(0).playerName + " ties with a " + winners.get(0).getHandType());
			}

			for (int i=0; i< winners.size(); i++){
				winners.get(i).awardChips(pot/winners.size());
				//twitter.appendToCompoundTweet("## " + winners.get(0).playerName + " gets " + pot/winners.size() + " chips. ##\n");
				UI.printout("## " + winners.get(0).playerName + " gets " + pot/winners.size() + " chips. ##\n");

			}
		}
	}


	/**
	 * Shows the pot to the interface
	 */
	private void displayPot(){
		//twitter.appendToCompoundTweet("\nThe pot has " + pot + " chips to play for.\n");
		UI.printout("\nThe pot has " + pot + " chips to play for.\n");
	}

	/*
	 * Initialises and plays two separate instances of a hand of poker 
	 */
	public static void main(String[] args) throws InterruptedException {
		DeckOfCards deck = new DeckOfCards();
		OutputTerminal console = new OutputTerminal();
		int ante = 1;
		//TwitterFactory twitterO = new TwitterFactory();
		//TwitterStreamer twitterS = new TwitterStreamer();
		//TwitterInteraction testTwitteri = new TwitterInteraction(TwitterStreamer.twitter);

		ArrayList<PokerPlayer> players = new ArrayList<PokerPlayer>(5);

		for(int i=0;i<5;i++){
			AutomatedPokerPlayer computerPlayer = new AutomatedPokerPlayer(deck, console);
			players.add(computerPlayer);

			System.out.println(computerPlayer.getHandType());
		}


		// First hand of poker
		/*
		new HandOfPoker(players, ante, deck, console);
		new HandOfPoker(players, ante, deck, console);
		new HandOfPoker(players, ante, deck, console);
		new HandOfPoker(players, ante, deck, console);
		 */
		/*console.printout("\n\n\n" +
				"~~~~~~~~~~~~~~~~~~~~~~~~~~~-----------~~~~~~~~~~~~~~~~~~~~~~~~~~~" +
				"\n\n\n"
				);

		// Second hand
		console.printout("\n~~~~~~~~~~~~~~~~~~~~~~~~~~~-----------~~~~~~~~~~~~~~~~~~~~~~~~~~~");*/
	}

	public int getPot(){
		return pot;
	}
	public void setPot(int pots){
		pot = pots;
	}

	public void fillPlayers(){
		ArrayList<PokerPlayer> temp = new ArrayList<PokerPlayer>();
		for (PokerPlayer name: betRecordz.keySet()){
			temp.add(name);
		} 
		players.clear();
		players.addAll(temp);

	}
}
