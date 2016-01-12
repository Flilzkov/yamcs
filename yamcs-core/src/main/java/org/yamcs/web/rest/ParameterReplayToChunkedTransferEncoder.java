package org.yamcs.web.rest;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.api.MediaType;
import org.yamcs.protobuf.Pvalue.ParameterData;
import org.yamcs.protobuf.Yamcs.ReplayStatus;
import org.yamcs.web.HttpException;
import org.yamcs.web.HttpHandler;
import org.yamcs.web.HttpHandler.ChunkedTransferStats;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelFuture;

/**
 * Reads a yamcs replay and maps it directly to an output buffer. If that buffer grows larger
 * than the treshold size for one chunk, this will cause a chunk to be written out.
 * Could maybe be replaced by using built-in netty functionality, but would need to investigate.
 */
public abstract class ParameterReplayToChunkedTransferEncoder extends RestParameterReplayListener {
    
    private static final Logger log = LoggerFactory.getLogger(ParameterReplayToChunkedTransferEncoder.class);
    private static final int CHUNK_TRESHOLD = 8096;
    
    private ByteBuf buf;
    protected ByteBufOutputStream bufOut;
    private ChannelFuture lastChannelFuture;
    
    protected RestRequest req;
    protected MediaType contentType;
    protected boolean failed = false;
    private ChunkedTransferStats stats;
    
    public ParameterReplayToChunkedTransferEncoder(RestRequest req, MediaType contentType) throws HttpException {
        super();
        this.req = req;
        this.contentType = contentType;
        resetBuffer();
        lastChannelFuture = HttpHandler.startChunkedTransfer(req.getChannelHandlerContext(), contentType);
        stats = req.getChannelHandlerContext().attr(HttpHandler.CTX_CHUNK_STATS).get();
    }
    
    protected void resetBuffer() {
        buf = req.getChannelHandlerContext().alloc().buffer();
        bufOut = new ByteBufOutputStream(buf);
    }
    
    protected void closeBufferOutputStream() throws IOException {
        bufOut.close();
    }
    
    @Override
    public void onParameterData(ParameterData pdata) {
        if (failed) {
            log.warn("Already failed. Ignoring parameter data");
            return;
        }
        try {
            processParameterData(pdata, bufOut);
            if (buf.readableBytes() >= CHUNK_TRESHOLD) {
                closeBufferOutputStream();
                writeChunk();
                resetBuffer();
            }
        } catch (IOException e) {
            log.error("Closing replay due to IO error", e);
            failed = true;
            requestReplayAbortion();
        }
    }
    
    public abstract void processParameterData(ParameterData pdata, ByteBufOutputStream bufOut) throws IOException;
    
    @Override
    public void stateChanged(ReplayStatus rs) {
        if (failed) {
            log.warn("Closing channel because transfer failed");
            req.getChannelHandlerContext().channel().close();
            return;
        }
        try {
            closeBufferOutputStream();
            if (buf.readableBytes() > 0) {
                writeChunk();
            }
            HttpHandler.stopChunkedTransfer(req.getChannelHandlerContext(), lastChannelFuture);
        } catch (IOException e) {
            log.error("Could not write final chunk of data", e);
        }
    }
    
    private void writeChunk() throws IOException {
        stats.totalBytes += buf.readableBytes();
        stats.chunkCount++;
        lastChannelFuture = HttpHandler.writeChunk(req.getChannelHandlerContext(), buf);
    }
}
