package onet;

import java.io.IOException;

public class MainClass {
    public static void main(String[] args) throws IOException, InterruptedException {
       String startUrl = "https://www.onet.pl/";
       WebScraper ws = new WebScraper(startUrl);
       ws.start();
    }
}