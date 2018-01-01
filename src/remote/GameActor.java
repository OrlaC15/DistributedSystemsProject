package remote;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import poker.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class GameActor extends UntypedActor {
    static int port = 2600;
    static ActorSystem gameSystem = ActorSystem.create("GameSystem", createConfig());

    public static void main(String[] args) {
        final ActorRef game = gameSystem.actorOf(Props.create(GameActor.class), "Game");

        try {
            MulticastSender sender = new MulticastSender();
            sender.startSender();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Config createConfig() {
        String address = "0.0.0.0";
        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("akka.actor.provider", "akka.remote.RemoteActorRefProvider");
        map.put("akka.remote.transport", "akka.remote.netty.NettyRemoteTransport");
        map.put("akka.remote.netty.tcp.hostname", address);
        map.put("akka.remote.netty.tcp.port", String.valueOf(port++));
        return ConfigFactory.parseMap(map);
    }


    @Override
    public void onReceive(Object o) throws Exception {
            if(o.toString().compareTo("Play")==0){
                ActorSystem dealerSystem = ActorSystem.create("DealerSystem", createConfig());
                final ActorRef dealer = dealerSystem.actorOf(Props.create(DealerActor.class, "Dealer"), "Dealer");
                ActorRef player = getSender();
                //DeckOfCards deck = new DeckOfCards();

                ActorRef deck = dealerSystem.actorOf(Props.create(DeckActor.class, new DeckOfCards()));

                OutputTerminal a = new OutputTerminal(dealer,player);

                GameOfPoker test = new GameOfPoker(dealer, player, deck, dealerSystem);
                test.run();
            }
        }
    }
