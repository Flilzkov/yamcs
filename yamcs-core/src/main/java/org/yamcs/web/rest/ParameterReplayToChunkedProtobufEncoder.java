package org.yamcs.web.rest;

import java.io.IOException;

import org.yamcs.protobuf.Pvalue.ParameterData;
import org.yamcs.protobuf.Pvalue.ParameterValue;
import org.yamcs.api.MediaType;
import org.yamcs.protobuf.SchemaPvalue;
import org.yamcs.web.HttpException;

import com.fasterxml.jackson.core.JsonGenerator;

import io.netty.buffer.ByteBufOutputStream;
import io.protostuff.JsonIOUtil;

/**
 * Facilitates working with chunked transfer of gpb parameters
 */
public class ParameterReplayToChunkedProtobufEncoder extends ParameterReplayToChunkedTransferEncoder {
    
    public ParameterReplayToChunkedProtobufEncoder(RestRequest req) throws HttpException {
        super(req, req.deriveTargetContentType());
    }
    
    @Override
    public void processParameterData(ParameterData pdata, ByteBufOutputStream bufOut) throws IOException {
        for (ParameterValue pval : pdata.getParameterList()) {
            if (MediaType.PROTOBUF.equals(contentType)) {
                pval.writeDelimitedTo(bufOut);
            } else {
                JsonGenerator generator = req.createJsonGenerator(bufOut);
                JsonIOUtil.writeTo(generator, pval, SchemaPvalue.ParameterValue.WRITE, false);
                generator.close();
            }
        }
    }
}
