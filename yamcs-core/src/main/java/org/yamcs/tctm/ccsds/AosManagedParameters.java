package org.yamcs.tctm.ccsds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yamcs.ConfigurationException;
import org.yamcs.YConfiguration;

public class AosManagedParameters implements ManagedParameters {
    enum ServiceType {
        /** Multiplexing Protocol Data Unit */
        M_PDU,
        /** Bitstream Protocol Data Unit */
        B_PDU,
        /** Virtual Channel Access Service Data Unit */
        VCA_SDU, 
        /** IDLE frames are those with vcId = 63*/
        IDLE
    };

    String physicalChannelName;
    int frameLength;

    int spacecraftId;
    boolean frameHeaderErrorControlPresent;
    int insertZoneLength; // 0 means insert zone not present
    boolean frameErroControlPresent;
    Map<Integer, VcManagedParameters> vcParams = new HashMap<>();

    static class VcManagedParameters {
        int vcId;
        ServiceType type;
        boolean ocfPresent;

        // if type = M_PDU
        int maxPacketSize;
        String packetPreprocessorClassName;
        Map<String, Object> packetPreprocessorArgs;

        static VcManagedParameters parseConfig(Map<String, Object> config) {
            VcManagedParameters vmp = new VcManagedParameters();
            vmp.vcId = YConfiguration.getInt(config, "vcId");
            if (vmp.vcId < 0 || vmp.vcId > 62) {
                throw new ConfigurationException("Invalid vcId: " + vmp.vcId);
            }
            vmp.type = YConfiguration.getEnum(config, "type", ServiceType.class);
            vmp.ocfPresent = YConfiguration.getBoolean(config, "ocfPresent");
            if (vmp.type == ServiceType.M_PDU) {
                vmp.maxPacketSize = YConfiguration.getInt(config, "maxPacketSize");
                if (vmp.maxPacketSize < 7) {
                    throw new ConfigurationException("invalid maxPacketSize: " + vmp.maxPacketSize);
                }
            }
            return vmp;
        }
    }

    public static AosManagedParameters parseConfig(Map<String, Object> config) {
        AosManagedParameters amp = new AosManagedParameters();

        if (config.containsKey("physicalChannelName")) {
            amp.physicalChannelName = YConfiguration.getString(config, "physicalChannelName");
        }
        amp.frameLength = YConfiguration.getInt(config, "frameLength");
        if (amp.frameLength < 8 || amp.frameLength > 0xFFFF) {
            throw new ConfigurationException("Invalid frame length " + amp.frameLength);
        }
        amp.frameHeaderErrorControlPresent = YConfiguration.getBoolean(config, "frameHeaderErrorControlPresent");
        amp.frameErroControlPresent = YConfiguration.getBoolean(config, "frameErroControlPresent");
        amp.insertZoneLength = YConfiguration.getInt(config, "insertZoneLength");

        if (amp.insertZoneLength < 0 || amp.insertZoneLength > amp.frameLength - 6) {
            throw new ConfigurationException("Invalid insert zone length " + amp.insertZoneLength);
        }

        List<Map<String, Object>> l = YConfiguration.getList(config, "virtualChannels");
        for (Map<String, Object> m : l) {
            VcManagedParameters vmp = VcManagedParameters.parseConfig(m);
            if (amp.vcParams.containsKey(vmp.vcId)) {
                throw new ConfigurationException("duplicate configuration of vcId " + vmp.vcId);
            }
            amp.vcParams.put(vmp.vcId, vmp);
        }
        return amp;
    }

    @Override
    public int getMaxFrameLength() {
        return frameLength;
    }

    @Override
    public int getMinFrameLength() {
        return frameLength;
    }

    @Override
    public Map<Integer, VirtualChannelHandler> createVcHandlers(String yamcsInstance) {
        Map<Integer, VirtualChannelHandler> m = new HashMap<>();
        for (Map.Entry<Integer, VcManagedParameters> me : vcParams.entrySet()) {
            VcManagedParameters vmp = me.getValue();
            switch (vmp.type) {
            case B_PDU:
                throw new UnsupportedOperationException("B_PDU not supported (TODO)");
            case M_PDU:
                VirtualChannelPacketHandler vcph = new VirtualChannelPacketHandler(yamcsInstance, vmp.packetPreprocessorClassName, vmp.packetPreprocessorArgs);
                m.put(vmp.vcId, vcph);
                break;
            case VCA_SDU:
                throw new UnsupportedOperationException("VCA_SDU not supported (TODO)");
            case IDLE:
                m.put(vmp.vcId, new IdleFrameHandler());
                break;
            }
        }
        return m;
    }
}
