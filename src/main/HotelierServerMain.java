package main;

import RankingHandler.NotifySender;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Hotel;
import model.User;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class HotelierServerMain {
    public static int serverPort; // porta su cui si mette in ascolto il server
    public static int timeout;
    // parametri per il pool di thread
    public static int corePoolSize;
    public static int maxPoolSize;
    public static long keepAliveTime;
    public static int maxQueueSize;
    // parametri per la multicast socket
    public static String multicastAddress;
    public static int multicastPort;
    public static MulticastSocket ms;
    // file per persistenza dei dati
    public static File filesHotelsJson; // file json fornito dalla prof per la prima creazione degli hotel
    public static File dbHotels; // file json per il backup successivo degli hotel
    public static File dbUsers; // file json per il backup successivo degli utenti
    // strutture dati condivise
    public static ConcurrentHashMap<String, User> users = new ConcurrentHashMap<String, User>(); // struttura dati concorrente per la memorizzazione degli utenti
    public static ConcurrentHashMap<String, User> loggedUsers = new ConcurrentHashMap<String, User>(); // struttura dati per la memorizzazione degli utenti loggati
    public static ConcurrentHashMap<Integer, Hotel> hotels = new ConcurrentHashMap<Integer, Hotel>(); // struttura dati concorrente per la memorizzazione degli hotel
    // pool di thread per la gestione delle connessioni
    public static ExecutorService pool;
    // thread per il calcolo periodico del ranking
    public static final ScheduledExecutorService rankingThread = Executors.newSingleThreadScheduledExecutor();
    // variabile per il controllo del ciclo
    private static volatile boolean running = true;

    public static void main(String[] args) throws IOException {
        // leggo file di configurazione per parametri di input
        readInputParameter();
        // aggiunge un hook di terminazione per gestire in modo pulito la terminazione del server
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Chiusura server in corso...");
            stopServer(); // Chiude in modo pulito il server
        }));
        // inizializzo le strutture dati contenenti i valori degli hotel
        // dal file fornito dalla prof se non esiste il 'databse' degli hotel(prima attivazione del server)
        // dal database degli hotel se non è la prima attivazione
        if(!dbHotels.exists()) {
            initializationHotel(filesHotelsJson);
        }else {
            initializationHotel(dbHotels);
        }
        // inizializzazione della struttura dati che contiene gli utenti (se esiste il file per il 'database')
        if(dbUsers.exists()) {
            initializationUser(dbUsers);
        }
        //  creazione del server e inizializzazione del pool di thread
        pool = new ThreadPoolExecutor(corePoolSize,
                maxPoolSize,keepAliveTime,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>(maxPoolSize),
                new ThreadPoolExecutor.AbortPolicy());
        // creazione della socket multicast per notificare cambiamenti nel ranking
        ms = new MulticastSocket(multicastPort);
        InetAddress ia = InetAddress.getByName(multicastAddress);
        ms.joinGroup(new InetSocketAddress(ia, multicastPort), null);
        // creazione del thread periodico per il calcolo del ranking
        rankingThread.scheduleAtFixedRate(new NotifySender(ms, hotels, multicastAddress, multicastPort), 0, 1, TimeUnit.MILLISECONDS);
        // creazione socket
        ServerSocket server = new ServerSocket(serverPort);
        System.out.println("Server in ascolto sulla porta " + serverPort + "...");
        while(running) {
            Socket connection = null;
            try{
                connection = server.accept();
            }catch(SocketException e) {
                e.printStackTrace();
                System.exit(1);
            }
            pool.execute(new ClientHandler(connection, users, dbUsers, loggedUsers, hotels, dbHotels));
        }
        server.close();
    }

    // metodo per leggere i file di configurazione
    public static void readInputParameter() throws IOException {
        try(FileInputStream inputParameter = new FileInputStream("/home/samu08/unipi/laboratorio3/hotelier/src/utils/server.properties")) {
            Properties properties = new Properties();
            properties.load(inputParameter);
            serverPort = Integer.parseInt(properties.getProperty("port"));
            filesHotelsJson = new File(properties.getProperty("fileHotel"));
            dbUsers = new File(properties.getProperty("fileUsers"));
            dbHotels = new File(properties.getProperty("fileHotels"));
            timeout = Integer.parseInt(properties.getProperty("timeout"));
            corePoolSize = Integer.parseInt(properties.getProperty("corePoolSize"));
            maxPoolSize = Integer.parseInt(properties.getProperty("maxPoolSize"));
            keepAliveTime = Long.parseLong(properties.getProperty("keepAliveTime"));
            maxQueueSize = Integer.parseInt(properties.getProperty("maxQueueSize"));
            multicastAddress = properties.getProperty("multicastAddress");
            multicastPort = Integer.parseInt(properties.getProperty("multicastPort"));
        }
    }

    // metodo per l'inizializzazione degli hotels dal file json
    public static void initializationHotel(File file) {
        Gson gson = new Gson();
        try(FileReader reader = new FileReader(file)) {
            Hotel[] temporaryHotels = gson.fromJson(reader, Hotel[].class);
            for(Hotel h: temporaryHotels) {
                hotels.put(h.getId(), h);
            }
            // Scrive gli hotel nel file dbHotels in formato JSON leggibile
            Gson prettyGson = new GsonBuilder().setPrettyPrinting().create(); // Configurazione per scrittura formattata
            try(FileWriter writer = new FileWriter(dbHotels)) {
                prettyGson.toJson(temporaryHotels, writer); // Serializza la struttura hotels in formato leggibile
            }catch(IOException e) {
                e.printStackTrace();
                System.err.println("Errore durante la scrittura nel file di backup dbHotels");
                System.exit(1);
            }
        }catch(IOException e) {
            e.printStackTrace();
            System.err.println("File not found");
            System.exit(1);
        }
    }

    // metodo per l'inizializzazione degli utenti dal file json
    public static void initializationUser(File file) {
        Gson gson = new Gson();
        try(FileReader reader = new FileReader(file)) {
            User[] temporaryUsers = gson.fromJson(reader, User[].class);
            for(User u: temporaryUsers) {
                users.put(u.getName(), u);
            }
        }catch(IOException e) {
            e.printStackTrace();
            System.err.println("File not found");
            System.exit(1);
        }
    }

    // Metodo per terminare il server in modo controllato
    public static void stopServer() {
        // Ferma il ciclo principale
        running = false;
        // stoppo il thread che esegue il calcolo del ranking
        rankingThread.shutdown();
        try {
            if (!rankingThread.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                rankingThread.shutdownNow(); // Forza la terminazione del ranking thread
            }
        } catch (InterruptedException e) {
            rankingThread.shutdownNow();
        }
        // Chiudo la socket multicast
        if (ms != null && !ms.isClosed()) {
            ms.close();
        }
        // Salvataggio delle informazioni sugli hotel
        System.out.println("Salvataggio hotels...");
        try (FileWriter fw = new FileWriter(dbHotels)) {
            ArrayList<Hotel> temporaryHotels = new ArrayList<Hotel>();
            for(Hotel h: hotels.values()) {
                temporaryHotels.add(h);
            }
            String jsonObject = new GsonBuilder().setPrettyPrinting().create().toJson(temporaryHotels);
            fw.write(jsonObject);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio degli hotel: " + e.getMessage());
        }
        // Salvataggio delle informazioni sugli utenti
        System.out.println("Salvataggio utenti...");
        try (FileWriter fw = new FileWriter(dbUsers)) {
            Collection<User> objectsUser = users.values();
            String jsonObject = new GsonBuilder().setPrettyPrinting().create().toJson(objectsUser);
            fw.write(jsonObject);
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio degli utenti: " + e.getMessage());
        }
        // Controlla se il pool è stato inizializzato prima di chiamare shutdown
        if (pool != null && !pool.isShutdown()) {
            pool.shutdown(); // Ferma nuovi task
            try {
                if (!pool.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                    pool.shutdownNow(); // Forza la terminazione
                }
            } catch (InterruptedException e) {
                pool.shutdownNow();
            }
        } else {
            System.out.println("Il pool di thread non è stato inizializzato.");
        }
        System.out.println("Chiusura completata.");
    }
}