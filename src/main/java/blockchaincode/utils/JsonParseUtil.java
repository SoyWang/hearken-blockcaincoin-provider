package blockchaincode.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.ListOrderedMap;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import net.sf.json.util.CycleDetectionStrategy;

/**
 * json工具
 * @author WangSong
 *
 */
public class JsonParseUtil {

	/**
	 * 將jsonArry字符串转换成map（里面可能是多个对象的情况）
	 * @param json
	 * @return
	 */
	public static List<Map<String, Object>> parseJSON2List(String json) {
        JSONArray jsonArr = JSONArray.fromObject(json);
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Iterator<JSONObject> it = jsonArr.iterator();
        while (it.hasNext()) {
            JSONObject json2 = it.next();
            list.add(parseJSON2Map(json2.toString()));
        }
        return list;
    }

    private static Map<String, Object> parseJSON2Map(String jsonStr) {
        ListOrderedMap map = new ListOrderedMap();
        // 最外层解析
        JSONObject json = JSONObject.fromObject(jsonStr);
        for (Object k : json.keySet()) {
            Object v = json.get(k);
            // 如果内层还是数组的话，继续解析
            if (v instanceof JSONArray) {
                List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                Iterator<JSONObject> it = ((JSONArray) v).iterator();
                while (it.hasNext()) {
                    JSONObject json2 = it.next();
                    list.add(parseJSON2Map(json2.toString()));
                }
                map.put(k.toString(), list);
            } else {
                map.put(k.toString(), v);
            }
        }
        Iterator iterator = map.keySet().iterator();
        List<String> lsList = new ArrayList<String>();
        int d=0;
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            Object object = map.get(key);    
            // 进行遍历删除，当值为空的时候删除这条数据
            if (object.equals("")) {
                iterator.remove();
                map.remove(key);
            }
        }
        return map;
    }
    
    /**
     * 将对象转换成json
     * @param param
     * @return
     */
    public static String object2Json(Object param){
//    	JSON.toJSONString();
    	
    	JSONObject jsonObject = JSONObject.fromObject(param);
    	return jsonObject.toString();
    }
	
    /**
     * 将json字符串转换成map
     * @param json
     * @return
     */
    public static Map<String, Object> json2Map(String json) {
    	com.alibaba.fastjson.JSONObject jsonObject = com.alibaba.fastjson.JSONObject.parseObject(json);
    	Map<String, Object> valueMap = new HashMap<String, Object>();
    	valueMap.putAll(jsonObject);
    	return valueMap;
    }

    /**
     * list对象转换成json
     * @param param
     * @return
     */
    public static String list2String(List param){
    	JsonConfig jsonConfig = new JsonConfig();
    	jsonConfig.setCycleDetectionStrategy(CycleDetectionStrategy.LENIENT);
    	JSONArray json = JSONArray.fromObject(param, jsonConfig);
    	return json.toString();
    }
    
}
