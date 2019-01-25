package ntut.edu.tw.irobot;

import java.io.File;
import java.io.FileWriter;

import com.crawljax.core.CrawlerContext;
import com.crawljax.core.plugin.HostInterface;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.state.StateVertex;
import com.crawljax.core.plugin.HostInterface;

public class SamplePlugin implements OnNewStatePlugin {

	private HostInterface hostInterface;

	public SamplePlugin(HostInterface hostInterface) {
		this.hostInterface = hostInterface;
	}

	@Override
	public void onNewState(CrawlerContext context, StateVertex newState) {
		try {
			String dom = context.getBrowser().getStrippedDom();
			File file = new File(hostInterface.getOutputDirectory(), context.getCurrentState().getName() + ".html");

			FileWriter fw = new FileWriter(file, false);
			fw.write(dom);
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
