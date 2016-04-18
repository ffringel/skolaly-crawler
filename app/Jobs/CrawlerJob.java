package Jobs;

import models.ScholarshipSite;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Every("3h")
@OnApplicationStart
public class CrawlerJob extends Job {

    public void doJob() {
        int corePoolSize = 15;
        int maxPoolSize = 15;
        long keepAliveTime = 300000;

        List<ScholarshipSite> feeds = ScholarshipSite.getAll();

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(feeds));
        try {
            for (ScholarshipSite feed : feeds) {
                threadPoolExecutor.execute(feed);
            }
            threadPoolExecutor.shutdown();
        } catch (RejectedExecutionException e) {
            //Thread.sleep(100);
        }
    }
}
