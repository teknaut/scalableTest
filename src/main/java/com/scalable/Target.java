package com.scalable;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Target  {

    private StringBuilder SEARCH_URL        = new StringBuilder("https://www.google.com/search");
    private Logger LOG                      = Logger.getLogger(Target.class.getName());
    private int PAGE_LIMIT                  = 10;
    private ExecutorService executor        = Executors.newCachedThreadPool();
    private String searchTerm;

    public Target(String searchTerm) {
        try {
            this.searchTerm = URLEncoder.encode(searchTerm, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.log(Level.SEVERE, "CANNOT USE GIVEN SEARCH TERM", e);
        }
    }

    /**
     * Execute the process
     */
    public void run() {
        System.out.println("PROCESSING PLEASE WAIT...... ");

        Map<String, Integer> finalResult    = new HashMap<>();
        Map<String, Integer> tmpMap;
        SEARCH_URL.append("?q=").append(searchTerm).append("&num=").append(PAGE_LIMIT);

        try {
            StringBuilder intialResult = getHtml(SEARCH_URL.toString()).get();
            List<String> hrefs = getHrefsFromResult(intialResult);

            StringBuilder html;
            //iterate urls and merge a map of resulting script libs
            for (String url : hrefs) {
                html = getHtml(url).get();
                tmpMap = getScripts(html);
                tmpMap.forEach((k, v) -> finalResult.merge(k, v, (v1, v2) -> v1 + v2));
            }
        }
        catch(Exception ex){
            String err = "POSSIBLE INTERRUPTION IN REQUEST";
            LOG.log(Level.WARNING, err, ex);
            throw new IllegalStateException(err);
        }

        finalResult.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .forEach(System.out::println);

        System.out.println("PROCESS COMPLETE");
    }

    /**
     * Call a url and parse for JS libraries  adding to a map
     * containing a count of occurrences
     * @param html string to parse
     * @return  void
     */
    public Map<String, Integer> getScripts(StringBuilder html){
        Pattern p                   = Pattern.compile("<script.*?src=\"(.*?)\"");
        Matcher m                   = p.matcher(html.toString());
        Map<String, Integer> libs = new HashMap<>();
        String scriptSrc            = "";
        int count                   = 0;
        String tmp;
        while(m.find()){
            //chop off preceding  chars
            tmp = m.group(1).substring(m.group(1).lastIndexOf("/")+ 1);
            //now get rid off any url params after ?
            int pos = tmp.indexOf("?");
            scriptSrc = (pos > -1) ? tmp.substring(0, pos) : tmp;

            //ignore non js src's
            if(scriptSrc.endsWith(".js")){
                if(libs.get(scriptSrc) == null){
                    libs.put(scriptSrc, 1);
                }
                else{
                    count = libs.get(scriptSrc);
                    count++;
                    libs.put(scriptSrc, count);
                }
            }
        }
        return libs;
    }

    /**
     * search String for hrefs in <h3 class="r"> tags
     * and extract to list 
     * @param html
     * @return
     */
    public List<String> getHrefsFromResult(StringBuilder html) {
        List<String> hrefs  = new ArrayList<>();
        String url          = null;
        Pattern tag         = Pattern.compile("<h3 class=\"r\"><a href=\"(.*?)\">");//first find the h3 tags
        Matcher tagFinder   = tag.matcher(html);
        while(tagFinder.find()){
            url = tagFinder.group(1).replace("/url?q=", "");
            if(url.startsWith("http")) hrefs.add(url.substring(0, url.indexOf("&"))); //clean extra trailing characters
        }
        return hrefs;
    }


    /**
     * Execute request for given path
     * Attempt to speed up requests by not waiting for one to finish
     * before starting another one
     * @param PATH
     * @return
     */
    public Future<StringBuilder> getHtml(String PATH){

        try {

            Callable<StringBuilder> task = () -> {
                StringBuilder result = new StringBuilder();
                HttpURLConnection http = null;
                String s;
                try {
                    URL url = new URL(PATH.toString());
                    http = (HttpURLConnection) url.openConnection();
                    http.setRequestProperty(
                            "User-Agent",
                            "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2"
                    );
                    http.connect();
                    BufferedReader serverResponse = new BufferedReader(
                            new InputStreamReader(http.getInputStream()));
                    while ((s = serverResponse.readLine()) != null) {
                        result.append(s);
                    }
                    serverResponse.close();
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "PROBLEM SENDING REQUEST" + PATH.toString(), ex);
                } finally {
                    http.disconnect();
                }
                return result;
            };

            Future<StringBuilder> result = executor.submit(task);
            return result;
        }
        catch(Exception ex){
            LOG.log(Level.WARNING, "PROBLEM MAKING CALL TO : " + PATH , ex);
        }
        return null;
    }
}