package org.bigconnect.example.auth;

import com.google.inject.Singleton;
import com.google.inject.Inject;
import com.mware.core.exception.BcAccessDeniedException;
import com.mware.core.model.user.UserNameAuthorizationContext;
import com.mware.core.model.user.UserRepository;
import com.mware.core.user.User;
import com.mware.web.CurrentUser;
import com.mware.web.framework.ParameterizedHandler;
import com.mware.web.framework.annotations.Handle;
import com.mware.web.framework.annotations.Required;
import com.mware.web.util.RemoteAddressUtil;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

@Singleton
public class Login implements ParameterizedHandler {

    private final UserRepository userRepository;

    @Inject
    public Login(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Handle
    public JSONObject handle(
            HttpServletRequest request,
            @Required(name = "username") String username,
            @Required(name = "password") String password
    ) throws Exception {
        username = username.trim();
        password = password.trim();

        if (isValid(username, password)) {
            User user = findOrCreateUser(username);
            userRepository.updateUser(user, new UserNameAuthorizationContext(username, RemoteAddressUtil.getClientIpAddr(request)));
            CurrentUser.set(request, user.getUserId(), user.getUsername());
            JSONObject json = new JSONObject();
            json.put("status", "OK");
            return json;
        } else {
            throw new BcAccessDeniedException("", null, null);
        }
    }

    private User findOrCreateUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            // For form based authentication, username and displayName will be the same
            String randomPassword = UserRepository.createRandomPassword();
            user = userRepository.findOrAddUser(
                    username,
                    username,
                    null,
                    randomPassword
            );
        }
        return user;
    }

    private boolean isValid(String username, String password) {
        return username.equals(password);
    }
}
