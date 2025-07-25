package com.crawljax.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.state.Eventable.EventType;
import com.crawljax.core.state.StateFlowGraph;
import com.crawljax.core.state.StateVertex;
import com.crawljax.metrics.MetricsModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Striped;

/**
 * Contains all the {@link CandidateCrawlAction}s that still have to be fired to get a result.
 */
@Singleton
public class UnfiredCandidateActions {

	private static final Logger LOG = LoggerFactory.getLogger(UnfiredCandidateActions.class);

	private final Map<Integer, Queue<CandidateCrawlAction>> cache;
	private final BlockingQueue<Integer> statesWithCandidates;
	private final Striped<Lock> locks;
	private final Provider<StateFlowGraph> sfg;
	private final Counter crawlerLostCount;
	private final Counter unfiredActionsCount;
	private final ReadWriteLock consumersStateLock;
	private final Lock consumersWriteLock;
	private final Lock consumersReadLock;
	private int runningConsumers;

	@Inject
	UnfiredCandidateActions(BrowserConfiguration config, Provider<StateFlowGraph> sfg,
	        MetricRegistry registry) {
		this.sfg = sfg;
		cache = Maps.newHashMap();
		statesWithCandidates = Queues.newLinkedBlockingQueue();
		// Every browser gets a lock.
		locks = Striped.lock(config.getNumberOfBrowsers());

		crawlerLostCount =
		        registry.register(MetricsModule.EVENTS_PREFIX + "crawler_lost", new Counter());
		unfiredActionsCount =
		        registry.register(MetricsModule.EVENTS_PREFIX + "unfired_actions", new Counter());

		consumersStateLock = new ReentrantReadWriteLock();
		consumersWriteLock = consumersStateLock.writeLock();
		consumersReadLock = consumersStateLock.readLock();
		runningConsumers = 0;
	}

	/**
	 * @param state
	 *            The state you want to poll an {@link CandidateCrawlAction} for.
	 * @return The next to-be-crawled action or <code>null</code> if none available.
	 */
	CandidateCrawlAction pollActionOrNull(StateVertex state) {
		LOG.debug("Polling action for state {}", state.getName());
		Lock lock = locks.get(state.getId());
		try {
			lock.lock();
			Queue<CandidateCrawlAction> queue = cache.get(state.getId());
			if (queue == null) {
				return null;
			} else {
				CandidateCrawlAction action = queue.poll();
				if (queue.isEmpty()) {
					LOG.debug("All actions polled for state {}", state.getName());
					cache.remove(state.getId());
					removeStateFromQueue(state.getId());
					LOG.debug("There are now {} states with unfinished actions", cache.size());
				}
				return action;
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 *  For DQN Learning Method
	 * 
	 * @param state
	 * 			  The state you want to poll an {@link CandidateCrawlAction} for.
	 * @return The target action or <code>null</code> if none available.
	 */
	CandidateCrawlAction peekActionOrNull(StateVertex state) {
		LOG.debug("Learning Mode...");
		LOG.debug("Peeking action for state {}", state.getName());
		Lock lock = locks.get(state.getId());
		try {
			lock.lock();
			Queue<CandidateCrawlAction> queue = cache.get(state.getId());
			if (queue == null) {
				return null;
			} else {
				CandidateCrawlAction action = queue.peek();
				return action;
			}
		} finally {
			lock.unlock();
		}
	}

	private void removeStateFromQueue(int id) {
		consumersWriteLock.lock();
		try {
			while (statesWithCandidates.remove(id)) {
				LOG.trace("Removed id {} from the queue", id);
			}
		} finally {
			consumersWriteLock.unlock();
		}
	}

	/**
	 * @param extract
	 *            The actions you want to add to a state.
	 * @param currentState
	 *            The state you are in.
	 */
	public void addActions(ImmutableList<CandidateElement> extract, StateVertex currentState) {
		List<CandidateCrawlAction> actions = createCandidateCrawlActionList(extract);
		addActions(actions, currentState);
	}

	private List<CandidateCrawlAction> createCandidateCrawlActionList(ImmutableList<CandidateElement> extract) {
		List<CandidateCrawlAction> actions = new ArrayList<>(extract.size());
		for (CandidateElement candidateElement : extract) {
			String tagName = candidateElement.getElement().getTagName();
			String type = candidateElement.getElement().getAttribute("type");
			if ((tagName.equalsIgnoreCase("input") && !isInputActionClick(type))
					|| tagName.equalsIgnoreCase("textarea"))
				actions.add(new CandidateCrawlAction(candidateElement, EventType.input));
			else
				actions.add(new CandidateCrawlAction(candidateElement, EventType.click));
		}
		return actions;
	}

	private boolean isInputActionClick(String type) {
		return "button".equalsIgnoreCase(type) || "submit".equalsIgnoreCase(type)
				|| "reset".equalsIgnoreCase(type) || "image".equalsIgnoreCase(type);
	}

	/**
	 * @param actions
	 *            The actions you want to add to a state.
	 * @param state
	 *            The state name. This should be unique per state.
	 */
	void addActions(Collection<CandidateCrawlAction> actions, StateVertex state) {
		if (actions.isEmpty()) {
			LOG.debug("Received empty actions list. Ignoring...");
			return;
		}
		Lock lock = locks.get(state.getId());
		try {
			lock.lock();
			LOG.debug("Adding {} crawl actions for state {}", actions.size(), state.getId());
			if (cache.containsKey(state.getId())) {
				cache.get(state.getId()).addAll(actions);
			} else {
				cache.put(state.getId(), Queues.newConcurrentLinkedQueue(actions));
			}
			consumersWriteLock.lock();
			try {
				statesWithCandidates.add(state.getId());
			} finally {
				consumersWriteLock.unlock();
			}

			LOG.info("There are {} states with unfired actions", statesWithCandidates.size());
		} finally {
			lock.unlock();
		}

	}

	/**
	 * @param extract
	 *            The actions you want to add to a state.
	 * @param currentState
	 *            The state you are in.
	 */
	public void setActions(ImmutableList<CandidateElement> extract, StateVertex currentState) {
		LOG.debug("In Leaning Mode...");
		LOG.info("This part is for learning part, will reconstruct the order in cache.");
		List<CandidateCrawlAction> actions = createCandidateCrawlActionList(extract);
		setActions(actions, currentState);
	}

	/**
	 *  For DQN Learning Method
	 *  This will reconstruct the order in cache, 
	 * 			because when in clone state, robot can't change the order in cache
	 * 
	 *  @param actions
	 * 				The action list that is reconstructed
	 *  @param state
	 * 				The state that want to replace
	 */
	void setActions(Collection<CandidateCrawlAction> actions, StateVertex state) {
		if (actions.isEmpty()) {
			LOG.debug("Received empty actions list. Ignoring...");
			return;
		}
		Lock lock = locks.get(state.getId());
		try {
			lock.lock();
			LOG.debug("Setting {} crawl actions for state {}", actions.size(), state.getId());
			if (cache.containsKey(state.getId())) {
				cache.get(state.getId()).clear();
				cache.get(state.getId()).addAll(actions);
			} else {
				LOG.debug("Can't find state {} in cache, add it into cache...", state.getId());
				addActions(actions, state);
			}

			LOG.info("There are {} states with unfired actions", statesWithCandidates.size());
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @return If there are any pending actions to be crawled (and no task is being crawled).
	 */
	public boolean isEmpty() {
		consumersReadLock.lock();
		try {
			return runningConsumers == 0 && statesWithCandidates.isEmpty();
		} finally {
			consumersReadLock.unlock();
		}
	}

	/**
	 * @return A new crawl task as soon as one is ready. Until then, it blocks.
	 * @throws InterruptedException
	 *             when taking from the queue is interrupted.
	 * @see #taskDone()
	 */
	StateVertex awaitNewTask() throws InterruptedException {
		int id = statesWithCandidates.take();
		consumersWriteLock.lock();
		try {
			// Put it back the end of the queue. It will be removed later.
			statesWithCandidates.add(id);
			runningConsumers++;
		} finally {
			consumersWriteLock.unlock();
		}
		LOG.debug("New task polled for state {}", id);
		LOG.debug("There are {} active consumers", runningConsumers);
		LOG.info("There are {} states with unfired actions", statesWithCandidates.size());
		return sfg.get().getById(id);
	}

	public void purgeActionsForState(StateVertex crawlTask) {
		removeStateInCache(crawlTask.getId(), crawlTask.getName());
	}

	/**
	 * This method is for robot to restart the crawler
	 */
	public void retainInitialStateAndRemoveOthers() {
	    consumersWriteLock.lock();
	    try {
            for (int crawlTask : statesWithCandidates) {
                if (crawlTask != 0)
                    removeStateInCache(crawlTask, String.valueOf(crawlTask));
            }
        } finally {
            consumersWriteLock.unlock();
        }
	}

	private void removeStateInCache(int id, String name) {
		Lock lock = locks.get(id);
		try {
			lock.lock();
			LOG.debug("Removing tasks for target state {}", name);
			removeStateFromQueue(id);
			Queue<CandidateCrawlAction> removed = cache.remove(id);
			if (removed != null) {
				unfiredActionsCount.inc(removed.size());
			}
		} finally {
			lock.unlock();
			crawlerLostCount.inc();
		}
	}

	public void removeAllStateInCache() {
        consumersWriteLock.lock();
        try {
            for (int crawlTask : statesWithCandidates) {
                removeStateInCache(crawlTask, String.valueOf(crawlTask));
            }
        } finally {
            consumersWriteLock.unlock();
        }
    }

	/**
	 * Indicates that a task is done.
	 * <p>
	 * Should be called after processing a task.
	 * 
	 * @see #awaitNewTask()
	 */
	void taskDone() {
		consumersWriteLock.lock();
		try {
			runningConsumers--;
		} finally {
			consumersWriteLock.unlock();
		}
	}
}
