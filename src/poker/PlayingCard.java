package poker;

public class PlayingCard {
	
	//Constant chars to represent each suit of cards
	static public final char HEARTS = 'H', DIAMONDS = 'D', CLUBS = 'C', SPADES = 'S';
	
	// Array containing each of the suits to make initialization of a whole deck in a loop with an index very easy
	static public final char[] SUITS = {HEARTS, DIAMONDS, CLUBS, SPADES};
	
	/*
	 * Constant arrays for card types to initializing card types as easy as looping with array indexes
	 * CARD_TYPE[0] will match with FACE_VALUE[0] and GAME_VALUE[0] to make an Ace card and so on
	 */
	static public final String[] CARD_TYPES = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
	static public final int[] FACE_VALUES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
	static public final int[] GAME_VALUES = {14, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
	
	/**
	 * Constant for how many unique cards there are in a standard pack
	 * @see #PlayingCard(int)
	 */
	static public final int UNIQUE_CARD_COUNT = 52;
	
	//Private internal fields
	private String type;
	private char suit;
	private int faceValue, gameValue;
	
	/*
	 * Constructor as per specification, sets all internal fields and returns one playing cards
	 * See constant arrays above for default values for each parameter in order
	 * See simplified constructor directly below for easy initialization of a standard pack of cards
	 */
	public PlayingCard (String cardType, char suit, int faceValue, int gameValue){
		this.type = cardType;
		this.suit = suit;
		this.faceValue = faceValue;
		this.gameValue = gameValue;
	}
	
	/**
	 * Simplified constructor for making the initialization of a whole pack containing each
	 * card once sorted by suit then face value
	 * @return An array of 52 PlayingCard objects
	 */
	public static PlayingCard[] newFullPack(){
		
		PlayingCard[] newPack = new PlayingCard[52];
		for (int i=0; i<UNIQUE_CARD_COUNT; i++){
			newPack[i] = new PlayingCard(CARD_TYPES[i%13], SUITS[(i/13)%4], FACE_VALUES[i%13], GAME_VALUES[i%13]);
		}
		return newPack;
	}
	
	// Accessor method for suit variable
	public char getSuit(){
		return suit;
	}
	
	// Accessor method for faceValue variable
	public int getFaceValue(){
		return faceValue;
	}
	
	// Accessor method for gameValue variable
	public int getGameValue(){
		return gameValue;
	}
	
	/* 
	 * Returns String with type of card with a char denoting suit concatenated
	 * eg. 10S = 10 of Spades, AD = Ace of Diamonds
	 */
	public String toString (){
		return type + suit;
	}
	
	/*
	 * Main method which tests the class, it uses the simplified constructor and a loop
	 * to instantiate one of each card in a pack and then another for loop to iterate through
	 * the pack and print each card sorted by suit, then face value allowing a visual check
	 * on the console
	 */
	public static void main(String[] args) {
		
		// Array to store cards, we give it a new full pack of cards
		PlayingCard[] testDeck = newFullPack();
		
		// Printing all unique cards
		for (PlayingCard i: testDeck){
			System.out.println(i.toString());
		}

	}

}