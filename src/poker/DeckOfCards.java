package poker;

import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.Random;


public class DeckOfCards {

	private int cardsDealt;
	private PlayingCard[] deck;
	private Semaphore dealerAvailable;

	/*
	 * Constructor initializes internal fields of deck and semaphore
	 * Shuffles and resets deck to leave ready for play
	 */
	public DeckOfCards(){
		dealerAvailable = new Semaphore(1);
		deck = PlayingCard.newFullPack();
		shuffle();
		reset();
	}

	/**
	 * Returns next non-dealt card and increments cardsDealt index
	 * Uses the semaphore to lock down a critical section in case parallel access occurs
	 */
	public synchronized PlayingCard dealNext() throws InterruptedException{
		
		dealerAvailable.acquire();
		PlayingCard outputCard = null;
		if (cardsDealt < 52){
			outputCard = deck[cardsDealt];
			cardsDealt++;
		}
		dealerAvailable.release();
		
		return outputCard;
	}

	/**
	 * Shuffles the deck by randomly selecting two card indexes in the deck array and swapping the cards
	 * This is repeated by the size of the deck squared times to ensure the deck is thoroughly shuffled
	 * Note: Do not shuffle without resetting
	 */
	public synchronized void shuffle(){
		System.out.println("got into shuffle");
		Random rand = new Random(System.currentTimeMillis());
		System.out.println("got rand");
		for(int i=0; i<(deck.length*deck.length); i++){
			int index1, index2;
			do {
				index1 = rand.nextInt(deck.length);
				index2 = rand.nextInt(deck.length);
			} while (index1 == index2);

			PlayingCard temp = deck[index1];
			deck[index1] = deck[index2];
			deck[index2] = temp;
		}
		System.out.println("got through for loop in shuffle");
	}

	/**
	 * Returns a card to the bottom of the deck.
	 * Uses semaphore to ensure parallel access is not an issue
	 */
	public synchronized void returnCard(PlayingCard discarded) throws InterruptedException{
		int previousIndex = 0;
		dealerAvailable.acquire();
		// Find previous index of card in the deck array
		for (int i=0; i<deck.length && !deck[i].equals(discarded); i++){
			previousIndex = i;
		}
		// Move all other cards up one index in array
		for (int i=previousIndex; i<deck.length-1; i++){
			deck[i] = deck[i+1];
		}
		// Put discarded on bottom of deck
		deck[deck.length-1] = discarded;
		cardsDealt--;
		dealerAvailable.release();;
	}

	/**
	 * Sets cards dealt to zero
	 * Cards will need to be shuffled for a new game
	 */
	public synchronized void reset(){
		cardsDealt = 0;
	}
	
	public String toString() {
		return "cardsDealt:" + cardsDealt;
	}

	/*
	 * Main method tests the deck
	 * Simply run and read the error status at the bottom of the console  
	 */
	public static void main(String[] args) throws InterruptedException {

		boolean errorFound = false;
		DeckOfCards testDeck = new DeckOfCards();
		ArrayList<PlayingCard> cardsDealt = new ArrayList<PlayingCard>();
		ArrayList<PlayingCard> discardedCards = new ArrayList<PlayingCard>();

		// First we check dealing all the cards without discarding any that none are repeated
		for (int i=0; i<52 ; i++){
			PlayingCard nextCard = testDeck.dealNext();
			System.out.println(nextCard.toString());
			if (cardsDealt.contains(nextCard)){
				System.out.println("ERROR: DECK DEALING CARDS ALREADY DEALT");
				errorFound = true;
			}
			cardsDealt.add(nextCard);
		}

		// We check that deck returns a null when all cards are dealt
		if (testDeck.dealNext() != null){
			System.out.println("ERROR: DECK DEALING CARDS WHEN NONE ARE LEFT, SHOULD DEAL NULL");
			errorFound = true;
		}

		// Reset deck and cards dealt
		testDeck.reset();
		cardsDealt.clear();

		// Then deal all the cards, discarding half of them back to the bottom of the deck
		for (int i=0; i<26 ; i++){
			PlayingCard nextCard = testDeck.dealNext();
			System.out.println(nextCard.toString());

			//Make sure that no repeats come back to us
			if (cardsDealt.contains(nextCard)){
				System.out.println("ERROR: DECK DEALING CARDS ALREADY DEALT");
				errorFound = true;
			}

			// Check that we get all original cards before the discarded cards come back
			if (discardedCards.contains(nextCard)){
				System.out.println("ERROR: DECK DEALING CARDS RETURNED IMMEDIATELY");
				errorFound = true;
			}
			cardsDealt.add(nextCard);
			nextCard = testDeck.dealNext();
			testDeck.returnCard(nextCard);
			discardedCards.add(nextCard);
		}

		// Check that the discarded cards begin to deal when the deck is through and that no repeats happen
		for(int i=0; i<26; i++){
			PlayingCard nextCard = testDeck.dealNext();
			if (!discardedCards.contains(nextCard)){
				System.out.println("ERROR: DISCARDED CARDS NOT GOING TO BOTTOM OF THE DECK");
				errorFound = true;
			}
			if (cardsDealt.contains(nextCard)){
				System.out.println("ERROR: DECK DEALING CARDS ALREADY DEALT");
				errorFound = true;
			}
			cardsDealt.add(nextCard);
		}

		// Check that a null is dealt when all these are dealt too
		if (testDeck.dealNext() != null){
			System.out.println("ERROR: DECK DEALING CARDS WHEN NONE ARE LEFT, SHOULD DEAL NULL");
			errorFound = true;
		}

		// Print error status
		if (errorFound){
			System.out.println("###Error found, please check above in console for cause.");
		}
		else {
			System.out.println("###Test completed with no errors. Please cards printed above to manually check cards are in random order");
		}
	}

}
