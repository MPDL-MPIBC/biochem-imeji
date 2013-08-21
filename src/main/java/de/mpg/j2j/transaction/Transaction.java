package de.mpg.j2j.transaction;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;

import de.mpg.imeji.logic.Imeji;

/**
 * Transaction for Jena operations
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class Transaction
{
    private String modelURI;
    private boolean isException;
    private Exception exception;
    private static Logger logger = Logger.getLogger(Transaction.class);

    /**
     * Construct a {@link Transaction} for one model defined by its uri
     * 
     * @param modelURI
     */
    public Transaction(String modelURI)
    {
        this.modelURI = modelURI;
    }

    /**
     * Do the {@link Transaction} over the complete imeji {@link Dataset}
     */
    public void start()
    {
        start(Imeji.dataset);
    }

    /**
     * Do the {@link Transaction} over a {@link Dataset}.
     * 
     * @param dataset
     */
    public void start(Dataset dataset)
    {
        try
        {
            dataset.begin(getLockType());
            execute(dataset);
            dataset.commit();
        }
        catch (Exception e)
        {
            dataset.abort();
            isException = true;
            exception = e;
            logger.warn("Exception in a transaction: has been aborted", e);
        }
        finally
        {
            dataset.end();
        }
    }

    /**
     * Execute the operation of the {@link Transaction} Is called after the {@link Transaction} has been started
     * 
     * @param ds
     * @throws Exception
     */
    protected abstract void execute(Dataset ds) throws Exception;

    /**
     * Return the type of Jena lock ({@link ReadWrite}) uses for the {@link Transaction}
     * 
     * @return
     */
    protected abstract ReadWrite getLockType();

    /**
     * Return the {@link Model} of the {@link Dataset} according to the uri defined in constructor
     * 
     * @param dataset
     * @return
     */
    protected Model getModel(Dataset dataset)
    {
        if (modelURI != null)
        {
            return dataset.getNamedModel(modelURI);
        }
        return null;
    }

    /**
     * If the run Method caught an Exception, throw this exception
     * 
     * @throws Exception
     */
    public void throwException() throws Exception
    {
        if (isException)
        {
            throw exception;
        }
    }
}
