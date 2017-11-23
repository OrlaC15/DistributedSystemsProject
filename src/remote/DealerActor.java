package remote;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

public class DealerActor extends UntypedActor {
	private ActorRef me;
	private static ActorRef player;
	static volatile boolean gotPlayer = false;
	final static String actorName = "DealerActor";

	static Config createConfig() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("akka.actor.provider", "akka.remote.RemoteActorRefProvider");
		map.put("akka.remote.transport", "akka.remote.netty.NettyRemoteTransport");
		map.put("akka.remote.netty.tcp.hostname", "127.0.0.1");
		map.put("akka.remote.netty.tcp.port", "2600");
		return ConfigFactory.parseMap(map);
	}

	@Override
	public void preStart() {
		super.preStart();
		System.out.println(actorName + " started");
	}

	@Override
	public void postStop() {
		super.postStop();
		System.out.println(actorName + " stopped");
	}

	@Override
	public void onReceive(Object msg) {
		System.out.println("Player says: " + msg);
		player = getSender();
		System.out.print("Type a message to send to Player: ");
		gotPlayer = true;
	}

	public static void main(String[] args) {
		final ActorSystem actorSystem = ActorSystem.create("helloWorldRemoteAS", createConfig());
		final ActorRef me = actorSystem.actorOf(new Props(DealerActor.class), actorName);

		Scanner sc = new Scanner(System.in);
		String input = "";

		if (player == null) {
			System.out.println("No players currently connected.");
		}
		while (true) {
			// System.out.println("in loop");
			if (gotPlayer) {
				while (input.compareTo("exit") != 0) {
					input = sc.nextLine();
					player.tell(input, me);
				}
				System.out.println("exiting");
			}
		}

	}
}
