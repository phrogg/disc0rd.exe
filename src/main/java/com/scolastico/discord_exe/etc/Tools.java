package com.scolastico.discord_exe.etc;

import com.scolastico.discord_exe.Disc0rd;
import com.scolastico.discord_exe.config.ConfigDataStore;
import com.scolastico.discord_exe.event.extendedEventSystem.ExtendedEventDataStore;
import com.scolastico.discord_exe.mysql.ServerSettings;
import com.sun.net.httpserver.HttpExchange;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Tools {

    private Boolean _isShowingLoadingAnimation = false;
    private static Tools instance = null;

    private Tools() {}
    public static Tools getInstance() {
        if (instance == null) {
            instance = new Tools();
        }
        return instance;
    }

    public void generateNewSpacesInConsole(int times) {
        for (int tmp = 0; times > tmp; tmp++) {
            System.out.println("");
        }
    }

    public Boolean isShowingLoadingAnimation() {
        return _isShowingLoadingAnimation;
    }

    public void asyncLoadingAnimationWhileWaitingResult(Runnable function) {
        _isShowingLoadingAnimation = true;
        Thread thread = new Thread(function);
        thread.start();
        int counter = 50;
        while(thread.isAlive()) {
            if (counter >= 50) {
                System.out.print(".");
                counter = 0;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(10);
                counter++;
            } catch (InterruptedException ignored) {}
        }
        System.out.println(" [OK]");
        _isShowingLoadingAnimation = false;
    }

    public boolean isOwner(Guild guild, User user) {
        return guild.getOwnerId().equals(user.getId());
    }

    public String getAlphaNumericString(int length) {
        String AlphaNumericString = "abcdefghijklmnopqrstuvxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder stringBuilder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            stringBuilder.append(AlphaNumericString.charAt(index));
        }
        return stringBuilder.toString();
    }

    public HashMap<String, String> getPostValuesFromHttpExchange(HttpExchange httpExchange) {
        HashMap<String, String> hashMap = new HashMap<>();
        try {
            if (!httpExchange.getRequestHeaders().getFirst("Content-Type").equals("application/x-www-form-urlencoded")) return hashMap;
            StringBuilder stringBuilder = new StringBuilder();
            InputStream inputStream = httpExchange.getRequestBody();
            int i;
            while ((i = inputStream.read()) != -1) {
                stringBuilder.append((char) i);
            }
            for (String pair:stringBuilder.toString().split("&")) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    hashMap.put(keyValue[0], URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.toString()));
                }
            }
        } catch (Exception e) {
            ErrorHandler.getInstance().handle(e);
        }
        return hashMap;
    }

    public Color hex2Rgb(String colorStr) {
        return new Color(
                Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }

    public String rgb2Hex(Color color) {
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

    public boolean isColorSimilar(Color colorBase, Color color, int sensitivity) {
        for (int r = (-sensitivity); sensitivity >= r; r++) {
            for (int g = (-sensitivity); sensitivity >= g; g++) {
                for (int b = (-sensitivity); sensitivity >= b; b++) {
                    if ((colorBase.getRed() - r) == color.getRed() && (colorBase.getGreen() - g) == color.getGreen() && (colorBase.getBlue() - b) == color.getBlue()) return true;
                }
            }
        }
        return false;
    }

    public String sendPostRequest(String uri) {
        try {
            URL url = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
            return content.toString();
        } catch (Exception e) {
            ErrorHandler.getInstance().handle(e);
        }
        return null;
    }

    public int tryToParseInt(String integer) {
        try {
            return Integer.parseInt(integer);
        } catch (Exception ignored) {}
        return 0;
    }

    public void writeGuildLogLine(long guildId, String logText) {
        ServerSettings serverSettings = Disc0rd.getMysql().getServerSettings(guildId);
        String log = serverSettings.getLog();
        while (countLines(log) >= getLimitFromGuild(guildId).getLogLines()) {
            log = log.substring(0, log.lastIndexOf("\n"));
            if (log.equals("")) break;
        }
        log = "[" + new Date().toString() + "] " + logText + "\n" + log;
        serverSettings.setLog(log);
    }

    public int countLines(String str){
        String[] lines = str.split("\n");
        return  lines.length;
    }

    public String getStringWithVarsFromDataStore(ExtendedEventDataStore dataStore, String string) {
        for (String key:dataStore.getDataStore().keySet()) {
            string = string.replace("{" + key + "}", dataStore.getDataStore().get(key));
        }
        return string;
    }

    public ServerSettings.ServerLimits getLimitFromGuild(long guildId) {
        ServerSettings.ServerLimits serverLimits = Disc0rd.getMysql().getServerSettings(guildId).getServerLimits();
        ConfigDataStore.DefaultLimits defaultLimits = Disc0rd.getConfig().getDefaultLimits();
        if (serverLimits.getActionsPerEvent() == 0) serverLimits.setActionsPerEvent(defaultLimits.getActionsPerEvent());
        if (serverLimits.getEvents() == 0) serverLimits.setEvents(defaultLimits.getEvents());
        if (serverLimits.getLogLines() == 0) serverLimits.setLogLines(defaultLimits.getLogLines());
        return serverLimits;
    }

}
