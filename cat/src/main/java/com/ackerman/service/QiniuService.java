package com.ackerman.service;

import com.alibaba.fastjson.JSONObject;
import com.qiniu.http.Response;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * @Author: Ackerman
 * @Description:
 * @Date: Created in 上午10:45 18-6-24
 */
@Service
public class QiniuService {
    private static final Logger logger = LoggerFactory.getLogger(QiniuService.class);
    private String ACCESS_KEY = "y7rQlLBmNp9UBIwVT3j8x2i4UhXNuuCZuplVRB0M";
    private String SECRET_KEY = "rU2TPU50tvAdFqeQa_imw8F_3_JMmhxbqKYLpJZh";
    private String QINIU_IMAGE_DOMAIN = "http://oz15aje2y.bkt.clouddn.com/";
    private String bucket = "reddit";

    Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);
    UploadManager uploadManager = new UploadManager();

    private String getUpToken(){
        return auth.uploadToken(bucket);
    }

    /**
     * @Description: 保存用户上传的图片, 并且返回图片的访问地址
     * @Date: 上午11:16 18-6-24
     */
    public String saveImage(MultipartFile file) throws IOException{
        try{
            if(file == null){
                return null;
            }

            String fileName = UUID.randomUUID().toString().replaceAll("-", "");
            //七牛Response
            Response response = uploadManager.put(file.getBytes(), fileName, getUpToken());
            if(response.isOK() && response.isJson()){
                return QINIU_IMAGE_DOMAIN + JSONObject.parseObject(response.bodyString()).get("key");
            }
            else{
                logger.error("七牛云储存异常" + response.bodyString());
                return null;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}