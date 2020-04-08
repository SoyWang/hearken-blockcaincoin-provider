package blockchaincode.utils;

import java.util.Date;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;

import com.google.gson.Gson;

/**
 * 前后端数据转换工具类
 * @author WangSong
 *
 */
public class WebUtils {
	
	/**
	 * 把request对象中的请求参数封装到bean中
	 * @param request	http请求
	 * @param clazz		需要存入信息的对象class
	 * @return
	 */
	public static <T> T request2Bean(HttpServletRequest request,Class<T> clazz){
		try{
			T bean = clazz.newInstance();
			Enumeration e = request.getParameterNames();
			while(e.hasMoreElements()){
					String name = (String) e.nextElement(); 
					String value = request.getParameter(name);
					if(null != value && !"".equals(value)){
						//日期注册
						if(value.contains("-")){
							DateConverter converter = new DateConverter();
							converter.setPattern("yyyy-MM-dd");
							ConvertUtils.register(converter,Date.class);
						}
						//对象赋值
						BeanUtils.setProperty(bean, name, value);
					}
			}
			return bean;
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 响应到页面的数据
	 * @param code
	 * @param data
	 * @return
	 */
	public static String responseMsg(Integer code, Object data) {
		ResponseMsg msg =  new WebUtils().new ResponseMsg(data,code);
		return new Gson().toJson(msg);
	}
	
	/**
	 * 响应到页面的数据
	 * @param data
	 * @return
	 */
	public static String responseMsg(Object data) {
		ResponseMsg msg =  new WebUtils().new ResponseMsg(data);
		return new Gson().toJson(msg);
	}
	
	
	/**
	 *	内部类
	 */
	class ResponseMsg {

		private Object data;//返回信息
		private int status = 200;//状态码
		
		public ResponseMsg(Object result){
			this.data = result;
		}
		
		public ResponseMsg(Object result,int code){
			this.data = result;
			this.status = code;
		}
		
		public Object getData() {
			return data;
		}
		public void setData(Object result) {
			this.data = result;
		}
		
		public int getCode() {
			return status;
		}
		public void setCode(int code) {
			this.status = code;
		}
	}
	
}
