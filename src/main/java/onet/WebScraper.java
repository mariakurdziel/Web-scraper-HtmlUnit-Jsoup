package onet;

import com.opencsv.CSVWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class WebScraper {

    private List <Article> articles = new ArrayList<>();
    private WebDriver driver;

    public WebScraper(String startUrl) {
        System.setProperty("webdriver.chrome.driver","C:/chromedriver.exe");
        this.driver = new ChromeDriver();
        driver.get(startUrl);
    }

    private void writeToCsv() throws IOException {
        String filePath = "articles.csv";
        Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        try (
                CSVWriter csvWriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END); )  {

            String[] headerRecord = {"Title", "Description", "Text", "Image Name"};
            csvWriter.writeNext(headerRecord);

            for (Article a: articles) {
                csvWriter.writeNext(new String[]{ a.title, a.description, a.text, a.imageName});
            }
        }
    }

    private void scrapeImage(String url, String name) {
        try(InputStream in = new URL("http:"+url).openStream()){
            Files.copy(in, Paths.get("C:/Users/maria/IdeaProjects/ws/src/main/java/onet/images/"+name));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateImageName() {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(10);

        for (int i = 0; i < 10; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString()+".jpg";
    }

    private String chooseCategory(String category) {
        switch(category){
            case "s":
                return "SPORT";
            case "k" :
                 return "KULTURA";
            default:
                return "WIADOMOŚCI";
        }
    }

    private String getCategoryUrl(String category) {
        String categoryUrl="";
        String categoryName = chooseCategory(category);
        List <WebElement> categories = driver.findElements(By.xpath("//ul[@class='mainMenu']/li/a"));
        categories.removeIf(item -> item.getText().isEmpty());
        for(WebElement c: categories) {
            if(c.getText().equals(categoryName)) {
                categoryUrl=c.getAttribute("href");
                break;
            }
        }
        return categoryUrl;
    }

    private List <String> getNewsLinks(String url, int limit) throws InterruptedException {
        List <WebElement> articles = new ArrayList<>();
        List <String> links = new ArrayList<>();
        driver.get(url);
        while(articles.size() < limit) {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
            sleep(300);
            articles = driver.findElements(By.xpath("//div[contains(@class, 'items solrList ')]/a"));
        }
        articles = articles.subList(0,limit);

        for(WebElement a: articles) {
            links.add(a.getAttribute("href"));
        }
        return links;
    }

    private List <Article> getArticles(List <String> links) {
        for(String l: links) {
            driver.get(l);
            String page = driver.getPageSource();
            Document doc = Jsoup.parse(page);
            String titles = doc.getElementsByTag("title").text();
            String title[] = titles.split("\n");
            Elements description = doc.getElementsByAttributeValue("name","description");
            String desc = description.get(0).attr("content");
            String text = "";
            Elements paragraphs= doc.getElementsByClass("hyphenate ");
            for (Element p: paragraphs) {
                text+=p.text()+"\n";
            }
            Elements picture = doc.getElementsByTag("picture");
            String imageName;
            if(picture.size()>0) {
                String imgUrl = picture.get(0).getElementsByTag("img").attr("src");
                String url[] = imgUrl.split("/");
                imageName = url[url.length-1];
                if(imageName.equals(".jpg")) {
                    imageName = generateImageName();
                }
                scrapeImage(imgUrl,imageName);
            } else {
                imageName="Not Found";
            }
            Article a = new Article(title[0],desc,text,imageName);
            articles.add(a);
        }
        return articles;
    }

    public void start() throws IOException, InterruptedException {
        Scanner in = new Scanner(System.in);
        System.out.println("Wybierz kategorię artykułów: w - wiadomości, k - kultura, s-sport");
        String category = in.nextLine();
        System.out.println("Wybierz ilość artykułów");
        int limit = in.nextInt();

        String categoryUrl = getCategoryUrl(category);
        List<String> newsLinks = getNewsLinks(categoryUrl, limit);
        getArticles(newsLinks);
        writeToCsv();
    }
}
