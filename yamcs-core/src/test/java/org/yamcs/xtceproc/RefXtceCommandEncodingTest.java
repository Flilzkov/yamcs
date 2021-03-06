package org.yamcs.xtceproc;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.yamcs.ConfigurationException;
import org.yamcs.ErrorInCommand;
import org.yamcs.ProcessorConfig;
import org.yamcs.YConfiguration;
import org.yamcs.parameter.Value;
import org.yamcs.utils.ByteArrayUtils;
import org.yamcs.utils.StringConverter;
import org.yamcs.utils.TimeEncoding;
import org.yamcs.xtce.ArgumentAssignment;
import org.yamcs.xtce.MetaCommand;
import org.yamcs.xtce.XtceDb;
import org.yamcs.xtceproc.MetaCommandProcessor.CommandBuildResult;

/**
 * Created by msc on 27/05/15.
 */
public class RefXtceCommandEncodingTest {
    static XtceDb xtcedb;
    static MetaCommandProcessor metaCommandProcessor;

    @BeforeClass
    public static void beforeClass() throws ConfigurationException {
        YConfiguration.setupTest(null);
        xtcedb = XtceDbFactory.createInstanceByConfig("refxtce");
        metaCommandProcessor = new MetaCommandProcessor(new ProcessorData("test", "test", xtcedb, new ProcessorConfig()));
    }

    @Test
    public void testAbsTimeArg() throws ErrorInCommand {
        // encode command
        MetaCommand mc = xtcedb.getMetaCommand("/RefXtce/command1");

        String tstring = "2020-01-01T00:00:00.123Z";
        long tlong = TimeEncoding.parse(tstring);
        
        List<ArgumentAssignment> aaList = Arrays.asList(new ArgumentAssignment("t1", tstring), new ArgumentAssignment("t2", tstring));
        CommandBuildResult cbr = metaCommandProcessor.buildCommand(mc, aaList);
        Value v1 = cbr.args.get(mc.getArgument("t1"));
        assertEquals(tlong, v1.getTimestampValue());
        
        Value v2 = cbr.args.get(mc.getArgument("t2"));
        assertEquals(tlong, v2.getTimestampValue());
        
        byte[] cmdb = cbr.getCmdPacket();
        assertEquals(8, cmdb.length);
        
        int gpsTime = ByteArrayUtils.decodeInt(cmdb, 0);
        assertEquals(TimeEncoding.toGpsTimeMillisec(tlong)/1000, gpsTime);
        
        int unixTime = ByteArrayUtils.decodeInt(cmdb, 4);
        assertEquals(Instant.parse(tstring).toEpochMilli()/1000, unixTime);
        
    }
}
