package org.graylog.plugins.netflow.codecs;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.netflow.flows.FlowException;
import org.graylog.plugins.netflow.flows.NetFlowParser;
import org.graylog.plugins.netflow.flows.NetFlow;
import org.graylog.plugins.netflow.flows.NetFlowPacket;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.MultiMessageCodec;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

@Codec(name = "netflow", displayName = "NetFlow")
public class NetFlowCodec extends AbstractCodec implements MultiMessageCodec {
    private static final Logger LOG = LoggerFactory.getLogger(NetFlowCodec.class);

    @Inject
    protected NetFlowCodec(@Assisted Configuration configuration) {
        super(configuration);
    }

    @Nullable
    @Override
    public Message decode(RawMessage rawMessage) {
        throw new UnsupportedOperationException("MultiMessageCodec " + getClass() + " does not support decode()");
    }

    @Nullable
    @Override
    public Collection<Message> decodeMessages(@Nonnull RawMessage rawMessage) {
        try {
            final NetFlowPacket packet = NetFlowParser.parse(rawMessage);

            if (packet == null) {
                return null;
            }

            final List<Message> messages = Lists.newArrayListWithCapacity(packet.getFlows().size());

            for (NetFlow flow : packet.getFlows()) {
                messages.add(flow.toMessage());
            }

            return messages;
        } catch (FlowException e) {
            LOG.error("Error parsing NetFlow packet", e);
            return null;
        }
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<NetFlowCodec> {
        @Override
        NetFlowCodec create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {
            if (cr.containsField(NettyTransport.CK_PORT)) {
                cr.getField(NettyTransport.CK_PORT).setDefaultValue(2055);
            }
        }
    }
}