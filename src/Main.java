import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

public class Main implements Runnable {


    InetAddress inetAddress = InetAddress.getLocalHost();
    String name = inetAddress.getHostName();
    String thisIp =inetAddress.getHostAddress();
    String previous;
    String next;
    String previousIP = "";
    String nextIP = "";
    boolean setupb;
    ArrayList<String> files = new ArrayList<>();
    boolean first = false;
    boolean running = true;
    public static void main(String[] args) throws IOException {
        Thread t = new Thread(new Main());
        t.start();
    }

    public Main() throws IOException {
        sendUDPMessage("newNode "+name+"::"+thisIp, "230.0.0.0",10000);
        System.out.println("dees is mijn naam "+name);
        System.out.println("dees is mijn ip "+thisIp);
        chekFiles();
        System.out.println("Opgestart");
        setupb = false;
    }


    //Send UDP Messages
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



    //Recieve UDP Messages
    public void receiveUDPMessage(String ip, int port) throws
            IOException {
        byte[] buffer = new byte[1024];
        MulticastSocket socket = new MulticastSocket(port);
        InetAddress group = InetAddress.getByName("230.0.0.0");
        socket.joinGroup(group);
        while (running) {
            System.out.println("Waiting for multicast message...");
            DatagramPacket packet = new DatagramPacket(buffer,
                    buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(),
                    packet.getOffset(), packet.getLength());
            System.out.println(msg);
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


    public void receiveUDPUnicastMessage(String ip, int port) throws
            IOException {
        byte[] buffer = new byte[1024];
        MulticastSocket socket = new MulticastSocket(port);
        InetAddress group = InetAddress.getByName("230.0.0.0");
        socket.joinGroup(group);
        while (true) {
            System.out.println("Waiting for multicast message...");
            DatagramPacket packet = new DatagramPacket(buffer,
                    buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(),
                    packet.getOffset(), packet.getLength());
            System.out.println(msg);
            if (msg.contains("nodeCount"))
                setUp(msg);
            if (msg.contains("newNode"))
               getNameAndIp(msg);
            if (msg.contains("previous"))
                previous(msg);
            else if(msg.contains("next"))
                next(msg);
            if ("shutdown".equals(msg)) {
                shutdown();
                break;
            }
        }
        socket.leaveGroup(group);
        socket.close();
    }

    //Parse message to set up new next node
    private void next(String msg){
        String haha = msg.replace("next ","");
        if (!haha.isEmpty()) {
            String[] tokens = haha.split("::");
            next = tokens[0];
            nextIP = tokens[1];
            System.out.println("Mijne next is nu "+tokens[0]+" "+tokens[1]);
        }
    }

    //Parse message to set up new previous node
    private void previous(String msg){
        String haha = msg.replace("previous ","");
        if (!haha.isEmpty()) {
            String[] tokens = haha.split("::");
            previous = tokens[0];
            previousIP = tokens[1];
            System.out.println("Mijne previous is nu "+tokens[0]+" "+tokens[1]);
        }
    }
    //Parse name and IP of other nodes From UDP Muticast mesages
    private ArrayList<String> getNameAndIp(String msg) throws IOException {
        ArrayList<String> temp = new ArrayList<>();
        if (msg.contains("newNode")) {
            String haha = msg.replace("newNode ", "");
            if (!haha.isEmpty()) {
                String[] tokens = haha.split("::");
                for (String t : tokens)
                    temp.add(t);
            }

        }
        if (setupb) {
            if (first) {
                System.out.println("de tweede is erbij");
                sendUDPMessage("previous " + name + "::ip " + thisIp, temp.get(1), 10000);
                sendUDPMessage("next " + name + "::ip " + thisIp, temp.get(1), 10000);
                next = temp.get(0);
                nextIP = temp.get(1);
                previous = temp.get(0);
                previousIP = temp.get(1);
                System.out.println("Mijne next is nu "+next+" "+nextIP);
                System.out.println("Mijne previous is nu "+previous+" "+previousIP);
                first = false;
            } else {
                if (hashfunction(name, true) < hashfunction(temp.get(0), true) && hashfunction(temp.get(0), true) < hashfunction(next, true)) {
                    sendUDPMessage("previous " + name + "::ip " + thisIp, temp.get(1), 10000);
                    next = temp.get(0);
                    nextIP = temp.get(1);
                }
                if (hashfunction(previous, true) < hashfunction(temp.get(0), true) && hashfunction(temp.get(0), true) < hashfunction(name, true)) {
                    sendUDPMessage("next " + name + "::ip " + thisIp, temp.get(1), 10000);
                    previous = temp.get(0);
                    previousIP = temp.get(1);
                }
            }
        }


        return temp;
    }


    //Check locally stored files
    private void chekFiles(){
        File folder = new File("Files");
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String bestand = listOfFiles[i].getName().replace("Files\\","");
                files.add(bestand);
                System.out.println("Ik heb file "+bestand+" lokaal staan bruur.");
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }
    }

    //ShutDown
    private void shutdown() throws IOException {
        sendUDPMessage("next "+next+"::"+nextIP,previousIP,10000);
        sendUDPMessage("previous "+previous+"::"+previousIP,nextIP,10000);
        for (String file : files){
            sendUDPMessage("File "+file,previousIP,10000);
        }
        running = false;
        System.out.println("thread shut down");
    }


    private void setUp(String msg){
        String haha = msg.replace("nodeCount ","");
        if(Integer.parseInt(haha)<=1){
            System.out.println("ik ben de eerste");
             next = previous = name;
             nextIP = previousIP = thisIp;
            first = true;
        }
        setupb = true;
    }

    //Hashfunction, boolean specifies if the string is a node or not
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
                receiveUDPUnicastMessage("230.0.0.0", 10000);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}

