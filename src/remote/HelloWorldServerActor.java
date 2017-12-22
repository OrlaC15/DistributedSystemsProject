package remote;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        System.out.println("IP Address:- " + inetAddress.getHostAddress());
        System.out.println("Host Name:- " + inetAddress.getHostName());
        Map<String, Object> map = new HashMap<>();
        map.put("akka.actor.provider",   "akka.remote.RemoteActorRefProvider");
        map.put("akka.remote.transport", "akka.remote.netty.NettyRemoteTransport");
        map.put("akka.remote.netty.tcp.hostname", inetAddress.getHostAddress());
        map.put("akka.remote.netty.tcp.port", "2600");
       // map.put("akka.cluster.")
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
