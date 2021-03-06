package com.ag777.util.file.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.ag777.util.lang.collection.ListUtils;
import com.ag777.util.lang.collection.MapUtils;

/**
 * xml文件构造工具类
 * <p>
 * 	需要jar包:
 * <ul>
 * <li>dom4j-1.6.1.jar</li>
 * </ul>
 * </p>
 * @author wanggz
 * @version last modify at 2018年09月05日
 */
public class XmlBuilder {
	/*
	 * XML 规范不支持的字符
	 * 错误:xml 浏览器打开报错Input is not proper UTF-8, indicate encoding !
	 * 参考:https://blog.csdn.net/isaisai/article/details/53899089
	 */
	private static final Pattern p_invalid = Pattern.compile("\\u0000|\\u0001|\\u0002|\\u0003|\\u0004|\\u0005|\\u0006|\\u0007|\\u0008|\\u0009|\\u000a|\\u000b|\\u000c|\\u000d|\\u000e|\\u000f|\\u0010|\\u0011|\\u0012|\\u0013|\\u0014|\\u0015|\\u0016|\\u0017|\\u0018|\\u0019|\\u001a|\\u001b|\\u001c|\\u001d|\\u001e|\\u001f");
	
	private String tag;
	private Map<String, Object> attrMap;
	private String value;
	private boolean isCdata = false;
	
	private List<XmlBuilder> children;
	
	public XmlBuilder(String tag) {
		this.tag = tag;
	}
	
	public XmlBuilder(String tag, String value) {
		this.tag = tag;
		this.value = value;
	}
	
	public XmlBuilder(String tag, Integer value) {
		this.tag = tag;
		if(value != null) {
			this.value = value.toString();
		}
		
	}
	
	public XmlBuilder(String tag, Map<String, Object> attrMap, String value) {
		this.tag = tag;
		this.attrMap = attrMap;
		this.value = value;
	}
	
	/**
	 * 将该节点设置为cdata节点
	 * @return
	 */
	public XmlBuilder setCdata() {
		this.isCdata = true;
		return this;
	}

	public String tag() {
		return tag;
	}
	public XmlBuilder tag(String tag) {
		this.tag = tag;
		return this;
	}
	
	public Map<String, Object> attrMap() {
		return attrMap;
	}
	
	public XmlBuilder attr(String key, Object value) {
		if(key != null) {
			if(attrMap == null) {
				attrMap = MapUtils.newHashMap();
			}
			attrMap.put(key, value!=null?value:"");
		}
		return this;
	}
	public XmlBuilder clearAttr() {
		this.attrMap = null;
		return this;
	}
	public XmlBuilder attrMap(Map<String, Object> attrMap) {
		if(attrMap == null) {
			this.attrMap = null;
			return this;
		}
		if(this.attrMap == null) {
			this.attrMap = MapUtils.newHashMap();
		}
		
		attrMap.forEach((k,v)->{
			attr(k, v);
		});
		
		return this;
	}
	public String val() {
		return value;
	}
	public XmlBuilder val(String value) {
		this.value = value;
		return this;
	}
	
	public List<XmlBuilder> children() {
		return children;
	}
	public XmlBuilder clearChildren() {
		this.children = null;
		return this;
	}
	public XmlBuilder child(XmlBuilder child) {
		if(children == null) {
			children = ListUtils.newArrayList();
		}
		children.add(child);
		return this;
	}
	public XmlBuilder children(List<XmlBuilder> children) {
		if(children == null) {
			this.children = null;
			return this;
		}
		if(this.children == null) {
			this.children = children;
		} else {
			this.children.addAll(children);
		}
		return this;
	}
	public XmlBuilder children(XmlBuilder[] children) {
		if(children == null) {
			this.children = null;
			return this;
		}
		if(this.children == null) {
			this.children = ListUtils.ofList(children);
		} else {
			for (XmlBuilder child : children) {
				this.children.add(child);
			}
		}
		return this;
	}
	
	/**
	 * 复制该节点
	 */
	public XmlBuilder clone() {
		XmlBuilder builder = new XmlBuilder(tag);
		builder.tag(tag)
			.attrMap(attrMap)
			.val(value);
		if(children != null) {
			for (XmlBuilder child : children) {
				builder.child(child.clone());
			}
		}
		return builder;
	}
	
	public boolean saveAsHtml(String filePath) {
		
		Document document = buildElements();
        
        document.addDocType("html", null, null); 	
        
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setSuppressDeclaration(true); //注意这句
        return writeFile(document, filePath, format);
	}
	
	/**
	 * 构建并输出xml文档
	 * @return
	 */
	public boolean save(String filePath) {
        Document document = buildElements();
        OutputFormat format = OutputFormat.createPrettyPrint();  
        format.setEncoding("utf-8");
        return writeFile(document, filePath, format);
		
	}
	
	/**
	 * 填写节点的值和属性，递归构建子节点
	 * @param element
	 */
	private void fillElement(Element element) {
		if(value != null) {
			value = p_invalid.matcher(value).replaceAll("");	//替换xml规范不支持的字符串为空字符串
			if(!isCdata) {
				element.setText(value);
			} else {
				element.addCDATA(value);
			}
			
		}
		if(!MapUtils.isEmpty(attrMap)) {
			attrMap.forEach((k,v)->{
				element.addAttribute(k, v.toString());
			});
		}
		if(!ListUtils.isEmpty(children)) {
			for (XmlBuilder child : children) {
				Element childElement = element.addElement(child.tag());
				child.fillElement(childElement);	//递归
			}
		}
	}
	
	private Document buildElements() {
		//DocumentHelper提供了创建Document对象的方法  
        Document document = DocumentHelper.createDocument();

        //添加节点信息  
        Element rootElement = document.addElement(tag); 
        //填写值,属性,递归构造子节点
        fillElement(rootElement);
        return document;
	}
	
	public boolean writeFile(Document document, String filePath, OutputFormat format) {
		XMLWriter xmlWriter = null;
		try {
			new File(filePath).getParentFile().mkdirs();
			Writer fileWriter = new FileWriter(filePath);  
			//dom4j提供了专门写入文件的对象XMLWriter 
	        xmlWriter = new XMLWriter(fileWriter, format);  
	        xmlWriter.write(document);  
	        return true;
		} catch(IOException ex) {
			ex.printStackTrace();
		} finally {
			if(xmlWriter != null) {
				try {
					xmlWriter.flush();
					xmlWriter.close();
				} catch (IOException e) {
				}  
			}
		}
		return false;
	}
}
