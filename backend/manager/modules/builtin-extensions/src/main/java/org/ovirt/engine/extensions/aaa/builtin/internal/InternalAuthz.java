package org.ovirt.engine.extensions.aaa.builtin.internal;

import java.util.Arrays;
import java.util.Properties;

import org.ovirt.engine.api.extensions.Base;
import org.ovirt.engine.api.extensions.ExtMap;
import org.ovirt.engine.api.extensions.ExtUUID;
import org.ovirt.engine.api.extensions.Extension;
import org.ovirt.engine.api.extensions.aaa.Authn;
import org.ovirt.engine.api.extensions.aaa.Authz;

/**
 * This directory contains only the internal user as specified in the {@code AdminUser} configuration parameter.
 */
public class InternalAuthz implements Extension {

    private ExtMap context;

    private ExtMap adminUser;

    private static class Opaque {

        private boolean firstCall;
        private boolean isUser;

        public Opaque(boolean isUser) {
            firstCall = true;
            this.isUser = isUser;
        }
    }

    @Override
    public void invoke(ExtMap input, ExtMap output) {
        try {
            ExtUUID command = input.<ExtUUID> get(Base.InvokeKeys.COMMAND);
            if (command.equals(Base.InvokeCommands.INITIALIZE)) {
                doInit(input, output);
            } else if (command.equals(Authz.InvokeCommands.FETCH_PRINCIPAL_RECORD)) {
                doFetchPrincipalRecord(input, output);
            } else if (command.equals(Authz.InvokeCommands.QUERY_CLOSE)) {
                // Do nothing
            } else if (command.equals(Authz.InvokeCommands.QUERY_OPEN)) {
                output.put(Authz.InvokeKeys.QUERY_OPAQUE, new Opaque(input.<ExtUUID> get(Authz.InvokeKeys.QUERY_ENTITY)
                        .equals(Authz.QueryEntity.PRINCIPAL)));
            } else if (command.equals(Authz.InvokeCommands.QUERY_EXECUTE)) {
                doQueryExecute(input, output);
            } else {
                output.put(
                        Base.InvokeKeys.RESULT,
                        Base.InvokeResult.UNSUPPORTED
                        );
            }
            output.putIfAbsent(Authz.InvokeKeys.STATUS, Authz.Status.SUCCESS);
            output.putIfAbsent(Base.InvokeKeys.RESULT, Base.InvokeResult.SUCCESS);
        } catch (Exception ex) {
            output.mput(
                    Base.InvokeKeys.RESULT,
                    Base.InvokeResult.FAILED
                    ).mput(
                            Base.InvokeKeys.MESSAGE,
                            ex.getMessage()
                    );
        }
    }

    private void doQueryExecute(ExtMap input, ExtMap output) {
        Opaque opaque = input.<Opaque> get(Authz.InvokeKeys.QUERY_OPAQUE);
        output.put(Authz.InvokeKeys.QUERY_RESULT,
                opaque.firstCall && opaque.isUser ? Arrays.asList(adminUser)
                        : null);
        opaque.firstCall = false;
    }

    private void doFetchPrincipalRecord(ExtMap input, ExtMap output) {
        String principal = input.<ExtMap> get(Authn.InvokeKeys.AUTH_RECORD).get(Authn.AuthRecord.PRINCIPAL);
        if (principal.equals(adminUser.<String> get(Authz.PrincipalRecord.NAME))) {
            output.put(Authz.InvokeKeys.PRINCIPAL_RECORD, adminUser);
        }
    }

    private void doInit(ExtMap input, ExtMap output) {
        context = input.<ExtMap> get(Base.InvokeKeys.CONTEXT);
        Properties configuration = context.<Properties> get(Base.ContextKeys.CONFIGURATION);
        context.mput(
                Base.ContextKeys.AUTHOR,
                "The oVirt Project"
                ).mput(
                        Base.ContextKeys.EXTENSION_NAME,
                        "Internal Authz (Built-in)"
                ).mput(
                        Base.ContextKeys.LICENSE,
                        "ASL 2.0"
                ).mput(
                        Base.ContextKeys.HOME_URL,
                        "http://www.ovirt.org"
                ).mput(
                        Base.ContextKeys.VERSION,
                        "N/A"
                ).mput(
                        Authz.ContextKeys.QUERY_MAX_FILTER_SIZE,
                        configuration.get("config.query.filter.size")
                ).mput(
                        Base.ContextKeys.BUILD_INTERFACE_VERSION,
                        Base.INTERFACE_VERSION_CURRENT
                ).mput(
                        Authz.ContextKeys.AVAILABLE_NAMESPACES,
                        Arrays.asList("*")
                        );
        adminUser = new ExtMap().mput(
                Authz.PrincipalRecord.NAME,
                configuration.getProperty("config.authz.user.name")
                ).mput(
                        Authz.PrincipalRecord.ID,
                        configuration.getProperty("config.authz.user.id")
                );

    }

}
