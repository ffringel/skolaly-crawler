package models;

import com.torunski.crawler.filter.ILinkFilter;
import com.torunski.crawler.util.ILinkExtractor;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashSet;

public class RSSLinkExtractor implements ILinkExtractor {
    private static final String[] START_TAGS = new String[]{"<link>"};
    private static final String[] END_TAGS = new String[]{"</link>"};
    @Override
    public Collection retrieveLinks(String url, String content, ILinkFilter linkFilter) {
        String pageLower = content.toLowerCase();

        Collection result = new HashSet();
        for (int i = 0; i < START_TAGS.length; i++) {
            String tag = START_TAGS[i];
            int pos = 0;
            while (pos < content.length()) {
                int begin = pageLower.indexOf(tag, pos);
                if (begin > -1) {
                    int end = pageLower.indexOf(END_TAGS[i], begin);
                    if (end > -1) {
                        String link = content.substring(begin + tag.length(), end);
                        if (link != null) {
                            link = StringUtils.deleteWhitespace(link);
                        }
                        if ((link != null) && ((linkFilter == null) || (linkFilter.accept(url, link)))) {
                            result.add(link);
                        }
                        pos = end + 1;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return result;
    }
}
