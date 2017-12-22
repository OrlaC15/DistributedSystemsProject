package remote;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class PlayerClient {
    public static void main(String[] args) {
        boolean gotPlayerName = false;
        String playerName = "";

        System.out.print("Welcome to Poker! To fully enjoy the game please expand your terminal window!" +
                " Please enter your name: ");

        //get players name from console
        while (!gotPlayerName) {
            Scanner scanner1 = new Scanner(System.in);
            playerName = scanner1.nextLine();
            if (playerName.length() < 1) {
                System.out.print("\nThat is not a valid name, please enter your name: ");
                gotPlayerName = false;
            }
            else{
                gotPlayerName = true;
            }
        }

        System.out.println("Welcome to the game, "+playerName+". We hope you have fun!");
        System.out.println("We're looking for a game for you to join.");

        int port = 2107;
        MulticastReceiver n = null;
        String ipAddress = "";
        try {
            n = new MulticastReceiver();
            ipAddress = n.run();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("We found a game server at "+ipAddress+", starting a new game.");


        //Create actor for player
        ActorSystem playerSystem = ActorSystem.create("PlayerSystem", createConfig());
        final ActorRef me = playerSystem.actorOf(Props.create(PlayerActor.class, playerName), playerName);

        //Get dealer actor, we have to tell it we want to start a new game.
        final String path = "akka.tcp://GameSystem@"+ipAddress+":2600/user/Game";
        ActorRef dealerActor = playerSystem.actorFor(path);
        dealerActor.tell("Play", me);

    }

    private static Config createConfig() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("akka.actor.provider", "akka.remote.RemoteActorRefProvider");
        map.put("akka.remote.transport", "akka.remote.netty.NettyRemoteTransport");
        map.put("akka.remote.netty.tcp.hostname", "127.0.0.1");
        map.put("akka.remote.netty.tcp.port", "2701");
        return ConfigFactory.parseMap(map);
    }
}
