package com.avsmc.procurement.config.audit;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Wraps the auto-configured {@link DataSource} so that every pooled
 * connection carries the current request's actor as PostgreSQL session
 * variables (app.current_user_id / app.current_org_id) before any statement
 * executes. The audit triggers in audit.fn_audit_trigger read these variables,
 * so writes performed on a pooled connection are attributed to the acting
 * user even though connections are reused across requests.
 *
 * Variables are re-synced whenever the actor changes (or is cleared) for the
 * physical connection, which makes reuse across tenants safe.
 */
@Component
public class AuditAwareDataSourcePostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource && !isAlreadyWrapped(dataSource)) {
            return new AuditAwareDataSource(dataSource);
        }
        return bean;
    }

    private boolean isAlreadyWrapped(DataSource dataSource) {
        return dataSource instanceof AuditAwareDataSource;
    }

    static final class AuditAwareDataSource implements DataSource {

        private final DataSource delegate;

        AuditAwareDataSource(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return AuditAwareDataSourcePostProcessor.wrap(delegate.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return AuditAwareDataSourcePostProcessor.wrap(delegate.getConnection(username, password));
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) return iface.cast(this);
            return delegate.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return iface.isInstance(this) || delegate.isWrapperFor(iface);
        }

        @Override
        public java.io.PrintWriter getLogWriter() { return null; }

        @Override
        public void setLogWriter(java.io.PrintWriter out) { }

        @Override
        public void setLoginTimeout(int seconds) { }

        @Override
        public int getLoginTimeout() { return 0; }

        @Override
        public java.util.logging.Logger getParentLogger() {
            return java.util.logging.Logger.getGlobal();
        }
    }

    static Connection wrap(Connection target) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new AuditAwareInvocationHandler(target));
    }

    private static final class AuditAwareInvocationHandler implements InvocationHandler {

        private final Connection target;
        private UUID syncedUserId;
        private UUID syncedOrgId;

        AuditAwareInvocationHandler(Connection target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (name.startsWith("prepareStatement") || name.equals("createStatement")
                    || name.equals("prepareCall")) {
                syncAuditVariables();
            }
            try {
                return method.invoke(target, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private void syncAuditVariables() throws SQLException {
            AuditContext.Actor actor = AuditContext.get();
            UUID userId = actor != null ? actor.userId() : null;
            UUID orgId = actor != null ? actor.organisationId() : null;

            if (java.util.Objects.equals(userId, syncedUserId)
                    && java.util.Objects.equals(orgId, syncedOrgId)) {
                return;
            }

            try (Statement st = target.createStatement()) {
                st.execute("SET app.current_user_id = '" + (userId != null ? userId : "") + "'");
                st.execute("SET app.current_org_id = '" + (orgId != null ? orgId : "") + "'");
            } catch (SQLException e) {
                // A failure to sync audit metadata must not break the business write.
                syncedUserId = null;
                syncedOrgId = null;
                return;
            }
            syncedUserId = userId;
            syncedOrgId = orgId;
        }
    }
}
