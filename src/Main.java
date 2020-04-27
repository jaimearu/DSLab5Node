import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

public class Main implements Runnable {

    String name = "NodeNaam";
    String thisIp = "192.168.1.10";
    String previous;
    String next;
    String previousIP = "";
    String nextIP = "";
    boolean first = false;
    boolean running = true;
    public static void main(String[] args) throws IOException {
        Thread t = new Thread(new Main());
        t.start();
    }

    public Main() throws IOException {
        sendUDPMessage("newNode "+name+"::"+thisIp, "230.0.0.0",
                4321);
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
        while (running) {
            System.out.println("Waiting for multicast message...");
            DatagramPacket packet = new DatagramPacket(buffer,
                    buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(),
                    packet.getOffset(), packet.getLength());
            if(msg.contains("newNode"))
                getNameAndIp(msg);
            else if(msg.contains("nodeCount")){
                setUp(msg);
            }
            else if(msg.contains("shutdown"))
                shutdown();
        }
        socket.leaveGroup(group);
        socket.close();
    }
    private ArrayList<String> getNameAndIp(String msg) throws IOException {
        ArrayList<String> temp = new ArrayList<>();
        if (msg.contains("newNode")) {
            String haha = msg.replace("newNode ","");
            if (!haha.isEmpty()) {
                String[] tokens = haha.split("::");
                for (String t : tokens)
                    temp.add(t);
            }

            if(first){
                sendUDPMessage("previous "+name+"::ip "+thisIp,temp.get(1),5000);
                sendUDPMessage("next "+name+"::ip "+thisIp,temp.get(1),5000);
                first = false;
            }
            else{
                if(hashfunction(name,true)<hashfunction(temp.get(0),true) && hashfunction(temp.get(0),true) < hashfunction(next,true)){
                    sendUDPMessage("previous "+name+"::ip "+thisIp,temp.get(1),5000);
                    next = temp.get(0);
                    nextIP = temp.get(1);
                }
                if(hashfunction(previous,true)<hashfunction(temp.get(0),true) && hashfunction(temp.get(0),true) < hashfunction(name,true)){
                    sendUDPMessage("next "+name+"::ip "+thisIp,temp.get(1),5000);
                    previous = temp.get(0);
                    previousIP = temp.get(1);
                }
            }

        }

        return temp;
    }
    private void shutdown() throws IOException {
        sendUDPMessage("next "+next+"::ip "+nextIP,previousIP,5000);
        sendUDPMessage("previous "+previous+"::ip "+previousIP,nextIP,5000);
        running = false;
        System.out.println("thread shut down");
    }
    private void setUp(String msg){
        String haha = msg.replace("nodeCount ","");
        if(Integer.parseInt(haha)<1){
             next = previous = name;
             nextIP = previousIP = thisIp;
            first = true;
        }
    }
    private int hashfunction(String name, boolean node) {
        int hash=0;
        int temp = 0;
        int i;
        for (i = 0; i<name.length();i++) {
            hash = 3 * hash + name.charAt(i);
            temp = temp+ name.charAt(i);
        }
        hash = hash/(temp/7);

        if (node) {
            hash = (hash) / (5);
        }
        else
            hash = hash/53;
        return hash;
    }
    @Override
    public void run() {
        while (running) {
            try {
                receiveUDPMessage("230.0.0.0", 4321);
                receiveUDPMessage("230.0.0.0", 4321);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}

