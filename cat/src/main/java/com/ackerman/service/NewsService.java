package com.ackerman.service;

import com.ackerman._thrid.UserModel;
import com.ackerman.dao.NewsDao;
import com.ackerman.model.News;
import com.ackerman.utils.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午9:50 18-6-16
 */
@Service
public class NewsService implements InitializingBean{
    private Logger logger = LoggerFactory.getLogger(NewsService.class);

    @Autowired
    private MasterUtil masterUtil;

    @Autowired
    private JedisUtil jedisUtil;

    @Autowired
    private JedisClusterUtil jedisClusterUtil;

    @Autowired
    private NewsDao newsDao;



    @Autowired
    private LocalInfo localInfo;

    public List<News> getNewsByOffsetAndLimit(int offset, int limit){
        return newsDao.getNewsByOffsetAndLimit(offset, limit);
    }

    public News getNewsById(int id){
        return newsDao.getNewsById(id);
    }

    public News addNews(int userId, String title, String content, String imageUrl){
        try{
            News news = new News();
            news.setUserId(userId);
            news.setTitle(title);
            news.setContent(content);
            news.setCreateDate(new Date());
            if(imageUrl.equals("")){
                //设置默认图片
                news.setImageLink("http://oz15aje2y.bkt.clouddn.com/b402d0f985e64470a33a954ee400cb17.jpg");
            }else {
                news.setImageLink(imageUrl);
            }
            newsDao.addNews(news);

            return news;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @Description: 进程一启动, 先设置hotNews
     * @Date: 上午10:59 18-6-17
     */
    public void initHotNews(){
        try {
            jedisUtil.del(JedisUtil.HOT_NEWS_KEY);
            List<Integer> list = updateHotNewsId();
            for(int id : list){
                jedisUtil.rpush(JedisUtil.HOT_NEWS_KEY, String.valueOf(id));
            }
        }catch (Exception e){
            logger.error("设置启动hotNes失败", e);
        }
    }

    /**
     * @Description: 计算出热门新闻, 这里计算前10000条的新闻, 之后改进的, 可以计算一段时间以内
     *                  返回新闻的id编号, 分值大的放前面
     *              并且更新到mysql数据库
     * @Date: 上午9:38 18-6-17
     */
    public List<Integer> updateHotNewsId(){

        //分值从大到小
        PriorityQueue<News> queue = new PriorityQueue<>(new Comparator<News>(){
            @Override
            public int compare(News o1, News o2){
                if(o1.getScore() == o2.getScore())
                    return 0;
                return o1.getScore() < o2.getScore() ? 1 : -1;
            }
        });

        //TODO 每次选取一个星期内, 或者最新1000条新闻

        int limitNum = 1000;
        List<News> lastNews = newsDao.getLastNews(limitNum);
        for(News news : lastNews){
            //更新点赞数
            long likeCount = getNewsLikeCount(news.getId());
            news.setLikeCount(Integer.valueOf(String.valueOf(likeCount)));

            //更新新闻的分值. 并排序
            caculateScore(news);
            queue.add(news);
        }

        ArrayList<Integer> res = new ArrayList<>();
        while(!queue.isEmpty()){
            News news = queue.poll();
            res.add(news.getId());
        }

        return res;
    }

    //点赞数, 评论数越多, 创建时间越短, 分值越高
    public void caculateScore(News news){
        try{
            long likeScore = getNewsLikeCount(news.getId())*7;
            long commentScore = news.getCommentCount()*11;
            long createTime = (System.currentTimeMillis() - news.getCreateDate().getTime()) / (1000 * 60);
            createTime = createTime == 0 ? 1 : createTime;
            long score = (likeScore + commentScore) / createTime;
            news.setScore(score);
        }catch (Exception e){
            logger.error("计算分值错误", e);
        }
    }

    /**
     * @Description: attitude 1:喜欢   -1:不喜欢
     * @Date: 下午3:01 18-6-17
     */
    public long updateAttitudeOnNews(int newsId, int attitude){
        UserModel user = localInfo.getUser();
        String userId = String.valueOf(user.getId());
        String likeKey = Entity.getNewsAttitudeKey(newsId, Entity.LKIE_KEY);
        String dislikeKey = Entity.getNewsAttitudeKey(newsId, Entity.DISLKE_KEY);

        if(attitude == 1){
            if(jedisClusterUtil._sismember(dislikeKey, userId))
                jedisClusterUtil._srem(dislikeKey, userId);

            if(jedisClusterUtil._sismember(likeKey, userId))
                jedisClusterUtil._srem(likeKey, userId);
            else
                jedisClusterUtil._sadd(likeKey, userId);
        }
        else if(attitude == -1){
            if(jedisClusterUtil._sismember(likeKey, userId))
                jedisClusterUtil._srem(likeKey, userId);

            if(jedisClusterUtil._sismember(dislikeKey, userId))
                jedisClusterUtil._srem(dislikeKey, userId);
            else
                jedisClusterUtil._sadd(dislikeKey, userId);
        }

        return jedisClusterUtil._scard(likeKey) - jedisClusterUtil._scard(dislikeKey);
    }


    public long getNewsLikeCount(int newsId){
        String likeKey = Entity.getNewsAttitudeKey(newsId, Entity.LKIE_KEY);
        String dislikeKey = Entity.getNewsAttitudeKey(newsId, Entity.DISLKE_KEY);

        return jedisClusterUtil._scard(likeKey) - jedisClusterUtil._scard(dislikeKey);
    }





    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(new Runnable() {
            @Override
            public void run() {
                initHotNews();  //hot-news集合是单机的
                //like-key和dislike-key是集群的
                masterUtil.start();

                while (true){
                    long sleepMills = masterUtil.waitForMillseconds();
//                    System.out.println("睡眠时间: " + sleepMills);
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleepMills);

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    String nextPath = masterUtil.createNextMasterNodePath();
                    if(masterUtil.createNextMasterNode(nextPath)){
                        String lastPath = masterUtil.getLastMasterNodePath();
                        masterUtil.deleteLastMasterNode(lastPath);

                        List<Integer> list = updateHotNewsId();
                        StringBuilder sb = new StringBuilder();
                        for(int i = 0; i < list.size(); ++i){
                            if(i == list.size()-1)
                                sb.append(list.get(i));
                            else
                                sb.append(list.get(i) + ",");
                        }
                        masterUtil.setData(nextPath, sb.toString().getBytes());

                        System.out.println("----------------------------------------------------");
                        System.out.println("设置节点数据成功:" + sb.toString());
                        System.out.println("----------------------------------------------------");
                    }
                    else{
                        try{
                            //等待master设置完数据
                            TimeUnit.SECONDS.sleep(10);
                        }catch (Exception e){}

                        byte []data = masterUtil.fetchData(nextPath);
                        System.out.println("----------------------------------------------------");
                        System.out.println("从节点中获取数据:" + new String(data));
                        System.out.println("----------------------------------------------------");

                        jedisUtil.del(JedisUtil.HOT_NEWS_KEY);
                        String []ids = (new String(data)).split(",");
                        for(String id : ids){
                            jedisUtil.rpush(JedisUtil.HOT_NEWS_KEY, id);
                        }
                    }

                }
            }
        }).start();
    }
}