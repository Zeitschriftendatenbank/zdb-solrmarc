package org.solrmarc.marcoverride;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.IllegalAddException;
import org.marc4j.marc.Subfield;
import org.marc4j.marc.VariableField;
import org.marc4j.marc.impl.Verifier;

/**
 * 
 * @author Robert Haschart
 * @version $Id: UVARecordImpl.java 1240 2010-09-03 20:55:17Z rh9ec@virginia.edu $
 *
 */
public class RemapRecordImpl extends NoSortRecordImpl
{

    /**
	 * 
	 */
    private static String remapPropertiesFilename = null;
    private static Properties remapProperties = null;
	
	public RemapRecordImpl()
    {
        super();
    }
    
    public void addVariableField(final VariableField field) 
    {
        Properties remapProperties = RemapMarcFactoryImpl.getRemapProperties();
        if (!(field instanceof VariableField)){
            throw new IllegalAddException("Expected VariableField instance");
        }

        String tag = field.getTag();
        if (Verifier.isControlNumberField(tag)) 
        {
            ControlField cfield = getControlNumberField();
            if (cfield != null)
            {
                if (!((ControlField)field).getData().startsWith("u") && 
                       cfield.getData().startsWith("u") )
                {
                    // ditch it!
                    return;
                }
            }
        }
        else if (remapProperties.containsKey(tag))
        {
            List<Subfield> subfields = ((DataField)field).getSubfields();
            List<Subfield> todelete = new ArrayList<Subfield>();
            for (int i = 0; remapProperties.containsKey(tag+"_"+i); i++)
            {
                String remapString = remapProperties.getProperty(tag+"_"+i);
                String mapParts[] = remapString.split("=>");
                if (eval(mapParts[0], (DataField)field))
                {
                    process(mapParts[1], (DataField)field, todelete);
                }
            }

            if (todelete.size() != 0)
            {
                for (Subfield sf : todelete)
                {
                    ((DataField)field).removeSubfield(sf);
                }
            }

        }
        super.addVariableField(field);
    }

    private boolean eval(String conditional, DataField field)
    {
        List<Subfield> subfields;
        if (conditional.startsWith("subfieldmatches("))
        {
            String args[] = getTwoArgs(conditional);
            if (args.length == 2 && args[0].length() == 1)
            {
                subfields = field.getSubfields(args[0].charAt(0));
                for (Subfield sf : subfields)
                {
                    if (sf.getData().matches(args[1]))
                        return(true);
                }
            }
        }
        else if (conditional.startsWith("subfieldcontains("))
        {
            String args[] = getTwoArgs(conditional);
            if (args.length == 2 && args[0].length() == 1)
            {
                subfields = field.getSubfields(args[0].charAt(0));
                for (Subfield sf : subfields)
                {
                    if (sf.getData().contains(args[1]))
                        return(true);
                }
            }
        }
        else if (conditional.startsWith("subfieldexists("))
        {
            String arg = getOneArg(conditional);
            if (arg.length() == 1)
            {
                subfields = field.getSubfields(arg.charAt(0));
                if (subfields.size() > 0) return(true);
            }
        }
        else if (conditional.startsWith("and("))
        {
            String args[] = getTwoConditionals(conditional);
            if (args.length == 2)
            {
                return(eval(args[0], field) && eval(args[1], field));
            }
        }
        else if (conditional.startsWith("or("))
        {
            String args[] = getTwoConditionals(conditional);
            if (args.length == 2)
            {
                return(eval(args[0], field) || eval(args[1], field));
            }
        }
        return false;
    }
    
    private void process(String command, DataField field, List<Subfield> todelete)
    {
        List<Subfield> subfields;
        if (command.startsWith("replace("))
        {
            String args[] = getThreeArgs(command);
            if (args.length == 3 && args[0].length() == 1)
            {
                subfields = field.getSubfields(args[0].charAt(0));
                for (Subfield sf : subfields)
                {
                    String newData = sf.getData().replaceAll(args[1], args[2]);
                    if (!newData.equals(sf.getData()))
                    {
                        sf.setData(newData);
                    }
                }
            }
        }
        else if (command.startsWith("deletesubfield("))
        {
            String arg = getOneArg(command);
            if (arg.length() == 1)
            {
                subfields = field.getSubfields(arg.charAt(0));
                for (Subfield sf : subfields)
                {
                    todelete.add(sf);
                }
            }
        }        
    }
    
    static Pattern oneArg = Pattern.compile("[a-z]*[(]\"((\\\"|[^\"])*)\"[ ]*[)]");
    private String getOneArg(String conditional)
    {
        Matcher m = oneArg.matcher(conditional.trim());
        if (m.matches())
        {
            return(m.group(1).replaceAll("\\\"", "\""));
        }
        return null;
    }
    
    static Pattern twoArgs = Pattern.compile("[a-z]*[(]\"((\\\"|[^\"])*)\",[ ]*\"((\\\"|[^\"])*)\"[)]");
    private String[] getTwoArgs(String conditional)
    {
        Matcher m = twoArgs.matcher(conditional.trim());
        if (m.matches())
        {
            String result[] = new String[]{m.group(1).replaceAll("\\\"", "\""), m.group(3).replaceAll("\\\"", "\"")};
            return(result);
        }
        return null;
    }
    
    static Pattern threeArgs = Pattern.compile("[a-z]*[(][ ]*\"((\\\"|[^\"])*)\",[ ]*\"((\\\"|[^\"])*)\",[ ]*\"((\\\"|[^\"])*)\"[)]");
    private String[] getThreeArgs(String conditional)
    {
        Matcher m = threeArgs.matcher(conditional.trim());
        if (m.matches())
        {
            String result[] = new String[]{m.group(1).replaceAll("\\\"", "\""), m.group(3).replaceAll("\\\"", "\""), m.group(5).replaceAll("\\\"", "\"")};
            return(result);
        }
        return null;
    }
    
    static Pattern twoConditionals = Pattern.compile("[a-z]*[(]([a-z]*[(].*[)]),[ ]*([a-z]*[(].*[)])[)]");
    private String[] getTwoConditionals(String conditional)
    {
        Matcher m = twoConditionals.matcher(conditional.trim());
        if (m.matches())
        {
            String result[] = new String[]{m.group(1), m.group(2)};
            return(result);
        }
        return null;
    }

}
