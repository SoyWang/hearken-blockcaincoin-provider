package blockchaincode.utils;

import org.apache.derby.impl.tools.sysinfo.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 解决：当项目打包成jar之后resources路径下面的证书文件访问不到
 * 思路：
 * 	1、运行时先复制一个jar
 * 	2、将复制的jar解压到jar文件目录
 * 	3、删除复制的jar跟解压的非证书文件夹
 * @author WangSong
 */
public class CryptoUtil {
	private static Logger log = LoggerFactory.getLogger(CryptoUtil.class);

	private CryptoUtil(){}


	public static void pass()throws Exception{
		/** 获取当前jar位置 **/
		URL url = Main.class.getClassLoader().getResource("crypto-config/");
		/** 当前需要是个jar文件 **/
		String protocol = url.getProtocol();//大概是jar
		if (!"jar".equalsIgnoreCase(protocol)) {
			log.error("没有以jar运行，不重复生成证书文件。");
			return;
		}
        //jar:file:/home/hkdas-123/HKDAS/app/blockchaincode-provider-1.0.0-jar-with-dependencies.jar!/
        String jarPath = url.toString().substring(0, url.toString().indexOf("!/") + 2);//url路径
        String oldJar = jarPath.substring(jarPath.indexOf("/"),jarPath.lastIndexOf("!/"));//jar路径
        String jarFolder = oldJar.substring(0,oldJar.indexOf("blockchaincode"));//jar包路径
        String copiedJar = jarFolder + "copied.jar";//复制jar包的目的jar文件路径
		//打印一下当前目录下的所有文件
		printfFiles(1,jarFolder);
        /** 清空一下之前复制的jar包、证书文件夹 **/
        resetFolder("copied",jarFolder);
        //打印一下当前目录下的文件
		printfFiles(2,jarFolder);
		/** 将jar复制一份到当前文件夹 **/
		JarUtil.copyJarByJarFile(new File(oldJar), new File(copiedJar));
		/** 解压复制的jar文件 **/
		JarUtil.unJarByJarFile(new File(copiedJar), new File(jarFolder));
		/** 删除--除了证书文件夹和jar文件的所有文件夹 **/
        deleteDirExcludeJarAndCrypto(jarFolder);
		//打印一下当前目录下的文件
		printfFiles(3,jarFolder);
	}


//	public static void main(String[] args) {
//		try{
//			//jar:file:D:/360极速浏览器下载/blockchaincode-provider-1.0.0-jar-with-dependencies.jar!/
//			String oldJar = "D:/360极速浏览器下载/blockchaincode-provider-1.0.0-jar-with-dependencies.jar";//jar路径
//			String jarFolder = oldJar.substring(0,oldJar.indexOf("blockchaincode"));//jar包路径
//			String copiedJar = jarFolder + "copied.jar";//复制jar包的目的jar文件路径
//			//打印一下当前目录下的所有文件
//			printfFiles(1,jarFolder);
//			/** 清空一下之前复制的jar包、证书文件夹 **/
//			resetFolder("copied",jarFolder);
//			//打印一下当前目录下的文件
//			printfFiles(2,jarFolder);
//			/** 将jar复制一份到当前文件夹 **/
//			JarUtil.copyJarByJarFile(new File(oldJar), new File(copiedJar));
//			/** 解压复制的jar文件 **/
//			JarUtil.unJarByJarFile(new File(copiedJar), new File(jarFolder));
//			/** 删除--除了证书文件夹和jar文件的所有文件夹 **/
//			deleteDirExcludeJarAndCrypto(jarFolder);
//			//打印一下当前目录下的文件
//			printfFiles(3,jarFolder);
//		}catch (Exception e){
//			e.printStackTrace();
//		}
//	}




	//打印一下当前目录下第一级目录文件
	private static void printfFiles(int i, String jarFolder) {
		File file = new File(jarFolder);
		String[] content = file.list();//取得当前目录下所有文件和文件夹
		switch (i){
			case 1:{
				logPrint(content,"原来的目录下第一级文件：",jarFolder);
				break;
			}
			case 2:{
				logPrint(content,"重置后目录下第一级文件：",jarFolder);
				break;
			}
			case 3:{
				logPrint(content,"删除后目录下第一级文件：",jarFolder);
				break;
			}
		}
	}
	private static void logPrint(String[] content,String type,String filePath){
		for(String name : content){
			File file = new File(filePath, name);
			long time = file.lastModified();;
			String format = new SimpleDateFormat("yyyy年MM月dd日HH点mm分ss秒").format(new Date(time));
			log.error(type+name+"，文件创建时间："+format);
			System.out.println(type+name+"，文件创建时间："+format);
		}
	}

	//清除上次解压和压缩生成的文件(copied.jar、证书文件夹)
    private static boolean resetFolder( String copiedJarName, String path) {
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
                resetFolder(copiedJarName,temp.getAbsolutePath());//递归调用，删除目录里的内容
                temp.delete();//删除空目录
            }else{
                //不删除当前上传到服务器的jar文件
                if(name.contains(".jar") && !name.contains(copiedJarName))
                    continue;
                //直接删除所有文件
                deleteFile(temp);
            }
        }
        return true;
    }

    //清空文件夹下除了证书文件夹和jar文件的所有文件
	private static boolean deleteDirExcludeJarAndCrypto(String path){
		File file = new File(path);
		if(!file.exists()){//判断是否待删除目录是否存在
//			System.err.println("The dir are not exists!");
			log.error("The dir are not exists!"+file.getAbsolutePath());
			return false;
		}

		String[] content = file.list();//取得当前目录下所有文件和文件夹
		for(String name : content){
			File temp = new File(path, name);
			if(temp.isDirectory()){
				//如果是证书文件，不删除
				if(name.contains("crypto-config"))
					continue;

                deleteDirExcludeJarAndCrypto(temp.getAbsolutePath());//递归调用，删除目录里的内容
				temp.delete();//删除空目录
			}else{
				//如果是jar包，不删除
				if(name.contains(".jar"))
					continue;
				deleteFile(temp);
			}
		}
		return true;
	}

	/**
	 * 删除单个文件
	 *
	 * @param fileName
	 *            要删除的文件的文件名
	 * @return 单个文件删除成功返回true，否则返回false
	 */
	public static boolean deleteFile(File file) {
		String fileName = file.getName();
		// 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
		if (file.exists() && file.isFile()) {
			if (file.delete()) {
				System.out.println("删除单个文件" + fileName + "成功！");
				return true;
			} else {
				System.out.println("删除单个文件" + fileName + "失败！");
				log.error("删除单个文件" + fileName + "失败！ 文件路径："+file.getAbsolutePath());
				return false;
			}
		} else {
			System.out.println("删除单个文件失败：" + fileName + "不存在！");
			log.error("删除单个文件失败：" + fileName + "不存在！ 文件路径："+file.getAbsolutePath());
			return false;
		}
	}
}
