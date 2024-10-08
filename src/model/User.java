package model;

public class User {
    //private int id;
    private String name;
    private String password;
    private String badge;
    private int reviewNumber;

    // costruttore della classe User
    public User(String name, String password, String badge, int reviewNumber) {
        setName(name);
        setPassword(password);
        setBadge(badge);
        setReviewNumber(reviewNumber);
    }


    public String getName() {return this.name;}
    public void setName(String name) {this.name = name;}

    public String getPassword() {return this.password;}
    public void setPassword(String password) {this.password = password;}

    public String getBadge() {return this.badge;}
    public void setBadge(String badge) {this.badge = badge;}

    public int getReviewNumber() {return this.reviewNumber;}
    public void setReviewNumber(int reviewNumber) {this.reviewNumber = reviewNumber;}

    public void updateReviewNumber() {
        this.setReviewNumber(reviewNumber+1);
        updateBadge();
    }

    public void updateBadge() {
        if(this.getReviewNumber() >= 3 && this.reviewNumber <= 5 ) this.setBadge("Recensore Esperto");
        else if(this.getReviewNumber() >= 6 && this.reviewNumber <= 8 ) this.setBadge("Contributore");
        else if(this.getReviewNumber() >= 9 && this.reviewNumber <= 11 ) this.setBadge("Contributore Esperto");
        else if(this.getReviewNumber() >= 12) this.setBadge("Contributore Super");
    }

    @Override
    public String toString() {
        return "name: " + name+", badge: "+badge+
                "Numero di recensioni: "+reviewNumber;
    }
}
