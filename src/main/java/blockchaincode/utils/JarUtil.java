package blockchaincode.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * jar包压缩跟解压工具类
 * @author WangSong
 *
 */
public class JarUtil {
	private static Logger log =  LoggerFactory.getLogger(JarUtil.class);

	
	private JarUtil(){}
	
	/**
	 * 复制jar by JarFile
	 * @param src
	 * @param des
	 * @throws IOException
	 */
    public static void copyJarByJarFile(File src , File des) throws IOException{
        //重点
        JarFile jarFile = new JarFile(src);
        Enumeration<JarEntry> jarEntrys = jarFile.entries();
        JarOutputStream jarOut = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(des)));
        byte[] bytes = new byte[1024];
        
        while(jarEntrys.hasMoreElements()){
            JarEntry entryTemp = jarEntrys.nextElement();
            jarOut.putNextEntry(entryTemp);
            BufferedInputStream in = new BufferedInputStream(jarFile.getInputStream(entryTemp));
            int len = in.read(bytes, 0, bytes.length);
            while(len != -1){
                jarOut.write(bytes, 0, len);
                len = in.read(bytes, 0, bytes.length);
            }
            in.close();
            jarOut.closeEntry();
            log.info("复制完成: " + entryTemp.getName());
        }
        log.error("复制完成! ");
        jarOut.finish();
        jarOut.close();
        jarFile.close();
    }
	
	/**
	 * 解压jar文件by JarFile
	 * @param src
	 * @param desDir
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static void unJarByJarFile(File src , File desDir) throws Exception{
        JarFile jarFile = new JarFile(src);
        Enumeration<JarEntry> jarEntrys = jarFile.entries();
        if(!desDir.exists())	
        	desDir.mkdirs(); //建立用户指定存放的目录
        byte[] bytes = new byte[1024];    
        
        while(jarEntrys.hasMoreElements()){
            ZipEntry entryTemp = jarEntrys.nextElement();
            File desTemp = new File(desDir.getAbsoluteFile() + File.separator + entryTemp.getName());
            
            if(entryTemp.isDirectory()){    //jar条目是空目录
                if(!desTemp.exists())
                	desTemp.mkdirs();
                log.info("makeDir" + entryTemp.getName());
            }else{    //jar条目是文件
                //因为manifest的Entry是"META-INF/MANIFEST.MF",写出会报"FileNotFoundException"
                File desTempParent = desTemp.getParentFile();
                if(!desTempParent.exists())
                    desTempParent.mkdirs();
                BufferedInputStream in = new BufferedInputStream(jarFile.getInputStream(entryTemp));
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(desTemp));
                
                int len = in.read(bytes, 0, bytes.length);
                while(len != -1){
                    out.write(bytes, 0, len);
                    len = in.read(bytes, 0, bytes.length);
                }
                
                in.close();
                out.flush();
                out.close();
                
                log.info("解压完成: " + entryTemp.getName());
            }
        }
        jarFile.close();
        log.error("解压完成！" );
    }
    
}
