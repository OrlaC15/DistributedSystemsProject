package remote;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.HashMap;
import java.util.Map;

public class HelloWorldServerActor extends UntypedActor {

    final static String actorName = "HelloWorldServerActor";

    public static void main(String[] args) {
        final ActorSystem actorSystem = ActorSystem.create("helloWorldRemoteAS", createConfig());
        final ActorRef actor = actorSystem.actorOf(new Props(HelloWorldServerActor.class), actorName);
        actor.tell("Sending test message to myself",actor);
    }

    static Config createConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("akka.actor.provider",   "akka.remote.RemoteActorRefProvider");
        map.put("akka.remote.transport", "akka.remote.netty.NettyRemoteTransport");
        map.put("akka.remote.netty.tcp.hostname", "127.0.0.1");
        map.put("akka.remote.netty.tcp.port", "2600");
        return ConfigFactory.parseMap(map);
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

    @Override
    public void onReceive(Object msg) {
        System.out.println(actorName + " got: " + msg);
    }
}
