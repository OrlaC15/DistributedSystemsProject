package poker;

import java.io.IOException;


public abstract class PokerPlayer {
	private DeckOfCards deck;
	protected HandOfPoker currentRound;
	protected HandOfCards hand;
	protected int playerPot;
	protected int roundPot;
	protected int highBet;
	protected String playerName;
	protected int roundOverallBet;
	
	public int lowestPotBetLimit =Integer.MAX_VALUE;
	public String lowestPotPlayerName ="";
	
	public PokerPlayer(DeckOfCards inputDeck) throws InterruptedException {
		deck = inputDeck;
		hand = new HandOfCards(deck);
		playerPot = GameOfPoker.PLAYER_POT_DEFAULT;
	}
	
	public void passHandOfPokerRef(HandOfPoker currentRound){
		this.currentRound = currentRound;
		roundPot = currentRound.pot;
		highBet = currentRound.highBet;
	}
	
	public int truncateBet(int bet) {
		if (bet > lowestPotBetLimit){
			bet = lowestPotBetLimit;
			bet = (lowestPotBetLimit < 0)? 0: lowestPotBetLimit; 
			currentRound.printTruncatedBetPrompt(lowestPotPlayerName, bet);
		}
		return bet;
	}
	
	/*
	 * This method tests if every card in the hand has 0 probability, in this case
	 * no discard is possible. I use this in my main to stop the discards in the case
	 * where none are possible.
	 */
	protected boolean noPossibleDiscardsLeft(){
		boolean noPossibleDiscards = false;
		int totalDiscardProbability = 0;
		for(int i=0;i<HandOfCards.CARDS_HELD;i++){
			totalDiscardProbability += hand.getDiscardProbability(i);
		}
		
		//If the sum of all the discard probabilities is 0, then they all must have probability 0.
		if(totalDiscardProbability==0){
			noPossibleDiscards = true;
		}

		return noPossibleDiscards;
	}

	/*
	 * This is a method I use to return a string value of the type of hand,
	 * it is simply used so that I can show the before and after of removing
	 * iterating through the hand and removing/adding cards.
	 */
	protected String getHandType(){
		String handType = "";

		if(hand.isFlush()){
			handType = "Flush";
		}
		if(hand.isFourOfAKind()){
			handType = "Four of a Kind";
		}
		if(hand.isFullHouse()){
			handType = "Full House";
		}
		if(hand.isHighHand()){
			handType = "High Hand";
		}
		if(hand.isOnePair()){
			handType = "One Pair";
		}
		if(hand.isRoyalFlush()){
			handType = "Royal Flush";
		}
		if(hand.isStraight()){
			handType = "Straight";
		}
		if(hand.isStraightFlush()){
			handType = "Straight Flush";
		}
		if(hand.isThreeOfAKind()){
			handType = "Three of a Kind";
		}
		if(hand.isTwoPair()){
			handType = "Two Pairs";
		}

		return handType;
	}
	
	public void dealNewHand() throws InterruptedException{
		hand = new HandOfCards(deck);
	}
		
	public boolean hasMatchedHighBet(){

		if(roundOverallBet == currentRound.highBet){
			return true;
		}
		else{
			return false;
		}
	}
	
	public abstract boolean isHuman();
	
	public abstract int getCall();
	
	public abstract int getBet();
	
	public abstract boolean showCards(PokerPlayer handWinner);
	
	public abstract int getPlayerType();
	
	public abstract int discard() throws InterruptedException, IOException;
	
	public void awardChips(int amountWon) {
		playerPot += amountWon;
	}
	
	public void subtractChips (int chips) {
		playerPot = playerPot - chips;
	}
	
	public String toString() {
		return playerName;
	}
	
/*	public static void main(String[] args) throws InterruptedException {	
		int numTestsToRun = 100;
		/*
		 * Here I run tests of 100 random hands that start as a random hand and discards
		 * and replenishes cards until every card in the hand has a discard
		 * probability of 0 (no more changes possible), or no more cards can be dealt.
		 *
		for(int k=0;k<numTestsToRun;k++){
			DeckOfCards deck = new DeckOfCards();
			//PokerPlayer player = new PokerPlayer(deck);
			HandOfCards hand = player.hand;

			//This stores a string representation of the hand of cards before discarding
			String handBeforeDiscarding = hand.toString();

			//This is the hand type before discarding.
			String startingHandType = player.getHandType();

			int totalCardsDiscarded = 0;
			int roundsOfDiscards = 0;

			*
			 * Stop discarding cards when there are no possible discards left. 
			 *
			while(!(player.noPossibleDiscardsLeft()) && roundsOfDiscards <50){
				totalCardsDiscarded += hand.discard();
				roundsOfDiscards++;
			}
			//This stores a String with the hand type (high hand, flush, etc.) of the hand after discarding
			String newHandType = player.getHandType();

			System.out.println("Test Number " + (k+1)+ ":");
			System.out.println("----------------------------------------------------------------------------------------");
			System.out.println("Went from a " +startingHandType+ " to a "+newHandType + " by discarding "+totalCardsDiscarded+ " cards in "+roundsOfDiscards+" rounds of discards.");
			System.out.println(handBeforeDiscarding + "  --->  " + hand);
			System.out.println("----------------------------------------------------------------------------------------\n");

		}
	}

*/
}
