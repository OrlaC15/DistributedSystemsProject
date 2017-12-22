package remote;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


public class MulticastSender {

    private MulticastSocket s;
    private InetAddress group;

    private static final String MULTICAST_GROUP = "228.5.6.7";
    private static final int port = 2107;

    public MulticastSender ( ) throws IOException{
        this.group = InetAddress.getByName(MULTICAST_GROUP);
        this.s = new MulticastSocket(port);
        this.s.joinGroup(group);
    }

    public void send(String msg ) throws IOException{
        msg = "I am the actor";
        DatagramPacket hi = new DatagramPacket(msg.getBytes(), msg.length(), group, port);
        s.send(hi);
    }

    public void startSender() throws IOException {
        while (true) {
            try {
                this.send(InetAddress.getLocalHost().getHostAddress());
                Thread.sleep(5000);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
