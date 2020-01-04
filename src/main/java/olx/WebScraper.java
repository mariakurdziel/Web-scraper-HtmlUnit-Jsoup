package olx;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
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
import java.util.Scanner;
import java.util.stream.Collectors;

public class WebScraper {

    private List<Offer> offers = new ArrayList<>();
    private WebClient client;
    private String startUrl;

    public WebScraper(String startUrl) {
        this.startUrl=startUrl;
        client = new WebClient();
        client.getOptions().setJavaScriptEnabled(true);
        client.setAjaxController(new NicelyResynchronizingAjaxController());
        client.getOptions().setCssEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);
    }

    public Offer parseOffer(Element el){
        Elements offerElement = el.getElementsByClass("marginright5 link linkWithHash detailsLink");
        String url = offerElement.attr("href");
        String title = offerElement.text();
        String salary = el.getElementsByClass("list-item__price").get(0).text();
        String location = el.getElementsByClass("breadcrumb x-normal").get(0).text();
        String timeOfWork = el.getElementsByClass("breadcrumb breadcrumb--job-type x-normal").get(0).text();
        String typeOfAgreement = el.getElementsByClass("breadcrumb breadcrumb--with-divider x-normal").get(0).text();

        return new Offer(title, location, timeOfWork, typeOfAgreement, salary, url);
    }

    private void writeToCsv() throws IOException {
        String filePath = "offers.csv";
        Writer writer = Files.newBufferedWriter(Paths.get(filePath), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        try (
                CSVWriter csvWriter = new CSVWriter(writer, CSVWriter.DEFAULT_SEPARATOR,
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END); )  {

            String[] headerRecord = {"Title", "Location", "Time of work", "Type of agreement", "Salary", "Article url"};
            csvWriter.writeNext(headerRecord);

            for (Offer o: this.offers) {
                csvWriter.writeNext(new String[]{ o.title, o.location, o.timeOfWork, o.typeOfAgreement, o.salary, o.articleUrl});
            }
        }
    }

    public void parsePages(List<String> links) throws IOException {
        Elements elements = new Elements();

        for (String l: links) {
            long startTime = System.nanoTime();
            HtmlPage page = client.getPage(l);
            long endTime = System.nanoTime();
            long duration = endTime - startTime;
            System.out.println(duration);
            Document doc = Jsoup.parse(page.asXml());
            elements.addAll(doc.getElementsByClass("offer-wrapper"));
        }
        for (Element el: elements) {
            offers.add(parseOffer(el));
        }
    }

    public List <String> getPagesLinks(HtmlPage searchPage) {
        List<HtmlAnchor> urlElements = searchPage.getByXPath("//*[@id=\"body-container\"]/div[3]/div/div[8]/span/a");
        urlElements.remove(urlElements.size()-1);
        List<String> links = new ArrayList<>();
        links.add(searchPage.getUrl().toString());

        for (HtmlAnchor url: urlElements) {
            links.add(url.getAttribute("href"));
        }
        return links;
    }

    public String chooseCategory(String category) {
        switch(category) {
            case "i":
                return "IT / telekomunikacja";
            case "b":
                return "Budowa / remonty";
            case "g":
                return "Gastronomia";
            default:
                return "Administracja biurowa";
        }
    }

    public String chooseTypeOfAgreement(String typeOfAgreement) {
        switch(typeOfAgreement) {
                case "a":
                    return "//*[@id=\"f-part_contract\"]";
                case "b":
                    return "//*[@ id = \"param_contract\"]/div/ul/li[3]/label[2]";
                default:
                    return "//*[@id=\"param_contract\"]/div/a/span[1]";
            }
    }

    public HtmlPage fillForm(String city, String category, String typeOfAgreement) throws IOException {
        String category_name = chooseCategory(category);
        String xpath = chooseTypeOfAgreement(typeOfAgreement);
        HtmlPage page = client.getPage(startUrl);
        HtmlSpan showAll = page.getFirstByXPath("//*[@id=\"topLinkShowAll\"]/span/span");
        showAll.click();
        DomElement categories = page.getElementById("topLink");
        DomNodeList<HtmlElement> links =categories.getElementsByTagName("a");
        HtmlAnchor link = (HtmlAnchor) links.stream().filter(l -> l.asText().equals(category_name))
                .collect(Collectors.toList()).get(0);
        HtmlPage categoryPage = client.getPage(link.getAttribute("href"));
        HtmlSpan  typeOfAgreementSpan = categoryPage.getFirstByXPath("//*[@id=\"param_contract\"]/div/a/span[1]");
        typeOfAgreementSpan.click();
        HtmlCheckBoxInput agreement = categoryPage.getFirstByXPath(xpath);
        agreement.click();
        HtmlTextInput cityField = (HtmlTextInput) categoryPage.getElementById("cityField");
        cityField.type(city);
        client.waitForBackgroundJavaScript(11000);
        HtmlListItem proposition = categoryPage.getFirstByXPath("//*[@id=\"autosuggest-geo-ul\"]/li[1]");
        proposition.click();
        DomElement searchButton = categoryPage.getElementById("search-submit");
        HtmlPage firstPage = searchButton.click();
        client.getOptions().setJavaScriptEnabled(false);

      return firstPage;
    }


    public void start() throws IOException {
        Scanner in = new Scanner(System.in);
        System.out.println("Wpisz nazwę miasta");
        String city = in.nextLine();
        System.out.println("Wybierz specjalność: i - IT, g - Gastronomia, b - Budownictwo, a - Administracja");
        String category = in.nextLine();
        System.out.println("Wybierz czas pracy: a - umowa o pracę, b - umowa o dzieło, c - umowa zlecenie");
        String type_of_agreement = in.nextLine();
        HtmlPage firstSearchPage = fillForm(city, category, type_of_agreement);
        List<String> pages = getPagesLinks(firstSearchPage);
        parsePages(pages);
        writeToCsv();
    }

}
