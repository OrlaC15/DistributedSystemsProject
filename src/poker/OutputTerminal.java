package poker;

import akka.actor.ActorRef;
import akka.pattern.Patterns;
import akka.util.Timeout;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Scanner;

public class OutputTerminal {
	ActorRef dealer;
	ActorRef player;

	public OutputTerminal(ActorRef dealer, ActorRef player){
		this.dealer = dealer;
		this.player = player;
	}
	
	public void printout(String Output){

		player.tell(Output, dealer);
		
		/*System.setOut(GameOfPoker.defaultPrintStream);
		
		System.out.println("TERMINAL##>"+Output);
		
		System.setOut(new PrintStream(new OutputStream() {
			  public void write(int b) {
			    // NO-OP
			  }
			}));*/

	}
	
	public String readInString()  {

		//player.tell("I need your input now.", dealer);
		Timeout timeout = new Timeout(Duration.create(30, "seconds"));
		Future<Object> future = Patterns.ask(player, "NeedReply", timeout);
		String result = "null";
		try {
			result = (String) Await.result(future, timeout.duration());
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(result);

		return result;

		/*
		Scanner reader = new Scanner(System.in);
		
		String input = reader.next();
		
		return input;*/
	}
	/*
	public int readInSingleInt(){
		Scanner reader = new Scanner(System.in);
		
		int input = reader.nextInt();
		
		return input;
	}

	public ArrayList<Integer> readinMultipleInt(){
		Scanner reader = new Scanner(System.in);
				
		String input = reader.nextLine();		
		ArrayList<Integer> numbers = new ArrayList<Integer>();

		for(int i=0;i<input.length();i++){
			if(Character.getNumericValue(input.charAt(i)) >0 && Character.getNumericValue(input.charAt(i)) <=5){
				numbers.add(Character.getNumericValue(input.charAt(i)));
			}
		}
		return numbers;
	}*/
	
}
