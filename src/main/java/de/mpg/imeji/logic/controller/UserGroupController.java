/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License"). You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE
 * or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at license/ESCIDOC.LICENSE.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft
 * für wissenschaftlich-technische Information mbH and Max-Planck-
 * Gesellschaft zur Förderung der Wissenschaft e.V.
 * All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.SearchFactory;
import de.mpg.imeji.logic.search.query.SPARQLQueries;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.writer.WriterFacade;
import de.mpg.j2j.exceptions.NotFoundException;

/**
 * Implements CRUD Methods for a {@link UserGroup}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class UserGroupController
{
    private static final ReaderFacade reader = new ReaderFacade(Imeji.userModel);
    private static final WriterFacade writer = new WriterFacade(Imeji.userModel);

    /**
     * Create a {@link UserGroup}
     * 
     * @param group
     * @throws Exception
     */
    public void create(UserGroup group, User user) throws Exception
    {
        writer.create(WriterFacade.toList(group), user);
    }

    /**
     * Read a {@link UserGroup} with the given uri
     * 
     * @param uri
     * @return
     * @throws Exception
     */
    public UserGroup read(String uri, User user) throws Exception
    {
        return (UserGroup)reader.read(uri, user, new UserGroup());
    }

    /**
     * Read a {@link UserGroup} with the given {@link URI}
     * 
     * @param uri
     * @return
     * @throws Exception
     */
    public UserGroup read(URI uri, User user) throws Exception
    {
        return read(uri.toString(), user);
    }

    /**
     * Update a {@link UserGroup}
     * 
     * @param group
     * @param user
     * @throws Exception
     */
    public void update(UserGroup group, User user) throws Exception
    {
        writer.update(WriterFacade.toList(group), user);
    }

    /**
     * Delete a {@link UserGroup}
     * 
     * @param group
     * @param user
     * @throws Exception
     */
    public void delete(UserGroup group, User user) throws Exception
    {
        writer.delete(WriterFacade.toList(group), user);
    }

    /**
     * Search all {@link UserGroup} having a {@link Grant} for the object defined in grantFor
     * 
     * @param grantFor
     * @param user
     * @return
     */
    public Collection<UserGroup> searchByGrantFor(String grantFor, User user)
    {
        return searchBySPARQLQuery(SPARQLQueries.selectUserGroupWithGrantFor(grantFor), user);
    }

    /**
     * Retrieve all {@link UserGroup} Only allowed for System administrator
     * 
     * @return
     */
    public Collection<UserGroup> searchByName(String q, User user)
    {
        return searchBySPARQLQuery(SPARQLQueries.selectUserGroupAll(q), user);
    }

    /**
     * Retrieve all {@link UserGroup} a user is member of
     * 
     * @return
     */
    public Collection<UserGroup> searchByUser(User member, User user)
    {
        return searchBySPARQLQuery(SPARQLQueries.selectUserGroupOfUser(member), Imeji.adminUser);
    }

    /**
     * Search {@link UserGroup} according a SPARQL Query
     * 
     * @param q
     * @param user
     * @return
     */
    private Collection<UserGroup> searchBySPARQLQuery(String q, User user)
    {
        Collection<UserGroup> userGroups = new ArrayList<UserGroup>();
        Search search = SearchFactory.create();
        for (String uri : search.searchSimpleForQuery(q).getResults())
        {
            try
            {
                userGroups.add((UserGroup)reader.read(uri, user, new UserGroup()));
            }
            catch (NotFoundException e)
            {
                throw new RuntimeException("UserGroup " + uri + " not found", e);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
        return userGroups;
    }
}
