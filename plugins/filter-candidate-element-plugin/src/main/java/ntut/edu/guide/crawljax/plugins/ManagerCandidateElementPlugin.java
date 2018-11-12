package ntut.edu.guide.crawljax.plugins;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import java.util.*;

import org.json.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.crawljax.browser.WebDriverBackedEmbeddedBrowser;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.plugin.HostInterface;
import com.crawljax.core.plugin.PreStateCrawlingPlugin;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.StateVertex;
import com.crawljax.util.XPathHelper;
import com.google.common.collect.ImmutableList;
import com.google.gson.*;

public class ManagerCandidateElementPlugin implements PreStateCrawlingPlugin {
	private HostInterface hostInterface;
	private File inputsFile = null;
	private String inputsFileName = "";
	private JSONObject inputsJSONObjectRoot = null;

	private static final Logger LOGGER = LoggerFactory.getLogger(ManagerCandidateElementPlugin.class);
	private WriteFileThreadPool writeFileThreadPool = new WriteFileThreadPool(3);
	final static String[] TAG_NAMES = { "input", "button", "img", "a" };
	final HashSet<String> foundedState = new HashSet<String>();

	public ManagerCandidateElementPlugin(HostInterface hostInterface) {
		this.hostInterface = hostInterface;
	}

	@Override
	public void preStateCrawling(CrawlerContext context, ImmutableList<CandidateElement> immutableList,
			StateVertex stateVertex) {
		if (foundedState.contains(stateVertex.getName()))
			return;
		else
			foundedState.add(stateVertex.getName());
		try {
			LOGGER.info("Entering Filter Candidate-Element Plugin.");
			WebDriver driver = ((WebDriverBackedEmbeddedBrowser) context.getBrowser()).getBrowser();
			List<Node> founded = FindAllElement(stateVertex.getDocument());
			WriteFoundedElement(immutableList, founded, driver, stateVertex);
			List<CandidateElement> result = ReconstructCandidate(founded, immutableList, stateVertex, driver);

			LinkedList<CandidateElement> resultLinkedList = new LinkedList<>();
			resultLinkedList.addAll(result);
			stateVertex.setElementsFound(resultLinkedList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void readJSON() {
		if (this.inputsFile == null || this.inputsJSONObjectRoot == null) {
			try {
				// Path inputsPath = getURLIDInputsDirPath();
				this.inputsFileName = this.hostInterface.getParameters().get("inputsFileName");
				LOGGER.info("Inputs file name: {}", this.inputsFileName);
				Path fullPath = URLIDFileRetriever.getURLIDFilePath(this.inputsFileName);
				LOGGER.info("Full path to inputs file: {}", fullPath.toAbsolutePath());
				this.inputsFile = fullPath.toAbsolutePath().toFile();

				BufferedReader br = new BufferedReader(
						new InputStreamReader(new FileInputStream(this.inputsFile), "UTF-8"));
				JSONTokener jsonTokener = new JSONTokener(br);
				this.inputsJSONObjectRoot = new JSONObject(jsonTokener);
			} catch (Exception e) {
				LOGGER.error("The plugin throws exception: {}", e.getMessage());
				LOGGER.error("The plugin is not going to work.");
				return;
			}
		}
	}

	private List<CandidateElement> ReconstructCandidate(List<Node> elementInPage,
			List<CandidateElement> candidateElement, StateVertex stateVertex, WebDriver driver) {
		String url = stateVertex.getUrl();

		readJSON();

		JSONArray reconstructMap = null;
		try {
			reconstructMap = this.inputsJSONObjectRoot.getJSONObject("candidateReconstructMap").getJSONArray(url);
		} catch (JSONException exception) {
			LOGGER.warn("This page using default candidate element. {} ", url);
			return candidateElement;
		}

		Map<String, Integer> orderMap = new HashMap<String, Integer>();
		for (int i = 0; i < reconstructMap.length(); i++) {
			String key = reconstructMap.getJSONObject(i).getString("xpath");
			int newOrder = reconstructMap.getJSONObject(i).getInt("order");
			orderMap.put(key, newOrder);
		}

		Map<Integer, CandidateElement> tempMap = new HashMap<Integer, CandidateElement>();
		List<CandidateElement> result = new ArrayList<CandidateElement>();
		for (Iterator<CandidateElement> iterator = candidateElement.iterator(); iterator.hasNext();) {
			CandidateElement nextElement = iterator.next();
			String elementXpath = nextElement.getIdentification().getValue();
			if (orderMap.containsKey(elementXpath)) {
				int newOrder = orderMap.get(elementXpath);
				if (newOrder > 0) {
					tempMap.put(newOrder - 1, nextElement);
				}
				orderMap.remove(elementXpath);
			}
		}
		for (Map.Entry<String, Integer> entry : orderMap.entrySet()) {
			if (entry.getValue() > 0) {
				Node target = null;
				for (Node node : elementInPage) {
					if (XPathHelper.getXPathExpression(node).equals(entry.getKey())) {
						target = node;
						break;
					}
				}
				Element element = (Element) target;
				CandidateElement ce = new CandidateElement(element,
						new Identification(Identification.How.xpath, entry.getKey()), "");
				tempMap.put(entry.getValue() - 1, ce);
			}
		}

		SortedSet<Integer> sortedKeys = new TreeSet<Integer>(tempMap.keySet());
		for (Integer key : sortedKeys) {
			CandidateElement ce = tempMap.get(key);
			int index = key;
			while (index > result.size())
				index--;
			result.add(index, ce);
		}

		return result;
	}

	private void WriteFoundedElement(ImmutableList<CandidateElement> immutableList, List<Node> founded,
			WebDriver driver, StateVertex stateVertex) {
		List<String> candidateElements = new ArrayList<String>();
		for (CandidateElement element : immutableList) {
			candidateElements.add(element.getIdentification().getValue());
		}
		File parentDir = hostInterface.getOutputDirectory();
		JsonArray out = new JsonArray();

		Iterator<Node> nodeIt = founded.iterator();
		while (nodeIt.hasNext()) {
			Node node = nodeIt.next();
			String Xpath = XPathHelper.getXPathExpression(node);
			WebElement element = null;
			try {
				element = driver.findElement(By.xpath(Xpath));
			} catch (Exception e) {
				continue;
			}
			if (!element.isDisplayed() || !element.isEnabled()) {
				nodeIt.remove();
				continue;
			}
			JsonObject object = new JsonObject();
			object.addProperty("id", ((Element) node).getAttribute("id"));
			object.addProperty("xpath", Xpath);
			object.addProperty("tagName", node.getNodeName());
			object.addProperty("defaultOrder", 0);
			for (int i = 0; i < candidateElements.size(); i++) {
				if (candidateElements.get(i).equals(Xpath)) {
					object.addProperty("defaultOrder", i + 1);
					break;
				}
			}

			JsonObject location = new JsonObject();
			location.addProperty("x", element.getLocation().getX());
			location.addProperty("y", element.getLocation().getY());
			object.add("location", location);

			JsonObject size = new JsonObject();
			size.addProperty("width", element.getSize().getWidth());
			size.addProperty("height", element.getSize().getHeight());
			object.add("size", size);
			out.add(object);
		}
		try {
			writeFileThreadPool.execute(() -> {
				try {
					String outJsonString = out.toString();
					File outDir = new File(parentDir, stateVertex.getName() + ".json");
					Writer writer = new OutputStreamWriter(new FileOutputStream(outDir), StandardCharsets.UTF_8);
					writer.write(outJsonString);
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private List<Node> FindAllElement(Document dom) {
		List<Node> founded = new ArrayList<>();
		for (String tag : TAG_NAMES) {
			NodeList nodeList = dom.getDocumentElement().getElementsByTagName(tag);
			for (int i = 0; i < nodeList.getLength(); i++) {
				founded.add(nodeList.item(i));
			}
		}
		return founded;
	}
}
