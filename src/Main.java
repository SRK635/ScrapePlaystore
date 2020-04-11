import com.opencsv.CSVWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Main {
    public static class App {
        String name;
        long reviews;
        String last;

        private long getDays() throws ParseException {
            Date date = new SimpleDateFormat("MMMMM dd, yyyy").parse(last);
            Date dateNow = new Date();
            return ((dateNow.getTime() - date.getTime()) / 86400000);
        }

        private long getScore() throws ParseException {
            try {
                return reviews / getDays();
            } catch (ArithmeticException zero) {
                //TODO handle divide by zero exception
                return reviews;
            }
        }

        JSONObject getJSON() throws ParseException {
            JSONObject object = new JSONObject();
            object.put("name", name);
            object.put("reviews", reviews);
            object.put("last-updated", last);
            object.put("last-updated-in-days", getDays());
            object.put("score", getScore());
            return object;
        }

        String[] getCSVObject() throws ParseException {
            return new String[]{name,
                    String.valueOf(reviews),
                    last,
                    String.valueOf(getDays()),
                    String.valueOf(getScore())};
        }
    }

    public static void main(String[] args) throws IOException {
        ArrayList<String> allApps = getAllLinks();
        ArrayList<App> appDetails = getAppDetails(allApps);
        getJSON(appDetails);
        getCSV(appDetails);
    }

    private static void getJSON(ArrayList<App> apps) throws IOException {
        JSONObject json = new JSONObject();
        json.put("count", apps.size());
        JSONArray array = new JSONArray();
        for (App e : apps) {
            try {
                array.add(e.getJSON());
            } catch (ParseException ignore) {
            }
        }
        json.put("data", array);
        FileWriter writer = new FileWriter("json-output.txt");
        writer.write(json.toJSONString());
        writer.close();
    }

    private static void getCSV(ArrayList<App> apps) throws IOException {
        FileWriter output = new FileWriter("csv-output.csv");
        CSVWriter writer = new CSVWriter(output);
        String[] header = {"NAME", "REVIEWS", "LAST-UPDATED", "LAST UPDATED IN DAYS", "SCORE"};
        writer.writeNext(header);
        for (App e : apps) {
            try {
                writer.writeNext(e.getCSVObject());
            } catch (ParseException ignore) {
            }
        }
        writer.close();
    }

    private static ArrayList<App> getAppDetails(ArrayList<String> allApps) {
        ArrayList<App> data = new ArrayList<>();
        for (String appLink : allApps) {
            try {
                App app = new App();
                Document doc = Jsoup.connect(appLink).get();
                Element name = doc.getElementsByAttributeValue("itemprop", "name").first();
                app.name = name.text();
                app.reviews = Long.parseLong(doc.getElementsByAttributeValueEnding("aria-label", "ratings").first().text().replace(",", ""));
                Elements updatedList = doc.getElementsContainingOwnText("Updated");
                Element updated = null;
                for (Element e : updatedList) {
                    if (e.attr("class").equals("BgcNfc"))
                        updated = e.parent();
                }
                app.last = updated.getElementsByAttributeValueStarting("class", "htlgb").first().text();
                data.add(app);
            } catch (NullPointerException | IOException ignore) {

            }
        }
        return data;
    }

    private static ArrayList<String> getAllLinks() throws IOException {
        Document doc = Jsoup.connect("https://play.google.com/store/apps/top").get();
        Elements ahref = doc.select("a[href]");
        ArrayList<String> allApps = new ArrayList<>();
        for (Element e : ahref) {
            String link = e.attr("href");
            if (link.startsWith("/store/apps/details?")) {
                String fullLink = "https://play.google.com" + link;
                if (!allApps.contains(fullLink))
                    allApps.add("https://play.google.com" + link);
            }
        }
        return allApps;
    }
}
