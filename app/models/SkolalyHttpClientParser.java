package models;

import com.torunski.crawler.filter.ILinkFilter;
import com.torunski.crawler.link.Link;
import com.torunski.crawler.parser.IParser;
import com.torunski.crawler.parser.PageData;
import com.torunski.crawler.parser.httpclient.AbstractHttpClient;
import com.torunski.crawler.parser.httpclient.HttpClientUtil;
import com.torunski.crawler.parser.httpclient.PageDataHttpClient;
import com.torunski.crawler.util.ILinkExtractor;
import com.torunski.crawler.util.LinksUtil;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;

/**
 * Re-implementation of {@link com.torunski.crawler.parser.httpclient.SimpleHttpClientParser} that:
 * <ul>
 *     <li>Follow redirect</li>
 *     <li>Allow to change the user agent</li>
 * </ul>
 *
 * @author Blaise Fringel
 */
public class SkolalyHttpClientParser extends AbstractHttpClient implements IParser {
    private static final transient Log LOG = LogFactory.getLog(SkolalyHttpClientParser.class);

    /** user agent HTTP header of the crawler. */
    public static final String USER_AGENT = "SmartAndSimpleWebCrawler/1.3 (https://crawler.dev.java.net)";
    //public static final String USER_AGENT = "SmartAndSimpleWebCrawler/2.0 (http://crawler.torunski.com)";

    /** set the default link extractor of LinksUtil. */
    private ILinkExtractor linkExtractor = LinksUtil.DEFAULT_LINK_EXTRACTOR;
    private String _userAgent = USER_AGENT;
    private boolean _followRedirect;

    /**
     * The constructor of SkolalyHttpClientParser for single HTTP connections.
     */
    public SkolalyHttpClientParser() {
        this(false);
    }

    /**
     * Create an instance of NjorkuHttpClientParser.
     *
     * @param multiThreaded
     *       true for creating a multi threaded connection manager else
     *       only a single connection is allowed
     */
    protected SkolalyHttpClientParser(boolean multiThreaded) {
        super(multiThreaded);
    }

    /**
     * Loads the data of the URI. A crawler can load different URIs at the same
     * time and parse them lately. Hence all necessary information have to be
     * stored in a PageData object. E.g. different threads can download the
     * content of the URI parallel and parse them in a different order.
     *
     * @param link the link of the page
     * @return the page data of the uri with a status code
     *
     * @see com.torunski.crawler.parser.IParser#load(com.torunski.crawler.link.Link)
     */
    @Override
    public PageData load(Link link) {
        // Download the content
        String uri = link.getURI();
        LOG.debug("download: " + uri);

        GetMethod httpGet;
        try {
            httpGet = new GetMethod(uri);
            httpGet.setFollowRedirects(true);
        } catch (Exception e) {
            LOG.info("HTTP get failed for " + uri);
            return new PageDataHttpClient(link, PageData.ERROR);
        }

        //Provide a custom retry handler
        httpGet.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(5, false));
        httpGet.setRequestHeader("User-Agent", USER_AGENT);

        if (link.getTimestamp() > 0) {
            httpGet.setRequestHeader(HttpClientUtil.HEADER_IF_MODIFIED_SINCE, DateUtil.formatDate(new Date(link.getTimestamp())));
        }

        int statusCode = 0;
        String responseBody = null;
        try {
            //Execute the method
            statusCode = client.executeMethod(httpGet);

            if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
                LOG.info("Content not modified since last request of " + uri);
            } else if (HttpClientUtil.isRedirect(statusCode)) {
                final URI redirect = HttpClientUtil.getRedirectURI(new URI(link.getURI(), false), httpGet);
                if (redirect != null) {
                    responseBody = redirect.getURI();
                    LOG.info("Redirect found for " + uri + " to" + redirect);
                } else {
                    statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                    LOG.warn("Invalid redirect for " + uri);
                }
            } else if (statusCode != HttpStatus.SC_OK) {
                LOG.info("Method failed: " + httpGet.getStatusLine() + " for " + uri);
                return new PageDataHttpClient(link, PageData.ERROR);
            } else if (!containsText(httpGet)) {
                LOG.warn("URL does not contain text or content-type is wrong of " + uri);
                httpGet.abort();
            } else {
                // read the response body as a stream
                responseBody = httpGet.getResponseBodyAsString();
                // don't overwrite the values of the given link object
                link = new Link(httpGet.getURI().getURI());
                link.setTimestamp(HttpClientUtil.getLastModified(httpGet));
            }
        } catch (IOException e) {
            responseBody = null;
            LOG.warn("Failed reading from uri=" + uri, e);
        } finally {
            // Release the connection.
            httpGet.releaseConnection();
        }

        if (HttpClientUtil.isRedirect(statusCode)) {
            PageDataHttpClient pageDataHttpClient = new PageDataHttpClient(link, PageData.REDIRECT);
            pageDataHttpClient.setData(responseBody);
            return pageDataHttpClient;
        } else if (responseBody != null) {
            return new PageDataHttpClient(link, responseBody, httpGet.getResponseCharSet());
        } else {
            if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
                return new PageDataHttpClient(link, PageData.NOT_MODIFIED);
            } else {
                return new PageDataHttpClient(link, PageData.ERROR);
            }
        }
    }

    private boolean containsText(HttpMethodBase methodBase) {
        Header contentType = methodBase.getRequestHeader("content-type");
        if (contentType != null) {
            HeaderElement[] elements = contentType.getElements();
            for (HeaderElement element : elements) {
                String value = element.getValue();
                if (value.startsWith("text") || value.startsWith("application/rss+xml")
                        || value.startsWith("application/xhtml+xml") || value.startsWith("application/xml")) {
                    return true;
                }
            }
            // if no correct content-type is found, so it isn't text
            return false;
        }
        // if no content type is set, it may any of the above
        return true;
    }

    @Override
    public Collection parse(PageData pageData, ILinkFilter linkFilter) {
        if (!(pageData instanceof PageDataHttpClient)) {
            LOG.warn("Type mismatch in " + this.getClass().getName());
            return Collections.EMPTY_LIST;
        }

        if (pageData.getStatus() == pageData.REDIRECT) {
            Collection links = new HashSet();
            if ((linkFilter == null) || (linkFilter.accept(pageData.getLink().getURI(), (String)pageData.getData()))) {
                links.add((String)pageData.getData());
            }
            return links;
        }
        return linkExtractor.retrieveLinks(pageData.getLink().getURI(), (String) pageData.getData(), linkFilter);
    }

    /**
     * @return the used link extractor.
     * @since 1.1
     */
    public ILinkExtractor getLinkExtractor() {
        return linkExtractor;
    }

    /**
     * Sets link extractor used to extract links from the retrieved content.
     *
     * @see com.torunski.crawler.util.ILinkExtractor
     * @param linkExtractor
     *            the link extractor used to extract links from the content.
     * @since 1.1
     */
    public void setLinkExtractor(ILinkExtractor linkExtractor) {
        if (linkExtractor == null) {
            throw new IllegalArgumentException("Parameter linkExtractor is null.");
        }
        this.linkExtractor = linkExtractor;
    }

    public void setUserAgent (String userAgent) {
        _userAgent = userAgent;
    }

    public String getUserAgent () {
        return _userAgent;
    }

    public void setFollowRedirect (boolean followRedirect) {
        _followRedirect = followRedirect;
    }

    public boolean isFollowRedirect () {
        return _followRedirect;
    }

}
