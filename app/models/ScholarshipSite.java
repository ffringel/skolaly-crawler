package models;

import com.torunski.crawler.Crawler;
import com.torunski.crawler.filter.ServerFilter;
import com.torunski.crawler.model.MaxIterationsModel;
import play.db.jpa.Model;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.List;

@Entity
@Table(name = "ScholarshipSite")
public class ScholarshipSite  extends Model implements Runnable {

    public String site_url;

    public String start;

    public String titleTag;

    public String detailsTag;

    public String published_date;

    public String include_url;

    public ScholarshipSite(String site_url, String start, String titleTag, String detailsTag, String published_date, String include_url) {
        this.site_url = site_url;
        this.start = start;
        this.titleTag= titleTag;
        this.detailsTag = detailsTag;
        this.published_date = published_date;
        this.include_url = include_url;
    }

    public void crawl() {
        try {
            String server = this.site_url;
            String start = this.start;

            Crawler crawler = new Crawler();
            SkolalyHttpClientParser parser = new SkolalyHttpClientParser(true);
            parser.setLinkExtractor(new SkolalyLinkExtractorImpl());
            parser.setFollowRedirect(true);

            crawler.setLinkFilter(new ServerFilter(server));
            crawler.setModel(new MaxIterationsModel(1000));
            crawler.addParserListener(new DocumentEventParserListener(this));
            crawler.setParser(parser);

            crawler.start(server, start);
        } catch (NoSuchMethodError error) {
            error.getCause();
        }
    }

    public static List<ScholarshipSite> getAll() {
        return ScholarshipSite.all().fetch();
    }

    @Override
    public void run() {
       this.crawl();
    }
}
