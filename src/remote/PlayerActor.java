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
import poker.HumanPokerPlayer;
import poker.PokerPlayer;

public class PlayerActor extends UntypedActor {
    private PokerPlayer player;

    private static volatile boolean receivedMessage = false;

    private static ActorSystem me;
    private static ActorRef dealer;

    static String actorName = "";

    public PlayerActor(String myName) {
        actorName = myName;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message.toString().compareTo("NeedReply") == 0) {
            String response = "myResponse";

            Scanner scanner1 = new Scanner(System.in);
            response = scanner1.nextLine();
           // System.out.println(response);

            getSender().tell(response, null);
        } else {
            System.out.println("Dealer says: " + message);
            receivedMessage = true;
        }
        //System.out.print("Type a message to send to Dealer: ");
    }

    @Override
    public void preStart() {
        try {
            super.preStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(actorName + " started");

    }

    @Override
    public void postStop() {
        try {
            super.postStop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(actorName + " stopped");
    }

    private static Config createConfig() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("akka.actor.provider", "akka.remote.RemoteActorRefProvider");
        map.put("akka.remote.transport", "akka.remote.netty.NettyRemoteTransport");
        map.put("akka.remote.netty.tcp.hostname", "127.0.0.1");
        map.put("akka.remote.netty.tcp.port", "2701");
        return ConfigFactory.parseMap(map);
    }

    public static void main(String[] args) throws InterruptedException {
        // DeckOfCards d = new DeckOfCards();
        // HumanPokerPlayer p = new HumanPokerPlayer(d);
        me = ActorSystem.create("Player", createConfig());
        final ActorRef player = me.actorOf(new Props(PlayerActor.class), "PlayerActor");
        final String path = "akka.tcp://helloWorldRemoteAS@127.0.0.1:2600/user/DealerActor";
        dealer = me.actorFor(path);
        Scanner sc = new Scanner(System.in);
        String input = "";
        System.out.print("Type a message to send to dealer: ");
        while (input.compareTo("exit") != 0) {

            input = sc.nextLine();
            dealer.tell(input, player);

        }
        System.out.println("exiting");
    }
}
