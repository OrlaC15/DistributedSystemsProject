package remote;

import akka.actor.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import poker.DeckOfCards;
import poker.HandOfCards;
import poker.PlayingCard;

import java.util.HashMap;
import java.util.Map;

public class DeckActor extends UntypedActor {

    private DeckOfCards deck;

    public DeckActor(DeckOfCards deck){
        this.deck = deck;
    }

    @Override
    public void onReceive(Object message) {

        if (message.equals("Deal Next")){
            PlayingCard card = null;
            try {
                card = deck.dealNext();
            }
            catch (Exception e){
                System.out.println(e.getStackTrace());
            }
            getSender().tell(card, getSelf());
        }
        else if (message.equals("Shuffle")){
            deck.shuffle();
        }
        else if (message.equals("Reset")){
            deck.reset();
        }
        else {
            System.out.println("Shocking Carry On Here: " +message);
        }
    }

    public static void main(String args[]){
        ActorSystem system = ActorSystem.create();
        ActorRef testDeck = system.actorOf(Props.create(DeckActor.class, new DeckOfCards()));

        HandOfCards handOfCards = null;
        try {
            handOfCards = new HandOfCards(testDeck);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i=0; i<25; i++){
            System.out.println(handOfCards.toString());
            try {
                handOfCards.replaceCardFromDeck(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private static Config createConfig() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("akka.actor.provider", "akka.remote.RemoteActorRefProvider");
        map.put("akka.remote.transport", "akka.remote.netty.NettyRemoteTransport");
        map.put("akka.remote.netty.tcp.hostname", "127.0.0.1");
        map.put("akka.remote.netty.tcp.port", "2799");
        return ConfigFactory.parseMap(map);
    }
}
