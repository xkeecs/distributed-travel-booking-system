package ResImpl;

import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlParser
{

	public class Tuple2<X, Y>
	{
		public final X x;
		public final Y y;

		public Tuple2(X x, Y y)
		{
			this.x = x;
			this.y = y;
		}
	}

	private HashMap<String, Tuple2<String, Integer>> propertyMap = null;

	public XmlParser(String confPath)
	{
		propertyMap = new HashMap<String, Tuple2<String, Integer>>();
		parse(confPath);
	}

	public HashMap<String, Tuple2<String, Integer>> getPropertyMap()
	{
		return propertyMap;
	}

	public Tuple2<String, Integer> getTuple2(String key)
	{
		return propertyMap.get(key);
	}

	/**
	 * parsing operation
	 */
	private void parse(String confPath)
	{
		try
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setIgnoringElementContentWhitespace(true);
			DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(confPath);
			Element configurationRoot = doc.getDocumentElement();
			NodeList propertyList = configurationRoot.getChildNodes();
			for (int i = 0; i < propertyList.getLength(); i++)
			{
				Node property = propertyList.item(i);
				Element eleProperty = (Element) property;
				propertyMap.put(
						getTagValue("name", eleProperty),
						new Tuple2<String, Integer>(getTagValue("ip", eleProperty), Integer.parseInt(getTagValue(
								"port", eleProperty))));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * get the value with String type
	 * 
	 * @param tagName
	 *            , the property name
	 * @param ele
	 *            , the element
	 * @return the value
	 */
	private String getTagValue(String tagName, Element ele)
	{
		NodeList nlList = ele.getElementsByTagName(tagName).item(0).getChildNodes();
		Node nValue = nlList.item(0);
		if (nValue == null)
		{
			return null;
		}
		return nValue.getNodeValue();
	}
}