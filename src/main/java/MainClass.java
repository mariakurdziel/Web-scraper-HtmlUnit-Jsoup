import java.io.IOException;

public class MainClass {
    public static void main(String[] args) throws IOException {
        String startUrl = "https://www.otomoto.pl/";
        WebScraper ws = new WebScraper(startUrl);
        ws.start();
    }
}
