package com.ag777.util.remote.svn;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.ISVNOptions;
import org.tmatesoft.svn.core.wc.SVNChangelistClient;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;

import com.ag777.util.file.FileNioUtils;
import com.ag777.util.lang.collection.ListUtils;
import com.ag777.util.remote.svn.exception.SVNCheckoutException;
import com.ag777.util.remote.svn.model.BasicWithCertificateTrustedAuthenticationManager;

/**
 * svn操作工具类(对svnkit的二次封装)
 * 
 * <p>
 * 	需要jar包:
 * <ul>
 * <li>svnkit-1.9.3.jar</li>
 * <li>sqljet-1.1.11.jar</li>
 * <li>sequence-library-1.0.3.jar</li>
 * <li>antlr-runtime-3.5.2.jar</li>
 * </ul>
 * </p>
 * 各种client的基本操作可以参考dalao的文章:https://www.cnblogs.com/douJiangYouTiao888/p/6142300.html
 * 
 * @author ag777
 * @version create on 2018年12月28日,last modify at 2019年09月04日
 */
public class SvnUtils {
	
	private SvnUtils() {}
	
	/**
	 * 建立连接
	 * @param url
	 * @param account
	 * @param password
	 * @return
	 * @throws SVNException
	 */
	public static SVNRepository connect(String url, String account, String password) throws SVNException {
		return connect(getSvnUrl(url), account, password);
	}
	
	/**
	 * 建立连接
	 * @param url
	 * @param account
	 * @param password
	 * @return
	 * @throws SVNException
	 */
	public static SVNRepository connect(SVNURL url, String account, String password) throws SVNException {
		DAVRepositoryFactory.setup(); // 初始化
		ISVNAuthenticationManager authManager = new BasicWithCertificateTrustedAuthenticationManager(account,
				password); // 提供认证
		SVNRepository repos = SVNRepositoryFactory.create(url);
		repos.setAuthenticationManager(authManager); // 设置认证
		return repos;
	}
	
	/**
	 * 将url转化为SvnUrl对象
	 * @param url
	 * @return
	 * @throws SVNException
	 */
	public static SVNURL getSvnUrl(String url) throws SVNException {
		return SVNURL.parseURIEncoded(url); // 某目录在svn的位置，获取目录对应的URL。即版本库对应的URL地址
	}
	
	/**
	 * 获取对应日期的版本号
	 * @param repos
	 * @param date
	 * @return
	 * @throws SVNException
	 */
	public static long getReversion(SVNRepository repos, Date date) throws SVNException {
		return repos.getDatedRevision(date);
	}
	
	/**
	 * 获取svn版本日志数组
	 * @param repos
	 * @param startRevision
	 * @param endRevision
	 * @return
	 * @throws SVNException
	 */
	public static SVNLogEntry[] getSvnLogEntries(SVNRepository repos, long startRevision, long endRevision) throws SVNException {
		@SuppressWarnings("unchecked")
		Collection<SVNLogEntry> logEntries = repos.log(new String[]{""}, null,
				startRevision, endRevision, true, true);
		return logEntries.toArray(new SVNLogEntry[0]);
	}
	
	/**
	 * 根据版本日志数组获取版本号列表
	 * @param logEntries
	 * @return
	 */
	public static List<Long> getVersionList(SVNLogEntry[] logEntries) {
		List<Long> result = ListUtils.newArrayList();
		for (SVNLogEntry svnLogEntry : logEntries) {
			result.add(svnLogEntry.getRevision());
		}
		return result;
	}
	
	/**
	 * 根据起止日期获取版本号列表
	 * @param repos
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws SVNException
	 */
	public static List<Long> getVersionList(SVNRepository repos, Date startDate, Date endDate) throws SVNException {
		long startRevision = getReversion(repos, startDate);
		long endRevision = getReversion(repos, endDate);
		SVNLogEntry[] logEntries = getSvnLogEntries(repos, startRevision, endRevision);
		return getVersionList(logEntries);
	}
	
	
	/**
	 * 测试svn连接
	 * @param url
	 * @throws SVNException
	 */
	public static void testConnect(String url) throws SVNException {
		testConnect(getSvnUrl(url));
	}
	
	/**
	 * 测试svn连接
	 * @param url
	 * @param account
	 * @param password
	 * @throws SVNException
	 */
	public static void testConnect(SVNURL url) throws SVNException {
		SVNRepository r = connect(url, "", "");
		try{
			r.testConnection();
		} finally {
			r.closeSession();
			r = null;
		}
		
	}
	
	/**
	 * 检出某个版本的单个svn文件
	 * @param version 版本
	 * @param repos svn连接
	 * @param filePath 文件路径
	 * @param outFilePath 导出文件路径
	 * @return
	 * @throws SVNCheckoutException
	 */
	public long downLoadFileFromSVN(SVNRepository repos, long version, String filePath, String outFilePath) throws SVNCheckoutException {
		SVNNodeKind node = null;
		try {
			if(version == 0){
				version = repos.getLatestRevision();
			}
			node = repos.checkPath(filePath, version);
		} catch (SVNException e) {
			throw new SVNCheckoutException("SVN检测不到该文件:" + filePath, e);
		}
		if (node != SVNNodeKind.FILE) {
			throw new SVNCheckoutException(node.toString() + "不是文件");
		}
		SVNProperties properties = new SVNProperties();
		try {
			OutputStream outputStream = FileNioUtils.getOutputStream(outFilePath);
			repos.getFile(filePath, version, properties, outputStream);
			outputStream.close();
		} catch (SVNException e) {
			throw new SVNCheckoutException("获取SVN服务器中的" + filePath + "文件失败", e);
		} catch (IOException e) {
			throw new SVNCheckoutException("SVN check out file faild.", e);
		}
		return Long.parseLong(properties.getStringValue("svn:entry:revision"));
	}
	
	/**
	 * 获取clientManger
	 * @param account
	 * @param password
	 * @return
	 */
	public static SVNClientManager getClientManager(String account, String password) {
		ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
		//实例化客户端管理类
		return SVNClientManager.newInstance((DefaultSVNOptions) options, account, password);
	}
	
	/**
	 * 获取updateClient
	 * <p>
	 * 可以执行检出(checkout)，切换(switch)，导出(export)，更新(update)等操作
	 * </p>
	 * @param account
	 * @param password
	 * @return
	 */
	public static SVNUpdateClient getUpdateClient(String account, String password) {
		//实例化客户端管理类
		SVNClientManager ourClientManager = getClientManager(account, password);
		return ourClientManager.getUpdateClient();
	}
	
	/**
	 * 获取adminClient
	 * <p>
	 * 可以执行创建仓库(createRepository)等操作
	 * </p>
	 * @param account
	 * @param password
	 * @return
	 */
	public static SVNAdminClient getAdminClient(String account, String password) {
		//实例化客户端管理类
		SVNClientManager ourClientManager = getClientManager(account, password);
		return ourClientManager.getAdminClient();
	}
	
	/**
	 * 获取changelistClient
	 * @param account
	 * @param password
	 * @return
	 */
	public static SVNChangelistClient getChangelistClient(String account, String password) {
		//实例化客户端管理类
		SVNClientManager ourClientManager = getClientManager(account, password);
		return ourClientManager.getChangelistClient();
	}
	
	/**
	 * checkout最新版本
	 * @param url
	 * @param account
	 * @param password
	 * @param workPath
	 * @return 导出版本
	 * @throws SVNException
	 */
	public static long checkout(String url, String account, String password, String workPath) throws SVNException {
		//要把版本库的内容check out到的目录
		//FIle wcDir = new File("d:/test")
		File wcDir = new File(workPath);
		//通过客户端管理类获得updateClient类的实例。
		SVNUpdateClient updateClient = getUpdateClient(account, password);
		 //sets externals not to be ignored during the checkout
		updateClient.setIgnoreExternals(false);
		//执行check out 操作，返回工作副本的版本号。
		return updateClient.doCheckout(getSvnUrl(url), wcDir, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY,false);
	}
	
	/**
	 * 更新本地仓库到某个版本
	 * @param updateClient
	 * @param wcPath
	 * @param updateToRevision
	 * @return
	 * @throws SVNException
	 */
	public static long update(SVNUpdateClient updateClient, String wcPath,
			SVNRevision updateToRevision) throws SVNException {
 
		/*
		 * sets externals not to be ignored during the update
		 */
		updateClient.setIgnoreExternals(false);
 
		/*
		 * returns the number of the revision wcPath was updated to
		 */
		return updateClient.doUpdate(new File(wcPath), updateToRevision, SVNDepth.INFINITY, false, false);
	}
	
	
	/**
	 * Commit work copy's change to svn
	 * @param clientManager
	 * @param wcPath 
	 *			working copy paths which changes are to be committed
	 * @param keepLocks
	 *			whether to unlock or not files in the repository
	 * @param commitMessage
	 *			commit log message
	 * @return
	 * @throws SVNException
	 */
	public static SVNCommitInfo commit(SVNClientManager clientManager,
		File wcPath, boolean keepLocks, String commitMessage) throws SVNException {
		return clientManager.getCommitClient().doCommit(
				new File[] { wcPath }, keepLocks, commitMessage, null,
				null, false, false, SVNDepth.fromRecurse(true));
	}
	
	public static void main(String[] args) throws SVNException {
		String url = "";
		String account = "xxxx";
		String password = "xxx";
		String tempPath = "f:/test/";
//		SVNRepository repos = connect(url, account, password);
//		try {
//			Console.prettyLog(getSvnLogEntries(repos, 0, -1));
//		} finally {
//			repos.closeSession();
//		}
		long version = checkout(url, account, password, tempPath);
		System.out.println("检出版本["+version+"]至"+tempPath);
	}
	
}
