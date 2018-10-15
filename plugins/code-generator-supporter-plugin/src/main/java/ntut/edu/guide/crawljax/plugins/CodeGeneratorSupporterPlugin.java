package ntut.edu.guide.crawljax.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.w3c.dom.Element;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.browser.WebDriverBackedEmbeddedBrowser;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.plugin.AfterRetrievePathPlugin;
import com.crawljax.core.plugin.DomChangeNotifierPlugin;
import com.crawljax.core.plugin.HostInterface;
import com.crawljax.core.plugin.LoginPlugin;
import com.crawljax.core.plugin.OnAlertPresentedPlugin;
import com.crawljax.core.plugin.OnCloneStatePlugin;
import com.crawljax.core.plugin.OnFireEventFailedPlugin;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.PostCrawlingPlugin;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertex;
import com.crawljax.util.XPathHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class CodeGeneratorSupporterPlugin
		implements OnNewStatePlugin, PostCrawlingPlugin, LoginPlugin, OnAlertPresentedPlugin, OnCloneStatePlugin,
		AfterRetrievePathPlugin, OnFireEventFailedPlugin, DomChangeNotifierPlugin {

	private HostInterface hostInterface;
	private static final Logger LOGGER = LoggerFactory.getLogger(CodeGeneratorSupporterPlugin.class);
	private JsonObject crawlJaxExtendParameter = new JsonObject();

	private String extractExpression = "option";

	public CodeGeneratorSupporterPlugin(HostInterface hostInterface) {
		this.hostInterface = hostInterface;

		crawlJaxExtendParameter.add("crawlPath", new JsonArray());
		crawlJaxExtendParameter.add("elements", new JsonObject());
		crawlJaxExtendParameter.add("redirect", new JsonObject());
		crawlJaxExtendParameter.add("alert", new JsonObject());
	}

	@Override
	public void login(EmbeddedBrowser browser) {
		JsonObject object = new JsonObject();
		object.addProperty("type", "reset");
		object.addProperty("identify", "reset");
		crawlJaxExtendParameter.getAsJsonArray("crawlPath").add(object);
	}

	@Override
	public boolean isDomChanged(CrawlerContext context, StateVertex domBefore, String[] e, StateVertex domAfter) {
		JsonObject object = new JsonObject();
		object.addProperty("type", e[0]);
		object.addProperty("identify", e[1]);
		crawlJaxExtendParameter.getAsJsonArray("crawlPath").add(object);

		return !domAfter.equals(domBefore);
	}

	@Override
	public void postCrawling(CrawlSession session, ExitStatus exitReason) {
		try {
			File outputDir = hostInterface.getOutputDirectory();
			File fileToWrite = new File(outputDir, "crawlJaxExtendParameter.json");
			String jsonString = crawlJaxExtendParameter.toString();
			Writer writer = new OutputStreamWriter(new FileOutputStream(fileToWrite), StandardCharsets.UTF_8);
			writer.write(jsonString);
			writer.close();
		} catch (IOException exception) {
			LOGGER.warn("Cannot write json to given directory.");
		}
	}

	@Override
	public void onNewState(CrawlerContext context, StateVertex newState) {
		LOGGER.info("Entering Code Generator Supporter Plugin.");
		final String GET_TEXT_EXECUTER = "if(arguments[0].firstChild.nodeType === Node.TEXT_NODE) return arguments[0].firstChild.textContent; return '';";
		WebDriver driver = ((WebDriverBackedEmbeddedBrowser) context.getBrowser()).getBrowser();
		String newUrl = checkURLRedirect(newState, driver);
		if (!newUrl.isEmpty()) {
			JsonObject object = crawlJaxExtendParameter.getAsJsonObject("redirect");
			if (!object.has(newUrl))
				object.add(newState.getUrl(), new JsonObject());
			object.getAsJsonObject(newState.getUrl()).addProperty("to", newUrl);
		}
		JsonObject object = new JsonObject();
		object.addProperty("type", "state");
		object.addProperty("identify", newState.getName());
		crawlJaxExtendParameter.getAsJsonArray("crawlPath").add(object);
		try {
			JsonArray element = new JsonArray();
			Node[] nodes = ExtractAllElement(newState.getDocument());
			for (Node node : nodes) {
				if (node.getNodeName().toLowerCase().equals("iframe")
						|| ((Element) node).getTagName().toLowerCase().matches(extractExpression)) {
					continue;
				}
				try {
					String text = node.getFirstChild().getNodeValue();
					if (text.equals(""))
						throw new Exception();

					String Xpath = XPathHelper.getXPathExpression(node);
					WebElement we = driver.findElement(By.xpath(Xpath));
					if (we == null)
						throw new Exception();

					Dimension size = we.getSize();
					String display = we.getCssValue("display");
					Boolean isDisplay = (size.height * size.width) > 0 && !display.equalsIgnoreCase("none");
					if (!isDisplay)
						continue;

					JsonObject object2 = new JsonObject();
					object2.addProperty("xpath", Xpath);
					object2.addProperty("text",
							(String) ((JavascriptExecutor) driver).executeScript(GET_TEXT_EXECUTER, we));
					object2.addProperty("display", isDisplay ? "true" : "false");
					object2.addProperty("sizeRatio", size.width == 0 ? 0 : (float) size.height / (float) size.width);
					element.add(object2);
				} catch (Exception ex) {
					continue;
				}
			}
			crawlJaxExtendParameter.getAsJsonObject("elements").add(newState.getName(), element);
		} catch (IOException e) {
			LOGGER.warn("Can't extract element from page because : %s", e.getMessage());
		}
		LOGGER.info("Leaving Code Generator Supporter Plugin.");
	}

	@Override
	public void onCloneState(CrawlerContext crawlerContext, StateVertex stateVertex) {
		JsonArray crawlPath = crawlJaxExtendParameter.getAsJsonArray("crawlPath");

		JsonObject object = new JsonObject();
		object.addProperty("type", "state");
		object.addProperty("identify", stateVertex.getName());
		crawlPath.add(object);
	}

	@Override
	public void afterRetrievePath(CrawlerContext context, List<String[]> path, StateVertex targetState) {
		JsonArray crawlPath = crawlJaxExtendParameter.getAsJsonArray("crawlPath");
		JsonObject identifyObject = new JsonObject();
		JsonArray jsonPath = new JsonArray();
		for (String[] trace : path) {
			JsonObject object = new JsonObject();
			object.addProperty(trace[0], trace[1]);
			jsonPath.add(object);
		}

		JsonObject object = new JsonObject();
		object.addProperty("type", "follow");
		identifyObject.add(targetState.getName(), jsonPath);
		object.add("identify", identifyObject);
		crawlPath.add(object);
	}

	@Override
	public void onFireEventFailed(CrawlerContext context, Eventable eventable, List<Eventable> pathToFailure) {
		LOGGER.error("{} fail to click", eventable.toString());
		LOGGER.error("path : {}", pathToFailure.toString());
	}

	@Override
	public void onAlertPresented(StateVertex state, Eventable event, String alertText) {
		JsonObject object = crawlJaxExtendParameter.getAsJsonObject("alert");
		if (object.has(state.getUrl())) {
			if (!object.getAsJsonObject(state.getUrl()).has(event.getIdentification().getValue())) {
				object.getAsJsonObject(state.getUrl()).addProperty(event.getIdentification().getValue(), alertText);
			}
		} else {
			object.add(state.getUrl(), new JsonObject());
			object.getAsJsonObject(state.getUrl()).addProperty(event.getIdentification().getValue(), alertText);
		}
	}

	private String checkURLRedirect(StateVertex newState, WebDriver driver) {
		String url = driver.getCurrentUrl();
		if (url.endsWith("/"))
			url = url.substring(0, url.length() - 1);
		String originUrl = newState.getUrl();
		if (originUrl.endsWith("/"))
			originUrl = originUrl.substring(0, originUrl.length() - 1);
		if (originUrl.equals(url))
			return "";
		return url;
	}

	private Node[] ExtractAllElement(Document dom) {
		ArrayList<Node> nodes = new ArrayList<Node>();

		NodeList nodeList = dom.getElementsByTagName("BODY");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node currentNode = nodeList.item(i);
			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				nodes.addAll(ExtractAllElement(currentNode));
			}
		}

		return nodes.toArray(new Node[0]);
	}

	private ArrayList<Node> ExtractAllElement(Node node) {
		ArrayList<Node> nodes = new ArrayList<Node>();

		if (node.hasChildNodes() && node.getNodeType() == Node.ELEMENT_NODE) {
			Node currentNode = node.getFirstChild();
			while (currentNode != null) {
				if (!currentNode.getNodeName().startsWith("#")) {
					ArrayList<Node> childnodes = ExtractAllElement(currentNode);
					nodes.addAll(childnodes);
					nodes.add(currentNode);
				}
				currentNode = currentNode.getNextSibling();
			}
		}
		return nodes;
	}
}
