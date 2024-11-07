package RankingHandler;

import model.Hotel;

import java.text.ParseException;
import java.util.concurrent.ConcurrentHashMap;


public class LocalRankingAlgorithm {
    private ConcurrentHashMap<Integer, Hotel> hotels;
    // costante per decadimento temporale
    private double lambda = 0.1;
    // // Peso per la qualità delle recensioni
    private double qualityWeight = 0.5;
    // Peso per l'attualità delle recensioni
    private double recencyWeight = 0.4;
    // Peso per la quantità delle recensioni
    private double quantityWeight = 0.3;


    public LocalRankingAlgorithm(ConcurrentHashMap<Integer, Hotel> hotels) {
        setHotels(hotels);
    }

    // getter e setter
    public ConcurrentHashMap<Integer, Hotel> getHotelList() {return hotels;}
    public void setHotels(ConcurrentHashMap<Integer, Hotel> hotels) {this.hotels = hotels;}

    // metodo per il calcolo del ranking degli hotel
    public void ranking() {
        // Aggiorna il tempo corrente ogni volta che si calcola il ranking
        long now = System.currentTimeMillis();
        // Calcola il punteggio per ogni hotel
        for (Hotel hotel : hotels.values()) {
            double score;
            try {
                score = calculateScore(hotel, now);
                hotel.setRanking(score);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    // Metodo per calcolare il punteggio totale di un hotel
    private double calculateScore(Hotel hotel,long now) throws ParseException {
        // Score del rate dell'hotel (punteggio medio)
        double rateScore = hotel.getRate();
        // Numero di recensioni dell'hotel
        int reviewNumber = hotel.getReviewNumber();
        // Influenza dell'attualità delle recensioni
        double reviewTimeScore = calculateReviewTimeScore(hotel, now) * recencyWeight;
        // Calcola il punteggio della qualità delle recensioni
        double qualityScore = rateScore * qualityWeight;
        // Calcola il punteggio della quantità delle recensioni
        double quantityScore = Math.log(reviewNumber + 1) * quantityWeight;
        // Combina i vari punteggi ponderati per ottenere il punteggio finale
        double totalScore = qualityScore + quantityScore + reviewTimeScore;
        // Assicurati che il punteggio non sia negativo
        return totalScore;
    }

    // Metodo per calcolare l'attualità delle recensioni
    private double calculateReviewTimeScore(Hotel hotel, long now) throws ParseException {
        // Se l'hotel non ha recensioni, il punteggio di attualità è 0
        if (hotel.getDates() == null || hotel.getDates().isEmpty()) {
            return 0;
        }

        // Calcolo la media delle date in millisecondi
        double averageDateScore = hotel.calculateAverageDate();
        // Calcola il tempo trascorso rispetto ad ora
        double diffTime = now - averageDateScore;
        // Calcola il peso dell'attualità usando il decadimento esponenziale
        double timeScore = Math.exp(-lambda * (diffTime / (1000 * 60 * 60 * 24)));
        // Restituisci il punteggio attualità finale
        return timeScore;
    }
}
