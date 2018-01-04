package poker;

import akka.actor.ActorRef;

import java.io.*;
import java.util.Random;





public class AutomatedPokerPlayer extends PokerPlayer {
	private int playerType;
	private int playerBluffProbability;
	private static final String FILE_OF_NAMES = "src/PlayerNames/AutomatedPokerPlayerNames.txt";
	private static final int COCKY_RAISE = 0;
	private static final int COCKY_SEE = 1;
	private static final int COCKY_FOLD = 2;
	private static final int CONSERVATIVE_RAISE = 3;
	private static final int CONSERVATIVE_SEE = 4;
	private static final int CONSERVATIVE_FOLD = 5;

	//private static TwitterInteraction twitter;

	OutputTerminal output = new OutputTerminal(null,null);
	private int currentBet;





	
	public AutomatedPokerPlayer(ActorRef inputDeck, OutputTerminal UI) throws InterruptedException {
		super(inputDeck);
		playerName = getPlayerName(FILE_OF_NAMES);
		playerType = randomPokerPlayerType();
		playerBluffProbability = getBluffProbability();
		output = UI;
	}
	
	/**
	 * Counts the number of lines in a given file
	 * @return
	 * @throws IOException
	 */
	public static int countFileLines(String f) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(f));
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return (count == 0 && !empty) ? 1 : count;
		} finally {
			is.close();
		}
	}

	/**
	 * Returns the player type of the player in question
	 * @return
	 */
	public int getPlayerType() {
		System.out.println("PT =     " + this.playerType);
		return this.playerType;
	}

	/**
	 * Retrieves a quote for folding/raising/calling for each player type
	 * @param quoteNumber
	 * @return
	 */


	/**
	 * Retrieves a random line from a file.
	 * Primarily to be used in text files with player quotes and player names
	 * to take a random line from each file. 
	 * @param filename
	 * @return
	 */
	public String getRandomLineFromFile(String filename){
		String out = "";
		try{
			Random rand = new Random();
			int number_line = rand.nextInt(countFileLines(filename));

			BufferedReader read = new BufferedReader(new FileReader(filename));
			String line;
			for(int i=0; i<=number_line; i++){
				line = read.readLine();
				if(i==number_line){
					out = line;
				}
			}
			return out;
		}
		catch(Exception e){
			e.printStackTrace();
			return out;
		}
	}

	/**
	 * Retrieves a random name from the FILE_OF_NAMES for this 
	 * automated poker player.
	 * @return
	 */
	private String getPlayerName(String fileOfNames) {
		String playerName = null;
		try{
			playerName = getRandomLineFromFile(fileOfNames);
		}
		catch (Exception e){
			playerName = "Bot Player";
		}
		return playerName;
	}

	/**      
	 * Selects a random strategy for an automated poker player ranging from risky to conservative.
	 * This produces a random value between 1 & 5, where 1 is conservative and 5 is risky, 2 is 
	 * slightly conservative, 4 is slightly risky, and 3 is balanced. 
	 */
	private int randomPokerPlayerType(){		
		playerType = getRandomValue(5);
		return playerType;
	}

	/**
	 * Selects a bluff probability that affects whether a player will fold, raise, see etc.
	 */
	private int getBluffProbability(){
		playerBluffProbability = getRandomValue(100);
		return playerBluffProbability;
	}

	/**
	 * Produces a random integer value in the range passed in as a parameter,
	 * from one up to and including the range value.
	 */
	private int getRandomValue(int range){

		return new Random().nextInt(range) + 1;
	}

	/**
	 * Retrieves the call value for a player who can no longer raise the betting. 
	 * Otherwise the player folds.
	 */
	public int getCall(){
		int betValue = getBetValueCalculation();
		int callValue = getCallValueCalculation(betValue);
		int returnValue = 0;

		if(betValue >= currentRound.highBet && callValue > currentRound.highBet){
			returnValue = see(betValue);	
			this.subtractChips(returnValue - currentBet);
		}
		else{
			returnValue = 0;
		}

		return returnValue;
	}
	
	/**
	 * Retrieves the bet value the player wishes to bet.
	 */
	public int getBet(){

		int betValue = getBetValueCalculation();	//the value at which a player would bet up to 
		int callValue = getCallValueCalculation(betValue);   //the value at which a player would call up to, based on the bet value and player type
		boolean hasRaised = false;
		boolean bettingHasBeenRaised = false;

		int returnValue = 0;

		//if nobody has bet
		if(currentRound.highBet == 0){
			if(this.hand.getGameValue() < 100500000){
				output.printout("I check");
			}
			else{
				output.printout("I bet " + betValue + " to start.");
			}
			returnValue = betValue;
		}
		
		if(this.playerPot <= (GameOfPoker.PLAYER_POT_DEFAULT/5)){
			output.printout(this.playerName + " goes all in with " + playerPot + " chips!");
			returnValue = playerPot;
		}
		//if a players betValue/callValue are both less than the highbet then fold. 
		else if(betValue <= currentRound.highBet-GameOfPoker.PLAYER_POT_DEFAULT/10 && callValue < currentRound.highBet-GameOfPoker.PLAYER_POT_DEFAULT/10){
			returnValue = fold(betValue);
		}
		//if the betValue is higher than the high bet, and this player has not previously raised, then raise.
		else if(betValue > currentRound.highBet && hasRaised == false){
			hasRaised = true;
			
			if(bettingHasBeenRaised == true){
				returnValue = reRaise(betValue);
			}
			else{
				bettingHasBeenRaised = true;
				returnValue = raise(betValue);
			}
		}
		//this may occur if a player chooses to bluff
		else if(playerBluffProbability > 75 && hasRaised != true){
			hasRaised = true;
			if(bettingHasBeenRaised == true){
				returnValue = reRaise(currentRound.highBet+betValue);
			}
			else{
				bettingHasBeenRaised = true;
				returnValue = raise(currentRound.highBet+betValue);
			}
		}
		//if decides not to fold/raise/bluff then see(call) the highBet.
		else{
			returnValue = see(betValue);
		}
		
		currentBet = truncateBet(returnValue);
		this.roundOverallBet+=returnValue;
		
		
		this.subtractChips(returnValue);

		
		return returnValue;
	}

	/**
	 * Multiplies the betValue of a players and by a certain value based on their playerType to
	 * find a value that they would risk calling to. For example, if the highBet is at 60 and the
	 * betValue of the players hand is at 40, a player with type 5 (risky) would call up to a value
	 * of 40 * 1.55 = 62 however a less risky player with type 2 (slightly conservative would only
	 * call up to a value of 40 * 1.25 = 50.
	 */
	private int getCallValueCalculation(int betValue) {
		float playerTypeCalculation = (float) (0.75 + (1 - ((float)1/playerType)));
		int callValue = (int) (betValue*playerTypeCalculation);
		
		return callValue;
	}


	/**
	 * Uses the HandGameValue(HGV), the PlayerType(PT) & the Pot Size(PS) to calculate
	 * a betting value for the hand: (PS * HGV) / (15 - PT)
	 */
	private int getBetValueCalculation(){
		
		int betCalculationValue = 13;
		int handGameValue = this.hand.getGameValue()/100000000;	
		int betValue = (int) ((playerPot*handGameValue)/(betCalculationValue-playerType));		
		return betValue;
	}

	/**
	 * Calls the high bet if the betValue for the hand is within a small range of the high bet
	 */
	private int see(int betValue){
		betValue = currentRound.highBet;

		return betValue;
	}

	/**
	 * Raises the betting if the value of the hand is greater than the high bet by 2 or more.
	 */
	private int raise(int betValue){
		int raiseValue = betValue - currentRound.highBet;

		return raiseValue;
	}

	/**
	 * Raises the betting if the value of the hand is greater than the high bet by 2 or more.
	 */
	private int reRaise(int betValue){
		int raiseValue = betValue - currentRound.highBet;

		return raiseValue;
	}

	/**
	 * Folds if the value of the hand is significantly lower than the value of the high bet.
	 */
	private int fold(int betValue){
		betValue = 0;

		return betValue;
	}

	/**
	 * Determines if the poker player won this hand of poker
	 * @param handWinner
	 * @return
	 */
	private boolean didWinThisHand(String handWinner){
		if (getSelf().path().toString().compareTo(handWinner)==0){
			return true;
		}
		else{
			return false;
		}
	}

	/**
	 * Provides a player response for the player who wins the hand.
	 */
	public boolean showCards(String handWinner){
		boolean wonRound = didWinThisHand(handWinner);

		if(wonRound == true){
			output.printout("here is my winning hand: " + this.hand + "\n");
			return true;
		}
		else if(wonRound == false){
			output.printout("I did not win, I choose not to show my hand.\n");
			return false;
		}
		else{
			output.printout("here is my hand: \n" + this.hand + "\n");
			return true;
		}
	}

	@Override
	/**
	 * Discard method calls discard method in HandOfCards class. 
	 * Likelihood of discarding increases with the players risk aversion type.
	 */
	public int discard() throws InterruptedException,IOException {
		int cards = hand.discard();
		return cards;
	}
	
	public void testAppendString(){
		output.printout("This is coming from AutomatedPokerPlayer Class");
	}


	@Override
	public boolean isHuman() {
		
		return false;
	}

	@Override
	public void onReceive(Object o) throws Exception {

		if (o.toString().compareTo("deal new hand") == 0) {
			System.out.println("....dealing hand in actor "+getSelf());
			dealNewHand();
		}

		if (o.toString().compareTo("get hand") == 0) {
			System.out.println("....sending back hand["+hand+"] in actor "+getSelf());
			getSender().tell(hand, getSelf());
		}

		if(o.toString().contains("get call")){
			int call = getCall();
			getSender().tell(call, getSelf());
		}

		if(o.toString().compareTo("get pot")==0){
			System.out.println("....sending back pot ("+playerPot+") from ("+getSelf()+") to "+getSender());
			getSender().tell(playerPot, getSelf());
		}

		if(o.toString().contains("get bet")){
			int bet = getBet();
			System.out.println("sending back the bet of "+bet+" from "+getSelf());
			getSender().tell(bet, getSelf());
		}

		if(o.toString().contains("are you human?")){
			getSender().tell(false,getSelf());
		}

		if(o instanceof HandOfPoker){
			currentRound = (HandOfPoker) o;
		}

		if(o.toString().compareTo("has matched high bet")==0){
			getSender().tell(hasMatchedHighBet(), getSelf());
		}


		if(o.toString().compareTo("player pot")==0){
			getSender().tell(playerPot, getSelf());
		}

		if(o.toString().compareTo("get hand type")==0){
			getSender().tell(getHandType(), getSelf());
		}

		if(o.toString().compareTo("discard")==0){
			int discard = discard();
			getSender().tell(discard, getSelf());
		}

		if(o.toString().compareTo("round overall bet")==0){
			roundOverallBet =0;
		}

		if(o.toString().contains("show cards")){
			String winner = o.toString().substring(o.toString().indexOf(":")+1, o.toString().length());
			showCards(winner);
		}
	}
}
