/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import de.mpg.imeji.logic.concurrency.locks.Lock;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.security.Operations.OperationsType;
import de.mpg.imeji.logic.security.Security;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.presentation.metadata.editors.SimpleImageEditor;
import de.mpg.imeji.presentation.metadata.util.SuggestBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.UrlHelper;

/**
 * Metadata Editor for the detail item page
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SingleEditBean
{
    private Item item = null;
    private MetadataProfile profile = null;
    private SimpleImageEditor editor = null;
    private String toggleState = "displayMd";
    private int mdPosition = 0;
    private String pageUrl = "";
    private List<SuperMetadataBean> metadataList;

    /**
     * Constructor
     * 
     * @param im
     * @param profile
     * @param pageUrl
     */
    public SingleEditBean(Item im, MetadataProfile profile, String pageUrl)
    {
        item = im;
        this.profile = profile;
        this.pageUrl = pageUrl;
        init();
    }

    /**
     * Check in the url if the editor should be automatically shown
     * 
     * @return
     */
    public String getCheckToggleState()
    {
        toggleState = "displayMd";
        if (UrlHelper.getParameterBoolean("edit"))
        {
            showEditor();
        }
        return "";
    }

    /**
     * Initialize the page
     */
    public void init()
    {
        editor = new SimpleImageEditor(item, profile, null);
        ((SuggestBean)BeanHelper.getSessionBean(SuggestBean.class)).init(profile);
        metadataList = new ArrayList<SuperMetadataBean>();
        metadataList.addAll(editor.getItems().get(0).getMds().getTree().getList());
    }

    /**
     * Save the {@link Item} with its {@link Metadata}
     * 
     * @return
     * @throws Exception
     */
    public String save() throws Exception
    {
        editor.save();
        reloadPage();
        cancel();
        return "";
    }

    /**
     * Cancel the editing, and reset original values
     * 
     * @return
     * @throws Exception
     */
    public String cancel() throws Exception
    {
        this.toggleState = "displayMd";
        SessionBean sb = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
        Locks.unLock(new Lock(this.item.getId().toString(), sb.getUser().getEmail()));
        reloadImage();
        editor = new SimpleImageEditor(item, profile, null);
        return "";
    }

    /**
     * Reload the current page
     * 
     * @throws IOException
     */
    private void reloadPage() throws IOException
    {
        FacesContext.getCurrentInstance().getExternalContext().redirect(pageUrl + "?init=1");
    }

    /**
     * Reload the current image
     */
    private void reloadImage()
    {
        SessionBean sessionBean = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
        ItemController itemController = new ItemController(sessionBean.getUser());
        try
        {
            item = itemController.retrieve(item.getId());
        }
        catch (Exception e)
        {
        	//TODO
            BeanHelper.error("Error reload item" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Show the metadata editor
     * 
     * @return
     */
    public String showEditor()
    {
        SessionBean sb = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
        Security security = new Security();
        if (security.check(OperationsType.UPDATE, sb.getUser(), item))
        {
            this.toggleState = "editMd";
            try
            {
                Locks.lock(new Lock(item.getId().toString(), sb.getUser().getEmail()));
            }
            catch (Exception e)
            {
                BeanHelper.error(sb.getMessage("error_editor_image_locked"));
            }
            // init();
        }
        else
        {
            BeanHelper.error(sb.getMessage("error_editor_not_allowed"));
        }
        return "";
    }

    public SimpleImageEditor getEditor()
    {
        return editor;
    }

    public void setEditor(SimpleImageEditor editor)
    {
        this.editor = editor;
    }

    public Item getImage()
    {
        return item;
    }

    public void setImage(Item item)
    {
        this.item = item;
    }

    public MetadataProfile getProfile()
    {
        return profile;
    }

    public void setProfile(MetadataProfile profile)
    {
        this.profile = profile;
    }

    public int getMdPosition()
    {
        return mdPosition;
    }

    public void setMdPosition(int mdPosition)
    {
        this.mdPosition = mdPosition;
    }

    public String getToggleState()
    {
        return toggleState;
    }

    public void setToggleState(String toggleState)
    {
        this.toggleState = toggleState;
    }

    public List<SuperMetadataBean> getMetadataList()
    {
        return metadataList;
    }

    public void setMetadataList(List<SuperMetadataBean> metadataList)
    {
        this.metadataList = metadataList;
    }
}
