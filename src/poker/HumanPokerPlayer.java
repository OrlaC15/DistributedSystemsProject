package poker;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class HumanPokerPlayer extends PokerPlayer implements Runnable {
	private ActorRef a;
	public PictureOfHand pic;

	/*
	public HumanPokerPlayer(DeckOfCards inputDeck) throws InterruptedException {
		super(inputDeck);
		twitter = t;
		a = inputDeck;
		this.playerName = t.username; 
		// TODO Auto-generated constructor stub
	}
	 */

	ActorRef player;
	ActorRef dealer;
	OutputTerminal output;

	public HumanPokerPlayer(ActorRef inputDeck, ActorRef dealer, ActorRef player, OutputTerminal UI) throws InterruptedException {
		super(inputDeck);
		this.playerName = player.path().name();
		a= inputDeck;
		this.player = player;
		this.dealer = dealer;
		output = UI;
		// TODO Auto-generated constructor stub
	}



	public int currentBet =0;
	public boolean askToDiscard = false;
	public boolean splitPot = false;
	public boolean isSplitPot() {
		return splitPot;
	}



	/**
	 * Sets the value of the split pot
	 */

	public void setSplitPot(boolean splitPot) {
		this.splitPot = splitPot;
	}

	/**
	 * Should return the value of the bet for the human player.
	 */
	public int getBet(){
		int ret = -1;
		try {
			ret = inHandBet();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.roundOverallBet+=ret;

		return ret;
	}



	/**
	 * If the player is the last in the betting cycle he has the option to call or fold on the hand brining an end to the betting cycle
	 */
	@Override
	public int getCall() {

		String callResponse = "Call";
		String foldResponse = "Fold";
		int call = 0;

		//twitter.updateStatus("Do you want to call the betting or fold? \nTweet 'Call' to call or 'Fold' to fold\n");
		output.printout("Do you want to call the betting or fold? \nTweet 'Call' to call or 'Fold' to fold\n");

		String Answer = output.readInString();
		//String Answer = twitter.waitForTweet();

		if (Answer.equalsIgnoreCase(callResponse)){

			call = currentRound.highBet;

		}else if(Answer.equalsIgnoreCase(foldResponse)){

			call =0;
			try {
				this.Fold();
			}  catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			output.printout("Sorry not a valid response");
			//twitter.updateStatus("Sorry not a valid response");
			this.getCall();

		}


		//playerPot = playerPot - (call -currentBet);
		//currentBet = call;

		this.subtractChips(call - currentBet);
		currentBet = currentRound.highBet;
		return call;

	}

	@Override
	public int getPlayerType() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Should return whether or not the player wants to show his/her cards at the end of the round.
	 * The PokerPlayer that won the hand is past in to be used to check if a player has won.
	 * In many cases a player will only show his/her hand if they won a hand of poker.
	 * @return
	 */
	public boolean showCards(String handWinner){
		if(handWinner.compareTo(getSelf().path().toString())==0){
			output.printout("I win, here is my hand: " + this.hand);
			return true;
		}
		else{
			output.printout("I lose, I do not show my hand");
			return false;
		}

	}


	/**
	 * This asks the player if he want to discard how many he wants to discard and which cards, 
	 * if carries out the discard and sends an updated pic of the hand as a tweet.
	 */

	public int discard() throws InterruptedException,   IOException {
		PictureOfHand pic = new PictureOfHand();

		String positiveResponse = "y";
		String negativeResponse = "n";
		askToDiscard = true;
		int amountToDiscard = 0;

		//twitter.updateStatusWithTextAndImage("Here are your cards! do you want to replace any!?\n If so tweet 'Y' for yes or 'N' for no", pic.createImage(this.hand)  );
		output.printout("Here are your cards! do you want to replace any!?\n If so, reply 'Y' for yes or 'N' for no\n"+hand.toString());
		//String Answer = twitter.waitForTweet();
		String Answer = output.readInString();

		if (Answer.equalsIgnoreCase(positiveResponse)) {
			output.printout("OK how many cards do you need to change you can discard up to 3 cards");
			//twitter.updateStatus("How many cards do you need to change? You can discard up to 3 cards.");
			//int amountToDiscard = output.readInSingleInt();
			boolean gotNumber = false;
			while(gotNumber == false){
				try{
					//String stringToDiscard = twitter.waitForTweet();
					String stringToDiscard = output.readInString();
					if(stringToDiscard.equals(null)){
						break;
					}
					amountToDiscard = Integer.parseInt(stringToDiscard);
					gotNumber =true;
				}catch(NumberFormatException ex){ // handle your exception
					//twitter.updateStatus("Invalid input, try again.");
					output.printout("Invalid input try again.");
				}
				if (amountToDiscard == 1) {
					output.printout("which card do you want to discard? 1 is the first card up to 5 the rightmost card");
					//twitter.updateStatus("Which card(s) do you want to discard? Cards are labelled 1 to 5 from left to right");
					//int discardedCard = output.readinMultipleInt().get(0);
					//String discardedCardString = twitter.waitForTweet();
					String discardedCardString = output.readInString();
					if(discardedCardString.equals(null)){
						break;
					}
					int discardedCard = readinMultipleInt(discardedCardString).get(0);
					if (discardedCard > 0 && discardedCard <= 5) {
						this.hand.replaceCardFromDeck(discardedCard - 1);
						this.hand.sort();
						//twitter.updateStatusWithTextAndImage("Here is your updated hand goodluck!", pic.createImage(this.hand)  );
						output.printout("Here is your updated hand goodluck!\n" + hand.toString());
					} else {
						output.printout("Sorry this isnt a valid card..");
						//twitter.updateStatus("Sorry this isnt a valid card..");
						this.discard();
						break;
					}

				}else if(amountToDiscard == 2 || amountToDiscard == 3 ){
					output.printout("which cards do you want to discard? 1 is the first card up to 5 the rightmost card in the format'123' ");
					//twitter.updateStatus("Which cards do you want to discard? The cards are labelled 1 to 5 from left to right");
					ArrayList<Integer> discardedCard = new ArrayList<Integer>();

					//String discardedCardString = twitter.waitForTweet();
					String discardedCardString = output.readInString();
					discardedCard = readinMultipleInt(discardedCardString);
					if(discardedCard.size() == amountToDiscard){
						for(int i = 0; i<amountToDiscard; i++){
							this.hand.replaceCardFromDeck(discardedCard.get(i)-1);
						}
						this.hand.sort();
						//twitter.updateStatusWithTextAndImage("Here is your updated hand! Good Luck!!", pic.createImage(this.hand)  );
						output.printout("Here is your updated hand! Good Luck!!\n"+hand.toString());

					}else{
						output.printout("Sorry one of the card positions you entered is invalid");
						//twitter.updateStatus("Sorry one of the card positions you entered is invalid");
						this.discard();
						break;
					}

				}else{
					output.printout("Sorry you can only remove between 1 and 3 cards");
					//twitter.updateStatus("Sorry you can only remove between 1 and 3 cards");
					this.discard();
					break;
				}
			}}else if(Answer.equalsIgnoreCase(negativeResponse)){
				output.printout("OK lets continue...");
				//twitter.updateStatus("OK lets continue...");
			}
			else if (Answer.equals(null)){

			}
		return amountToDiscard;
	}


	/**
	 * Tweets the initial had to the player as an image
	 */
	public void tweetInitialCards() throws  IOException, InterruptedException {
		//pic = new PictureOfHand();
		//twitter.updateStatusWithTextAndImage("These are your cards!", pic.createImage(this.hand)  );
		//twitter.updateStatus(this.hand.toString());
		output.printout("These are your cards!");
		output.printout(hand.toString());

	}

	/**
	 * this reads in multiple integers from the user. 
	 */
	public ArrayList<Integer> readinMultipleInt(String input){	
		ArrayList<Integer> numbers = new ArrayList<Integer>();
		for(int i=0;i<input.length();i++){
			if(Character.getNumericValue(input.charAt(i)) >0 && Character.getNumericValue(input.charAt(i)) <=5){
				numbers.add(Character.getNumericValue(input.charAt(i)));
			}
		}
		if(numbers.size() == 0){
			numbers.add(-1);
		}
		System.out.println("Testing reading in multiple integers:");
		for(int i=0;i<numbers.size();i++){
			System.out.println("number "+i+"---> "+numbers.get(i));
		}
		return numbers;
	}

	/**
	 * Returns true if the bet is below the player pot
	 */

	public boolean validBet(int bet){

		boolean validBet = false;

		if(bet<playerPot){
			validBet = true;
		}

		return validBet;
	}

	/**
	 * this returns the bet for the player wether he is the opening bet or or one of the subsequent bets
	 */
	public int getBet(String Bet) {
		boolean isNumber = false;
		int finalBet = 0;

		while(isNumber == false){
			try{
				int bet = Integer.parseInt(Bet);

				if(bet <= this.playerPot && bet >= 0 ){
					finalBet = bet;	
					isNumber =true;
					break;
				}
				else{
					//twitter.updateStatus("Sorry this is an invalid bet");
					output.printout("Sorry this is an invalid bet\n");
					this.getBet();
					break;
				}
			}catch(NumberFormatException ex){
				output.printout("sorry invalid input, try again.");
				//twitter.updateStatus("Sorry Invalid Input");
				Bet = (output.readInString());
			}
		}

		return finalBet;

	}
	/**
	 * will either bet or check there as it is the opening bet
	 */
	public int openingBet() throws   InterruptedException{
		String betResponse = "Bet";
		String checkResponse = "Check";
		int bet = 0;

		if(this.playerPot < 1){
			bet = 0;
		}
		else{
			//twitter.updateStatus("Do you want to open betting? \nTweet 'Bet' to bet or 'Check' to check");
			output.printout("Do you want to open betting? \nTweet 'Bet' to bet or 'Check' to check\n");
		}
		String Answer = output.readInString();
		//String Answer = twitter.waitForTweet();

		if (Answer.equalsIgnoreCase(betResponse)){
			output.printout("How much would you like to bet?\n");
			//twitter.updateStatus("How much would you like to bet?");
			//String openingBet = twitter.waitForTweet();
			String openingBet = output.readInString();
			currentBet = truncateBet(getBet(openingBet));
			if(openingBet.equals(null)){
				return -1;
			}

		}else if(Answer.equalsIgnoreCase(checkResponse)){
			bet =0;
		}else{
			output.printout("Sorry not a valid response");
			//twitter.updateStatus("Sorry not a valid response");
			this.openingBet();

		}
		bet = truncateBet(currentBet);
		//playerPot = playerPot-bet;
		this.roundOverallBet+=bet;

		this.subtractChips(bet);

		return bet;
	}

	/**
	 * returns the boolean for discarding
	 */
	public boolean isAskToDiscard() {
		return askToDiscard;
	}

	/**
	 * sets the boolean for discarding
	 */
	public void setAskToDiscard(boolean askToDiscard) {
		this.askToDiscard = askToDiscard;
	}
	/**
	 * Betting function used to allow the player to call raise or fold the betting in the middle of a hand
	 * 
	 */
	public int inHandBet() throws   InterruptedException{
		int bet = 0;
		String callResponse = "Call";
		String raiseResponse = "Raise";
		String FoldResponse = "Fold";
		if(playerPot< currentRound.highBet){
			output.printout("sorry you cannot take part as the bet is larger than your pot the pot will be split here and you can win up to this amount in the hand");
			//twitter.updateStatus("sorry you cannot take part as the bet is larger than your pot the pot will" 
			//		+" be split here and you can win up to this amount in the hand");
			splitPot = true;
		}
		if(currentRound.highBet == 0){
			this.openingBet();
		}else{
			//twitter.updateStatus("The pot is at " + currentRound.pot + ". Reply with 'call', 'raise' or 'fold' to continue");
			if((currentRound.highBet - currentBet) == 0){
				output.printout("POT = " + currentRound.pot + "\nHighest Bet = " + currentRound.highBet + 
						".\nYou currently have the highest bet with " + currentBet + "chips.\n"
						+ "Reply with 'call', 'raise' or 'fold' to continue");				
			}
			else{
				output.printout("POT = " + currentRound.pot + "\nHighest Bet = " + currentRound.highBet + 
						".\nBet/Call " + (currentRound.highBet - currentBet) + " chips to remain in this hand.\n"
						+ "Reply with 'call', 'raise' or 'fold' to continue");
			}
			System.out.println("getting reply");
			//String Answer = twitter.waitForTweet();
			String Answer = output.readInString();
			output.printout("-------------------\n\n\n\n");
			output.printout("Answer was: "+Answer);
			output.printout("--------------------\n\n\n");

			//System.out.println("\n\n\n\n\n@@@@@@@@@@@@@@@@@@" + Answer + "\n\n\n");

			if(Answer.equalsIgnoreCase(callResponse)){
				output.printout("Ok you have called the pot at "+ currentRound.highBet + "betting");
				//twitter.updateStatus("Ok you have called the pot at "+ currentRound.highBet + "betting");
				bet = (currentRound.highBet-currentBet);
			}else if(Answer.equalsIgnoreCase(raiseResponse)){
				//twitter.updateStatus("The pot is at " + currentRound.pot + " and it will take " + (currentRound.highBet - currentBet) + " to meet the current bet."
				//		+ " How much do you want to raise by?");
				output.printout("The pot is at " + currentRound.pot + " and it will take " + (currentRound.highBet - currentBet) + " to meet the current bet."
						+ " How much do you want to raise by?");

				//String betAmountString = twitter.waitForTweet();
				String betAmountString = output.readInString();
				if(!(betAmountString.equals(null))){
					bet = getBet(betAmountString);
					bet = bet + (currentRound.highBet - currentBet);
					bet = truncateBet(bet);
					currentBet = bet;
					if(!validBet(currentBet)){
						//twitter.updateStatus("Sorry you dont have the money to make this bet");
						output.printout("Sorry you dont have the money to make this bet");
						this.inHandBet();
					}
				}

			}else if(Answer.equalsIgnoreCase(FoldResponse)){
				this.Fold();
				currentBet = 0;
			}
			else{
				//twitter.updateStatus("Sorry that isnt a valid response");
				output.printout("Sorry that isnt a valid response");
				this.inHandBet();

			}
		}
		this.subtractChips(bet);
		return bet;
	}

	/**
	 * returns if the player has folded or not
	 */

	public boolean Fold() throws   InterruptedException {
		String positiveResponse = "y";
		String negativeResponse = "n";

		boolean isFold = false;

		//twitter.updateStatus("Are you sure you want to fold??\n If so tweet Y for yes or N for no");
		output.printout("Are you sure you want to fold??\n If so tweet Y for yes or N for no");
		//String Answer = twitter.waitForTweet();
		String Answer = output.readInString();

		if(!(Answer.equals(null))){
			if (Answer.equalsIgnoreCase(positiveResponse)) {
				isFold = true;
			} else if (Answer.equalsIgnoreCase(negativeResponse)) {
				isFold = false;
				this.inHandBet();
			} else {
				//twitter.updateStatus("Sorry I didnt regcognise this response");
				output.printout("Sorry I didnt regcognise this response");
				this.Fold();
			}
			System.out.println(isFold);

			return isFold;
		}
		return isFold;
	}

	/*public void runApp() throws InterruptedException,   IOException{

		DeckOfCards deck = new DeckOfCards();
		HumanPokerPlayer human = new HumanPokerPlayer(deck, dealer, player);

		System.out.println(human.hand);

		human.discard();

		System.out.println(human.hand);

		human.Fold();
	}*/
	/*
	public static void main(String[] args) throws InterruptedException,   IOException {

		DeckOfCards deck = new DeckOfCards();
		HumanPokerPlayer human = new HumanPokerPlayer(deck, null,null, null);

		System.out.println(human.hand);

		human.discard();

		System.out.println(human.hand);

		human.Fold();

	}*/

	@Override
	public void run() {
		//twitter.appendToCompoundTweet("Testing Compound Tweet");
		//twitter.appendToCompoundTweet("This is coming from the HumanPokerPlayer class");

		try {
			discard();
		} catch (InterruptedException  | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	/*
	public void testAppendString(){
		twitter.appendToCompoundTweet("This is from the HumanPokerPlayer Class");
	}
	 */

	public void replyForNextRound() throws   InterruptedException {
		String[] positiveResponses = {
				"Great game!",
				"Well played!",
				"My goodness!",
				"Didn't see that coming!",
				"Astounding!",
				"What a hand to win with, eh?!"
		};
		Random rand = new Random();

		//twitter.appendToCompoundTweet(positiveResponses[rand.nextInt(positiveResponses.length)] + " Ready for the next round?");
		//twitter.appendToCompoundTweet("Tweet #FOAKLeave to leave or reply to continue. . .");
		//twitter.postCompoundTweet();
		//twitter.waitForTweet();
		output.printout(positiveResponses[rand.nextInt(positiveResponses.length)] + " Ready for the next round?");
		output.printout("reply #FOAKLeave to leave or reply to continue..");
		output.readInString();
	}

	@Override
	public boolean isHuman() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void onReceive(Object o) throws Exception {

		if(o.toString().compareTo("deal new hand")==0){
			System.out.println("....dealing hand in actor "+getSelf());
			dealNewHand();
		}

		if(o.toString().compareTo("get hand")==0){
			System.out.println("....sending back hand["+hand+"] in actor "+getSelf());
			getSender().tell(hand, getSelf());
		}

		if(o.toString().compareTo("get pot")==0){
			System.out.println("....sending back pot ("+playerPot+") from ("+getSelf()+") to "+getSender());
			getSender().tell(playerPot, ActorRef.noSender());
		}

		if(o.toString().compareTo("get opening bet")==0){
			int oBet = openingBet();
			System.out.println("....sending back opening bet ("+oBet+") from ("+getSelf()+") to "+getSender());
			getSender().tell(oBet, ActorRef.noSender());
		}

		if(o.toString().contains("set split pot")){
			boolean choice = Boolean.parseBoolean(o.toString().substring(o.toString().indexOf("set split pot"+"set split pot".length(), o.toString().length()-1)));
			this.setSplitPot(choice);
		}
		if(o.toString().contains("get bet")){
			int bet = this.getBet();
			System.out.println("sending back the bet of "+bet+" from "+getSelf());
			getSender().tell(bet, getSelf());
		}

		if(o.toString().contains("get call")){
			int call = getCall();
			getSender().tell(call, getSelf());
		}

		if(o.toString().contains("show initial cards")){
			this.tweetInitialCards();
		}

		if(o.toString().contains("in hand bet")){
			int bet = this.inHandBet();
			getSender().tell(bet,getSelf());
		}

		if(o.toString().contains("get fold")){
			boolean fold = this.Fold();
			getSender().tell(fold,getSelf());
		}

		if(o.toString().contains("reply for next round")){
			this.replyForNextRound();
		}

		if(o.toString().contains("are you human?")){
			getSender().tell(true,getSelf());
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

		if(o.toString().compareTo("reset current bet")==0){
			currentBet=0;
		}

		if(o.toString().compareTo("tweet initial cards")==0){
			tweetInitialCards();
		}

		if(o.toString().compareTo("reply for next round")==0){
			replyForNextRound();
		}

		if(o.toString().compareTo("round overall bet")==0){
			roundOverallBet =0;
		}

		if(o.toString().contains("show cards")){
			String winner = o.toString().substring(o.toString().indexOf(":")+1, o.toString().length());
			showCards(winner);
		}
	}

	/*@Override
	public void onReceive(Object o) throws Exception {
		if(o instanceof Map){
			Map map = (Map) object;
			if(map.containsKey())

		}
	}*/
}
