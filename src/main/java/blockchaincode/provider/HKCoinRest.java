package blockchaincode.provider;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import blockchaincode.service.IHKCoinService;
import blockchaincode.service.impl.HKCoinServiceImpl;
import blockchaincode.utils.JsonParseUtil;
import blockchaincode.utils.WebUtils;

import com.sunsheen.jfids.das.common.web.BaseResource;
import com.sunsheen.jfids.das.core.annotation.Bean;
import com.sunsheen.jfids.das.core.annotation.Describe;

/**
 * 核格构件币rest接口
 * @author WangSong
 *
 */
@Bean("HKCoin-REST")
@Path("/hearken-coin")
public class HKCoinRest extends BaseResource{
	private static  Logger logger = LoggerFactory.getLogger(HKCoinRest.class);
	//将request转换成json串
	private String req2Json() throws IOException{
		// 读取请求内容
        BufferedReader br = new BufferedReader(new InputStreamReader(servletRequest.getInputStream()));
        String line = null;
        StringBuilder sb = new StringBuilder();
        while((line = br.readLine())!=null){
            sb.append(line);
        }
        // 将资料解码
        String reqBody = sb.toString();
        return reqBody;
	}
	
	
	/**
	 * 初始化账户
	 * @return
	 */
	@Describe("初始化账户")
	@Path("/init-account")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public String initAccount() {
		IHKCoinService service = new HKCoinServiceImpl();
		try {		
			Map<String,Object> map = JsonParseUtil.json2Map(req2Json());
			String username = "";
			for(Map.Entry<String, Object> result : map.entrySet()){
				if(result.getKey().equals("username")){
					username = String.valueOf(result.getValue());
				}
			}
			String proPath = System.getProperty("user.dir");//当前项目路径地址
			/** windows上目录 **/
//			String realPath = proPath.concat(File.separator +"src"+
//					File.separator +"main"+ File.separator +"resources");
			/** Linux上目录 **/
			String realPath = (proPath.substring(0,proPath.indexOf("bin"))).concat("app"); //xxxx/bin/command
			service.initAccount(username, realPath);
		} catch (Exception e) {
			e.printStackTrace();
			return WebUtils.responseMsg(500,e.getMessage());
		}
		return WebUtils.responseMsg("初始化用户积分账户成功！");
	}
	
	/**
	 * 增加代币
	 * @return
	 */
	@Describe("当前账户增加代币")
	@POST
	@Path("/award")
	@Produces(MediaType.APPLICATION_JSON)
	public String award(){
		IHKCoinService service = new HKCoinServiceImpl();
		String username = null,amount = null;//被奖励用户、奖励金额
		//調用方法
		try {
			Map<String,Object> map = JsonParseUtil.json2Map(req2Json());
			for(Map.Entry<String, Object> result : map.entrySet()){
				if(result.getKey().equals("username")){
					username = String.valueOf(result.getValue());
				}else if(result.getKey().equals("amount")){
					amount = String.valueOf(result.getValue());
				}
			}
			String proPath = System.getProperty("user.dir");//当前项目路径地址
			/** windows上目录 **/
//			String realPath = proPath.concat(File.separator +"src"+
//					File.separator +"main"+ File.separator +"resources");
			/** Linux上目录 **/
			String realPath = (proPath.substring(0,proPath.indexOf("bin"))).concat("app");
			service.award(username, amount, realPath);
		} catch (Exception e) {
			e.printStackTrace();
			return WebUtils.responseMsg(500,e.getMessage());
		}
		
		return WebUtils.responseMsg(amount + "积分已经增加到："+username +"的账户");
	}
	
	/**
	 * 减少代币
	 * @return
	 */
	@Describe("当前账户扣除代币")
	@POST
	@Path("/recycle")
	@Produces(MediaType.APPLICATION_JSON)
	public String recycle(){
		IHKCoinService service = new HKCoinServiceImpl();
		String username = null,amount = null;
		//調用方法
		try {
			Map<String,Object> map = JsonParseUtil.json2Map(req2Json());
			for(Map.Entry<String, Object> result : map.entrySet()){
				if(result.getKey().equals("username")){
					username = String.valueOf(result.getValue());
				}
				if(result.getKey().equals("amount")){
					amount = String.valueOf(result.getValue());
				}
			}
			String proPath = System.getProperty("user.dir");//当前项目路径地址
			/** windows上目录 **/
//			String realPath = proPath.concat(File.separator +"src"+
//					File.separator +"main"+ File.separator +"resources");
			/** Linux上目录 **/
			String realPath = (proPath.substring(0,proPath.indexOf("bin"))).concat("app");
			service.recycle(username, amount, realPath);
		} catch (Exception e) {
			e.printStackTrace();
			return WebUtils.responseMsg(500,e.getMessage());
		}
		
		return WebUtils.responseMsg(amount + "积分已经从："+username +"的账户扣除");
	}
	
	/**
	 * 查询当前用户币
	 * @return
	 */
	@Describe("当前用户构件币数量")
	@Path("/user-account")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String coinInfo() {
		IHKCoinService service = new HKCoinServiceImpl();
		Map result = null;
		try {
			String username = servletRequest.getParameter("username");
			String proPath = System.getProperty("user.dir");//当前项目路径地址
			/** windows上目录 **/
//			String realPath = proPath.concat(File.separator +"src"+
//					File.separator +"main"+ File.separator +"resources");
			/** Linux上目录 **/
			String realPath = (proPath.substring(0,proPath.indexOf("bin"))).concat("app");
			result = service.getUserAccount(username, realPath);
		} catch (Exception e) {
			e.printStackTrace();
			return WebUtils.responseMsg(500,e.getMessage());
		}
		if(result.keySet().contains(500)){
			return WebUtils.responseMsg(500,result.get(500));
		}
		return WebUtils.responseMsg(result.get(200));
	}
	
	@Describe("注册管理员账户")
	@Path("/admin")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public String admin() {
		HKCoinServiceImpl service = new HKCoinServiceImpl();
		String username = null;
		try {
			String json = req2Json();
			Map<String,Object> map = new HashMap();
			if(null != json)
				map = JsonParseUtil.json2Map(json);
			for(Map.Entry<String, Object> result : map.entrySet()){
				if(result.getKey().equals("username")){
					username = String.valueOf(result.getValue());
				}
			}
			String proPath = System.getProperty("user.dir");//当前项目路径地址
			/** windows上目录 **/
//			String realPath = proPath.concat(File.separator +"src"+
//					File.separator +"main"+ File.separator +"resources");
			/** Linux上目录 **/
			String realPath = (proPath.substring(0,proPath.indexOf("bin"))).concat("app");
			service.admin(username, realPath);
		} catch (Exception e) {
			e.printStackTrace();
			return WebUtils.responseMsg(500,e.getMessage());
		}
		return WebUtils.responseMsg("注册管理员成功");
	}
	
	@Describe("初始化币池")
	@Path("/initCurrency")
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public String initCurrency() {
		HKCoinServiceImpl service = new HKCoinServiceImpl();
		String username = null;
		try {
			Map<String,Object> map = JsonParseUtil.json2Map(req2Json());
			for(Map.Entry<String, Object> result : map.entrySet()){
				if(result.getKey().equals("username")){
					username = String.valueOf(result.getValue());
				}
			}
			String proPath = System.getProperty("user.dir");//当前项目路径地址
			/** windows上目录 **/
//			String realPath = proPath.concat(File.separator +"src"+
//					File.separator +"main"+ File.separator +"resources");
			/** Linux上目录 **/
			String realPath = (proPath.substring(0,proPath.indexOf("bin"))).concat("app");
			service.initCurrency(username, realPath);
		} catch (Exception e) {
			e.printStackTrace();
			return WebUtils.responseMsg(500,e.getMessage());
		}
		return WebUtils.responseMsg("币池创建成功");
	}
	
	/**
	 * 查询构件币的所有记录
	 * @return
	 */
	@Describe("构件币的所有记录")
	@Path("/coin-history")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String coinHistory() {
		IHKCoinService service = new HKCoinServiceImpl();
		Map result = null;
		try {
			String proPath = System.getProperty("user.dir");//当前项目路径地址
			/** windows上目录 **/
//			String realPath = proPath.concat(File.separator +"src"+
//					File.separator +"main"+ File.separator +"resources");
			/** Linux上目录 **/
			String realPath = (proPath.substring(0,proPath.indexOf("bin"))).concat("app");
			result = service.coinHistory(realPath);
		} catch (Exception e) {
			e.printStackTrace();
			return WebUtils.responseMsg(500,e.getMessage());
		}
		if(result.keySet().contains(500)){
			return WebUtils.responseMsg(500,result.get(500));
		}
		return WebUtils.responseMsg(result.get(200));
	}
	
	@Describe("查询指定用户构件币记录")
	@Path("/user-coin-history")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String userCoinHistory() {
		IHKCoinService service = new HKCoinServiceImpl();
		Map result = null;
		try {
			String username = servletRequest.getParameter("username");
			String proPath = System.getProperty("user.dir");//当前项目路径地址
			/** windows上目录 **/
//			String realPath = proPath.concat(File.separator +"src"+
//					File.separator +"main"+ File.separator +"resources");
			/** Linux上目录 **/
			String realPath = (proPath.substring(0,proPath.indexOf("bin"))).concat("app");
			result = service.userCoinHistory(username, realPath);
		} catch (Exception e) {
			e.printStackTrace();
			return WebUtils.responseMsg(500,e.getMessage());
		}
		if(result.keySet().contains(500)){
			return WebUtils.responseMsg(500,result.get(500));
		}
		return WebUtils.responseMsg(result.get(200));
	}
	
}
