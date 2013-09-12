package de.mpg.imeji.logic.export.format;

import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.client.HttpResponseException;

import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.export.Export;
import de.mpg.imeji.logic.export.format.rdf.RDFAlbumExport;
import de.mpg.imeji.logic.export.format.rdf.RDFCollectionExport;
import de.mpg.imeji.logic.export.format.rdf.RDFImageExport;
import de.mpg.imeji.logic.export.format.rdf.RDFProfileExport;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.PropertyBean;

/**
 * {@link Export} in rdf
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class RDFExport extends Export
{
    protected String[] filteredTriples = {};
    protected List<String> filteredResources = new ArrayList<String>();
    protected Map<String, String> namespaces;
    protected String modelURI;

    /**
     * Factory for {@link RDFExport}
     * 
     * @param type
     * @return
     * @throws HttpResponseException
     */
    public static RDFExport factory(String type) throws HttpResponseException
    {
        if ("image".equalsIgnoreCase(type))
        {
            return new RDFImageExport();
        }
        else if ("collection".equalsIgnoreCase(type))
        {
            return new RDFCollectionExport();
        }
        else if ("album".equalsIgnoreCase(type))
        {
            return new RDFAlbumExport();
        }
        else if ("profile".equals(type))
        {
            return new RDFProfileExport();
        }
        throw new HttpResponseException(400, "Type " + type + " is not supported.");
    }

    @Override
    public void export(OutputStream out, SearchResult sr)
    {
        initNamespaces();
        filterResources(sr, super.user);
        exportIntoOut(sr, out);
    }

    @Override
    public String getContentType()
    {
        return "application/rdf+xml";
    }

    /**
     * Initialize the namespaces used for the export
     */
    protected abstract void initNamespaces();

    /**
     * Write the {@link SearchResult} in an {@link OutputStream}
     * 
     * @param sr
     * @param out
     */
    private void exportIntoOut(SearchResult sr, OutputStream out)
    {
        namespaces.put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf");
        Imeji.dataset.begin(ReadWrite.READ);
        try
        {
            Model model = Imeji.dataset.getNamedModel(modelURI);
            StringWriter writer = new StringWriter();
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.append("<rdf:RDF");
            for (String key : namespaces.keySet())
            {
                writer.append(" xmlns:" + namespaces.get(key) + "=\"" + key + "\"");
            }
            writer.append(">");
            for (String s : sr.getResults())
            {
                Resource resource = model.getResource(s);
                newLine(writer);
                writer.append(openTagResource(toUrl(s)));
                writer.append(exportResource(resource).getBuffer());
                writer.append(closeTagResource());
            }
            newLine(writer);
            writer.append("</rdf:RDF>");
            out.write(writer.getBuffer().toString().replace("&", "&amp;").getBytes());
        }
        catch (Exception e)
        {
            Imeji.dataset.abort();
            throw new RuntimeException("Error in export", e);
        }
        finally
        {
            Imeji.dataset.end();
        }
    }

    /**
     * Write a {@link Resource} in rdf
     * 
     * @param r
     * @return
     */
    private StringWriter exportResource(Resource r)
    {
        StringWriter writer = new StringWriter();
        if (isFilteredResource(r))
        {
            return writer;
        }
        newLine(writer);
        for (StmtIterator iterator = r.listProperties(); iterator.hasNext();)
        {
            Statement st = iterator.next();
            if (!isFiltered(st))
            {
                try
                {
                    writer.append(openTag(st, toUrl(st.getResource().getURI())));
                    writer.append(exportResource(st.getResource()).getBuffer());
                }
                catch (Exception e)
                {
                    /* Not a resource */
                }
                try
                {
                    if (st.getLiteral().toString() != null)
                    {
                        writer.append(openTag(st, null));
                        String literal = st.getLiteral().getString();
                        // Enclose embedded html
                        if (literal.contains("<html>"))
                            literal = "<![CDATA[" + literal + "]]>";
                        writer.append(literal);
                    }
                }
                catch (Exception e)
                {
                    /* Not a literal */
                }
                writer.append(closeTag(st));
                newLine(writer);
            }
        }
        return writer;
    }

    /**
     * TRansform an uri (used as id in rdf) to a real url
     * 
     * @param uri
     * @return
     */
    private String toUrl(String uri)
    {
        return uri.replace(PropertyBean.baseURI(), PropertyBean.applicationURL());
    }

    /**
     * Open an html tag for an rdf resource
     * 
     * @param uri
     * @return
     */
    protected abstract String openTagResource(String uri);

    /**
     * Close an html tag for an rdf resource
     * 
     * @return
     */
    protected abstract String closeTagResource();

    /**
     * Open an html tag for statement
     * 
     * @param st
     * @param resourceURI
     * @return
     */
    private String openTag(Statement st, String resourceURI)
    {
        String tag = "<" + getNamespace(st.getPredicate().getNameSpace()) + ":" + st.getPredicate().getLocalName();
        if (resourceURI != null)
        {
            tag += " rdf:resource=\"" + resourceURI + "\"";
        }
        tag += ">";
        return tag;
    }

    /**
     * Close an html tag for statement
     * 
     * @param st
     * @return
     */
    private String closeTag(Statement st)
    {
        return "</" + getNamespace(st.getPredicate().getNameSpace()) + ":" + st.getPredicate().getLocalName() + ">";
    }

    /**
     * return the namespace
     * 
     * @param ns
     * @return
     */
    private String getNamespace(String ns)
    {
        String ns1 = namespaces.get(ns);
        if (ns1 != null)
        {
            return ns1;
        }
        return ns;
    }

    /**
     * Add a new line separator
     * 
     * @param writer
     */
    private void newLine(StringWriter writer)
    {
        writer.append("\n");
    }

    /**
     * If the {@link Statement} is filtered (namespace has been defined as to be filtered), then return false
     * 
     * @param st
     * @return
     */
    private boolean isFiltered(Statement st)
    {
        // Check if the triple is filtered
        for (String s : filteredTriples)
        {
            if (s.equals(st.getPredicate().getURI()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * If the {@link Resource} is filtered (for instance a metadata which should not be displayed, because restricted to
     * current user), return true
     * 
     * @param r
     * @return
     */
    private boolean isFilteredResource(Resource r)
    {
        for (String s : filteredResources)
        {
            if (s.equals(r.getURI().toString()))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Check which {@link Resource} should be filtered
     * 
     * @param sr
     * @param user
     */
    protected abstract void filterResources(SearchResult sr, User user);
}
