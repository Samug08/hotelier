package RankingHandler;

import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class NotifyReceiver implements Runnable {
    private MulticastSocket ms;
    public static DatagramPacket receivedPacket; // Datagramma ricevuto dal clientUDP
    public static byte[] buffer;

    public NotifyReceiver(MulticastSocket ms) {
        setMs(ms);
    }

    // getter e setter
    public MulticastSocket getMs() {return ms;}
    public void setMs(MulticastSocket ms) {this.ms = ms;}

    // metodo run che si occupa di ricevere le notifiche udp dalla multicastSocket
    @Override
    public void run() {
        try {
            // inizializzo il buffer
            buffer = new byte[1024];
            while(true) {
                // Prepariamo il DatagramPacket per ricevere i dati
                receivedPacket = new DatagramPacket(buffer, buffer.length);
                // Riceviamo il pacchetto dalla multicast
                ms.receive(receivedPacket);
                // Estraiamo il messaggio dal pacchetto
                String receivedMessage = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                // Sincornizzo system.out per non sovrapporre l'output
                synchronized (System.out) {
                    // Stampiamo il messaggio ricevuto
                    System.out.println(receivedMessage);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
