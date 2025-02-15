package com.linagora.tmail.combined.identity;

import static org.apache.james.user.ldap.DockerLdapSingleton.ADMIN;
import static org.apache.james.user.ldap.DockerLdapSingleton.ADMIN_LOCAL_PART;
import static org.apache.james.user.ldap.DockerLdapSingleton.ADMIN_PASSWORD;

import java.util.Optional;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.plist.PropertyListConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.james.backends.cassandra.CassandraClusterExtension;
import org.apache.james.core.Username;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.cassandra.CassandraRepositoryConfiguration;
import org.apache.james.user.cassandra.CassandraUsersDAO;
import org.apache.james.user.cassandra.CassandraUsersRepositoryModule;
import org.apache.james.user.ldap.DockerLdapSingleton;
import org.apache.james.user.ldap.LdapGenericContainer;
import org.apache.james.user.ldap.ReadOnlyLDAPUsersDAO;
import org.apache.james.user.lib.model.Algorithm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linagora.tmail.combined.identity.CombinedUsersRepositoryContract.CombinedTestSystem;

public class CombinedUsersRepositoryTest {

    static LdapGenericContainer ldapContainer = DockerLdapSingleton.ldapContainer;

    @BeforeAll
    static void setUpAll() {
        ldapContainer.start();
    }

    @AfterAll
    static void afterAll() {
        ldapContainer.stop();
    }

    @RegisterExtension
    static CassandraClusterExtension cassandraCluster = new CassandraClusterExtension(CassandraUsersRepositoryModule.MODULE);

    @Nested
    class WhenEnableVirtualHosting implements CombinedUsersRepositoryContract.WithVirtualHostingContract {
        @RegisterExtension
        UserRepositoryExtension extension = UserRepositoryExtension.withVirtualHost();
        @RegisterExtension
        CombinedUsersRepositoryContract.CombinedUserRepositoryExtension combinedExtension = CombinedUsersRepositoryContract.CombinedUserRepositoryExtension.withVirtualHost();

        private CombinedUsersRepository combinedUsersRepository;
        private ReadOnlyLDAPUsersDAO readOnlyLDAPUsersDAO;
        private CassandraUsersDAO cassandraUsersDAO;
        private CombinedTestSystem testSystem;

        @BeforeEach
        void setUp(CombinedTestSystem testSystem) throws Exception {
            this.testSystem = testSystem;
            readOnlyLDAPUsersDAO = new ReadOnlyLDAPUsersDAO();
            readOnlyLDAPUsersDAO.configure(ldapRepositoryConfiguration(ldapContainer, true));
            readOnlyLDAPUsersDAO.init();

            cassandraUsersDAO = new CassandraUsersDAO(cassandraCluster.getCassandraCluster().getConf(), CassandraRepositoryConfiguration.DEFAULT);
            combinedUsersRepository = getUsersRepository(new CombinedUserDAO(readOnlyLDAPUsersDAO, cassandraUsersDAO), testSystem.getDomainList(), true, Optional.empty());
        }

        @Override
        public CombinedUsersRepository testee() {
            return combinedUsersRepository;
        }

        @Override
        public UsersRepository testee(Optional<Username> administrator) throws Exception {
            return getUsersRepository(new CombinedUserDAO(readOnlyLDAPUsersDAO, cassandraUsersDAO), testSystem.getDomainList(), extension.isSupportVirtualHosting(), administrator);
        }

        @Override
        public CassandraUsersDAO cassandraUsersDAO() {
            return cassandraUsersDAO;
        }
    }

    @Nested
    class WhenDisableVirtualHosting implements CombinedUsersRepositoryContract.WithOutVirtualHostingContract {
        @RegisterExtension
        UserRepositoryExtension extension = UserRepositoryExtension.withoutVirtualHosting();
        @RegisterExtension
        CombinedUsersRepositoryContract.CombinedUserRepositoryExtension combinedExtension = CombinedUsersRepositoryContract.CombinedUserRepositoryExtension.withoutVirtualHosting();

        private CombinedUsersRepository combinedUsersRepository;
        private ReadOnlyLDAPUsersDAO readOnlyLDAPUsersDAO;
        private CassandraUsersDAO cassandraUsersDAO;
        private CombinedTestSystem testSystem;

        @BeforeEach
        void setUp(CombinedTestSystem testSystem) throws Exception {
            this.testSystem = testSystem;
            readOnlyLDAPUsersDAO = new ReadOnlyLDAPUsersDAO();
            readOnlyLDAPUsersDAO.configure(ldapRepositoryConfiguration(ldapContainer, false));
            readOnlyLDAPUsersDAO.init();

            cassandraUsersDAO = new CassandraUsersDAO(cassandraCluster.getCassandraCluster().getConf(), CassandraRepositoryConfiguration.DEFAULT);

            combinedUsersRepository = getUsersRepository(new CombinedUserDAO(readOnlyLDAPUsersDAO, cassandraUsersDAO), testSystem.getDomainList(), false, Optional.empty());
        }

        @Override
        public CombinedUsersRepository testee() {
            return combinedUsersRepository;
        }

        @Override
        public UsersRepository testee(Optional<Username> administrator) throws Exception {
            return getUsersRepository(new CombinedUserDAO(readOnlyLDAPUsersDAO, cassandraUsersDAO), testSystem.getDomainList(), extension.isSupportVirtualHosting(), administrator);
        }

        @Override
        public CassandraUsersDAO cassandraUsersDAO() {
            return cassandraUsersDAO;
        }
    }

    private static CombinedUsersRepository getUsersRepository(CombinedUserDAO combinedUserDAO,
                                                              DomainList domainList,
                                                              boolean enableVirtualHosting,
                                                              Optional<Username> administrator) throws Exception {
        CombinedUsersRepository repository = new CombinedUsersRepository(domainList, combinedUserDAO);
        BaseHierarchicalConfiguration configuration = new BaseHierarchicalConfiguration();
        configuration.addProperty("enableVirtualHosting", String.valueOf(enableVirtualHosting));
        administrator.ifPresent(username -> configuration.addProperty("administratorId", username.asString()));
        repository.configure(configuration);
        return repository;
    }

    static HierarchicalConfiguration<ImmutableNode> ldapRepositoryConfiguration(LdapGenericContainer ldapContainer, boolean enableVirtualHosting) {
        PropertyListConfiguration configuration = baseConfiguration(ldapContainer);
        if (enableVirtualHosting) {
            configuration.addProperty("[@userIdAttribute]", "mail");
            configuration.addProperty("supportsVirtualHosting", true);
            configuration.addProperty("[@administratorId]", ADMIN.asString());
        } else {
            configuration.addProperty("[@userIdAttribute]", "uid");
            configuration.addProperty("[@administratorId]", ADMIN_LOCAL_PART);
        }
        return configuration;
    }

    private static PropertyListConfiguration baseConfiguration(LdapGenericContainer ldapContainer) {
        PropertyListConfiguration configuration = new PropertyListConfiguration();
        configuration.addProperty("[@ldapHost]", ldapContainer.getLdapHost());
        configuration.addProperty("[@principal]", "cn=admin,dc=james,dc=org");
        configuration.addProperty("[@credentials]", ADMIN_PASSWORD);
        configuration.addProperty("[@userBase]", "ou=People,dc=james,dc=org");
        configuration.addProperty("[@userObjectClass]", "inetOrgPerson");
        configuration.addProperty("[@maxRetries]", "1");
        configuration.addProperty("[@retryStartInterval]", "0");
        configuration.addProperty("[@retryMaxInterval]", "2");
        configuration.addProperty("[@retryIntervalScale]", "1000");
        configuration.addProperty("[@connectionTimeout]", "1000");
        configuration.addProperty("[@readTimeout]", "1000");
        return configuration;
    }

}
