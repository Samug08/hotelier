package model;



import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;


public class Hotel {
    private int id;
    private String name;
    private String description;
    private String city;
    private String phone;
    private String[] services;
    private int rate;
    private Rating ratings;
    private int reviewNumber;
    private ConcurrentLinkedQueue<String> dates;
    private double ranking;

    // costruttore della classe Hotel
    public Hotel(int id, String name, String description, String city, String phone, String[] services, int rate, Rating ratings, int reviewNumber, ConcurrentLinkedQueue<String> dates) {
        setId(id);
        setName(name);
        setDescription(description);
        setCity(city);
        setPhone(phone);
        setServices(services);
        setRate(rate);
        setRatings(ratings);
        setReviewNumber(reviewNumber);
    }

    // getter e setter per tutte le variabili della classe Hotel
    public int getId() {return id;}
    public void setId(int id) {this.id = id;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    public String getCity() {return city;}
    public void setCity(String city) {this.city = city;}
    public String getPhone() {return phone;}
    public void setPhone(String phone) {this.phone = phone;}
    public String[] getServices() {return services;}
    public void setServices(String[] services) {this.services = services;}
    public int getRate() {return rate;}
    public void setRate(int rate) {this.rate = rate;}
    public Rating getRatings() {return ratings;}
    public void setRatings(Rating ratings) {this.ratings = ratings;}
    public int getReviewNumber() {return reviewNumber;}
    public void setReviewNumber(int reviewNumber) {this.reviewNumber = reviewNumber;}
    public ConcurrentLinkedQueue<String> getDates() {return dates;}
    public void setDates(ConcurrentLinkedQueue<String> dates) {this.dates = dates;}
    public double getRanking() {return ranking;}
    public void setRanking(double ranking) {this.ranking = ranking;}

    // metodo per ottenere la data di ora
    public String now() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
        String formattedNow = now.format(formatter);
        return formattedNow;
    }

    // metodo per aggiungere la data della recensione
    public void addDates() {
        // aggiorno la data della recensione
        if(getDates() == null) {
            dates = new ConcurrentLinkedQueue<>();
        }
        String date = this.now();
        dates.add(date);
    }

    // metodo per calcolare la media delle date delle recensioni
    public double calculateAverageDate() throws ParseException {
        if(dates == null || dates.isEmpty()) {return 0;}
        long sum = 0;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
        for (String d: dates) {
            // converto la data in un numero in millisecondi
            Date date = formatter.parse(d);
            sum += date.getTime();
        }
        // calcola la media
        double average = (double) sum/dates.size();
        return average;
    }

    // metodo per aggiornare il rate di un hotel
    public void updateRate(int rate) {
        // se la struttura non ha ancora recensioni, allora aggiorno il rate
        if (this.getReviewNumber() == 0) {
            this.setRate(rate);
            return;
        }
        // aggiorno il rate totale della struttura in base al numero di recensioni esistenti
        int oldScore = this.getRate();
        int totalReviews = this.getReviewNumber();
        int newScore = Math.round(((oldScore * totalReviews) + rate) / (totalReviews + 1));
        this.setRate(newScore);
    }

    // metodo per aggiornare i ratings di un hotel
    public void updateRatings(int[] ratings) {
        // Numero totale di recensioni
        int totalReviews = this.getReviewNumber();
        if(totalReviews == 0) {
            this.ratings.setCleaning(ratings[0]);
            this.ratings.setPosition(ratings[1]);
            this.ratings.setServices(ratings[2]);
            this.ratings.setQuality(ratings[3]);
            return;
        }
        // update cleaning
        int oldCleaning = this.ratings.getCleaning();
        int newCleaning = Math.round(((oldCleaning * totalReviews) + ratings[0]) / (totalReviews + 1));
        this.ratings.setCleaning(newCleaning);
        // update position
        int oldPosition = this.ratings.getPosition();
        int newPosition = Math.round(((oldPosition * totalReviews) + ratings[1]) / (totalReviews + 1));
        this.ratings.setPosition(newPosition);
        // update services
        int oldServices = this.ratings.getServices();
        int newServices = Math.round(((oldServices * totalReviews) + ratings[2]) / (totalReviews + 1));
        this.ratings.setServices(newServices);
        // update quality
        int oldQuality = this.ratings.getQuality();
        int newQuality = Math.round(((oldQuality * totalReviews) + ratings[3]) / (totalReviews + 1));
        this.ratings.setQuality(newQuality);
    }

    // metodo per aggiornare il numero di recensioni di un hotel
    public void updateNumberReviews() {
        this.setReviewNumber(this.getReviewNumber() + 1);
    }

    // toString per la visualizzazione di un hotel
    @Override
    public String toString() {
        return "Id: " + id +
                "\nName: " + name +
                "\nDescription: " + description +
                "\nCity: " + city +
                "\nPhone: " + phone +
                "\nServices: " + String.join(", ", services) +
                "\nRate: " + rate +
                "\nRatings: " + ratings.toString() +
                "\nRanking: " + ranking;
    }
}
