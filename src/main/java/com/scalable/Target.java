package com.scalable;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Target  {

    private final StringBuilder SEARCH_URL  = new StringBuilder("https://www.google.com/search");
    private Logger LOG                      = Logger.getLogger(Target.class.getName());
    private int PAGE_LIMIT                  = 10;
    private Executor executor               = Executors.newCachedThreadPool();
    private final String searchTerm;

    public Target(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    /**
     * Execute the process
     */
    public void run() {
        System.out.println("PROCESSING PLEASE WAIT...... ");
        Map<String, Integer> finalResult    = new HashMap<>();
        Map<String, Integer> tmpMap;
        SEARCH_URL.append("?q=").append(searchTerm).append("&num=").append(PAGE_LIMIT);

        StringBuilder intialResult = getHtml(SEARCH_URL.toString());
        List<String> hrefs = getHrefsFromResult(intialResult);

        StringBuilder html;
        //iterate urls and merge a map of resulting script libs
        for(String url : hrefs){
            html    = getHtml(url);
            tmpMap  = getScripts(html);
            tmpMap.forEach((k, v) -> finalResult.merge(k, v, (v1, v2) -> v1 + v2));
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
     * @param PATH
     * @return
     */
    public StringBuilder getHtml(String PATH){
        StringBuilder result    = new StringBuilder();
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
    }
}