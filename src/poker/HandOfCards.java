package poker;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.Random;

public class HandOfCards {
	
	/*
	 * Static constant default values for the calculation of game values for hands
	 */
	public static final int ROYAL_FLUSH_DEFAULT = 1000000000;
	public static final int STRAIGHT_FLUSH_DEFAULT = 900000000;
	public static final int FOUR_OF_A_KIND_DEFAULT = 800000000;
	public static final int FULL_HOUSE_DEFAULT = 700000000;
	public static final int FLUSH_DEFAULT = 600000000;
	public static final int STRAIGHT_DEFAULT = 500000000;
	public static final int THREE_OF_A_KIND_DEFAULT = 400000000;
	public static final int TWO_PAIR_DEFAULT = 300000000;
	public static final int ONE_PAIR_DEFAULT = 200000000;
	public static final int HIGH_HAND_DEFAULT = 100000000;
	public static final int DISCARD_PROBABILITY_SCALE = 3;
	
	/*
	 * Set to true to show game values in toString() method, useful for testing
	 */
	public static final boolean SHOW_GAME_VALUES = false;
	
	/*
	 * Internal fields of hand
	 */
	public static final int CARDS_HELD = 5;
	public PlayingCard[] cardArray;
	//private DeckOfCards deck;
	private ActorRef deck;
	private int playerType;
	/*
	 * Constructor takes in deck, initializes card array and then fills in with 5
	 * cards dealt from deck
	 *
	public HandOfCards(DeckOfCards deck) throws InterruptedException {
		this.deck = deck;
		cardArray = new PlayingCard[CARDS_HELD];
		for (int i=0; i<CARDS_HELD; i++){
			cardArray[i] = this.deck.dealNext();
		}
		sort();
	}*/

	public HandOfCards(ActorRef deck) throws InterruptedException {
		this.deck = deck;
		cardArray = new PlayingCard[CARDS_HELD];
		for (int i=0; i<CARDS_HELD; i++){
			cardArray[i] = getNextCardFromDeck();
		}
		sort();
	}

	private PlayingCard getNextCardFromDeck(){
		Timeout timeout = new Timeout(Duration.create(HandOfPoker.TIMEOUT, "seconds"));
		Future<Object> future = Patterns.ask(deck, "Deal Next", timeout);
		PlayingCard result = null;
		try {
			result = (PlayingCard) Await.result(future, timeout.duration());
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}
	
	public void passPlayerType(ActorRef pokerPlayer){
		playerType = 1;// pokerPlayer.getPlayerType();

	}

	public int getAutomatedPlayerType(){
		return playerType;
	}
	
	public int increaseDiscardProbabilityValue(int discardProbability){
		//out.printout("DP1 ===  " + discardProbability );
		//out.printout("APT ====== " + getAutomatedPlayerType());
		
		float playerTypeCalculation = (float) (DISCARD_PROBABILITY_SCALE + (2 - ((float)1/getAutomatedPlayerType())));
		int newDiscardProbability = (int) (discardProbability*playerTypeCalculation);
		
		//out.printout("PTC = " + playerTypeCalculation);
		//out.printout("DP2 ===  " + newDiscardProbability );
		return newDiscardProbability;
	}
	

	
	public synchronized int discard() throws InterruptedException{
		//out.printout("PT ==== " + playerType);
		//out.printout("Hand =====  " + player.getHandType());
		int numCardsDiscarded = 0;
		Random rand = new Random();
		
		//Random number in the range [0,99]
		int randomNumber = rand.nextInt(80);
		//out.printout("RAND ====    " + randomNumber);
		for(int card=0;card<CARDS_HELD;card++){
			
			/*
			 * The higher our discard probability, the higher the chance that
			 * it will be higher than the random number between 0 and 99. This
			 * also takes into account the two outside cases of 0 and 100. If the 
			 * discard probability is 0, then it will never be greater than 
			 * the random number between 0 and 99, so it will never be discarded.
			 * If the discard probability is 100, it will always be greater than
			 * the random number, so it always will be discarded.
			 */
			if(getDiscardProbability(card) > randomNumber){
				replaceCardFromDeck(card);
				numCardsDiscarded++;
			}
			//If we've already discarded 3 cards from the hand.
			if(numCardsDiscarded == 3){
				break;
			}
		}
		
		//After placing a new card in the hand, we must sort the hand again.
		sort();
		return numCardsDiscarded;
	}
	
	/**
	 * Discards a card at the given index back to the deck and replaces it with a new one
	 * @throws InterruptedException 
	 */
	public synchronized void replaceCardFromDeck(int index) throws InterruptedException{
		if (index >= 0 && index < cardArray.length){
			//deck.returnCard(cardArray[index]);
			cardArray[index] = getNextCardFromDeck();
			
		}

	}
	
	/**
	 * Uses a bubble sort to sort the cards by game value in the hand from high game value to low
	 */
	protected synchronized void sort(){
		boolean swapped = true;
		while (swapped) {
			swapped = false;
			for (int i=1; i < cardArray.length; i++) {
				PlayingCard temp;
				if (cardArray[i-1].getGameValue() < cardArray[i].getGameValue()) {
					temp = cardArray[i-1];
					cardArray[i-1] = cardArray[i];
					cardArray[i] = temp;
					swapped = true;
				}
			}
		}
	}
	
	/**
	 * Returns a string of all the cards in the hand, along with their game value, separated by spaces
	 */
	public String toString(){
		String output = "";
		for (int i=0; i<cardArray.length; i++){
			output += cardArray[i];
			if (SHOW_GAME_VALUES) {
				output += "(" + cardArray[i].getGameValue() + ")" + " ";
			}
			output += " ";
		}
		return output;
	}
	
	/**
	 * Checks whether cards are in sequential order ie. they all decrement in gameValue by 1 as we 
	 * go along the sorted array.
	 * This saves a lot of code repeat in our public boolean hand checks below
	 * 
	 * Fixed to include Ace low sequences 
	 */
	private boolean hasSequentialOrder(){
		boolean sequentialCards = true;
		
		// Check for decrementing gameValues along the array
		for (int i=1; i<cardArray.length; i++){
			if (cardArray[i].getGameValue() != cardArray[i-1].getGameValue()-1){
				sequentialCards = false;
			}
		}
		
		// If we don't have sequential cards by game value, we check for an ace low sequence
		if (!sequentialCards){
			sequentialCards = hasAceLowSequence();
		}
		
		return sequentialCards;
	}
	
	/**
	 * Checks if there is an ace low sequence of cards based on face value
	 * ie. cardArray contains A,5,4,3,2 of any suit
	 */
	private boolean hasAceLowSequence() {
		boolean aceLowSequence = true;
		for(int i=1; i<cardArray.length; i++){
			if (cardArray[i].getFaceValue()-1 != cardArray[(i+1)%cardArray.length].getFaceValue()){
				aceLowSequence = false;
			}
		}
		return aceLowSequence;
	}
	
	
	/**
	 * Checks whether all cards in the array are the same suit
	 * Again, saves code repeat in public boolean hand checks
	 */
	private boolean hasAllSameSuit(){
		boolean sameSuit = true;
		for (int i=1; i<cardArray.length; i++){
			if (cardArray[i-1].getSuit() != cardArray[i].getSuit()){
				sameSuit = false;
			}
		}
		return sameSuit;
	}
	
	/**
	 * Checks whether there is a segment of the card array, where the card game values all match,
	 * with length equal EXACTLY to the number given as input
	 * Saves code repeat in all gameValue matching boolean methods for Hand checking except for isTwoPair
	 * @return False if no segment match of that length or segment of matching cards is longer than length 
	 * specified. Returns true if segment where all card values match of exact input length exists
	 */
	private boolean segmentMatch(int segmentLength){
		// Assume false first
		boolean segmentMatch = false;
		
		// Check all possible end points of segments for the cardArray
		for (int i=segmentLength-1; i<cardArray.length; i++){
			boolean thisSegmentMatches = true;
			
			// Check if previous cards in segment ending at i are equal in game value to cardArray[i]
			for (int j=i-1; j>i-segmentLength; j--){
				if (cardArray[j].getGameValue() != cardArray[i].getGameValue()){
					thisSegmentMatches = false;
					break;
				}
			}
			
			// If all cards in current segment match assume true for segmentMatch first
			if (thisSegmentMatches){
				segmentMatch = true;
			}
			
			/*
			 * If a potential match is found, then check if the card matches beyond the lower boundary
			 * of the segment if they exist
			 */
			if (thisSegmentMatches && i-segmentLength>=0){
				// If the card beyond the lower boundary also match, then the segment length is longer so we set false
				if (cardArray[i].getGameValue() == cardArray[i-segmentLength].getGameValue()){
					segmentMatch = false;
				}
			}
			
			/*
			 * If a potential match is found check if cards match beyond the upper bound of the segment if they exist
			 */
			if (thisSegmentMatches && i<cardArray.length-1){
				// If the card beyond the lower boundary also match, then the segment length is longer so we set false
				if (cardArray[i].getGameValue() == cardArray[i+1].getGameValue()){
					segmentMatch = false;
				}
			}
			
			// If the segment is a match, end the loop and return the boolean
			if (segmentMatch){
				break;
			}
		}
		return segmentMatch;
	}
	
	/**
	 * Checks if the hand matches the criteria for a royal flush
	 * ie. A,K,Q,J,10 of same suit 
	 */
	public boolean isRoyalFlush() {
		// Check first card in array is an ace & sequential order & same suit 
		return cardArray[0].getGameValue() == 14 && hasSequentialOrder() && hasAllSameSuit() && !hasAceLowSequence();
	}
	
	/**
	 * Checks if hand matches criteria for straight flush
	 * ie. Not royal flush, cards of same suit in sequential order
	 */
	public boolean isStraightFlush() {
		
		if (isRoyalFlush()){
			return false;
		}
		return hasSequentialOrder() && hasAllSameSuit();
	}
	
	/**
	 * Checks if hand matches criteria for four of a kind
	 * ie. there are 4 cards of equal game value in the hand
	 */
	public boolean isFourOfAKind() {
		
		return segmentMatch(4);
	}
	
	/**
	 * Checks if hand matches criteria for full house
	 * ie. Cards contain 3 of a kind and an additional pair
	 */
	public boolean isFullHouse() {
		
		return segmentMatch(3) && segmentMatch(2);
		
	}
	
	/**
	 * Checks if hand matches criteria for a simple flush
	 * ie. Not straight flush or royal flush, cards are all of same suit
	 */
	public boolean isFlush() {
		
		if (isStraightFlush() || isRoyalFlush()) {
			return false;
		}
		return hasAllSameSuit();
	}
	
	/**
	 * Checks if hand matches criteria for a simple straight
	 * ie. Not straight flush or royal flush, cards all in sequential order
	 */
	public boolean isStraight(){
		
		if (isStraightFlush() || isRoyalFlush()){
			return false;
		}
		return hasSequentialOrder();
	}
	
	/**
	 * Checks if hand matches criteria for three of a kind
	 * ie. Not a full house, contains exactly 3 matching cards
	 */
	public boolean isThreeOfAKind() {
		
		if (isFullHouse()){
			return false;
		}
		return segmentMatch(3);
	}
	
	/**
	 * Checks if hand matches criteria for Two Pair
	 * ie. hand contains two separate pairs of matching cards
	 */
	public boolean isTwoPair(){
		// Check all hands that could supersede this one 
		if (isFullHouse() || isFourOfAKind()){
			return false;
		}
		
		// Check if there are two pairs of adjacent cards in sorted array with the same game value
		boolean twoPair = false;
		//First check if there is a pair in the first three entries
		for (int i=1; i<cardArray.length-2; i++){
			if (cardArray[i-1].getGameValue() == cardArray[i].getGameValue()){
				/* 
				 * If one pair exists, we check the rest of the array for another pair of 
				 * adjacent cards with the same game value
				 */
				for (int j=i+2; j<cardArray.length; j++){
					if (cardArray[j-1].getGameValue() == cardArray[j].getGameValue()){
						twoPair = true;
					}
				}
			}
		}
		return twoPair;
	}
	
	/**
	 * Checks if hand matches criteria for a pair hand
	 * ie. Contains exactly one pair of matching cards
	 */
	public boolean isOnePair(){

		if (isTwoPair() || isFullHouse()){
			return false;
		}
		return segmentMatch(2);
	}
	
	/**
	 * Checks if hand is simply a high card hand
	 * ie. matches none of the other hand types
	 */
	public boolean isHighHand(){
		if (isRoyalFlush() || isStraightFlush() || isFourOfAKind() || isFullHouse() || isFlush() 
				|| isStraight() || isThreeOfAKind() || isTwoPair() || isOnePair()){
			return false;
		}
		else{
			return true;
		}
	}
	
	/**
	 * Private method returns a string containing the hand type info
	 * Useful for testing
	 */
	private String handType(){
		String handType ="";
		if (isRoyalFlush()){
			handType = "Royal Flush";
		}
		if(isStraightFlush()){
			handType = "Straight Flush";
		}
		if(isFourOfAKind()){
			handType = "Four Of A Kind";
		}
		if(isFullHouse()){
			handType = "Full House";
		}
		if (isFlush()) {
			handType = "Flush";
		}
		if (isStraight()){
			handType = "Straight";
		}
		if (isThreeOfAKind()) {
			handType = "Three Of A Kind";
		}
		if (isTwoPair()) {
			handType = "Two Pair";
		}
		if (isOnePair()) {
			handType = "One Pair";
		}
		if (isHighHand()){
			handType = "High Hand";
		}
		return handType;
	}
	
	/**
	 * Sets the hand to a specific array of cards for testing
	 */
	private void setHand(PlayingCard[] newHand){
		cardArray = newHand;
		sort();
	}
	
	/**
	 * Returns array of cards with segment of matching cards with length equal
	 * exactly to the input parameter brought to the front.
	 * Used below in calculating game values of hands 
	 */
	private PlayingCard[] segmentSort(int segmentLength){
		
		PlayingCard[] segmentSortedCards = new PlayingCard[CARDS_HELD];
		// Assume segment match is false first
		boolean segmentMatch = false;
		
		// Check all possible end points of segments for the cardArray
		for (int i=segmentLength-1; i<cardArray.length; i++){
			boolean thisSegmentMatches = true;
			
			// Check if previous cards in segment ending at i are equal in game value to cardArray[i]
			for (int j=i-1; j>i-segmentLength; j--){
				if (cardArray[j].getGameValue() != cardArray[i].getGameValue()){
					thisSegmentMatches = false;
					break;
				}
			}
			
			// If all cards in current segment match assume true for segmentMatch first
			if (thisSegmentMatches){
				segmentMatch = true;
			}
			
			/*
			 * If a potential match is found, then check if the card matches beyond the lower boundary
			 * of the segment if they exist
			 */
			if (thisSegmentMatches && i-segmentLength>=0){
				// If the card beyond the lower boundary also match, then the segment length is longer so we set false
				if (cardArray[i].getGameValue() == cardArray[i-segmentLength].getGameValue()){
					segmentMatch = false;
				}
			}
			
			/*
			 * If a potential match is found check if cards match beyond the upper bound of the segment if they exist
			 */
			if (thisSegmentMatches && i<cardArray.length-1){
				// If the card beyond the lower boundary also match, then the segment length is longer so we set false
				if (cardArray[i].getGameValue() == cardArray[i+1].getGameValue()){
					segmentMatch = false;
				}
			}
			
			/*
			 * If the card is a match, fill the segmentSortedCards first with the matching 
			 * segment, then with the remaining cards in order
			 */
			if (segmentMatch){
				int filledArrayIndex = 0; 
				
				// First copy the cards which match into the array
				for (int j = i-segmentLength+1; j<=i; j++){
					segmentSortedCards[filledArrayIndex] = cardArray[j];
					filledArrayIndex++;
				}
				
				// Copy the cards of higher value into the array in order
				for (int j=0; j<= i-segmentLength; j++){
					segmentSortedCards[filledArrayIndex] = cardArray[j];
					filledArrayIndex++;
				}
				
				// Copy the cards of higher value into the array in order
				for (int j = i+1; j < cardArray.length; j++){
					PlayingCard temp = cardArray[j];
					segmentSortedCards[filledArrayIndex] = temp;
					filledArrayIndex++;
				}
				break;
			}
		}
		return segmentSortedCards;
	}
	
	/**
	 * Calculates the game value of the hand of cards and returns as int.
	 * 
	 * Uses base 15 exponentials to differentiate card values within hands
	 * as this will ensure no overlap of values between hands and hands with
	 * higher card game values will return a higher hand game value.
	 * 
	 * Uses the official rules of poker that different suits are neither better 
	 * or worse than others. 
	 */
	public int getGameValue(){
		int gameValue =0;
		int exponentialBase = 15;
		/*
		 * If hand is royal flush, set to royal flush default.
		 * All royal flushes are same value across suits according to
		 * official rules of poker.
		 */
		if (isRoyalFlush()){
			gameValue = ROYAL_FLUSH_DEFAULT;
		}
		
		/*
		 * If straight Flush, add the game value of the highest card from the 
		 * sorted array to the default value.
		 * All suits same value according to poker rules
		 */
		if(isStraightFlush()){
			gameValue = STRAIGHT_FLUSH_DEFAULT;
			if (hasAceLowSequence()){
				// Ace is low in straight so we add the high card's game Value
				gameValue+= cardArray[1].getGameValue(); 
			}
			else{
				// Add highest gamevalue in the array
				gameValue += cardArray[0].getGameValue();
			}
		}
		
		/*
		 * If four of a kind, add the game value of the 4 matching cards by 15^1
		 * and add the value of the remaining card by 15^0 to the default value
		 */
		if(isFourOfAKind()){
			gameValue = FOUR_OF_A_KIND_DEFAULT;
			PlayingCard[] segmentSorted = segmentSort(4);
			gameValue += segmentSorted[0].getGameValue() * exponentialBase;
			gameValue += segmentSorted[4].getGameValue();
		}
		
		/*
		 * If full house add to the default the game value of the 3 matching cards 
		 * There can never be two full houses with the same 3 matching cards so 
		 * we ignore the 2 matched cards
		 */
		if(isFullHouse()){
			gameValue = FULL_HOUSE_DEFAULT;
			PlayingCard[] segmentSorted = segmentSort(3);
			gameValue += segmentSorted[0].getGameValue();
		}
		
		/*
		 * For flush add to the default the values of the cards by their base 15 
		 * exponentials according to their order in the sorted array
		 */
		if (isFlush()) {
			gameValue = FLUSH_DEFAULT;
			for (int i=0; i<cardArray.length; i++){
				gameValue += cardArray[i].getGameValue() 
						* Math.pow(exponentialBase, cardArray.length-i-1);
			}
		}
		
		/*
		 * For simple straight, add the value of the largest card in the sorted 
		 * array to the default value
		 */
		if (isStraight()){
			gameValue = STRAIGHT_DEFAULT;
			
			if (hasAceLowSequence()){
				// Ace is low in straight so we add the high card's game Value
				gameValue+= cardArray[1].getGameValue(); 
			}
			else{
				// Add highest gamevalue in the array
				gameValue += cardArray[0].getGameValue();
			}
		}
		/*
		 * For three of a kind add to the default the value of the matching cards
		 * It is impossible to have two hands with the same three matching cards
		 * so we ignore the remaining two cards
		 */
		if (isThreeOfAKind()) {
			gameValue = THREE_OF_A_KIND_DEFAULT;
			PlayingCard[] segmentSorted = segmentSort(3);
			gameValue += segmentSorted[0].getGameValue();
		}
		
		/*
		 * For two pair, add to the default the value of the higher pair by 15^2, the lower pair
		 * by 15^1 and the remaining card by 15^0
		 */
		if (isTwoPair()) {
			gameValue = TWO_PAIR_DEFAULT;
			
			// Ints to store game value of upper pair, lower pair and stray card respectively
			int factorBase2 =0, factorBase1 =0, factorBase0 =0;
			/*
			 * 3 cases, stray unmatched card is at front, middle or end of sorted array
			 */
			if (cardArray[0].getGameValue() != cardArray[1].getGameValue()){
				factorBase2 = cardArray[1].getGameValue();
				factorBase1 = cardArray[3].getGameValue();
				factorBase0 = cardArray[0].getGameValue();
			}
			else if (cardArray[2].getGameValue() != cardArray[3].getGameValue()){
				factorBase2 = cardArray[0].getGameValue();
				factorBase1 = cardArray[3].getGameValue();
				factorBase0 = cardArray[2].getGameValue();
			}
			else if (cardArray[3].getGameValue() != cardArray[4].getGameValue()){
				factorBase2 = cardArray[0].getGameValue();
				factorBase1 = cardArray[2].getGameValue();
				factorBase0 = cardArray[4].getGameValue();
			}
			
			gameValue += factorBase2 * Math.pow(exponentialBase, 2);
			gameValue += factorBase1 * Math.pow(exponentialBase, 1);
			gameValue += factorBase0 * Math.pow(exponentialBase, 0);
			
		}
		/*
		 * For 1 pair, add to the default value the value of the pair by 15^3 plus
		 * the remaining cards in order by 15^2, 15^1 and 15^ 0 respectively
		 */
		if (isOnePair()) {
			gameValue = ONE_PAIR_DEFAULT;
			PlayingCard[] segmentSorted = segmentSort(2);
			for (int i=1; i<segmentSorted.length; i++){
				gameValue += segmentSorted[i].getGameValue() 
						* Math.pow(exponentialBase, segmentSorted.length-i-1);
			}
			
		}
		/*
		 * For high hand, add to the default value the game values of the cards
		 * multiplied in order by 15^4, 15^3, 15^2, 15^1 and 15^0 respectively
		 */
		if (isHighHand()){
			gameValue = HIGH_HAND_DEFAULT;
			for (int i=0; i<cardArray.length; i++){
				gameValue += cardArray[i].getGameValue() 
						* Math.pow(exponentialBase, cardArray.length-i-1);
			}
		}
		
		return gameValue;
	}
	
	/**
	 * Returns a boolean of whether the hand is considered a busted flush
	 * ie. all cards are of the same suit but one
	 */
	public boolean isBustedFlush(){
		boolean bustedFlush = false;
		
		//We check each card in the array and compare its suit to the next
		for (int i=0; i<cardArray.length-1; i++){
			

			//If there is a single card that does not match the previous set true
			if (cardArray[i+1].getSuit() != cardArray[i].getSuit()){
				bustedFlush = true;
				
				//Then check the rest of the array matches card [i]
				for (int j=i+2; j<cardArray.length; j++){
					
					//If another card doesn't match i, we don't have a busted flush
					if (cardArray[i].getSuit() != cardArray[j].getSuit()){
						bustedFlush = false;
					}
					
				}
				//We only want to check for a single card difference so break after one check
				break;
			}
		}
		
		return bustedFlush;
	}
	
	/**
	 * Returns whether the hand is a broken straight
	 * ie. The cards are all in sequential order like a straight but one 
	 */
	public boolean isBrokenStraight(){
		
		if (isStraight()){
			return false;
		}
		
		boolean brokenStraight;
	
		brokenStraight = isBrokenStraightMissingLink() || isBrokenStraightSolidFour() || isBrokenStraightPairDisrupt();
		
	
		return brokenStraight;
	}
	
	/**
	 * Returns a boolean to say whether the hand is a broken straight with 
	 * a missing link card absent in the middle
	 * 
	 */
	private boolean isBrokenStraightMissingLink(){

		boolean brokenStraight = false;
		int missingCount;
		
		// Check for missing link broken straight with odd card at start of array
		brokenStraight = true;
		missingCount = 0;
		for (int i=1; i < cardArray.length-1; i++){
			if (cardArray[i].getGameValue() != cardArray[i+1].getGameValue()+1){
				if (cardArray[i].getGameValue() - cardArray[i+1].getGameValue() ==2){
					missingCount++;
					if (missingCount > 1){
						brokenStraight = false;
						break;
					}
				}
				else {
					brokenStraight = false;
					break;
				}
			}
		}
		
		// Check for missing link broken straight with odd card at end of array
		if (!brokenStraight) {
			brokenStraight = true;
			missingCount = 0;
			for (int i=0; i < cardArray.length-2; i++){
				if (cardArray[i].getGameValue() != cardArray[i+1].getGameValue()+1){
					if (cardArray[i].getGameValue() - cardArray[i+1].getGameValue() ==2){
						missingCount++;
						if (missingCount > 1){
							brokenStraight = false;
							break;
						}
					}
					else {
						brokenStraight = false;
						break;
					}
				}
			}
		}
		
		// Check for missing link broken straight with ace low
		if (!brokenStraight) {
			brokenStraight = true;
			missingCount = 0;
			for (int i=2; i < cardArray.length; i++){
				if (cardArray[i].getFaceValue() != cardArray[(i+1)%cardArray.length].getFaceValue()+1) {
					
					if (cardArray[i].getFaceValue() - cardArray[(i+1)%cardArray.length].getFaceValue() ==2){
						missingCount++;
						if (missingCount > 1){
							brokenStraight = false;
							break;
						}
					}
					else {
						brokenStraight = false;
						break;
					}
				}
			}
		}
		
		return brokenStraight;
	}
	
	/**
	 * Returns a boolean indicating whether the straight is a broken straight with 4 cards 
	 * in sequence and one odd card
	 */
	private boolean isBrokenStraightSolidFour() {
		
		boolean brokenStraight = false;
		
		// Check for broken straight with stray card at beginning of array
		if (cardArray[0].getGameValue() != cardArray[1].getGameValue()+1){
			brokenStraight = true;
			for (int i=1; i<cardArray.length-1; i++){
				if (cardArray[i].getGameValue() != cardArray[i+1].getGameValue()+1){
					brokenStraight = false;
				}
			}
		}
		
		// Check for broken straight with stray card at end of array
		if (!brokenStraight && cardArray[3].getGameValue() != cardArray[4].getGameValue()){
			brokenStraight = true;
			for (int i=0; i<cardArray.length-2; i++){
				if (cardArray[i].getGameValue() != cardArray[i+1].getGameValue()+1){
					brokenStraight = false;;
				}
			}
			
		}
		
		// Check for broken straight solid 4 with ace low
		if (!brokenStraight && cardArray[1].getGameValue() != cardArray[2].getGameValue()+1 &&
				cardArray[4].getFaceValue() == cardArray[0].getFaceValue()+1){
			brokenStraight = true;
			for (int i=2; i<cardArray.length-2; i++){
				if (cardArray[i].getFaceValue() != cardArray[(i+1)%cardArray.length].getFaceValue()+1){
					brokenStraight = false;
				}
			}
		}
		
		return brokenStraight;
		
	}
	
	/**
	 * Returns a boolean to indicate hand is a broken straight with a pair card
	 * interrupting the sequence in the array
	 */
	public boolean isBrokenStraightPairDisrupt(){
		
		boolean brokenStraight = true;
		int pairCount =0;
		 
		
		// Go through the array and check if all cards are straight except for 1 pair
		for (int i=0; i < cardArray.length-1; i++){
			
			if (cardArray[i].getGameValue() != cardArray[i+1].getGameValue()+1){
				
				if(cardArray[i].getGameValue() == cardArray[i+1].getGameValue()){
					pairCount++;
					if (pairCount > 1){
						brokenStraight = false;
						break;
					}
				}
				else {
					brokenStraight = false;
					break;
				}
			}
		}
		
		// Check for the ace low case 
		if (!brokenStraight){
			
			brokenStraight = true;
			
			for (int i=1; i < cardArray.length; i++){
				
				if (cardArray[i].getFaceValue() != cardArray[(i+1)%cardArray.length].getFaceValue()+1){
					
					if(cardArray[i].getFaceValue() == cardArray[(i+1)%cardArray.length].getFaceValue()){
						pairCount++;
						if (pairCount > 1){
							brokenStraight = false;
							break;
						}
					}
					else {
						brokenStraight = false;
						break;
					}
				}
			}
		}
		
		return brokenStraight;
	}
	
	
	/**
	 * Returns an integer probability from 0-100 of improving the hand from a 
	 * straight Flush by discarding the card at the position input
	 */
	
	private int discardProbabilityStraightFlush(int cardPosition) {

		// Start with probability 0 
		int discardProbability = 0;
		
		/*
		 *  Lowest card may improve the hand if a card gotten
		 *  increments the straight sequence
		 */
		if (cardPosition == cardArray.length-1){
			discardProbability += 100*1/(52-cardArray.length);
		}
		
		return discardProbability;
	}
	
	/**
	 * Returns a integer probability from 0-100 of improving the hand from a 
	 * full house by discarding the card at the position input
	 */
	private int discardProbabilityFullHouse(int cardPosition){
		

		int discardProbability = 0;
		
		// Throwing away the pair has a small chance of getting the card to increase the hand to a 4 of a kind
		PlayingCard[] segmentSorted = segmentSort(2);
		if (cardArray[cardPosition].getGameValue() == segmentSorted[0].getGameValue()){
			discardProbability += 100*1/(52 - cardArray.length);
		}
		
		return discardProbability;
	}
	
	/**
	 * Returns a integer probability from 0-100 of improving the hand from a 
	 * flush by discarding the card at the position input
	 */
	private int discardProbabilityFlush (int cardPosition){

		int discardProbability = 0;
		
		/*
		 *  If a broken straight is in the flush, getting a card to complete the flush 
		 *  has the possibility of making a straight flush
		 */
		if (isBrokenStraight()){
			
			// Check our missing card is from either the front or back of sequence 
			if (isBrokenStraightSolidFour()){
				
				// Check if the first card is the odd card and not ace low
				if (cardArray[0].getGameValue() != cardArray[1].getGameValue()+1 
						&& cardArray[4].getFaceValue() != cardArray[0].getFaceValue()+1) {
					
					/*
					 * If the position is the odd card we have the probability of getting a front
					 * or end to the sequence in the suit of our flush to improve 
					 */
					if (cardPosition == 0){
						discardProbability += 100*2/(52-cardArray.length);
					}
				}
				
				// Check if the last card is the odd card and aces not low
				else if (cardArray[3].getGameValue() != cardArray[4].getGameValue()+1
							&& cardArray[4].getFaceValue() != cardArray[0].getFaceValue()+1){
					
					/*
					 * If the position is the odd card we have the probability of getting a front
					 * or end to the sequence in the suit of our flush to improve 
					 */
					if (cardPosition == 4){
						if (cardArray[0].getGameValue() ==14){
							/*
							 * If the broken straight is ace high, only one card 
							 * can upgrade it to straight flush
							 */
							discardProbability += 100*1/(52-cardArray.length);
						}
						else {
							/*
							 * Else there are two cards that could front or end our
							 * broken flush to upgrade
							 */
							discardProbability += 100*2/(52-cardArray.length);
						}
					}
				}
				
				//Ace must be low and the odd card is position 1
				else {
					if (cardPosition == 1) {

						/*
						 * There is only one card in the deck that can improve the hand
						 * as ace is low
						 */
						discardProbability += 100*1/(52-cardArray.length);
					}
				}
				
			}
			else if (isBrokenStraightMissingLink()){

				// If the odd card is the top and not a broken ace low straight
				if (cardArray[0].getGameValue() != cardArray[1].getGameValue()+1 
						&& cardArray[0].getFaceValue() != cardArray[4].getFaceValue()-1){
					if (cardPosition ==0){
						/*
						 * Only one card in the remaining deck can make it a higher hand of a
						 * straight flush
						 */
						discardProbability += 100*1/(52-cardArray.length);
					}
				}
				
				// If the hand is an ace low broken straight, the odd card is in position 1
				else if (cardArray[3].getGameValue() == cardArray[4].getGameValue() +1
						&& cardArray[0].getFaceValue() == cardArray[4].getFaceValue()-1){
					if (cardPosition == 1){
						/*
						 * Only one card in the remaining deck can make it a higher hand of a
						 * straight flush
						 */
						discardProbability += 100*1/(52-cardArray.length);
					}
				}
				
				// Else the odd card is at the end of the array
				else {
					if (cardPosition == 4){
						/*
						 * Only one card in the remaining deck can make it a higher hand of a
						 * straight flush
						 */
						discardProbability += 100*1/(52-cardArray.length);
					}
				}
			}
		}
		
		return discardProbability;
	}
	
	/**
	 * Returns a integer probability from 0-100 of improving the hand from a 
	 * straight by discarding the card at the position input
	 */
	private int discardProbabilityStraight(int cardPosition) {
		
		// Start with probability 0 
		int discardProbability = 0;
		
		/*
		 *  Lowest card may improve the hand if a card gotten
		 *  increments the straight sequence
		 */
		if (cardPosition == cardArray.length-1){
			discardProbability += 100*4/(52-cardArray.length);
		}
		
		/*
		 * If card is a flush buster, increase discard probability by
		 * that of getting another card of the flushing suit  
		 */
		if (isBustedFlush()){
			if (cardArray[cardPosition].getSuit() != 
					cardArray[(cardPosition+1)%cardArray.length].getSuit()
					&& cardArray[cardPosition].getSuit() != 
							cardArray[(cardPosition+2)%cardArray.length].getSuit()){
				discardProbability += 100*(13-4)/(52-cardArray.length);
			}
		}
		
		return discardProbability;
	}
	
	/**
	 * Returns a integer probability from 0-100 of improving the hand from a 
	 * Three of A Kind by discarding the card at the position input
	 */
	private int discardProbabilityThreeOfAKind(int cardPosition) {
		
		int discardProbability = 0;
		PlayingCard[] segmentSorted = segmentSort(3);
		
		/*
		 * For the two unmatched cards we add the probability that we get the 
		 * remaining matched card for a four of a kind and that the two unmatched
		 * match to make a full house
		 */
		if (segmentSorted[0].getGameValue() != cardArray[cardPosition].getGameValue()){
			discardProbability += 100*1/(52-cardArray.length) + 100*3/(52-cardArray.length); 
		}
		
		return discardProbability;
	}
	
	/**
	 * Returns a integer probability from 0-100 of improving the hand from a 
	 * Two Pair by discarding the card at the position input
	 */
	private int discardProbabilityTwoPair(int cardPosition) {
		
		int discardProbability = 0;
		
		/*
		 * If card position is the unmatched card, we add the probability
		 *  that it will match one of the pairs to make a full house
		 */
		if (cardArray[0].getGameValue() != cardArray[1].getGameValue()){
			if (cardPosition == 0){
				discardProbability += 100*4/(52-cardArray.length); 
			}
		}
		else if (cardArray[2].getGameValue() != cardArray[3].getGameValue()){
			if (cardPosition == 2){
				discardProbability += 100*4/(52-cardArray.length); 
			}
		}
		else if (cardArray[3].getGameValue() != cardArray[4].getGameValue()){
			if (cardPosition == 4){
				discardProbability += 100*4/(52-cardArray.length); 
			}
		}
		
		return discardProbability;
	}
	
	/**
	 * Returns a integer probability from 0-100 of improving the hand from a 
	 * Two Pair by discarding the card at the position input
	 */
	private int discardProbabilityOnePair(int cardPosition) {
		
		int discardProbability = 0;

		PlayingCard[] segmentSorted = segmentSort(2);
		
		/*
		 * If the card at that position is busting a flush, we add the chance that
		 * we could make a flush if we discard it
		 */
		if (isBustedFlush()){
			if (cardArray[cardPosition].getSuit() != cardArray[(cardPosition+1)%cardArray.length].getSuit()){
				discardProbability += 100*(13-4)/(52-cardArray.length);
			}
		}
		
		/*
		 * If the card is a paired card breaking a straight, we add the probability
		 * we could make a straight by discarding it
		 */
		if (isBrokenStraight()){
			
			if (cardArray[cardPosition].getGameValue() == segmentSorted[0].getGameValue()){
				
				// If the broken straight is ace high, only one card from each suit can make the straight
				if (cardArray[0].getGameValue() == 14 && cardArray[4].getGameValue() != 2){
					discardProbability += 100*4/(52-cardArray.length);
				}
				// If broken straight is ace low, only one card from each suit can make the straight
				else if (cardArray[0].getFaceValue() == cardArray[4].getFaceValue()-1){
					discardProbability += 100*4/(52-cardArray.length);
				}
				// Else there are 2 cards from each suit that could the front or end of the straight
				else {
					discardProbability += 100*8/(52-cardArray.length);
				}
			}
		}
		
		/*
		 * For the unmatched cards we add the probability that they could match with 
		 * either the other two unmatched cards or the pair 
		 */
		if (cardArray[cardPosition].getGameValue() != segmentSorted[0].getGameValue()){
			discardProbability += 100*8/(52-cardArray.length);
		}
		
		return discardProbability;
	}
	
	/**
	 * Returns a integer probability from 0-100 of improving the hand from a 
	 * High Hand by discarding the card at the position input
	 */
	private int discardProbabilityHighHand(int cardPosition) {
		
		int discardProbability = 0;
		
		/*
		 * For the three lowest ranking cards, we add the probabilities that 
		 * they will match with one of the other four cards
		 */
		
		if (cardPosition>1){
			discardProbability += 100*4*3/(52-cardArray.length);
		}
		
		return discardProbability;
	}

	
	/**
	 * Take the position of a card in the array and returns an int in the range
	 * 0-100 to represent the possibility of the hand being discarded to improve
	 * the poker hand. Returns -1 for invalid input.
	 */
	public int getDiscardProbability(int cardPosition){
		
		// Return -1 if an invalid input is received
		if (cardPosition < 0 || cardPosition >= cardArray.length){
			return -1;
		}
		
		int discardProbability = 0;
		
		if (isRoyalFlush()){
			// No chance of improving a royal flush hand
			discardProbability = 0;
		}
		if (isStraightFlush()){
			discardProbability = discardProbabilityStraightFlush(cardPosition);
		}
		if (isFourOfAKind()){
			// No chance of improving a 4 of a kind hand
			discardProbability = 0;
		}
		if (isFullHouse()){
			discardProbability = discardProbabilityFullHouse(cardPosition);
		}
		if (isFlush()){
			discardProbability = discardProbabilityFlush(cardPosition);
		}
		if (isStraight()){
			discardProbability = discardProbabilityStraight(cardPosition);
		}
		if (isThreeOfAKind()){
			discardProbability = discardProbabilityThreeOfAKind(cardPosition);
		}
		if (isTwoPair()){
			discardProbability = discardProbabilityTwoPair(cardPosition);
		}
		if (isOnePair()){
			discardProbability = discardProbabilityOnePair(cardPosition);
		}
		if (isHighHand()){
			discardProbability = discardProbabilityHighHand(cardPosition);
		}
		
		return increaseDiscardProbabilityValue(discardProbability);
	}


}
