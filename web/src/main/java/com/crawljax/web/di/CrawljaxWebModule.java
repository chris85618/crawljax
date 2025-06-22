package com.crawljax.web.di;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import com.crawljax.web.LogWebSocketServlet;
import com.crawljax.web.fs.WorkDirManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.ServletContainer;

public class CrawljaxWebModule extends ServletModule {

	private final File outputFolder;
	private final File pluginsFolder;

	@BindingAnnotation
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface OutputFolder {
	}

	@BindingAnnotation
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface PluginsFolder {
	}

	public CrawljaxWebModule(File outputFolder, File pluginsFolder) {
		this.outputFolder = outputFolder;
		this.pluginsFolder = pluginsFolder;
	}

	@Override
	protected void configureServlets() {

		serve("/socket*").with(LogWebSocketServlet.class);

		final Map<String, String> params = Maps.newHashMap();
		params.put("com.sun.jersey.config.property.packages", "com.crawljax.web.jaxrs");
		params.put(ServletContainer.PROPERTY_WEB_PAGE_CONTENT_REGEX,
		        "/.*\\.(html|js|gif|png|css|ico)");
		filter("/rest/*").through(GuiceContainer.class, params);

		bind(File.class).annotatedWith(OutputFolder.class).toInstance(outputFolder);

		bind(WorkDirManager.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	public ObjectMapper provideObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new GuavaModule());
		return mapper;
	}

	@Provides
	@Singleton
	public JacksonJsonProvider provideJacksonJsonProvider(ObjectMapper mapper) {
		return new JacksonJsonProvider(mapper);
	}

	@Provides
	@PluginsFolder
	private File pluginsFolder() {
		if (!pluginsFolder.exists()) {
			pluginsFolder.mkdirs();
		}
		Preconditions.checkArgument(pluginsFolder.canWrite(), "Plugin directory is writable");
		return pluginsFolder;
	}

}
