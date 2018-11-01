package org.bigconnect.example.auth;

import com.google.inject.Singleton;
import com.google.inject.Inject;
import com.mware.web.framework.Handler;
import com.mware.core.model.Description;
import com.mware.core.model.Name;
import com.mware.web.AuthenticationHandler;
import com.mware.web.WebApp;
import com.mware.web.WebAppPlugin;

import javax.servlet.ServletContext;

@Name("Example BigConnect Authentication Plugin")
@Description("Registers an authentication plugin which demonstrates user/password login.")
@Singleton
public class ExampleAuthenticationPlugin implements WebAppPlugin {
    private final Login login;

    @Inject
    public ExampleAuthenticationPlugin(Login login) {
        this.login = login;
    }

    @Override
    public void init(WebApp app, ServletContext servletContext, Handler authenticationHandler) {
        app.registerBeforeAuthenticationJavaScript("/org/bigconnect/example/auth/plugin.js");
        app.registerJavaScript("/org/bigconnect/example/auth/authentication.js", false);
        app.registerJavaScriptTemplate("/org/bigconnect/example/auth/login.hbs");
        app.registerCss("/org/bigconnect/example/auth/login.css");
        app.registerResourceBundle("/org/bigconnect/example/auth/messages.properties");

        app.post(AuthenticationHandler.LOGIN_PATH, login);
    }
}
