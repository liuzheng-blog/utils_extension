package com.ag777.util;

import java.util.HashMap;
import java.util.Map;

import com.ag777.util.Utils;
import com.ag777.util.file.PropertyUtils;
import com.ag777.util.jsoup.JsoupUtils;

/**
 * 工具配置类
 * <p>
 * 		用于配置工具及获取版本信息
 * </p>
 * 
 * @author ag777
 * @version last modify at 2017年08月28日
 */
public class UtilsExtension{
	
	public static int jsoupTimeOut() {
		return JsoupUtils.defaultTimeOut();
	}
	/**
	* 定制jsoup连接默认的超时时间
	* @param timeOut
	* @return 
	*/
	public static void jsoupTimeOut(int timeOut) {
		JsoupUtils.defaultTimeOut(timeOut);
	}
	
	public static String info() {
		Map<String, Object> infoMap = infoMap();
		if(infoMap != null) {
			return Utils.jsonUtils().toJson(infoMap);
		}
		return null;
	}
	
	public static Map<String, Object> infoMap() {
		Map<String, Object> infoMap = Utils.infoMap();
		PropertyUtils pu = new PropertyUtils();
		try {
			
			pu.load(UtilsExtension.class.getResourceAsStream("/resource/utils_extension.properties"));
			if(infoMap == null) {
				 infoMap = new HashMap<String, Object>();
			}
			infoMap.put("extension_version", pu.get("versionName"));
			infoMap.put("extension_last_release_date", pu.get("last_release_date"));
			return infoMap;
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}
	
}
