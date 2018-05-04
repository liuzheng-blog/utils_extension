package com.ag777.util.remote.mail;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import com.ag777.util.lang.StringUtils;
import com.ag777.util.lang.collection.ListUtils;
import com.ag777.util.lang.exception.Assert;
import com.sun.mail.util.MailConnectException;

/**
 * 邮件操作工具类
 * <p>
 * 	需要jar包:
 * <ul>
 * <li>javax.mail.xxx.jar</li>
 * </ul>
 * 最新包请从github上获取
 * https://github.com/javaee/javamail/releases
 * </p>
 * 
 * @author ag777
 * @version create on 2018年04月16日,last modify at 2018年04月17日
 */
public class MailUtils {

	private MailUtils() {}
	
	/**
	 * 测试连接
	 * @param smtpHost
	 * @param user
	 * @param password
	 * @return
	 */
	public static boolean testConnect(
			String smtpHost,
			String user,
			String password) {
		Transport transport = null;
		Session mailSession = null;
		try {
			Properties properties = new Properties();
			properties.setProperty("mail.smtp.auth", "true");// 提供验证
			properties.setProperty("mail.transport.protocol", "smtp");// 使用的协议					
			properties.setProperty("mail.smtp.host", smtpHost);	// 这里是smtp协议
			// 设置发送验证机制
			Authenticator auth = new Authenticator() {
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, password);
				}
			};
			mailSession = Session.getInstance(properties, auth);
			transport = mailSession.getTransport();
			transport.connect(smtpHost, 25, user, password);
			return true;
		} catch(MailConnectException ex) { //连接失败
//			ex.printStackTrace();
		} catch(AuthenticationFailedException ex) {	//账号密码错误
//			ex.printStackTrace();
		} catch (MessagingException ex) {	//其他异常
//			ex.printStackTrace();
		} finally {
			mailSession = null;
			if(transport != null) {
				try {
					transport.close();
				} catch (MessagingException e) {
				}
				transport = null;
			}
		}
		return false;
	}
	
	/**
	 * 发送邮件
	 * <p>
	 * 是否使用缓存
	 * ①是:创建session时会使用getDefaultInstance方法,
	 * javamail首先是从缓存中查找是否有properties存在 
		如果存在，则加载默认的properties 
		如果不存在才加载用户自己定义的properties,
		单例模式,里面的username和password属性是final型的，无法更改
		选择使用缓存会导致一个问题，比如你登录了一个邮箱，然后使用错误的密码再次登录依然能够成功发送邮件
		②否:创建session时会使用getInstance方法,每次都会创建一个新对象,可以避免①中的问题，但是会增大系统开销
	 * </p>
	 * 
	 * @param smtpHost 邮件服务器地址
	 * @param user 发件邮箱账号
	 * @param password 发件邮箱密码
	 * @param from 发件邮箱
	 * @param fromDisplay 邮件显示的发件人
	 * @param to 收件邮箱
	 * @param subject 主题
	 * @param content 邮件内容
	 * @param attachments n.（用电子邮件发送的）附件( attachment的名词复数 )
	 * @param useCache 是否使用缓存
	 * @return
	 */
	public static boolean send(
			String smtpHost,
			String user,
			String password,
			String from,
			String fromDisplay,
			String to,
			String subject,
			String content,
			File[] attachments,
			boolean useCache) throws IllegalArgumentException {
		Assert.notBlank(smtpHost, "邮件服务器地址不能为空");
		try {
			sendWithException(smtpHost, user, password, from, fromDisplay, to, subject, content, attachments, useCache);
			return true;
		} catch (UnsupportedEncodingException | MessagingException ex) {
//			ex.printStackTrace();
		}

		return false;
	}
	
	/**
	 * 发送邮件(伴随异常返回)
	 * <p>
	 * 详见send()方法
	 * </p>
	 * 
	 * @param smtpHost
	 * @param user
	 * @param password
	 * @param from
	 * @param fromDisplay
	 * @param to
	 * @param subject
	 * @param content
	 * @param attachments
	 * @param useCache
	 * @throws IllegalArgumentException 参数验证异常
	 * @throws MailConnectException 连接失败
	 * @throws AuthenticationFailedException 账号密码错误
	 * @throws MessagingException 其他异常
	 * @throws UnsupportedEncodingException InternetAddress转换失败(发件箱，收件箱)
	 */
	public static void sendWithException(
			String smtpHost,
			String user,
			String password,
			String from,
			String fromDisplay,
			String to,
			String subject,
			String content,
			File[] attachments,
			boolean useCache) throws IllegalArgumentException, MailConnectException, AuthenticationFailedException, MessagingException, UnsupportedEncodingException {
		
		/*参数验证*/
		Assert.notBlank(smtpHost, "邮件服务器地址不能为空");
		Assert.notBlank(from, "发件邮箱不能为空");
		Assert.notBlank(to, "收件邮箱不能为空");
		if(fromDisplay == null) {	//发送人默认为邮箱
			fromDisplay = from;
		}
		subject = StringUtils.emptyIfNull(subject);
		content = StringUtils.emptyIfNull(content);
		
		//验证附件存在性
		if(!ListUtils.isEmpty(attachments)) {
			for (File file : attachments) {
				Assert.notNull(file, "邮件附件文件不能为空");
				Assert.notExisted(file, "邮件附件文件不存在:" + file.getAbsolutePath());
			}
		}
		/*参数验证 end*/
		
		Transport transport = null;
		Session mailSession = null;
		try {
			// 设置java mail属性，并添入数据源
			Properties properties = new Properties();
			properties.setProperty("mail.smtp.auth", "true");// 提供验证
			properties.setProperty("mail.transport.protocol", "smtp");// 使用的协议					
			properties.setProperty("mail.smtp.host", smtpHost);	// 这里是smtp协议
			// 设置发送验证机制
			Authenticator auth = new Authenticator() {
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(user, password);
				}
			};
			// 建立一个默认会话
			if(useCache) {	//使用缓存
				mailSession = Session.getDefaultInstance(properties, auth);
			} else {	//不使用缓存
				mailSession = Session.getInstance(properties, auth);
			}
			
			MimeMessage msg = new MimeMessage(mailSession); // 创建MIME邮件对象
			MimeMultipart mp = new MimeMultipart();

			msg.setFrom(new InternetAddress(from, fromDisplay));

			// 设置收件者地址
			msg.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(to));
			msg.setSubject(MimeUtility.encodeText(StringUtils.emptyIfNull(subject), "gb2312",
					"B"));// 设置邮件的标题

			// 设置并处理信息内容格式转为text/html
			addContent(StringUtils.emptyIfNull(content), mp);
			// 设置邮件附件
			if (!ListUtils.isEmpty(attachments)) {
				addAttchments(attachments, mp);
			}

			msg.setContent(mp);
			msg.saveChanges();
			// 发送邮件
			transport = mailSession.getTransport();
			transport.connect(smtpHost, 25, user, password);
			transport.sendMessage(msg, new Address[] { new InternetAddress(
					to) });
			
		} catch(MailConnectException ex) { //连接失败
			throw ex;
		} catch(AuthenticationFailedException ex) {	//账号密码错误
			throw ex;
		} catch (MessagingException ex) {		//其他异常
			throw ex;
		} catch (UnsupportedEncodingException ex) {	//InternetAddress转换失败(发件箱，收件箱)
			throw ex;
		} finally {
			mailSession = null;
			if(transport != null) {
				try {
					transport.close();
				} catch (MessagingException e) {
				}
				transport = null;
			}
		}
	}
	
	/**
	 * 往邮件里添加邮件内容
	 * @param mailBody 邮件内容
	 * @param multipart 容器
	 * @throws MessagingException
	 */
	private static void addContent(String mailContent, Multipart multipart) throws MessagingException {
		BodyPart bp = new MimeBodyPart();
		bp.setContent(
				"<meta http-equiv=Content-Type content=text/html; charset=gb2312>"
						+ mailContent, "text/html;charset=GB2312");
		multipart.addBodyPart(bp);
	}
	
	/**
	 * 往邮件里添加附件
	 * @param attachments 附件数组
	 * @param multipart
	 * @return
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException 
	 */
	private static void addAttchments(File[] attachments, Multipart multipart) throws MessagingException, UnsupportedEncodingException {
		for (File file : attachments) {
			addAttchment(file, multipart);
		}
	}
	
	/**
	 * 往邮件里添加附件
	 * @param attachment 附件
	 * @param multipart
	 * @return
	 * @throws MessagingException
	 * @throws UnsupportedEncodingException 
	 */
	private static void addAttchment(File attachment, Multipart multipart) throws MessagingException, UnsupportedEncodingException {
		BodyPart bp = new MimeBodyPart();
		FileDataSource fileds = new FileDataSource(attachment);
		bp.setDataHandler(new DataHandler(fileds));
		bp.setFileName(MimeUtility.encodeText(fileds.getName()));
		multipart.addBodyPart(bp);
	}

	public static void main(String[] args) {
//		System.out.println(
//				testConnect(
//				"192.168.161.106", 
//				"test", "123456")
//				);
		
		try {
			sendWithException(
					"xx", 
					"xx", "xxxx", "test@test.com", null, "test@test.com", null, null, null, false);
			System.out.println("成功");
		} catch (IllegalArgumentException e) {
			System.out.println("参数异常");
			e.printStackTrace();
		} catch (MailConnectException e) {
			System.out.println("连接不上");
			e.printStackTrace();
		} catch (AuthenticationFailedException e) {
			System.out.println("账号密码错误");
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			System.out.println("地址转换失败");
			e.printStackTrace();
		} catch (MessagingException e) {
			System.out.println("其他异常");
			e.printStackTrace();
		}
	}
}
