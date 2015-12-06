package net.voksul;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static String CLIENT_ID = null;
    public static String CLIENT_SECRET = null;
    public static String BACKUP_DIR = null;
    public static void main(String[] args) throws IOException, InterruptedException, ParseException {
        File config = new File("config.json");
        if(config.exists()) {
            JSONObject configObj = (JSONObject) new JSONParser().parse(new FileReader(config));
            if(configObj.containsKey("client_id")) {
                if(!((String)configObj.get("client_id")).equalsIgnoreCase("CLIENT_ID")) {
                    CLIENT_ID = (String) configObj.get("client_id");
                    CLIENT_SECRET = (String) configObj.get("client_secret");
                    BACKUP_DIR = (String) configObj.get("backup_dir_on_drive");
                }else{
                    System.out.println("Please edit the correct data into the configuration file as per the documentation on github.");
                    System.exit(0);
                }
            }else{
                System.out.println("Please edit the correct data into the configuration file as per the documentation on github.");
                System.exit(0);
            }
        }else{
            BufferedWriter bw = new BufferedWriter(new FileWriter(config));bw.write("{" + System.lineSeparator());
            bw.write("  \"client_id\": \"CLIENT_ID\"," + System.lineSeparator());
            bw.write("  \"client_secret\": \"CLIENT_SECRET\"," + System.lineSeparator());
            bw.write("  \"backup_dir_on_drive\": \"\"" + System.lineSeparator());
            bw.write("}" + System.lineSeparator());
            bw.flush();
            bw.close();
            System.out.println("Please edit the correct data into the configuration file as per the documentation on github.");
            System.exit(0);
        }


        File credentials = new File("credentials");
        String at = null,tt = null,rt = null;
        if(!credentials.exists()) {
            Request request = new Request("https://accounts.google.com/o/oauth2/v2/auth", Request.Method.GET);
            request.addParam("scope", "https://www.googleapis.com/auth/drive email profile");
            request.addParam("redirect_uri","urn:ietf:wg:oauth:2.0:oob");
            request.addParam("response_type","code");
            request.addParam("client_id",CLIENT_ID);
            System.out.println("Get code from the url below then input it.");
            System.out.println(request.getUrl());
            Scanner scan = new Scanner(System.in);
            String code = scan.nextLine();

            Request token = new Request("https://www.googleapis.com/oauth2/v4/token",Request.Method.POST);
            token.setContentType("application/x-www-form-urlencoded");
            token.addParam("code",code);
            token.addParam("client_id",CLIENT_ID);
            token.addParam("client_secret",CLIENT_SECRET);
            token.addParam("redirect_uri","urn:ietf:wg:oauth:2.0:oob");
            token.addParam("grant_type","authorization_code");
            JSONObject res = token.execute();
            if(res.containsKey("access_token")) {
                at = (String) res.get("access_token");
                rt = (String) res.get("refresh_token");
                tt = (String) res.get("token_type");
            }
            PrintWriter pw = new PrintWriter(new FileWriter(credentials));
            pw.println(at);
            pw.println(rt);
            pw.println(tt);

            pw.flush();
            pw.close();

        }else{
            BufferedReader br = new BufferedReader(new FileReader(credentials));
            at = br.readLine();
            rt = br.readLine();
            tt = br.readLine();
        }

        //Check validity of token
        Request list = new Request("https://www.googleapis.com/drive/v2/files", Request.Method.GET);
        list.addProperty("Authorization", "Bearer " + at);
        JSONObject listResp = list.execute();
        List<String> excludes = null;
        if(args.length == 2) {
            excludes = new ArrayList<>();
            File exclude = new File(args[1]);
            BufferedReader br = new BufferedReader(new FileReader(exclude));
            String line;
            while((line = br.readLine()) != null) {
                excludes.add(line);
            }
        }
        if(listResp.containsKey("error")) {
            Request renew = new Request("https://www.googleapis.com/oauth2/v4/token", Request.Method.POST);
            renew.setContentType("application/x-www-form-urlencoded");
            renew.addParam("client_id", CLIENT_ID);
            renew.addParam("client_secret",CLIENT_SECRET);
            renew.addParam("refresh_token",rt);
            renew.addParam("grant_type","refresh_token");
            JSONObject refresh = renew.execute();
            at = (String) refresh.get("access_token");
            tt = (String) refresh.get("token_type");

            PrintWriter pw = new PrintWriter(new FileWriter(credentials));
            pw.println(at);
            pw.println(rt);
            pw.println(tt);

            pw.flush();
            pw.close();
        }
        System.out.println("Tarring folder/file..");
        SimpleDateFormat df = new SimpleDateFormat("M-d-y");
        String fileName = "backup-" + df.format(new Date()) + ".tar";
        String[] params = new String[4+excludes.size()];
        params[0] = "tar";
        params[1] = "-cf";
        params[2] = fileName;
        params[3] = args[0];
        for(int i = 4; i < params.length; i++) {
            params[i] = "--exclude="+excludes.get(i-4);
        }
        ProcessBuilder pb = new ProcessBuilder(params);
        long start = System.currentTimeMillis();
        Process p = pb.start();
        p.waitFor();
        long end = System.currentTimeMillis();
        System.out.println("Time elapsed: " + formatTime(end - start));
        File zipFile = new File(fileName);
        System.out.println("Uploading..");
        Request upload = new Request("https://www.googleapis.com/upload/drive/v2/files?uploadType=resumable", Request.Method.POST);
        upload.setContentType("application/x-tar");
        upload.addProperty("Authorization", "Bearer " + at);

        JSONObject obj = upload.upload(zipFile, "{ title: \"" + zipFile.getName() + "\"}",at);
        if(!obj.containsKey("error")) {
            System.out.println("\nUpload successful");
            Request move = new Request("https://www.googleapis.com/drive/v2/files/" + obj.get("id") + "/parents", Request.Method.POST);
            move.addProperty("Authorization", "Bearer " + at);
            move.setContentType("application/json");
            move.addParam("json","{ id: \"" + BACKUP_DIR + "\" }");
            JSONObject moveJ = move.execute();
        }
        System.out.println("Removing local backup file..");
        ProcessBuilder pb2 = new ProcessBuilder("rm", fileName);
        pb2.start();
    }

    private static String formatTime(long millis) {
        long hours = (long) Math.floor(millis/ (3600*1000));
        long left1 = millis % (3600 & 1000);
        long minutes = (long) Math.floor(millis/ (60 * 1000));
        long left2 = left1 % (60 * 1000);
        long seconds = (long) Math.floor(millis / 1000);
        return hours + "h " + minutes + "m " + seconds + "s";
    }
}
