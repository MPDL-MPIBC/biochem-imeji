/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.concurrency.locks;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Thread checking periodically if some {@link Lock} needs to be unlocked
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class LocksSurveyor extends Thread
{
    private static Logger logger = Logger.getLogger(LocksSurveyor.class);
    private boolean signal = false;
    private boolean running = false;

    @Override
    public void run()
    {
        logger.info("Lock Surveyor started: " + getName());
        Locks.init();
        running = true;
        while (!signal)
        {
            try
            {
                synchronized (logger)
                {
                    List<Lock> list = new ArrayList<Lock>(Locks.getExpiredLocks());
                    if (!Locks.getExpiredLocks().isEmpty())
                    {
                        logger.info("Unlocking dead locks...");
                        for (Lock l : Locks.getExpiredLocks())
                        {
                            list.add(l);
                        }
                        for (Lock l : list)
                        {
                            logger.info("on " + l.getUri() + " by " + l.getEmail());
                            Locks.unLock(l);
                        }
                    }
                    // wait a bit...
                    logger.wait(10000);
                    // Thread.sleep(10000);
                }
            }
            catch (NegativeArraySizeException e)
            {
                Locks.init();
                logger.error("Locks have been reinitialized. All locks have been released: ", e);
            }
            catch (Exception e)
            {
                logger.error("Locks Surveyor encountered a problem: ", e);
            }
        }
        logger.warn("Lock Surveyor stopped. It should not occurs if application is still running!");
        running = false;
    }

    /**
     * End the {@link Thread}
     */
    public void terminate()
    {
        logger.warn("Locks surveyor signaled to terminate!");
        signal = true;
        while (running)
        {
            logger.debug("Waiting for LocksSurveyor to stop...");
        }
    }
}
