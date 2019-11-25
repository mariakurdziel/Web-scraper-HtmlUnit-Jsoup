import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.opencsv.CSVWriter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class WebScraper {

    private WebClient client;
    private String startUrl;
    private String baseUrl;
    private List<String> pageUrls = new ArrayList<String>();
    private List<Article> articles = new ArrayList<Article>();

    public WebScraper(String startUrl) {
        this.startUrl  = startUrl;
        client = new WebClient();
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
    }

    private void writeToCsv() throws IOException {
        String filePath = "cars.csv";
        Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        try (
        CSVWriter csvWriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END); )  {

            String[] headerRecord = {"Model Name", "Production Year", "Mileage", "Engine Capacity", "Price", "Currency", "City", "Region"};
            csvWriter.writeNext(headerRecord);

            for (Article a: this.articles) {
                csvWriter.writeNext(new String[]{ a.modelName, a.productionYear, a.mileage, a.engineCapacity, a.price, a.currency, a.city, a.region});
            }
        }
    }

    private void parseArticle(Element element) {
        String article = element.toString();
        Document doc = Jsoup.parse(article);
        String modelName = doc.getElementsByClass("offer-title ds-title").get(0).text();
        Elements generalInfos = doc.getElementsByClass("ds-param");
        String productionYear = generalInfos.get(0).text();
        String mileage = generalInfos.get(1).text();
        String engineCapacity = generalInfos.get(2).text();
        String price = doc.getElementsByClass("offer-price__number ds-price-number").get(0).text();
        String currency = doc.getElementsByClass("offer-price__currency ds-price-currency").get(0).text();
        String city = doc.getElementsByClass("ds-location-city").get(0).text();
        String region = doc.getElementsByClass("ds-location-region").get(0).text();
        this.articles.add(new Article(modelName, productionYear, mileage, engineCapacity, price, currency, city, region));
    }

    private void parsePage() throws IOException {
        Elements list_of_articles = new Elements();

        for (String url: this.pageUrls) {
            HtmlPage currentPage = client.getPage(url);
            String htmlPage = currentPage.asXml();
            Document doc = Jsoup.parse(htmlPage);
            Elements articles  = doc.select("article");
            list_of_articles.addAll(articles);
        }

        for (Element article: list_of_articles) {
            parseArticle(article);
        }
    }

    private void initializeListOfUrls(int numberOfPages) {
        for(int i = 2; i< numberOfPages+1; i++) {
            String pageUrl = this.baseUrl + new Integer(i).toString();
            this.pageUrls.add(pageUrl);
        }
    }

    private int extractSiteProperties(HtmlPage page) {
        DomAttr site_links = page.getFirstByXPath("//ul[@class = 'om-pager rel']/li/a/@href");
        this.baseUrl =   site_links.getNodeValue().substring(0, site_links.getNodeValue().lastIndexOf("=")+1);
        HtmlSpan lastPage = page.getFirstByXPath("//ul[@class = 'om-pager rel']/li[6]/a/span");
        return new Integer(lastPage.asText());
    }

    private HtmlPage fillForm() throws IOException {
        HtmlPage page = this.client.getPage(this.startUrl);
        HtmlAnchor category = page.getFirstByXPath("//ul[@class='category-tabs']/li/a");
        category.click();
        HtmlButton button = page.getFirstByXPath("//*[@id=\"searchmain_29\"]/button[1]");
        HtmlPage search_page = button.click();
        HtmlSelect carBrand = (HtmlSelect) search_page.getElementById("param571");
        HtmlOption brand_option = carBrand.getOptionByValue("opel");
        carBrand.setSelectedAttribute(brand_option, true);
        HtmlInput priceFrom = search_page.getElementByName("search[filter_float_price:from]");
        priceFrom.setValueAttribute("2000");
        HtmlInput priceTo = search_page.getElementByName("search[filter_float_price:to]");
        priceTo.setValueAttribute("20000");
        HtmlInput yearFrom = search_page.getElementByName("search[filter_float_year:from]");
        yearFrom.setValueAttribute("2009");
        HtmlInput yearTo = search_page.getElementByName("search[filter_float_year:to]");
        yearTo.setValueAttribute("2019");
        HtmlInput mileageFrom = search_page.getElementByName("search[filter_float_mileage:from]");
        mileageFrom.setValueAttribute("75000");
        HtmlInput mileageTo = search_page.getElementByName("search[filter_float_mileage:to]");
        mileageTo.setValueAttribute("150000");
        HtmlSelect fuelType = (HtmlSelect) search_page.getElementById("param581");
        HtmlOption fuel_option = fuelType.getOptionByText("Benzyna");
        fuelType.setSelectedAttribute(fuel_option, true);
        HtmlButton search_button = (HtmlButton) search_page.getElementById("submit-filters");
        HtmlPage first_page = search_button.click();
        this.pageUrls.add(first_page.getUrl().toString());
        return first_page;
    }

    public void start() throws IOException {
        HtmlPage firstPage = this.fillForm();
        int numberOfPages = extractSiteProperties(firstPage);
        this.initializeListOfUrls(numberOfPages);
        this.parsePage();
        this.writeToCsv();
    }


}
