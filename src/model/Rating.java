package model;

public class Rating {
    private int cleaning;
    private int position;
    private int services;
    private int quality;

    // costruttore della classe Rating
    public Rating(int cleaning, int position, int services, int quality) {
        setCleaning(cleaning);
        setPosition(position);
        setServices(services);
        setQuality(quality);
    }

    // getter e setter per tutte le variabili della classe Rating
    public int getCleaning() {return cleaning;}
    public void setCleaning(int cleaning) {this.cleaning = cleaning;}

    public int getPosition() {return position;}
    public void setPosition(int position) {this.position = position;}

    public int getServices() {return services;}
    public void setServices(int services) {this.services = services;}

    public int getQuality() {return quality;}
    public void setQuality(int quality) {this.quality = quality;}

    @Override
    public String toString() {
        return "[ cleaning: " + cleaning +
                ", position: " + position +
                ", services: " + services +
                ", quality: " + quality + " ]";
    }
}
