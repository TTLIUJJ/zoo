package com.ackerman.controller;

import com.ackerman._thrid.*;
import com.ackerman.model.Comment;
import com.ackerman.model.News;
import com.ackerman.service.CommentService;
import com.ackerman.service.NewsService;
import com.ackerman.service.QiniuService;
import com.ackerman.service.UserService;
import com.ackerman.utils.JedisUtil;
import com.ackerman.utils.JsonUtil;
import com.ackerman.utils.LocalInfo;
import com.ackerman.utils.ViewObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 下午11:18 18-6-16
 */
@Controller
public class NewsController {
    private static Logger logger = LoggerFactory.getLogger(NewsController.class);

    @Autowired
    private NewsService newsService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private QiniuService qiniuService;

    @Autowired
    private JedisUtil jedisUtil;

    @Autowired
    private LocalInfo localInfo;

    @Autowired
    private JsonUtil jsonUtil;


    @RequestMapping(path = {"/", "/index" }, method = {RequestMethod.GET, RequestMethod.POST})
    public String index(@RequestParam(value = "offset", defaultValue = "0") int offset,
                        @RequestParam(value = "limit", defaultValue = "10") int limit,
                        Model model){
        try{
            List<ViewObject> vos = new ArrayList<>(limit);
            List<News> newsList = newsService.getNewsByOffsetAndLimit(0, limit);
            for(int i = 0; i < newsList.size(); ++i){
                News news = newsList.get(i);
                ViewObject vo = new ViewObject();

                vo.set("index", i+1+offset);
                vo.set("news", news);
                vo.set("author", userService.getUserFromId(news.getUserId()));
                vo.set("likeCount", newsService.getNewsLikeCount(news.getId()));
                vos.add(vo);
            }
            model.addAttribute("vos", vos);
        }catch (Exception e){
            e.printStackTrace();
        }

        return "index";
    }

    @RequestMapping(path = "/hot", method = RequestMethod.GET)
    public String hot(@RequestParam(value = "offset", defaultValue = "0") int offset,
                      @RequestParam(value = "limit", defaultValue = "10") int limit,
                      Model model){
        try{
            List<ViewObject> vos = new ArrayList<>(limit);
            long len = jedisUtil.llen(JedisUtil.HOT_NEWS_KEY);

            for(long i = 0; i < len; ++i){
                ViewObject vo = new ViewObject();

                String newsId = jedisUtil.lindex(JedisUtil.HOT_NEWS_KEY, i);
                News news = newsService.getNewsById(Integer.valueOf(newsId));
                vo.set("news", news);
                vo.set("author", userService.getUserFromId(news.getUserId()));
                vo.set("index", i+1+offset);
                vo.set("likeCount", newsService.getNewsLikeCount(news.getId()));

                vos.add(vo);
            }
            model.addAttribute("vos", vos);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "index";
    }


    @RequestMapping(path = "/newsAttitude", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public String setUserAttitudeOnNews(@RequestParam("newsId") int newsId,
                                        @RequestParam("attitude") int attitude){

        long likeCount = 0;
        UserModel user = localInfo.getUser();
        if(user == null)
            return jsonUtil.getJsonString(1, "登录后才能点击");

        try{
            likeCount = newsService.updateAttitudeOnNews(newsId, attitude);
        }catch (Exception e){
            logger.error("点赞点踩异常", e);
            return jsonUtil.getJsonString(1, "系统异常, 稍后再试");
        }

        return jsonUtil.getJsonString(0, String.valueOf(likeCount));
    }


    @RequestMapping(path = "/newsDisplay", method = RequestMethod.GET)
    public String newsDisplay(@RequestParam("newsId") int newsId,
                              Model model){
        try{
            News news = newsService.getNewsById(newsId);
            if(news == null){
                return "redirect:/";
            }
            List<Comment> comments = commentService.getCommentListByUserId(newsId);
            List<ViewObject> vos = new ArrayList<>();
            for(Comment comment : comments){
                ViewObject vo = new ViewObject();

                UserModel user = userService.getUserFromId(comment.getUserId());

                vo.set("comment", comment);
                vo.set("commenter", user);
                vos.add(vo);
            }

            model.addAttribute("news", news);
            model.addAttribute("likeCount", newsService.getNewsLikeCount(news.getId()));
            model.addAttribute("author", userService.getUserFromId(news.getUserId()));
            model.addAttribute("commentVOs", vos);
        }catch (Exception e){
            logger.error("获取页面详情异常", e);
            return "redirect:/index";
        }

        return "newsDisplay";
    }

    @RequestMapping(path = "/newsDisplay/addComment", method = {RequestMethod.POST})
    @ResponseBody
    public String addComment(@RequestParam("newsId") int newsId,
                             @RequestParam("content") String content){

        try {
            UserModel user = localInfo.getUser();
            if(user == null){
                return jsonUtil.getJsonString(1, "登录后才能评论");
            }

            if(commentService.addComment(user.getId(), newsId, content) == 0){
                return jsonUtil.getJsonString(1, "增加评论异常, 稍后再试");
            }

            Map<String, Object> map = new HashMap<>();
            map.put("reviewer", user.getUsername());
            map.put("comment_content", content);
            map.put("comment_count", commentService.incrAndGetCommentCount(newsId));
            map.put("create_time", (new Date()).toString());

            return jsonUtil.getJsonString(0, map);
        }catch (Exception e){
            logger.error("增加评论异常", e);
            return jsonUtil.getJsonString(1, "系统内部出错");
        }
    }

    @RequestMapping(path = "/publish", method = RequestMethod.GET)
    public String publish(){
        return "publish";
    }


    @RequestMapping(path = {"/uploadImage"}, method = {RequestMethod.POST})
    @ResponseBody
    public String uploadImage(@RequestParam("uploadImg") MultipartFile imageFile){
        UserModel user = localInfo.getUser();
        if(user == null){
            return jsonUtil.getJsonString(1, "请先登录");
        }

        Map<String, Object> map = new HashMap<>();
        String imageUrl = null;
        try{
            imageUrl = qiniuService.saveImage(imageFile);
            if(imageUrl == null){
                map.put("fail", "图片上传异常");
            }
        }catch (Exception e){
            logger.error("添加分享图片失败" + e.getMessage());
            map.put("fail", "图片上传异常");
        }
        return jsonUtil.getJsonString(0, imageUrl);
    }


    @RequestMapping(path = "/addBlog", method = RequestMethod.POST)
    @ResponseBody
    public String addBlog(@RequestParam(value = "title", defaultValue = "") String title,
                          @RequestParam(value = "content", defaultValue = "") String content,
                          @RequestParam(value = "imageUrl", defaultValue = "") String imageUrl){
        try{
            UserModel user = localInfo.getUser();
            if(user == null){
                return "登录后才能发表";
            }
            News news = newsService.addNews(user.getId(), title, content, imageUrl);
            if(news == null){
                return "发帖失败";
            }
        }catch (Exception e){
            e.printStackTrace();
            return "网络异常, 稍后再试";
        }

        return "发帖成功";
    }
}