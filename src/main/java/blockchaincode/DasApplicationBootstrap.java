package blockchaincode;

import blockchaincode.utils.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blockchaincode.utils.TempCryptoFolderUtil;

import com.sunsheen.jfids.das.core.DasApplication;
import com.sunsheen.jfids.das.core.annotation.DasBootApplication;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * 当独立开发HKDAS应用时（Java工程、Maven工程），使用这种方式启动应用。
 * @author WangSong
 *
 */

@DasBootApplication()
public class DasApplicationBootstrap {
	private static Logger log = LoggerFactory.getLogger(DasApplicationBootstrap.class);

	public static void main(String[] args) {
		ExecutorService service = Executors.newSingleThreadExecutor();
		service.execute(new Runnable() {
			@Override
			public void run() {
				//将证书文件拷贝到项目同级目录下
				try {
					CryptoUtil.pass();
				} catch (Exception e) {
					e.printStackTrace();
					log.error("当前jar包目录の证书文件拷贝异常！", e);
					System.err.println("证书文件拷贝异常！");
				}
			}
		});
		service.shutdown();//任务执行完成之后关闭线程池
		//启动
		DasApplication.run(DasApplicationBootstrap.class, args);
	}


}
