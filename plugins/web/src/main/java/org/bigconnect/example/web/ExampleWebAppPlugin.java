package org.bigconnect.example.web;

import com.mware.web.framework.Handler;
import com.mware.core.model.Description;
import com.mware.core.model.Name;
import com.mware.web.WebApp;
import com.mware.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Example Web App Plugin")
@Description("Registers a detail toolbar plugin that launches a Google search for the displayed person name.")
public class ExampleWebAppPlugin implements WebAppPlugin {
    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerJavaScript("/org/bigconnect/example/web/plugin.js", true);
        app.registerResourceBundle("/org/bigconnect/example/web/messages.properties");
    }
}
