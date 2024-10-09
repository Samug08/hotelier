package main;

import RankingHandler.NotifyReceiver;
import com.google.gson.Gson;
import model.Hotel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HotelierClientMain {
    // scanner per interazione con utente tramite CLI
    private static Scanner scanner;
    // variabili per la socket
    private static int port;
    private static String host;
    private static Socket socket;
    private static Scanner in;
    private static PrintWriter out;
    // variabili per la multicast
    private static String udpHost;
    private static int udpPort;
    private static MulticastSocket ms;
    public static final ScheduledExecutorService rankingReceiver = Executors.newSingleThreadScheduledExecutor();
    // variabili per la logica locale di login
    private static String localName;
    private static boolean logged = false;
    private static boolean running = true;

    public static void main(String[] args) {
        try {
            // leggo file di configurazione per parametri di input
            readInputParameter();
            // socket per la comunicazione con il server
            socket = new Socket(host, port);
            // messaggio di benvenuto
            System.out.println("BENVENUTO IN HOTELIER\n" +
                    "Se vuoi inserire recensioni sulle strutture devi registrarti ed effettuare il login;\n" +
                    "altrimenti puoi consultare le informazioni degli hotel presenti.");
            // scanner per l'interazione con l'utente da CLI, scanner di input (per leggere ciò che arriva dal server)
            // scanner di output (per inviare dati al server), con autoflush true
            try (Scanner inputScanner = new Scanner(System.in);
                 Scanner serverInput = new Scanner(socket.getInputStream());
                 PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true)) {
                scanner = inputScanner;
                in = serverInput;
                out = serverOutput;
                // ciclo while infinito per l'interazione con l'utente
                while(running) {
                    System.out.println("Digita il comando (\"-help\" per visualizzare i comandi disponibili)");
                    // switch sul tipo di operazione digitata dall'utente
                    String action = scanner.nextLine().trim();
                    switch(action) {
                    case "-register":
                        if(logged) {
                            System.out.println("Sei già loggato, prima di creare un nuovo account esegui il logout.");
                        }else {
                            // mando al server il tipo di operazione da svolgere
                            out.println("register");
                            // chiamo il metodo per la registrazione
                            register();
                        }
                        break;
                    case "-login":
                        if(logged) {
                            // se sei già loggato non puoi effettuare di nuovo il login
                            System.out.println("Sei già loggato");
                        }else {
                            // mando al server l'operazione da svolgere
                            out.println("login");
                            login();
                        }
                        break;
                    case "-logout":
                        // controllo se l'utente è loggato
                        if(logged) {
                            // mando al server il comando di logout
                            out.println("logout");
                            logout();
                        }else {
                            System.out.println("Per eseguire il logout devi essere loggato,\n" +
                                    "esegui prima il login poi riprova.");
                        }
                        break;
                    case "-searchHotel":
                        // mando al server la richiesta di ricerca hotel
                        out.println("searchHotel");
                        searchHotel();
                        break;
                    case "-searchAllHotels":
                        // mando al server la richiesta di ricerca di tutti gli hotel
                        out.println("searchAllHotels");
                        searchAllHotels();
                        break;
                    case "-insertReview":
                        if(logged) {
                            // se sono loggato invio al server il tipo di operazione
                            out.println("insertReview");
                            insertReview();
                        }else {
                            System.out.println("Per inserire una recensione devi essere loggato al tuo account.\n" +
                                    "se non hai un account registrati.");
                        }
                        break;
                    case "-showMyBadge":
                        if(logged) {
                            // mando al server la richiesta per visualizzare il badge
                            out.println("showMyBadge");
                            showMyBadge();
                        }else {
                            System.out.println("Per effettuare questa operazione devi essere loggato.");
                        }
                        break;
                    case "-help":
                        // visualizza i comandi disponibili
                        help();
                        break;
                    case "-clear":
                        // Sposta il cursore all'inizio e da l'impressione di una CLI nuova
                        System.out.print("\033[H\033[2J");
                        System.out.flush();
                        break;
                    case "-exit":
                        // uscita da hotelier
                        // quando esco effettua il logout in automatico se sono loggato
                        if(logged) {
                            System.out.println("Prima di uscire effettuo il logout...");
                            out.println("logout");
                            logout();
                        }
                        running = false;
                        System.out.println("Stai uscendo da hotelier, grazie e alla prossima!");
                        // Chiusura delle risorse
                        closeConnection();
                    default:
                        // gestione di comandi non riconosciuti
                        System.out.println("Comando non riconosciuto");
                        break;
                    }
                }
            }
        }catch(ConnectException ce) {
            System.out.println("Nessun server in ascolto sulla porta: " + port);
            ce.printStackTrace();
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            closeConnection();
        }
    }

    // metodo per leggere i file di configurazione
    public static void readInputParameter() throws IOException {
        try(FileInputStream inputParameter = new FileInputStream("/home/samu08/unipi/laboratorio3/hotelier/src/utils/client.properties")) {
            Properties properties = new Properties();
            properties.load(inputParameter);
            port = Integer.parseInt(properties.getProperty("client.port"));
            host = properties.getProperty("client.host");
            udpHost = properties.getProperty("udpHost");
            udpPort = Integer.parseInt(properties.getProperty("udpPort"));
        }
    }

    // metodo per visualizzare a schermo i comandi disponibili
    public static void help() {
        System.out.println("\nComandi: \n" +
                "\"-register\": (per registrarsi a hotelier)\n" +
                "\"-login\": (per effettuare il login)\n" +
                "\"-logout\": (per effettuare il logout)\n" +
                "\"-searchHotel\": (per cercare un hotel di un particolare hotel appartenente a una città)\n" +
                "\"-searchAllHotels\": (per cercare tutti gli hotel di una specifica città)\n" +
                "\"-insertReview\": (per inserire una recensione di un hotel)\n" +
                "\"-showMyBadge\": (per visualizzare il badge dell'utente)\n" +
                "\"-help\": (per visualizzare i comandi disponibili)\n" +
                "\"-clear\": (per ripulire il terminale)\n" +
                "\"-exit\": (per uscire da hotelier)\n");
    }

    // metodo per la registrazione dell'utente
    public static void register() {
        // richiesta nome per registrazione
        String name = requestValidString("Inserisci nome: ",
                "Il campo nome NON può essere vuoto!");
        out.println(name);
        if(in.hasNextLine()) {
            String responseName = in.nextLine();
            if(responseName.equals("OK")) {
                // richiesta psw per registrazione. Se arrivo qui il nome è 'corretto'
                String password = requestValidString("Inserisci password: ",
                        "Il campo password NON può essere vuoto!");
                out.println(password);
                if(in.hasNextLine()) {
                    String responsePsw = in.nextLine();
                    if(responsePsw.equals("OK")) {
                        // Codice da eseguire in caso di registrazione avvenuta con successo
                        System.out.println("Registrazione completata con successo.");
                    }else {
                        System.out.println(responsePsw);
                    }
                }else {
                    System.out.println("Errore: risposta non ricevuta per la password. Probabilmente il server è inattivo");
                }
            }else {
                System.out.println(responseName);
            }
        }else {
            System.out.println("Errore: risposta non ricevuta per il nome. Probabilmente il server è inattivo");
        }
    }

    // metodo per effettuare il login
    public static void login() throws IOException {
        // chiedo nome utente per verificare la sua presenza nel dbUsers lato server
        String name = requestValidString("Inserisci nome: ",
                "Il campo nome NON può essere vuoto!");
        // invio del nome al server
        out.println(name);
        // attendo risposta sul nome
        if(in.hasNextLine()) {
            String responseName = in.nextLine();
            if(responseName.equals("OK")) {
                // se va bene: richiesta password
                String password = requestValidString("Inserisci password: ",
                        "Il campo password NON può essere vuoto!");
                // invio la password al server
                out.println(password);
                // attendo risposta sul riscontro della password
                if(in.hasNextLine()) {
                    String responsePsw = in.nextLine();
                    if(responsePsw.equals("OK")) {
                        // se psw è corretta effettuo il login
                        logged = true;
                        localName = name;
                        System.out.println("Login effettuato con successo.");
                    }else {
                        System.out.println(responsePsw);
                    }
                }else {
                    System.out.println("Errore: impossibile effettuare il login. Probabilmente il server è inattivo");
                }
            }else {
                System.out.println(responseName);
            }
        }else {
            System.out.println("Errore: impossibile effettuare il login. Probabilmente il server è inattivo");
        }
        // se sono loggato posso ricevere le notifiche sulla multicastSocket e quindi la creo
        ms = new MulticastSocket(udpPort);
        InetAddress address = InetAddress.getByName(udpHost);
        ms.joinGroup(new InetSocketAddress(address, udpPort), null);
        rankingReceiver.scheduleAtFixedRate(new NotifyReceiver(ms), 0, 1, TimeUnit.MILLISECONDS);
    }

    // metodo per effettuare il logout
    public static void logout() throws IOException {
        // invio al server il nome con cui sono loggato
        out.println(localName);
        // attendo la conferma di logout
        if(in.hasNextLine() && in.nextLine().equals("OK")) {
            System.out.println("Logout effettuato con successo");
            logged = false;
            InetAddress ia = InetAddress.getByName(udpHost);
            ms.leaveGroup(new InetSocketAddress(ia, udpPort), null);
            ms.close();
        }else {
            System.out.println("Errore: impossibile effettuare il logout. Probabilmente il server è inattivo");
        }
    }

    // metodo per visualizzare il badge
    public static void showMyBadge() {
        // invio il nome di cui voglio sapere il badge
        out.println(localName);
        // attendo la risposta del server
        if(in.hasNextLine()) {
            String badge = in.nextLine();
            System.out.println("Il tuo badge è: "+badge);
        }else {
            System.out.println("Errore: impossibile richiedere il badge. Probabilmente il server è inattivo");
        }
    }

    // metodo per la ricerca di un particolare hotel di una particolare città
    public static void searchHotel() {
        String nameHotel;
        String city;
        // Chiedo il nome dell'hotel
        nameHotel = requestValidString("Inserisci il nome dell'hotel: ",
                "Il campo nome NON può essere vuoto!");
        // invio al server il nome dell'hotel
        out.println(nameHotel);
        // attendo risposta sul nome
        if(in.hasNextLine()) {
            String responseHotel = in.nextLine();
            if(responseHotel.equals("OK")) {
                // se il nome è corretto continuo con la ricerca
                // Chiedo la città in cui si trova
                city = requestValidString("Inserisci la città in cui si trova l'hotel: ",
                        "Il campo nome NON può essere vuoto!");
                // invio al server la città in cui si trova l'hotel
                out.println(city);
                // attendo risposta sul riscontro della città
                if(in.hasNextLine()) {
                    String responseCity = in.nextLine();
                    if(responseCity.equals("OK")) {
                        // se tutto è andato bene stampo i dettagli dell'hotel
                        String hotelJson = in.nextLine();
                        Gson gson = new Gson();
                        Hotel hotelInfo = gson.fromJson(hotelJson, Hotel.class);
                        System.out.println(hotelInfo);
                    }else {
                        // altrimenti stampo il messaggio di errore del server
                        System.out.println(responseCity);
                    }
                }else {
                    System.out.println("Errore: impossibile effettuare la ricerca. Probabilmente il server è inattivo");
                }
            }else {
                // altrimenti stampo il messaggio di errore del server
                System.out.println(responseHotel);
            }
        }else {
            System.out.println("Errore: impossibile effettuare la ricerca. Probabilmente il server è inattivo");
        }
    }

    // metodo per la ricerca di tutti gli hotel di una partricolare città
    public static void searchAllHotels() {
        String city;
        // richiedo all'utente una città valida
        city = requestValidString("Inserisci la città: ",
                "Il campo nome NON può essere vuoto!");
        // invio la città al server
        out.println(city);
        // attendo la risposta
        if(in.hasNextLine()){
            String response = in.nextLine();
            if(response.contentEquals("OK")) {
                // se tutto è andato bene stampo i dettagli degli hotel di quella città
                String hotelsJson = in.nextLine();
                Gson gson = new Gson();
                Hotel[] hotelsInfo = gson.fromJson(hotelsJson, Hotel[].class);
                for(Hotel h: hotelsInfo) {
                    System.out.println("\nHOTEL:-----------------------------------\n"+h+"\n" +
                            "-------------------------------------------\n");
                }
            }else {
                // altrimenti stampo il messaggio di errore del server
                System.out.println(response);
            }
        }else {
            System.out.println("Errore: impossibile effettuare la ricerca. Probabilmente il server è inattivo");
        }
    }


    // metodo per inserire una recensione ad un hotel
    public static void insertReview() {
        String nameHotel;
        String city;
        nameHotel = requestValidString("Inserisci il nome dell'hotel che vuoi recensire:",
                "Il campo nome NON può essere vuoto!");
        // invio al server il nome dell'hotel da recensire
        out.println(nameHotel);
        // attendo la risposta
        if(in.hasNextLine()) {
            String responseName = in.nextLine();
            if(responseName.contentEquals("OK")) {
                // esiste un hotel con quel nome, invio la città
                city = requestValidString("Inserisci la città in cui si trova "+nameHotel+":",
                        "Il campo nome NON può essere vuoto!");
                // invio la città al server 
                out.println(city);
                // attendo la risposta
                if(in.hasNextLine()) {
                    String responseCity = in.nextLine();
                    if(responseCity.contentEquals("OK")) {
                        // se arrivo qui esiste l'hotel in quella determinata città e posso recensirlo
                        int score = requestValidInt("Inserisci la tua recensione globale di "+nameHotel+":",
                                "Inserisci un intero non decimale tra 0 e 5!");
                        int cleaning = requestValidInt("Inserisci la tua recensione sulla pulizia di "+nameHotel+":",
                                "Inserisci un intero non decimale tra 0 e 5!");
                        int position = requestValidInt("Inserisci la tua recensione sulla posozione di "+nameHotel+":",
                                "Inserisci un intero non decimale tra 0 e 5!");
                        int services = requestValidInt("Inserisci la tua recensione sui servizi di "+nameHotel+":",
                                "Inserisci un intero non decimale tra 0 e 5!");
                        int quality = requestValidInt("Inserisci la tua recensione sulla qualità di "+nameHotel+":",
                                "Inserisci un intero non decimale tra 0 e 5!");
                        // invio al server i punteggi della recensione data dall'utente
                        out.println(score+","+cleaning+","+position+","+services+","+quality+","+localName);
                        // stampo il messaggio di conferma del server
                        if(in.hasNextLine()) {
                            String resultReview = in.nextLine();
                            System.out.println(resultReview);
                        }else {
                            System.out.println("Errore: impossibile recensire l'hotel. Probabilmente il server è inattivo");
                        }
                    }else {
                        System.out.println(responseCity);
                    }
                }else {
                    System.out.println("Errore: impossibile effettuare la ricerca. Probabilmente il server è inattivo");
                }
            }else {
                // altrimenti stampo il messaggio del server
                System.out.println(responseName);
            }
        }else {
            System.out.println("Errore: impossibile effettuare la ricerca. Probabilmente il server è inattivo");
        }
    }

    // metodo per ottenere input di tipo stringa validi dall'utente
    private static String requestValidString(String prompt, String message) {
        String input;
        do{
            System.out.println(prompt);
            input = scanner.nextLine().trim();
            if ((!input.isEmpty())) {
                break;
            }
            System.out.println(message);
        }while(true);
        return input;
    }

    // metodo per ottenere input di tipo intero validi dall'utente e compresi tra 0 e 5
    private static int requestValidInt(String prompt, String message) {
        int input;
        while(true) {
            System.out.println(prompt);
            try{
                input = Integer.parseInt(scanner.nextLine().trim()); // converte la stringa in int
                // verifica se l'input è compreso tra 0 e 5
                if(input >= 0 && input <= 5) {
                    break; // esce dal ciclo se il numero è nell'intervallo corretto
                }else {
                    System.out.println("Iserisci un numero tra 0 e 5.");
                }
            }catch(NumberFormatException e) {
                // gestisce l'input non valido
                System.out.println(message);
            }
        }
        return input;
    }


    // metodo per la chiusura delle risorse
    private static void closeConnection() {
        try{
            if(socket != null && !socket.isClosed()) socket.close();
            if(in != null) in.close();
            if(scanner != null) scanner.close();
            if(out != null) out.close();
            if(rankingReceiver != null) rankingReceiver.shutdown();
            System.exit(0);
        }catch(IOException e) {
            System.err.println("Errore durante la chiusura del socket o degli stream: " + e.getMessage());
        }
    }
}
