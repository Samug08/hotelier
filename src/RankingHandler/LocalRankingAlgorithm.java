package RankingHandler;

import model.Hotel;

import java.text.ParseException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalRankingAlgorithm {
    private ConcurrentHashMap<Integer, Hotel> hotels;
    // costante per decadimento temporale
    private double pesoQualita = 0.5;  // Peso della qualità delle recensioni
    private double pesoQuantita = 0.3; // Peso del numero di recensioni
    private double pesoAttualita = 0.2; // Peso dell'attualità delle recensioni

    public LocalRankingAlgorithm(ConcurrentHashMap<Integer, Hotel> hotels) {
        setHotels(hotels);
    }

    // getter e setter
    public ConcurrentHashMap<Integer, Hotel> getHotelList() {return hotels;}
    public void setHotels(ConcurrentHashMap<Integer, Hotel> hotels) {this.hotels = hotels;}


    public ConcurrentHashMap<Integer, Double> ranking() {
        ConcurrentHashMap<Integer, Double> hotelScores = new ConcurrentHashMap<>();

        // Calcola il punteggio per ogni hotel
        for (Hotel hotel : hotels.values()) {
            double score = 0;
            try {
                score = calcolaPunteggio(hotel);
                hotel.setRanking(score);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            hotelScores.put(hotel.getId(), score); // Inserisce l'hotel con il punteggio calcolato
        }

        return hotelScores; // Ritorna i punteggi calcolati per ogni hotel
    }

    // Metodo per calcolare il punteggio totale di un hotel
    private double calcolaPunteggio(Hotel hotel) throws ParseException {
        // Punteggio medio (qualità)
        double punteggioQualita = hotel.getRate();  // Punteggio globale dell'hotel

        // Numero di recensioni (quantità)
        int numeroRecensioni = hotel.getReviewNumber();  // Numero totale di recensioni

        // Punteggio basato sull'attualità delle recensioni
        double punteggioAttualita = calcolaPunteggioAttualita(hotel);

        // Combina i vari punteggi ponderati per ottenere il punteggio finale
        double punteggioFinale = (punteggioQualita * pesoQualita) +
                (numeroRecensioni * pesoQuantita) +
                (punteggioAttualita * pesoAttualita);

        return punteggioFinale;
    }

    // Metodo per calcolare il punteggio in base all'attualità delle recensioni
    private double calcolaPunteggioAttualita(Hotel hotel) throws ParseException {
        // Se l'hotel non ha recensioni, il punteggio di attualità è 0
        if (hotel.getDates() == null || hotel.getDates().isEmpty()) {
            return 0;
        }

        // Calcola la media delle date delle recensioni
        double mediaDate = hotel.calculateAverageDate();

        // Calcoliamo il peso in base alla data della recensione (es. recensioni più recenti valgono di più)
        long now = System.currentTimeMillis();  // Ottieni il tempo corrente in millisecondi
        long differenzaTempo = now - (long) mediaDate; // Differenza tra ora e la media delle date delle recensioni

        // Calcola il peso per l'attualità: recensioni più recenti hanno un punteggio più alto
        double attualita = 1.0 / (1.0 + (differenzaTempo / (1000 * 60 * 60 * 24 * 30.0))); // Ogni 30 giorni diminuisce il peso

        return attualita;
    }
}
