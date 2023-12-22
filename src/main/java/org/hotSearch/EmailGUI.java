package org.hotSearch;

import cn.hutool.json.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.hotSearch.Main.sendEmail;

public class EmailGUI extends JFrame {
    private JTextField senderEmailField;
    private JPasswordField authCodeField;
    private JTextField receiverEmailField;
    private JTextField mailHostField;
    private JTextField senderName;
    private static final String CONFIG_FILE_PATH = "email_Setting.json";
/**
 * @description TODO
 * @return
 * @author Albert_Luo
 * @date 2023/12/22 12:01
 */
    public EmailGUI() {
        // 设置窗口标题
        super("微博热搜拉取");
        // 设置外观为Nimbus
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }

        // 设置窗口关闭时的默认操作
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 创建面板
        JPanel panel = new JPanel(new GridLayout(5, 2));

        // 添加组件到面板
        panel.add(new JLabel("发件人邮箱:"));
        senderEmailField = new JTextField();
        senderEmailField.setToolTipText("输入发件人的邮箱");
        panel.add(senderEmailField);

        panel.add(new JLabel("输入授权码:"));
        authCodeField = new JPasswordField();
        authCodeField.setToolTipText("输入授权码");
        panel.add(authCodeField);

        panel.add(new JLabel("收件人邮箱:"));
        receiverEmailField = new JTextField();
        receiverEmailField.setToolTipText("输入收件人的邮箱（支持多个）示例：19222299@qq.com,22222270@qq.com");
        panel.add(receiverEmailField);

        panel.add(new JLabel("邮箱服务:"));
        mailHostField = new JTextField();
        mailHostField.setToolTipText("示例：smtp.163.com");
        panel.add(mailHostField);

        JButton submitButton = new JButton("提交");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitButtonClicked();
            }
        });
        panel.add(submitButton);

        // 将面板添加到窗口
        add(panel);

        // 设置窗口大小和可见性
        setSize(400, 200);
        setLocationRelativeTo(null); // 将窗口放置在屏幕中央
        setVisible(true);
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(CONFIG_FILE_PATH);
        if (configFile.exists()) {
            // 读取JSON文件内容
            String jsonContent = cn.hutool.core.io.FileUtil.readString(configFile, "UTF-8");

            // 解析JSON字符串为JSONObject
            JSONObject jsonObject = JSONUtil.parseObj(jsonContent);

            // 从 JSONObject 中读取值并设置到对应的输入框中
            senderEmailField.setText(jsonObject.getStr("sender"));
            authCodeField.setText(jsonObject.getStr("mail_pass"));
            // 获取 receivers 字段值（多个邮箱地址，以逗号分隔）
            String receivers = jsonObject.getStr("receivers");
            // 处理 receivers 字段值，去除中括号并替换双引号
            receivers = receivers.replace("[\"", "").replace("\"]", "").replace("\"", "");
            // 设置到 receiverEmailField 输入框中
            receiverEmailField.setText(receivers);
            mailHostField.setText(jsonObject.getStr("mail_host"));
        }
    }

    private boolean submitButtonClicked() {
        String senderEmail = senderEmailField.getText();
        String authCode = new String(authCodeField.getPassword());
        String receiverEmail = receiverEmailField.getText();
        String mailHost = mailHostField.getText();
        //String nickName = senderName.getText();

        // 格式校验，这里假设多个邮箱地址以逗号分隔
        if (!isValidEmailList(receiverEmail)) {
            JOptionPane.showMessageDialog(this, "收件人邮箱格式不规范！", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (!isValidEmail(senderEmail)) {
            JOptionPane.showMessageDialog(this, "请输入正确的邮箱格式！", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // 生成JSON字符串
        String json = generateJson(senderEmail, authCode, receiverEmail, mailHost);
        // 获取当前执行程序的位置
        String currentDir = System.getProperty("user.dir");
       /* // 选择保存JSON文件的位置
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save JSON File");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            // 获取用户选择的文件
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();

            // 将JSON字符串写入文件
            try (FileWriter fileWriter = new FileWriter(filePath)) {
                fileWriter.write(json);
                fileWriter.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }*/

        // 构建文件路径，这里假设文件名为config.json
        String filePath = currentDir + File.separator + CONFIG_FILE_PATH;

        // 将JSON字符串写入文件
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(json);
            fileWriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        try {
            String s = sendEmail();
            JOptionPane.showMessageDialog(this, "完成配置:\n" + filePath + "执行结果\n" + s + "\n...");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        // 创建一个 Timer，在3秒后执行关闭提示框的操作
        Timer timer = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.getRootFrame().dispose(); // 关闭所有JOptionPane
            }
        });
        timer.setRepeats(false); // 只执行一次
        // 启动 Timer
        timer.start();
        return true;
    }

    // 单个邮箱地址的格式校验方法，你可能需要更复杂的逻辑来验证邮箱格式
    private boolean isValidEmail(String email) {
        // 简单示例，你可能需要更复杂的逻辑来验证邮箱格式
        return email.matches("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    }

    // 格式校验方法，假设多个邮箱地址以逗号分隔
    private boolean isValidEmailList(String emailList) {
        String[] emails = emailList.split("\\s*,\\s*");
        for (String email : emails) {
            if (!isValidEmail(email.trim())) {
                return false;
            }
        }
        return true;
    }

    private String generateJson(String senderEmail, String authCode, String receiverEmail, String mailHost) {
        List<String> receiversList = Arrays.asList(receiverEmail.split("\\s*,\\s*"));

        // 构建JSON字符串
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        jsonBuilder.append("  \"mail_host\": \"").append(mailHost).append("\",\n");
        jsonBuilder.append("  \"mail_user\": \"").append(senderEmail.split("@")[0]).append("\",\n");
        jsonBuilder.append("  \"mail_pass\": \"").append(authCode).append("\",\n");
        jsonBuilder.append("  \"sender\": \"").append(senderEmail).append("\",\n");
        jsonBuilder.append("  \"receivers\": ").append(toJsonArray(receiversList)).append("\n");
        jsonBuilder.append("}");

        return jsonBuilder.toString();
    }

    private String toJsonArray(List<String> list) {
        // 构建JSON数组字符串
        StringBuilder jsonArray = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            String[] addresses = list.get(i).split("\\s*,\\s*");
            for (int j = 0; j < addresses.length; j++) {
                jsonArray.append("\"").append(addresses[j]).append("\"");
                if (j < addresses.length - 1) {
                    jsonArray.append(", ");
                }
            }
            if (i < list.size() - 1) {
                jsonArray.append(", ");
            }
        }
        jsonArray.append("]");
        return jsonArray.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new EmailGUI();
            }
        });
    }
}
