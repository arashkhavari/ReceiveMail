#### Receive email from POP3 mail server  
Root cause : POP3 not get unseen message or email folder.  

##### Configuration  
Absolute path ``` /opt/.env ``` :  
```bash
pop_server=pop.mail.server
pop_server_username=pop@mail.server
pop_server_password=password
last_hour=3
receive_from=from@mail.server
search_subject=search_text
mysql_url=jdbc:mysql://db_address:3306/dbname
mysql_user=username
mysql_pass=password
table_name=table
```

###### Last hour  
Check email from spesific last hour cause POP3 not get unseen message.  

###### Search subject  
Search spesific word/words in mail subject.  

##### Sample
```bash
java -jar target/ReceiveMail-1.0-SNAPSHOT-jar-with-dependencies.jar
```
```output```  
```bash
Email from=test@mail.com at=2020-04-11 03:07:50 about=test_subject request=test_body
```



