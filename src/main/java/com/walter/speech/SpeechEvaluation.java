package com.walter.speech;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.*;

/**
 * 讯飞语音评测工具
 * <p>返回结果说明地址：http://doc.xfyun.cn/ise_protocol/%E8%AF%84%E6%B5%8B%E7%BB%93%E6%9E%9C%E6%A0%BC%E5%BC%8F.html</p>
 *
 * @author wuchenxi
 * @date 2018/6/20 17:01
 */
@Component
public class SpeechEvaluation {

    private Logger logger = LoggerFactory.getLogger(SpeechEvaluation.class);

    @Autowired
    private HttpClientUtil httpClientUtil;

    @Value("${xfyun.ise.URL}")
    private String URL;

    @Value("${xfyun.ise.APP_ID}")
    private String APP_ID;

    @Value("${xfyun.ise.API_KEY}")
    private String API_KEY;

    /**
     * 获取语音评测得分
     *
     * @param text    评测的内容
     * @param fileUrl 语音文件地址
     * @return 测试得分（小数）
     */
    public Double getEvaluationRecord(String text, String fileUrl) {
        String result = this.getEvaluationResult(text, fileUrl);
        String record = JSONObject.parseObject(result).getJSONObject("data").getJSONObject("read_sentence").getJSONObject("rec_paper").getJSONObject("read_chapter").get("total_score").toString();
        return Double.valueOf(record);
    }

    /**
     * 获取评测结果
     *
     * @param text    评测的内容
     * @param fileUrl 语音文件地址
     * @return 测试结果，结果详细说明地址：
     * <a href="http://doc.xfyun.cn/ise_protocol/%E8%AF%84%E6%B5%8B%E7%BB%93%E6%9E%9C%E6%A0%BC%E5%BC%8F.html">
     * http://doc.xfyun.cn/ise_protocol/%E8%AF%84%E6%B5%8B%E7%BB%93%E6%9E%9C%E6%A0%BC%E5%BC%8F.html
     * </a>
     */
    public String getEvaluationResult(String text, String fileUrl) {

        // 读取音频文件
        FileOutputStream fileOutputStream = getFileOutputStream(fileUrl);

        String mp3Bytes = Base64.getEncoder().encodeToString(fileOutputStream.toString().getBytes());
        try {
            mp3Bytes = URLEncoder.encode(mp3Bytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 请求参数
        Map<String, String> map = new HashMap<>(16);
        map.put("aue", "raw");
        map.put("language", "en");
        map.put("category", "read_sentence");

        String param = JSONObject.toJSON(map).toString();
        String X_param = Base64.getMimeEncoder().encodeToString(param.getBytes());
        String currentTime = String.valueOf(System.currentTimeMillis() / 1000);

        CloseableHttpClient httpClient = httpClientUtil.getHttpClient();
        HttpPost post = new HttpPost(URL);
        post.addHeader("X-Appid", APP_ID);
        post.addHeader("X-CurTime", currentTime);
        post.addHeader("X-Param", X_param);
        post.addHeader("X-CheckSum", getMd5(API_KEY + currentTime + X_param));

        map.put("audio", mp3Bytes);
        map.put("text", text);

        List<BasicNameValuePair> param1 = new ArrayList<>();
        map.forEach((key, value) -> {
            if (value != null) {
                param1.add(new BasicNameValuePair(key, value));
            }
        });

        String result = null;
        CloseableHttpResponse response = null;
        try {
            post.setEntity(new UrlEncodedFormEntity(param1, "UTF-8"));
            response = httpClient.execute(post);
            HttpEntity entity = response.getEntity();
            result = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private FileOutputStream getFileOutputStream(String fileUrl) {
        FileOutputStream fileOutputStream = null;
        try {
            File file = new File(fileUrl);
            fileOutputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            logger.error("语音评测获取语音文件失败！路径： {}", fileUrl, e.getMessage());
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fileOutputStream;
    }

    public static String getMd5(String string) {
        if (string == null) {
            return null;
        }
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] byteArray = string.getBytes("UTF-8");
            byte[] md5Bytes = md5.digest(byteArray);
            StringBuffer hexValue = new StringBuffer();
            for (int i = 0; i < md5Bytes.length; i++) {
                int val = ((int) md5Bytes[i]) & 0xff;
                if (val < 16) {
                    hexValue.append("0");
                }
                hexValue.append(Integer.toHexString(val));
            }
            return hexValue.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
