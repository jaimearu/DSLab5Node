import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Map;

public class Main implements Runnable {

    public Main() throws IOException {
        sendUDPMessage("newNode nameOfTheNode::192.169.0.10", "230.0.0.0",
                4321);
    }
    public static void main(String[] args) throws IOException {
        Thread t = new Thread(new Main());
        t.start();
    }
    public static void sendUDPMessage(String message,
                                      String ipAddress, int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(ipAddress);
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length,
                group, port);
        socket.send(packet);
        socket.close();
    }
    public void receiveUDPMessage(String ip, int port) throws
            IOException {
        byte[] buffer = new byte[1024];
        MulticastSocket socket = new MulticastSocket(4321);
        InetAddress group = InetAddress.getByName("230.0.0.0");
        socket.joinGroup(group);
        while (true) {
            System.out.println("Waiting for multicast message...");
            DatagramPacket packet = new DatagramPacket(buffer,
                    buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(),
                    packet.getOffset(), packet.getLength());
            getNameAndIp(msg);
            if ("OK".equals(msg)) {
                System.out.println("No more message. Exiting : " + msg);
                break;
            }
        }
        socket.leaveGroup(group);
        socket.close();
    }

    // DEES INLEZE EN HASHE
    private ArrayList<String> getNameAndIp(String msg){
        ArrayList<String> temp = new ArrayList<>();
        if (msg.contains("newNode")) {
            String haha = msg.replace("newNode ","");
            if (!haha.isEmpty()) {
                String[] tokens = haha.split("::");
                for (String t : tokens)
                    temp.add(t);
            }
            System.out.println(temp.toString());
            System.out.println("Node added");
        }
        if (msg.contains("remNode")) {
            String haha = msg.replace("remNode ","");
            if (!haha.isEmpty()) {
                String[] tokens = haha.split("::");
                for (String t : tokens)
                    temp.add(t);
            }
            System.out.println(temp.toString());
            System.out.println("Node removed");
        }


        return temp;
    }
    @Override
    public void run() {
        try {
            receiveUDPMessage("230.0.0.0", 4321);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
