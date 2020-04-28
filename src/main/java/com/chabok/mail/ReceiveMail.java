package com.chabok.mail;

import javax.mail.*;
import java.io.*;
import java.sql.*;
import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;
import io.github.cdimascio.dotenv.Dotenv;

public class ReceiveMail {
    // Read from dotenv
    public static Dotenv dotenv = Dotenv.configure().directory("/opt/.env").load();
    // MySQL Config
    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static final String DB_URL =dotenv.get("mysql_url");
    static final String USER =dotenv.get("mysql_user");
    static final String PASS =dotenv.get("mysql_pass");
    // void receiveMail
    public static void receiveMail(String userName, String password) throws IOException {
        try {
            // Connect to mail server
            Properties properties = new Properties();
            properties.setProperty("mail.store.protocol", "pop3");
            Session emailSession = Session.getDefaultInstance(properties);
            Store emailStore = emailSession.getStore("pop3");
            emailStore.connect(dotenv.get("pop_server"), userName, password);
            Folder emailFolder = emailStore.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);
            Message messages[] = emailFolder.getMessages();
            for ( Message message : messages ) {
                // Change date format
                String input = message.getSentDate().toString();
                SimpleDateFormat parser=new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy");
                Date date = parser.parse(input);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String formattedDate = formatter.format(date);
                // Change message body type and get paragraphs from html body
                String body = message.getContent().toString();
                Document doc = Jsoup.parse(body);
                Elements paragraphs = doc.select("p");
                // Set from date in search emails
                Date fromdate = new Date(System.currentTimeMillis() -
                        (Integer.parseInt(Objects.requireNonNull(dotenv.get("last_hour"))) * 60 * 60 * 1000));
                // Clean receive from email address
                String receiveFrom = message.getFrom()[0].toString().split("<")[1].split(">")[0];
                // check email date is newer than from date
                if( date.compareTo(fromdate) > 0 ) {
                    // check specified email sender
                    if(message.getFrom()[0].toString().contains(Objects.requireNonNull(dotenv.get("receive_from")))) {
                        // check specified email subject
                        if(message.getSubject().contains(Objects.requireNonNull(dotenv.get("search_subject")))) {
                            // Get current date
                            // SimpleDateFormat currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            LocalDateTime now = LocalDateTime.now();
                            // Hashmap get key value pair from email body
                            Map<String, String> map = new HashMap<String, String>();
                            for(String keyValue : paragraphs.toString().replaceAll("\\<.*?\\>", "")
                                    .replace("&nbsp;"," ").split("\\n")) {
                                String[] key = keyValue.split(":");
                                if(key.length == 2){
                                    map.put(key[0].trim(), key[1].trim());
                                }
                            }
                            // Check all data is correct and not missing any key
                            if (map.get("appId") != null && map.get("adminName") != null && map.get("adminLastName") != null
                                    && map.get("appName") != null && map.get("appDesc") != null
                                    && map.get("adminEmail") != null && map.get("adminPhone") != null
                                    && map.get("password") != null ){
                                Connection conn = null;
                                Statement stmt = null;
                                // Search in database by appId if exist then pass if not insert record
                                try{
                                    Class.forName("com.mysql.jdbc.Driver");
                                    conn = DriverManager.getConnection(DB_URL, USER, PASS);
                                    stmt = conn.createStatement();
                                    String selectQuery = ("SELECT count(*) as total From "+dotenv.get("table_name")+" where appId ='"+ map.get("appId")+"'");
                                    ResultSet rs = stmt.executeQuery(selectQuery);
                                    rs.next();
                                    int recordExist = rs.getInt("total");
                                    rs.close();
                                    if(recordExist == 0) {
                                        String insertQuery = "INSERT INTO "+dotenv.get("table_name")+"(appId, adminName, adminLastName, appName," +
                                                "appDesc, adminEmail, adminPhone, password, requestFrom, startDate, modifiedDate," +
                                                "status) VALUES('"+map.get("appId")+"','"+map.get("adminName")+"','"+map.get("adminLastName")+"'," +
                                                "'"+map.get("appName")+"','"+map.get("appDesc")+"','"+map.get("adminEmail")+"'," +
                                                "'"+map.get("adminPhone")+"','"+map.get("password")+"','"+receiveFrom+"','"+formattedDate+"'," +
                                                "'"+dtf.format(now)+"','Start_request')";
                                        stmt.executeUpdate(insertQuery);
                                    }
                                }catch (SQLException | ClassNotFoundException e){
                                    e.printStackTrace();
                                }finally {
                                    try {
                                        if(stmt!=null)
                                            conn.close();
                                    }catch (SQLException e){
                                    }
                                    try {
                                        if(conn!=null)
                                            conn.close();
                                    }catch (SQLException e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            emailFolder.close(false);
            emailStore.close();
        } catch (ParseException | IOException | MessagingException nspe) {
            nspe.printStackTrace();
        }
    }

    public static void main(String... args) throws IOException {
        receiveMail(dotenv.get("pop_server_username"), dotenv.get("pop_server_password"));
    }
}
