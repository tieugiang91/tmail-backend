package com.linagora.tmail.james;

import org.apache.james.CassandraExtension;
import org.apache.james.JamesServerBuilder;
import org.apache.james.JamesServerExtension;
import org.apache.james.jmap.draft.JMAPDraftConfiguration;
import org.apache.james.mailbox.opendistro.DockerOpenDistroExtension;
import org.apache.james.mailbox.opendistro.DockerOpenDistroSingleton;
import org.apache.james.modules.AwsS3BlobStoreExtension;
import org.apache.james.modules.RabbitMQExtension;
import org.apache.james.modules.TestJMAPServerModule;
import org.apache.james.utils.GuiceProbe;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.common.collect.ImmutableList;
import com.google.inject.multibindings.Multibinder;
import com.linagora.tmail.blob.blobid.list.BlobStoreConfiguration;
import com.linagora.tmail.encrypted.MailboxConfiguration;
import com.linagora.tmail.encrypted.cassandra.EncryptedEmailContentStoreCassandraModule;
import com.linagora.tmail.james.app.DistributedJamesConfiguration;
import com.linagora.tmail.james.app.DistributedServer;
import com.linagora.tmail.james.common.LinagoraShortLivedTokenRoutesContract;
import com.linagora.tmail.james.common.module.JmapGuiceKeystoreManagerModule;
import com.linagora.tmail.james.common.probe.JmapGuiceEncryptedEmailContentStoreProbe;
import com.linagora.tmail.module.LinagoraTestJMAPServerModule;

public class DistributedShortLivedTokenRoutesTest implements LinagoraShortLivedTokenRoutesContract {

    @RegisterExtension
    static JamesServerExtension
        testExtension = new JamesServerBuilder<DistributedJamesConfiguration>(tmpDir ->
        DistributedJamesConfiguration.builder()
            .workingDirectory(tmpDir)
            .configurationFromClasspath()
            .blobStore(BlobStoreConfiguration.builder()
                .disableCache()
                .deduplication()
                .noCryptoConfig()
                .disableSingleSave())
            .mailbox(new MailboxConfiguration(true))
            .build())
        .extension(new DockerOpenDistroExtension(DockerOpenDistroSingleton.INSTANCE))
        .extension(new CassandraExtension())
        .extension(new RabbitMQExtension())
        .extension(new AwsS3BlobStoreExtension())
        .server(configuration -> DistributedServer.createServer(configuration)
            .overrideWith(new LinagoraTestJMAPServerModule())
            .overrideWith(new JmapGuiceKeystoreManagerModule())
            .overrideWith(new EncryptedEmailContentStoreCassandraModule())
            .overrideWith(binder -> Multibinder.newSetBinder(binder, GuiceProbe.class)
                .addBinding()
                .to(JmapGuiceEncryptedEmailContentStoreProbe.class))
            .overrideWith(binder -> binder.bind(JMAPDraftConfiguration.class)
                .toInstance(TestJMAPServerModule
                    .jmapDraftConfigurationBuilder()
                    .jwtPublicKeyPem(ImmutableList.of(LinagoraTestJMAPServerModule.JWT_PUBLIC_PEM_KEY))
                    .build())))
        .build();
}
