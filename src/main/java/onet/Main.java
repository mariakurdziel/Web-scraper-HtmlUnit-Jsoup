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
import otomoto.Annoucement;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class Main {

    private static void writeToCsv(List <Article> articles) throws IOException {
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

    public static void scrapeImage(String url, String name) {
        try(InputStream in = new URL("http:"+url).openStream()){
            Files.copy(in, Paths.get("C:/Users/maria/IdeaProjects/ws/src/main/java/onet/images/"+name));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static String generateImageName() {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(10);

        for (int i = 0; i < 10; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString()+".jpg";
    }

    public static String getCategoryUrl(WebDriver driver) {
        List <WebElement> categories = driver.findElements(By.xpath("//ul[@class='mainMenu']/li/a"));
        categories.removeIf(item -> item.getText().isEmpty());
        return categories.get(0).getAttribute("href");
    }

    public static List <String> getNewsLinks(WebDriver driver,String url, int limit) throws IOException, InterruptedException {
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

    public static List <Article> getArticles(List <String> links, WebDriver driver) {
        List<Article> articles = new ArrayList<>();

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
                //scrapeImage(imgUrl,imageName);
            } else {
                imageName="Not Found";
            }
            System.out.println(imageName);
            Article a = new Article(title[0],desc,text,imageName);
            articles.add(a);
        }
        return articles;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
       String startUrl = "https://www.onet.pl/";
       System.setProperty("webdriver.chrome.driver","C:/chromedriver.exe");
       WebDriver driver = new ChromeDriver();
       driver.get(startUrl);
       String categoryUrl = getCategoryUrl(driver);
       List <String> newsLinks = getNewsLinks(driver,categoryUrl, 50);
       List <Article> articles = getArticles(newsLinks, driver);
       writeToCsv(articles);

    }
}