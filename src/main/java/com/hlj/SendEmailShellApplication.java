package com.hlj;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@SpringBootApplication
public class SendEmailShellApplication implements CommandLineRunner{
	
	private static Logger logger = LoggerFactory.getLogger(SendEmailShellApplication.class);
	private static final String DEFAULT_ENCODING = "utf-8";
	@Autowired
	private JavaMailSender mailSender;
	@Value("${email.sender.default}")
	private String defaultSender;

	public static void main(String[] args) {
		SpringApplication.run(SendEmailShellApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		//-from -to -subject -body -attach
		logger.info(Arrays.toString(args));
		Map<String,String> map = new HashMap<>();
		String key = null;
		for (int i = 0; i < args.length; i++) {
			if(args[i].startsWith("-")){
				key = args[i].replaceFirst("-+", ""); //去除前缀 -from --> from
				map.put(key, "help".equals(key)?"":null);
			}else{
				map.put(key, (map.get(key)==null?"":map.get(key)+" ")+args[i]);
			}
		}
		if(map.get("help")!=null){
			logger.info("Usage: -from sender(默认为no-reply@helijia.com) -to receiver（必填） -subject 主题（必填） -body 正文 -attach 附件");
			outputHelpInfo();
			return;
		}
		if(map.get("to")==null||map.get("subject")==null){
			logger.warn("必须指定邮件接收人（-to）和主题(-subject)");
			System.out.println("必须指定邮件接收人（-to）和主题(-subject)");
			outputHelpInfo();
			return;
		}
		sendMail(map);
	}

	private void outputHelpInfo() throws IOException {
		Resource resource = new ClassPathResource("usage.txt");
		BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
		reader.lines().forEach(System.out::println);
	}
	
	public void sendMail(Map<String,String> map) {
		logger.info(map.toString());
		try {
			MimeMessage msg = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg, true, DEFAULT_ENCODING);

			helper.setFrom(Optional.ofNullable(map.get("from")).orElse(defaultSender));
			helper.setTo(map.get("to").split(","));
			helper.setSubject(map.get("subject"));
			String content = Optional.ofNullable(map.get("body")).orElse("");
			helper.setText(content , false);
			String attach = map.get("attach");
			if(attach!=null){
				File file = Paths.get(attach).toFile();
				helper.addAttachment(attach, file);
			}
			mailSender.send(msg);
			System.out.println("发送邮件成功");
		} catch (MessagingException e) {
			logger.error("构造邮件失败", e);
		} catch (Exception e) {
			logger.error("发送邮件失败", e);
		}
	}

}
