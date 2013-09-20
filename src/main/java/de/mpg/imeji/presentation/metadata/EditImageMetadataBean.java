/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.concurrency.locks.Lock;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.vo.SearchQuery;
import de.mpg.imeji.logic.util.MetadataFactory;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.lang.MetadataLabels;
import de.mpg.imeji.presentation.metadata.editors.MetadataEditor;
import de.mpg.imeji.presentation.metadata.editors.MetadataMultipleEditor;
import de.mpg.imeji.presentation.metadata.util.MetadataHelper;
import de.mpg.imeji.presentation.metadata.util.SuggestBean;
import de.mpg.imeji.presentation.search.URLQueryTransformer;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ImejiFactory;
import de.mpg.imeji.presentation.util.ObjectLoader;
import de.mpg.imeji.presentation.util.ProfileHelper;
import de.mpg.imeji.presentation.util.UrlHelper;

/**
 * Bean for batch and multiple metadata editor
 * 
 * @author saquet
 */
public class EditImageMetadataBean
{
    // objects
    private List<Item> allItems;
    private MetadataEditor editor = null;
    private MetadataProfile profile = null;
    private Statement statement = null;
    /**
     * the {@link EditorItemBean} used to for the editor and which will be copied to all {@link Item}
     */
    private EditorItemBean editorItem = null;
    // menus
    private List<SelectItem> statementMenu = null;
    private String selectedStatementName = null;
    private List<SelectItem> modeRadio = null;
    private String selectedMode = "basic";
    // other
    private int mdPosition;
    private int imagePosition;
    private boolean isProfileWithStatements = true;
    private int lockedImages = 0;
    private boolean initialized = false;
    private SessionBean session = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
    private static Logger logger = Logger.getLogger(EditImageMetadataBean.class);
    // url parameters
    private String type = "all";
    private String query = "";
    private String collectionId = null;

    /**
     * Bean for batch and multiple metadata editor
     */
    public EditImageMetadataBean()
    {
        statementMenu = new ArrayList<SelectItem>();
        modeRadio = new ArrayList<SelectItem>();
    }

    /**
     * Initialize all elements of the bean
     */
    public void init()
    {
        reset();
        try
        {
            allItems = loadItems();
            initProfileAndStatement(allItems);
            initStatementsMenu();
            initEditor(new ArrayList<Item>(allItems));
            ((MetadataLabels)BeanHelper.getSessionBean(MetadataLabels.class)).init(profile);
            initialized = true;
        }
        catch (Exception e)
        {
            BeanHelper.error(((SessionBean)BeanHelper.getSessionBean(SessionBean.class)).getLabel("error") + " " + e);
            logger.error("Error init Edit page", e);
        }
    }

    /**
     * Set all pages element to their default values
     */
    public void reset()
    {
        initialized = false;
        statementMenu = new ArrayList<SelectItem>();
        modeRadio = new ArrayList<SelectItem>();
        if (editor != null)
        {
            editor.reset();
        }
        statement = null;
    }

    /**
     * Go back to the previous page
     * 
     * @return
     * @throws IOException
     */
    public String cancel() throws IOException
    {
        redirectToView();
        return "";
    }

    /**
     * Read the url paramameters when the page is first called. This method ios called directly from the xhtml page
     * 
     * @return
     */
    public String getUrlParameters()
    {
        type = UrlHelper.getParameterValue("type");
        query = UrlHelper.getParameterValue("q");
        collectionId = UrlHelper.getParameterValue("c");
        return "";
    }

    /**
     * Load the items to be edited
     * 
     * @return
     * @throws IOException
     */
    private List<Item> loadItems() throws IOException
    {
        List<String> uris = new ArrayList<String>();
        if ("selected".equals(type))
        {
            uris = getSelectedItems();
        }
        else if ("all".equals(type) && query != null && collectionId != null)
        {
            uris = searchItems();
        }
        else
        {
            return allItems;
        }
        return loaditems(uris);
    }

    /**
     * Load the profile of the images, and set the statement to be edited.
     * 
     * @param items
     */
    private void initProfileAndStatement(List<Item> items)
    {
        profile = null;
        if (items != null && items.size() > 0)
        {
            profile = ObjectLoader.loadProfile(items.get(0).getMetadataSet().getProfile(), session.getUser());
        }
        statement = getSelectedStatement();
    }

    /**
     * Init the {@link MetadataEditor}
     * 
     * @param items
     * @return
     */
    private String initEditor(List<Item> items)
    {
        try
        {
            isProfileWithStatements = true;
            if (statement != null)
            {
                initEmtpyEditorItem();
                editor = new MetadataMultipleEditor(items, profile, getSelectedStatement());
                ((SuggestBean)BeanHelper.getSessionBean(SuggestBean.class)).init(profile);
            }
            else
            {
                logger.error("No statement found");
                isProfileWithStatements = false;
                BeanHelper.error(((SessionBean)BeanHelper.getSessionBean(SessionBean.class)).getLabel("profile_empty"));
            }
            initModeMenu();
        }
        catch (Exception e)
        {
            BeanHelper.error(((SessionBean)BeanHelper.getSessionBean(SessionBean.class)).getLabel("error") + " " + e);
            logger.error("Error init Edit page", e);
        }
        return "";
    }

    /**
     * Initialize the {@link EditorItemBean} with a new emtpy one;
     */
    private void initEmtpyEditorItem()
    {
        Item emtpyItem = new Item();
        emtpyItem.getMetadataSets().add(ImejiFactory.newMetadataSet(profile.getId()));
        editorItem = new EditorItemBean(emtpyItem, profile, true);
        editorItem.getMds().addEmtpyValues();
    }

    /**
     * Init the radio select menu with the 3 edit modes (overwrite all values, append new value, add if empty)
     */
    private void initModeMenu()
    {
        selectedMode = "basic";
        modeRadio = new ArrayList<SelectItem>();
        modeRadio.add(new SelectItem("basic", ((SessionBean)BeanHelper.getSessionBean(SessionBean.class))
                .getMessage("editor_basic")));
        if (this.statement.getMaxOccurs().equals("unbounded"))
        {
            modeRadio.add(new SelectItem("append", ((SessionBean)BeanHelper.getSessionBean(SessionBean.class))
                    .getMessage("editor_append")));
        }
        modeRadio.add(new SelectItem("overwrite", ((SessionBean)BeanHelper.getSessionBean(SessionBean.class))
                .getMessage("editor_overwrite")));
    }

    /**
     * Initialize the select menu with the possible statement to edit (i.e. statement of the profiles)
     */
    private void initStatementsMenu()
    {
        statementMenu = new ArrayList<SelectItem>();
        for (Statement s : profile.getStatements())
        {
            if (s.getParent() == null)
            {
                // Add a statement to the menu only if it doen'st have a parent statement. If it has a parent, then it
                // will be editable by choosing the parent in the menu
                statementMenu.add(new SelectItem(s.getId().toString(), ((MetadataLabels)BeanHelper
                        .getSessionBean(MetadataLabels.class)).getInternationalizedLabels().get(s.getId())));
            }
        }
    }

    /**
     * Change the statement to edit
     * 
     * @return
     */
    public String changeStatement()
    {
        statement = getSelectedStatement();
        // Reset the original items
        initEditor(new ArrayList<Item>(allItems));
        return "";
    }

    /**
     * Set to the original state
     * 
     * @return
     */
    public String resetChanges()
    {
        init();
        return "";
    }

    /**
     * Load the list of items
     * 
     * @param uris
     * @return
     */
    public List<Item> loaditems(List<String> uris)
    {
        ItemController itemController = new ItemController(session.getUser());
        return (List<Item>)itemController.loadItems(uris, -1, 0);
    }

    /**
     * Load the selected item from the session
     * 
     * @return
     */
    public List<String> getSelectedItems()
    {
        List<String> l = new ArrayList<String>(session.getSelected().size());
        for (String uri : session.getSelected())
        {
            l.add(uri);
        }
        return l;
    }

    /**
     * Search for item according to the query
     * 
     * @return
     * @throws IOException
     */
    public List<String> searchItems() throws IOException
    {
        SearchQuery sq = URLQueryTransformer.parseStringQuery(query);
        ItemController itemController = new ItemController(session.getUser());
        SearchResult sr = itemController.search(URI.create(collectionId), sq, null, null);
        return sr.getResults();
    }

    /**
     * For batch edit: Add the same values to all images and save.
     * 
     * @return
     * @throws IOException
     */
    public String addToAllSaveAndRedirect() throws IOException
    {
        addToAll();
        saveAndRedirect();
        return "";
    }

    /**
     * For the Multiple Edit: Save the current values
     * 
     * @return
     * @throws IOException
     */
    public String saveAndRedirect() throws IOException
    {
        editor.save();
        redirectToView();
        return "";
    }

    /**
     * Lock the {@link Item} which are currently in the editor. This prevent other users to make concurrent
     * modification.
     * 
     * @param items
     */
    private void lockImages(List<Item> items)
    {
        lockedImages = 0;
        for (int i = 0; i < items.size(); i++)
        {
            try
            {
                Locks.lock(new Lock(items.get(i).getId().toString(), session.getUser().getEmail()));
            }
            catch (Exception e)
            {
                editor.getItems().remove(i);
                lockedImages++;
                i--;
            }
        }
    }

    /**
     * Release the lock on all current {@link Item}
     */
    private void unlockImages()
    {
        SessionBean sb = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
        for (EditorItemBean eib : editor.getItems())
        {
            Locks.unLock(new Lock(eib.asItem().getId().toString(), sb.getUser().getEmail()));
        }
    }

    /**
     * Called method when "add to all" button is clicked
     * 
     * @return
     */
    public String addToAll()
    {
        for (EditorItemBean eib : editor.getItems())
        {
            if ("overwrite".equals(selectedMode))
            {
                // remove all metadata which have the same statement
                eib.clear(statement);
                // Prepare the emtpy values in which the new values will be added
                eib.getMds().addEmtpyValues();
            }
            else if ("append".equals(selectedMode))
            {
                // Add an emtpy metadata at the position we want to have it
                appendEmtpyMetadataIfNecessary(eib);
            }
            // Add the Metadata which has been entered to the emtpy Metadata with the same statement in the editor
            eib = pasteMetadataIfEmtpy(eib);
        }
        // Make a new Emtpy Metadata of the same statement
        initEmtpyEditorItem();
        return "";
    }

    /**
     * redirect to previous page
     * 
     * @throws IOException
     */
    public void redirectToView() throws IOException
    {
        initialized = false;
        unlockImages();
        HistorySession hs = (HistorySession)BeanHelper.getSessionBean(HistorySession.class);
        FacesContext.getCurrentInstance().getExternalContext()
                .redirect(hs.getPreviousPage().getUri().toString().replace("?h=", ""));
    }

    /**
     * Remove all metadata
     * 
     * @return
     */
    public String clearAll()
    {
        for (EditorItemBean eib : editor.getItems())
        {
            eib.clear(statement);
            eib.getMds().addEmtpyValues();
        }
        return "";
    }

    /**
     * fill all emtpy Metadata of passed {@link EditorItemBean} with the values of the current one
     * 
     * @param im
     * @return
     */
    private EditorItemBean pasteMetadataIfEmtpy(EditorItemBean eib)
    {
        List<SuperMetadataBean> list = fillEmtpyValues(eib.getMds().getTree().getList(), editorItem.getMds().getTree()
                .getList());
        list = SuperMetadataTree.resetPosition(list);
        eib.getMds().initTreeFromList(list);
        return eib;
    }

    /**
     * Fill l1 emtpy metadata with non emtpy metadata from l2
     * 
     * @param l1
     * @param l2
     */
    private List<SuperMetadataBean> fillEmtpyValues(List<SuperMetadataBean> l1, List<SuperMetadataBean> l2)
    {
        List<SuperMetadataBean> filled = new ArrayList<SuperMetadataBean>();
        for (SuperMetadataBean md1 : l1)
        {
            boolean emtpy1 = MetadataHelper.isEmpty(md1.asMetadata());
            for (SuperMetadataBean md2 : l2)
            {
                boolean emtpy2 = MetadataHelper.isEmpty(md2.asMetadata());
                if (md1.getStatementId().equals(md2.getStatementId()))
                {
                    if (emtpy1 && !emtpy2)
                        filled.add(md2.copy());
                    else
                        filled.add(md1);
                }
            }
        }
        return filled;
    }

    /**
     * Add an emtpy {@link Metadata} accroding to the current {@link Statement} to the {@link EditorItemBean}. If the
     * {@link EditorItemBean} has already an emtpy {@link Metadata} for this {@link Statement}, then don't had it.
     * 
     * @param eib
     */
    private void appendEmtpyMetadataIfNecessary(EditorItemBean eib)
    {
        List<SuperMetadataBean> l = eib.getMds().getTree().getList();
        for (Statement st : profile.getStatements())
            if (hasValue(eib, st))
                l.add(new SuperMetadataBean(MetadataFactory.createMetadata(st), st));
        eib.getMds().initTreeFromList(SuperMetadataTree.resetPosition(l));
    }

    /**
     * True if the {@link EditorItemBean} has a value for the {@link Statement}
     * 
     * @param eib
     * @param st
     * @return
     */
    private boolean hasValue(EditorItemBean eib, Statement st)
    {
        for (SuperMetadataBean md : eib.getMds().getTree().getList())
            if (md.getStatement().getId().compareTo(st.getId()) == 0 && !MetadataHelper.isEmpty(md.asMetadata()))
                return true;
        return false;
    }

    /**
     * Return the {@link Statement} which is currently edited
     * 
     * @return
     */
    public Statement getSelectedStatement()
    {
        if (profile != null)
        {
            for (Statement s : profile.getStatements())
            {
                if (s.getId().toString().equals(selectedStatementName))
                {
                    return s;
                }
            }
        }
        return getDefaultStatement();
    }

    /**
     * Return the first {@link Statement} of the current {@link MetadataProfile}
     * 
     * @return
     */
    public Statement getDefaultStatement()
    {
        if (profile != null && profile.getStatements().iterator().hasNext())
        {
            return profile.getStatements().iterator().next();
        }
        return null;
    }

    public boolean isEditableStatement(Statement st)
    {
        URI lastParent = ProfileHelper.getLastParent(st, profile);
        return statement.getId().compareTo(st.getId()) == 0
                || (lastParent != null && statement.getId().compareTo(lastParent) == 0);
    }

    public int getMdPosition()
    {
        return mdPosition;
    }

    public void setMdPosition(int mdPosition)
    {
        this.mdPosition = mdPosition;
    }

    public int getImagePosition()
    {
        return imagePosition;
    }

    public void setImagePosition(int imagePosition)
    {
        this.imagePosition = imagePosition;
    }

    public MetadataEditor getEditor()
    {
        return editor;
    }

    public void setEditor(MetadataEditor editor)
    {
        this.editor = editor;
    }

    public List<SelectItem> getStatementMenu()
    {
        return statementMenu;
    }

    public void setStatementMenu(List<SelectItem> statementMenu)
    {
        this.statementMenu = statementMenu;
    }

    public String getSelectedStatementName()
    {
        return selectedStatementName;
    }

    public void setSelectedStatementName(String selectedStatementName)
    {
        this.selectedStatementName = selectedStatementName;
    }

    public MetadataProfile getProfile()
    {
        return profile;
    }

    public void setProfile(MetadataProfile profile)
    {
        this.profile = profile;
    }

    public List<SelectItem> getModeRadio()
    {
        return modeRadio;
    }

    public void setModeRadio(List<SelectItem> modeRadio)
    {
        this.modeRadio = modeRadio;
    }

    public String getSelectedMode()
    {
        return selectedMode;
    }

    public void setSelectedMode(String selectedMode)
    {
        this.selectedMode = selectedMode;
    }

    public String getEditType()
    {
        return type;
    }

    public void setEditType(String editType)
    {
        this.type = editType;
    }

    public Statement getStatement()
    {
        return statement;
    }

    public void setStatement(Statement statement)
    {
        this.statement = statement;
    }

    public boolean isProfileWithStatements()
    {
        return isProfileWithStatements;
    }

    public void setProfileWithStatements(boolean isProfileWithStatements)
    {
        this.isProfileWithStatements = isProfileWithStatements;
    }

    public int getLockedImages()
    {
        return lockedImages;
    }

    public void setLockedImages(int lockedImages)
    {
        this.lockedImages = lockedImages;
    }

    public void setInitialized(boolean initialized)
    {
        this.initialized = initialized;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    /**
     * @return the editorItemBean
     */
    public EditorItemBean getEditorItem()
    {
        return editorItem;
    }

    /**
     * @param editorItemBean the editorItemBean to set
     */
    public void setEditorItem(EditorItemBean editorItemBean)
    {
        this.editorItem = editorItemBean;
    }
}
