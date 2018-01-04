package remote;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;


public class  MulticastReceiver  {

    private MulticastSocket s;
    private InetAddress group;

    private static final String MULTICAST_GROUP = "228.5.6.7";
    private static final int port = 2107;


    public MulticastReceiver ( ) throws IOException{
        this.group = InetAddress.getByName(MULTICAST_GROUP);
        this.s = new MulticastSocket(port);
        this.s.joinGroup(group);
    }

    public String run(){
        while (true){
            byte[] buf = new byte[1000];
            DatagramPacket recv = new DatagramPacket(buf, buf.length);
            try {
                s.receive(recv);
                System.out.println("The address: "+recv.getAddress().getHostAddress());
                String actorsAddress = recv.getAddress().getHostAddress();
                return actorsAddress;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
