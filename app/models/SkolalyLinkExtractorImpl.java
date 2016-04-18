package models;

import com.torunski.crawler.filter.ILinkFilter;
import com.torunski.crawler.util.ILinkExtractor;
import com.torunski.crawler.util.LinksUtil;

import java.util.Collection;

/**
 * Created by fabulous on 4/24/15.
 */
public class SkolalyLinkExtractorImpl implements ILinkExtractor {

    private static final ILinkExtractor RSS = new RSSLinkExtractor();
    private static final ILinkExtractor HTML = LinksUtil.DEFAULT_LINK_EXTRACTOR;
    @Override
    public Collection retrieveLinks(String url, String content, ILinkFilter iLinkFilter) {
        if (content.contains("<rss") || content.contains("<rdf:RDF")) {
            return RSS.retrieveLinks(url, content, iLinkFilter);
        } else {
            return HTML.retrieveLinks(url, content, iLinkFilter);
        }
    }
}
