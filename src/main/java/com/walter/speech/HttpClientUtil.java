package com.walter.speech;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * http 请求工具类
 * 
 * @author wuchenxi
 * @date 2018年4月24日 下午1:01:58
 *
 */
@Component
public class HttpClientUtil {

	@Value("${http.maxTotal}")
	private Integer maxTotal;

	@Value("${http.timeOut}")
	private Integer timeOut;

	@Value("${http.defaultMaxPerRout}")
	private Integer defaultMaxPerRoute;

	PoolingHttpClientConnectionManager cm = null;

	@PostConstruct
	public void init() {
		LayeredConnectionSocketFactory sslsf = null;
		try {
			sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("https", sslsf).register("http", new PlainConnectionSocketFactory()).build();
		cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		cm.setMaxTotal(maxTotal);
		cm.setDefaultMaxPerRoute(defaultMaxPerRoute);
	}

	public CloseableHttpClient getHttpClient() {
		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();

		return httpClient;
	}

	/**
	 * post请求
	 * 
	 * @param url
	 *            请求地址
	 * @param params
	 *            求情参数
	 * @return 响应内容
	 * @throws IOException
	 */
	public String post(String url, Map<String, String> params) throws IOException {
		String result;
		CloseableHttpClient httpClient = this.getHttpClient();
		/** HttpPost */
		HttpPost httpPost = new HttpPost(url);

		List<BasicNameValuePair> param = new ArrayList<>();
		params.forEach((key, value) -> {
			if (value != null) {
				param.add(new BasicNameValuePair(key, value));
			}
		});

		httpPost.setEntity(new UrlEncodedFormEntity(param, "UTF-8"));
		/** HttpResponse */
		CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
		try {
			HttpEntity httpEntity = httpResponse.getEntity();
			result = EntityUtils.toString(httpEntity, "utf-8");
			EntityUtils.consume(httpEntity);
		} finally {
			try {
				if (httpResponse != null) {
					httpResponse.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * get请求
	 * @param url	请求url
	 * @return	响应数据
	 */
	public String get(String url) {
		String result = null;
		CloseableHttpClient httpClient = this.getHttpClient();
		HttpGet httpGet = new HttpGet(url);
		
		HttpResponse httpResponse;
		try {
			httpResponse = httpClient.execute(httpGet);
			HttpEntity entity = httpResponse.getEntity();
			result = EntityUtils.toString(entity);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
}
