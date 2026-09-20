package com.wapo.flagship.network;

import android.net.Uri;

import com.wapo.flagship.FlagshipApplication;
import com.wapo.flagship.Utils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.*;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class HttpUtil {
    public static String getString(String url) throws IOException, HttpException {
        return getString(url, null, null, null);
    }

    public static String getString(String url, Map<String, String> headers) throws IOException, HttpException {
        return getString(url, null, null, headers);
    }

    public static String getString(String url, String userName, String password, Map<String, String> headers) throws IOException, HttpException {
        HttpEntity responseEntity = httpGet(url, userName, password, headers);
        return Utils.inputStreamToString(responseEntity.getContent());
    }

    public static HttpEntity httpGet(String url) throws IOException, HttpException {
        return httpGet(url, null, null, null);
    }

    public static HttpEntity httpGet(String url, Map<String, String> headers) throws IOException, HttpException {
        return httpGet(url, null, null, headers);
    }

    public static HttpEntity httpGet(String url, String userName, String password, Map<String, String> headers) throws IOException, HttpException {
        return throwExceptionsOnErrors(httpGetResponse(url, userName, password, headers)).getEntity();
    }

    public static HttpResponse httpGetResponse(String url) throws IOException, HttpException {
        return httpGetResponse(url, null, null, null);
    }

    public static HttpResponse httpGetResponse(String url, Map<String, String> headers) throws IOException, HttpException {
        return httpGetResponse(url, null, null, headers);
    }

    public static HttpResponse httpGetResponse(String url, int connTimeout, int soTimeout, Map<String, String> headers) throws IOException, HttpException {
        return httpGetResponse(url, null, null, connTimeout, soTimeout, headers);
    }

    public static HttpResponse httpGetResponse(String url, String userName, String password, Map<String, String> headers) throws IOException, HttpException {
        return httpGetResponse(url, userName, password, 7000, 7000, headers);
    }

    public static HttpResponse httpGetResponse(String url, String userName, String password, int connTimeout, int soTimeout, Map<String, String> headers) throws HttpException, IOException {
        if (!FlagshipApplication.getInstance().isOnline()) {
            throw new AppIsOfflineException();
        }

        HttpClient client = getClient(url, userName, password, connTimeout, soTimeout);
        HttpGet httpGet = new HttpGet(url);
        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                httpGet.addHeader(h.getKey(), h.getValue());
            }
        }
        try {
            return client.execute(httpGet);
        } catch (UnknownHostException e) {
            FlagshipApplication.getInstance().reportNetworkError(System.currentTimeMillis());
            throw e;
        } catch (ConnectTimeoutException e) {
            FlagshipApplication.getInstance().reportNetworkError(System.currentTimeMillis());
            throw e;
        } catch (NoRouteToHostException e) {
            FlagshipApplication.getInstance().reportNetworkError(System.currentTimeMillis());
            throw e;
        } catch (IllegalStateException e) {
            throw new HttpException("Illegal state", e);
        }
    }

    public static InputStream getResource(String url) throws IOException, HttpException {
        return getResource(url, null, null, null);
    }

    public static InputStream getResource(String url, Map<String, String> headers) throws IOException, HttpException {
        return getResource(url, null, null, headers);
    }

    public static InputStream getResource(String url, String userName, String password, Map<String, String> headers) throws IOException, HttpException {
        HttpEntity responseEntity = httpGet(url, userName, password, headers);
        InputStream is = responseEntity.getContent();
        if (BufferedInputStream.class.isAssignableFrom(is.getClass())) {
            return is;
        }
        return new BufferedInputStream(is);
    }

    public static byte[] getResourceContent(String url) throws IOException, HttpException {
        return getResourceContent(url, null, null, null);
    }

    public static byte[] getResourceContent(String url, Map<String, String> headers) throws IOException, HttpException {
        return getResourceContent(url, null, null, headers);
    }

    public static byte[] getResourceContent(String url, String userName, String password, Map<String, String> headers) throws IOException, HttpException {
        InputStream is = getResource(url, userName, password, headers);
        ByteArrayOutputStream baus = new ByteArrayOutputStream();
        byte[] buff = new byte[8192];
        int len;
        while((len = is.read(buff)) > 0) {
            baus.write(buff, 0, len);
        }
        is.close();
        return baus.toByteArray();
    }

    public static File downloadFile(String url, File file, Map<String, String> headers, HashMap<String, String> outHeaders) throws IOException, HttpException {
        return downloadFile(url, file, headers, outHeaders, null);
    }

    public static File downloadFile(String url, File file, Map<String, String> headers, HashMap<String, String> outHeaders, Utils.Action2<Long, Long> callback) throws IOException, HttpException {
        HttpResponse response = httpGetResponse(url, headers);
        HttpEntity entity = throwExceptionsOnErrors(response).getEntity();

        long contentLength = -1;
        if (outHeaders != null) {
            for (Header header: response.getAllHeaders()) {
                if ("Content-Type".equals(header.getName())) {
                    String [] parts = header.getValue().split(";");
                    outHeaders.put("mime-type", parts[0].trim());
                    for (int i = 1; i < parts.length; i++) {
                        String part = parts[i].trim();
                        if (part.startsWith("charset")) {
                            outHeaders.put("encoding", part.substring(part.indexOf("=") + 1));
                        }
                    }
                }

                outHeaders.put(header.getName(), header.getValue());
                if (contentLength == -1 && "Content-Length".equals(header.getName())) {
                    try {
                        contentLength = Long.parseLong(header.getValue());
                    } catch (Exception ex) {
                    }
                }
            }
        }

        BufferedInputStream bis = new BufferedInputStream(entity.getContent());
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        byte[] buff = new byte[102400];
        int len = 0;
        long downloaded = 0;
        while((len = bis.read(buff, 0, buff.length)) > 0) {
            bos.write(buff, 0, len);

            downloaded += len;
            if (callback != null) {
                callback.run(downloaded, contentLength);
            }
        }
        bis.close();
        bos.flush();
        bos.close();

        return file;
    }

    public static HttpClient getClient(String url, String userName, String password) throws HttpException {
        return getClient(url, userName, password, 7000, 20000);
    }

    public static HttpClient getClient(String url, String userName, String password, int connectionTimeout, int soTimeout) throws HttpException {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout);
        HttpConnectionParams.setSoTimeout(httpParams, soTimeout);

        Uri uri = Uri.parse(url);

        SchemeRegistry schemaReg = new SchemeRegistry();
        String schema = uri.getScheme();
        int port = uri.getPort();
        if(port <= 0){
            if("http".equals(schema)){
                port = 80;
            }else if("https".equals(schema)){
                port = 443;
            } else {
                throw new IndexOutOfBoundsException("Unsupported port and schema");
            }
        }
        try {
            schemaReg.register(new Scheme("http", new PlainSocketFactory(), port));
            if (!"http".equals(schema)) {
                schemaReg.register(new Scheme(schema, new SSLSocketFactoryExt(null), port));
            }
        } catch (java.security.GeneralSecurityException e) {
            throw new HttpException(e.getMessage(), e);
        }
        ThreadSafeClientConnManager connMngr = new ThreadSafeClientConnManager(httpParams, schemaReg);

        DefaultHttpClient httpClient = new DefaultHttpClient(connMngr, httpParams);
        httpClient.setParams(httpParams);

        if(userName != null || password != null){
            httpClient.getCredentialsProvider().setCredentials(
                    new AuthScope(null, -1),
                    new UsernamePasswordCredentials(userName,password)
            );
        }

        return httpClient;
    }

    private static HttpResponse throwExceptionsOnErrors(HttpResponse response) throws HttpException {
        final StatusLine statusLine = response.getStatusLine();
        final int code = statusLine.getStatusCode();
        final HttpEntity responseEntity = response.getEntity();

        if (code != 200){
            String content = "Content is unreachable";
            if (responseEntity != null) {
                try {
                    content = Utils.inputStreamToString(responseEntity.getContent());
                } catch (IOException e) {
                }
            }
            if (code > 499) {
                FlagshipApplication.getInstance().reportNetworkError(System.currentTimeMillis());
            }
            throw new HttpUtilException(code, content);
        }

        if (responseEntity == null) {
            throw new HttpException("Empty response");
        }
        return response;
    }

    public static boolean is400Exception(int code) {
        return code >= 400 && code < 500;
    }


    public static class HttpUtilException extends HttpException {
        private int _httpCode;

        public HttpUtilException(int code, String content) {
            super(content);
            _httpCode = code;
        }

        public int getHttpCode() {
            return _httpCode;
        }
    }

    public static class AppIsOfflineException extends HttpException {}
}
