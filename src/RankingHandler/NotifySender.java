package RankingHandler;

import model.Hotel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NotifySender implements Runnable {
    private ConcurrentHashMap<Integer, Hotel> hotels;
    private MulticastSocket ms;
    private String multicastAddress;
    private int multicastPort;
    // mappa con {città1: [hotel in ordine di ranking], città2: ...}
    private Map<String, List<Hotel>> previousRankingInCity = new HashMap<>();

    public NotifySender(MulticastSocket ms, ConcurrentHashMap<Integer, Hotel> hotels, String multicastAddress, int multicastPort) {
        setMs(ms);
        setHotels(hotels);
        setMulticastAddress(multicastAddress);
        setMulticastPort(multicastPort);
    }

    // getter e setter
    public void setMs(MulticastSocket ms) {this.ms = ms;}
    public MulticastSocket getMs() {return ms;}
    public void setHotels(ConcurrentHashMap<Integer, Hotel> hotels) {this.hotels = hotels;}
    public ConcurrentHashMap<Integer, Hotel> getHotels() {return hotels;}
    public void setMulticastAddress(String multicastAddress) {this.multicastAddress = multicastAddress;}
    public String getMulticastAddress() {return multicastAddress;}
    public void setMulticastPort(int multicastPort) {this.multicastPort = multicastPort;}
    public int getMulticastPort() {return multicastPort;}

    // metodo run che avvia il calcolo del ranking algorithm e notifica il client ogni volta
    // in cui cambia la "classifica" degli hotel in una città in base al ranking
    @Override
    public void run() {
        try {
            // Raggruppa gli hotel per città
            // hotelsInCity è fatto cosi': {città1: h1->h2->h3..., città2: h1->h2...}
            Map<String, List<Hotel>> hotelsInCity = new HashMap<>();
            for (Hotel h: hotels.values()) {
                hotelsInCity.computeIfAbsent(h.getCity(), x -> new ArrayList<>()).add(h);
            }
            // Per ogni città, calcola il ranking e verifica se è cambiato
            for (String city : hotelsInCity.keySet()) {
                // hotelList a ogni iterazione è un array dei soli hotel di una particolare città
                List<Hotel> hotelList = hotelsInCity.get(city);
                // calcola il ranking e restituisco una mappa: {idHotel: _, ranking: _,....}
                LocalRankingAlgorithm rankingAlgorithm = new LocalRankingAlgorithm(hotels);
                rankingAlgorithm.ranking();
                // Ordina gli hotel della particolare città del ciclo for in base al ranking
                hotelList.sort(Comparator.comparingDouble(Hotel::getRanking).reversed());
                // Confronta il ranking attuale con quello precedente per questa città per vedere se
                // c'è stato un cambiamento in prima posizione o se è nullo.
                if(!previousRankingInCity.containsKey(city) || previousRankingInCity.get(city) == null ||
                        hotelList.get(0).getId() != previousRankingInCity.get(city).get(0).getId()) {
                    // Se il primo hotel con ranking maggiore è cambaito notifica il client
                    String message = "\"" + hotelList.get(0).getName() + "\" adesso è quello con rank maggiore nella città di " + city;
                    sendMulticastMessage(message);
                }
                // Aggiorna il ranking per la città in cui siamo (ciclo for)
                previousRankingInCity.put(city, new ArrayList<>(hotelList));
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Metodo per inviare un messaggio via multicast
    private void sendMulticastMessage(String message) throws IOException {
        byte[] bufferMsg = message.getBytes();
        InetAddress group = InetAddress.getByName(multicastAddress);
        DatagramPacket packet = new DatagramPacket(bufferMsg, bufferMsg.length, group, multicastPort);
        ms.send(packet);
    }
}
