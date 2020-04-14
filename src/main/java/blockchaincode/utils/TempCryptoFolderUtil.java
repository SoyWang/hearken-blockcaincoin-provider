package blockchaincode.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.derby.impl.tools.sysinfo.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 解决：当项目打包成jar之后resources路径下面的证书文件访问不到
 * 思路：
 * 	1、通过流读取当前项目下resources目录下的所有证书文件
 * 	2、在内存中创建一个临时文件夹
 * 	3、将resources中的所有证书文件拷贝到内存的临时文件夹中
 * 	4、持久化到jar包同级目录
 * @author WangSong
 *
 */
@Deprecated
public class TempCryptoFolderUtil {
	private static Logger log = LoggerFactory.getLogger(TempCryptoFolderUtil.class);

	/**
	 * 拷贝证书文件到jar包同级目录下
	 * @param cryptoFolderName	证书文件夹名称
	 * @throws Exception
	 */
	public static void copyFolder(String cryptoFolderName) throws Exception{
		//		/home/hkdas-123/HKDAS/bin/command/crypto-config
		URL url = Main.class.getClassLoader().getResource(cryptoFolderName+"/");
		//	jar:file:/home/hkdas-123/HKDAS/app/blockchaincode-provider-1.0.0-jar-with-dependencies.jar!/crypto-config/
		log.error("======================================URL:"+url.toString());

		//当前需要是个jar文件
		String protocol = url.getProtocol();//大概是jar
		if (!"jar".equalsIgnoreCase(protocol)) {
			log.error("当前文件不是一个jar");
			return;
		}

		String jarPath = url.toString().substring(0, url.toString().indexOf("!/") + 2);
		//	jar:file:/home/hkdas-123/HKDAS/app/blockchaincode-provider-1.0.0-jar-with-dependencies.jar!/
		log.error("======================================jarPath:"+jarPath);

		String cryptoPath = System.getProperty("user.dir");
		cryptoPath = (cryptoPath.substring(0,cryptoPath.indexOf("bin"))).concat("app");// /home/hkdas-123/HKDAS/app
		log.error("======================================cryptoPath:"+cryptoPath);

		//当前jar包目录下是否存在证书文件，已经存在的删除
		cryptoFolderCheck(cryptoPath,cryptoFolderName);

		URL jarURL = new URL(jarPath);
		JarURLConnection jarCon = (JarURLConnection) jarURL.openConnection();
		if(null == jarCon)
			return;
		JarFile jarFile = jarCon.getJarFile();
		//读取jar包中的证书文件
		Enumeration<JarEntry> jarEntrys = jarFile.entries();
		while (jarEntrys.hasMoreElements()) {
			JarEntry entry = jarEntrys.nextElement();
			String name = entry.getName();//当前目录名称
			//获取证书文件目录，并拷贝证书文件夹
			if (name.startsWith(cryptoFolderName+"/")) {
//				copy(Main.class.getClassLoader().getResourceAsStream(name),
//						cryptoPath +File.separator+ cryptoFolderName);
				log.error("当前为证书文件夹："+name);
				//这里卡住了。。。。。。。。。

			}
		}
	}


	//将输入流拷贝到指定目录
	private static void copy(InputStream input,String proPath) throws Exception{
		int index;
		byte[] bytes = new byte[1024];
		FileOutputStream downloadFile = new FileOutputStream(proPath);//文件写入的文件夹
		while ((index = input.read(bytes)) != -1) {
			downloadFile.write(bytes, 0, index);
			downloadFile.flush();
		}
		input.close();
		downloadFile.close();
	}

	//当前目录下证书文件夹校验
	private static void cryptoFolderCheck(String jarPath,String cryptoFolderName) {
		File folder = new File(jarPath +File.separator+ cryptoFolderName);
		if(!folder.exists())
			folder.mkdirs(); //	/home/hkdas-123/HKDAS/app目录下
		else
			deleteDir(folder.getAbsolutePath());
	}

	//清空文件夹下所有内容
	private static boolean deleteDir(String path){
		File file = new File(path);
		if(!file.exists()){//判断是否待删除目录是否存在
//			System.err.println("The dir are not exists!");
			log.error("The dir are not exists!"+file.getAbsolutePath());
			return false;
		}

		String[] content = file.list();//取得当前目录下所有文件和文件夹
		for(String name : content){
			File temp = new File(path, name);
			if(temp.isDirectory()){//判断是否是目录
				deleteDir(temp.getAbsolutePath());//递归调用，删除目录里的内容
				temp.delete();//删除空目录
			}else{
				if(!temp.delete()){//直接删除文件
//					System.err.println("Failed to delete " + name);
					log.error("Failed to delete " + name);
				}
			}
		}
		return true;
	}

	/*
	URL url = MyClass.class.getResource("resources/");
	if (url == null) {
		 // error - missing folder
	} else {
		File dir = new File(url.toURI());
		for (File nextFile : dir.listFiles()) {
			// Do something with nextFile
		}
	}
	 */

}
