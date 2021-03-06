package io.metersphere.security;


import io.metersphere.base.domain.Role;
import io.metersphere.commons.constants.UserSource;
import io.metersphere.commons.user.SessionUser;
import io.metersphere.commons.utils.SessionUtils;
import io.metersphere.dto.UserDTO;
import io.metersphere.i18n.Translator;
import io.metersphere.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 自定义Realm 注入service 可能会导致在 service的aop 失效，例如@Transactional,
 * 解决方法：
 * <p>
 * 1. 这里改成注入mapper，这样mapper 中的事务失效<br/>
 * 2. 这里仍然注入service，在配置ShiroConfig 的时候不去set realm, 等到spring 初始化完成之后
 * set realm
 * </p>
 */
public class ShiroDBRealm extends AuthorizingRealm {

    private Logger logger = LoggerFactory.getLogger(ShiroDBRealm.class);
    @Resource
    private UserService userService;

    @Value("${run.mode:release}")
    private String runMode;

    /**
     * 权限认证
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

        String userid = (String) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();

        // roles 内容填充
        UserDTO userDTO = userService.getUser(userid);
        Set<String> roles = userDTO.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        authorizationInfo.setRoles(roles);

        return authorizationInfo;
    }

    /**
     * 登录认证
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authenticationToken;
        String login = (String) SecurityUtils.getSubject().getSession().getAttribute("authenticate");

        String username = token.getUsername();
        String password = String.valueOf(token.getPassword());

        String userId;

        if (StringUtils.equals("local", runMode)) {
            UserDTO user = getUserWithOutAuthenticate(username);
            userId = user.getUser_id();
            SessionUser sessionUser = SessionUser.fromUser(user);
            SessionUtils.putUser(sessionUser);
            return new SimpleAuthenticationInfo(userId, password, getName());
        }

        UserDTO user = getUserWithOutAuthenticate(username);
        userId = user.getUser_id();

        if (StringUtils.equals(login, UserSource.LOCAL.name())) {
            return loginLocalMode(userId, password);
        }

        if (StringUtils.equals(login, UserSource.LDAP.name())) {
            return loginLdapMode(userId, password);
        }

        SessionUser sessionUser = SessionUser.fromUser(user);
        SessionUtils.putUser(sessionUser);
        return new SimpleAuthenticationInfo(userId, password, getName());

    }

    private UserDTO getUserWithOutAuthenticate(String username) {
        UserDTO user = userService.getUser(username);
        String msg;
        if (user == null) {
            user = userService.getUserDTOByEmail(user.getUser_id());
            if (user == null) {
                msg = "The user does not exist: " + user.getUser_id();
                logger.warn(msg);
                throw new UnknownAccountException(Translator.get("user_not_exist") + user.getUser_id());
            }
        }
        return user;
    }


    private AuthenticationInfo loginLdapMode(String userId, String password) {
        // userId 或 email 有一个相同就返回User
        String email = (String) SecurityUtils.getSubject().getSession().getAttribute("email");
        UserDTO user = userService.getLoginUser(userId, Arrays.asList(UserSource.LDAP.name(), UserSource.LOCAL.name()));
        String msg;
        if (user == null) {
            user = userService.getUserDTOByEmail(email, UserSource.LDAP.name(), UserSource.LOCAL.name());
            if (user == null) {
                msg = "The user does not exist: " + userId;
                logger.warn(msg);
                throw new UnknownAccountException(Translator.get("user_not_exist") + userId);
            }
            userId = user.getId();
        }

        SessionUser sessionUser = SessionUser.fromUser(user);
        SessionUtils.putUser(sessionUser);
        return new SimpleAuthenticationInfo(userId, password, getName());

    }

    private AuthenticationInfo loginLocalMode(String userId, String password) {
        UserDTO user = userService.getLoginUser(userId, Collections.singletonList(UserSource.LOCAL.name()));
        String msg;
        if (user == null) {
            user = userService.getUserDTOByEmail(userId, UserSource.LOCAL.name());
            if (user == null) {
                msg = "The user does not exist: " + userId;
                logger.warn(msg);
                throw new UnknownAccountException(Translator.get("user_not_exist") + userId);
            }
            userId = user.getId();
        }
        // 密码验证
        if (!userService.checkUserPassword(userId, password)) {
            throw new IncorrectCredentialsException(Translator.get("password_is_incorrect"));
        }
        //
        SessionUser sessionUser = SessionUser.fromUser(user);
        SessionUtils.putUser(sessionUser);
        return new SimpleAuthenticationInfo(userId, password, getName());
    }

    @Override
    public boolean isPermitted(PrincipalCollection principals, String permission) {
        return true;
    }
}
