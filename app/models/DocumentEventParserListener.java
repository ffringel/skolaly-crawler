package models;

import com.torunski.crawler.events.IParserEventListener;
import com.torunski.crawler.events.ParserEvent;
import com.torunski.crawler.parser.PageData;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Properties;

public class DocumentEventParserListener implements IParserEventListener {
    private ScholarshipSite scholarshipSite;

    public DocumentEventParserListener(ScholarshipSite scholarshipSite) {
        this.scholarshipSite = scholarshipSite;
    }

    @Override
    public void parse(ParserEvent parserEvent) {
        Properties props = new Properties();
        props.put("metadata.broker.list", "172.31.5.81:9092, 172.31.5.82:9092");
        props.put("serializer.class", "kafka.serializer.StringEncoder");
        props.put("request.required.acks", "1");
        props.put("producer.type", "async");

        ProducerConfig config = new ProducerConfig(props);
        Producer<String, String> producer = new Producer<String, String>(config);

        if (parserEvent.getLink().getURI().startsWith(scholarshipSite.site_url + scholarshipSite.include_url)) {
            if (parserEvent.getPageData().getStatus() == PageData.OK) {

                String pageData = parserEvent.getPageData().getData().toString();
                String pageLink = parserEvent.getLink().getURI();

                Utility utility = new Utility();
                try {
                    Document doc = Jsoup.parse(pageData);

                    if (doc != null) {

                        String title = doc.select(scholarshipSite.titleTag).first().text();
                        String details = doc.select(scholarshipSite.detailsTag).first().text();
                        String publish_date = doc.select(scholarshipSite.published_date).first().text();
                        String md5 = utility.getMD5Hash(pageLink);

                        System.out.println(pageLink);
                        System.out.println(title);
                        System.out.println(details);
                        System.out.println(md5);
                        System.out.println(publish_date);


                        String msg = pageLink + "~" + title + "~" + details + "~" +
                                md5 + "~" + publish_date;
                        KeyedMessage<String, String> data = new KeyedMessage<String, String>("scholarship-feed", msg);
                        producer.send(data);
                    }
                } catch (NullPointerException e) {
                    e.getMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //producer.close();
    }
}
