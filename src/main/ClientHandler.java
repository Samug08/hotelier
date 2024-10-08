package main;

import RankingHandler.LocalRankingAlgorithm;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Hotel;
import model.User;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientHandler implements Runnable {

    // variabili per la socket
    private Socket socket;
    private Scanner in;
    private PrintWriter out;
    // strutture dati condivise
    private ConcurrentHashMap<String, User> users;
    private ConcurrentHashMap<String, User> loggedUsers;
    private ConcurrentHashMap<Integer, Hotel> hotels;
    // file per la persistenza dei dati
    private File dbUsers;
    private File dbHotels;
    private volatile boolean running = true;

    // costruttore
    public ClientHandler(Socket socket, ConcurrentHashMap<String, User> users, File dbUsers, ConcurrentHashMap<String, User> loggedUsers,
                         ConcurrentHashMap<Integer, Hotel> hotels, File dbHotels) {
        this.socket = socket;
        this.users = users;
        this.dbUsers = dbUsers;
        this.loggedUsers = loggedUsers;
        this.hotels = hotels;
        this.dbHotels = dbHotels;
    }


    @Override
    public void run() {
        try {
            System.out.println("L'host con indirizzo: "+socket.getInetAddress()+" e nome: "+socket.getInetAddress().getHostName()+" si è connesso a hotelier.");
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true);
            while(running) {
                handleConnection();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }finally{
            // chiusura della connessione
            closeConnection();
        }
    }

    public void handleConnection() throws IOException {
        // ciclo while per vedere il comando da eseguire
        while (running && in.hasNextLine()) {
            // comando che arriva dal client
            String action = in.nextLine();
            // Ignora input vuoti o spazi bianchi
            if (action.isEmpty()) {
                continue;
            }
            switch (action) {
                case "register":
                    register();
                    break;
                case "login":
                    login();
                    break;
                case "logout":
                    logout();
                    break;
                case "showMyBadge":
                    showMyBadge();
                    break;
                case "searchHotel":
                    searchHotel();
                    break;
                case "searchAllHotels":
                    searchAllHotels();
                    break;
                case "insertReview":
                    insertReview();
                    break;
                default:
                    System.out.println(action);
                    System.out.println("Comando non riconosciuto.");
                    break;
            }
        }
    }

    // metodo per registrarsi a hotelier
    public void register() throws IOException {
        // attendo dal client il nome
        String name = in.nextLine();
        if(users.containsKey(name)) {
            // se esiste già un utente con il nome indicato, mando un messaggio di avviso al client
            out.println("Sei già registrato, effettua il login. Oppure il Nome utente è già in uso, riprova con un nome diverso");
        }else {
            // se il nome non è già in uso invio "OK" al client e attendo la password
            out.println(("OK"));
            String password = in.nextLine();
            // adesso creo l'utente con badge base: "Recensore", lo inserisco nella struttura dati per gli utenti registrati
            // e persisto l'informazione la server aggiornando il file Json dbUsers
            String badge = "Recensore";
            int numberOfReview = 0;
            User user = new User(name, password, badge, numberOfReview);
            users.put(name, user);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonUser = gson.toJson(user);
            appendUser(jsonUser);
            out.println("OK");
        }
    }

    // metodo di appoggio per salvare gli utenti registrati sul file Json dbUsers
    private synchronized void appendUser(String jsonUser) throws IOException {
        // Se il file non esiste, lo creiamo e iniziamo un array JSON
        if (!dbUsers.exists()) {
            try (FileWriter writer = new FileWriter(dbUsers)) {
                writer.write("[\n");
                writer.write(jsonUser);
                writer.write("\n]");
            }
        } else {
            // Se il file esiste già, dobbiamo aprirlo in modalità append
            RandomAccessFile raf = new RandomAccessFile(dbUsers, "rw");
            long fileLength = raf.length();

            // Spostiamo il puntatore al secondo ultimo carattere (prima di ']')
            if (fileLength > 2) {
                raf.setLength(fileLength - 1); // Rimuove la ']' finale
                raf.seek(fileLength - 1);
                raf.writeBytes(",\n"); // Aggiungiamo una virgola per separare i nuovi oggetti
            }

            // Scriviamo il nuovo utente e chiudiamo l'array con la ']' finale
            raf.writeBytes(jsonUser);
            raf.writeBytes("\n]");
            raf.close();
        }
    }

    // metodo per effettuare il login ad hotelier
    public void login() {
        // attendo nome utente
        String name;
        if(in.hasNextLine()) {
            name = in.nextLine();
        }else {
            return;
        }
        // controllo se è registrato
        if(users.containsKey(name) && !loggedUsers.containsKey(name)) {
            // se è registrato mando la strings "OK" al client
            out.println("OK");
            // attendo la psw dal client
            String password = in.nextLine();
            // recupero tra gli utenti registrati quello con il nome passato
            User utente = users.get(name);
            // controllo se la password corrisponde con quella dell'utente
            if(utente.getPassword().contentEquals(password)) {
                // se corrisponde aggiungo l'utente ai loggati e notifico l'avvenuta registrazione al client
                loggedUsers.put(name, users.get(name));
                out.println("OK");
            }else{
                // altrimenti notifico al client l'insuccesso dell'operazione
                out.println("Password errata, ritenta il login");
            }
        }else{
            // se non è registrato lo notifico al client
            out.println("Nome utente errato, non registrato, o già in uso.");
        }
    }

    // metodo per effettuare il logout dall'account hotelier
    public void logout() {
        String name = in.nextLine();
        if(loggedUsers.containsKey(name)) {
            out.println("OK");
            loggedUsers.remove(name);
        }else {
            out.println("Utente non loggato, impossibile effettuare il logout");
        }
    }

    // metodo per mostrare il badge di un particolare utente
    public void showMyBadge() {
        String name = in.nextLine();
        String badge = loggedUsers.get(name).getBadge();
        out.println(badge);
    }

    // metodo per cercare un hotel in una determinata città
    public void searchHotel() {
        // attendo la stringa nameHotel
        String nameHotel = in.nextLine();
        boolean found = false;
        for(Hotel h: hotels.values()) {
            // se esiste un hotel con quel nome mi faccio mandare dal client anche la città
            if(h.getName().trim().toLowerCase().contentEquals(nameHotel.toLowerCase())) {
                out.println("OK");
                String city = in.nextLine();
                if(h.getCity().trim().toLowerCase().contentEquals(city.toLowerCase())) {
                    // esiste un hotel con quel nome in quella determinata città
                    out.println("OK");
                    found = true;
                    Gson gson = new Gson();
                    String hotelJson = gson.toJson(h);
                    out.println(hotelJson);
                    break;
                }else {
                    out.println("Non esiste nessun hotel con nome "+nameHotel+" in questa città: "+city);
                }
                return;  // Interrompe la ricerca dopo aver trovato il nome corrispondente
            }
        }
        // se non ho trovato nessuna corrispondenza lo notifico al client
        if(!found) {
            out.println("Nome hotel errato o hotel inesistente.");
        }

    }

    // metodo per cercare tutti gli hotel in una particolare città
    public void searchAllHotels() {
        String city = in.nextLine();
        ArrayList<Hotel> hotelInCity = new ArrayList<Hotel>();
        boolean existOne = false;
        for(Hotel h: hotels.values()) {
            if(h.getCity().trim().toLowerCase().contentEquals(city.toLowerCase())) {
                // se sono qui vuol dire che c'è almeno un hotel di quella città
                // e lo aggiungi all'array di hotel da inviare al client
                existOne = true;
                hotelInCity.add(h);
            }
        }
        // una volta finito il ciclo for in hotelInCity ci sono gli hotel da inviare al client
        // se la variabile OK e true (c'è almeno un hotel)
        if(existOne) {
            out.println("OK");
            Gson gson = new Gson();
            String hotelsJson = gson.toJson(hotelInCity);
            out.println(hotelsJson);
        }else {
            // vuol dire o che non ci sono hotel in quella città o ha digitato male la città
            out.println("Hai digitato male la città, o non ci sono hotel in questa città. Riprova");
        }

    }

    public void insertReview() {
        // attendo la stringa nameHotel
        String nameHotel = in.nextLine();
        boolean found = false;
        for(Hotel h: hotels.values()) {
            // se esiste un hotel con quel nome mi faccio mandare dal client anche la città
            if(h.getName().trim().toLowerCase().contentEquals(nameHotel.toLowerCase())) {
                found = true;
                out.println("OK");
                String city = in.nextLine();
                if(h.getCity().trim().toLowerCase().contentEquals(city.toLowerCase())) {
                    // esiste un hotel con quel nome in quella determinata città
                    out.println("OK");
                    // A questo punto, ricevo i punteggi della recensione
                    String reviewScores = in.nextLine();
                    String[] scores = reviewScores.split(",");
                    if (scores.length == 6) {
                        int score = Integer.parseInt(scores[0]);
                        int cleaning = Integer.parseInt(scores[1]);
                        int position = Integer.parseInt(scores[2]);
                        int services = Integer.parseInt(scores[3]);
                        int quality = Integer.parseInt(scores[4]);
                        String userName = scores[5];
                        // aggiorno il numero di recensioni dell'utente
                        User user = loggedUsers.get(userName);
                        user.updateReviewNumber();
                        // persistenza degli utenti nel database
                        synchronized(dbUsers) {
                            try (FileWriter fw = new FileWriter(dbUsers)) {
                                Collection<User> objectsUser = users.values();
                                String jsonObject = new GsonBuilder().setPrettyPrinting().create().toJson(objectsUser);
                                fw.write(jsonObject);
                            } catch (IOException e) {
                                System.err.println("Errore nel salvataggio degli utenti: " + e.getMessage());
                            }
                        }
                        // creo l'array dei ratings
                        int[] ratings = new int[5];
                        ratings[0] = score;
                        ratings[1] = cleaning;
                        ratings[2] = position;
                        ratings[3] = services;
                        ratings[4] = quality;
                        // aggiorno le recensioni dell'hotel e persisto i dati nel file Json
                        h.addDates();
                        h.updateRate(score);
                        h.updateRatings(ratings);
                        h.updateNumberReviews();
                        // persisto i dati degli hotel sul file Json
                        synchronized(dbHotels) {
                            try (FileWriter fw = new FileWriter(dbHotels)) {
                                ArrayList<Hotel> temporaryHotels = new ArrayList<Hotel>();
                                for (Hotel hotel : hotels.values()) {
                                    temporaryHotels.add(hotel);
                                }
                                String jsonObject = new GsonBuilder().setPrettyPrinting().create().toJson(temporaryHotels);
                                fw.write(jsonObject);
                            } catch (IOException e) {
                                System.err.println("Errore nel salvataggio degli hotel: " + e.getMessage());
                            }
                        }
                        // notifico al client l'avvenuta recensione
                        out.println("Recensione aggiunta con successo all'hotel " + nameHotel);
                    }
                }else {
                    out.println("Non esiste nessun hotel con nome "+nameHotel+" in questa città: "+city);
                    return;
                }
            }
        }
        if(!found) {
            out.println("Nome hotel errato o hotel inesistente.");
        }
    }


    // Metodo per chiudere la connessione e liberare le risorse
    private void closeConnection() {
        try {
            running = false;
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.out.println("Connessione chiusa: " + socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
