/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.user;

import java.net.URI;

import de.mpg.imeji.logic.controller.GrantController;
import de.mpg.imeji.logic.security.Authorization;
import de.mpg.imeji.logic.security.Security;
import de.mpg.imeji.logic.security.Operations.OperationsType;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ObjectLoader;

public class SharingManager
{
    public boolean share(Object o, User owner, String email, GrantType role, boolean replaceGrant)
    {
        if (isShareUser(o, owner))
        {
            try
            {
                User target = ObjectLoader.loadUser(email, owner);
                if (target == null)
                    return false;
                else if (owner.getEmail().equals(email))
                {
                    BeanHelper.error("Forbidden action!");
                    return false;
                }
                GrantController gc = new GrantController(owner);
                URI uri = null;
                if (o instanceof Container)
                {
                    uri = ((Container)o).getId();
                }
                else if (o instanceof MetadataProfile)
                {
                    uri = ((MetadataProfile)o).getId();
                }
                Grant ng = new Grant(role, uri);
                if (replaceGrant)
                    target = gc.updateGrant(target, ng);
                else
                    target = gc.addGrant(target, ng);
                if (o instanceof CollectionImeji)
                {
                    try
                    {
                        Authorization authorization = new Authorization();
                        if (GrantType.CONTAINER_EDITOR.equals(role))
                        {
                            if (replaceGrant)
                                gc.updateGrant(target,
                                        new Grant(GrantType.PROFILE_ADMIN, ((CollectionImeji)o).getProfile()));
                            else
                                gc.addGrant(target,
                                        new Grant(GrantType.PROFILE_ADMIN, ((CollectionImeji)o).getProfile()));
                        }
                        else if (!authorization.is(GrantType.PROFILE_ADMIN, target, ((CollectionImeji)o).getProfile())
                                && !authorization.is(GrantType.PROFILE_EDITOR, target,
                                        ((CollectionImeji)o).getProfile()))
                        {
                            if (replaceGrant)
                                gc.updateGrant(target,
                                        new Grant(GrantType.PROFILE_VIEWER, ((CollectionImeji)o).getProfile()));
                            else
                                gc.addGrant(target,
                                        new Grant(GrantType.PROFILE_VIEWER, ((CollectionImeji)o).getProfile()));
                        }
                    }
                    catch (Exception e)
                    {
                        gc.removeGrant(target, ng);
                        throw e;
                    }
                }
                return true;
            }
            catch (Exception e)
            {
                BeanHelper.error(e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        BeanHelper.error(((SessionBean)BeanHelper.getSessionBean(SessionBean.class))
                .getMessage("error_share_not_enough_priviliges"));
        return false;
    }

    public boolean isShareUser(Object o, User user)
    {
        Security security = new Security();
        return security.check(OperationsType.UPDATE, user, o);
    }
}
