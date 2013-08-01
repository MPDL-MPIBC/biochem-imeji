/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.controller.UserController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.AuthorizationBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ObjectLoader;

/**
 * Bean for the pages "CollectionEntryPage" and "ViewCollection"
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ViewCollectionBean extends CollectionBean
{
    private SessionBean sessionBean = null;
    private List<Person> persons = null;
    private static Logger logger = Logger.getLogger(ViewCollectionBean.class);

    /**
     * Construct a default {@link ViewCollectionBean}
     */
    public ViewCollectionBean()
    {
        super();
        sessionBean = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
    }

    /**
     * Initialize all elements of the page.
     */
    public void init()
    {
        try
        {
            User user = sessionBean.getUser();
            String id = getId();
            setCollection(ObjectLoader.loadCollectionLazy(ObjectHelper.getURI(CollectionImeji.class, id), user));
            if (getCollection() != null && getCollection().getId() != null)
            {
                ItemController ic = new ItemController(sessionBean.getUser());
                ic.loadContainerItems(getCollection(), user, 13, 0);
                setSize(getCollection().getImages().size());
            }
            if (getCollection() != null)
            {
                ((AuthorizationBean)BeanHelper.getSessionBean(AuthorizationBean.class)).init(getCollection());
                setProfile(ObjectLoader.loadProfile(getCollection().getProfile(), user));
                setProfileId(ObjectHelper.getId(getProfile().getId()));
                // super.setTab(TabType.COLLECTION);
                persons = new ArrayList<Person>();
                for (Person p : super.getCollection().getMetadata().getPersons())
                {
                    List<Organization> orgs = new ArrayList<Organization>();
                    for (Organization o : p.getOrganizations())
                    {
                        orgs.add(o);
                    }
                    p.setOrganizations(orgs);
                    persons.add(p);
                }
                getCollection().getMetadata().setPersons(persons);
            }
        }
        catch (Exception e)
        {
            BeanHelper.error(e.getMessage());
            logger.error("Error init of collection home page", e);
        }
    }

    public List<Person> getPersons()
    {
        return persons;
    }

    public void setPersons(List<Person> persons)
    {
        this.persons = persons;
    }

    @Override
    protected String getNavigationString()
    {
        return "pretty:collectionInfos";
    }

    public String getPersonString()
    {
        String personString = "";
        for (Person p : getCollection().getMetadata().getPersons())
        {
            if (!"".equalsIgnoreCase(personString))
                personString += " / ";
            personString += p.getFamilyName() + ", " + p.getGivenName() + " ";
        }
        return personString;
    }

    public String getSmallDescription()
    {
        if (this.getCollection() == null || this.getCollection().getMetadata().getDescription() == null)
            return "No Description";
        if (this.getCollection().getMetadata().getDescription().length() > 100)
        {
            return this.getCollection().getMetadata().getDescription().substring(0, 100) + "...";
        }
        else
        {
            return this.getCollection().getMetadata().getDescription();
        }
    }

    public String getFormattedDescription()
    {
        if (this.getCollection() == null || this.getCollection().getMetadata().getDescription() == null)
            return "";
        return this.getCollection().getMetadata().getDescription().replaceAll("\n", "<br/>");
    }

    /**
     * Return the {@link User} having uploaded the file for this item
     * 
     * @return
     * @throws Exception
     */
    public User getCollectionCreator() throws Exception
    {
        User user = null;
        UserController uc = new UserController();
        user = uc.retrieve(super.getCollection().getCreatedBy());
        return user;
    }

    public int getCollectionNumberOfItems()
    {
        int num = 0;
        num = super.getCollection().getImages().size();
        return num;
    }

    public String getCitation()
    {
        String title = super.getCollection().getMetadata().getTitle();
        String author = this.getPersonString();
        String url = super.getPageUrl();
        String citation = title + " " + sessionBean.getLabel("from") + " <i>" + author + "</i></br>" + url;
        return citation;
    }
}
