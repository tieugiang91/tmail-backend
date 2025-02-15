/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package com.linagora.tmail.james;

import org.apache.james.GuiceJamesServer;
import org.apache.james.JamesServerBuilder;
import org.apache.james.JamesServerExtension;
import org.apache.james.jmap.rfc8621.contract.EmailQueryMethodContract;
import org.apache.james.junit.categories.Unstable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linagora.tmail.james.app.MemoryConfiguration;
import com.linagora.tmail.james.app.MemoryServer;
import com.linagora.tmail.module.LinagoraTestJMAPServerModule;

public class MemoryEmailQueryMethodTest implements EmailQueryMethodContract {
    @RegisterExtension
    static JamesServerExtension testExtension = new JamesServerBuilder<MemoryConfiguration>(tmpDir ->
        MemoryConfiguration.builder()
            .workingDirectory(tmpDir)
            .configurationFromClasspath()
            .build())
        .server(configuration -> MemoryServer.createServer(configuration)
            .overrideWith(new LinagoraTestJMAPServerModule()))
        .build();

    @Test
    @Override
    @Disabled("JAMES-3377 Not supported for in-memory test")
    public void emailQueryFilterByTextShouldIgnoreMarkupsInHtmlBody(GuiceJamesServer server) {}

    @Test
    @Override
    @Disabled("JAMES-3377 Not supported for in-memory test" +
        "In memory do not attempt message parsing a performs a full match on the raw message content")
    public void emailQueryFilterByTextShouldIgnoreAttachmentContent(GuiceJamesServer server) {}

    @Override
    @Tag(Unstable.TAG)
    public void shouldListMailsReceivedBeforeADate(GuiceJamesServer server) {
        EmailQueryMethodContract.super.shouldListMailsReceivedBeforeADate(server);
    }

    @Override
    @Tag(Unstable.TAG)
    public void shouldListMailsReceivedAfterADate(GuiceJamesServer server) {
        EmailQueryMethodContract.super.shouldListMailsReceivedAfterADate(server);
    }

}
