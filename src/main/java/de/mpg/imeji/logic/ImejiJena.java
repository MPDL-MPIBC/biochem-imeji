/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.lf5.util.StreamUtils;

import com.hp.hpl.jena.Jena;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.Lock;
import com.hp.hpl.jena.shared.LockMRSW;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.sys.LockMRSWLite;

import de.mpg.imeji.logic.util.Counter;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.util.PropertyReader;
import de.mpg.j2j.annotations.j2jModel;
import de.mpg.j2j.exceptions.NotFoundException;

/**
 * {@link Jena} interface for imeji
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ImejiJena
{
    public static String tdbPath = null;
    public static String collectionModel;
    public static String albumModel;
    public static String imageModel;
    public static String userModel;
    public static String profileModel;
    public static String counterModel = "http://imeji.org/counter";
    public static Dataset imejiDataSet;
    public static URI counterID = URI.create("http://imeji.org/counter/0");
    private static Logger logger = Logger.getLogger(ImejiJena.class);
    public static User adminUser;
    private static final String ADMIN_EMAIL_INIT = "admin@imeji.org";
    private static final String ADMIN_PASSWORD_INIT = "admin";

    /**
     * Initialize the {@link Jena} database according to imeji.properties<br/>
     * Called when the server (Tomcat of JBoss) is started
     */
    public static void init()
    {
        try
        {
            tdbPath = PropertyReader.getProperty("imeji.tdb.path");
            // tdbPath = "C:\\Projects\\Imeji\\tdb\\testjena";
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error reading property imeji.tdb.path", e);
        }
        init(tdbPath);
    }

    /**
     * Run the migration instruction (SPARQL Update queries defines in the migration.xml file)
     * 
     * @throws IOException
     */
    public static void runMigration() throws IOException
    {
        File f = new File(ImejiJena.tdbPath + StringHelper.urlSeparator + "migration.txt");
        FileInputStream in = null;
        try
        {
            in = new FileInputStream(f);
        }
        catch (FileNotFoundException e)
        {
            logger.info("No" + f.getAbsolutePath() + " found, no migration runs");
        }
        if (in != null)
        {
            String migrationRequests = new String(StreamUtils.getBytes(in), "UTF-8");
            logger.info("Running migration with query: ");
            logger.info(migrationRequests);
            ImejiSPARQL.execUpdate(migrationRequests);
            logger.info("Migration done!");
        }
    }

    /**
     * Initialize a {@link Jena} database according at one path location in filesystem
     * 
     * @param path
     */
    public static void init(String path)
    {
        File f = new File(path);
        if (!f.exists())
        {
            f.getParentFile().mkdirs();
        }
        tdbPath = f.getAbsolutePath();
        logger.info("Initializing Jena dataset (" + tdbPath + ")...");
        imejiDataSet = TDBFactory.createDataset(tdbPath);
        logger.info("... done!");
        logger.info("Initializing Jena models...");
        albumModel = getModelName(Album.class);
        collectionModel = getModelName(CollectionImeji.class);
        imageModel = getModelName(Item.class);
        userModel = getModelName(User.class);
        profileModel = getModelName(MetadataProfile.class);
        initModel(albumModel);
        initModel(collectionModel);
        initModel(imageModel);
        initModel(userModel);
        initModel(profileModel);
        initModel(counterModel);
        logger.info("Initializing Admin user...");
        initadminUser();
        logger.info("... done!");
        logger.info("Initializing counter...");
        initCounter();
        logger.info("... done!");
    }

    /**
     * Initialize (Create when not existing) a {@link Model} with a given name
     * 
     * @param name
     */
    private static void initModel(String name)
    {
        try
        {
            imejiDataSet = TDBFactory.createDataset(tdbPath);
            // Careful: This is a read locks. A write lock would lead to corrupted graph
            imejiDataSet.begin(ReadWrite.READ);
            if (imejiDataSet.containsNamedModel(name))
            {
                imejiDataSet.getNamedModel(name);
            }
            else
            {
                Model m = ModelFactory.createDefaultModel();
                imejiDataSet.addNamedModel(name, m);
            }
            imejiDataSet.commit();
        }
        finally
        {
            imejiDataSet.end();
        }
    }

    /**
     * Initialize the system administrator {@link User}, accoring to credentials in imeji.properties
     */
    private static void initadminUser()
    {
        adminUser = new User();
        adminUser.setEmail(ADMIN_EMAIL_INIT);
        adminUser.setName("imeji Sysadmin");
        adminUser.setNick("sysadmin");
        try
        {
            adminUser.setEncryptedPassword(StringHelper.convertToMD5(ADMIN_PASSWORD_INIT));
        }
        catch (Exception e)
        {
            throw new RuntimeException("error creating admin user: ", e);
        }
        Grant grant = new Grant();
        grant.setGrantType(URI.create("http://imeji.org/terms/grantType#" + GrantType.SYSADMIN.name()));
        grant.setGrantFor(URI.create("http://imeji.org/"));
        adminUser.getGrants().add(grant);
    }

    /**
     * Initialized the {@link Counter}
     */
    public static void initCounter()
    {
        int counterFirstValue = 0;
        try
        {
            counterFirstValue = Integer.parseInt(PropertyReader.getProperty("imeji.counter.first.value"));
        }
        catch (Exception e)
        {
            logger.warn("Property imeji.counter.first.value not found!!! Add property to your property file. (IGNORE BY UNIT TESTS)");
        }
        Counter c = new Counter();
        try
        {
            ImejiRDF2Bean rdf2Bean = new ImejiRDF2Bean(counterModel);
            c = (Counter)rdf2Bean.load(c.getId().toString(), adminUser, c);
            if (c.getCounter() < counterFirstValue)
            {
                createNewCouter(c, counterFirstValue);
            }
            logger.info("IMPORTANT: Counter found with value : " + c.getCounter());
        }
        catch (NotFoundException e)
        {
            logger.warn("IMPORTANT: Counter not found, creating a new one...");
            createNewCouter(c, counterFirstValue);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a new {@link Counter}
     * 
     * @param c
     * @param counterFirstValue
     */
    private static void createNewCouter(Counter c, int counterFirstValue)
    {
        c.setCounter(counterFirstValue);
        ImejiBean2RDF bean2rdf = new ImejiBean2RDF(counterModel);
        try
        {
            bean2rdf.create(bean2rdf.toList(c), adminUser);
            logger.warn("IMPORTANT: New Counter created with value " + c.getCounter());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the name of the model if defined in a {@link Class} with {@link j2jModel} annotation
     * 
     * @param voClass
     * @return
     */
    public static String getModelName(Class<?> voClass)
    {
        j2jModel j2jModel = voClass.getAnnotation(j2jModel.class);
        return "http://imeji.org/" + j2jModel.value();
    }

    /**
     * Print all data in one {@link Model} as RDF
     * 
     * @param modelName
     */
    public static void printModel(String modelName)
    {
        try
        {
            imejiDataSet.begin(ReadWrite.READ);
            imejiDataSet.getNamedModel(modelName).write(System.out, "RDF/XML-ABBREV");
            imejiDataSet.commit();
        }
        finally
        {
            imejiDataSet.end();
        }
    }

    /**
     * For testing/debugging purpose: run a select query an display the result
     * 
     * @param q
     */
    public static void selectQueryForTesting(String q)
    {
        try
        {
            ImejiJena.imejiDataSet.begin(ReadWrite.READ);
            for (Iterator<String> it = ImejiJena.imejiDataSet.listNames(); it.hasNext();)
            {
                String s = it.next();
                System.out.println(s);
            }
            QueryExecution qe = QueryExecutionFactory.create(q, Syntax.syntaxARQ, ImejiJena.imejiDataSet);
            qe.getContext().set(TDB.symUnionDefaultGraph, true);
            ResultSet rs = qe.execSelect();
            ResultSetFormatter.out(System.out, rs);
        }
        finally
        {
            ImejiJena.imejiDataSet.end();
        }
    }
}