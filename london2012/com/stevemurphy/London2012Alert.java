package com.stevemurphy;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class London2012Alert
{
    static Map<String, Date> sessionIds = new HashMap<String, Date>();
    private static Random random;

    public static void main(String[] args)
    {
        random = new Random();

        try {
            while (true) {
                fetchSessions(args[0]);

                purgeOldSessions();

                randomWait(15);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void purgeOldSessions()
    {
        Map<String, Date> retained = new HashMap<String, Date>();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, -5);

        for (String s : sessionIds.keySet()) {
            Date date = sessionIds.get(s);

            if (!date.before(calendar.getTime())) {
                retained.put(s, date);
            }
            else {
                System.out.println(new Date() + ": Deleting expired session " + s);
            }
        }

        sessionIds = retained;
        System.out.println(new Date() + ": Sessions " + sessionIds.size());
    }

    private static void fetchSessions(String urlString) throws Exception
    {
        int offset = 0;

        Set<String> sessions = new HashSet<String>();
        for (; ; ) {
            String tofetch = urlString + "&offset=" + offset;
            String page = getPage(tofetch);

            if (page.indexOf(
                    "We did not find any sessions that meet your criteria. Please change your search criteria and try again.") > 0) {
                return;
            }
            int index = page.indexOf("<input type=\"hidden\" name=\"id\" value=");

            while (index > 0) {
                page = page.substring(index + "<input type=\"hidden\" name=\"id\" value=".length() + 1);
                index = page.indexOf("<input type=\"hidden\" name=\"id\" value=");

                String sessionId = page.substring(0, "0000455ACF240E28".length());

                if (sessions.add(sessionId)) {
                    fetchSession(sessionId);
                }
            }

            randomWait(2);
            offset++;
        }
    }


    private static void fetchSession(String sessionId) throws Exception
    {
        String page  = getPage("http://www.tickets.london2012.com/eventdetails?id=" + sessionId);

        if (page.contains("Add to shopping list"))
        {
            if (sessionIds.put(sessionId, new Date()) == null)
            {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE))
                {
                    desktop.browse(new URI("http://www.tickets.london2012.com/eventdetails?id=" + sessionId + "&cachebuster=" + random.nextInt(Integer.MAX_VALUE)));
                }

                makeSomenoise();
            }
        }
        else {
            System.out.println(new Date() + ": Gone :-( sessionId = " + sessionId);

        }
    }

    private static void makeSomenoise()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < 10; i++) {
                    Toolkit.getDefaultToolkit().beep();
                    try {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

            }
        });

        thread.start();
    }

    private static void randomWait(int seconds) throws InterruptedException
    {
        Thread.sleep((seconds + random.nextInt(seconds)) * 1000);
    }

    // Send some random user agents...
    static private String[] userAgents =
            {
                    "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/22.0.1207.1 Safari/537.1",
                    "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:14.0) Gecko/20120405 Firefox/14.0a1",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/534.55.3 (KHTML, like Gecko) Version/5.1.3 Safari/534.53.10",
                    "Mozilla/5.0 (Windows; U; MSIE 9.0; WIndows NT 9.0; en-US))"
            };

    private static String getPage(String page) throws Exception
    {
        String cacheBusted = page  + "&cachebuster=" + random.nextInt(Integer.MAX_VALUE);

        System.out.println(new Date() + ": " + cacheBusted);
        URL url = new URL(cacheBusted);
        URLConnection connection = url.openConnection();

        // Pretent to be something else.
        connection.setRequestProperty("User-Agent", userAgents[random.nextInt(userAgents.length)] );
        connection.setUseCaches(false);


        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));


            StringBuilder stringBuilder = new StringBuilder();

            String line = in.readLine();

            while (line != null) {
                stringBuilder.append(line);
                line = in.readLine();
            }

            return stringBuilder.toString();
        }
        finally {
            if (in != null) in.close();
        }
    }
}
