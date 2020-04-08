package blockchaincode;

import com.sunsheen.jfids.das.core.DasApplication;
import com.sunsheen.jfids.das.core.annotation.DasBootApplication;

/**
 *
 * 当独立开发HKDAS应用时（Java工程、Maven工程），使用这种方式启动应用。
 * @author WangSong
 *
 */

@DasBootApplication() 
public class DasApplicationBootstrap {

	public static void main(String[] args) {
		DasApplication.run(DasApplicationBootstrap.class, args);
	}
	

}
