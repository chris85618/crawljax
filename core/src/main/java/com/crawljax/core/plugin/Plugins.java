package com.crawljax.core.plugin;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.condition.invariant.Invariant;
import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlSession;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.ExitNotifier.ExitStatus;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.StateVertex;
import com.crawljax.metrics.MetricsModule;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;

/**
 * Class for invoking plugins. The methods in this class are invoked from the Crawljax Core.
 */
@Singleton
public class Plugins {

	private static final Logger LOGGER = LoggerFactory.getLogger(Plugins.class
	        .getName());

	@SuppressWarnings("unchecked")
	static final ImmutableSet<Class<? extends Plugin>> KNOWN_PLUGINS = ImmutableSet
	        .of(DomChangeNotifierPlugin.class, OnBrowserCreatedPlugin.class,
	                OnFireEventFailedPlugin.class,
	                OnInvariantViolationPlugin.class, OnNewStatePlugin.class,
	                OnRevisitStatePlugin.class, OnUrlLoadPlugin.class,
	                PostCrawlingPlugin.class, PreStateCrawlingPlugin.class,
					PreCrawlingPlugin.class, AfterRetrievePathPlugin.class,
					LoginPlugin.class, OnAlertPresentedPlugin.class,
					OnCloneStatePlugin.class, AfterReceiveRobotActionPlugin.class,
					OnNewFoundStatePlugin.class, OnRevisitStateWithExtractElementPlugin.class,
					OnHtmlAttributeFilteringPlugin.class, OnCountingDepthPlugin.class, PostStateCrawlingPlugin.class);

	private final ImmutableListMultimap<Class<? extends Plugin>, Plugin> plugins;

	private final ImmutableMap<Class<? extends Plugin>, Counter> counters;

	private final MetricRegistry registry;

	@Inject
	public Plugins(CrawljaxConfiguration config, MetricRegistry registry) {
		this.registry = registry;
		List<? extends Plugin> plugins = config.getPlugins();
		Preconditions.checkNotNull(plugins);
		ImmutableListMultimap.Builder<Class<? extends Plugin>, Plugin> builder =
		        ImmutableListMultimap
		                .builder();
		if (plugins.isEmpty()) {
			LOGGER.warn("No plugins loaded. There will be no output");
		} else {
			addPlugins(plugins, builder);
		}
		this.plugins = builder.build();

		checkArgument(
		        this.plugins.get(DomChangeNotifierPlugin.class).size() < 2,
		        "Only one or none "
		                + DomChangeNotifierPlugin.class.getSimpleName()
		                + " can be specified");

		this.counters = registerCounters(registry);
	}

	private ImmutableMap<Class<? extends Plugin>, Counter> registerCounters(
	        MetricRegistry registry) {
		ImmutableMap.Builder<Class<? extends Plugin>, Counter> builder = ImmutableMap.builder();
		for (Class<? extends Plugin> plugin : KNOWN_PLUGINS) {
			String name = MetricsModule.PLUGINS_PREFIX + plugin.getSimpleName() + ".invocations";
			Counter c = registry.register(name, new Counter());
			builder.put(plugin, c);
		}
		return builder.build();
	}

	private void addPlugins(
	        List<? extends Plugin> plugins,
	        ImmutableListMultimap.Builder<Class<? extends Plugin>, Plugin> builder) {
		ArrayList<Plugin> unusedPlugins = Lists.newArrayList(plugins);
		for (Plugin plugin : plugins) {
			for (Class<?> clasz : plugin.getClass().getInterfaces()) {
				if (KNOWN_PLUGINS.contains(clasz)) {
					@SuppressWarnings("unchecked")
					Class<? extends Plugin> pluginclass = (Class<? extends Plugin>) clasz;
					builder.put(pluginclass, plugin);
					LOGGER.info("Loaded {} as a {}", plugin,
					        clasz.getSimpleName());
					unusedPlugins.remove(plugin);
				}

			}
		}
		if (!unusedPlugins.isEmpty()) {
			LOGGER.warn(
			        "These plugins were added but are ignored because they are unkown to Crawljax, {}",
			        unusedPlugins);
		}
	}

	private void reportFailingPlugin(Plugin plugin, RuntimeException e) {
		incrementFailCounterFor(plugin);
		LOGGER.error("Plugin {} errored while running. {}", plugin, e.getMessage(), e);
	}

	private void incrementFailCounterFor(Plugin plugin) {
		registry.counter(MetricsModule.PLUGINS_PREFIX + plugin.getClass().getSimpleName()
		        + ".fail_count").inc();
	}

	/**
	 * load and run the OnUrlLoadPlugins. The OnURLloadPlugins are run just after the Browser has
	 * gone to the initial url. Not only the first time but also every time the Core navigates back.
	 * Warning the instance of the browser offered is not a clone but the current and after wards
	 * used browser instance, changes and operations may cause 'strange' behaviour.
	 * <p>
	 * This method can be called from multiple threads with different {@link CrawlerContext}
	 * </p>
	 * 
	 * @param context
	 *            the current {@link CrawlerContext} for this crawler.
	 */
	public void runOnUrlLoadPlugins(CrawlerContext context) {
		LOGGER.debug("Running OnUrlLoadPlugins...");
		counters.get(OnUrlLoadPlugin.class).inc();
		for (Plugin plugin : plugins.get(OnUrlLoadPlugin.class)) {
			if (plugin instanceof OnUrlLoadPlugin) {
				try {
					LOGGER.debug("Calling plugin {}", plugin);
					((OnUrlLoadPlugin) plugin).onUrlLoad(context);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	/**
	 * load and run the OnNewStatePlugins. OnNewStatePlugins are plugins that are ran when a new
	 * state was found. This also happens for the Index State. Warning the session is not a clone,
	 * chaning the session can cause strange behaviour of Crawljax.
	 * <p>
	 * This method can be called from multiple threads with different {@link CrawlerContext}
	 * </p>
	 * 
	 * @param context
	 *            the current {@link CrawlerContext} for this crawler.
	 * @param newState
	 *            The new state
	 */
	public void runOnNewStatePlugins(CrawlerContext context,
	        StateVertex newState) {
		LOGGER.debug("Running OnNewStatePlugins...");
		counters.get(OnNewStatePlugin.class).inc();
		for (Plugin plugin : plugins.get(OnNewStatePlugin.class)) {
			if (plugin instanceof OnNewStatePlugin) {
				try {
					LOGGER.debug("Calling plugin {}", plugin);
					((OnNewStatePlugin) plugin).onNewState(context, newState);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	/**
	 * Run the OnInvariantViolation plugins when an Invariant is violated. Invariant are checked
	 * when the state machine is updated that is when the dom is changed after a click on a
	 * clickable. When a invariant fails this kind of plugins are executed. Warning the session is
	 * not a clone, chaning the session can cause strange behaviour of Crawljax.
	 * 
	 * @param invariant
	 *            the failed invariants
	 * @param context
	 *            the current {@link CrawlerContext} for this crawler.
	 */
	public void runOnInvariantViolationPlugins(Invariant invariant,
	        CrawlerContext context) {
		LOGGER.debug("Running OnInvariantViolationPlugins...");
		counters.get(OnInvariantViolationPlugin.class).inc();
		for (Plugin plugin : plugins.get(OnInvariantViolationPlugin.class)) {
			if (plugin instanceof OnInvariantViolationPlugin) {
				try {
					LOGGER.debug("Calling plugin {}", plugin);
					((OnInvariantViolationPlugin) plugin).onInvariantViolation(
					        invariant, context);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	/**
	 * load and run the postCrawlingPlugins. PostCrawlingPlugins are executed after the crawling is
	 * finished Warning: changing the session can change the behavior of other post crawl plugins.
	 * It is not a clone!
	 * 
	 * @param exitReason
	 *            The reason Crawljax has stopped.
	 * @param session
	 *            the current {@link CrawlSession} for this crawler.
	 */
	public void runPostCrawlingPlugins(CrawlSession session, ExitStatus exitReason) {
		LOGGER.debug("Running PostCrawlingPlugins...");
		counters.get(PostCrawlingPlugin.class).inc();
		for (Plugin plugin : plugins.get(PostCrawlingPlugin.class)) {
			if (plugin instanceof PostCrawlingPlugin) {
				try {
					LOGGER.debug("Calling plugin {}", plugin);
					((PostCrawlingPlugin) plugin).postCrawling(session,
					        exitReason);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	/**
	 * load and run the onRevisitStateValidator. As a difference to other SessionPlugins this plugin
	 * needs an explicit current state because the session.getCurrentState() does not contain the
	 * correct current state because we are in back-tracking
	 * 
	 * @param context
	 *            the current {@link CrawlerContext} for this crawler.
	 * @param currentState
	 *            the state the 'back tracking' operation is currently in
	 */
	public void runOnRevisitStatePlugins(CrawlerContext context,
	        StateVertex currentState) {
		LOGGER.debug("Running OnRevisitStatePlugins...");
		counters.get(OnRevisitStatePlugin.class).inc();
		for (Plugin plugin : plugins.get(OnRevisitStatePlugin.class)) {
			if (plugin instanceof OnRevisitStatePlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((OnRevisitStatePlugin) plugin).onRevisitState(context,
					        currentState);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	//ch-sh begin
	public void runOnCloneStatePlugins(CrawlerContext context, StateVertex currentState) {
		LOGGER.debug("Running OnCloneStatePlugins...");
		counters.get(OnCloneStatePlugin.class).inc();
		for (Plugin plugin : plugins.get(OnCloneStatePlugin.class)) {
			if (plugin instanceof OnCloneStatePlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((OnCloneStatePlugin) plugin).onCloneState(context, currentState);					
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}	

	public void runLoginPlugin(final EmbeddedBrowser browser) {
		counters.get(LoginPlugin.class).inc();
		for (Plugin plugin : plugins.get(LoginPlugin.class)) {
			if (plugin instanceof LoginPlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((LoginPlugin) plugin).login(browser);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}
	
	public void runOnAlertPresentedPlugin(final StateVertex state, final Eventable event, final String alertText) {
		counters.get(OnAlertPresentedPlugin.class).inc();

		for (Plugin plugin : plugins.get(OnAlertPresentedPlugin.class)) {
			if (plugin instanceof OnAlertPresentedPlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((OnAlertPresentedPlugin) plugin).onAlertPresented(state, event, alertText);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	public void runAfterRetrievePathPlugin(CrawlerContext context, List<String[]> path, StateVertex targetState) {
		LOGGER.debug("Running AfterRetrievePathPlugins...");
		counters.get(AfterRetrievePathPlugin.class).inc();
		for (Plugin plugin : plugins.get(AfterRetrievePathPlugin.class)) {
			if (plugin instanceof AfterRetrievePathPlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((AfterRetrievePathPlugin) plugin).afterRetrievePath(context, path, targetState);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}
	//ch-sh end

	/**
	 * load and run the PreStateCrawlingPlugins. Method that is called before the current state is
	 * crawled (before firing events on the current DOM state). Example: filter candidate elements.
	 * Warning the session and candidateElements are not clones, changes will result in changed
	 * behaviour.
	 * 
	 * @param context
	 *            the current {@link CrawlerContext} for this crawler.
	 * @param candidateElements
	 *            the elements which crawljax is about to crawl
	 * @param state
	 *            The state being violated.
	 */
	public void runPreStateCrawlingPlugins(CrawlerContext context,
	        ImmutableList<CandidateElement> candidateElements, StateVertex state) {
		LOGGER.debug("Running PreStateCrawlingPlugins...");
		counters.get(PreStateCrawlingPlugin.class).inc();
		for (Plugin plugin : plugins.get(PreStateCrawlingPlugin.class)) {
			if (plugin instanceof PreStateCrawlingPlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((PreStateCrawlingPlugin) plugin).preStateCrawling(context,
					        candidateElements, state);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	/**
	 * load and run the PreStateCrawlingPlugins. Method that is called before the current state is
	 * crawled (before firing events on the current DOM state). Example: filter candidate elements.
	 * Warning the session and candidateElements are not clones, changes will result in changed
	 * behaviour.
	 * 
	 * @param context
	 *            the current {@link CrawlerContext} for this crawler.
	 * @param candidateElements
	 *            the elements which crawljax is about to crawl
	 * @param state
	 *            The state being violated.
	 */
	public void runPostStateCrawlingPlugins(CrawlerContext context, StateVertex currentState, ImmutableList<Eventable> events) {
		LOGGER.debug("Running PostStateCrawlingPlugins...");
		counters.get(PostStateCrawlingPlugin.class).inc();
		for (Plugin plugin : plugins.get(PostStateCrawlingPlugin.class)) {
			if (plugin instanceof PostStateCrawlingPlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				((PostStateCrawlingPlugin) plugin).postStateCrawling(context, currentState, events);
			}
		}
	}

	/**
	 * Run the {@link PreCrawlingPlugin}s. No {@link CrawlerContext} is available at this stage.
	 * 
	 * @param config
	 *            The given {@link CrawljaxConfiguration}.
	 */
	public void runPreCrawlingPlugins(CrawljaxConfiguration config) {
		LOGGER.debug("Running PreCrawlingPlugins...");
		counters.get(PreStateCrawlingPlugin.class).inc();
		for (Plugin plugin : plugins.get(PreCrawlingPlugin.class)) {
			if (plugin instanceof PreCrawlingPlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((PreCrawlingPlugin) plugin).preCrawling(config);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	/**
	 * Load and run the OnFireEventFailedPlugins, this call has been made from the fireEvent when
	 * the event is not fireable. the Path is the Path leading TO this eventable (not included).
	 * 
	 * @param eventable
	 *            the eventable not able to fire.
	 * @param path
	 *            the path TO this eventable.
	 */
	public void runOnFireEventFailedPlugins(CrawlerContext context,
	        Eventable eventable, List<Eventable> path) {
		LOGGER.debug("Running OnFireEventFailedPlugins...");
		counters.get(OnFireEventFailedPlugin.class).inc();
		for (Plugin plugin : plugins.get(OnFireEventFailedPlugin.class)) {
			if (plugin instanceof OnFireEventFailedPlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((OnFireEventFailedPlugin) plugin).onFireEventFailed(
					        context, eventable, path);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	/**
	 * Load and run the OnBrowserCreatedPlugins, this call has been made from the browserpool when a
	 * new browser has been created and ready to be used by the Crawler. The PreCrawling plugins are
	 * executed before these plugins are executed except that the precrawling plugins are only
	 * executed on the first created browser.
	 * 
	 * @param newBrowser
	 *            the new created browser object
	 */
	public void runOnBrowserCreatedPlugins(EmbeddedBrowser newBrowser) {
		LOGGER.debug("Running OnBrowserCreatedPlugins...");
		counters.get(OnBrowserCreatedPlugin.class).inc();
		for (Plugin plugin : plugins.get(OnBrowserCreatedPlugin.class)) {
			if (plugin instanceof OnBrowserCreatedPlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((OnBrowserCreatedPlugin) plugin)
					        .onBrowserCreated(newBrowser);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	/**
	 * Load and run the DomChangeNotifierPlugin.
	 */
	public boolean runDomChangeNotifierPlugins(final CrawlerContext context,
	        final StateVertex stateBefore, final String[] event,
	        final StateVertex stateAfter) {
		counters.get(DomChangeNotifierPlugin.class).inc();
		if (plugins.get(DomChangeNotifierPlugin.class).isEmpty()) {
			LOGGER.debug("No DomChangeNotifierPlugin found. Performing default DOM comparison...");
			return defaultDomComparison(stateBefore, stateAfter);
		} else {
			DomChangeNotifierPlugin domChange = (DomChangeNotifierPlugin) plugins
			        .get(DomChangeNotifierPlugin.class).get(0);
			LOGGER.debug("Calling plugin {}", domChange);
			try {
				return domChange.isDomChanged(context, stateBefore.getDom(),
				        event, stateAfter.getDom());
			} catch (RuntimeException ex) {
				LOGGER.error(
				        "Could not run {} because of error {}. Now running default DOM comparison",
				        domChange, ex.getMessage(), ex);
				incrementFailCounterFor(domChange);
				return defaultDomComparison(stateBefore, stateAfter);
			}
		}
	}


	/**
	 * Load and run the DomChangeNotifierPlugin.
	 */
	public boolean runAfterReceiveRobotActionPlugins() {
		counters.get(AfterReceiveRobotActionPlugin.class).inc();
		if (plugins.get(AfterReceiveRobotActionPlugin.class).isEmpty()) {
			LOGGER.debug("No AfterReceiveRobotActionPlugin found. Performing default crawing...");
			return false;
		} else {
			AfterReceiveRobotActionPlugin restartSignal = (AfterReceiveRobotActionPlugin) plugins
			        .get(AfterReceiveRobotActionPlugin.class).get(0);
			LOGGER.debug("Calling plugin {}", restartSignal);
			try {
				return restartSignal.isRestartOrNot();
			} catch (RuntimeException ex) {
				LOGGER.error(
				        "Could not run {} because of error {}. Now return default procedure...",
				        restartSignal, ex.getMessage(), ex);
				incrementFailCounterFor(restartSignal);
				return false;
			}
		}
	}

	public String runOnNewFoundStatePlugins(String dom) {
		counters.get(OnNewFoundStatePlugin.class).inc();
		if (plugins.get(OnNewFoundStatePlugin.class).isEmpty()) {
			LOGGER.debug("No OnNewFoundStatePlugin found. Performing default crawing...");
			return dom;
		} else {
			OnNewFoundStatePlugin foundNewState = (OnNewFoundStatePlugin) plugins
			        .get(OnNewFoundStatePlugin.class).get(0);
			LOGGER.debug("Calling plugin {}", foundNewState);
			try {
				return foundNewState.onNewFoundState(dom);
			} catch (RuntimeException ex) {
				LOGGER.error(
				        "Could not run {} because of error {}. Now return default procedure...",
				        foundNewState, ex.getMessage(), ex);
				incrementFailCounterFor(foundNewState);
				return dom;
			}
		}
	}

	/**
	 * This will be call at reset method in {@link com.crawljax.core.Crawler}
	 * @param context
	 * @param candidateElements
	 * @param state
	 */
	public void runOnRevisitStateWithExtractElementsPlugin(CrawlerContext context,
														   ImmutableList<CandidateElement> candidateElements, StateVertex state) {
		LOGGER.debug("Running OnRestartCrawlingStatePlugins...");
		counters.get(OnRevisitStateWithExtractElementPlugin.class).inc();
		for (Plugin plugin : plugins.get(OnRevisitStateWithExtractElementPlugin.class)) {
			if (plugin instanceof OnRevisitStateWithExtractElementPlugin) {
				LOGGER.debug("Calling plugin {}", plugin);
				try {
					((OnRevisitStateWithExtractElementPlugin) plugin).onRestartCrawling(context,
							candidateElements, state);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	public String runOnHtmlAttributeFilteringPlugins(String dom, String url) {
		LOGGER.debug("Running OnHtmlAttributeFilteringPlugins...");
		counters.get(OnHtmlAttributeFilteringPlugin.class).inc();
		String filteredHtml = dom;
		for (Plugin plugin : plugins.get(OnHtmlAttributeFilteringPlugin.class)) {
			if (plugin instanceof OnHtmlAttributeFilteringPlugin) {
				try {
					LOGGER.debug("Calling plugin {}", plugin);
					filteredHtml = ((OnHtmlAttributeFilteringPlugin) plugin).filterDom(dom, url);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
		return filteredHtml;
	}

	public void runOnCountingDepthPlugins(StateVertex currentState, AtomicInteger crawlDepth) {
		LOGGER.debug("Running OnSpecificPagePlugin...");
		counters.get(OnCountingDepthPlugin.class).inc();
		for (Plugin plugin : plugins.get(OnCountingDepthPlugin.class)) {
			if (plugin instanceof OnCountingDepthPlugin) {
				try {
					LOGGER.debug("Calling plugin {}", plugin);
					((OnCountingDepthPlugin) plugin).controlDepth(currentState, crawlDepth);
				} catch (RuntimeException e) {
					reportFailingPlugin(plugin, e);
				}
			}
		}
	}

	private boolean defaultDomComparison(final StateVertex stateBefore,
	        final StateVertex stateAfter) {
		// default DOM comparison behavior
		boolean isChanged = !stateAfter.equals(stateBefore);
		if (isChanged) {
			LOGGER.debug("Dom is Changed!");
			return true;
		} else {
			LOGGER.debug("Dom not Changed!");
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(plugins);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Plugins) {
			Plugins that = (Plugins) object;
			return Objects.equals(this.plugins, that.plugins);
		}
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("plugins", plugins).toString();
	}

	/**
	 * @return A {@link ImmutableSet} of the {@link Plugin} names that are installed.
	 */
	public ImmutableSet<String> pluginNames() {
		ImmutableSortedSet.Builder<String> names = ImmutableSortedSet
		        .naturalOrder();
		for (Plugin plugin : plugins.values()) {
			names.add(plugin.toString());
		}
		return names.build();
	}

}
