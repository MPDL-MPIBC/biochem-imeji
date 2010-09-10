package de.mpg.imeji.image;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.myfaces.trinidad.component.core.nav.CoreCommandButton;

import de.mpg.imeji.beans.SessionBean;
import de.mpg.imeji.util.BeanHelper;
import de.mpg.jena.vo.Image;

public class ImageBean
{
    private Image image;
    private boolean selected;

    public ImageBean(Image img)
    {
        this.image = img;
    }

    public void setImage(Image image)
    {
        this.image = image;
    }

    public Image getImage()
    {
        return image;
    }

    public void selectListener(ValueChangeEvent event)
    {
        SessionBean sb = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
        if (event.getNewValue() != null && event.getNewValue() != event.getOldValue())
        {
            selected = Boolean.parseBoolean(event.getNewValue().toString());
        }
        if (!selected)
            sb.getSelected().remove(image.getId());
        else
            sb.getSelected().add(this.image.getId());
    }

    public void select(ActionEvent event)
    {
        SessionBean sb = (SessionBean)BeanHelper.getSessionBean(SessionBean.class);
        if (!selected)
            sb.getSelected().remove(image.getId());
        else
            sb.getSelected().add(this.image.getId());
    }

    /**
     * @return the selected
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    public String getThumbnailImageUrlAsString()
    {
        return image.getThumbnailImageUrl().toString();
    }

    public String getId()
    {
        return image.getId().toString();
    }
}
