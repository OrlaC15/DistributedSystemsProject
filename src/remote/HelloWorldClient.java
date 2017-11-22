package remote;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.HashMap;
import java.util.Map;

public class HelloWorldClient {

    public static void main(String[] args) throws Exception {
        final ActorSystem actorSystem = ActorSystem.create("clientAS", createConfig());
        final String path = "akka.tcp://helloWorldRemoteAS@127.0.0.1:2600/user/HelloWorldServerActor";
        final ActorRef actor = actorSystem.actorFor(path);
        actor.tell("Message from client", null);
        //Thread.sleep(5000);
        //actorSystem.shutdown();

    }

    static Config createConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("akka.actor.provider",   "akka.remote.RemoteActorRefProvider");
        map.put("akka.remote.transport", "akka.remote.netty.NettyRemoteTransport");
        map.put("akka.remote.netty.tcp.hostname", "127.0.0.1");
        map.put("akka.remote.netty.tcp.port", "2700");
        return ConfigFactory.parseMap(map);
    }
}
