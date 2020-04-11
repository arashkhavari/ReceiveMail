import javax.mail.*;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class JavaMail {
    // Read config.properties file in resource dir
    private static ResourceBundle rb = ResourceBundle.getBundle("config");
    public static void receiveMail(String userName, String password) throws IOException {
        try {
            // Connect to mail server
            Properties properties = new Properties();
            properties.setProperty("mail.store.protocol", "pop3");
            Session emailSession = Session.getDefaultInstance(properties);
            Store emailStore = emailSession.getStore("pop3");
            emailStore.connect(rb.getString("pop_server"), userName, password);
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
                // Check encode path file is exist
                File checkfile=new File(rb.getString("encode_file_path"));
                if(!checkfile.getParentFile().exists()){
                    checkfile.getParentFile().mkdir();
                }
                if(!checkfile.exists()){
                    checkfile.createNewFile();
                }
                // Set fromdate in search emails
                Date fromdate = new Date(System.currentTimeMillis() - (Integer.parseInt(rb.getString("last_hour")) * 60 * 60 * 1000));
                // check email date is newer than from date
                if( date.compareTo(fromdate) > 0 ) {
                    // check spesified email sender
                    if(message.getFrom()[0].toString().contains(rb.getString("receive_from"))) {
                        // check spesified email subject
                        if(message.getSubject().contains(rb.getString ("search_subject"))) {
                            // encode subject for uniq email
                            String subjectencoded = Base64.getEncoder().encodeToString(message.getSubject().getBytes());
                            // Read encode file for check duplicate subject
                            String[] words=null;
                            int duplicate=0;
                            File f1=new File(rb.getString("encode_file_path"));
                            FileReader fr = new FileReader(f1);
                            BufferedReader br = new BufferedReader(fr);
                            String s;
                            while (( s = br.readLine()) != null) {
                                words=s.split("\\r?\\n");
                                for (String word : words) {
                                    if(word.equals(subjectencoded)){
                                        duplicate++;
                                    }
                                }
                            }
                            fr.close();
                            if(duplicate != 0){
                                continue;
                            }else{
                                FileWriter fw = null;
                                BufferedWriter bw = null;
                                PrintWriter pw = null;
                                try {
                                    // Write new email encode subject to encode file
                                    fw = new FileWriter(rb.getString("encode_file_path"), true);
                                    bw = new BufferedWriter(fw);
                                    pw = new PrintWriter(bw);
                                    pw.println(subjectencoded);
                                    pw.flush();
                                    // show email data on sysout
                                    System.out.printf(rb.getString("sysout_format"), message.getFrom()[0], formattedDate, message.getSubject(), paragraphs.text());
                                } finally {
                                    try {
                                        pw.close();
                                        bw.close();
                                        fw.close();
                                    } catch (IOException io) {}
                                }
                            }
                        }
                    }
                }
            }
            emailFolder.close(false);
            emailStore.close();
        } catch (NoSuchProviderException nspe) {
            nspe.printStackTrace();
        } catch (MessagingException me) {
            me.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void main(String... args) throws IOException {
        receiveMail(rb.getString("pop_server_username"), rb.getString("pop_server_password"));
    }
}
