package olx;

public class Offer {
    public String title;
    public String location;
    public String timeOfWork;
    public String typeOfAgreement;
    public String salary;
    public String articleUrl;

    public Offer(String title, String location, String timeOfWork, String typeOfAgreement, String salary, String articleUrl) {
        this.title = title;
        this.location = location;
        this.timeOfWork = timeOfWork;
        this.typeOfAgreement = typeOfAgreement;
        this.salary = salary;
        this.articleUrl = articleUrl;
    }
}
