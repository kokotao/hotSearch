package org.hotSearch;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class Main {
    static String mailHost = null;
    static String mailUser = null;
    static String mailPass = null;
    static String sender = null;
    static JSONArray receivers = null;

    public static void main(String[] args) throws FileNotFoundException {
        String s = sendEmail();
        System.out.println(s);
    }

    /**
     * @return java.lang.String
     * @description TODO 获取热搜数据
     * @author Albert_Luo
     * @date 2023/12/21 15:16
     */
    public static String getHotSearchData() {
        String newsContent = "";
        String attachment = "";
        try {
            String url = "https://weibo.com/ajax/side/hotSearch";
            String jsonData = fetchDataFromUrl(url);
            //save data to json
            String hotSearchContent = "新浪热搜榜单.txt";
            //convert data
            JSONObject parse = (JSONObject) JSON.parse(jsonData);
            String data = parse.getString("data");
            JSONObject jsonObject = JSON.parseObject(data);
            String realtime = jsonObject.getString("realtime");
            JSONArray jsonArray = JSON.parseArray(realtime);
            // 遍历 JSON 数组
            for (int i = 0; i < jsonArray.size(); i++) {
                // 获取数组中的每个对象
                JSONObject item = jsonArray.getJSONObject(i);

                // 获取对象中的特定键的值
                String note = item.getString("note");
                int rank = item.getIntValue("rank");
                String iconDescColor = item.getString("icon_desc_color");
                String rawHot = item.getString("raw_hot");
                // 拼接链接
                String noteLink = "https://s.weibo.com/weibo?q=%23" + note + "%23";
                newsContent += "<div style='font-family: Arial, sans-serif; font-size: 15px; margin-bottom: 5px;'>"
                        + "<strong></strong>热度排行：" + rank
                        + " <a href='" + noteLink + "' style='color: " + iconDescColor + "; text-decoration: none;'>" + note + "  当前热度：<span style='color:#0000FF'>" + rawHot + "</sapn><br></a>"
                        + "</br></div>";
                attachment += "热度排行：" + rank + "\n" + note + "当前热度：" + rawHot + "\n";
            }
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(hotSearchContent));
            bufferedWriter.write(attachment);
            bufferedWriter.flush();
            bufferedWriter.close();
            System.out.println("getHotSearchData执行完毕");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newsContent;
    }

    private static String fetchDataFromUrl(String url) throws IOException {
        StringBuilder jsonData = new StringBuilder();

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        // 设置请求头（模拟浏览器请求）
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");

        // 获取输入流
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonData.append(line);
            }
        }

        return jsonData.toString();
    }

    public static String sendEmail() throws FileNotFoundException {
        emailJson();
        // 你的邮箱和密码
        final String username = sender;
        final String password = mailPass;
        // 邮件接收者的邮箱地址
        String[] toEmails = (String[]) receivers.toArray(new String[0]);

        // 邮件主题
        String subject = "热搜数据邮件";
        String newContent = getHotSearchData();
        // 邮件配置
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
//        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.host", mailHost); // 以Gmail为例，根据实际情况更改
        // props.put("mail.smtp.port", "465");
        // 创建Session对象
        Session session;
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        String html_News = null;
        if (!newContent.isEmpty()) {
            html_News = "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "</head>\n" +
                    "                         <body style=\"text-align: center;width: 500px;background-color: #fbf6ee8c;\">\n" +
                    "                           <p style=\"color:#0000ff8c;text-align: center;font-size: 18px;\"><strong>新闻热度排行榜</strong></p>\n" +
                    "                            <div style=\"margin-bottom: 5px;padding: 2px;margin: 2px;background: #f0f8ff6e;\">" + newContent + "</div>\n" +
                    "                         </body>\n" +
                    "                       </html>";

        } else {
            html_News = "<html>\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "</head>\n" +
                    "                         <body style=\"text-align: center;width: 500px;background-color: #fbf6ee8c;\">\n" +
                    "                           <p style=\"color:#0000ff8c;text-align: center;font-size: 18px;\"><strong>新闻热度排行榜</strong></p>\n" +
                    "                            <div style=\"margin-bottom: 5px;padding: 2px;margin: 2px;background: #f0f8ff6e;\">暂无热榜，有可能程序出现问题了</div>\n" +
                    "                         </body>\n" +
                    "                       </html>";
        }
        try {
            // 创建MimeMessage对象
            Message message = new MimeMessage(session);
            // Message message = new MimeMultipart(session);
            Multipart multipart = new MimeMultipart();
            // 设置发件人地址
            message.setFrom(new InternetAddress(username));
            // 设置多个收件人地址
            InternetAddress[] toAddresses = new InternetAddress[toEmails.length];
            for (int i = 0; i < toEmails.length; i++) {
                toAddresses[i] = new InternetAddress(toEmails[i]);
            }
            // 设置收件人地址
            message.setRecipients(Message.RecipientType.TO, toAddresses);
            // 设置邮件主题
            message.setSubject(subject);
// 添加文本部分
            BodyPart textBodyPart = new MimeBodyPart();
            textBodyPart.setContent(html_News, "text/html;charset=UTF-8");
            multipart.addBodyPart(textBodyPart);
            // 添加附件部分
            BodyPart attachmentBodyPart = new MimeBodyPart();
            String currentDirectory = System.getProperty("user.dir");
            String attachment = FileUtil.normalize(currentDirectory + "/新浪热搜榜单.txt");
            DataHandler handler = new DataHandler(new FileDataSource(attachment));
            attachmentBodyPart.setDataHandler(handler);
            String fileName = MimeUtility.encodeWord("新浪热搜榜单.txt", "utf-8", "B");
            attachmentBodyPart.setFileName(fileName); // 设置附件的文件名
            multipart.addBodyPart(attachmentBodyPart);
            //设置内容编码，防止发送的内容中文乱码。
            message.setContent(multipart);
            // 发送邮件
            Transport.send(message);

            return "邮件发送成功";
        } catch (MessagingException e) {
            e.printStackTrace();
            return "邮件发送失败！" + e.getMessage();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void emailJson() throws FileNotFoundException {
        String currentDirectory = System.getProperty("user.dir");
        // 构建 JSON 文件路径
        String jsonFilePath = FileUtil.normalize(currentDirectory + "/email_Setting.json");
        // JSON 文件路径
        //  String jsonFilePath = "email_Setting.json";
// 从文件读取 JSON 数据
        String jsonStr = FileUtil.readString(jsonFilePath, "UTF-8");
        JSONObject parse = (JSONObject) JSONObject.parse(jsonStr);

        // 获取各个字段的值
        mailHost = parse.getString("mail_host");
        mailUser = parse.getString("mail_user");
        mailPass = parse.getString("mail_pass");
        sender = parse.getString("sender");
        String receivers1 = parse.getString("receivers");
        // 获取收件人数组
        receivers = (JSONArray) JSONArray.parse(receivers1);

    }
}
