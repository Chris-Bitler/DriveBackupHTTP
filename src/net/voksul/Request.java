package net.voksul;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;

public class Request {
    HashMap<String,String> params = new HashMap<>();
    HashMap<String,String> properties = new HashMap<>();
    Method type;
    String url;
    String contentType;

    public Request(String url) {
        this.url = url;
        this.type = Method.GET;
    }

    public Request(String url, Method type) {
        this.url = url;
        this.type = type;
    }

    public void addParam(String k,String v) {
        params.put(k,v);
    }

    public void addProperty(String k,String v) {
        properties.put(k,v);
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getUrl() {
        if(type == Method.GET) {
            return url + "?" + encodeParams();
        }else if(type == Method.POST) {
            return url;
        }
        return "";
    }

    private String encodeParams() {
        String data = "";
        boolean firstVal = true;
        for(Map.Entry<String,String> entry : params.entrySet()) {
            try {
                data += ((!firstVal ? "&" : "") + entry.getKey() + "=" + URLEncoder.encode(entry.getValue(),"UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            firstVal = false;
        }
        return data;
    }

    public JSONObject execute() {
        if(type == Method.POST) {
            String result = "";
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.addRequestProperty("Content-Type",contentType);
                for(Map.Entry<String,String> entry : properties.entrySet()) {
                    conn.addRequestProperty(entry.getKey(),entry.getValue());
                }
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                String data = "";
                if(!contentType.equalsIgnoreCase("application/json")) {
                    data = encodeParams();
                }else{
                    data = params.get("json");
                }

                conn.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
                conn.getOutputStream().write(data.getBytes());
                conn.getOutputStream().flush();
                conn.getOutputStream().close();
                if(conn.getResponseCode() == 200) {

                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = br.readLine()) != null) {
                        result += line;
                    }
                }else{
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    String line;
                    while((line = br.readLine()) != null) {
                        result += line;
                    }
                }
                JSONObject obj = (JSONObject) new JSONParser().parse(result);
                return obj;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }else if(type == Method.GET) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url + "?" + encodeParams()).openConnection();
                conn.addRequestProperty("Content-Type", contentType);
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    conn.addRequestProperty(entry.getKey(), entry.getValue());
                }
                conn.setDoOutput(true);
                conn.setDoInput(true);

                String data = encodeParams();
                if(conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String result = "";
                    String line;
                    while ((line = br.readLine()) != null) {
                        result += line;
                    }

                    JSONObject obj = (JSONObject) new JSONParser().parse(result);
                    return obj;
                }else{
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    String result = "";
                    String line;
                    while ((line = br.readLine()) != null) {
                        result += line;
                    }

                    JSONObject obj = (JSONObject) new JSONParser().parse(result);
                    return obj;
                }
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public JSONObject upload(File f, String data, String at) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.addRequestProperty("Content-Type","application/json; charset=UTF-8");
            for(Map.Entry<String,String> entry : properties.entrySet()) {
                conn.addRequestProperty(entry.getKey(),entry.getValue());
            }
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            conn.setRequestProperty("Content-Length", String.valueOf(data.getBytes().length));
            conn.setRequestProperty("X-Upload-Content-Type", contentType);
            conn.setRequestProperty("X-Upload-Content-Length", String.valueOf(f.length()));
            conn.getOutputStream().write(data.getBytes());
            conn.getOutputStream().flush();
            conn.getOutputStream().close();
            if(conn.getResponseCode() == 200) {

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                }
            }else{
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                String line;
                while((line = br.readLine()) != null) {
                }
            }
            HttpURLConnection conn2 = (HttpURLConnection) new URL(conn.getHeaderField("Location")).openConnection();
            conn2.setRequestMethod("PUT");
            conn2.setRequestProperty("Content-Length", String.valueOf(f.length()));
            conn2.setFixedLengthStreamingMode(f.length());
            conn2.setRequestProperty("Content-Type",contentType);
            conn2.setRequestProperty("Authorization", "Bearer " + at);
            conn2.setDoOutput(true);
            conn2.setDoInput(true);
            FileInputStream fis = new FileInputStream(f);
            long written = 0;
            byte[] buffer = new byte[8192];
            int count;
            while((count = fis.read(buffer)) != -1) {
                conn2.getOutputStream().write(buffer,0,count);
                written+=count;
                updateProgress(written,f.length());
            }
            conn2.getOutputStream().flush();
            conn2.getOutputStream().close();

            String result = "";

            if(conn2.getResponseCode() == 201) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn2.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    result += line;
                }
            }else{
                BufferedReader br = new BufferedReader(new InputStreamReader(conn2.getInputStream()));
                String line;
                while((line = br.readLine()) != null) {
                    result += line;
                }
            }
            return (JSONObject) new JSONParser().parse(result);
        }catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void updateProgress(long part, long total) {
        int percent = (int) Math.round((part * 100.0) / total + 0.5);
        percent = (percent > 100 ? 100 : percent);
        final int width = 50; // progress bar width in chars
        System.out.print("\r[");
        int i = 0;
        for (; i <= (int)(((double)percent/100)*width); i++) {
            System.out.print("=");
        }
        for (; i < width; i++) {
            System.out.print(" ");
        }
        DecimalFormat df = new DecimalFormat("0");
        System.out.print("] " + df.format(percent) + "%");
    }

    enum Method {
        POST,
        GET
    }
}
