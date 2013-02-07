package org.solrmarc.marcoverride;

import java.util.Properties;

import org.marc4j.marc.Leader;
import org.marc4j.marc.Record;
import org.marc4j.marc.impl.MarcFactoryImpl;
import org.solrmarc.tools.Utils;

/**
 * 
 * @author Robert Haschart
 * @version $Id: UVAMarcFactoryImpl.java 19 2008-06-20 14:58:26Z wayne.graham $
 *
 */
public class RemapMarcFactoryImpl  extends MarcFactoryImpl
{
    private static String remapPropertiesFilename = null;
    private static Properties remapProperties = null;

    public static Properties getRemapProperties()
    {
        return remapProperties;
    }

    public RemapMarcFactoryImpl() 
    {
        remapPropertiesFilename = System.getProperty("marc.override.reader.remapURL", null);
        remapProperties = Utils.loadProperties(remapPropertiesFilename);
    }
    
    public Record newRecord(Leader leader) {
        RemapRecordImpl record = new RemapRecordImpl();
        record.setLeader(leader);
        return record;
    }

}
